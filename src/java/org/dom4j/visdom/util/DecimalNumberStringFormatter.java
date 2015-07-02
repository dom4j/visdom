/*
 * Copyright 2001 (C) MetaStuff, Ltd. All Rights Reserved.
 * 
 * This software is open source. 
 * See the bottom of this file for the licence.
 * 
 * $Id: DecimalNumberStringFormatter.java,v 1.1.1.1 2001/05/22 08:12:57 jstrachan Exp $
 */

package org.dom4j.visdom.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 * Formats the given Date object as a string using java.text.DateFormat.
 */
public class DecimalNumberStringFormatter implements StringFormatter
{
    /**
     * Default constructor uses the default java.text.NumberFormat style
     */
    public DecimalNumberStringFormatter()
    {
        formatter = NumberFormat.getInstance();
    }

    /**
     * Allows a formatter to be defined in terms of a pattern
     * using java.text.DecimalFormat syntax
     */
    public DecimalNumberStringFormatter( String pattern )
    {
        formatter = new DecimalFormat( pattern );
    }

    /**
     * Allows a formatter to be defined in terms of a pattern
     * using java.text.DecimalFormat syntax and an additional
     * factor to be divided.
     * <BR> For example if you had the number of bytes for a file in a number
     * of value 2048 and you used this method with parameters ( "#'K'", 1024 ) you
     * would get a formatting of 2K.
     */
    public DecimalNumberStringFormatter( String pattern, long factor )
    {
        formatter = new DecimalFormat( pattern );
        this.factor = factor;
    }

    //-------------------------------------------------------------------------
    // StringFormatter interface
    //-------------------------------------------------------------------------
    public String               valueToString( Object object )
    {
        if ( object instanceof Number )
        {
            // format the date
            Number number = (Number) object;
            double d = number.doubleValue();
            if ( factor != 0 )
            {
                d /= factor;
            }
            return formatter.format( d );

        }
        return object.toString();
    }


    //-------------------------------------------------------------------------
    // Attributes
    //-------------------------------------------------------------------------
    NumberFormat                formatter;
    long                        factor;
}




/*
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "DOM4J" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of MetaStuff, Ltd.  For written permission,
 *    please contact dom4j-info@metastuff.com.
 *
 * 4. Products derived from this Software may not be called "DOM4J"
 *    nor may "DOM4J" appear in their names without prior written
 *    permission of MetaStuff, Ltd. DOM4J is a registered
 *    trademark of MetaStuff, Ltd.
 *
 * 5. Due credit should be given to the DOM4J Project
 *    (http://dom4j.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY METASTUFF, LTD. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * METASTUFF, LTD. OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2001 (C) MetaStuff, Ltd. All Rights Reserved.
 *
 * $Id: DecimalNumberStringFormatter.java,v 1.1.1.1 2001/05/22 08:12:57 jstrachan Exp $
 */
