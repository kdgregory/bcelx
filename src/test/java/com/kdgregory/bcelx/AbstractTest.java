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

import java.io.IOException;
import java.io.InputStream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import net.sf.kdgcommons.io.IOUtil;
import net.sf.kdgcommons.lang.StringUtil;


/**
 *  Common superclass for BCELX tests, providing common utility methods and assertions.
 */
public abstract class AbstractTest
{
    /**
     *  Most of the tests need a target class, defined as a nested (static) public class.
     *  This method converts the classname into a filename, and loads it.
     */
    public static JavaClass loadNestedClass(Class<?> klass)
    throws IOException
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
}
