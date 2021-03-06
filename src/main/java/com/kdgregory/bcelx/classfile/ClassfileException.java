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


/**
 *  This exception may be thrown by any of the methods in this package. It is a
 *  runtime exception, generally indicating a bug in the calling code. It may
 *  or may not wrap an underlying cause.
 */
public class ClassfileException
extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public ClassfileException(String message)
    {
        super(message);
    }

    public ClassfileException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
