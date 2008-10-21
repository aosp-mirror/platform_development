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

package com.android.ide.eclipse.adt.editors.java;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * Read only java editors. This looks like the regular Java editor, except that it
 * prevents editing the files. This is used for automatically generated java classes.
 */
public class ReadOnlyJavaEditor extends AbstractDecoratedTextEditor {
    
    public final static String ID =
        "com.android.ide.eclipse.adt.editors.java.ReadOnlyJavaEditor"; //$NON-NLS-1$

    public ReadOnlyJavaEditor() {
        IPreferenceStore javaUiStore = PreferenceConstants.getPreferenceStore();
        JavaTextTools jtt = new JavaTextTools(javaUiStore);

        JavaSourceViewerConfiguration jsvc = new JavaSourceViewerConfiguration(
                jtt.getColorManager(), javaUiStore, this,  IJavaPartitions.JAVA_PARTITIONING);

        setSourceViewerConfiguration(jsvc);
    }

    @Override
    public boolean isEditable() {
        return false;
    }
}
