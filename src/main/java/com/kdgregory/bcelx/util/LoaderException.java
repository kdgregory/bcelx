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


/**
 *  This exception is thrown when a {@link ParsedClassLoader} is unable to load
 *  a class. Instances will usually have an underlying cause, which will usually
 *  be <code>IOException</code>.
 */
public class LoaderException
extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public LoaderException(String message)
    {
        super(message);
    }

    public LoaderException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
