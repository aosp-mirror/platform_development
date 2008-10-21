/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Edit an integer field, validating it as a port number.
 */
public class PortFieldEditor extends IntegerFieldEditor {

    public boolean mRecursiveCheck = false;

    public PortFieldEditor(String name, String label, Composite parent) {
        super(name, label, parent);
        setValidateStrategy(VALIDATE_ON_KEY_STROKE);
    }

    /*
     * Get the current value of the field, as an integer.
     */
    public int getCurrentValue() {
        int val;
        try {
            val = Integer.parseInt(getStringValue());
        }
        catch (NumberFormatException nfe) {
            val = -1;
        }
        return val;
    }

    /*
     * Check the validity of the field.
     */
    @Override
    protected boolean checkState() {
        if (super.checkState() == false) {
            return false;
        }
        //Log.i("ddms", "check state " + getStringValue());
        boolean err = false;
        int val = getCurrentValue();
        if (val < 1024 || val > 32767) {
            setErrorMessage("Port must be between 1024 and 32767");
            err = true;
        } else {
            setErrorMessage(null);
            err = false;
        }
        showErrorMessage();
        return !err;
    }

    protected void updateCheckState(PortFieldEditor pfe) {
        pfe.refreshValidState();
    }
}
