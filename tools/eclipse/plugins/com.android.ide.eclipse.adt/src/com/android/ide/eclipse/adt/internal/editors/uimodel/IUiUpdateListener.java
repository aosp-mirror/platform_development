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

package com.android.ide.eclipse.adt.internal.editors.uimodel;


/**
 * Listen to update notifications in UI nodes.
 */
public interface IUiUpdateListener {

    /** Update state of the UI node */
    public enum UiUpdateState {
        /** The node's attributes have been updated. They may or may not actually have changed. */
        ATTR_UPDATED,
        /** The node sub-structure (i.e. child nodes) has changed */
        CHILDREN_CHANGED,
        /** The XML counterpart for the UI node has just been created. */
        CREATED,
        /** The XML counterpart for the UI node has just been deleted.
         *  Note that mandatory UI nodes are never actually deleted. */
        DELETED
    }

    /**
     * Indicates that an UiElementNode has been updated.
     * <p/>
     * This happens when an {@link UiElementNode} is refreshed to match the
     * XML model. The actual UI element node may or may not have changed.
     * 
     * @param ui_node The {@link UiElementNode} being updated.
     */
    public void uiElementNodeUpdated(UiElementNode ui_node, UiUpdateState state);
}
