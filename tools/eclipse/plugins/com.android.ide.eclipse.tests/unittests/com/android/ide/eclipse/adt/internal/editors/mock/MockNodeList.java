/*
 * Copyright (C) 2007 The Android Open Source Project
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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;


/**
 * A quick mock implementation of NodeList on top of ArrayList.
 */
public class MockNodeList implements NodeList {

    ArrayList<MockXmlNode> mChildren;

    /**
    * Constructs a node list from a given children list.
    * 
    * @param children The children list. Can be null.
     */
    public MockNodeList(MockXmlNode[] children) {
        mChildren = new ArrayList<MockXmlNode>();
        if (children != null) {
            for (MockXmlNode n : children) {
                mChildren.add(n);
            }
        }
    }

    public int getLength() {
        return mChildren.size();
    }

    public Node item(int index) {
        if (index >= 0 && index < mChildren.size()) {
            return mChildren.get(index);
        }
        return null;
    }

    public ArrayList<MockXmlNode> getArrayList() {
        return mChildren;
    }
}
