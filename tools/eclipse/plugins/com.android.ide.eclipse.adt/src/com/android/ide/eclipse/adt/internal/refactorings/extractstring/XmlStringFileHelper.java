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

import com.android.sdklib.xml.AndroidXPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

/**
 * An helper utility to get IDs out of an Android XML resource file.
 */
class XmlStringFileHelper {

    /** A temporary cache of R.string IDs defined by a given xml file. The key is the
     * project path of the file, the data is a set of known string Ids for that file.
     *
     * Map type: map [String filename] => map [String id => String value].
     */
    private HashMap<String, Map<String, String>> mResIdCache =
        new HashMap<String, Map<String, String>>();
    /** An instance of XPath, created lazily on demand. */
    private XPath mXPath;

    public XmlStringFileHelper() {
    }

    /**
     * Utility method used by the wizard to retrieve the actual value definition of a given
     * string ID.
     *
     * @param project The project contain the XML file.
     * @param xmlFileWsPath The project path of the XML file, e.g. "/res/values/strings.xml".
     *          The given file may or may not exist.
     * @param stringId The string ID to find.
     * @return The value string if the ID is defined, null otherwise.
     */
    public String valueOfStringId(IProject project, String xmlFileWsPath, String stringId) {
        Map<String, String> cache = getResIdsForFile(project, xmlFileWsPath);
        return cache.get(stringId);
    }

    /**
     * Utility method that retrieves all the *string* IDs defined in the given Android resource
     * file. The instance maintains an internal cache so a given file is retrieved only once.
     * Callers should consider the set to be read-only.
     *
     * @param project The project contain the XML file.
     * @param xmlFileWsPath The project path of the XML file, e.g. "/res/values/strings.xml".
     *          The given file may or may not exist.
     * @return The map of string IDs => values defined in the given file. Cached. Never null.
     */
    public Map<String, String> getResIdsForFile(IProject project, String xmlFileWsPath) {
        Map<String, String> cache = mResIdCache.get(xmlFileWsPath);
        if (cache == null) {
            cache = internalGetResIdsForFile(project, xmlFileWsPath);
            mResIdCache.put(xmlFileWsPath, cache);
        }
        return cache;
    }

    /**
     * Extract all the defined string IDs from a given file using XPath.
     * @param project The project contain the XML file.
     * @param xmlFileWsPath The project path of the file to parse. It may not exist.
     * @return The map of all string IDs => values defined in the file.
     *   The returned set is always non null. It is empty if the file does not exist.
     */
    private Map<String, String> internalGetResIdsForFile(IProject project, String xmlFileWsPath) {
        TreeMap<String, String> ids = new TreeMap<String, String>();

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

                String xpathExpr = "/resources/string";                         //$NON-NLS-1$

                Object result = mXPath.evaluate(xpathExpr, source, XPathConstants.NODESET);
                if (result instanceof NodeList) {
                    NodeList list = (NodeList) result;
                    for (int n = list.getLength() - 1; n >= 0; n--) {
                        Node strNode = list.item(n);
                        NamedNodeMap attrs = strNode.getAttributes();
                        Node nameAttr = attrs.getNamedItem("name");             //$NON-NLS-1$
                        if (nameAttr != null) {
                            String id = nameAttr.getNodeValue();
                            String text = strNode.getTextContent();
                            ids.put(id, text);
                        }
                    }
                }

            } catch (CoreException e1) {
                // IFile.getContents failed. Ignore.
            } catch (XPathExpressionException e2) {
                // mXPath.evaluate failed. Ignore.
            }
        }

        return ids;
    }

}
