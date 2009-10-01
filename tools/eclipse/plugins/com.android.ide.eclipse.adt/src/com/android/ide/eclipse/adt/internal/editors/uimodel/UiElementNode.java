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

package com.android.ide.eclipse.adt.internal.editors.uimodel;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.AndroidEditor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.SeparatorAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.CustomViewDescriptorService;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.LayoutDescriptors;
import com.android.ide.eclipse.adt.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.adt.internal.editors.resources.descriptors.ResourcesDescriptors;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener.UiUpdateState;
import com.android.ide.eclipse.adt.internal.editors.xml.descriptors.XmlDescriptors;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Represents an XML node that can be modified by the user interface in the XML editor.
 * <p/>
 * Each tree viewer used in the application page's parts needs to keep a model representing
 * each underlying node in the tree. This interface represents the base type for such a node.
 * <p/>
 * Each node acts as an intermediary model between the actual XML model (the real data support)
 * and the tree viewers or the corresponding page parts.
 * <p/>
 * Element nodes don't contain data per se. Their data is contained in their attributes
 * as well as their children's attributes, see {@link UiAttributeNode}.
 * <p/>
 * The structure of a given {@link UiElementNode} is declared by a corresponding
 * {@link ElementDescriptor}.
 * <p/>
 * The class implements {@link IPropertySource}, in order to fill the Eclipse property tab when
 * an element is selected. The {@link AttributeDescriptor} are used property descriptors.
 */
public class UiElementNode implements IPropertySource {

    /** List of prefixes removed from android:id strings when creating short descriptions. */
    private static String[] ID_PREFIXES = {
        "@android:id/", //$NON-NLS-1$
        "@+id/", "@id/", "@+", "@" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /** The element descriptor for the node. Always present, never null. */
    private ElementDescriptor mDescriptor;
    /** The parent element node in the UI model. It is null for a root element or until
     *  the node is attached to its parent. */
    private UiElementNode mUiParent;
    /** The {@link AndroidEditor} handling the UI hierarchy. This is defined only for the
     *  root node. All children have the value set to null and query their parent. */
    private AndroidEditor mEditor;
    /** The XML {@link Document} model that is being mirror by the UI model. This is defined
     *  only for the root node. All children have the value set to null and query their parent. */
    private Document mXmlDocument;
    /** The XML {@link Node} mirror by this UI node. This can be null for mandatory UI node which
     *  have no corresponding XML node or for new UI nodes before their XML node is set. */
    private Node mXmlNode;
    /** The list of all UI children nodes. Can be empty but never null. There's one UI children
     *  node per existing XML children node. */
    private ArrayList<UiElementNode> mUiChildren;
    /** The list of <em>all</em> UI attributes, as declared in the {@link ElementDescriptor}.
     *  The list is always defined and never null. Unlike the UiElementNode children list, this
     *  is always defined, even for attributes that do not exist in the XML model -- that's because
     *  "missing" attributes in the XML model simply mean a default value is used. Also note that
     *  the underlying collection is a map, so order is not respected. To get the desired attribute
     *  order, iterate through the {@link ElementDescriptor}'s attribute list. */
    private HashMap<AttributeDescriptor, UiAttributeNode> mUiAttributes;
    private HashSet<UiAttributeNode> mUnknownUiAttributes;
    /** A read-only view of the UI children node collection. */
    private List<UiElementNode> mReadOnlyUiChildren;
    /** A read-only view of the UI attributes collection. */
    private Collection<UiAttributeNode> mReadOnlyUiAttributes;
    /** A map of hidden attribute descriptors. Key is the XML name. */
    private Map<String, AttributeDescriptor> mCachedHiddenAttributes;
    /** An optional list of {@link IUiUpdateListener}. Most element nodes will not have any
     *  listeners attached, so the list is only created on demand and can be null. */
    private ArrayList<IUiUpdateListener> mUiUpdateListeners;
    /** Error Flag */
    private boolean mHasError;
    /** Temporary data used by the editors. This data is not sync'ed with the XML */
    private Object mEditData;

    /**
     * Creates a new {@link UiElementNode} described by a given {@link ElementDescriptor}.
     *
     * @param elementDescriptor The {@link ElementDescriptor} for the XML node. Cannot be null.
     */
    public UiElementNode(ElementDescriptor elementDescriptor) {
        mDescriptor = elementDescriptor;
        clearContent();
    }

    /**
     * Clears the {@link UiElementNode} by resetting the children list and
     * the {@link UiAttributeNode}s list.
     * Also resets the attached XML node, document, editor if any.
     * <p/>
     * The parent {@link UiElementNode} node is not reset so that it's position
     * in the hierarchy be left intact, if any.
     */
    /* package */ void clearContent() {
        mXmlNode = null;
        mXmlDocument = null;
        mEditor = null;
        clearAttributes();
        mReadOnlyUiChildren = null;
        if (mUiChildren == null) {
            mUiChildren = new ArrayList<UiElementNode>();
        } else {
            // We can't remove mandatory nodes, we just clear them.
            for (int i = mUiChildren.size() - 1; i >= 0; --i) {
                removeUiChildAtIndex(i);
            }
        }
    }

    /**
     * Clears the internal list of attributes, the read-only cached version of it
     * and the read-only cached hidden attribute list.
     */
    private void clearAttributes() {
        mUiAttributes = null;
        mReadOnlyUiAttributes = null;
        mCachedHiddenAttributes = null;
        mUnknownUiAttributes = new HashSet<UiAttributeNode>();
    }

    /**
     * Gets or creates the internal UiAttributes list.
     * <p/>
     * When the descriptor derives from ViewElementDescriptor, this list depends on the
     * current UiParent node.
     *
     * @return A new set of {@link UiAttributeNode} that matches the expected
     *         attributes for this node.
     */
    private HashMap<AttributeDescriptor, UiAttributeNode> getInternalUiAttributes() {
        if (mUiAttributes == null) {
            AttributeDescriptor[] attr_list = getAttributeDescriptors();
            mUiAttributes = new HashMap<AttributeDescriptor, UiAttributeNode>(attr_list.length);
            for (AttributeDescriptor desc : attr_list) {
                UiAttributeNode ui_node = desc.createUiNode(this);
                if (ui_node != null) {  // Some AttributeDescriptors do not have UI associated
                    mUiAttributes.put(desc, ui_node);
                }
            }
        }
        return mUiAttributes;
    }

    /**
     * Computes a short string describing the UI node suitable for tree views.
     * Uses the element's attribute "android:name" if present, or the "android:label" one
     * followed by the element's name.
     *
     * @return A short string describing the UI node suitable for tree views.
     */
    public String getShortDescription() {
        if (mXmlNode != null && mXmlNode instanceof Element && mXmlNode.hasAttributes()) {

            // Application and Manifest nodes have a special treatment: they are unique nodes
            // so we don't bother trying to differentiate their strings and we fall back to
            // just using the UI name below.
            Element elem = (Element) mXmlNode;

            String attr = elem.getAttributeNS(SdkConstants.NS_RESOURCES,
                                              AndroidManifestDescriptors.ANDROID_NAME_ATTR);
            if (attr == null || attr.length() == 0) {
                attr = elem.getAttributeNS(SdkConstants.NS_RESOURCES,
                                           AndroidManifestDescriptors.ANDROID_LABEL_ATTR);
            }
            if (attr == null || attr.length() == 0) {
                attr = elem.getAttributeNS(SdkConstants.NS_RESOURCES,
                                           XmlDescriptors.PREF_KEY_ATTR);
            }
            if (attr == null || attr.length() == 0) {
                attr = elem.getAttribute(ResourcesDescriptors.NAME_ATTR);
            }
            if (attr == null || attr.length() == 0) {
                attr = elem.getAttributeNS(SdkConstants.NS_RESOURCES,
                                           LayoutDescriptors.ID_ATTR);

                if (attr != null && attr.length() > 0) {
                    for (String prefix : ID_PREFIXES) {
                        if (attr.startsWith(prefix)) {
                            attr = attr.substring(prefix.length());
                            break;
                        }
                    }
                }
            }
            if (attr != null && attr.length() > 0) {
                return String.format("%1$s (%2$s)", attr, mDescriptor.getUiName());
            }
        }

        return String.format("%1$s", mDescriptor.getUiName());
    }

    /**
     * Computes a "breadcrumb trail" description for this node.
     * It will look something like "Manifest > Application > .myactivity (Activity) > Intent-Filter"
     *
     * @param include_root Whether to include the root (e.g. "Manifest") or not. Has no effect
     *                     when called on the root node itself.
     * @return The "breadcrumb trail" description for this node.
     */
    public String getBreadcrumbTrailDescription(boolean include_root) {
        StringBuilder sb = new StringBuilder(getShortDescription());

        for (UiElementNode ui_node = getUiParent();
                ui_node != null;
                ui_node = ui_node.getUiParent()) {
            if (!include_root && ui_node.getUiParent() == null) {
                break;
            }
            sb.insert(0, String.format("%1$s > ", ui_node.getShortDescription())); //$NON-NLS-1$
        }

        return sb.toString();
    }

    /**
     * Sets the XML {@link Document}.
     * <p/>
     * The XML {@link Document} is initially null. The XML {@link Document} must be set only on the
     * UI root element node (this method takes care of that.)
     */
    public void setXmlDocument(Document xml_doc) {
        if (mUiParent == null) {
            mXmlDocument = xml_doc;
        } else {
            mUiParent.setXmlDocument(xml_doc);
        }
    }

    /**
     * Returns the XML {@link Document}.
     * <p/>
     * The value is initially null until the UI node is attached to its UI parent -- the value
     * of the document is then propagated.
     *
     * @return the XML {@link Document} or the parent's XML {@link Document} or null.
     */
    public Document getXmlDocument() {
        if (mXmlDocument != null) {
            return mXmlDocument;
        } else if (mUiParent != null) {
            return mUiParent.getXmlDocument();
        }
        return null;
    }

    /**
     * Returns the XML node associated with this UI node.
     * <p/>
     * Some {@link ElementDescriptor} are declared as being "mandatory". This means the
     * corresponding UI node will exist even if there is no corresponding XML node. Such structure
     * is created and enforced by the parent of the tree, not the element themselves. However
     * such nodes will likely not have an XML node associated, so getXmlNode() can return null.
     *
     * @return The associated XML node. Can be null for mandatory nodes.
     */
    public Node getXmlNode() {
        return mXmlNode;
    }

    /**
     * Returns the {@link ElementDescriptor} for this node. This is never null.
     * <p/>
     * Do not use this to call getDescriptor().getAttributes(), instead call
     * getAttributeDescriptors() which can be overriden by derived classes.
     */
    public ElementDescriptor getDescriptor() {
        return mDescriptor;
    }

    /**
     * Returns the {@link AttributeDescriptor} array for the descriptor of this node.
     * <p/>
     * Use this instead of getDescriptor().getAttributes() -- derived classes can override
     * this to manipulate the attribute descriptor list depending on the current UI node.
     */
    public AttributeDescriptor[] getAttributeDescriptors() {
        return mDescriptor.getAttributes();
    }

    /**
     * Returns the hidden {@link AttributeDescriptor} array for the descriptor of this node.
     * This is a subset of the getAttributeDescriptors() list.
     * <p/>
     * Use this instead of getDescriptor().getHiddenAttributes() -- potentially derived classes
     * could override this to manipulate the attribute descriptor list depending on the current
     * UI node. There's no need for it right now so keep it private.
     */
    private Map<String, AttributeDescriptor> getHiddenAttributeDescriptors() {
        if (mCachedHiddenAttributes == null) {
            mCachedHiddenAttributes = new HashMap<String, AttributeDescriptor>();
            for (AttributeDescriptor attr_desc : getAttributeDescriptors()) {
                if (attr_desc instanceof XmlnsAttributeDescriptor) {
                    mCachedHiddenAttributes.put(
                            ((XmlnsAttributeDescriptor) attr_desc).getXmlNsName(),
                            attr_desc);
                }
            }
        }
        return mCachedHiddenAttributes;
    }

    /**
     * Sets the parent of this UiElementNode.
     * <p/>
     * The root node has no parent.
     */
    protected void setUiParent(UiElementNode parent) {
        mUiParent = parent;
        // Invalidate the internal UiAttributes list, as it may depend on the actual UiParent.
        clearAttributes();
    }

    /**
     * @return The parent {@link UiElementNode} or null if this is the root node.
     */
    public UiElementNode getUiParent() {
        return mUiParent;
    }

    /**
     * Returns The root {@link UiElementNode}.
     */
    public UiElementNode getUiRoot() {
        UiElementNode root = this;
        while (root.mUiParent != null) {
            root = root.mUiParent;
        }

        return root;
    }

    /**
     * Returns the previous UI sibling of this UI node.
     * If the node does not have a previous sibling, returns null.
     */
    public UiElementNode getUiPreviousSibling() {
        if (mUiParent != null) {
            List<UiElementNode> childlist = mUiParent.getUiChildren();
            if (childlist != null && childlist.size() > 1 && childlist.get(0) != this) {
                int index = childlist.indexOf(this);
                return index > 0 ? childlist.get(index - 1) : null;
            }
        }
        return null;
    }

    /**
     * Returns the next UI sibling of this UI node.
     * If the node does not have a next sibling, returns null.
     */
    public UiElementNode getUiNextSibling() {
        if (mUiParent != null) {
            List<UiElementNode> childlist = mUiParent.getUiChildren();
            if (childlist != null) {
                int size = childlist.size();
                if (size > 1 && childlist.get(size - 1) != this) {
                    int index = childlist.indexOf(this);
                    return index >= 0 && index < size - 1 ? childlist.get(index + 1) : null;
                }
            }
        }
        return null;
    }

    /**
     * Sets the {@link AndroidEditor} handling this {@link UiElementNode} hierarchy.
     * <p/>
     * The editor must always be set on the root node. This method takes care of that.
     */
    public void setEditor(AndroidEditor editor) {
        if (mUiParent == null) {
            mEditor = editor;
        } else {
            mUiParent.setEditor(editor);
        }
    }

    /**
     * Returns the {@link AndroidEditor} that embeds this {@link UiElementNode}.
     * <p/>
     * The value is initially null until the node is attached to its parent -- the value
     * of the root node is then propagated.
     *
     * @return The embedding {@link AndroidEditor} or null.
     */
    public AndroidEditor getEditor() {
        return mUiParent == null ? mEditor : mUiParent.getEditor();
    }

    /**
     * Returns the Android target data for the file being edited.
     */
    public AndroidTargetData getAndroidTarget() {
        return getEditor().getTargetData();
    }

    /**
     * @return A read-only version of the children collection.
     */
    public List<UiElementNode> getUiChildren() {
        if (mReadOnlyUiChildren == null) {
            mReadOnlyUiChildren = Collections.unmodifiableList(mUiChildren);
        }
        return mReadOnlyUiChildren;
    }

    /**
     * @return A read-only version of the attributes collection.
     */
    public Collection<UiAttributeNode> getUiAttributes() {
        if (mReadOnlyUiAttributes == null) {
            mReadOnlyUiAttributes = Collections.unmodifiableCollection(
                    getInternalUiAttributes().values());
        }
        return mReadOnlyUiAttributes;
    }

    /**
     * @return A read-only version of the unknown attributes collection.
     */
    public Collection<UiAttributeNode> getUnknownUiAttributes() {
        return Collections.unmodifiableCollection(mUnknownUiAttributes);
    }

    /**
     * Sets the error flag value.
     * @param errorFlag the error flag
     */
    public final void setHasError(boolean errorFlag) {
        mHasError = errorFlag;
    }

    /**
     * Returns whether this node, its attributes, or one of the children nodes (and attributes)
     * has errors.
     */
    public final boolean hasError() {
        if (mHasError) {
            return true;
        }

        // get the error value from the attributes.
        Collection<UiAttributeNode> attributes = getInternalUiAttributes().values();
        for (UiAttributeNode attribute : attributes) {
            if (attribute.hasError()) {
                return true;
            }
        }

        // and now from the children.
        for (UiElementNode child : mUiChildren) {
            if (child.hasError()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a new {@link IUiUpdateListener} to the internal update listener list.
     */
    public void addUpdateListener(IUiUpdateListener listener) {
       if (mUiUpdateListeners == null) {
           mUiUpdateListeners = new ArrayList<IUiUpdateListener>();
       }
       if (!mUiUpdateListeners.contains(listener)) {
           mUiUpdateListeners.add(listener);
       }
    }

    /**
     * Removes an existing {@link IUiUpdateListener} from the internal update listener list.
     * Does nothing if the list is empty or the listener is not registered.
     */
    public void removeUpdateListener(IUiUpdateListener listener) {
       if (mUiUpdateListeners != null) {
           mUiUpdateListeners.remove(listener);
       }
    }

    /**
     * Finds a child node relative to this node using a path-like expression.
     * F.ex. "node1/node2" would find a child "node1" that contains a child "node2" and
     * returns the latter. If there are multiple nodes with the same name at the same
     * level, always uses the first one found.
     *
     * @param path The path like expression to select a child node.
     * @return The ui node found or null.
     */
    public UiElementNode findUiChildNode(String path) {
        String[] items = path.split("/");  //$NON-NLS-1$
        UiElementNode ui_node = this;
        for (String item : items) {
            boolean next_segment = false;
            for (UiElementNode c : ui_node.mUiChildren) {
                if (c.getDescriptor().getXmlName().equals(item)) {
                    ui_node = c;
                    next_segment = true;
                    break;
                }
            }
            if (!next_segment) {
                return null;
            }
        }
        return ui_node;
    }

    /**
     * Finds an {@link UiElementNode} which contains the give XML {@link Node}.
     * Looks recursively in all children UI nodes.
     *
     * @param xmlNode The XML node to look for.
     * @return The {@link UiElementNode} that contains xmlNode or null if not found,
     */
    public UiElementNode findXmlNode(Node xmlNode) {
        if (xmlNode == null) {
            return null;
        }
        if (getXmlNode() == xmlNode) {
            return this;
        }

        for (UiElementNode uiChild : mUiChildren) {
            UiElementNode found = uiChild.findXmlNode(xmlNode);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Returns the {@link UiAttributeNode} matching this attribute descriptor or
     * null if not found.
     *
     * @param attr_desc The {@link AttributeDescriptor} to match.
     * @return the {@link UiAttributeNode} matching this attribute descriptor or null
     *         if not found.
     */
    public UiAttributeNode findUiAttribute(AttributeDescriptor attr_desc) {
        return getInternalUiAttributes().get(attr_desc);
    }

    /**
     * Populate this element node with all values from the given XML node.
     *
     * This fails if the given XML node has a different element name -- it won't change the
     * type of this ui node.
     *
     * This method can be both used for populating values the first time and updating values
     * after the XML model changed.
     *
     * @param xml_node The XML node to mirror
     * @return Returns true if the XML structure has changed (nodes added, removed or replaced)
     */
    public boolean loadFromXmlNode(Node xml_node) {
        boolean structure_changed = (mXmlNode != xml_node);
        mXmlNode = xml_node;
        if (xml_node != null) {
            updateAttributeList(xml_node);
            structure_changed |= updateElementList(xml_node);
            invokeUiUpdateListeners(structure_changed ? UiUpdateState.CHILDREN_CHANGED
                                                      : UiUpdateState.ATTR_UPDATED);
        }
        return structure_changed;
    }

    /**
     * Clears the UI node and reload it from the given XML node.
     * <p/>
     * This works by clearing all references to any previous XML or UI nodes and
     * then reloads the XML document from scratch. The editor reference is kept.
     * <p/>
     * This is used in the special case where the ElementDescriptor structure has changed.
     * Rather than try to diff inflated UI nodes (as loadFromXmlNode does), we don't bother
     * and reload everything. This is not subtle and should be used very rarely.
     *
     * @param xml_node The XML node or document to reload. Can be null.
     */
    public void reloadFromXmlNode(Node xml_node) {
        // The editor needs to be preserved, it is not affected by an XML change.
        AndroidEditor editor = getEditor();
        clearContent();
        setEditor(editor);
        if (xml_node != null) {
            setXmlDocument(xml_node.getOwnerDocument());
        }
        // This will reload all the XML and recreate the UI structure from scratch.
        loadFromXmlNode(xml_node);
    }

    /**
     * Called by attributes when they want to commit their value
     * to an XML node.
     * <p/>
     * For mandatory nodes, this makes sure the underlying XML element node
     * exists in the model. If not, it is created and assigned as the underlying
     * XML node.
     * </br>
     * For non-mandatory nodes, simply return the underlying XML node, which
     * must always exists.
     *
     * @return The XML node matching this {@link UiElementNode} or null.
     */
    public Node prepareCommit() {
        if (getDescriptor().isMandatory()) {
            createXmlNode();
            // The new XML node has been created.
            // We don't need to refresh using loadFromXmlNode() since there are
            // no attributes or elements that need to be loading into this node.
        }
        return getXmlNode();
    }

    /**
     * Commits the attributes (all internal, inherited from UI parent & unknown attributes).
     * This is called by the UI when the embedding part needs to be committed.
     */
    public void commit() {
        for (UiAttributeNode ui_attr : getInternalUiAttributes().values()) {
            ui_attr.commit();
        }

        for (UiAttributeNode ui_attr : mUnknownUiAttributes) {
            ui_attr.commit();
        }
    }

    /**
     * Returns true if the part has been modified with respect to the data
     * loaded from the model.
     */
    public boolean isDirty() {
        for (UiAttributeNode ui_attr : getInternalUiAttributes().values()) {
            if (ui_attr.isDirty()) {
                return true;
            }
        }

        for (UiAttributeNode ui_attr : mUnknownUiAttributes) {
            if (ui_attr.isDirty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates the underlying XML element node for this UI node if it doesn't already
     * exists.
     *
     * @return The new value of getXmlNode() (can be null if creation failed)
     */
    public Node createXmlNode() {
        if (mXmlNode != null) {
            return null;
        }
        Node parentXmlNode = null;
        if (mUiParent != null) {
            parentXmlNode = mUiParent.prepareCommit();
            if (parentXmlNode == null) {
                // The parent failed to create its own backing XML node. Abort.
                // No need to throw an exception, the parent will most likely
                // have done so itself.
                return null;
            }
        }

        String element_name = getDescriptor().getXmlName();
        Document doc = getXmlDocument();

        // We *must* have a root node. If not, we need to abort.
        if (doc == null) {
            throw new RuntimeException(
                    String.format("Missing XML document for %1$s XML node.", element_name));
        }

        // If we get here and parent_xml_node is null, the node is to be created
        // as the root node of the document (which can't be null, cf check above).
        if (parentXmlNode == null) {
            parentXmlNode = doc;
        }

        mXmlNode = doc.createElement(element_name);

        Node xmlNextSibling = null;

        UiElementNode uiNextSibling = getUiNextSibling();
        if (uiNextSibling != null) {
            xmlNextSibling = uiNextSibling.getXmlNode();
        }

        parentXmlNode.insertBefore(mXmlNode, xmlNextSibling);

        // Insert a separator after the tag, to make it easier to read
        Text sep = doc.createTextNode("\n");
        parentXmlNode.appendChild(sep);

        // Set all initial attributes in the XML node if they are not empty.
        // Iterate on the descriptor list to get the desired order and then use the
        // internal values, if any.
        for (AttributeDescriptor attr_desc : getAttributeDescriptors()) {
            if (attr_desc instanceof XmlnsAttributeDescriptor) {
                XmlnsAttributeDescriptor desc = (XmlnsAttributeDescriptor) attr_desc;
                Attr attr = doc.createAttributeNS(XmlnsAttributeDescriptor.XMLNS_URI,
                        desc.getXmlNsName());
                attr.setValue(desc.getValue());
                attr.setPrefix(desc.getXmlNsPrefix());
                mXmlNode.getAttributes().setNamedItemNS(attr);
            } else {
                UiAttributeNode ui_attr = getInternalUiAttributes().get(attr_desc);
                commitAttributeToXml(ui_attr, ui_attr.getCurrentValue());
            }
        }

        invokeUiUpdateListeners(UiUpdateState.CREATED);
        return mXmlNode;
    }

    /**
     * Removes the XML node corresponding to this UI node if it exists
     * and also removes all mirrored information in this UI node (i.e. children, attributes)
     *
     * @return The removed node or null if it didn't exist in the firtst place.
     */
    public Node deleteXmlNode() {
        if (mXmlNode == null) {
            return null;
        }

        // First clear the internals of the node and *then* actually deletes the XML
        // node (because doing so will generate an update even and this node may be
        // revisited via loadFromXmlNode).
        Node old_xml_node = mXmlNode;
        clearContent();

        Node xml_parent = old_xml_node.getParentNode();
        if (xml_parent == null) {
            xml_parent = getXmlDocument();
        }
        old_xml_node = xml_parent.removeChild(old_xml_node);

        invokeUiUpdateListeners(UiUpdateState.DELETED);
        return old_xml_node;
    }

    /**
     * Updates the element list for this UiElementNode.
     * At the end, the list of children UiElementNode here will match the one from the
     * provided XML {@link Node}:
     * <ul>
     * <li> Walk both the current ui children list and the xml children list at the same time.
     * <li> If we have a new xml child but already reached the end of the ui child list, add the
     *      new xml node.
     * <li> Otherwise, check if the xml node is referenced later in the ui child list and if so,
     *      move it here. It means the XML child list has been reordered.
     * <li> Otherwise, this is a new XML node that we add in the middle of the ui child list.
     * <li> At the end, we may have finished walking the xml child list but still have remaining
     *      ui children, simply delete them as they matching trailing xml nodes that have been
     *      removed unless they are mandatory ui nodes.
     * </ul>
     * Note that only the first case is used when populating the ui list the first time.
     *
     * @param xml_node The XML node to mirror
     * @return True when the XML structure has changed.
     */
    protected boolean updateElementList(Node xml_node) {
        boolean structure_changed = false;
        int ui_index = 0;
        Node xml_child = xml_node.getFirstChild();
        while (xml_child != null) {
            if (xml_child.getNodeType() == Node.ELEMENT_NODE) {
                String element_name = xml_child.getNodeName();
                UiElementNode ui_node = null;
                if (mUiChildren.size() <= ui_index) {
                    // A new node is being added at the end of the list
                    ElementDescriptor desc = mDescriptor.findChildrenDescriptor(element_name,
                            false /* recursive */);
                    if (desc == null) {
                        // Unknown node. Create a temporary descriptor for it.
                        // most important we want to auto-add unknown attributes to it.
                        AndroidEditor editor = getEditor();
                        IEditorInput editorInput = editor.getEditorInput();
                        if (editorInput instanceof IFileEditorInput) {
                            IFileEditorInput fileInput = (IFileEditorInput)editorInput;
                            desc = CustomViewDescriptorService.getInstance().getDescriptor(
                                    fileInput.getFile().getProject(), element_name);
                            if (desc == null) {
                                desc = new ElementDescriptor(element_name);
                            }
                        } else {
                            desc = new ElementDescriptor(element_name);
                            // TODO associate a new "?" icon to this descriptor.
                        }
                    }
                    structure_changed = true;
                    ui_node = appendNewUiChild(desc);
                    ui_index++;
                } else {
                    // A new node is being inserted or moved.
                    // Note: mandatory nodes can be created without an XML node in which case
                    // getXmlNode() is null.
                    UiElementNode ui_child;
                    int n = mUiChildren.size();
                    for (int j = ui_index; j < n; j++) {
                        ui_child = mUiChildren.get(j);
                        if (ui_child.getXmlNode() != null && ui_child.getXmlNode() == xml_child) {
                            if (j > ui_index) {
                                // Found the same XML node at some later index, now move it here.
                                mUiChildren.remove(j);
                                mUiChildren.add(ui_index, ui_child);
                                structure_changed = true;
                            }
                            ui_node = ui_child;
                            ui_index++;
                            break;
                        }
                    }

                    if (ui_node == null) {
                        // Look for an unused mandatory node with no XML node attached
                        // referencing the same XML element name
                        for (int j = ui_index; j < n; j++) {
                            ui_child = mUiChildren.get(j);
                            if (ui_child.getXmlNode() == null &&
                                    ui_child.getDescriptor().isMandatory() &&
                                    ui_child.getDescriptor().getXmlName().equals(element_name)) {
                                if (j > ui_index) {
                                    // Found it, now move it here
                                    mUiChildren.remove(j);
                                    mUiChildren.add(ui_index, ui_child);
                                }
                                // assign the XML node to this empty mandatory element.
                                ui_child.mXmlNode = xml_child;
                                structure_changed = true;
                                ui_node = ui_child;
                                ui_index++;
                            }
                        }
                    }

                    if (ui_node == null) {
                        // Inserting new node
                        ElementDescriptor desc = mDescriptor.findChildrenDescriptor(element_name,
                                false /* recursive */);
                        if (desc == null) {
                            // Unknown element. Simply ignore it.
                            AdtPlugin.log(IStatus.WARNING,
                                    "AndroidManifest: Ignoring unknown '%s' XML element", //$NON-NLS-1$
                                    element_name);
                        } else {
                            structure_changed = true;
                            ui_node = insertNewUiChild(ui_index, desc);
                            ui_index++;
                        }
                    }
                }
                if (ui_node != null) {
                    // If we touched an UI Node, even an existing one, refresh its content.
                    // For new nodes, this will populate them recursively.
                    structure_changed |= ui_node.loadFromXmlNode(xml_child);
                }
            }
            xml_child = xml_child.getNextSibling();
        }

        // There might be extra UI nodes at the end if the XML node list got shorter.
        for (int index = mUiChildren.size() - 1; index >= ui_index; --index) {
             structure_changed |= removeUiChildAtIndex(index);
        }

        return structure_changed;
    }

    /**
     * Internal helper to remove an UI child node given by its index in the
     * internal child list.
     *
     * Also invokes the update listener on the node to be deleted *after* the node has
     * been removed.
     *
     * @param ui_index The index of the UI child to remove, range 0 .. mUiChildren.size()-1
     * @return True if the structure has changed
     * @throws IndexOutOfBoundsException if index is out of mUiChildren's bounds. Of course you
     *         know that could never happen unless the computer is on fire or something.
     */
    private boolean removeUiChildAtIndex(int ui_index) {
        UiElementNode ui_node = mUiChildren.get(ui_index);
        ElementDescriptor desc = ui_node.getDescriptor();

        try {
            if (ui_node.getDescriptor().isMandatory()) {
                // This is a mandatory node. Such a node must exist in the UiNode hierarchy
                // even if there's no XML counterpart. However we only need to keep one.

                // Check if the parent (e.g. this node) has another similar ui child node.
                boolean keepNode = true;
                for (UiElementNode child : mUiChildren) {
                    if (child != ui_node && child.getDescriptor() == desc) {
                        // We found another child with the same descriptor that is not
                        // the node we want to remove. This means we have one mandatory
                        // node so we can safely remove ui_node.
                        keepNode = false;
                        break;
                    }
                }

                if (keepNode) {
                    // We can't remove a mandatory node as we need to keep at least one
                    // mandatory node in the parent. Instead we just clear its content
                    // (including its XML Node reference).

                    // A mandatory node with no XML means it doesn't really exist, so it can't be
                    // deleted. So the structure will change only if the ui node is actually
                    // associated to an XML node.
                    boolean xml_exists = (ui_node.getXmlNode() != null);

                    ui_node.clearContent();
                    return xml_exists;
                }
            }

            mUiChildren.remove(ui_index);
            return true;
        } finally {
            // Tell listeners that a node has been removed.
            // The model has already been modified.
            invokeUiUpdateListeners(UiUpdateState.DELETED);
        }
    }

    /**
     * Creates a new {@link UiElementNode} from the given {@link ElementDescriptor}
     * and appends it to the end of the element children list.
     *
     * @param descriptor The {@link ElementDescriptor} that knows how to create the UI node.
     * @return The new UI node that has been appended
     */
    public UiElementNode appendNewUiChild(ElementDescriptor descriptor) {
        UiElementNode ui_node;
        ui_node = descriptor.createUiNode();
        mUiChildren.add(ui_node);
        ui_node.setUiParent(this);
        ui_node.invokeUiUpdateListeners(UiUpdateState.CREATED);
        return ui_node;
    }

    /**
     * Creates a new {@link UiElementNode} from the given {@link ElementDescriptor}
     * and inserts it in the element children list at the specified position.
     *
     * @param index The position where to insert in the element children list.
     * @param descriptor The {@link ElementDescriptor} that knows how to create the UI node.
     * @return The new UI node.
     */
    public UiElementNode insertNewUiChild(int index, ElementDescriptor descriptor) {
        UiElementNode ui_node;
        ui_node = descriptor.createUiNode();
        mUiChildren.add(index, ui_node);
        ui_node.setUiParent(this);
        ui_node.invokeUiUpdateListeners(UiUpdateState.CREATED);
        return ui_node;
    }

    /**
     * Updates the {@link UiAttributeNode} list for this {@link UiElementNode}.
     * <p/>
     * For a given {@link UiElementNode}, the attribute list always exists in
     * full and is totally independent of whether the XML model actually
     * has the corresponding attributes.
     * <p/>
     * For each attribute declared in this {@link UiElementNode}, get
     * the corresponding XML attribute. It may not exist, in which case the
     * value will be null. We don't really know if a value has changed, so
     * the updateValue() is called on the UI sattribute in all cases.
     *
     * @param xmlNode The XML node to mirror
     */
    protected void updateAttributeList(Node xmlNode) {
        NamedNodeMap xmlAttrMap = xmlNode.getAttributes();
        HashSet<Node> visited = new HashSet<Node>();

        // For all known (i.e. expected) UI attributes, find an existing XML attribute of
        // same (uri, local name) and update the internal Ui attribute value.
        for (UiAttributeNode uiAttr : getInternalUiAttributes().values()) {
            AttributeDescriptor desc = uiAttr.getDescriptor();
            if (!(desc instanceof SeparatorAttributeDescriptor)) {
                Node xmlAttr = xmlAttrMap == null ? null :
                    xmlAttrMap.getNamedItemNS(desc.getNamespaceUri(), desc.getXmlLocalName());
                uiAttr.updateValue(xmlAttr);
                visited.add(xmlAttr);
            }
        }

        // Clone the current list of unknown attributes. We'll then remove from this list when
        // we still attributes which are still unknown. What will be left are the old unknown
        // attributes that have been deleted in the current XML attribute list.
        @SuppressWarnings("unchecked") //$NON-NLS-1$
        HashSet<UiAttributeNode> deleted = (HashSet<UiAttributeNode>) mUnknownUiAttributes.clone();

        // We need to ignore hidden attributes.
        Map<String, AttributeDescriptor> hiddenAttrDesc = getHiddenAttributeDescriptors();

        // Traverse the actual XML attribute list to find unknown attributes
        if (xmlAttrMap != null) {
            for (int i = 0; i < xmlAttrMap.getLength(); i++) {
                Node xmlAttr = xmlAttrMap.item(i);
                // Ignore attributes which have actual descriptors
                if (visited.contains(xmlAttr)) {
                    continue;
                }

                String xmlFullName = xmlAttr.getNodeName();

                // Ignore attributes which are hidden (based on the prefix:localName key)
                if (hiddenAttrDesc.containsKey(xmlFullName)) {
                    continue;
                }

                String xmlAttrLocalName = xmlAttr.getLocalName();
                String xmlNsUri = xmlAttr.getNamespaceURI();

                UiAttributeNode uiAttr = null;
                for (UiAttributeNode a : mUnknownUiAttributes) {
                    String aLocalName = a.getDescriptor().getXmlLocalName();
                    String aNsUri = a.getDescriptor().getNamespaceUri();
                    if (aLocalName.equals(xmlAttrLocalName) &&
                            (aNsUri == xmlNsUri || (aNsUri != null && aNsUri.equals(xmlNsUri)))) {
                        // This attribute is still present in the unknown list
                        uiAttr = a;
                        // It has not been deleted
                        deleted.remove(a);
                        break;
                    }
                }
                if (uiAttr == null) {
                    // Create a new unknown attribute
                    TextAttributeDescriptor desc = new TextAttributeDescriptor(
                            xmlAttrLocalName, // xml name
                            xmlFullName, // ui name
                            xmlNsUri, // NS uri
                            "Unknown XML attribute"); // tooltip, translatable
                    uiAttr = desc.createUiNode(this);
                    mUnknownUiAttributes.add(uiAttr);
                }

                uiAttr.updateValue(xmlAttr);
            }

            // Remove from the internal list unknown attributes that have been deleted from the xml
            for (UiAttributeNode a : deleted) {
                mUnknownUiAttributes.remove(a);
            }
        }
    }

    /**
     * Invoke all registered {@link IUiUpdateListener} listening on this UI update for this node.
     */
    protected void invokeUiUpdateListeners(UiUpdateState state) {
        if (mUiUpdateListeners != null) {
            for (IUiUpdateListener listener : mUiUpdateListeners) {
                try {
                    listener.uiElementNodeUpdated(this, state);
                } catch (Exception e) {
                    // prevent a crashing listener from crashing the whole invocation chain
                    AdtPlugin.log(e, "UIElement Listener failed: %s, state=%s",  //$NON-NLS-1$
                            getBreadcrumbTrailDescription(true),
                            state.toString());
                }
            }
        }
    }

    // --- for derived implementations only ---

    // TODO doc
    protected void setXmlNode(Node xml_node) {
        mXmlNode = xml_node;
    }

    /**
     * Sets the temporary data used by the editors.
     * @param data the data.
     *
     * @since GLE1
     * @deprecated Used by GLE1. Should be deprecated for GLE2.
     */
    public void setEditData(Object data) {
        mEditData = data;
    }

    /**
     * Returns the temporary data used by the editors for this object.
     * @return the data, or <code>null</code> if none has been set.
     */
    public Object getEditData() {
        return mEditData;
    }

    public void refreshUi() {
        invokeUiUpdateListeners(UiUpdateState.ATTR_UPDATED);
    }


    // ------------- Helpers

    /**
     * Helper method to commit a single attribute value to XML.
     * <p/>
     * This method updates the XML regardless of the current XML value.
     * Callers should check first if an update is needed.
     * If the new value is empty, the XML attribute will be actually removed.
     * <p/>
     * Note that the caller MUST ensure that modifying the underlying XML model is
     * safe and must take care of marking the model as dirty if necessary.
     *
     * @see AndroidEditor#editXmlModel(Runnable)
     *
     * @param uiAttr The attribute node to commit. Must be a child of this UiElementNode.
     * @param newValue The new value to set.
     * @return True if the XML attribute was modified or removed, false if nothing changed.
     */
    public boolean commitAttributeToXml(UiAttributeNode uiAttr, String newValue) {
        // Get (or create) the underlying XML element node that contains the attributes.
        Node element = prepareCommit();
        if (element != null && uiAttr != null) {
            String attrLocalName = uiAttr.getDescriptor().getXmlLocalName();
            String attrNsUri = uiAttr.getDescriptor().getNamespaceUri();

            NamedNodeMap attrMap = element.getAttributes();
            if (newValue == null || newValue.length() == 0) {
                // Remove attribute if it's empty
                if (attrMap.getNamedItemNS(attrNsUri, attrLocalName) != null) {
                    attrMap.removeNamedItemNS(attrNsUri, attrLocalName);
                    return true;
                }
            } else {
                // Add or replace an attribute
                Document doc = element.getOwnerDocument();
                if (doc != null) {
                    Attr attr = doc.createAttributeNS(attrNsUri, attrLocalName);
                    attr.setValue(newValue);
                    attr.setPrefix(lookupNamespacePrefix(element, attrNsUri));
                    attrMap.setNamedItemNS(attr);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to commit all dirty attributes values to XML.
     * <p/>
     * This method is useful if {@link #setAttributeValue(String, String, boolean)} has been
     * called more than once and all the attributes marked as dirty must be commited to the
     * XML. It calls {@link #commitAttributeToXml(UiAttributeNode, String)} on each dirty
     * attribute.
     * <p/>
     * Note that the caller MUST ensure that modifying the underlying XML model is
     * safe and must take care of marking the model as dirty if necessary.
     *
     * @see AndroidEditor#editXmlModel(Runnable)
     *
     * @return True if one or more values were actually modified or removed,
     *         false if nothing changed.
     */
    public boolean commitDirtyAttributesToXml() {
        boolean result = false;
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        for (Entry<AttributeDescriptor, UiAttributeNode> entry : attributeMap.entrySet()) {
            UiAttributeNode ui_attr = entry.getValue();
            if (ui_attr.isDirty()) {
                result |= commitAttributeToXml(ui_attr, ui_attr.getCurrentValue());
                ui_attr.setDirty(false);
            }
        }
        return result;
    }

    /**
     * Returns the namespace prefix matching the requested namespace URI.
     * If no such declaration is found, returns the default "android" prefix.
     *
     * @param node The current node. Must not be null.
     * @param nsUri The namespace URI of which the prefix is to be found,
     *              e.g. SdkConstants.NS_RESOURCES
     * @return The first prefix declared or the default "android" prefix.
     */
    private String lookupNamespacePrefix(Node node, String nsUri) {
        // Note: Node.lookupPrefix is not implemented in wst/xml/core NodeImpl.java
        // The following code emulates this simple call:
        //   String prefix = node.lookupPrefix(SdkConstants.NS_RESOURCES);

        // if the requested URI is null, it denotes an attribute with no namespace.
        if (nsUri == null) {
            return null;
        }

        // per XML specification, the "xmlns" URI is reserved
        if (XmlnsAttributeDescriptor.XMLNS_URI.equals(nsUri)) {
            return "xmlns"; //$NON-NLS-1$
        }

        HashSet<String> visited = new HashSet<String>();
        Document doc = node == null ? null : node.getOwnerDocument();

        for (; node != null && node.getNodeType() == Node.ELEMENT_NODE;
               node = node.getParentNode()) {
            NamedNodeMap attrs = node.getAttributes();
            for (int n = attrs.getLength() - 1; n >= 0; --n) {
                Node attr = attrs.item(n);
                if ("xmlns".equals(attr.getPrefix())) {  //$NON-NLS-1$
                    String uri = attr.getNodeValue();
                    String nsPrefix = attr.getLocalName();
                    // Is this the URI we are looking for? If yes, we found its prefix.
                    if (nsUri.equals(uri)) {
                        return nsPrefix;
                    }
                    visited.add(nsPrefix);
                }
            }
        }

        // Use a sensible default prefix if we can't find one.
        // We need to make sure the prefix is not one that was declared in the scope
        // visited above. Use a default namespace prefix "android" for the Android resource
        // NS and use "ns" for all other custom namespaces.
        String prefix = SdkConstants.NS_RESOURCES.equals(nsUri) ? "android" : "ns"; //$NON-NLS-1$ //$NON-NLS-2$
        String base = prefix;
        for (int i = 1; visited.contains(prefix); i++) {
            prefix = base + Integer.toString(i);
        }

        // Also create & define this prefix/URI in the XML document as an attribute in the
        // first element of the document.
        if (doc != null) {
            node = doc.getFirstChild();
            while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
                node = node.getNextSibling();
            }
            if (node != null) {
                Attr attr = doc.createAttributeNS(XmlnsAttributeDescriptor.XMLNS_URI, prefix);
                attr.setValue(nsUri);
                attr.setPrefix("xmlns"); //$NON-NLS-1$
                node.getAttributes().setNamedItemNS(attr);
            }
        }

        return prefix;
    }

    /**
     * Utility method to internally set the value of a text attribute for the current
     * UiElementNode.
     * <p/>
     * This method is a helper. It silently ignores the errors such as the requested
     * attribute not being present in the element or attribute not being settable.
     * It accepts inherited attributes (such as layout).
     * <p/>
     * This does not commit to the XML model. It does mark the attribute node as dirty.
     * This is up to the caller.
     *
     * @see #commitAttributeToXml(UiAttributeNode, String)
     * @see #commitDirtyAttributesToXml()
     *
     * @param attrXmlName The XML name of the attribute to modify
     * @param value The new value for the attribute. If set to null, the attribute is removed.
     * @param override True if the value must be set even if one already exists.
     * @return The {@link UiAttributeNode} that has been modified or null.
     */
    public UiAttributeNode setAttributeValue(String attrXmlName, String value, boolean override) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        if (value == null) {
            value = ""; //$NON-NLS-1$ -- this removes an attribute
        }

        for (Entry<AttributeDescriptor, UiAttributeNode> entry : attributeMap.entrySet()) {
            AttributeDescriptor ui_desc = entry.getKey();
            if (ui_desc.getXmlLocalName().equals(attrXmlName)) {
                UiAttributeNode ui_attr = entry.getValue();
                // Not all attributes are editable, ignore those which are not
                if (ui_attr instanceof IUiSettableAttributeNode) {
                    String current = ui_attr.getCurrentValue();
                    // Only update (and mark as dirty) if the attribute did not have any
                    // value or if the value was different.
                    if (override || current == null || !current.equals(value)) {
                        ((IUiSettableAttributeNode) ui_attr).setCurrentValue(value);
                        // mark the attribute as dirty since their internal content
                        // as been modified, but not the underlying XML model
                        ui_attr.setDirty(true);
                        return ui_attr;
                    }
                }
                break;
            }
        }
        return null;
    }

    /**
     * Utility method to retrieve the internal value of an attribute.
     * <p/>
     * Note that this retrieves the *field* value if the attribute has some UI, and
     * not the actual XML value. They may differ if the attribute is dirty.
     *
     * @param attrXmlName The XML name of the attribute to modify
     * @return The current internal value for the attribute or null in case of error.
     */
    public String getAttributeValue(String attrXmlName) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        for (Entry<AttributeDescriptor, UiAttributeNode> entry : attributeMap.entrySet()) {
            AttributeDescriptor ui_desc = entry.getKey();
            if (ui_desc.getXmlLocalName().equals(attrXmlName)) {
                UiAttributeNode ui_attr = entry.getValue();
                return ui_attr.getCurrentValue();
            }
        }
        return null;
    }

    // ------ IPropertySource methods

    public Object getEditableValue() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
     *
     * Returns the property descriptor for this node. Since the descriptors are not linked to the
     * data, the AttributeDescriptor are used directly.
     */
    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> propDescs = new ArrayList<IPropertyDescriptor>();

        // get the standard descriptors
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();
        Set<AttributeDescriptor> keys = attributeMap.keySet();


        // we only want the descriptor that do implement the IPropertyDescriptor interface.
        for (AttributeDescriptor key : keys) {
            if (key instanceof IPropertyDescriptor) {
                propDescs.add((IPropertyDescriptor)key);
            }
        }

        // now get the descriptor from the unknown attributes
        for (UiAttributeNode unknownNode : mUnknownUiAttributes) {
            if (unknownNode.getDescriptor() instanceof IPropertyDescriptor) {
                propDescs.add((IPropertyDescriptor)unknownNode.getDescriptor());
            }
        }

        // TODO cache this maybe, as it's not going to change (except for unknown descriptors)
        return propDescs.toArray(new IPropertyDescriptor[propDescs.size()]);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
     *
     * Returns the value of a given property. The id is the result of IPropertyDescriptor.getId(),
     * which return the AttributeDescriptor itself.
     */
    public Object getPropertyValue(Object id) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);

        if (attribute == null) {
            // look for the id in the unknown attributes.
            for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
                if (id == unknownAttr.getDescriptor()) {
                    return unknownAttr;
                }
            }
        }

        return attribute;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#isPropertySet(java.lang.Object)
     *
     * Returns whether the property is set. In our case this is if the string is non empty.
     */
    public boolean isPropertySet(Object id) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);

        if (attribute != null) {
            return attribute.getCurrentValue().length() > 0;
        }

        // look for the id in the unknown attributes.
        for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
            if (id == unknownAttr.getDescriptor()) {
                return unknownAttr.getCurrentValue().length() > 0;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java.lang.Object)
     *
     * Reset the property to its default value. For now we simply empty it.
     */
    public void resetPropertyValue(Object id) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);
        if (attribute != null) {
            // TODO: reset the value of the attribute

            return;
        }

        // look for the id in the unknown attributes.
        for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
            if (id == unknownAttr.getDescriptor()) {
                // TODO: reset the value of the attribute

                return;
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object, java.lang.Object)
     *
     * Set the property value. id is the result of IPropertyDescriptor.getId(), which is the
     * AttributeDescriptor itself. Value should be a String.
     */
    public void setPropertyValue(Object id, Object value) {
        HashMap<AttributeDescriptor, UiAttributeNode> attributeMap = getInternalUiAttributes();

        UiAttributeNode attribute = attributeMap.get(id);

        if (attribute == null) {
            // look for the id in the unknown attributes.
            for (UiAttributeNode unknownAttr : mUnknownUiAttributes) {
                if (id == unknownAttr.getDescriptor()) {
                    attribute = unknownAttr;
                    break;
                }
            }
        }

        if (attribute != null) {

            // get the current value and compare it to the new value
            String oldValue = attribute.getCurrentValue();
            final String newValue = (String)value;

            if (oldValue.equals(newValue)) {
                return;
            }

            final UiAttributeNode fAttribute = attribute;
            AndroidEditor editor = getEditor();
            editor.editXmlModel(new Runnable() {
                public void run() {
                    commitAttributeToXml(fAttribute, newValue);
                }
            });
        }
    }
}
