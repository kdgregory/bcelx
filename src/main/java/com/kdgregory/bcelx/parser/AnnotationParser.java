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

    private JavaClass classFile;
    private ConstantPool cp;

    // these will be lazily instantiated as needed
    private List<Annotation> classVisibleAnnotations;
    private List<Annotation> classInvisibleAnnotations;
    private Map<Method,Map<String,Annotation>> methodAnnotations;


    public AnnotationParser(JavaClass classFile)
    {
        this.classFile = classFile;
        this.cp = classFile.getConstantPool();
    }


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
        if (classVisibleAnnotations == null)
        {
            classVisibleAnnotations = parseAnnotationsFromAttributes(
                                        classFile.getAttributes(),
                                        RetentionPolicy.RUNTIME);
        }
        return classVisibleAnnotations;
    }


    /**
     *  Returns the runtime-invisible annotations for this class, an empty list if
     *  there aren't any such annotations.
     */
    public List<Annotation> getClassInvisibleAnnotations()
    {
        if (classInvisibleAnnotations == null)
        {
            classInvisibleAnnotations = parseAnnotationsFromAttributes(
                                        classFile.getAttributes(),
                                        RetentionPolicy.CLASS);
        }
        return classInvisibleAnnotations;
    }


    /**
     *  Returns a list of the methods that are marked with the specified
     *  runtime-visible annotation class, an empty list if there aren't any.
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
     *  Returns a list of the methods that are marked with the specified runtime
     *  visible annotation, and whose parameters match the name-value pairs passed
     *  in the provided map. Annotation parameters not included in the map are
     *  considered matched, and an empty map matches all parameters.
     *  <p>
     *  All parameter values are passed as object instances. Numeric values must be
     *  passed as the appropriate wrapper type. Array-valued parameters may be passed
     *  as either an array or a <code>List</code>.
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


    /**
     *  The common parsing code: given the attributes that belong to a class /
     *  method / field, finds those that are hold attributes, and pull the
     *  attributes out of them.
     */
    private List<Annotation> parseAnnotationsFromAttributes(Attribute[] attrs, RetentionPolicy annoPolicy)
    {
        List<Annotation> result = new ArrayList<Annotation>();
        String attrName = (annoPolicy == RetentionPolicy.CLASS) ? ATTR_ANNOTATION_CLASS
                        : (annoPolicy == RetentionPolicy.RUNTIME) ? ATTR_ANNOTATION_RUNTIME
                        : "";  // should never happen, means we won't match anything
        try
        {
            for (Attribute attr : attrs)
            {
                String name = stringConstant(attr.getNameIndex());
                if (attrName.equals(name))
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
     *  Builds the maps that associated annotations to methods. This is intended
     *  to happen lazily.
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

            List<Annotation> annotations = parseAnnotationsFromAttributes(
                                            method.getAttributes(),
                                            RetentionPolicy.RUNTIME);
            for (Annotation anno : annotations)
            {
                annoMap.put(anno.getClassName(), anno);
            }
        }
    }
}
