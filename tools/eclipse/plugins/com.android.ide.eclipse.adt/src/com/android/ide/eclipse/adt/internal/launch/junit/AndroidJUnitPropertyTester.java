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
package com.android.ide.eclipse.adt.internal.launch.junit;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

/**
 * A {@link PropertyTester} that checks if selected elements can be run as Android
 * JUnit tests.
 * <p/>
 * Based on org.eclipse.jdt.internal.junit.JUnitPropertyTester. The only substantial difference in
 * this implementation is source folders cannot be run as Android JUnit.
 */
@SuppressWarnings("restriction")
public class AndroidJUnitPropertyTester extends PropertyTester {
    private static final String PROPERTY_IS_TEST = "isTest";  //$NON-NLS-1$
    
    private static final String PROPERTY_CAN_LAUNCH_AS_JUNIT_TEST = "canLaunchAsJUnit"; //$NON-NLS-1$

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.corext.refactoring.participants.properties.IPropertyEvaluator#test(java.lang.Object, java.lang.String, java.lang.String)
     */
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IAdaptable)) {
            final String elementName = (receiver == null ? "null" : //$NON-NLS-1$
                receiver.getClass().getName());
            throw new IllegalArgumentException(
                    String.format("Element must be of type IAdaptable, is %s", //$NON-NLS-1$
                            elementName));
        }

        IJavaElement element;
        if (receiver instanceof IJavaElement) {
            element = (IJavaElement) receiver;
        } else if (receiver instanceof IResource) {
            element = JavaCore.create((IResource) receiver);
            if (element == null) {
                return false;
            }
        } else { // is IAdaptable
            element= (IJavaElement) ((IAdaptable) receiver).getAdapter(IJavaElement.class);
            if (element == null) {
                IResource resource = (IResource) ((IAdaptable) receiver).getAdapter(
                        IResource.class);
                element = JavaCore.create(resource);
                if (element == null) {
                    return false;
                }
            }
        }
        if (PROPERTY_IS_TEST.equals(property)) { 
            return isJUnitTest(element);
        } else if (PROPERTY_CAN_LAUNCH_AS_JUNIT_TEST.equals(property)) {
            return canLaunchAsJUnitTest(element);
        }
        throw new IllegalArgumentException(
                String.format("Unknown test property '%s'", property)); //$NON-NLS-1$
    }
    
    private boolean canLaunchAsJUnitTest(IJavaElement element) {
        try {
            switch (element.getElementType()) {
                case IJavaElement.JAVA_PROJECT:
                    return true; // can run, let JDT detect if there are tests
                case IJavaElement.PACKAGE_FRAGMENT_ROOT:
                    return false; // not supported by Android test runner
                case IJavaElement.PACKAGE_FRAGMENT:
                    return ((IPackageFragment) element).hasChildren(); 
                case IJavaElement.COMPILATION_UNIT:
                case IJavaElement.CLASS_FILE:
                case IJavaElement.TYPE:
                case IJavaElement.METHOD:
                    return isJUnitTest(element);
                default:
                    return false;
            }
        } catch (JavaModelException e) {
            return false;
        }
    }

    /**
     * Return whether the target resource is a JUnit test.
     */
    private boolean isJUnitTest(IJavaElement element) {
        try {
            IType testType = null;
            if (element instanceof ICompilationUnit) {
                testType = (((ICompilationUnit) element)).findPrimaryType();
            } else if (element instanceof IClassFile) {
                testType = (((IClassFile) element)).getType();
            } else if (element instanceof IType) {
                testType = (IType) element;
            } else if (element instanceof IMember) {
                testType = ((IMember) element).getDeclaringType();
            }
            if (testType != null && testType.exists()) {
                return TestSearchEngine.isTestOrTestSuite(testType);
            }
        } catch (CoreException e) {
            // ignore, return false
        }
        return false;
    }
}
