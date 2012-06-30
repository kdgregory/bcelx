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

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import net.sf.kdgcommons.io.IOUtil;
import net.sf.kdgcommons.lang.StringUtil;

import com.kdgregory.bcelx.classfile.Annotation;


public class TestAnnotationParser
{
//----------------------------------------------------------------------------
//  Support Code
//----------------------------------------------------------------------------

    private static JavaClass loadClass(Class<?> klass)
    throws Exception
    {
        InputStream in = null;
        try
        {
            String className = StringUtil.extractRightOfLast(klass.getName(), ".") + ".class";
            in = klass.getResourceAsStream(className);
            return new ClassParser(in, className).parse();
        }
        finally
        {
            IOUtil.closeQuietly(in);
        }
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
    public @interface DefaultStringAnnotation
    {
        String value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultNumericAnnotation
    {
        int value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultClassAnnotation
    {
        Class<?> value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultEnumAnnotation
    {
        MyEnum value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultStringArrayAnnotation
    {
        String[] value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultIntArrayAnnotation
    {
        int[] value();
    }


    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TwoParamAnnotation
    {
        String name();
        int quantity();
    }


//----------------------------------------------------------------------------
//  And classes that use those annotations
//----------------------------------------------------------------------------

    @RuntimeMarkerAnnotation
    @ClassMarkerAnnotation
    public static class MarkedClass
    { /* nothing here */ }


    @DefaultStringAnnotation("foo")
    public static class DefaultStringAnnotatedClass
    { /* nothing here */ }


    @DefaultNumericAnnotation(12)
    public static class DefaultNumericAnnotatedClass
    { /* nothing here */ }


    @DefaultClassAnnotation(String.class)
    public static class DefaultClassAnnotatedClass
    { /* nothing here */ }


    @DefaultClassAnnotation(MyLocalClass.class)
    public static class DefaultInnerClassAnnotatedClass
    { /* nothing here */ }


    @DefaultEnumAnnotation(MyEnum.RED)
    public static class DefaultEnumAnnotatedClass
    { /* nothing here */ }


    @DefaultStringArrayAnnotation({"foo", "bar"})
    public static class DefaultStringArrayAnnotatedClass
    { /* nothing here */ }


    @DefaultIntArrayAnnotation({1, 10, 100})
    public static class DefaultNumericArrayAnnotatedClass
    { /* nothing here */ }


    @TwoParamAnnotation(name="foo", quantity=12)
    public static class TwoParamAnnotatedClass
    { /* nothing here */ }


    @DefaultStringAnnotation("foo")
    @DefaultNumericAnnotation(12)
    public static class MultiplyAnnotatedClass
    { /* nothing here */ }


    public static class ClassWithAnnotatedMethods
    {
        @DefaultStringAnnotation("foo")
        public void myMethod() { /* no body */ }


        @DefaultStringAnnotation("bar")
        public void myOtherMethod() { /* no body */ }


        @TwoParamAnnotation(name="foo", quantity=12)
        public void aThirdMethod() { /* no body */ }


        @TwoParamAnnotation(name="foo", quantity=14)
        public void andAFourth() { /* no body */ }
    }


//----------------------------------------------------------------------------
//  Testcases
//----------------------------------------------------------------------------

    @Test
    public void testClassMarkerAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(MarkedClass.class));

        List<Annotation> annos1 = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos1.size());

        Annotation anno1 = annos1.get(0);
        assertEquals("value of runtime visible annotation",
                     "@TestAnnotationParser.RuntimeMarkerAnnotation",
                     anno1.toString());
        assertEquals("fq classname of runtime visible annotation",
                     "com.kdgregory.bcelx.parser.TestAnnotationParser.RuntimeMarkerAnnotation",
                     anno1.getClassName());
        assertEquals("count of parameters",
                     0,
                     anno1.getParams().size());

        List<Annotation> annos2 = ap.getClassInvisibleAnnotations();
        assertEquals("count of runtime invisible annotations", 1, annos2.size());

        Annotation anno2 = annos2.get(0);
        assertEquals("value of runtime invisible annotation",
                     "@TestAnnotationParser.ClassMarkerAnnotation",
                     anno2.toString());
        assertEquals("fq classname of runtime visible annotation",
                     "com.kdgregory.bcelx.parser.TestAnnotationParser.ClassMarkerAnnotation",
                     anno2.getClassName());
        assertEquals("count of parameters",
                     0,
                     anno2.getParams().size());

    }


    @Test
    public void testStringValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(DefaultStringAnnotatedClass.class));

        List<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.get(0);
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.DefaultStringAnnotation(\"foo\")",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getParam("value");
        assertEquals("value as string",
                     "foo",
                     annoValue.getScalar());
        assertTrue("equality to correct value",
                     annoValue.valueEquals("foo"));
        assertFalse("equality to bogus value",
                     annoValue.valueEquals("bar"));
    }


    @Test
    public void testNumberValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(DefaultNumericAnnotatedClass.class));

        List<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.get(0);
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.DefaultNumericAnnotation(12)",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getParam("value");
        assertEquals("value as number",
                     new Integer(12),
                     annoValue.getScalar());
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
        AnnotationParser ap1 = new AnnotationParser(loadClass(DefaultClassAnnotatedClass.class));

        List<Annotation> annos1 = ap1.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos1.size());

        Annotation anno1 = annos1.get(0);
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.DefaultClassAnnotation(java.lang.String.class)",
                     anno1.toString());
        assertEquals("count of parameters",
                     1,
                     anno1.getParams().size());

        Annotation.ParamValue anno1Value = anno1.getParam("value");
        assertEquals("value as class",
                     String.class,
                     anno1Value.getKlass());
        assertTrue("equality to correct value",
                    anno1Value.valueEquals(String.class));
        assertFalse("equality to incorrect value",
                    anno1Value.valueEquals(Number.class));
        assertTrue("equality to correct value, as String",
                    anno1Value.valueEquals("java.lang.String.class"));
        assertFalse("equality to incorrect value, as String",
                    anno1Value.valueEquals("java.lang.String"));


        AnnotationParser ap2 = new AnnotationParser(loadClass(DefaultInnerClassAnnotatedClass.class));

        List<Annotation> annos2 = ap2.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos2.size());

        Annotation anno2 = annos2.get(0);
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.DefaultClassAnnotation(com.kdgregory.bcelx.parser.TestAnnotationParser.MyLocalClass.class)",
                     anno2.toString());
        assertEquals("count of parameters",
                     1,
                     anno1.getParams().size());

        Annotation.ParamValue anno2Value = anno2.getParam("value");
        assertEquals("value as class",
                     MyLocalClass.class,
                     anno2Value.getKlass());
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
        AnnotationParser ap = new AnnotationParser(loadClass(DefaultEnumAnnotatedClass.class));

        List<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.get(0);
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.DefaultEnumAnnotation(com.kdgregory.bcelx.parser.TestAnnotationParser.MyEnum.RED)",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getParam("value");
        assertEquals("value as enum",
                     MyEnum.RED,
                     annoValue.getEnum());
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
        AnnotationParser ap = new AnnotationParser(loadClass(DefaultStringArrayAnnotatedClass.class));

        List<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.get(0);
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.DefaultStringArrayAnnotation({\"foo\",\"bar\"})",
                     anno.toString());
        assertEquals("count of parameters",
                     1,
                     anno.getParams().size());

        Annotation.ParamValue annoValue = anno.getParam("value");
        assertEquals("value as List<Object>",
                     Arrays.asList("foo", "bar"),
                     annoValue.getArrayAsObjects());
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
        AnnotationParser ap = new AnnotationParser(loadClass(TwoParamAnnotatedClass.class));

        List<Annotation> annos = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, annos.size());

        Annotation anno = annos.get(0);
        assertEquals("string value of annotation",
                     "@TestAnnotationParser.TwoParamAnnotation(name=\"foo\", quantity=12)",
                     anno.toString());
        assertEquals("count of parameters",
                     2,
                     anno.getParams().size());
        assertEquals("name as string",
                     "foo",
                     anno.getParam("name").getScalar());
        assertEquals("value as number",
                     new Integer(12),
                     anno.getParam("quantity").getScalar());
    }


    @Test
    public void testMultipleAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(MultiplyAnnotatedClass.class));

        List<Annotation> anno = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 2, anno.size());
        assertEquals("string value of annotation 0",
                     "@TestAnnotationParser.DefaultStringAnnotation(\"foo\")",
                     anno.get(0).toString());
        assertEquals("string value of annotation 1",
                     "@TestAnnotationParser.DefaultNumericAnnotation(12)",
                     anno.get(1).toString());
    }


    @Test
    public void testRetrieveAnnotatedMethod() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(ClassWithAnnotatedMethods.class));

        List<Method> methods1 = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.DefaultStringAnnotation");
        assertEquals("number of annotated methods", 2, methods1.size());

        // can't guarantee order of return, so we'll extract name and validate
        Set<String> methodNames = new TreeSet<String>();
        for (Method method : methods1)
            methodNames.add(method.getName());
        assertTrue("annotated methods includes myMethod()",      methodNames.contains("myMethod"));
        assertTrue("annotated methods includes myOtherMethod()", methodNames.contains("myOtherMethod"));
    }


    @Test
    public void testRetrieveAnnotatedMethodWithFilter() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(ClassWithAnnotatedMethods.class));

        // test 1: a one-parameter annotation

        Map<String,Object> filter1 = new HashMap<String,Object>();
        filter1.put("value", "foo");
        List<Method> methods1 = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.DefaultStringAnnotation",
                                                       filter1);
        assertEquals("count of methods, string(foo)", 1, methods1.size());
        assertEquals("name of method, string(foo)",   "myMethod", methods1.get(0).getName());

        // test 2: multiple parameters

        Map<String,Object> filter2 = new HashMap<String,Object>();
        filter2.put("name", "foo");
        filter2.put("quantity", Integer.valueOf(12));
        List<Method> methods2 = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.TwoParamAnnotation",
                                                       filter2);
        assertEquals("count of methods, (foo,12)", 1, methods2.size());
        assertEquals("name of method, (foo,12)",   "aThirdMethod", methods2.get(0).getName());

        // test 3: missing param is a wildcard

        Map<String,Object> filter3 = new HashMap<String,Object>();
        filter3.put("name", "foo");
        List<Method> methods3 = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.TwoParamAnnotation",
                                                       filter3);

        // again, we have no guarantee for return order, so need to extract names
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
        AnnotationParser ap = new AnnotationParser(loadClass(ClassWithAnnotatedMethods.class));

        Map<String,Object> filter = new HashMap<String,Object>();
        filter.put("argle", "bargle");
        List<Method> methods = ap.getAnnotatedMethods("com.kdgregory.bcelx.parser.TestAnnotationParser.DefaultStringAnnotation", filter);
        assertEquals(0, methods.size());
    }


    @Test
    public void testGetAnnotationsForMethod() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(ClassWithAnnotatedMethods.class));

        Method method = null;
        for (Method mm : ap.getParsedClass().getMethods())
        {
            if (mm.getName().equals("myOtherMethod"))
            {
                method = mm;
                break;
            }
        }
        assertNotNull("we properly extracted the method", method);

        List<Annotation> annos = ap.getMethodAnnotations(method);
        assertEquals("number of annotations", 1, annos.size());
        assertEquals("annotation param",      "bar", annos.get(0).getParam("value").getScalar());

        // and make sure that we don't blow up if the method doesn't exist

        AnnotationParser ap2 = new AnnotationParser(loadClass(DefaultStringAnnotatedClass.class));
        List<Annotation> annos2 = ap2.getMethodAnnotations(method);
        assertEquals("number of annotations, missing method", 0, annos2.size());

    }
}
