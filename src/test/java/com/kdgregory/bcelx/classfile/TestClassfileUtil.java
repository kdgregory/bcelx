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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import net.sf.kdgcommons.bean.Introspection;
import net.sf.kdgcommons.bean.IntrospectionCache;

import com.kdgregory.bcelx.AbstractTest;
import com.kdgregory.bcelx.SupportObjects;


public class TestClassfileUtil
extends AbstractTest
{
//----------------------------------------------------------------------------
//  A basic test class that has fields and methods
//----------------------------------------------------------------------------

    public static class SimpleClass
    {
        private String field1;
        protected int field2;

        public void foo()
        {
            field2 = 12;
        }

        public int bar(Integer val)
        {
            field2 = val.intValue();
            return field2;
        }

        public String baz(String val)
        {
            field1 = String.valueOf(val);
            return field1;
        }

        public String baz(Integer val)
        {
            field1 = String.valueOf(val);
            return field1;
        }

        public String baz(int val)
        {
            field1 = String.valueOf(val);
            return field1;
        }
    }


//----------------------------------------------------------------------------
//  A test class for the getReferencedClasses() test
//  -- note that we rely on the public annotations from TestAnnotationParser
//----------------------------------------------------------------------------

    @SupportObjects.RuntimeMarkerAnnotation
    @SupportObjects.AnnotationValuedAnnotation(@SupportObjects.StringValuedAnnotation("foo"))
    public static class ClassWithLotsOfReferences
    implements Serializable
    {
        private static final long serialVersionUID = 1L;

        // no references; has to be pulled from Method
        protected Introspection foo;

        @SupportObjects.FieldAnnotation1("foo")
        protected IntrospectionCache cache = null;

        @SupportObjects.NumericValuedAnnotation(12)
        public long transform(@SupportObjects.ParamAnnotation1("foo") String value)
        throws ArithmeticException
        {
            return new BigInteger(value).longValue();
        }

        // verifies that we retrieve referenced classes from return type
        public Number transform2(String value)
        {
            return new BigInteger(value);
        }

        // verifies that we retrieve referenced classes from parameters
        public void ignore(BigDecimal value)
        {
            // does nothing
        }

        // and properly handle an array of objects
        public Double[] getArray()
        {
            return new Double[1];
        }

        // this should put an array reference in CONSTANT_Class
        public void varargMethod(StringBuilder... builders)
        {
            for (Object builder : builders)
            {
                builder.getClass(); // a no-op to stop Eclipse warnings
            }
        }
    }


//----------------------------------------------------------------------------
//  Test Cases
//----------------------------------------------------------------------------

    @Test
    public void testGetMethod() throws Exception
    {
        JavaClass testClass = loadNestedClass(SimpleClass.class);

        Method m1 = ClassfileUtil.getMethod(testClass, "foo");
        assertEquals("method without params", "foo", m1.getName());
        assertEquals("method without params", "()V", m1.getSignature());

        Method m2 = ClassfileUtil.getMethod(testClass, "bar", Integer.class);
        assertEquals("method with params", "bar", m2.getName());
        assertEquals("method with params", "(Ljava/lang/Integer;)I", m2.getSignature());

        Method m3 = ClassfileUtil.getMethod(testClass, "baz", String.class);
        assertEquals("overloaded method, variant 1", "(Ljava/lang/String;)Ljava/lang/String;", m3.getSignature());

        Method m4 = ClassfileUtil.getMethod(testClass, "baz", Integer.class);
        assertEquals("overloaded method, variant 2", "(Ljava/lang/Integer;)Ljava/lang/String;", m4.getSignature());

        Method m5 = ClassfileUtil.getMethod(testClass, "baz", Integer.TYPE);
        assertEquals("overloaded method, variant 2", "(I)Ljava/lang/String;", m5.getSignature());
    }


    @Test
    public void testExtractReferencedClasses() throws Exception
    {
        JavaClass testClass = loadNestedClass(ClassWithLotsOfReferences.class);
        Set<String> references = ClassfileUtil.extractReferencedClasses(testClass);

        assertTrue("self",                  references.contains("com.kdgregory.bcelx.classfile.TestClassfileUtil.ClassWithLotsOfReferences"));
        assertTrue("superclass",            references.contains("java.lang.Object"));
        assertTrue("implemented interface", references.contains("java.io.Serializable"));
        assertTrue("unreferenced field",    references.contains("net.sf.kdgcommons.bean.Introspection"));
        assertTrue("assigned field",        references.contains("net.sf.kdgcommons.bean.IntrospectionCache"));
        assertTrue("return value",          references.contains("java.lang.Number"));
        assertTrue("parameter",             references.contains("java.math.BigDecimal"));
        assertTrue("class annotation",      references.contains("com.kdgregory.bcelx.SupportObjects.RuntimeMarkerAnnotation"));
        assertTrue("method annotation",     references.contains("com.kdgregory.bcelx.SupportObjects.NumericValuedAnnotation"));
        assertTrue("field annotation",      references.contains("com.kdgregory.bcelx.SupportObjects.FieldAnnotation1"));
        assertTrue("parameter annotation",  references.contains("com.kdgregory.bcelx.SupportObjects.ParamAnnotation1"));
        assertTrue("nested annotation",     references.contains("com.kdgregory.bcelx.SupportObjects.StringValuedAnnotation"));

        // regression test: some classes were not getting converted
        for (String name : references)
        {
            if (name.startsWith("[") || name.endsWith(";"))
                fail("unconverted classname: " + name);
        }
    }
}