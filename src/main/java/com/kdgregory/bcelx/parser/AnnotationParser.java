// Copyright Keith D Gregory
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.kdgregory.bcelx.parser;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantObject;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Unknown;

import net.sf.kdgcommons.lang.ClassUtil;
import net.sf.kdgcommons.lang.UnreachableCodeException;

import com.kdgregory.bcelx.classfile.Annotation;
import com.kdgregory.bcelx.classfile.Annotation.ArrayValue;
import com.kdgregory.bcelx.classfile.Annotation.ClassValue;
import com.kdgregory.bcelx.classfile.Annotation.EnumValue;
import com.kdgregory.bcelx.classfile.Annotation.ParamType;
import com.kdgregory.bcelx.classfile.Annotation.ParamValue;
import com.kdgregory.bcelx.classfile.Annotation.ScalarValue;


/**
 *  Extracts annotations from classfile data.
 *  <p>
 *  Any of the methods from this class can throw {@link ParseException}. This is a
 *  runtime exception; it almost certainly indicates a bug in the parser (or a bad
 *  classfile, which is equally unrecoverable).
 */
public class AnnotationParser
{
    private final static String ATTR_ANNOTATION_RUNTIME = "RuntimeVisibleAnnotations";
    private final static String ATTR_ANNOTATION_CLASS   = "RuntimeInvisibleAnnotations";


//----------------------------------------------------------------------------
//  Instance variables and constructors
//----------------------------------------------------------------------------

    // initialized by ctor
    private JavaClass classFile;
    private ConstantPool cp;

    // lazily initialized
    private List<Annotation> classAnnotations;
    private Map<Method,Map<String,Annotation>> methodAnnotations;


    public AnnotationParser(JavaClass classFile)
    {
        this.classFile = classFile;
        this.cp = classFile.getConstantPool();
    }


//----------------------------------------------------------------------------
//  Public methods
//----------------------------------------------------------------------------

    /**
     *  Returns the parsed class object. This is a convenience for callers who may
     *  be passed just the parser instance (because whoever created the parser can
     *  simply hold onto the parsed class).
     */
    public JavaClass getParsedClass()
    {
        return classFile;
    }


    /**
     *  Returns the runtime-visible annotations for this class, an empty list if
     *  there aren't any such annotations.
     */
    public List<Annotation> getClassVisibleAnnotations()
    {
        lazyBuildClassAnnotationList();
        List<Annotation> result = new ArrayList<Annotation>(classAnnotations.size());
        for (Annotation anno : classAnnotations)
        {
            if (anno.getRetentionPolicy() == RetentionPolicy.RUNTIME)
                result.add(anno);
        }
        return result;
    }


    /**
     *  Returns the runtime-invisible annotations for this class, an empty list if
     *  there aren't any such annotations.
     */
    public List<Annotation> getClassInvisibleAnnotations()
    {
        lazyBuildClassAnnotationList();
        List<Annotation> result = new ArrayList<Annotation>(classAnnotations.size());
        for (Annotation anno : classAnnotations)
        {
            if (anno.getRetentionPolicy() == RetentionPolicy.CLASS)
                result.add(anno);
        }
        return result;
    }


    /**
     *  Returns a list of the methods that are marked with the specified
     *  annotation (visible or invisible), an empty list if there aren't any.
     *  <p>
     *  This method examines all methods that are declared by the class, including
     *  private and protected methods. Caller is responsible for filtering the list
     *  based on method attributes, and/or loading any superclasses/interfaces.
     *  <p>
     *  The order of the returned methods is not guaranteed, and may differ between
     *  invocations.
     */
    public List<Method> getAnnotatedMethods(String className)
    {
        List<Method> result = new ArrayList<Method>();

        lazyBuildMethodAnnotationMap();
        for (Map.Entry<Method,Map<String,Annotation>> annos : methodAnnotations.entrySet())
        {

            if (annos.getValue().containsKey(className))
                result.add(annos.getKey());
        }

        return result;
    }


    /**
     *  Returns a list of the methods that are marked with the specified
     *  annotation (visible or invisible), and whose parameters match the
     *  name-value pairs passed in the provided map. Returns an empty list
     *  if there are no methods that match.
     *  <p>
     *  Parameter values must be an appropriate type, as specified by
     *  <code>Annotation.ParamValue.valueEquals()</code>. Parameters that do
     *  not appear in the map are considered matched (wildcarded). An empty
     *  map matches all parameters (ie, is equivalent to the single-argument
     *  variant of this method).
     *  <p>
     *  This method examines all methods that are declared by the class, including
     *  private and protected methods. Caller is responsible for filtering the list
     *  based on method attributes, and/or loading any superclasses/interfaces.
     *  <p>
     *  The order of the returned methods is not guaranteed, and may differ between
     *  invocations.
     */
    public List<Method> getAnnotatedMethods(String className, Map<String,Object> filter)
    {
        List<Method> result = new ArrayList<Method>();

        for (Method method : getAnnotatedMethods(className))
        {
            inner:
            {
                Annotation anno = methodAnnotations.get(method).get(className);
                for (Map.Entry<String,Object> param : filter.entrySet())
                {
                    String paramName = param.getKey();
                    Object paramValue = param.getValue();
                    ParamValue annoParam = anno.getParam(paramName);
                    if ((annoParam == null) || !annoParam.valueEquals(paramValue))
                        break inner;
                }
                result.add(method);
            }
        }

        return result;
    }


    /**
     *  Returns all runtime-visible annotations for the passed method. Will return
     *  an empty list if there are no annotations, or if the method does not belong
     *  to the class associated with this parser.
     */
    public List<Annotation> getMethodAnnotations(Method method)
    {
        lazyBuildMethodAnnotationMap();
        Map<String,Annotation> annos = methodAnnotations.get(method);
        return (annos == null)
             ? Collections.<Annotation>emptyList()
             : new ArrayList<Annotation>(annos.values());
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------


    /**
     *  Builds the list of annotations from the class attributes.
     */
    private void lazyBuildClassAnnotationList()
    {
        if (classAnnotations != null)
            return;

        classAnnotations = parseAnnotationsFromAttributes(classFile.getAttributes());
    }


    /**
     *  Builds the map of methods to their annotations.
     */
    private void lazyBuildMethodAnnotationMap()
    {
        if (methodAnnotations != null)
            return;

        methodAnnotations = new IdentityHashMap<Method,Map<String,Annotation>>();
        for (Method method : classFile.getMethods())
        {
            Map<String,Annotation> annoMap = new HashMap<String,Annotation>();
            methodAnnotations.put(method, annoMap);
            for (Annotation anno : parseAnnotationsFromAttributes(method.getAttributes()))
            {
                annoMap.put(anno.getClassName(), anno);
            }
        }
    }


    /**
     *  The common parsing code: given the attributes that belong to a class /
     *  method / field, finds those that are hold annotations, and pull the
     *  annotations out of them. This method combines visible and invisible
     *  attributes; caller is responsible for separating them based on
     *  <code>RetentionPolicy</code>.
     */
    private List<Annotation> parseAnnotationsFromAttributes(Attribute[] attrs)
    {
        List<Annotation> result = new ArrayList<Annotation>();
        try
        {
            for (Attribute attr : attrs)
            {
                String name = stringConstant(attr.getNameIndex());
                RetentionPolicy annoPolicy = name.equals(ATTR_ANNOTATION_CLASS) ? RetentionPolicy.CLASS
                                           : name.equals(ATTR_ANNOTATION_RUNTIME) ? RetentionPolicy.RUNTIME
                                           : null;
                if (annoPolicy != null)
                {
                    byte[] attrData = ((Unknown)attr).getBytes();
                    DataInputStream in = new DataInputStream(
                                            new ByteArrayInputStream(attrData));
                    int numAnnos = in.readUnsignedShort();
                    for (int ii = 0 ; ii < numAnnos ; ii++)
                    {
                        result.add(parseAnnotationFromStream(in, annoPolicy));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new ParseException("unable to parse attribute", ex);
        }
        return result;
    }


    /**
     *  Parses a single annotation from the passed stream.
     */
    private Annotation parseAnnotationFromStream(DataInputStream in, RetentionPolicy annoPolicy)
    throws IOException
    {
        String annoTypeName = typeNameConstant(in.readUnsignedShort());
        Annotation anno = new Annotation(annoTypeName, annoPolicy);

        int numElementValuePairs = in.readUnsignedShort();
        for (int jj = 0 ; jj < numElementValuePairs ; jj++)
        {
            String paramName = stringConstant(in.readUnsignedShort());
            ParamValue paramValue = parseAnnotationValue(in);
            anno.addParam(paramName, paramValue);
        }
        return anno;
    }


    private ParamValue parseAnnotationValue(DataInputStream in)
    throws IOException
    {
        int tag = in.readUnsignedByte();

        switch (tag)
        {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
                int baseIndex = in.readUnsignedShort();
                ConstantObject baseValue = (ConstantObject)cp.getConstant(baseIndex);
                return new ScalarValue(ParamType.NUMBER, baseValue.getConstantValue(cp));
            case 's':
                int stringIndex = in.readUnsignedShort();
                return new ScalarValue(ParamType.STRING, stringConstant(stringIndex));
            case 'c':
                int classIndex = in.readUnsignedShort();
                return new ClassValue(typeNameConstant(classIndex));
            case 'e':
                int enumTypeIndex = in.readUnsignedShort();
                int enumValueIndex = in.readUnsignedShort();
                return new EnumValue(typeNameConstant(enumTypeIndex), stringConstant(enumValueIndex));
            case '[':
                int arraySize = in.readUnsignedShort();
                List<ParamValue> arrayValues = new ArrayList<ParamValue>(arraySize);
                for (int ii = 0 ; ii < arraySize ; ii++)
                {
                    arrayValues.add(parseAnnotationValue(in));
                }
                return new ArrayValue(arrayValues);
            default :
                throw new UnreachableCodeException("invalid value tag: " + tag);
        }
    }


    /**
     *  Retrieves a string from the constant pool by its index.
     */
    private String stringConstant(int index)
    {
        ConstantUtf8 val = (ConstantUtf8)cp.getConstant(index);
        return val.getBytes();
    }


    /**
     *  Retrieves a string that represents a classname, and converts it from
     *  internal format to external.
     */
    private String typeNameConstant(int index)
    {
        String str = stringConstant(index);
        return ClassUtil.internalNameToExternal(str);
    }
}
