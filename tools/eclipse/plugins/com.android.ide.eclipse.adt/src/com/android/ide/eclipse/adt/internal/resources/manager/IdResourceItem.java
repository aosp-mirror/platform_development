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

package com.android.ide.eclipse.adt.internal.resources.manager;

import com.android.ide.eclipse.adt.internal.resources.IIdResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;

/**
 * Represents a resource item of type {@link ResourceType#ID}
 */
public class IdResourceItem extends ProjectResourceItem implements IIdResourceItem {

    private final boolean mIsDeclaredInline;

    /**
     * Constructs a new ResourceItem.
     * @param name the name of the resource as it appears in the XML and R.java files.
     * @param isDeclaredInline Whether this id was declared inline.
     */
    IdResourceItem(String name, boolean isDeclaredInline) {
        super(name);
        mIsDeclaredInline = isDeclaredInline;
    }

    /*
     * (non-Javadoc)
     * Returns whether the ID resource has been declared inline inside another resource XML file. 
     */
    public boolean isDeclaredInline() {
        return mIsDeclaredInline;
    }

    /* (non-Javadoc)
     * Returns whether the item can be edited (ie, the id was not declared inline).
     */
    @Override
    public boolean isEditableDirectly() {
        return !mIsDeclaredInline;
    }
}
