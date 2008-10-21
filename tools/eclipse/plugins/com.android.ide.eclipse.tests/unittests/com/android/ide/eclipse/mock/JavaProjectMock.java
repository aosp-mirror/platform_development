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

package com.android.ide.eclipse.mock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.eval.IEvaluationContext;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Map;

public class JavaProjectMock implements IJavaProject {
    
    private IProject mProject;

    private IClasspathEntry[] mEntries;
    private IPath mOutputLocation;
    private String mCompilerCompliance = "1.4"; //$NON-NLS-1
    private String mCompilerSource = "1.4"; //$NON-NLS-1
    private String mCompilerTarget = "1.4"; //$NON-NLS-1

    public JavaProjectMock(IClasspathEntry[] entries, IPath outputLocation) {
        mEntries = entries;
        mOutputLocation = outputLocation;
    }
    
    public IProject getProject() {
        if (mProject == null) {
            mProject = new ProjectMock();
        }

        return mProject;
    }

    
    public void setRawClasspath(IClasspathEntry[] entries, IProgressMonitor monitor)
            throws JavaModelException {
        mEntries = entries;
    }

    public void setRawClasspath(IClasspathEntry[] entries, IPath outputLocation,
            IProgressMonitor monitor) throws JavaModelException {
        mEntries = entries;
        mOutputLocation = outputLocation;
    }
    
    public IClasspathEntry[] getRawClasspath() throws JavaModelException {
        return mEntries;
    }
    
    public IPath getOutputLocation() throws JavaModelException {
        return mOutputLocation;
    }
    
    public String getOption(String optionName, boolean inheritJavaCoreOptions) {
        if (optionName.equals(JavaCore.COMPILER_COMPLIANCE)) {
            return mCompilerCompliance;
        } else if (optionName.equals(JavaCore.COMPILER_SOURCE)) {
            return mCompilerSource;
        } else if (optionName.equals(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM)) {
            return mCompilerTarget;
        }
        
        return null;
    }
        
    public void setOption(String optionName, String optionValue) {
        if (optionName.equals(JavaCore.COMPILER_COMPLIANCE)) {
            mCompilerCompliance = optionValue;
        } else if (optionName.equals(JavaCore.COMPILER_SOURCE)) {
            mCompilerSource = optionValue;
        } else if (optionName.equals(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM)) {
            mCompilerTarget = optionValue;
        } else {
            throw new NotImplementedException();
        }
    }


    // -------- UNIMPLEMENTED METHODS ----------------
    
    public IClasspathEntry decodeClasspathEntry(String encodedEntry) {
        throw new NotImplementedException();
    }

    public String encodeClasspathEntry(IClasspathEntry classpathEntry) {
        throw new NotImplementedException();
    }

    public IJavaElement findElement(IPath path) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IJavaElement findElement(IPath path, WorkingCopyOwner owner) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IPackageFragment findPackageFragment(IPath path) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IPackageFragmentRoot findPackageFragmentRoot(IPath path) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IPackageFragmentRoot[] findPackageFragmentRoots(IClasspathEntry entry) {
        throw new NotImplementedException();
    }

    public IType findType(String fullyQualifiedName) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IType findType(String fullyQualifiedName, IProgressMonitor progressMonitor)
            throws JavaModelException {
        throw new NotImplementedException();
    }

    public IType findType(String fullyQualifiedName, WorkingCopyOwner owner)
            throws JavaModelException {
        throw new NotImplementedException();
    }

    public IType findType(String packageName, String typeQualifiedName) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IType findType(String fullyQualifiedName, WorkingCopyOwner owner,
            IProgressMonitor progressMonitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IType findType(String packageName, String typeQualifiedName,
            IProgressMonitor progressMonitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner)
            throws JavaModelException {
        throw new NotImplementedException();
    }

    public IType findType(String packageName, String typeQualifiedName, WorkingCopyOwner owner,
            IProgressMonitor progressMonitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IPackageFragmentRoot[] getAllPackageFragmentRoots() throws JavaModelException {
        throw new NotImplementedException();
    }

    public Object[] getNonJavaResources() throws JavaModelException {
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public Map getOptions(boolean inheritJavaCoreOptions) {
        throw new NotImplementedException();
    }

    public IPackageFragmentRoot getPackageFragmentRoot(String jarPath) {
        throw new NotImplementedException();
    }

    public IPackageFragmentRoot getPackageFragmentRoot(IResource resource) {
        throw new NotImplementedException();
    }

    public IPackageFragmentRoot[] getPackageFragmentRoots() throws JavaModelException {
        throw new NotImplementedException();
    }

    public IPackageFragmentRoot[] getPackageFragmentRoots(IClasspathEntry entry) {
        throw new NotImplementedException();
    }

    public IPackageFragment[] getPackageFragments() throws JavaModelException {
        throw new NotImplementedException();
    }

    public String[] getRequiredProjectNames() throws JavaModelException {
        throw new NotImplementedException();
    }

    public IClasspathEntry[] getResolvedClasspath(boolean ignoreUnresolvedEntry)
            throws JavaModelException {
        throw new NotImplementedException();
    }

    public boolean hasBuildState() {
        throw new NotImplementedException();
    }

    public boolean hasClasspathCycle(IClasspathEntry[] entries) {
        throw new NotImplementedException();
    }

    public boolean isOnClasspath(IJavaElement element) {
        throw new NotImplementedException();
    }

    public boolean isOnClasspath(IResource resource) {
        throw new NotImplementedException();
    }

    public IEvaluationContext newEvaluationContext() {
        throw new NotImplementedException();
    }

    public ITypeHierarchy newTypeHierarchy(IRegion region, IProgressMonitor monitor)
            throws JavaModelException {
        throw new NotImplementedException();
    }

    public ITypeHierarchy newTypeHierarchy(IRegion region, WorkingCopyOwner owner,
            IProgressMonitor monitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public ITypeHierarchy newTypeHierarchy(IType type, IRegion region, IProgressMonitor monitor)
            throws JavaModelException {
        throw new NotImplementedException();
    }

    public ITypeHierarchy newTypeHierarchy(IType type, IRegion region, WorkingCopyOwner owner,
            IProgressMonitor monitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IPath readOutputLocation() {
        throw new NotImplementedException();
    }

    public IClasspathEntry[] readRawClasspath() {
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public void setOptions(Map newOptions) {
        throw new NotImplementedException();
    }

    public void setOutputLocation(IPath path, IProgressMonitor monitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public void setRawClasspath(IClasspathEntry[] entries, boolean canModifyResources,
            IProgressMonitor monitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public void setRawClasspath(IClasspathEntry[] entries, IPath outputLocation,
            boolean canModifyResources, IProgressMonitor monitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IJavaElement[] getChildren() throws JavaModelException {
        throw new NotImplementedException();
    }

    public boolean hasChildren() throws JavaModelException {
        throw new NotImplementedException();
    }

    public boolean exists() {
        throw new NotImplementedException();
    }

    public IJavaElement getAncestor(int ancestorType) {
        throw new NotImplementedException();
    }

    public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaModelException {
        throw new NotImplementedException();
    }

    public IResource getCorrespondingResource() throws JavaModelException {
        throw new NotImplementedException();
    }

    public String getElementName() {
        throw new NotImplementedException();
    }

    public int getElementType() {
        throw new NotImplementedException();
    }

    public String getHandleIdentifier() {
        throw new NotImplementedException();
    }

    public IJavaModel getJavaModel() {
        throw new NotImplementedException();
    }

    public IJavaProject getJavaProject() {
        throw new NotImplementedException();
    }

    public IOpenable getOpenable() {
        throw new NotImplementedException();
    }

    public IJavaElement getParent() {
        throw new NotImplementedException();
    }

    public IPath getPath() {
        throw new NotImplementedException();
    }

    public IJavaElement getPrimaryElement() {
        throw new NotImplementedException();
    }

    public IResource getResource() {
        throw new NotImplementedException();
    }

    public ISchedulingRule getSchedulingRule() {
        throw new NotImplementedException();
    }

    public IResource getUnderlyingResource() throws JavaModelException {
        throw new NotImplementedException();
    }

    public boolean isReadOnly() {
        throw new NotImplementedException();
    }

    public boolean isStructureKnown() throws JavaModelException {
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        throw new NotImplementedException();
    }

    public void close() throws JavaModelException {
        throw new NotImplementedException();
    }

    public String findRecommendedLineSeparator() throws JavaModelException {
        throw new NotImplementedException();
    }

    public IBuffer getBuffer() throws JavaModelException {
        throw new NotImplementedException();
    }

    public boolean hasUnsavedChanges() throws JavaModelException {
        throw new NotImplementedException();
    }

    public boolean isConsistent() throws JavaModelException {
        throw new NotImplementedException();
    }

    public boolean isOpen() {
        throw new NotImplementedException();
    }

    public void makeConsistent(IProgressMonitor progress) throws JavaModelException {
        throw new NotImplementedException();
    }

    public void open(IProgressMonitor progress) throws JavaModelException {
        throw new NotImplementedException();
    }

    public void save(IProgressMonitor progress, boolean force) throws JavaModelException {
        throw new NotImplementedException();
    }

	public IJavaElement findElement(String bindingKey, WorkingCopyOwner owner)
			throws JavaModelException {
        throw new NotImplementedException();
	}
}
