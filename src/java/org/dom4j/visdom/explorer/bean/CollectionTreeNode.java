/*
 * Copyright 2001 (C) MetaStuff, Ltd. All Rights Reserved.
 * 
 * This software is open source. 
 * See the bottom of this file for the licence.
 * 
 * $Id$
 */

package org.dom4j.visdom.explorer.bean;

import org.dom4j.visdom.explorer.TreeModelAdapter;
import org.dom4j.visdom.explorer.ViewableNode;
import org.dom4j.visdom.explorer.util.BaseTreeNode;
import org.dom4j.visdom.explorer.util.ObjectTreeNode;
import org.dom4j.visdom.explorer.util.DefaultTreeNodeFactory;
import org.dom4j.visdom.explorer.util.TreeNodeFactory;
import org.dom4j.visdom.explorer.util.TreeChildrenSorter;
import org.dom4j.visdom.table.DefaultTablePanel;
import org.dom4j.visdom.table.CollectionTableModel;
import org.dom4j.visdom.util.holder.DefaultObjectHolder;
import org.dom4j.visdom.util.holder.ObjectHolder;
import org.dom4j.visdom.util.ObjectToString;

import javax.swing.tree.MutableTreeNode;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

import java.awt.Component;

public class CollectionTreeNode extends ObjectTreeNode implements ViewableNode
{
    public CollectionTreeNode( String                 name,
                               Object                 object,
                               TreeNodeFactory        factory )
    {
        super( name, object, factory );
    }

    //-------------------------------------------------------------------------
    // ViewableNode interface
    //-------------------------------------------------------------------------

    public Component  makeComponentView( final TreeModelAdapter treeModelAdapter )
    {
        Collection collection = getCollection();
        if ( collection != null )
        {
            CollectionTableModel model = new CollectionTableModel( collection );
            return new DefaultTablePanel( model );
        }
        else
        {
            return null;
        }
    }

    //-------------------------------------------------------------------------
    // Overriden methods
    //-------------------------------------------------------------------------
    public void
    loadChildren()
    {
        Collection collection = getCollection();
        if ( collection != null )
        {
            int pos = 0;

            for ( Iterator i = collection.iterator(); i.hasNext(); )
            {
                Object value = i.next();

                String name = ObjectToString.numberToString(pos);

                MutableTreeNode newNode = createTreeNode( name, value );
                //MutableTreeNode newNode = getTreeNodeFactory().getTreeNode( name, holder );

                insert( newNode, pos++ );
            }
        }
    }


    //-------------1------------------------------------------------------------
    // Implementation methods
    //-------------------------------------------------------------------------
    protected Collection
    getCollection()
    {
        Object object = getObject();
        if ( object != null && object instanceof Collection )
        {
            return (Collection) object;
        }
        return null;
    }



    //-------------------------------------------------------------------------
    // Implementation methods
    //-------------------------------------------------------------------------



    //-------------------------------------------------------------------------
    // Attributes
    //-------------------------------------------------------------------------
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
 * $Id$
 */
