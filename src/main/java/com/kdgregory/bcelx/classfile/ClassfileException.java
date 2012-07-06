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
