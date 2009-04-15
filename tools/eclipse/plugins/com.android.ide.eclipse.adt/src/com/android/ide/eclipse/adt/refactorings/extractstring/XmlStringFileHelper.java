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

package com.android.ide.eclipse.adt.refactorings.extractstring;

import com.android.ide.eclipse.common.project.AndroidXPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.util.HashMap;
import java.util.HashSet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * 
 */
class XmlStringFileHelper {

    /** A temporary cache of R.string IDs defined by a given xml file. The key is the
     * project path of the file, the data is a set of known string Ids for that file. */
    private HashMap<String,HashSet<String>> mResIdCache;
    /** An instance of XPath, created lazily on demand. */
    private XPath mXPath;

    public XmlStringFileHelper() {
    }
    
    /**
     * Utility method used by the wizard to check whether the given string ID is already
     * defined in the XML file which path is given.
     * 
     * @param project The project contain the XML file. 
     * @param xmlFileWsPath The project path of the XML file, e.g. "/res/values/strings.xml".
     *          The given file may or may not exist.
     * @param stringId The string ID to find.
     * @return True if such a string ID is already defined.
     */
    public boolean isResIdDuplicate(IProject project, String xmlFileWsPath, String stringId) {
        // This is going to be called many times on the same file.
        // Build a cache of the existing IDs for a given file.
        if (mResIdCache == null) {
            mResIdCache = new HashMap<String, HashSet<String>>();
        }
        HashSet<String> cache = mResIdCache.get(xmlFileWsPath);
        if (cache == null) {
            cache = getResIdsForFile(project, xmlFileWsPath);
            mResIdCache.put(xmlFileWsPath, cache);
        }
        
        return cache.contains(stringId);
    }

    /**
     * Extract all the defined string IDs from a given file using XPath.
     * @param project The project contain the XML file. 
     * @param xmlFileWsPath The project path of the file to parse. It may not exist.
     * @return The set of all string IDs defined in the file. The returned set is always non
     *   null. It is empty if the file does not exist.
     */
    private HashSet<String> getResIdsForFile(IProject project, String xmlFileWsPath) {
        HashSet<String> ids = new HashSet<String>();
        
        if (mXPath == null) {
            mXPath = AndroidXPathFactory.newXPath();
        }

        // Access the project that contains the resource that contains the compilation unit
        IResource resource = project.getFile(xmlFileWsPath);
        
        if (resource != null && resource.exists() && resource.getType() == IResource.FILE) {
            InputSource source;
            try {
                source = new InputSource(((IFile) resource).getContents());

                // We want all the IDs in an XML structure like this:
                // <resources>
                //    <string name="ID">something</string>
                // </resources>
                
                String xpathExpr = "/resources/string/@name";   //$NON-NLS-1$
                
                Object result = mXPath.evaluate(xpathExpr, source, XPathConstants.NODESET);
                if (result instanceof NodeList) {
                    NodeList list = (NodeList) result;
                    for (int n = list.getLength() - 1; n >= 0; n--) {
                        String id = list.item(n).getNodeValue();
                        ids.add(id);
                    }
                }
                
            } catch (CoreException e1) {
                // IFile.getContents failed. Ignore.
            } catch (XPathExpressionException e) {
                // mXPath.evaluate failed. Ignore.
            }
        }
        
        return ids;
    }

}
