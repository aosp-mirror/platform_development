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

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolder;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Matching strategy for the Layout Editor. This is used to open all configurations of a layout
 * in the same editor.
 */
public class MatchingStrategy implements IEditorMatchingStrategy {

    public boolean matches(IEditorReference editorRef, IEditorInput input) {
        // first check that the file being opened is a layout file.
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput)input;
            
            // get the IFile object and check it's in one of the layout folders.
            IFile iFile = fileInput.getFile();
            ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(iFile);
            
            // if it's a layout, we know check the name of the fileInput against the name of the
            // file being currently edited by the editor since those are independent of the config.
            if (resFolder != null && resFolder.getType() == ResourceFolderType.LAYOUT) {
                try {
                    IEditorInput editorInput = editorRef.getEditorInput();
                    if (editorInput instanceof FileEditorInput) {
                        FileEditorInput editorFileInput = (FileEditorInput)editorInput;
                        IFile editorIFile = editorFileInput.getFile();
                        
                        return editorIFile.getProject().equals(iFile.getProject())
                            && editorIFile.getName().equals(iFile.getName());
                    }
                } catch (PartInitException e) {
                    // we do nothing, we'll just return false.
                }
            }
        }
        return false;
    }
}
