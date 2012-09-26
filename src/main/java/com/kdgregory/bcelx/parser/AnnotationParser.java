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
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantObject;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Unknown;

import net.sf.kdgcommons.lang.ClassUtil;

import com.kdgregory.bcelx.classfile.Annotation;
import com.kdgregory.bcelx.classfile.Annotation.ArrayValue;
import com.kdgregory.bcelx.classfile.Annotation.AnnotationValue;
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
 *  <p>
 *  This class does not attempt to make defensive copies of its internal state. If
 *  you muck with return values, don't expect them to be valid later. To keep you
 *  honest, most of the methods return immutable empty collections if there isn't
 *  any real data.
 *  <p>
 *  This class attempts to maintain the order that annotations appear in the classfile,
 *  which should in turn mimic the order that they appear in the sourcefile. If you mix
 *  runtime-visible and runtime-invisible annotations on the same target, however,
 *  there are no guarantees about which will appear first.
 */
public class AnnotationParser
{
    private final static String ATTR_STANDARD_VISIBLE   = "RuntimeVisibleAnnotations";
    private final static String ATTR_STANDARD_INVISIBLE = "RuntimeInvisibleAnnotations";
    private final static String ATTR_PARAM_VISIBLE      = "RuntimeVisibleParameterAnnotations";
    private final static String ATTR_PARAM_INVISIBLE    = "RuntimeInvisibleParameterAnnotations";


//----------------------------------------------------------------------------
//  Instance variables and constructors
//----------------------------------------------------------------------------

    // initialized by ctor

    private JavaClass classFile;
    private ConstantPool cp;

    // - lookup tables for annotations; these get filled the first time a given
    //   annotation target (class, method, parameter, field) is requested
    // - all Map<String,Annotation> are keyed by the annotation classname, and are
    //   LinkedHashMap (so that the order of retrieval should match the order that
    //   the annotations appear on the target, ex visible/invisible split)

    private Map<String,Annotation> classAnnotations;
    private Map<Method,Map<String,Annotation>> methodAnnotations;
    private Map<Method,List<Map<String,Annotation>>> parameterAnnotations;
    private Map<Field,Map<String,Annotation>> fieldAnnotations;


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
     *  be given just the parser instance.
     */
    public JavaClass getParsedClass()
    {
        return classFile;
    }


    /**
     *  Returns all annotations on a class, visible and invisiable. If the class has
     *  no annotations, returns an empty collection.
     */
    public Collection<Annotation> getClassAnnotations()
    {
        lazyBuildClassAnnotationMap();
        List<Annotation> result = new ArrayList<Annotation>(classAnnotations.size());
        result.addAll(classAnnotations.values());
        return result;
    }


    /**
     *  Returns the runtime-visible annotations for this class, an empty collection if
     *  there aren't any such annotations.
     */
    public Collection<Annotation> getClassVisibleAnnotations()
    {
        lazyBuildClassAnnotationMap();
        List<Annotation> result = new ArrayList<Annotation>(classAnnotations.size());
        for (Annotation anno : classAnnotations.values())
        {
            if (anno.getRetentionPolicy() == RetentionPolicy.RUNTIME)
                result.add(anno);
        }
        return result;
    }


    /**
     *  Returns the runtime-invisible annotations for this class, an empty collection if
     *  there aren't any such annotations.
     */
    public Collection<Annotation> getClassInvisibleAnnotations()
    {
        lazyBuildClassAnnotationMap();
        List<Annotation> result = new ArrayList<Annotation>(classAnnotations.size());
        for (Annotation anno : classAnnotations.values())
        {
            if (anno.getRetentionPolicy() == RetentionPolicy.CLASS)
                result.add(anno);
        }
        return result;
    }


    /**
     *  Retrieves a single class-level annotation (visible or invisible) by its classname.
     *  Returns null if the annotation is not present.
     */
    public Annotation getClassAnnotation(String annoClass)
    {
        lazyBuildClassAnnotationMap();
        return classAnnotations.get(annoClass);
    }


    /**
     *  Returns the methods that are marked with the specified annotation (visible
     *  or invisible), an empty collection if there aren't any.
     *  <p>
     *  This method operates on <code>declared</code> methods, including private
     *  and protected methods. It will not return any methods declared by the
     *  superclass.
     *  <p>
     *  The order of the returned methods is not guaranteed, and may differ between
     *  invocations.
     */
    public Collection<Method> getAnnotatedMethods(String className)
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
     *  Returns the methods that are marked with the specified annotation (visible
     *  or invisible), and whose parameters match the name-value pairs passed in
     *  the provided map. Returns an empty list if there are no methods that match.
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
    public Collection<Method> getAnnotatedMethods(String className, Map<String,Object> filter)
    {
        List<Method> result = new ArrayList<Method>();

        for (Method method : getAnnotatedMethods(className))
        {
            inner:
            {
                Annotation anno = methodAnnotations.get(method).get(className);
                for (Map.Entry<String,Object> param : filter.entrySet())
                {
                    String filterName = param.getKey();
                    Object filterValue = param.getValue();
                    ParamValue annoParam = anno.getParam(filterName);
                    if ((annoParam == null) || !annoParam.valueEquals(filterValue))
                        break inner;
                }
                result.add(method);
            }
        }

        return result;
    }


    /**
     *  Returns all annotations for the passed method, an empty collection if there are
     *  no annotations or if the method does not belong to the class associated with
     *  this parser.
     */
    public Collection<Annotation> getMethodAnnotations(Method method)
    {
        lazyBuildMethodAnnotationMap();
        Map<String,Annotation> annos = methodAnnotations.get(method);
        return (annos == null)
             ? Collections.<Annotation>emptyList()
             : new ArrayList<Annotation>(annos.values());
    }


    /**
     *  Returns a single annotation from the passed method, <code>null</code> if the
     *  method does not have that annotation.
     */
    public Annotation getMethodAnnotation(Method method, String annoClass)
    {
        lazyBuildMethodAnnotationMap();
        Map<String,Annotation> annos = methodAnnotations.get(method);
        return (annos != null) ? annos.get(annoClass) : null;
    }


    /**
     *  Returns the annotations for a method's parameters. The result is a list, each
     *  element of which corresponds to a parameter by position; if the method has no
     *  parameters, this list will be empty. Each element in the returned list is a
     *  list of the annotations for that parameter.
     */
    public List<Map<String,Annotation>> getParameterAnnotatons(Method method)
    {
        lazyBuildParameterAnnotations();
        return parameterAnnotations.get(method);
    }


    /**
     *  Returns a single annotation from the specified method parameter,
     *  <code>null</code> if the parameter does not have that annotation.
     *  Parameter indices are numbered from 0.
     */
    public Annotation getParameterAnnotation(Method method, int paramIndex, String annoClass)
    {
        lazyBuildParameterAnnotations();
        List<Map<String,Annotation>> allAnnos = parameterAnnotations.get(method);
        if ((allAnnos == null) || (allAnnos.size() <= paramIndex))
        {
            throw new IllegalArgumentException("invalid method/parameter: " + method.getName() + "/" + paramIndex);
        }

        Map<String,Annotation> paramAnnos = allAnnos.get(paramIndex);
        return paramAnnos.get(annoClass);
    }


    /**
     *  Returns all annotations for a single field, keyed by the annotation
     *  classname. Returns an empty map if the field is not annotated.
     */
    public Map<String,Annotation> getFieldAnnotations(Field field)
    {
        lazyBuildFieldAnnotationMap();

        Map<String,Annotation> annos = fieldAnnotations.get(field);
        if (annos == null)
            annos = Collections.emptyMap();

        return annos;
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    /**
     *  Builds the list of annotations for the class as a whole.
     */
    private void lazyBuildClassAnnotationMap()
    {
        if (classAnnotations != null)
            return;

        classAnnotations = new LinkedHashMap<String,Annotation>();
        for (Annotation anno :  parseStandardAnnotations(classFile.getAttributes()))
        {
            classAnnotations.put(anno.getClassName(), anno);
        }
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
            Map<String,Annotation> annoMap = new LinkedHashMap<String,Annotation>();
            methodAnnotations.put(method, annoMap);
            for (Annotation anno : parseStandardAnnotations(method.getAttributes()))
            {
                annoMap.put(anno.getClassName(), anno);
            }
        }
    }


    /**
     *  Builds the map of methods to their parameter annotations.
     */
    private void lazyBuildParameterAnnotations()
    {
        if (parameterAnnotations != null)
            return;

        parameterAnnotations = new IdentityHashMap<Method,List<Map<String,Annotation>>>();
        for (Method method : classFile.getMethods())
        {
            List<Map<String,Annotation>> annos = parseParameterAnnotations(method);
            parameterAnnotations.put(method, annos);
        }
    }

    /**
     *  Builds the map of member variables to their annotations.
     */
    private void lazyBuildFieldAnnotationMap()
    {
        if (fieldAnnotations != null)
            return;

        fieldAnnotations = new IdentityHashMap<Field,Map<String,Annotation>>();
        for (Field field : classFile.getFields())
        {
            Map<String,Annotation> annoMap = new LinkedHashMap<String,Annotation>();
            fieldAnnotations.put(field, annoMap);
            for (Annotation anno : parseStandardAnnotations(field.getAttributes()))
            {
                annoMap.put(anno.getClassName(), anno);
            }
        }
    }


    /**
     *  Top-level parsing code for standard annotations: given the attributes that
     *  belong to a class / method / field, finds those that are hold annotations
     *  and pulls the annotations out of them. This method combines visible and
     *  invisible annotations; caller is responsible for separating them based on
     *  <code>RetentionPolicy</code>.
     */
    private List<Annotation> parseStandardAnnotations(Attribute[] attrs)
    {
        List<Annotation> result = new ArrayList<Annotation>();
        try
        {
            for (Attribute attr : attrs)
            {
                String name = stringConstant(attr.getNameIndex());
                RetentionPolicy annoPolicy = name.equals(ATTR_STANDARD_INVISIBLE) ? RetentionPolicy.CLASS
                                           : name.equals(ATTR_STANDARD_VISIBLE) ? RetentionPolicy.RUNTIME
                                           : null;
                if (annoPolicy != null)
                {
                    byte[] attrData = ((Unknown)attr).getBytes();
                    DataInputStream in = new DataInputStream(
                                            new ByteArrayInputStream(attrData));
                    result.addAll(parseAnnotationsFromStream(in, annoPolicy));
                }
            }
        }
        catch (Exception ex)
        {
            if (ex instanceof ParseException)
                throw (ParseException)ex;
            throw new ParseException("unable to parse attribute", ex);
        }
        return result;
    }


    /**
     *  Top-level parsing code for parameter annotations. Returns a list-of-lists,
     *  containing an entry for each method parameter. These entries hold the actual
     *  annotations; they may be empty.
     *  <p>
     *  Note: the contained lists will be immutable, however the returned list isn't.
     */
    private List<Map<String,Annotation>> parseParameterAnnotations(Method method)
    {
        List<Map<String,Annotation>> result = new ArrayList<Map<String,Annotation>>();
        for (int ii = 0 ; ii < method.getArgumentTypes().length ; ii++)
        {
            result.add(new LinkedHashMap<String,Annotation>());
        }

        try
        {
            for (Attribute attr : method.getAttributes())
            {
                String name = stringConstant(attr.getNameIndex());
                RetentionPolicy annoPolicy = name.equals(ATTR_PARAM_INVISIBLE) ? RetentionPolicy.CLASS
                                           : name.equals(ATTR_PARAM_VISIBLE)   ? RetentionPolicy.RUNTIME
                                           : null;
                if (annoPolicy != null)
                {
                    byte[] attrData = ((Unknown)attr).getBytes();
                    DataInputStream in = new DataInputStream(
                                            new ByteArrayInputStream(attrData));
                    int numParams = in.readUnsignedByte();
                    for (int ii = 0 ; ii < numParams ; ii++)
                    {
                        Map<String,Annotation> existing = result.get(ii);
                        for (Annotation anno : parseAnnotationsFromStream(in, annoPolicy))
                        {
                            existing.put(anno.getClassName(), anno);
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            if (ex instanceof ParseException)
                throw (ParseException)ex;
            throw new ParseException("unable to parse attribute", ex);
        }
        return result;
    }


    /**
     *  Parses a list of annotations from the passed stream. This is common code for
     *  both standard and parameter annotations.
     */
    private List<Annotation> parseAnnotationsFromStream(DataInputStream in, RetentionPolicy policy)
    throws IOException
    {
        List<Annotation> result = new ArrayList<Annotation>();
        int numAnnos = in.readUnsignedShort();
        for (int ii = 0 ; ii < numAnnos ; ii++)
        {
            result.add(parseAnnotationFromStream(in, policy));
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
            case '@':
                Annotation nested = parseAnnotationFromStream(in, RetentionPolicy.RUNTIME);
                return new AnnotationValue(nested);
            default :
                throw new ParseException("unsupported value tag: " + tag);
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
