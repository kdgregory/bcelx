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

import net.sf.kdgcommons.bean.Introspection;
import net.sf.kdgcommons.bean.IntrospectionCache;

import com.kdgregory.bcelx.AbstractTest;
import com.kdgregory.bcelx.parser.TestAnnotationParser.FieldAnnotation1;
import com.kdgregory.bcelx.parser.TestAnnotationParser.ParamAnnotation1;
import com.kdgregory.bcelx.parser.TestAnnotationParser.RuntimeMarkerAnnotation;
import com.kdgregory.bcelx.parser.TestAnnotationParser.StringValuedAnnotation;


public class TestClassfileUtil
extends AbstractTest
{
//----------------------------------------------------------------------------
//  A test class for the getReferencedClasses() test
//  -- note that we rely on the public annotations from TestAnnotationParser
//----------------------------------------------------------------------------

    @RuntimeMarkerAnnotation
    public static class ClassWithLotsOfReferences
    implements Serializable
    {
        private static final long serialVersionUID = 1L;

        // no references; has to be pulled from Method
        protected Introspection foo;

        // this will have a  CONSTANT_Fieldref_info
        @FieldAnnotation1("foo")
        protected IntrospectionCache cache = null;

        @StringValuedAnnotation("bar")
        public long transform(@ParamAnnotation1("foo") String value)
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
    }


//----------------------------------------------------------------------------
//  Test Cases
//----------------------------------------------------------------------------

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
        assertTrue("class annotation",      references.contains("com.kdgregory.bcelx.parser.TestAnnotationParser.RuntimeMarkerAnnotation"));
        assertTrue("method annotation",     references.contains("com.kdgregory.bcelx.parser.TestAnnotationParser.StringValuedAnnotation"));
        assertTrue("field annotation",      references.contains("com.kdgregory.bcelx.parser.TestAnnotationParser.FieldAnnotation1"));
        assertTrue("parameter annotation",  references.contains("com.kdgregory.bcelx.parser.TestAnnotationParser.ParamAnnotation1"));
    }

}