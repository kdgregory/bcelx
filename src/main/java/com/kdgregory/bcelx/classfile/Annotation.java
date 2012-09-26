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

package com.kdgregory.bcelx.classfile;

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.kdgcommons.lang.StringBuilderUtil;
import net.sf.kdgcommons.lang.StringUtil;
import net.sf.kdgcommons.lang.UnreachableCodeException;

/**
 *  Holds an annotation, its parameters, and its scope. Instances of this
 *  class are mutable, but they are intended to be read-only after construction.
 */
public class Annotation
{
    private String annoName;
    private String className;
    private RetentionPolicy policy;
    private Map<String,ParamValue> params = new LinkedHashMap<String,ParamValue>();

    private String stringValue; // lazily built

//----------------------------------------------------------------------------
//  Object construction methods
//----------------------------------------------------------------------------

    /**
     *  Creates a new instance. The constructor takes the annotation's classname
     *  and retention policy; any parameters are added later.
     */
    public Annotation(String fqClassname, RetentionPolicy policy)
    {
        String simpleName = StringUtil.extractRightOfLast(fqClassname, ".");
        this.annoName = "@" + simpleName.replace('$', '.');
        this.className = fqClassname.replace('$', '.');
        this.policy = policy;
    }


    public void addParam(String name, ParamValue value)
    {
        params.put(name, value);
    }


//----------------------------------------------------------------------------
//  Public Methods
//----------------------------------------------------------------------------

    /**
     *  Returns the annotation's name, formatted "@SimpleName".
     */
    public String getName()
    {
        return annoName;
    }


    /**
     *  Returns the annotation's fully-qualified classname.
     */
    public String getClassName()
    {
        return className;
    }


    /**
     *  Returns the retention policy of the annotation. Limited to policies
     *  <code>CLASS</code> and <code>RUNTIME</code>.
     */
    public RetentionPolicy getRetentionPolicy()
    {
        return policy;
    }


    /**
     *  Returns a map of the annotation's parameters, keyed by parameter name.
     */
    public Map<String,ParamValue> getParams()
    {
        return Collections.unmodifiableMap(params);
    }


    /**
     *  Returns a single parameter value by name, <code>null</code> if that
     *  parameter does not exist.
     */
    public ParamValue getParam(String name)
    {
        return params.get(name);
    }


    /**
     *  Returns the "value" parameter, <code>null</code> if there's no parameter
     *  with that name.
     */
    public ParamValue getValue()
    {
        return params.get("value");
    }


    /**
     *  Returns a string representation of the annotation. This representation
     *  will match what would you see in the source code using the annotation,
     *  with the caveat that default values are explicitly shown.
     */
    @Override
    public String toString()
    {
        if (stringValue != null)
            return stringValue;

        StringBuilder sb = new StringBuilder(128).append(annoName);
        if ((params.size() == 1) && (params.containsKey("value")))
        {
            sb.append("(").append(params.get("value")).append(")");
        }
        else if (params.size() > 1)
        {
            sb.append("(");
            for (Map.Entry<String,ParamValue> entry : params.entrySet())
            {
                StringBuilderUtil.appendUnless(sb, "(", ", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            sb.append(")");
        }

        stringValue = sb.toString();
        return stringValue;
    }


//----------------------------------------------------------------------------
//  Supporting Classes
//----------------------------------------------------------------------------

    /**
     *  The various types that a parameter value can take. This may be used to
     *  control how calling code processes a value.
     */
    public enum ParamType
    {
        /** A <code>java.lang.String</code>. */
        STRING,

        /** A <code>java.lang.Number</code> subclass, as a primitive wrapper. */
        NUMBER,

        /** A class. */
        CLASS,

        /** An enumeration value. */
        ENUM,

        /** A nested annotation */
        ANNOTATION,

        /** An array of values. */
        ARRAY
    }


    /**
     *  Holds a single parameter value. The caller can either retrieve the actual
     *  value, via one of the <code>getAsXXX()</code> methods, or get the string
     *  representation, which should match the value as it appeared in source.
     */
    public static abstract class ParamValue
    {
        private ParamType type;

        protected ParamValue(ParamType type)
        {
            this.type = type;
        }


        /**
         *  Returns the type of this parameter. This may be used by calling code
         *  to decide on a specific <code>getAsXXX()</code> method.
         */
        public ParamType getType()
        {
            return type;
        }


        /**
         *  Returns a string representation of this parameter. The format of this
         *  string follows the form that you'd declare the parameter in a source
         *  file. Specifically:
         *  <ul>
         *  <li> Strings are delimited by quotes: "foo"
         *  <li> Numeric values are not delimited: 12
         *  <li> Classnames are fully qualified, with inner classes using a dot
         *       rather than a dollar-sign: java.util.Map.Entry
         *  <li> Enums use a fully qualified classname, followed by a dot and the
         *       enum values: com.example.MyEnum.FOO
         *  <li> Arrays are bracketed and comma-separated: {"foo","bar"}
         *  </ul>
         */
        @Override
        public String toString()
        {
            throw new UnreachableCodeException("this method is defined by subclass");
        }


        /**
         *  Compares the passed value to this enum's value:
         *  <ul>
         *  <li> String: passed value must be a <code>String</code>
         *  <li> Number: passed value must be a numeric wrapper type (eg:
         *       <code>Integer</code>); note that class is part of the
         *       <code>equals()</code> contract for wrappers
         *  <li> Class: passed value may be a class instance (this will
         *       load the class if it isn't already) or a <code>String</code>
         *       in the format of {@link #toString} (which won't)
         *  <li> Enum: passed value may be an enum instance (which will
         *       load the enum class if it isn't already), or a <code>String</code>
         *       in the format of {@link #toString} (which won't)
         *  <li> Array: passed value may be an object array or <code>List</code>
         *  </ul>
         */
        public abstract boolean valueEquals(Object obj);


        /**
         *  Returns <code>STRING</code> and <code>NUMBER</code> parameter values.
         */
        public Object asScalar()
        {
            throw new ClassfileException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Returns the instance for a <code>CLASS</code> value.
         *  <p>
         *  Note that the class must be available on the classpath, and will be
         *  loaded if necessary. Will throw if the class cannot be loaded.
         */
        public Class<?> asClass()
        {
            throw new ClassfileException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Returns the instance for an <code>ENUM</code> value.
         *  <p>
         *  Note that the enum class must be available on the classpath, and will be
         *  loaded if necessary. Will throw if the class cannot be loaded.
         */
        public Enum<?> asEnum()
        {
            throw new ClassfileException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Returns the instance for an <code>ANNOTATION</code> value.
         *  <p>
         *  Note that this is a BCELX <code>Annotation</code> object, containing nested
         *  parameters as defined in the classfile; it is not the annotation's actual
         *  class (and therefore does not require the annotation class to be on the
         *  classpath). Note also that, regardless of the original annotation'sretention
         *  policy, the returned value will be marked as <code>RUNTIME</code>.
         */
        public Annotation asAnnotation()
        {
            throw new ClassfileException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Returns a list of values for an <code>ARRAY</code> value. Note that
         *  the list contents are themselves <code>ParamValue</code> instances, so
         *  you have type information and can retrieve as a string. If you know
         *  the contained object type, you can call {@link#getArrayAsObjects},
         *  which returns the actual values held in the array.
         */
        public List<ParamValue> asListOfValues()
        {
            throw new ClassfileException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Converts an array parameter into a list of objects. Any enum or class
         *  values will be loaded during this process, and must appear on the
         *  classpath. Will throw if the class cannot be loaded.
         */
        public List<Object> asListOfObjects()
        {
            throw new ClassfileException("this method not appropriate for " + getType() + " values");
        }
    }


    public static class ScalarValue
    extends ParamValue
    {
        private Object value;
        private String stringValue;

        public ScalarValue(ParamType type, Object value)
        {
            super(type);
            this.value = value;
        }

        @Override
        public String toString()
        {
            if (stringValue == null)
            {
                if (getType() == ParamType.STRING)
                    stringValue = "\"" + value + "\"";
                else
                    stringValue = String.valueOf(value);
            }
            return stringValue;
        }

        @Override
        public boolean valueEquals(Object obj)
        {
            return asScalar().equals(obj);
        }

        @Override
        public Object asScalar()
        {
            return value;
        }
    }


    public static class ClassValue
    extends ParamValue
    {
        private String className;
        private String stringValue;

        public ClassValue(String className)
        {
            super(ParamType.CLASS);
            this.className = className;
        }

        @Override
        public String toString()
        {
            if (stringValue == null)
            {
                stringValue = className.replace('$', '.') + ".class";
            }
            return stringValue;
        }

        @Override
        public boolean valueEquals(Object obj)
        {
            if (obj instanceof Class)
                return asClass().equals(obj);
            else if (obj instanceof String)
                return toString().equals(obj);
            else
                return false;
        }

        @Override
        public Class<?> asClass()
        {
            try
            {
                return Class.forName(className);
            }
            catch (ClassNotFoundException ex)
            {
                throw new ClassfileException("unable to load class: " + className, ex);
            }
        }
    }


    public static class EnumValue
    extends ParamValue
    {
        private String enumClassName;
        private String enumValue;
        private String stringValue;

        public EnumValue(String enumClassName, String enumValue)
        {
            super(ParamType.ENUM);
            this.enumClassName = enumClassName;
            this.enumValue = enumValue;
        }

        @Override
        public String toString()
        {
            if (stringValue == null)
            {
                stringValue = enumClassName.replace('$', '.') + "." + enumValue;
            }
            return stringValue;
        }

        @Override
        public boolean valueEquals(Object obj)
        {
            if (obj instanceof Enum)
                return asEnum().equals(obj);
            else if (obj instanceof String)
                return toString().equals(obj);
            else
                return false;
        }

        @Override
        public Enum<?> asEnum()
        {
            try
            {
                Class<?> enumKlass = Class.forName(enumClassName);

                // I can't figure my way around the parameter bounds of Enum.valueOf(),
                // so this seems the easiest way to make this work
                for (Object value : enumKlass.getEnumConstants())
                {
                    Enum<?> ee = (Enum<?>)value;
                    if (ee.name().equals(enumValue))
                        return ee;
                }

                return null;
            }
            catch (ClassNotFoundException ex)
            {
                throw new ClassfileException("unable to load class: " + enumClassName, ex);
            }
        }
    }


    public static class AnnotationValue
    extends ParamValue
    {
        private Annotation annotation;
        private String stringValue;

        public AnnotationValue(Annotation annotation)
        {
            super(ParamType.ANNOTATION);
            this.annotation = annotation;
        }

        @Override
        public String toString()
        {
            if (stringValue == null)
            {
                stringValue = annotation.toString();
            }
            return stringValue;
        }

        @Override
        public Annotation asAnnotation()
        {
            return annotation;
        }

        @Override
        public boolean valueEquals(Object obj)
        {
            if (obj instanceof Annotation)
                return asAnnotation().equals(obj);
            else if (obj instanceof String)
                return toString().equals(obj);
            else
                return false;
        }
    }


    public static class ArrayValue
    extends ParamValue
    {
        private List<ParamValue> values;
        private String stringValue;

        public ArrayValue(List<ParamValue> values)
        {
            super(ParamType.ARRAY);
            this.values = values;
        }

        @Override
        public String toString()
        {
            if (stringValue == null)
            {
                StringBuilder sb = new StringBuilder(128).append("{");
                for (ParamValue arrayValue : values)
                {
                    StringBuilderUtil.appendUnless(sb, "{", ",");
                    sb.append(arrayValue.toString());
                }
                sb.append("}");
                stringValue = sb.toString();
            }
            return stringValue;
        }

        @Override
        public boolean valueEquals(Object obj)
        {
            if (obj instanceof Object[])
                obj = Arrays.asList((Object[])obj);

            if (!(obj instanceof List))
                return false;

            return asListOfObjects().equals(obj);
        }

        @Override
        public List<ParamValue> asListOfValues()
        {
            // need to do a deep copy so that we don't expose mutable state
            // for multi-dimensional arrays
            List<ParamValue> ret = new ArrayList<ParamValue>(values.size());
            for (ParamValue value : values)
            {
                if (value.getType() == ParamType.ARRAY)
                    ret.add(new ArrayValue(value.asListOfValues()));
                else
                    ret.add(value);
            }
            return ret;
        }

        @Override
        public List<Object> asListOfObjects()
        {
            List<Object> ret = new ArrayList<Object>(values.size());
            for (ParamValue value : values)
            {
                switch (value.getType())
                {
                    case STRING :
                    case NUMBER :
                        ret.add(value.asScalar());
                        break;
                    case CLASS :
                        ret.add(value.asClass());
                        break;
                    case ENUM :
                        ret.add(value.asEnum());
                        break;
                    case ARRAY :
                        ret.add(value.asListOfObjects());
                        break;
                    default :
                        throw new UnreachableCodeException("unexpected param type: " + value.getType());
                }
            }
            return ret;
        }

    }
}
