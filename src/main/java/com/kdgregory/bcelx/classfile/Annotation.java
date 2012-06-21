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
        this.className = fqClassname;
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
         */
        @Override
        public String toString()
        {
            throw new UnreachableCodeException("this method is defined by subclass");
        }


        /**
         *  Returns <code>STRING</code> and <code>NUMBER</code> parameter values.
         */
        public Object getScalar()
        {
            throw new UnsupportedOperationException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Returns the instance for a <code>CLASS</code> value.
         *  <p>
         *  Note that the class must be available on the classpath, and will be
         *  loaded if necessary.
         */
        public Class<?> getKlass()
        throws ClassNotFoundException
        {
            throw new UnsupportedOperationException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Returns the instance for an <code>ENUM</code> value.
         *  <p>
         *  Note that the enum class must be available on the classpath, and will be
         *  loaded if necessary.
         */
        public Enum<?> getEnum()
        throws ClassNotFoundException
        {
            throw new UnsupportedOperationException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Returns a list of values for an <code>ARRAY</code> value. Note that
         *  the list contents are themselves <code>ParamValue</code> instances, so
         *  you have type information and can retrieve as a string. If you know
         *  the contained object type, you can call {@link#getArrayAsObjects},
         *  which returns the actual values held in the array.
         */
        public List<ParamValue> getArray()
        {
            throw new UnsupportedOperationException("this method not appropriate for " + getType() + " values");
        }


        /**
         *  Converts an array parameter into a list of objects. Any enum or class
         *  values will be loaded during this process, and must appear on the
         *  classpath.
         */
        public List<Object> getArrayAsObjects()
        throws ClassNotFoundException
        {
            throw new UnsupportedOperationException("this method not appropriate for " + getType() + " values");
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
        public Object getScalar()
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
        public Class<?> getKlass()
        throws ClassNotFoundException
        {
            return Class.forName(className);
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
        public Enum<?> getEnum()
        throws ClassNotFoundException
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
                    StringBuilderUtil.appendUnless(sb, "{", ", ");
                    sb.append(arrayValue.toString());
                }
                sb.append("}");
                stringValue = sb.toString();
            }
            return stringValue;
        }

        @Override
        public List<ParamValue> getArray()
        {
            // need to do a deep copy so that we don't expose mutable state
            // for multi-dimensional arrays
            List<ParamValue> ret = new ArrayList<ParamValue>(values.size());
            for (ParamValue value : values)
            {
                if (value.getType() == ParamType.ARRAY)
                    ret.add(new ArrayValue(value.getArray()));
                else
                    ret.add(value);
            }
            return ret;
        }

        @Override
        public List<Object> getArrayAsObjects()
        throws ClassNotFoundException
        {
            List<Object> ret = new ArrayList<Object>(values.size());
            for (ParamValue value : values)
            {
                switch (value.getType())
                {
                    case STRING :
                    case NUMBER :
                        ret.add(value.getScalar());
                        break;
                    case CLASS :
                        ret.add(value.getKlass());
                        break;
                    case ENUM :
                        ret.add(value.getEnum());
                        break;
                    case ARRAY :
                        ret.add(value.getArrayAsObjects());
                        break;
                    default :
                        throw new UnreachableCodeException("unexpected param type: " + value.getType());
                }
            }
            return ret;
        }

    }
}
