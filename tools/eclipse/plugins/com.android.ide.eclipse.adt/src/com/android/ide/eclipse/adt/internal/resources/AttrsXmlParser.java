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

package com.android.ide.eclipse.adt.internal.resources;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.resources.DeclareStyleableInfo.AttributeInfo;
import com.android.ide.eclipse.adt.internal.resources.DeclareStyleableInfo.AttributeInfo.Format;
import com.android.ide.eclipse.adt.internal.resources.ViewClassInfo.LayoutParamsInfo;

import org.eclipse.core.runtime.IStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Parser for attributes description files.
 */
public final class AttrsXmlParser {

    private Document mDocument;
    private String mOsAttrsXmlPath;
    // all attributes that have the same name are supposed to have the same
    // parameters so we'll keep a cache of them to avoid processing them twice.
    private HashMap<String, AttributeInfo> mAttributeMap;

    /** Map of all attribute names for a given element */
    private HashMap<String, DeclareStyleableInfo> mStyleMap;
    
    /** Map of all (constant, value) pairs for attributes of format enum or flag.
     * E.g. for attribute name=gravity, this tells us there's an enum/flag called "center"
     * with value 0x11. 
     */
    private Map<String, Map<String, Integer>> mEnumFlagValues;
    
    
    /**
     * Creates a new {@link AttrsXmlParser}, set to load things from the given
     * XML file. Nothing has been parsed yet. Callers should call {@link #preload()}
     * next.
     */
    public AttrsXmlParser(String osAttrsXmlPath) {
        this(osAttrsXmlPath, null /* inheritableAttributes */);
    }

    /**
     * Creates a new {@link AttrsXmlParser} set to load things from the given
     * XML file. If inheritableAttributes is non-null, it must point to a preloaded
     * {@link AttrsXmlParser} which attributes will be used for this one. Since
     * already defined attributes are not modifiable, they are thus "inherited".
     */
    public AttrsXmlParser(String osAttrsXmlPath, AttrsXmlParser inheritableAttributes) {
        mOsAttrsXmlPath = osAttrsXmlPath;

        // styles are not inheritable.
        mStyleMap = new HashMap<String, DeclareStyleableInfo>();

        if (inheritableAttributes == null) {
            mAttributeMap = new HashMap<String, AttributeInfo>();
            mEnumFlagValues = new HashMap<String, Map<String,Integer>>();
        } else {
            mAttributeMap = new HashMap<String, AttributeInfo>(inheritableAttributes.mAttributeMap);
            mEnumFlagValues = new HashMap<String, Map<String,Integer>>(
                                                             inheritableAttributes.mEnumFlagValues);
        }
    }

    /**
     * @return The OS path of the attrs.xml file parsed
     */
    public String getOsAttrsXmlPath() {
        return mOsAttrsXmlPath;
    }
    
    /**
     * Preloads the document, parsing all attributes and declared styles.
     * 
     * @return Self, for command chaining.
     */
    public AttrsXmlParser preload() {
        Document doc = getDocument();

        if (doc == null) {
            AdtPlugin.log(IStatus.WARNING, "Failed to find %1$s", //$NON-NLS-1$
                    mOsAttrsXmlPath);
            return this;
        }

        Node res = doc.getFirstChild();
        while (res != null &&
                res.getNodeType() != Node.ELEMENT_NODE &&
                !res.getNodeName().equals("resources")) { //$NON-NLS-1$
            res = res.getNextSibling();
        }
        
        if (res == null) {
            AdtPlugin.log(IStatus.WARNING, "Failed to find a <resources> node in %1$s", //$NON-NLS-1$
                    mOsAttrsXmlPath);
            return this;
        }
        
        parseResources(res);
        return this;
    }

    /**
     * Loads all attributes & javadoc for the view class info based on the class name.
     */
    public void loadViewAttributes(ViewClassInfo info) {
        if (getDocument() != null) {
            String xmlName = info.getShortClassName();
            DeclareStyleableInfo style = mStyleMap.get(xmlName);
            if (style != null) {
                info.setAttributes(style.getAttributes());
                info.setJavaDoc(style.getJavaDoc());
            }
        }
    }

    /**
     * Loads all attributes for the layout data info based on the class name.
     */
    public void loadLayoutParamsAttributes(LayoutParamsInfo info) {
        if (getDocument() != null) {
            // Transforms "LinearLayout" and "LayoutParams" into "LinearLayout_Layout".
            String xmlName = String.format("%1$s_%2$s", //$NON-NLS-1$
                    info.getViewLayoutClass().getShortClassName(),
                    info.getShortClassName());
            xmlName = xmlName.replaceFirst("Params$", ""); //$NON-NLS-1$ //$NON-NLS-2$

            DeclareStyleableInfo style = mStyleMap.get(xmlName);
            if (style != null) {
                info.setAttributes(style.getAttributes());
            }
        }
    }
    
    /**
     * Returns a list of all decleare-styleable found in the xml file.
     */
    public Map<String, DeclareStyleableInfo> getDeclareStyleableList() {
        return Collections.unmodifiableMap(mStyleMap);
    }
    
    /**
     * Returns a map of all enum and flag constants sorted by parent attribute name.
     * The map is attribute_name => (constant_name => integer_value).
     */
    public Map<String, Map<String, Integer>> getEnumFlagValues() {
        return mEnumFlagValues;
    }

    //-------------------------

    /**
     * Creates an XML document from the attrs.xml OS path.
     * May return null if the file doesn't exist or cannot be parsed. 
     */
    private Document getDocument() {
        if (mDocument == null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(false);
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                mDocument = builder.parse(new File(mOsAttrsXmlPath));
            } catch (ParserConfigurationException e) {
                AdtPlugin.log(e, "Failed to create XML document builder for %1$s", //$NON-NLS-1$
                        mOsAttrsXmlPath);
            } catch (SAXException e) {
                AdtPlugin.log(e, "Failed to parse XML document %1$s", //$NON-NLS-1$
                        mOsAttrsXmlPath);
            } catch (IOException e) {
                AdtPlugin.log(e, "Failed to read XML document %1$s", //$NON-NLS-1$
                        mOsAttrsXmlPath);
            }
        }
        return mDocument;
    }

    /**
     * Finds all the <declare-styleable> and <attr> nodes in the top <resources> node.
     */
    private void parseResources(Node res) {
        Node lastComment = null;
        for (Node node = res.getFirstChild(); node != null; node = node.getNextSibling()) {
            switch (node.getNodeType()) {
            case Node.COMMENT_NODE:
                lastComment = node;
                break;
            case Node.ELEMENT_NODE:
                if (node.getNodeName().equals("declare-styleable")) {          //$NON-NLS-1$
                    Node nameNode = node.getAttributes().getNamedItem("name"); //$NON-NLS-1$
                    if (nameNode != null) {
                        String name = nameNode.getNodeValue();
                        
                        Node parentNode = node.getAttributes().getNamedItem("parent"); //$NON-NLS-1$
                        String parents = parentNode == null ? null : parentNode.getNodeValue();
                        
                        if (name != null && !mStyleMap.containsKey(name)) {
                            DeclareStyleableInfo style = parseDeclaredStyleable(name, node);
                            if (parents != null) {
                                style.setParents(parents.split("[ ,|]"));  //$NON-NLS-1$
                            }
                            mStyleMap.put(name, style);
                            if (lastComment != null) {
                                style.setJavaDoc(parseJavadoc(lastComment.getNodeValue()));
                            }
                        }
                    }
                } else if (node.getNodeName().equals("attr")) {                //$NON-NLS-1$
                    parseAttr(node, lastComment);
                }
                lastComment = null;
                break;
            }
        }
    }

    /**
     * Parses an <attr> node and convert it into an {@link AttributeInfo} if it is valid.
     */
    private AttributeInfo parseAttr(Node attrNode, Node lastComment) {
        AttributeInfo info = null;
        Node nameNode = attrNode.getAttributes().getNamedItem("name"); //$NON-NLS-1$
        if (nameNode != null) {
            String name = nameNode.getNodeValue();
            if (name != null) {
                info = mAttributeMap.get(name);
                // If the attribute is unknown yet, parse it.
                // If the attribute is know but its format is unknown, parse it too.
                if (info == null || info.getFormats().length == 0) {
                    info = parseAttributeTypes(attrNode, name);
                    if (info != null) {
                        mAttributeMap.put(name, info);
                    }
                } else if (lastComment != null) {
                    info = new AttributeInfo(info);
                }
                if (info != null) {
                    if (lastComment != null) {
                        info.setJavaDoc(parseJavadoc(lastComment.getNodeValue()));
                        info.setDeprecatedDoc(parseDeprecatedDoc(lastComment.getNodeValue()));
                    }
                }
            }
        }
        return info;
    }

    /**
     * Finds all the attributes for a particular style node,
     * e.g. a declare-styleable of name "TextView" or "LinearLayout_Layout".
     * 
     * @param styleName The name of the declare-styleable node
     * @param declareStyleableNode The declare-styleable node itself 
     */
    private DeclareStyleableInfo parseDeclaredStyleable(String styleName,
            Node declareStyleableNode) {
        ArrayList<AttributeInfo> attrs = new ArrayList<AttributeInfo>();
        Node lastComment = null;
        for (Node node = declareStyleableNode.getFirstChild();
             node != null;
             node = node.getNextSibling()) {

            switch (node.getNodeType()) {
            case Node.COMMENT_NODE:
                lastComment = node;
                break;
            case Node.ELEMENT_NODE:
                if (node.getNodeName().equals("attr")) {                       //$NON-NLS-1$
                    AttributeInfo info = parseAttr(node, lastComment);
                    if (info != null) {
                        attrs.add(info);
                    }
                }
                lastComment = null;
                break;
            }
            
        }
        
        return new DeclareStyleableInfo(styleName, attrs.toArray(new AttributeInfo[attrs.size()]));
    }

    /**
     * Returns the {@link AttributeInfo} for a specific <attr> XML node.
     * This gets the javadoc, the type, the name and the enum/flag values if any.
     * <p/>
     * The XML node is expected to have the following attributes:
     * <ul>
     * <li>"name", which is mandatory. The node is skipped if this is missing.</li>
     * <li>"format".</li>
     * </ul>
     * The format may be one type or two types (e.g. "reference|color").
     * An extra format can be implied: "enum" or "flag" are not specified in the "format" attribute,
     * they are implicitely stated by the presence of sub-nodes <enum> or <flag>.
     * <p/>
     * By design, <attr> nodes of the same name MUST have the same type.
     * Attribute nodes are thus cached by name and reused as much as possible.
     * When reusing a node, it is duplicated and its javadoc reassigned. 
     */
    private AttributeInfo parseAttributeTypes(Node attrNode, String name) {
        TreeSet<AttributeInfo.Format> formats = new TreeSet<AttributeInfo.Format>();
        String[] enumValues = null;
        String[] flagValues = null;

        Node attrFormat = attrNode.getAttributes().getNamedItem("format"); //$NON-NLS-1$
        if (attrFormat != null) {
            for (String f : attrFormat.getNodeValue().split("\\|")) { //$NON-NLS-1$
                try {
                    Format format = AttributeInfo.Format.valueOf(f.toUpperCase());
                    // enum and flags are handled differently right below
                    if (format != null &&
                            format != AttributeInfo.Format.ENUM &&
                            format != AttributeInfo.Format.FLAG) {
                        formats.add(format);
                    }
                } catch (IllegalArgumentException e) {
                    AdtPlugin.log(e, "Unknown format name '%s' in <attr name=\"%s\">, file '%s'.", //$NON-NLS-1$
                            f, name, getOsAttrsXmlPath());
                }
            }
        }

        // does this <attr> have <enum> children?
        enumValues = parseEnumFlagValues(attrNode, "enum", name); //$NON-NLS-1$
        if (enumValues != null) {
            formats.add(AttributeInfo.Format.ENUM);
        }

        // does this <attr> have <flag> children?
        flagValues = parseEnumFlagValues(attrNode, "flag", name); //$NON-NLS-1$
        if (flagValues != null) {
            formats.add(AttributeInfo.Format.FLAG);
        }

        AttributeInfo info = new AttributeInfo(name,
                formats.toArray(new AttributeInfo.Format[formats.size()]));
        info.setEnumValues(enumValues);
        info.setFlagValues(flagValues);
        return info;
    }

    /**
     * Given an XML node that represents an <attr> node, this method searches
     * if the node has any children nodes named "target" (e.g. "enum" or "flag").
     * Such nodes must have a "name" attribute.
     * <p/>
     * If "attrNode" is null, look for any <attr> that has the given attrNode
     * and the requested children nodes.
     * <p/>
     * This method collects all the possible names of these children nodes and
     * return them.
     * 
     * @param attrNode The <attr> XML node
     * @param filter The child node to look for, either "enum" or "flag".
     * @param attrName The value of the name attribute of <attr> 
     * 
     * @return Null if there are no such children nodes, otherwise an array of length >= 1
     *         of all the names of these children nodes.
     */
    private String[] parseEnumFlagValues(Node attrNode, String filter, String attrName) {
        ArrayList<String> names = null;
        for (Node child = attrNode.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(filter)) {
                Node nameNode = child.getAttributes().getNamedItem("name");  //$NON-NLS-1$
                if (nameNode == null) {
                    AdtPlugin.log(IStatus.WARNING,
                            "Missing name attribute in <attr name=\"%s\"><%s></attr>", //$NON-NLS-1$
                            attrName, filter);
                } else {
                    if (names == null) {
                        names = new ArrayList<String>();
                    }
                    String name = nameNode.getNodeValue();
                    names.add(name);
                    
                    Node valueNode = child.getAttributes().getNamedItem("value");  //$NON-NLS-1$
                    if (valueNode == null) {
                        AdtPlugin.log(IStatus.WARNING,
                                "Missing value attribute in <attr name=\"%s\"><%s name=\"%s\"></attr>", //$NON-NLS-1$
                                attrName, filter, name);
                    } else {
                        String value = valueNode.getNodeValue();
                        try {
                            int i = value.startsWith("0x") ?
                                    Integer.parseInt(value.substring(2), 16 /* radix */) :
                                    Integer.parseInt(value);
                            
                            Map<String, Integer> map = mEnumFlagValues.get(attrName);
                            if (map == null) {
                                map = new HashMap<String, Integer>();
                                mEnumFlagValues.put(attrName, map);
                            }
                            map.put(name, Integer.valueOf(i));
                            
                        } catch(NumberFormatException e) {
                            AdtPlugin.log(e,
                                    "Value in <attr name=\"%s\"><%s name=\"%s\" value=\"%s\"></attr> is not a valid decimal or hexadecimal", //$NON-NLS-1$
                                    attrName, filter, name, value);
                        }
                    }
                }
            }
        }
        return names == null ? null : names.toArray(new String[names.size()]);
    }
    
    /**
     * Parses the javadoc comment.
     * Only keeps the first sentence.
     * <p/>
     * This does not remove nor simplify links and references. Such a transformation
     * is done later at "display" time in {@link DescriptorsUtils#formatTooltip(String)} and co.
     */
    private String parseJavadoc(String comment) {
        if (comment == null) {
            return null;
        }
        
        // sanitize & collapse whitespace
        comment = comment.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$

        // Explicitly remove any @deprecated tags since they are handled separately.
        comment = comment.replaceAll("(?:\\{@deprecated[^}]*\\}|@deprecated[^@}]*)", "");

        // take everything up to the first dot that is followed by a space or the end of the line.
        // I love regexps :-). For the curious, the regexp is:
        // - start of line
        // - ignore whitespace
        // - group:
        //   - everything, not greedy
        //   - non-capturing group (?: )
        //      - end of string
        //      or
        //      - not preceded by a letter, a dot and another letter (for "i.e" and "e.g" )
        //                            (<! non-capturing zero-width negative look-behind)
        //      - a dot
        //      - followed by a space (?= non-capturing zero-width positive look-ahead)
        // - anything else is ignored
        comment = comment.replaceFirst("^\\s*(.*?(?:$|(?<![a-zA-Z]\\.[a-zA-Z])\\.(?=\\s))).*", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
        
        return comment;
    }


    /**
     * Parses the javadoc and extract the first @deprecated tag, if any.
     * Returns null if there's no @deprecated tag.
     * The deprecated tag can be of two forms:
     * - {+@deprecated ...text till the next bracket }
     *   Note: there should be no space or + between { and @. I need one in this comment otherwise
     *   this method will be tagged as deprecated ;-)
     * - @deprecated ...text till the next @tag or end of the comment.
     * In both cases the comment can be multi-line.
     */
    private String parseDeprecatedDoc(String comment) {
        // Skip if we can't even find the tag in the comment.
        if (comment == null) {
            return null;
        }
        
        // sanitize & collapse whitespace
        comment = comment.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$

        int pos = comment.indexOf("{@deprecated");
        if (pos >= 0) {
            comment = comment.substring(pos + 12 /* len of {@deprecated */);
            comment = comment.replaceFirst("^([^}]*).*", "$1");
        } else if ((pos = comment.indexOf("@deprecated")) >= 0) {
            comment = comment.substring(pos + 11 /* len of @deprecated */);
            comment = comment.replaceFirst("^(.*?)(?:@.*|$)", "$1");
        } else {
            return null;
        }
        
        return comment.trim();
    }
}
