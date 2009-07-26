/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.mock;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;

class MockNamedNodeMap implements NamedNodeMap {
    
    /** map for access by namespace/name */
    private final HashMap<String, HashMap<String, Node>> mNodeMap =
        new HashMap<String, HashMap<String, Node>>();
    
    /** list for access by index */
    private final ArrayList<Node> mNodeList = new ArrayList<Node>();
     
    public MockXmlNode addAttribute(String namespace, String localName, String value) {
        MockXmlNode node = new MockXmlNode(namespace, localName, value);

        if (namespace == null) {
            namespace = ""; // no namespace
        }
        
        // get the map for the namespace
        HashMap<String, Node> map = mNodeMap.get(namespace);
        if (map == null) {
            map = new HashMap<String, Node>();
            mNodeMap.put(namespace, map);
        }
        
        
        map.put(localName, node);
        mNodeList.add(node);
        
        return node;
    }
    
    // --------- NamedNodeMap -------

    public int getLength() {
        return mNodeList.size();
    }

    public Node getNamedItem(String name) {
        HashMap<String, Node> map = mNodeMap.get(""); // no namespace
        if (map != null) {
            return map.get(name);
        }
        
        return null;
    }

    public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
        if (namespaceURI == null) {
            namespaceURI = ""; //no namespace
        }
        
        HashMap<String, Node> map = mNodeMap.get(namespaceURI);
        if (map != null) {
            return map.get(localName);
        }
        
        return null;
    }

    public Node item(int index) {
        return mNodeList.get(index);
    }

    public Node removeNamedItem(String name) throws DOMException {
        throw new NotImplementedException();
    }

    public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
        throw new NotImplementedException();
    }

    public Node setNamedItem(Node arg) throws DOMException {
        throw new NotImplementedException();
    }

    public Node setNamedItemNS(Node arg) throws DOMException {
        throw new NotImplementedException();
    }

}
