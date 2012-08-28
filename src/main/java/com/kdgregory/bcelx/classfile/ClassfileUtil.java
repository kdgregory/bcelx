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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import net.sf.kdgcommons.lang.ClassUtil;

import com.kdgregory.bcelx.parser.AnnotationParser;


/**
 *  A collection of static utility methods for examining the BCEL <code>JavaClass</code>.
 *  These exist to provide a single reference implementation of operations that involve
 *  lots of rules (such as extracting referenced classes).
 */
public class ClassfileUtil
{
    /**
     *  Returns all classes referenced by the given class. This traverses the constant
     *  pool to find direct references, and picks apart the references for fields,
     *  methods, and annotations.
     */
    public static Set<String> extractReferencedClasses(JavaClass klass)
    {
        // TreeSet is easier to debug vs HashSet, and not much slower
        TreeSet<String> result = new TreeSet<String>();

        extractReferencedClassesFromConstantPool(klass, result);
        extractReferencedClassesFromMethods(klass, result);
        extractReferencedClassesFromFields(klass, result);
        extractReferencedClassesFromAnnotations(klass, result);
        return result;
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    // This method is somewhat confusingly named: it does convert a signature,
    // but also handles "internal" classnames that don't follow the signature
    // format. CONSTANT_Class_info can have both, and I didn' want to replicate
    // 80% of this method with some external tests to see what should be called.
    private static void addClassFromSignature(String name, Set<String> result)
    {
        while (name.startsWith("["))
            name = name.substring(1);

        if (name.length() == 1)
            return; // primitive type, we don't care about it

        if (name.startsWith("L") & name.endsWith(";"))
        {
            name = name.substring(1, name.length() - 1);
        }

        name = name.replace('/', '.').replace('$', '.');

        result.add(name);
    }


    private static void extractReferencedClassesFromConstantPool(JavaClass parsedClass, Set<String> result)
    {
        ConstantPool cp = parsedClass.getConstantPool();
        for (int ii = 0 ; ii < cp.getLength() ; ii++)
        {
            Constant cc = cp.getConstant(ii);
            if (cc instanceof ConstantClass)
            {
                String className = ((ConstantClass)cc).getBytes(cp);
                addClassFromSignature(className, result);
            }
        }
    }


    private static void extractReferencedClassesFromMethods(JavaClass parsedClass, Set<String> result)
    {
        for (Method method : parsedClass.getMethods())
        {
            addClassFromSignature(method.getReturnType().getSignature(), result);
            for (Type param : method.getArgumentTypes())
            {
                addClassFromSignature(param.getSignature(), result);
            }
        }
    }


    private static void extractReferencedClassesFromFields(JavaClass parsedClass, Set<String> result)
    {
        for (Field field : parsedClass.getFields())
        {
            addClassFromSignature(field.getSignature(), result);
        }
    }


    private static void extractReferencedClassesFromAnnotations(JavaClass parsedClass, Set<String> result)
    {
        AnnotationParser ap = new AnnotationParser(parsedClass);
        for (Annotation anno : ap.getClassAnnotations())
            result.add(anno.getClassName());

        for (Method method : parsedClass.getMethods())
        {
            for (Annotation anno : ap.getMethodAnnotations(method))
                result.add(anno.getClassName());
            for (Map<String,Annotation> anno : ap.getParameterAnnotatons(method))
                result.addAll(anno.keySet());
        }

        for (Field field : parsedClass.getFields())
        {
            result.addAll(ap.getFieldAnnotations(field).keySet());
        }
    }
}
