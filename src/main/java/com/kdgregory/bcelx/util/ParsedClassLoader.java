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

package com.kdgregory.bcelx.util;

import org.apache.bcel.classfile.JavaClass;


/**
 *  This interface is used for code that needs to load multiple classes (for
 *  example, a utility to walk a class hierarchy). It provides a single method,
 *  {@link #loadClass}}.
 *  <p>
 *  The interface exists because the <code>org.apache.bcel.util.Repository</code>
 *  provides more methods than I want, since this library is (currently) meant
 *  for read-only usage. If you already have a <code>Repository</code>, you can
 *  wrap it with a {@link BcelRepositoryLoader}.
 *  <p>
 *  Implementations are permitted to cache parsed classes.
 */
public interface ParsedClassLoader
{
    /**
     *  Returns the passed class corresponding to the passed fully-qualified name.
     *
     *  @throws LoaderException if unable to parse the class.
     */
    public JavaClass loadParsedClass(String className);
}
