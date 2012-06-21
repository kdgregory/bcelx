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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantObject;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Unknown;

import net.sf.kdgcommons.lang.ClassUtil;
import net.sf.kdgcommons.lang.UnreachableCodeException;

import com.kdgregory.bcelx.classfile.Annotation;
import com.kdgregory.bcelx.classfile.Annotation.*;


/**
 *  Extracts annotations from classfile data.
 *  <p>
 *  Any of the methods from this class can throw {@link ParseException}. This is a
 *  runtime exception; it almost certainly indicates a bug in the parser (or a bad
 *  classfile, which is equally unrecoverable).
 */
public class AnnotationParser
{
    private final static String ATTR_ANNOTATION_RUNTIME = "RuntimeVisibleAnnotations";
    private final static String ATTR_ANNOTATION_CLASS   = "RuntimeInvisibleAnnotations";

    private JavaClass classFile;
    private ConstantPool cp;


    // these will be lazily instantiated as needed
    List<Annotation> classVisibleAnnotations;
    List<Annotation> classInvisibleAnnotations;


    public AnnotationParser(JavaClass classFile)
    {
        this.classFile = classFile;
        this.cp = classFile.getConstantPool();
    }


    /**
     *  Returns the runtime-visible annotations for this class, an empty list if
     *  there aren't any such annotations.
     */
    public List<Annotation> getClassVisibleAnnotations()
    {
        if (classVisibleAnnotations == null)
        {
            classVisibleAnnotations = parseAnnotationsFromAttributes(
                                        classFile.getAttributes(),
                                        RetentionPolicy.RUNTIME);
        }
        return classVisibleAnnotations;
    }


    /**
     *  Returns the runtime-invisible annotations for this class, an empty list if
     *  there aren't any such annotations.
     */
    public List<Annotation> getClassInvisibleAnnotations()
    {
        if (classInvisibleAnnotations == null)
        {
            classInvisibleAnnotations = parseAnnotationsFromAttributes(
                                        classFile.getAttributes(),
                                        RetentionPolicy.CLASS);
        }
        return classInvisibleAnnotations;
    }


//----------------------------------------------------------------------------
//  Internals
//----------------------------------------------------------------------------

    /**
     *  Retrieves a string from the constant pool by its index.
     */
    private String stringConstant(int index)
    {
        ConstantUtf8 val = (ConstantUtf8)cp.getConstant(index);
        return val.getBytes();
    }


    /**
     *  Retrieves a string that represents a classname, and converts it from
     *  internal format to external.
     */
    private String typeNameConstant(int index)
    {
        String str = stringConstant(index);
        return ClassUtil.internalNameToExternal(str);
    }


    /**
     *  The common parsing code: given the attributes that belong to a class /
     *  method / field, finds those that are hold attributes, and pull the
     *  attributes out of them.
     */
    private List<Annotation> parseAnnotationsFromAttributes(Attribute[] attrs, RetentionPolicy annoPolicy)
    {
        List<Annotation> result = new ArrayList<Annotation>();
        String attrName = (annoPolicy == RetentionPolicy.CLASS) ? ATTR_ANNOTATION_CLASS
                        : (annoPolicy == RetentionPolicy.RUNTIME) ? ATTR_ANNOTATION_RUNTIME
                        : "";  // should never happen, means we won't match anything
        try
        {
            for (Attribute attr : attrs)
            {
                String name = stringConstant(attr.getNameIndex());
                if (attrName.equals(name))
                {
                    byte[] attrData = ((Unknown)attr).getBytes();
                    DataInputStream in = new DataInputStream(
                                            new ByteArrayInputStream(attrData));
                    int numAnnos = in.readUnsignedShort();
                    for (int ii = 0 ; ii < numAnnos ; ii++)
                    {
                        result.add(parseAnnotationFromStream(in, annoPolicy));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new ParseException("unable to parse attribute", ex);
        }
        return result;
    }


    /**
     *  Parses a single annotation from the passed stream.
     */
    private Annotation parseAnnotationFromStream(DataInputStream in, RetentionPolicy annoPolicy)
    throws IOException
    {
        String annoTypeName = typeNameConstant(in.readUnsignedShort());
        Annotation anno = new Annotation(annoTypeName, annoPolicy);

        int numElementValuePairs = in.readUnsignedShort();
        for (int jj = 0 ; jj < numElementValuePairs ; jj++)
        {
            String paramName = stringConstant(in.readUnsignedShort());
            ParamValue paramValue = parseAnnotationValue(in);
            anno.addParam(paramName, paramValue);
        }
        return anno;
    }

    private ParamValue parseAnnotationValue(DataInputStream in)
    throws IOException
    {
        int tag = in.readUnsignedByte();

        switch (tag)
        {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
                int baseIndex = in.readUnsignedShort();
                ConstantObject baseValue = (ConstantObject)cp.getConstant(baseIndex);
                return new ScalarValue(ParamType.NUMBER, baseValue.getConstantValue(cp));
            case 's':
                int stringIndex = in.readUnsignedShort();
                return new ScalarValue(ParamType.STRING, stringConstant(stringIndex));
            case 'c':
                int classIndex = in.readUnsignedShort();
                return new ClassValue(typeNameConstant(classIndex));
            case 'e':
                int enumTypeIndex = in.readUnsignedShort();
                int enumValueIndex = in.readUnsignedShort();
                return new EnumValue(typeNameConstant(enumTypeIndex), stringConstant(enumValueIndex));
            case '[':
                int arraySize = in.readUnsignedShort();
                List<ParamValue> arrayValues = new ArrayList<ParamValue>(arraySize);
                for (int ii = 0 ; ii < arraySize ; ii++)
                {
                    arrayValues.add(parseAnnotationValue(in));
                }
                return new ArrayValue(arrayValues);
            default :
                throw new UnreachableCodeException("invalid value tag: " + tag);
        }
    }
}
