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
import java.util.List;
import java.util.Map;

public abstract class Annotation
{

    /**
     *  Returns the annotation's name, formatted "@SimpleName".
     */
    public abstract String getName();
    
    
    /**
     *  Returns the annotation's fully-qualified classname.
     */
    public abstract String getClassName();
    
    
    /**
     *  Returns the retention policy of the annotation. Limited to policies
     *  <code>CLASS</code> and <code>RUNTIME</code>.
     */
    public RetentionPolicy getRetention()
    {
        return null;
    }
    
    
    /**
     *  Returns a map of the annotation's parameters, keyed by parameter name.
     */
    public abstract Map<String,ParamValue> getParams();
    
    
    /**
     *  Returns a string representation of the annotation. This representation
     *  will match what would you see in the source code using the annotation,
     *  with the caveat that default values are explicitly shown. 
     */
    @Override
    public abstract String toString();
    
    
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
     *  Holds a single parameter value. There are multiple ways that you can
     *  retrieve this value. 
     */
    public static class ParamValue
    {
        /**
         *  Returns the type of this parameter. This may be used by calling code
         *  to call a specific <code>getXXX</code> method.
         */
        public ParamType getType()
        {
            return ParamType.STRING;
        }
        
        
        /**
         *  Returns a string representation of this parameter. The format of this
         *  string follows the form that you'd declare the parameter in a source
         *  file. Specifically:
         */
        @Override
        public String toString() 
        {
            return "";
        }
        
        
        /**
         *  Returns <code>STRING</code> and <code>NUMBER</code> parameter values.
         */
        public Object getScalar()
        {
            return null;
        }
        
        
        /**
         *  Returns the actual class instance for a <code>CLASS</code> value.
         *  Note that the class must be available on the classpath, and will be
         *  loaded if necessary.
         */
        public Class<?> getKlass()
        {
            return null;
        }
        
        
        /**
         *  Returns the instance for an <code>ENUM</code> value. Note that the
         *  enum class must be available on the classpath.
         */
        public Enum<?> getEnum()
        {
            return null;
        }
        
        
        /**
         *  Returns a list of values for an <code>ARRAY</code> value. Note that
         *  the list contents are themselves <code>ParamValue</code> instances,
         *  so you can process recursively.
         */
        public List<ParamValue> getArray()
        {
            return null;
        }
    }
}
