/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.hierarchyviewer.ui.model;

import com.android.hierarchyviewer.scene.ViewNode;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;

public class ViewsTreeModel implements TreeModel {
    private final ViewNode root;

    public ViewsTreeModel(ViewNode root) {
        this.root = root;
    }

    public Object getRoot() {
        return root;
    }

    public Object getChild(Object o, int i) {
        return ((ViewNode) o).children.get(i);
    }

    public int getChildCount(Object o) {
        return ((ViewNode) o).children.size();
    }

    public boolean isLeaf(Object child) {
        ViewNode node = (ViewNode) child;
        return node.children == null || node.children.size() == 0;
    }

    public void valueForPathChanged(TreePath treePath, Object child) {
    }

    public int getIndexOfChild(Object parent, Object child) {
        //noinspection SuspiciousMethodCalls
        return ((ViewNode) parent).children.indexOf(child);
    }

    public void addTreeModelListener(TreeModelListener treeModelListener) {
    }

    public void removeTreeModelListener(TreeModelListener treeModelListener) {
    }
}
