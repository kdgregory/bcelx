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

package com.kdgregory.bcelx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 *  Holds common supporting objects that may be used by any test. These objects
 *  are nested within this abstract class as a packaging mechanism: it avoids
 *  the configuration needed to keep JUnit from treating them as tests.
 */
public abstract class SupportObjects
{

//----------------------------------------------------------------------------
//  Annotations
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
    public @interface AnnotationValuedAnnotation
    {
        SupportObjects.StringValuedAnnotation value();
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
//  Other support classes
//----------------------------------------------------------------------------

    public enum MyEnum { RED, GREEN, BLUE }


    public static class MyLocalClass
    { /* nothing here */ }
}
