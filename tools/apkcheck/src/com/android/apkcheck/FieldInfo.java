/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apkcheck;

/**
 * Container representing a method with parameters.
 */
public class FieldInfo {
    private String mName;
    private String mType;
    private String mNameAndType;
    private boolean mTypeNormalized;

    /**
     * Constructs a FieldInfo.
     *
     * @param name Field name.
     * @param type Fully-qualified binary or non-binary type name.
     */
    public FieldInfo(String name, String type) {
        mName = name;
        mType = type;
    }

    /**
     * Returns the combined name and type.  This value is used as a hash
     * table key.
     */
    public String getNameAndType() {
        if (mNameAndType == null)
            mNameAndType = mName + ":" + TypeUtils.typeToDescriptor(mType);
        return mNameAndType;
    }

    /**
     * Normalize the type used in fields.
     */
    public void normalizeType(ApiList apiList) {
        if (!mTypeNormalized) {
            String type = TypeUtils.ambiguousToBinaryName(mType, apiList);
            if (!type.equals(mType)) {
                /* name changed, force regen on name+type */
                mType = type;
                mNameAndType = null;
            }
            mTypeNormalized = true;
        }
    }
}

