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

package com.android.ide.eclipse.adt.internal.editors;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.adt.internal.editors.descriptors.SeparatorAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.TextValueDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiFlagAttributeNode;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Content Assist Processor for Android XML files
 */
public abstract class AndroidContentAssist implements IContentAssistProcessor {

    /** Regexp to detect a full attribute after an element tag.
     * <pre>Syntax:
     *    name = "..." quoted string with all but < and "
     * or:
     *    name = '...' quoted string with all but < and '
     * </pre>
     */
    private static Pattern sFirstAttribute = Pattern.compile(
            "^ *[a-zA-Z_:]+ *= *(?:\"[^<\"]*\"|'[^<']*')");  //$NON-NLS-1$

    /** Regexp to detect an element tag name */
    private static Pattern sFirstElementWord = Pattern.compile("^[a-zA-Z0-9_:]+"); //$NON-NLS-1$

    /** Regexp to detect whitespace */
    private static Pattern sWhitespace = Pattern.compile("\\s+"); //$NON-NLS-1$

    protected final static String ROOT_ELEMENT = "";

    /** Descriptor of the root of the XML hierarchy. This a "fake" ElementDescriptor which
     *  is used to list all the possible roots given by actual implementations.
     *  DO NOT USE DIRECTLY. Call {@link #getRootDescriptor()} instead. */
    private ElementDescriptor mRootDescriptor;

    private final int mDescriptorId;

    private AndroidEditor mEditor;

    /**
     * Constructor for AndroidContentAssist
     * @param descriptorId An id for {@link AndroidTargetData#getDescriptorProvider(int)}.
     *      The Id can be one of {@link AndroidTargetData#DESCRIPTOR_MANIFEST},
     *      {@link AndroidTargetData#DESCRIPTOR_LAYOUT},
     *      {@link AndroidTargetData#DESCRIPTOR_MENU},
     *      or {@link AndroidTargetData#DESCRIPTOR_XML}.
     *      All other values will throw an {@link IllegalArgumentException} later at runtime.
     */
    public AndroidContentAssist(int descriptorId) {
        mDescriptorId = descriptorId;
    }

    /**
     * Returns a list of completion proposals based on the
     * specified location within the document that corresponds
     * to the current cursor position within the text viewer.
     *
     * @param viewer the viewer whose document is used to compute the proposals
     * @param offset an offset within the document for which completions should be computed
     * @return an array of completion proposals or <code>null</code> if no proposals are possible
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

        if (mEditor == null) {
            mEditor = getAndroidEditor(viewer);
            if (mEditor == null) {
                // This should not happen. Duck and forget.
                AdtPlugin.log(IStatus.ERROR, "Editor not found during completion");
                return null;
            }
        }

        UiElementNode rootUiNode = mEditor.getUiRootNode();

        Object[] choices = null; /* An array of ElementDescriptor, or AttributeDescriptor
                                    or String or null */
        String parent = "";      //$NON-NLS-1$
        String wordPrefix = extractElementPrefix(viewer, offset);
        char needTag = 0;
        boolean isElement = false;
        boolean isAttribute = false;

        Node currentNode = getNode(viewer, offset);
        if (currentNode == null)
            return null;

        // check to see if we can find a UiElementNode matching this XML node
        UiElementNode currentUiNode =
            rootUiNode == null ? null : rootUiNode.findXmlNode(currentNode);

        if (currentNode == null) {
            // Should not happen (an XML doc always has at least a doc node). Just give up.
            return null;
        }

        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
            parent = currentNode.getNodeName();

            if (wordPrefix.equals(parent)) {
                // We are still editing the element's tag name, not the attributes
                // (the element's tag name may not even be complete)
                isElement = true;
                choices = getChoicesForElement(parent, currentNode);
            } else {
                // We're not editing the current node name, so we might be editing its
                // attributes instead...
                isAttribute = true;
                AttribInfo info = parseAttributeInfo(viewer, offset);
                if (info != null) {
                    // We're editing attributes in an element node (either the attributes' names
                    // or their values).
                    choices = getChoicesForAttribute(parent, currentNode, currentUiNode, info);

                    if (info.correctedPrefix != null) {
                        wordPrefix = info.correctedPrefix;
                    }
                    needTag = info.needTag;
                }
            }
        } else if (currentNode.getNodeType() == Node.TEXT_NODE) {
            isElement = true;
            // Examine the parent of the text node.
            choices = getChoicesForTextNode(currentNode);
        }

        // Abort if we can't recognize the context or there are no completion choices
        if (choices == null || choices.length == 0) return null;

        if (isElement) {
            // If we found some suggestions, do we need to add an opening "<" bracket
            // for the element? We don't if the cursor is right after "<" or "</".
            // Per XML Spec, there's no whitespace between "<" or "</" and the tag name.
            int offset2 = offset - wordPrefix.length() - 1;
            int c1 = extractChar(viewer, offset2);
            if (!((c1 == '<') || (c1 == '/' && extractChar(viewer, offset2 - 1) == '<'))) {
                needTag = '<';
            }
        }

        // get the selection length
        int selectionLength = 0;
        ISelection selection = viewer.getSelectionProvider().getSelection();
        if (selection instanceof TextSelection) {
            TextSelection textSelection = (TextSelection)selection;
            selectionLength = textSelection.getLength();
        }

        return computeProposals(offset, currentNode, choices, wordPrefix, needTag,
                isAttribute, selectionLength);
    }

    /**
     * Returns the namespace prefix matching the Android Resource URI.
     * If no such declaration is found, returns the default "android" prefix.
     *
     * @param node The current node. Must not be null.
     * @param nsUri The namespace URI of which the prefix is to be found,
     *              e.g. {@link SdkConstants#NS_RESOURCES}
     * @return The first prefix declared or the default "android" prefix.
     */
    private String lookupNamespacePrefix(Node node, String nsUri) {
        // Note: Node.lookupPrefix is not implemented in wst/xml/core NodeImpl.java
        // The following emulates this:
        //   String prefix = node.lookupPrefix(SdkConstants.NS_RESOURCES);

        if (XmlnsAttributeDescriptor.XMLNS_URI.equals(nsUri)) {
            return "xmlns"; //$NON-NLS-1$
        }

        HashSet<String> visited = new HashSet<String>();

        String prefix = null;
        for (; prefix == null &&
                    node != null &&
                    node.getNodeType() == Node.ELEMENT_NODE;
               node = node.getParentNode()) {
            NamedNodeMap attrs = node.getAttributes();
            for (int n = attrs.getLength() - 1; n >= 0; --n) {
                Node attr = attrs.item(n);
                if ("xmlns".equals(attr.getPrefix())) {  //$NON-NLS-1$
                    String uri = attr.getNodeValue();
                    if (SdkConstants.NS_RESOURCES.equals(uri)) {
                        return attr.getLocalName();
                    }
                    visited.add(uri);
                }
            }
        }

        // Use a sensible default prefix if we can't find one.
        // We need to make sure the prefix is not one that was declared in the scope
        // visited above.
        prefix = SdkConstants.NS_RESOURCES.equals(nsUri) ? "android" : "ns"; //$NON-NLS-1$ //$NON-NLS-2$
        String base = prefix;
        for (int i = 1; visited.contains(prefix); i++) {
            prefix = base + Integer.toString(i);
        }
        return prefix;
    }

    /**
     * Gets the choices when the user is editing the name of an XML element.
     * <p/>
     * The user is editing the name of an element (the "parent").
     * Find the grand-parent and if one is found, return its children element list.
     * The name which is being edited should be one of those.
     * <p/>
     * Example: <manifest><applic*cursor* => returns the list of all elements that
     * can be found under <manifest>, of which <application> is one of the choices.
     *
     * @return an ElementDescriptor[] or null if no valid element was found.
     */
    private Object[] getChoicesForElement(String parent, Node current_node) {
        ElementDescriptor grandparent = null;
        if (current_node.getParentNode().getNodeType() == Node.ELEMENT_NODE) {
            grandparent = getDescriptor(current_node.getParentNode().getNodeName());
        } else if (current_node.getParentNode().getNodeType() == Node.DOCUMENT_NODE) {
            grandparent = getRootDescriptor();
        }
        if (grandparent != null) {
            for (ElementDescriptor e : grandparent.getChildren()) {
                if (e.getXmlName().startsWith(parent)) {
                    return grandparent.getChildren();
                }
            }
        }

        return null;
    }

    /**
     * Gets the choices when the user is editing an XML attribute.
     * <p/>
     * In input, attrInfo contains details on the analyzed context, namely whether the
     * user is editing an attribute value (isInValue) or an attribute name.
     * <p/>
     * In output, attrInfo also contains two possible new values (this is a hack to circumvent
     * the lack of out-parameters in Java):
     * - AttribInfo.correctedPrefix if the user has been editing an attribute value and it has
     *   been detected that what the user typed is different from what extractElementPrefix()
     *   predicted. This happens because extractElementPrefix() stops when a character that
     *   cannot be an element name appears whereas parseAttributeInfo() uses a grammar more
     *   lenient as suitable for attribute values.
     * - AttribInfo.needTag will be non-zero if we find that the attribute completion proposal
     *   must be double-quoted.
     * @param currentUiNode
     *
     * @return an AttributeDescriptor[] if the user is editing an attribute name.
     *         a String[] if the user is editing an attribute value with some known values,
     *         or null if nothing is known about the context.
     */
    private Object[] getChoicesForAttribute(String parent,
            Node currentNode, UiElementNode currentUiNode, AttribInfo attrInfo) {
        Object[] choices = null;
        if (attrInfo.isInValue) {
            // Editing an attribute's value... Get the attribute name and then the
            // possible choices for the tuple(parent,attribute)
            String value = attrInfo.value;
            if (value.startsWith("'") || value.startsWith("\"")) {   //$NON-NLS-1$   //$NON-NLS-2$
                value = value.substring(1);
                // The prefix that was found at the beginning only scan for characters
                // valid for tag name. We now know the real prefix for this attribute's
                // value, which is needed to generate the completion choices below.
                attrInfo.correctedPrefix = value;
            } else {
                attrInfo.needTag = '"';
            }

            if (currentUiNode != null) {
                // look for an UI attribute matching the current attribute name
                String attrName = attrInfo.name;
                // remove any namespace prefix from the attribute name
                int pos = attrName.indexOf(':');
                if (pos >= 0) {
                    attrName = attrName.substring(pos + 1);
                }

                UiAttributeNode currAttrNode = null;
                for (UiAttributeNode attrNode : currentUiNode.getUiAttributes()) {
                    if (attrNode.getDescriptor().getXmlLocalName().equals(attrName)) {
                        currAttrNode = attrNode;
                        break;
                    }
                }

                if (currAttrNode != null) {
                    choices = currAttrNode.getPossibleValues(value);

                    if (currAttrNode instanceof UiFlagAttributeNode) {
                        // A "flag" can consist of several values separated by "or" (|).
                        // If the correct prefix contains such a pipe character, we change
                        // it so that only the currently edited value is completed.
                        pos = value.indexOf('|');
                        if (pos >= 0) {
                            attrInfo.correctedPrefix = value = value.substring(pos + 1);
                            attrInfo.needTag = 0;
                        }
                    }
                }
            }

            if (choices == null) {
                // fallback on the older descriptor-only based lookup.

                // in order to properly handle the special case of the name attribute in
                // the action tag, we need the grandparent of the action node, to know
                // what type of actions we need.
                // e.g. activity -> intent-filter -> action[@name]
                String greatGrandParentName = null;
                Node grandParent = currentNode.getParentNode();
                if (grandParent != null) {
                    Node greatGrandParent = grandParent.getParentNode();
                    if (greatGrandParent != null) {
                        greatGrandParentName = greatGrandParent.getLocalName();
                    }
                }

                AndroidTargetData data = mEditor.getTargetData();
                if (data != null) {
                    choices = data.getAttributeValues(parent, attrInfo.name, greatGrandParentName);
                }
            }
        } else {
            // Editing an attribute's name... Get attributes valid for the parent node.
            if (currentUiNode != null) {
                choices = currentUiNode.getAttributeDescriptors();
            } else {
                ElementDescriptor parent_desc = getDescriptor(parent);
                choices = parent_desc.getAttributes();
            }
        }
        return choices;
    }

    /**
     * Gets the choices when the user is editing an XML text node.
     * <p/>
     * This means the user is editing outside of any XML element or attribute.
     * Simply return the list of XML elements that can be present there, based on the
     * parent of the current node.
     *
     * @return An ElementDescriptor[] or null.
     */
    private Object[] getChoicesForTextNode(Node currentNode) {
        Object[] choices = null;
        String parent;
        Node parent_node = currentNode.getParentNode();
        if (parent_node.getNodeType() == Node.ELEMENT_NODE) {
            // We're editing a text node which parent is an element node. Limit
            // content assist to elements valid for the parent.
            parent = parent_node.getNodeName();
            ElementDescriptor desc = getDescriptor(parent);
            if (desc != null) {
                choices = desc.getChildren();
            }
        } else if (parent_node.getNodeType() == Node.DOCUMENT_NODE) {
            // We're editing a text node at the first level (i.e. root node).
            // Limit content assist to the only valid root elements.
            choices = getRootDescriptor().getChildren();
        }
        return choices;
    }

    /**
     * Given a list of choices found, generates the proposals to be displayed to the user.
     * <p/>
     * Choices is an object array. Items of the array can be:
     * - ElementDescriptor: a possible element descriptor which XML name should be completed.
     * - AttributeDescriptor: a possible attribute descriptor which XML name should be completed.
     * - String: string values to display as-is to the user. Typically those are possible
     *           values for a given attribute.
     *
     * @return The ICompletionProposal[] to display to the user.
     */
    private ICompletionProposal[] computeProposals(int offset, Node currentNode,
            Object[] choices, String wordPrefix, char need_tag,
            boolean is_attribute, int selectionLength) {
        ArrayList<CompletionProposal> proposals = new ArrayList<CompletionProposal>();
        HashMap<String, String> nsUriMap = new HashMap<String, String>();

        for (Object choice : choices) {
            String keyword = null;
            String nsPrefix = null;
            Image icon = null;
            String tooltip = null;
            if (choice instanceof ElementDescriptor) {
                keyword = ((ElementDescriptor)choice).getXmlName();
                icon    = ((ElementDescriptor)choice).getIcon();
                tooltip = DescriptorsUtils.formatTooltip(((ElementDescriptor)choice).getTooltip());
            } else if (choice instanceof TextValueDescriptor) {
                continue; // Value nodes are not part of the completion choices
            } else if (choice instanceof SeparatorAttributeDescriptor) {
                continue; // not real attribute descriptors
            } else if (choice instanceof AttributeDescriptor) {
                keyword = ((AttributeDescriptor)choice).getXmlLocalName();
                icon    = ((AttributeDescriptor)choice).getIcon();
                if (choice instanceof TextAttributeDescriptor) {
                    tooltip = ((TextAttributeDescriptor) choice).getTooltip();
                }

                // Get the namespace URI for the attribute. Note that some attributes
                // do not have a namespace and thus return null here.
                String nsUri = ((AttributeDescriptor)choice).getNamespaceUri();
                if (nsUri != null) {
                    nsPrefix = nsUriMap.get(nsUri);
                    if (nsPrefix == null) {
                        nsPrefix = lookupNamespacePrefix(currentNode, nsUri);
                        nsUriMap.put(nsUri, nsPrefix);
                    }
                }
                if (nsPrefix != null) {
                    nsPrefix += ":"; //$NON-NLS-1$
                }

            } else if (choice instanceof String) {
                keyword = (String) choice;
            } else {
                continue; // discard unknown choice
            }

            String nsKeyword = nsPrefix == null ? keyword : (nsPrefix + keyword);

            if (keyword.startsWith(wordPrefix) ||
                    (nsPrefix != null && keyword.startsWith(nsPrefix)) ||
                    (nsPrefix != null && nsKeyword.startsWith(wordPrefix))) {
                if (nsPrefix != null) {
                    keyword = nsPrefix + keyword;
                }
                String end_tag = ""; //$NON-NLS-1$
                if (need_tag != 0) {
                    if (need_tag == '"') {
                        keyword = need_tag + keyword;
                        end_tag = String.valueOf(need_tag);
                    } else if (need_tag == '<') {
                        if (elementCanHaveChildren(choice)) {
                            end_tag = String.format("></%1$s>", keyword);  //$NON-NLS-1$
                            keyword = need_tag + keyword;
                        } else {
                            keyword = need_tag + keyword;
                            end_tag = "/>";  //$NON-NLS-1$
                        }
                    }
                }
                CompletionProposal proposal = new CompletionProposal(
                        keyword + end_tag,                  // String replacementString
                        offset - wordPrefix.length(),           // int replacementOffset
                        wordPrefix.length() + selectionLength,  // int replacementLength
                        keyword.length(),                   // int cursorPosition (rel. to rplcmntOffset)
                        icon,                               // Image image
                        null,                               // String displayString
                        null,                               // IContextInformation contextInformation
                        tooltip                             // String additionalProposalInfo
                        );

                proposals.add(proposal);
            }
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    /**
     * Indicates whether this descriptor describes an element that can potentially
     * have children (either sub-elements or text value). If an element can have children,
     * we want to explicitly write an opening and a separate closing tag.
     * <p/>
     * Elements can have children if the descriptor has children element descriptors
     * or if one of the attributes is a TextValueDescriptor.
     *
     * @param descriptor An ElementDescriptor or an AttributeDescriptor
     * @return True if the descriptor is an ElementDescriptor that can have children or a text value
     */
    private boolean elementCanHaveChildren(Object descriptor) {
        if (descriptor instanceof ElementDescriptor) {
            ElementDescriptor desc = (ElementDescriptor) descriptor;
            if (desc.hasChildren()) {
                return true;
            }
            for (AttributeDescriptor attr_desc : desc.getAttributes()) {
                if (attr_desc instanceof TextValueDescriptor) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the element descriptor matching a given XML node name or null if it can't be
     * found.
     * <p/>
     * This is simplistic; ideally we should consider the parent's chain to make sure we
     * can differentiate between different hierarchy trees. Right now the first match found
     * is returned.
     */
    private ElementDescriptor getDescriptor(String nodeName) {
        return getRootDescriptor().findChildrenDescriptor(nodeName, true /* recursive */);
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    /**
     * Returns the characters which when entered by the user should
     * automatically trigger the presentation of possible completions.
     *
     * In our case, we auto-activate on opening tags and attributes namespace.
     *
     * @return the auto activation characters for completion proposal or <code>null</code>
     *      if no auto activation is desired
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[]{ '<', ':', '=' };
    }

    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    public String getErrorMessage() {
        return null;
    }

    /**
     * Heuristically extracts the prefix used for determining template relevance
     * from the viewer's document. The default implementation returns the String from
     * offset backwards that forms a potential XML element name, attribute name or
     * attribute value.
     *
     * The part were we access the docment was extracted from
     * org.eclipse.jface.text.templatesTemplateCompletionProcessor and adapted to our needs.
     *
     * @param viewer the viewer
     * @param offset offset into document
     * @return the prefix to consider
     */
    protected String extractElementPrefix(ITextViewer viewer, int offset) {
        int i = offset;
        IDocument document = viewer.getDocument();
        if (i > document.getLength()) return ""; //$NON-NLS-1$

        try {
            for (; i > 0; --i) {
                char ch = document.getChar(i - 1);

                // We want all characters that can form a valid:
                // - element name, e.g. anything that is a valid Java class/variable literal.
                // - attribute name, including : for the namespace
                // - attribute value.
                // Before we were inclusive and that made the code fragile. So now we're
                // going to be exclusive: take everything till we get one of:
                // - any form of whitespace
                // - any xml separator, e.g. < > ' " and =
                if (Character.isWhitespace(ch) ||
                        ch == '<' || ch == '>' || ch == '\'' || ch == '"' || ch == '=') {
                    break;
                }
            }

            return document.get(i, offset - i);
        } catch (BadLocationException e) {
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Extracts the character at the given offset.
     * Returns 0 if the offset is invalid.
     */
    protected char extractChar(ITextViewer viewer, int offset) {
        IDocument document = viewer.getDocument();
        if (offset > document.getLength()) return 0;

        try {
            return document.getChar(offset);
        } catch (BadLocationException e) {
            return 0;
        }
    }

    /**
     * Information about the current edit of an attribute as reported by parseAttributeInfo.
     */
    private class AttribInfo {
        /** True if the cursor is located in an attribute's value, false if in an attribute name */
        public boolean isInValue = false;
        /** The attribute name. Null when not set. */
        public String name = null;
        /** The attribute value. Null when not set. The value *may* start with a quote
         *  (' or "), in which case we know we don't need to quote the string for the user */
        public String value = null;
        /** String typed by the user so far (i.e. right before requesting code completion),
         *  which will be corrected if we find a possible completion for an attribute value.
         *  See the long comment in getChoicesForAttribute(). */
        public String correctedPrefix = null;
        /** Non-zero if an attribute value need a start/end tag (i.e. quotes or brackets) */
        public char needTag = 0;
    }


    /**
     * Try to guess if the cursor is editing an element's name or an attribute following an
     * element. If it's an attribute, try to find if an attribute name is being defined or
     * its value.
     * <br/>
     * This is currently *only* called when we know the cursor is after a complete element
     * tag name, so it should never return null.
     * <br/>
     * Reference for XML syntax: http://www.w3.org/TR/2006/REC-xml-20060816/#sec-starttags
     * <br/>
     * @return An AttribInfo describing which attribute is being edited or null if the cursor is
     *         not editing an attribute (in which case it must be an element's name).
     */
    private AttribInfo parseAttributeInfo(ITextViewer viewer, int offset) {
        AttribInfo info = new AttribInfo();

        IDocument document = viewer.getDocument();
        int n = document.getLength();
        if (offset <= n) {
            try {
                n = offset;
                for (;offset > 0; --offset) {
                    char ch = document.getChar(offset - 1);
                    if (ch == '<') break;
                }

                // text will contain the full string of the current element,
                // i.e. whatever is after the "<" to the current cursor
                String text = document.get(offset, n - offset);

                // Normalize whitespace to single spaces
                text = sWhitespace.matcher(text).replaceAll(" "); //$NON-NLS-1$

                // Remove the leading element name. By spec, it must be after the < without
                // any whitespace. If there's nothing left, no attribute has been defined yet.
                // Be sure to keep any whitespace after the initial word if any, as it matters.
                text = sFirstElementWord.matcher(text).replaceFirst("");  //$NON-NLS-1$

                // There MUST be space after the element name. If not, the cursor is still
                // defining the element name.
                if (!text.startsWith(" ")) { //$NON-NLS-1$
                    return null;
                }

                // Remove full attributes:
                // Syntax:
                //    name = "..." quoted string with all but < and "
                // or:
                //    name = '...' quoted string with all but < and '
                String temp;
                do {
                    temp = text;
                    text = sFirstAttribute.matcher(temp).replaceFirst("");  //$NON-NLS-1$
                } while(!temp.equals(text));

                // Now we're left with 3 cases:
                // - nothing: either there is no attribute definition or the cursor located after
                //   a completed attribute definition.
                // - a string with no =: the user is writing an attribute name. This case can be
                //   merged with the previous one.
                // - string with an = sign, optionally followed by a quote (' or "): the user is
                //   writing the value of the attribute.
                int pos_equal = text.indexOf('=');
                if (pos_equal == -1) {
                    info.isInValue = false;
                    info.name = text.trim();
                } else {
                    info.isInValue = true;
                    info.name = text.substring(0, pos_equal).trim();
                    info.value = text.substring(pos_equal + 1).trim();
                }
                return info;
            } catch (BadLocationException e) {
                // pass
            }
        }

        return null;
    }


    /**
     * Returns the XML DOM node corresponding to the given offset of the given document.
     */
    protected Node getNode(ITextViewer viewer, int offset) {
        Node node = null;
        try {
            IModelManager mm = StructuredModelManager.getModelManager();
            if (mm != null) {
                IStructuredModel model = mm.getExistingModelForRead(viewer.getDocument());
                if (model != null) {
                    for(; offset >= 0 && node == null; --offset) {
                        node = (Node) model.getIndexedRegion(offset);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore exceptions.
        }

        return node;
    }

    /**
     * Computes (if needed) and returns the root descriptor.
     */
    private ElementDescriptor getRootDescriptor() {
        if (mRootDescriptor == null) {
            AndroidTargetData data = mEditor.getTargetData();
            if (data != null) {
                IDescriptorProvider descriptorProvider = data.getDescriptorProvider(mDescriptorId);

                if (descriptorProvider != null) {
                    mRootDescriptor = new ElementDescriptor("",     //$NON-NLS-1$
                            descriptorProvider.getRootElementDescriptors());
                }
            }
        }

        return mRootDescriptor;
    }

    /**
     * Returns the active {@link AndroidEditor} matching this source viewer.
     */
    private AndroidEditor getAndroidEditor(ITextViewer viewer) {
        IWorkbenchWindow wwin = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (wwin != null) {
            IWorkbenchPage page = wwin.getActivePage();
            if (page != null) {
                IEditorPart editor = page.getActiveEditor();
                if (editor instanceof AndroidEditor) {
                    ISourceViewer ssviewer = ((AndroidEditor) editor).getStructuredSourceViewer();
                    if (ssviewer == viewer) {
                        return (AndroidEditor) editor;
                    }
                }
            }
        }

        return null;
    }



}
