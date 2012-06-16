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

import java.util.Collections;
import java.util.List;

import org.apache.bcel.util.ClassPath.ClassFile;

import com.kdgregory.bcelx.classfile.Annotation;



/**
 *  Extracts annotations from classfile data.
 */
public class AnnotationParser
{
    private ClassFile classFile;
    
    
    public AnnotationParser(ClassFile classFile)
    {
        this.classFile = classFile;
    }
    
    
    /**
     *  Returns the runtime-scope annotations for this class, an empty list if
     *  there aren't any such annotations.
     */
    public List<Annotation> getRuntimeAnnotations()
    {
        return Collections.emptyList();
    }
}
