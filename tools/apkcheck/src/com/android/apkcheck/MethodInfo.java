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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Container representing a method with parameters.
 */
public class MethodInfo {
    private String mName;
    private String mReturn;
    private String mNameAndDescriptor;
    private ArrayList<String> mParameters;
    private boolean mParametersNormalized;

    /**
     * Constructs MethodInfo.  Tuck the method return type away for
     * later construction of the signature.
     */
    public MethodInfo(String name, String returnType) {
        mName = name;
        mReturn = returnType;
        mParameters = new ArrayList<String>();
    }

    /**
     * Returns the method signature.  This is generated when needed.
     */
    public String getNameAndDescriptor() {
        if (mNameAndDescriptor == null) {
            StringBuilder newSig = new StringBuilder(mName);
            newSig.append(":(");
            for (int i = 0; i < mParameters.size(); i++) {
                String humanType = mParameters.get(i);
                String sigType = TypeUtils.typeToDescriptor(humanType);
                newSig.append(sigType);
            }
            newSig.append(")");
            newSig.append(TypeUtils.typeToDescriptor(mReturn));
            mNameAndDescriptor = newSig.toString();
        }
        return mNameAndDescriptor;
    }

    /**
     * Adds a parameter to the method.  The "type" is a primitive or
     * object type, formatted in human-centric form.  For now we just
     * store it.
     */
    public void addParameter(String type) {
        mParameters.add(type);
        if (mNameAndDescriptor != null) {
            System.err.println("WARNING: late add of params to method");
            mNameAndDescriptor = null;      // force regen
        }
    }

    /**
     * Normalizes the types in parameter lists to unambiguous binary form.
     *
     * The public API file must be fully parsed before calling here,
     * because we need the full set of package names.
     */
    public void normalizeTypes(ApiList apiList) {
        if (!mParametersNormalized) {
            mReturn = TypeUtils.ambiguousToBinaryName(mReturn, apiList);

            for (int i = 0; i < mParameters.size(); i++) {
                String fixed = TypeUtils.ambiguousToBinaryName(mParameters.get(i),
                        apiList);
                mParameters.set(i, fixed);
            }

            mNameAndDescriptor = null;      // force regen
            mParametersNormalized = true;
        }
    }
}

