/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.refactorings.extractstring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.Map;

/**
 * A descriptor that allows an {@link ExtractStringRefactoring} to be created from
 * a previous instance of itself.
 */
public class ExtractStringDescriptor extends RefactoringDescriptor {

    public static final String ID =
        "com.android.ide.eclipse.adt.refactoring.extract.string";  //$NON-NLS-1$
    
    private final Map<String, String> mArguments;

    public ExtractStringDescriptor(String project, String description, String comment,
            Map<String, String> arguments) {
        super(ID, project, description, comment,
                RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE //flags
        );
        mArguments = arguments;
    }
    
    public Map<String, String> getArguments() {
        return mArguments;
    }

    /**
     * Creates a new refactoring instance for this refactoring descriptor based on
     * an argument map. The argument map is created by the refactoring itself in
     * {@link ExtractStringRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)}
     * <p/>
     * This is apparently used to replay a refactoring.
     * 
     * {@inheritDoc}
     * 
     * @throws CoreException
     */
    @Override
    public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
        try {
            ExtractStringRefactoring ref = new ExtractStringRefactoring(mArguments);
            return ref;
        } catch (NullPointerException e) {
            status.addFatalError("Failed to recreate ExtractStringRefactoring from descriptor");
            return null;
        }
    }

}
