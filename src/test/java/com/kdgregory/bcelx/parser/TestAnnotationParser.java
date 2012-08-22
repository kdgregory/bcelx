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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;

import com.kdgregory.bcelx.AbstractTest;
import com.kdgregory.bcelx.classfile.Annotation;


public class TestAnnotationParser
extends AbstractTest
{
//----------------------------------------------------------------------------
//  Support Code
//----------------------------------------------------------------------------

    private static Method getMethod(AnnotationParser ap, String methodName)
    {
        for (Method method : ap.getParsedClass().getMethods())
        {
            if (method.getName().equals(methodName))
            {
                return method;
            }
        }
        fail("unable to extract method: " + methodName);
        return null; // no, we won't get here, but javac doesn't know that
    }


//----------------------------------------------------------------------------
//  Some classes to be used as annotation values
//----------------------------------------------------------------------------

    public enum MyEnum { RED, GREEN, BLUE }

    public static class MyLocalClass
    { /* nothing here */ }


//----------------------------------------------------------------------------
//  The annotations that we'll be looking for
//----------------------------------------------------------------------------

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.CLASS)
    public @interface ClassMarkerAnnotation
    {
        // nothing here
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RuntimeMarkerAnnotation
    {
        // nothing here either
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface StringValuedAnnotation
    {
        String value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NumericValuedAnnotation
    {
        int value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ClassValuedAnnotation
    {
        Class<?> value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EnumValuedAnnotation
    {
        MyEnum value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface StringArrayValuedAnnotation
    {
        String[] value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IntArrayValuedAnnotation
    {
        int[] value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MultiValueAnnotation
    {
        String name();
        int quantity();
    }


    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ParamAnnotation1
    {
        String value();
    }


    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.CLASS)
    public @interface ParamAnnotation2
    {
        // nothing here
    }


    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FieldAnnotation1
    {
        String value();
    }


    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.CLASS)
    public @interface FieldAnnotation2
    {
        // nothing here
    }


//----------------------------------------------------------------------------
//  And classes that use those annotations
//----------------------------------------------------------------------------

    @RuntimeMarkerAnnotation
    @ClassMarkerAnnotation
    public static class MarkedClass
    { /* nothing here */ }


    @StringValuedAnnotation("foo")
    public static class DefaultStringAnnotatedClass
    { /* nothing here */ }


    @NumericValuedAnnotation(12)
    public static class DefaultNumericAnnotatedClass
    { /* nothing here */ }


    @ClassValuedAnnotation(String.class)
    public static class DefaultClassAnnotatedClass
    { /* nothing here */ }


    @ClassValuedAnnotation(MyLocalClass.class)
    public static class DefaultInnerClassAnnotatedClass
    { /* nothing here */ }


    @EnumValuedAnnotation(MyEnum.RED)
    public static class DefaultEnumAnnotatedClass
    { /* nothing here */ }


    @StringArrayValuedAnnotation({"foo", "bar"})
    public static class DefaultStringArrayAnnotatedClass
    { /* nothing here */ }


    @IntArrayValuedAnnotation({1, 10, 100})
    public static class DefaultNumericArrayAnnotatedClass
    { /* nothing here */ }


    @MultiValueAnnotation(name="foo", quantity=12)
    public static class TwoParamAnnotatedClass
    { /* nothing here */ }


    @StringValuedAnnotation("foo")
    @NumericValuedAnnotation(12)
    public static class MultiplyAnnotatedClass
    { /* nothing here */ }


    public static class ClassWithAnnotatedMethods
    {
        @StringValuedAnnotation("foo")
        public void myMethod() { /* no body */ }

        @StringValuedAnnotation("bar")
        @EnumValuedAnnotation(MyEnum.BLUE)
        public void myOtherMethod() { /* no body */ }

        @MultiValueAnnotation(name="foo", quantity=12)
        public void aThirdMethod() { /* no body */ }

        @MultiValueAnnotation(name="foo", quantity=14)
        public void andAFourth() { /* no body */ }

        @SuppressWarnings("unused")
        @NumericValuedAnnotation(12)
        private void privateMethod() { /* no body */ }
    }


    public static class ClassWithAnnotatedParameters
    {
        public String foo(
                @ParamAnnotation1("argle") String argle,
                @ParamAnnotation1("bargle") @ParamAnnotation2 String bargle,
                String wargle)
        {
            return argle + bargle + wargle;
        }
    }


    public static class ClassWithAnnotatedFields
    {
        @FieldAnnotation1("foo")
        protected String foo;

        @FieldAnnotation1("bar") @FieldAnnotation2
        protected int bar;

        protected String baz;
    }


//----------------------------------------------------------------------------
//  Testcases
//----------------------------------------------------------------------------

    @Test
    public void testClassMarkerAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(MarkedClass.class));

        Collection<Annotation> annos1 = ap.getClassAnnotations();
        assertEquals("count of total class annotations", 2, annos1.size());

        // can't guarantee order that annotation attributes will appear, so must
        // revert to tracking their names
        Set<String> anno1Values = new HashSet<String>();
        for (Annotation anno : annos1)
        {
            anno1Values.add(anno.toString());
        }
        assertTrue("first class annotation",
                   anno1Values.contains("@TestAnnotationParser.RuntimeMarkerAnnotation"));
        assertTrue("second class annotation",
                   anno1Values.contains("@TestAnnotationParser.ClassMarkerAnnotation"));


        Collection<Annotation> annos2 = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos2.size());

        Annotation anno2 = annos2.iterator().next();
        assertEquals("value of runtime visible annotation",
                     "@TestAnnotationParser.RuntimeMarkerAnnotation",
                     anno2.toString());
        assertEquals("fq classname of runtime visible annotation",
                     "com.kdgregory.bcelx.parser.TestAnnotationParser.RuntimeMarkerAnnotation",
                     anno2.getClassName());
        assertEquals("count of parameters",
                     0, anno2.getParams().size());


        Collection<Annotation> annos3 = ap.getClassInvisibleAnnotations();
        assertEquals("count of runtime invisible annotations", 1, annos3.size());

        Annotation anno3 = annos3.iterator().next();
        assertEquals("value of runtime invisible annotation",
                     "@TestAnnotationParser.ClassMarkerAnnotation",
                     anno3.toString());
        assertEquals("fq classname of runtime visible annotation",
                     "com.kdgregory.bcelx.parser.TestAnnotationParser.ClassMarkerAnnotation",
                     anno3.getClassName());
        assertEquals("count of parameters",
                     0, anno3.getParams().size());


        assertEquals("retrieve visible annotation by name",
                     "@TestAnnotationParser.RuntimeMarkerAnnotation",
                     ap.getClassAnnotation("com.kdgregory.bcelx.parser.TestAnnotationParser.RuntimeMarkerAnnotation").toString());
        assertEquals("retrieve invisible annotation by name",
                     "@TestAnnotationParser.ClassMarkerAnnotation",
                     ap.getClassAnnotation("com.kdgregory.bcelx.parser.TestAnnotationParser.ClassMarkerAnnotation").toString());
    }


    @Test
    public void testStringValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(DefaultStringAnnotatedClass.class));

        Collection<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.iterator().next();
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.StringValuedAnnotation(\"foo\")",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getValue();
        assertEquals("value as string",
                     "foo",
                     annoValue.asScalar());
        assertTrue("equality to correct value",
                     annoValue.valueEquals("foo"));
        assertFalse("equality to bogus value",
                     annoValue.valueEquals("bar"));
    }


    @Test
    public void testNumberValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(DefaultNumericAnnotatedClass.class));

        Collection<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.iterator().next();
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.NumericValuedAnnotation(12)",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getValue();
        assertEquals("value as number",
                     new Integer(12),
                     annoValue.asScalar());
        assertTrue("equality to correct value",
                    annoValue.valueEquals(new Integer(12)));
        assertFalse("equality to bogus value",
                    annoValue.valueEquals(new Integer(13)));
        assertFalse("equality to correct value, incorrect type",
                    annoValue.valueEquals(new Short((short)12)));
    }


    @Test
    public void testClassValuedAnnotations() throws Exception
    {
        AnnotationParser ap1 = new AnnotationParser(loadNestedClass(DefaultClassAnnotatedClass.class));

        Collection<Annotation> annos1 = ap1.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos1.size());

        Annotation anno1 = annos1.iterator().next();
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.ClassValuedAnnotation(java.lang.String.class)",
                     anno1.toString());
        assertEquals("simpleName of annotation",
                     "@TestAnnotationParser.ClassValuedAnnotation",
                     anno1.getName());
        assertEquals("count of parameters",
                     1,
                     anno1.getParams().size());

        Annotation.ParamValue anno1Value = anno1.getValue();
        assertEquals("value as class",
                     String.class,
                     anno1Value.asClass());
        assertTrue("equality to correct value",
                    anno1Value.valueEquals(String.class));
        assertFalse("equality to incorrect value",
                    anno1Value.valueEquals(Number.class));
        assertTrue("equality to correct value, as String",
                    anno1Value.valueEquals("java.lang.String.class"));
        assertFalse("equality to incorrect value, as String",
                    anno1Value.valueEquals("java.lang.String"));


        AnnotationParser ap2 = new AnnotationParser(loadNestedClass(DefaultInnerClassAnnotatedClass.class));

        Collection<Annotation> annos2 = ap2.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos2.size());

        Annotation anno2 = annos2.iterator().next();
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.ClassValuedAnnotation(com.kdgregory.bcelx.parser.TestAnnotationParser.MyLocalClass.class)",
                     anno2.toString());
        assertEquals("count of parameters",
                     1,
                     anno1.getParams().size());

        Annotation.ParamValue anno2Value = anno2.getValue();
        assertEquals("value as class",
                     MyLocalClass.class,
                     anno2Value.asClass());
        assertTrue("equality to correct value",
                    anno2Value.valueEquals(MyLocalClass.class));
        assertFalse("equality to incorrect value",
                    anno2Value.valueEquals(Number.class));
        assertTrue("equality to correct value, as String",
                    anno2Value.valueEquals("com.kdgregory.bcelx.parser.TestAnnotationParser.MyLocalClass.class"));
        assertFalse("equality to incorrect value, as String",
                    anno2Value.valueEquals("java.lang.String.class"));
    }


    @Test
    public void testEnumValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(DefaultEnumAnnotatedClass.class));

        Collection<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.iterator().next();
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.EnumValuedAnnotation(com.kdgregory.bcelx.parser.TestAnnotationParser.MyEnum.RED)",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getValue();
        assertEquals("value as enum",
                     MyEnum.RED,
                     annoValue.asEnum());
        assertTrue("equality to correct value",
                    annoValue.valueEquals(MyEnum.RED));
        assertFalse("equality to incorrect value",
                    annoValue.valueEquals(MyEnum.BLUE));
        assertTrue("equality to correct value, as String",
                    annoValue.valueEquals("com.kdgregory.bcelx.parser.TestAnnotationParser.MyEnum.RED"));
        assertFalse("equality to incorrect value, as String",
                    annoValue.valueEquals("com.kdgregory.bcelx.parser.TestAnnotationParser.MyEnum.BLUE"));
    }


    @Test
    public void testArrayValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(DefaultStringArrayAnnotatedClass.class));

        Collection<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.iterator().next();
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.StringArrayValuedAnnotation({\"foo\",\"bar\"})",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getValue();
        assertEquals("value as List<Object>",
                     Arrays.asList("foo", "bar"),
                     annoValue.asListOfObjects());
        assertTrue("equality to correct value",
                    annoValue.valueEquals(Arrays.asList("foo", "bar")));
        assertFalse("equality to incorrect value",
                    annoValue.valueEquals(Arrays.asList("bar", "foo")));
        assertFalse("equality to incorrect value",
                    annoValue.valueEquals("foo"));
    }


    @Test
    public void testMultiParamAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(TwoParamAnnotatedClass.class));

        Collection<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.iterator().next();
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.MultiValueAnnotation(name=\"foo\", quantity=12)",
                     anno.toString());
        assertEquals("count of parameters",
                     2,
                     anno.getParams().size());
        assertEquals("name as string",
                     "foo",
                     anno.getParam("name").asScalar());
        assertEquals("value as number",
                     new Integer(12),
                     anno.getParam("quantity").asScalar());
    }


    @Test
    public void testMultipleAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(MultiplyAnnotatedClass.class));

        Collection<Annotation> annos = ap.getClassVisibleAnnotations();
        Iterator<Annotation> annoItx = annos.iterator();
        assertEquals("count of runtime visible annotations", 2, annos.size());
        assertEquals("string value of annotation 0",
                     "@TestAnnotationParser.StringValuedAnnotation(\"foo\")",
                     annoItx.next().toString());
        assertEquals("string value of annotation 1",
                     "@TestAnnotationParser.NumericValuedAnnotation(12)",
                     annoItx.next().toString());

        Annotation anno = ap.getClassAnnotation("com.kdgregory.bcelx.parser.TestAnnotationParser.NumericValuedAnnotation");
        assertEquals("value of explicitly retrieved annotation", 12, anno.getValue().asScalar());
    }


    @Test
    public void testRetrieveAnnotatedMethod() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(ClassWithAnnotatedMethods.class));

        Collection<Method> methods1 = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.StringValuedAnnotation");
        assertEquals("number of annotated methods", 2, methods1.size());

        // can't guarantee order of return, so we'll extract name and validate
        Set<String> methodNames = new TreeSet<String>();
        for (Method method : methods1)
            methodNames.add(method.getName());
        assertTrue("annotated methods includes myMethod()",      methodNames.contains("myMethod"));
        assertTrue("annotated methods includes myOtherMethod()", methodNames.contains("myOtherMethod"));


        Collection<Method> methods2 = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.NumericValuedAnnotation");
        assertEquals("number of annotated private methods", 1, methods2.size());
        assertEquals("name of annotated private method", "privateMethod", methods2.iterator().next().getName());
    }


    @Test
    public void testRetrieveAnnotatedMethodWithFilter() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(ClassWithAnnotatedMethods.class));

        // test 1: a one-parameter annotation

        Map<String,Object> filter1 = new HashMap<String,Object>();
        filter1.put("value", "foo");
        Collection<Method> methods1 = ap.getAnnotatedMethods(
                                        "com.kdgregory.bcelx.parser.TestAnnotationParser.StringValuedAnnotation",
                                        filter1);
        assertEquals("count of methods, string(foo)", 1, methods1.size());
        assertEquals("name of method, string(foo)",   "myMethod", methods1.iterator().next().getName());

        // test 2: multiple parameters, only one selected

        Map<String,Object> filter2 = new HashMap<String,Object>();
        filter2.put("name", "foo");
        filter2.put("quantity", Integer.valueOf(12));
        Collection<Method> methods2 = ap.getAnnotatedMethods(
                                        "com.kdgregory.bcelx.parser.TestAnnotationParser.MultiValueAnnotation",
                                        filter2);
        assertEquals("count of methods, (foo,12)", 1, methods2.size());
        assertEquals("name of method, (foo,12)",   "aThirdMethod", methods2.iterator().next().getName());

        // test 3: missing param is a wildcard, selects multiple methods

        Map<String,Object> filter3 = new HashMap<String,Object>();
        filter3.put("name", "foo");
        Collection<Method> methods3 = ap.getAnnotatedMethods(
                                        "com.kdgregory.bcelx.parser.TestAnnotationParser.MultiValueAnnotation",
                                        filter3);

        // we have no guarantee for return order, so need to extract names
        Set<String> methods3Names = new TreeSet<String>();
        for (Method method : methods3)
            methods3Names.add(method.getName());

        assertEquals("count of methods, (foo,?)", 2, methods3Names.size());
        assertTrue("name of method, (foo,?)",        methods3Names.contains("aThirdMethod"));
        assertTrue("name of method, (foo,?)",        methods3Names.contains("andAFourth"));
    }


    @Test
    public void testRetrieveAnnotatedMethodDoesntThrowWithBadParam() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(ClassWithAnnotatedMethods.class));

        Map<String,Object> filter = new HashMap<String,Object>();
        filter.put("argle", "bargle");
        Collection<Method> methods = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.DefaultStringAnnotation", filter);
        assertEquals(0, methods.size());
    }


    @Test
    public void testGetAnnotationsForMethod() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(ClassWithAnnotatedMethods.class));
        Method method = getMethod(ap, "myOtherMethod");

        // all of the method annotations are runtime-visible, so we should get them in order

        Collection<Annotation> annos = ap.getMethodAnnotations(method);
        Iterator<Annotation> annoItx = annos.iterator();
        assertEquals("number of annotations", 2, annos.size());
        assertEquals("annotation param #1",   "bar",       annoItx.next().getValue().asScalar());
        assertEquals("annotation param #2",   MyEnum.BLUE, annoItx.next().getValue().asEnum());

        // and that we can get a particular named annotation

        Annotation anno1 = ap.getMethodAnnotation(method, "com.kdgregory.bcelx.parser.TestAnnotationParser.EnumValuedAnnotation");
        assertEquals("get by class, param", MyEnum.BLUE, anno1.getValue().asEnum());

        // and that we don't blow up if the annotation doesn't exist

        Annotation anno2 = ap.getMethodAnnotation(method, "com.kdgregory.bcelx.parser.TestAnnotationParser.NoSuchAnnotation");
        assertNull("get by nonexistent class", anno2);

        // and finally, that we don't blow up if the method itself doesn't exist

        AnnotationParser ap2 = new AnnotationParser(loadNestedClass(DefaultStringAnnotatedClass.class));
        Collection<Annotation> annos2 = ap2.getMethodAnnotations(method);
        assertEquals("number of annotations, missing method", 0, annos2.size());
    }


    @Test
    public void testGetAnnotationsForParameters() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(ClassWithAnnotatedParameters.class));
        Method method = getMethod(ap, "foo");

        List<Map<String,Annotation>> annos = ap.getParameterAnnotatons(method);
        assertEquals("number of parameters", 3,  annos.size());

        Map<String,Annotation> param1 = annos.get(0);
        assertEquals("param 1 #/annotations",   1, param1.size());
        assertEquals("param 1 anno 1 value",    "argle",
                                                param1.get("com.kdgregory.bcelx.parser.TestAnnotationParser.ParamAnnotation1").getValue().asScalar());

        Map<String,Annotation> param2 = annos.get(1);
        assertEquals("param 2 #/annotations",   2, param2.size());
        assertEquals("param 2 anno 1 value",    "bargle",
                                                param2.get("com.kdgregory.bcelx.parser.TestAnnotationParser.ParamAnnotation1").getValue().asScalar());
        assertNotNull("param 2 anno 2 exists ", param2.get("com.kdgregory.bcelx.parser.TestAnnotationParser.ParamAnnotation2"));

        Map<String,Annotation> param3 = annos.get(2);
        assertEquals("param 3 #/annotations",   0, param3.size());

        // check the explicit retrieval method

        Annotation param2anno1 = ap.getParameterAnnotation(method, 1, "com.kdgregory.bcelx.parser.TestAnnotationParser.ParamAnnotation1");
        assertSame("retrieved annotation by position/class", param2.get("com.kdgregory.bcelx.parser.TestAnnotationParser.ParamAnnotation1"), param2anno1);
    }


    @Test
    public void testGetAnnotationsForFields() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadNestedClass(ClassWithAnnotatedFields.class));

        for (Field field : ap.getParsedClass().getFields())
        {
            Map<String,Annotation> annos = ap.getFieldAnnotations(field);
            if (field.getName().equals("foo"))
            {
                assertEquals("field 'foo', #/annotations", 1, annos.size());
                assertEquals("field 'foo', anno 1 value",  "foo",
                                                           annos.get("com.kdgregory.bcelx.parser.TestAnnotationParser.FieldAnnotation1").getValue().asScalar());
            }
            else if (field.getName().equals("bar"))
            {
                assertEquals("field 'bar', #/annotations",  2, annos.size());
                assertEquals("field 'foo', anno 1 value",   "bar",
                                                            annos.get("com.kdgregory.bcelx.parser.TestAnnotationParser.FieldAnnotation1").getValue().asScalar());
                assertNotNull("field 'foo', anno 2 exists", annos.get("com.kdgregory.bcelx.parser.TestAnnotationParser.FieldAnnotation2"));
            }
            else if (field.getName().equals("baz"))
            {
                assertEquals("field 'baz', #/annotations", 0, annos.size());
            }
        }
    }
}