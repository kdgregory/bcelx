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

import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassLoaderRepository;


public class TestRepositoryLoader
{
    @Test
    public void testHappyPath() throws Exception
    {
        ClassLoaderRepository repo = new ClassLoaderRepository(this.getClass().getClassLoader());
        BcelRepositoryLoader loader = new BcelRepositoryLoader(repo);

        JavaClass parsedClass = loader.loadParsedClass(this.getClass().getName());
        assertEquals("parsed class: name", this.getClass().getName(), parsedClass.getClassName());
    }


    @Test(expected=LoaderException.class)
    public void testSadPath() throws Exception
    {
        ClassLoaderRepository repo = new ClassLoaderRepository(this.getClass().getClassLoader());
        BcelRepositoryLoader loader = new BcelRepositoryLoader(repo);
        loader.loadParsedClass("foo.bar.baz");
    }
}
