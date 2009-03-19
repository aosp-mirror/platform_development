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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

/**
 * Mock implementation of {@link IFile}.
 * 
 * Optionally backed by an in-memory byte array
 * 
 * <p/>Supported methods:
 * <ul>
 *     <li>getName()</li>
 *     <li>getContents()</li>
 *     <li>getContents(boolean force)</li>
 * </ul>
 */
public class FileMock implements IFile {

    private String mName;
    private byte[] mContentData; 

    public FileMock(String name) {
        this(name, new byte[0]);
    }
    
    public FileMock(String name, byte[] fileData) {
        mName = name;
        mContentData = fileData;
    }

    // -------- MOCKED METHODS ----------------

    public String getName() {
        return mName;
    }
    
    public InputStream getContents() throws CoreException {
        return new ByteArrayInputStream(mContentData);
    }

    public InputStream getContents(boolean force) throws CoreException {
        // ignore force
        return getContents();
    }
    
    // -------- UNIMPLEMENTED METHODS ----------------

    public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void appendContents(InputStream source, boolean force, boolean keepHistory,
            IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void create(InputStream source, boolean force, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void create(InputStream source, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void createLink(URI location, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public String getCharset() throws CoreException {
        throw new NotImplementedException();
    }

    public String getCharset(boolean checkImplicit) throws CoreException {
        throw new NotImplementedException();
    }

    public String getCharsetFor(Reader reader) throws CoreException {
        throw new NotImplementedException();
    }

    public IContentDescription getContentDescription() throws CoreException {
        throw new NotImplementedException();
    }

    public int getEncoding() throws CoreException {
        throw new NotImplementedException();
    }

    public IPath getFullPath() {
        throw new NotImplementedException();
    }

    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public boolean isReadOnly() {
        throw new NotImplementedException();
    }

    public void move(IPath destination, boolean force, boolean keepHistory, 
            IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void setCharset(String newCharset) throws CoreException {
        throw new NotImplementedException();
    }

    public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void setContents(InputStream source, boolean force, boolean keepHistory,
            IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void setContents(IFileState source, boolean force, boolean keepHistory,
            IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new NotImplementedException();
    }

    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new NotImplementedException();
    }

    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void copy(IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public IMarker createMarker(String type) throws CoreException {
        throw new NotImplementedException();
    }

    public IResourceProxy createProxy() {
        throw new NotImplementedException();
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) 
    throws CoreException {
        throw new NotImplementedException();
    }

    public boolean exists() {
        throw new NotImplementedException();
    }

    public IMarker findMarker(long id) throws CoreException {
        throw new NotImplementedException();
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth)
            throws CoreException {
        throw new NotImplementedException();
    }

    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth)
            throws CoreException {
        throw new NotImplementedException();
    }

    public String getFileExtension() {
        throw new NotImplementedException();
    }

    public long getLocalTimeStamp() {
        throw new NotImplementedException();
    }

    public IPath getLocation() {
        return new Path(mName);
    }

    public URI getLocationURI() {
        throw new NotImplementedException();
    }

    public IMarker getMarker(long id) {
        throw new NotImplementedException();
    }

    public long getModificationStamp() {
        throw new NotImplementedException();
    }

    public IContainer getParent() {
        throw new NotImplementedException();
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new NotImplementedException();
    }

    public IProject getProject() {
        throw new NotImplementedException();
    }

    public IPath getProjectRelativePath() {
        throw new NotImplementedException();
    }

    public IPath getRawLocation() {
        throw new NotImplementedException();
    }

    public URI getRawLocationURI() {
        throw new NotImplementedException();
    }

    public ResourceAttributes getResourceAttributes() {
        throw new NotImplementedException();
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        throw new NotImplementedException();
    }

    public int getType() {
        throw new NotImplementedException();
    }

    public IWorkspace getWorkspace() {
        throw new NotImplementedException();
    }

    public boolean isAccessible() {
        throw new NotImplementedException();
    }

    public boolean isDerived() {
        throw new NotImplementedException();
    }

    public boolean isLinked() {
        throw new NotImplementedException();
    }

    public boolean isLinked(int options) {
        throw new NotImplementedException();
    }

    public boolean isLocal(int depth) {
        throw new NotImplementedException();
    }

    public boolean isPhantom() {
        throw new NotImplementedException();
    }

    public boolean isSynchronized(int depth) {
        throw new NotImplementedException();
    }

    public boolean isTeamPrivateMember() {
        throw new NotImplementedException();
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new NotImplementedException();
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory,
            IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public void revertModificationStamp(long value) throws CoreException {
        throw new NotImplementedException();
    }

    public void setDerived(boolean isDerived) throws CoreException {
        throw new NotImplementedException();
    }

    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        throw new NotImplementedException();
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new NotImplementedException();
    }

    public void setReadOnly(boolean readOnly) {
        throw new NotImplementedException();
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new NotImplementedException();
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new NotImplementedException();
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new NotImplementedException();
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        throw new NotImplementedException();
    }

    public boolean contains(ISchedulingRule rule) {
        throw new NotImplementedException();
    }

    public boolean isConflicting(ISchedulingRule rule) {
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public Map getPersistentProperties() throws CoreException {
        throw new NotImplementedException();
    }

    @SuppressWarnings("unchecked")
    public Map getSessionProperties() throws CoreException {
        throw new NotImplementedException();
    }

    public boolean isDerived(int options) {
        throw new NotImplementedException();
    }

    public boolean isHidden() {
        throw new NotImplementedException();
    }

    public void setHidden(boolean isHidden) throws CoreException {
        throw new NotImplementedException();
    }

    public boolean isHidden(int options) {
        throw new NotImplementedException();
    }

    public boolean isTeamPrivateMember(int options) {
        throw new NotImplementedException();
    }
}

