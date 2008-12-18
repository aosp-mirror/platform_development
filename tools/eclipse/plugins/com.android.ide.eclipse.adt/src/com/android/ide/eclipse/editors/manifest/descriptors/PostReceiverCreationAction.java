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

package com.android.ide.eclipse.editors.manifest.descriptors;

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.editors.manifest.model.UiClassAttributeNode.IPostTypeCreationAction;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Action to be executed after an BroadcastReceiver class is created.
 */
class PostReceiverCreationAction implements IPostTypeCreationAction {
    
    private final static PostReceiverCreationAction sAction = new PostReceiverCreationAction();
    
    private PostReceiverCreationAction() {
        // private constructor to enforce singleton.
    }
    
    /**
     * Returns the action.
     */
    public static IPostTypeCreationAction getAction() {
        return sAction;
    }

    /**
     * Processes a newly created Activity.
     * 
     */
    public void processNewType(IType newType) {
        try {
            String methodContent = 
                "    @Override\n" +
                "    public void onReceive(Context context, Intent intent) {\n" +
                "        // TODO Auto-generated method stub\n" +
                "    }";
            newType.createMethod(methodContent, null /* sibling*/, false /* force */,
                    new NullProgressMonitor());

            // we need to add the import for Bundle, so we need the compilation unit.
            // Since the type could be enclosed in other types, we loop till we find it.
            ICompilationUnit compilationUnit = null;
            IJavaElement element = newType;
            do {
                IJavaElement parentElement = element.getParent();
                if (parentElement !=  null) {
                    if (parentElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
                        compilationUnit = (ICompilationUnit)parentElement;
                    }
                    
                    element = parentElement;
                } else {
                    break;
                }
            } while (compilationUnit == null);
            
            if (compilationUnit != null) {
                compilationUnit.createImport(AndroidConstants.CLASS_CONTEXT,
                        null /* sibling */, new NullProgressMonitor());
                compilationUnit.createImport(AndroidConstants.CLASS_INTENT,
                        null /* sibling */, new NullProgressMonitor());
            }
        } catch (JavaModelException e) {
            // looks like the class already existed (this happens when the user check to create
            // inherited abstract methods).
        }
    }
}
