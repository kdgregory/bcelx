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
import org.apache.bcel.util.Repository;


/**
 *  An adaptor class that wraps a BCEL <code>Repository</code> object.
 */
public class BcelRepositoryLoader
implements ParsedClassLoader
{
    private Repository repo;


    public BcelRepositoryLoader(Repository repo)
    {
        this.repo = repo;
    }


    @Override
    public JavaClass loadParsedClass(String className)
    {
        try
        {
            return repo.loadClass(className);
        }
        catch (ClassNotFoundException ex)
        {
            throw new LoaderException("Unable to load: " + className, ex);
        }
    }
}
