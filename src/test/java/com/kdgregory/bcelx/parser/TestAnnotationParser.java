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
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

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


//----------------------------------------------------------------------------
//  Testcases
//----------------------------------------------------------------------------

    @Test
    public void testClassMarkerAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(MarkedClass.class));

        List<Annotation> anno1 = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno1.size());
        assertEquals("value of runtime visible annotation",
                     "@TestAnnotationParser.RuntimeMarkerAnnotation",
                     anno1.get(0).toString());

        List<Annotation> anno2 = ap.getClassInvisibleAnnotations();
        assertEquals("count of runtime invisible annotations", 1, anno2.size());
        assertEquals("value of runtime invisible annotation",
                     "@TestAnnotationParser.ClassMarkerAnnotation",
                     anno2.get(0).toString());

    }


    @Test
    public void testStringValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(DefaultStringAnnotatedClass.class));

        List<Annotation> anno1 = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno1.size());
        assertEquals("value of annotation",
                     "@TestAnnotationParser.DefaultStringAnnotation(\"foo\")",
                     anno1.get(0).toString());
        assertEquals("value as string",
                     "foo",
                     anno1.get(0).getParams().get("value").getScalar());
    }


    @Test
    public void testNumberValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(DefaultNumericAnnotatedClass.class));

        List<Annotation> anno = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno.size());
        assertEquals("value of annotation",
                     "@TestAnnotationParser.DefaultNumericAnnotation(12)",
                     anno.get(0).toString());
        assertEquals("value as number",
                     new Integer(12),
                     anno.get(0).getParams().get("value").getScalar());
    }


    @Test
    public void testClassValuedAnnotations() throws Exception
    {
        AnnotationParser ap1 = new AnnotationParser(loadClass(DefaultClassAnnotatedClass.class));

        List<Annotation> anno1 = ap1.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno1.size());
        assertEquals("value of annotation",
                     "@TestAnnotationParser.DefaultClassAnnotation(java.lang.String.class)",
                     anno1.get(0).toString());
        assertEquals("value as class",
                     String.class,
                     anno1.get(0).getParams().get("value").getKlass());

        AnnotationParser ap2 = new AnnotationParser(loadClass(DefaultInnerClassAnnotatedClass.class));

        List<Annotation> anno2 = ap2.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno2.size());
        assertEquals("value of annotation",
                     "@TestAnnotationParser.DefaultClassAnnotation(com.kdgregory.bcelx.parser.TestAnnotationParser.MyLocalClass.class)",
                     anno2.get(0).toString());
        assertEquals("value as class",
                     MyLocalClass.class,
                     anno2.get(0).getParams().get("value").getKlass());
    }


    @Test
    public void testEnumValuedAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(DefaultEnumAnnotatedClass.class));

        List<Annotation> anno = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno.size());
        assertEquals("value of annotation",
                     "@TestAnnotationParser.DefaultEnumAnnotation(com.kdgregory.bcelx.parser.TestAnnotationParser.MyEnum.RED)",
                     anno.get(0).toString());
        assertEquals("value as enum",
                     MyEnum.RED,
                     anno.get(0).getParams().get("value").getEnum());
    }


    @Test
    public void testArrayValuedAnnotations() throws Exception
    {
        AnnotationParser ap1 = new AnnotationParser(loadClass(DefaultStringArrayAnnotatedClass.class));

        List<Annotation> anno1 = ap1.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno1.size());
        assertEquals("value of annotation",
                     "@TestAnnotationParser.DefaultStringArrayAnnotation({\"foo\", \"bar\"})",
                     anno1.get(0).toString());
        assertEquals("value as list",
                     Arrays.asList("foo", "bar"),
                     anno1.get(0).getParams().get("value").getArrayAsObjects());
    }


    @Test
    public void testMultiParamAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(TwoParamAnnotatedClass.class));

        List<Annotation> anno = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 1, anno.size());
        assertEquals("value of annotation",
                     "@TestAnnotationParser.TwoParamAnnotation(name=\"foo\", quantity=12)",
                     anno.get(0).toString());
        assertEquals("name as string",
                     "foo",
                     anno.get(0).getParams().get("name").getScalar());
        assertEquals("value as number",
                     new Integer(12),
                     anno.get(0).getParams().get("quantity").getScalar());
    }


    @Test
    public void testMultiAnnotations() throws Exception
    {
        AnnotationParser ap = new AnnotationParser(loadClass(MultiplyAnnotatedClass.class));

        List<Annotation> anno = ap.getClassVisibleAnnotations();
        assertEquals("count of runtime visible annotations", 2, anno.size());
        assertEquals("value of annotation 0",
                     "@TestAnnotationParser.DefaultStringAnnotation(\"foo\")",
                     anno.get(0).toString());
        assertEquals("value of annotation 1",
                     "@TestAnnotationParser.DefaultNumericAnnotation(12)",
                     anno.get(1).toString());
    }

}
