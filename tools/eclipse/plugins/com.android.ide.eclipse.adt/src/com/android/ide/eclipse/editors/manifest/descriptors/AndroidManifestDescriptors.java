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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.resources.DeclareStyleableInfo;
import com.android.ide.eclipse.common.resources.ResourceType;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.editors.descriptors.ListAttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.ReferenceAttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.runtime.IStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;


/**
 * Complete description of the AndroidManifest.xml structure.
 * <p/>
 * The root element are static instances which always exists.
 * However their sub-elements and attributes are created only when the SDK changes or is
 * loaded the first time.
 */
public final class AndroidManifestDescriptors implements IDescriptorProvider {

    private static final String MANIFEST_NODE_NAME = "manifest";                //$NON-NLS-1$
    private static final String ANDROID_MANIFEST_STYLEABLE = "AndroidManifest"; //$NON-NLS-1$

    // Public attributes names, attributes descriptors and elements descriptors
    
    public static final String ANDROID_LABEL_ATTR = "label";    //$NON-NLS-1$
    public static final String ANDROID_NAME_ATTR  = "name";     //$NON-NLS-1$
    public static final String PACKAGE_ATTR       = "package";  //$NON-NLS-1$

    /** The {@link ElementDescriptor} for the root Manifest element. */
    private final ElementDescriptor MANIFEST_ELEMENT;
    /** The {@link ElementDescriptor} for the root Application element. */
    private final ElementDescriptor APPLICATION_ELEMENT;

    /** The {@link ElementDescriptor} for the root Instrumentation element. */
    private final ElementDescriptor INTRUMENTATION_ELEMENT;
    /** The {@link ElementDescriptor} for the root Permission element. */
    private final ElementDescriptor PERMISSION_ELEMENT;
    /** The {@link ElementDescriptor} for the root UsesPermission element. */
    private final ElementDescriptor USES_PERMISSION_ELEMENT;
    /** The {@link ElementDescriptor} for the root UsesSdk element. */
    private final ElementDescriptor USES_SDK_ELEMENT;

    /** The {@link ElementDescriptor} for the root PermissionGroup element. */
    private final ElementDescriptor PERMISSION_GROUP_ELEMENT;
    /** The {@link ElementDescriptor} for the root PermissionTree element. */
    private final ElementDescriptor PERMISSION_TREE_ELEMENT;

    /** Private package attribute for the manifest element. Needs to be handled manually. */
    private final TextAttributeDescriptor PACKAGE_ATTR_DESC;
    
    public AndroidManifestDescriptors() {
        APPLICATION_ELEMENT = createElement("application", null, true); //$NON-NLS-1$ + no child & mandatory
        INTRUMENTATION_ELEMENT = createElement("instrumentation"); //$NON-NLS-1$

        PERMISSION_ELEMENT = createElement("permission"); //$NON-NLS-1$
        USES_PERMISSION_ELEMENT = createElement("uses-permission"); //$NON-NLS-1$
        USES_SDK_ELEMENT = createElement("uses-sdk", null, true); //$NON-NLS-1$ + no child & mandatory

        PERMISSION_GROUP_ELEMENT = createElement("permission-group"); //$NON-NLS-1$
        PERMISSION_TREE_ELEMENT = createElement("permission-tree"); //$NON-NLS-1$

        MANIFEST_ELEMENT = createElement(
                        MANIFEST_NODE_NAME, // xml name
                        new ElementDescriptor[] {
                                        APPLICATION_ELEMENT,
                                        INTRUMENTATION_ELEMENT,
                                        PERMISSION_ELEMENT,
                                        USES_PERMISSION_ELEMENT,
                                        PERMISSION_GROUP_ELEMENT,
                                        PERMISSION_TREE_ELEMENT,
                                        USES_SDK_ELEMENT,
                        },
                        true /* mandatory */);

        // The "package" attribute is treated differently as it doesn't have the standard
        // Android XML namespace.
        PACKAGE_ATTR_DESC = new PackageAttributeDescriptor(PACKAGE_ATTR,
                "Package",
                null /* nsUri */,
                "This attribute gives a unique name for the package, using a Java-style naming convention to avoid name collisions.\nFor example, applications published by Google could have names of the form com.google.app.appname");
    }
    
    public ElementDescriptor[] getRootElementDescriptors() {
        return new ElementDescriptor[] { MANIFEST_ELEMENT };
    }
    
    public ElementDescriptor getDescriptor() {
        return getManifestElement();
    }
    
    public ElementDescriptor getApplicationElement() {
        return APPLICATION_ELEMENT;
    }
    
    public ElementDescriptor getManifestElement() {
        return MANIFEST_ELEMENT;
    }
    
    public ElementDescriptor getUsesSdkElement() {
        return USES_SDK_ELEMENT;
    }
    
    public ElementDescriptor getInstrumentationElement() {
        return INTRUMENTATION_ELEMENT;
    }
    
    public ElementDescriptor getPermissionElement() {
        return PERMISSION_ELEMENT;
    }
    
    public ElementDescriptor getUsesPermissionElement() {
        return USES_PERMISSION_ELEMENT;
    }
    
    public ElementDescriptor getPermissionGroupElement() {
        return PERMISSION_GROUP_ELEMENT;
    }
    
    public ElementDescriptor getPermissionTreeElement() {
        return PERMISSION_TREE_ELEMENT;
    }

    /**
     * Updates the document descriptor.
     * <p/>
     * It first computes the new children of the descriptor and then updates them
     * all at once.
     * 
     * @param manifestMap The map style => attributes from the attrs_manifest.xml file
     */
    public synchronized void updateDescriptors(
            Map<String, DeclareStyleableInfo> manifestMap) {

        XmlnsAttributeDescriptor xmlns = new XmlnsAttributeDescriptor(
                "android", //$NON-NLS-1$
                SdkConstants.NS_RESOURCES); 

        // -- setup the required attributes overrides --
        
        Set<String> required = new HashSet<String>();
        required.add("provider/authorities");  //$NON-NLS-1$
        
        // -- setup the various attribute format overrides --
        
        // The key for each override is "element1,element2,.../attr-xml-local-name" or
        // "*/attr-xml-local-name" to match the attribute in any element.
        
        Map<String, Object> overrides = new HashMap<String, Object>();

        overrides.put("*/icon", new DescriptorsUtils.ITextAttributeCreator() { //$NON-NLS-1$
            public TextAttributeDescriptor create(String xmlName, String uiName, String nsUri,
                    String tooltip) {
                return new ReferenceAttributeDescriptor(
                        ResourceType.DRAWABLE,
                        xmlName, uiName, nsUri,
                        tooltip);
            }
        });
        
        overrides.put("*/theme",         ThemeAttributeDescriptor.class);   //$NON-NLS-1$
        overrides.put("*/permission",    ListAttributeDescriptor.class);    //$NON-NLS-1$
        overrides.put("*/targetPackage", PackageAttributeDescriptor.class); //$NON-NLS-1$
        
        overrides.put("uses-library/name", ListAttributeDescriptor.class);       //$NON-NLS-1$

        overrides.put("action,category,uses-permission/" + ANDROID_NAME_ATTR,    //$NON-NLS-1$
                      ListAttributeDescriptor.class);
        overrides.put("application/" + ANDROID_NAME_ATTR, ApplicationAttributeDescriptor.class);  //$NON-NLS-1$

        overrideClassName(overrides, "activity", AndroidConstants.CLASS_ACTIVITY);           //$NON-NLS-1$
        overrideClassName(overrides, "receiver", AndroidConstants.CLASS_BROADCASTRECEIVER);  //$NON-NLS-1$
        overrideClassName(overrides, "service", AndroidConstants.CLASS_SERVICE);             //$NON-NLS-1$
        overrideClassName(overrides, "provider", AndroidConstants.CLASS_CONTENTPROVIDER);    //$NON-NLS-1$
        overrideClassName(overrides, "instrumentation", AndroidConstants.CLASS_INSTRUMENTATION);    //$NON-NLS-1$

        // -- list element nodes already created --
        // These elements are referenced by already opened editors, so we want to update them
        // but not re-create them when reloading an SDK on the fly.
        
        HashMap<String, ElementDescriptor> elementDescs =
            new HashMap<String, ElementDescriptor>();
        elementDescs.put(MANIFEST_ELEMENT.getXmlLocalName(),         MANIFEST_ELEMENT);
        elementDescs.put(APPLICATION_ELEMENT.getXmlLocalName(),      APPLICATION_ELEMENT);
        elementDescs.put(INTRUMENTATION_ELEMENT.getXmlLocalName(),   INTRUMENTATION_ELEMENT);
        elementDescs.put(PERMISSION_ELEMENT.getXmlLocalName(),       PERMISSION_ELEMENT);
        elementDescs.put(USES_PERMISSION_ELEMENT.getXmlLocalName(),  USES_PERMISSION_ELEMENT);
        elementDescs.put(USES_SDK_ELEMENT.getXmlLocalName(),         USES_SDK_ELEMENT);
        elementDescs.put(PERMISSION_GROUP_ELEMENT.getXmlLocalName(), PERMISSION_GROUP_ELEMENT);
        elementDescs.put(PERMISSION_TREE_ELEMENT.getXmlLocalName(),  PERMISSION_TREE_ELEMENT);

        // --

        inflateElement(manifestMap,
                overrides,
                required,
                elementDescs,
                MANIFEST_ELEMENT,
                "AndroidManifest"); //$NON-NLS-1$
        insertAttribute(MANIFEST_ELEMENT, PACKAGE_ATTR_DESC);
        
        sanityCheck(manifestMap, MANIFEST_ELEMENT);
    }
    
    /**
     * Sets up an attribute override for ANDROID_NAME_ATTR using a ClassAttributeDescriptor
     * with the specified class name.
     */
    private static void overrideClassName(Map<String, Object> overrides,
            String elementName, final String className) {
        overrides.put(elementName + "/" + ANDROID_NAME_ATTR,
                new DescriptorsUtils.ITextAttributeCreator() {
            public TextAttributeDescriptor create(String xmlName, String uiName, String nsUri,
                    String tooltip) {
                uiName += "*";  //$NON-NLS-1$
                if (AndroidConstants.CLASS_ACTIVITY.equals(className)) {
                    return new ClassAttributeDescriptor(
                            className,
                            PostActivityCreationAction.getAction(),
                            xmlName,
                            uiName,
                            nsUri,
                            tooltip,
                            true /*mandatory */,
                            true /*defaultToProjectOnly*/);
                } else if (AndroidConstants.CLASS_BROADCASTRECEIVER.equals(className)) {
                    return new ClassAttributeDescriptor(
                            className,
                            PostReceiverCreationAction.getAction(),
                            xmlName,
                            uiName,
                            nsUri,
                            tooltip,
                            true /*mandatory */,
                            true /*defaultToProjectOnly*/);
                } else if (AndroidConstants.CLASS_INSTRUMENTATION.equals(className)) {
                    return new ClassAttributeDescriptor(
                            className,
                            null, // no post action
                            xmlName,
                            uiName,
                            nsUri,
                            tooltip,
                            true /*mandatory */,
                            false /*defaultToProjectOnly*/);
                } else {
                    return new ClassAttributeDescriptor(
                            className,
                            xmlName,
                            uiName,
                            nsUri,
                            tooltip,
                            true /*mandatory */);
                }
            }
        });
    }

    /**
     * Returns a new ElementDescriptor constructed from the information given here
     * and the javadoc & attributes extracted from the style map if any.
     * <p/>
     * Creates an element with no attribute overrides.
     */
    private ElementDescriptor createElement(
            String xmlName,
            ElementDescriptor[] childrenElements,
            boolean mandatory) {
        // Creates an element with no attribute overrides.
        String styleName = guessStyleName(xmlName);
        String sdkUrl = DescriptorsUtils.MANIFEST_SDK_URL + styleName; 
        String uiName = getUiName(xmlName);

        ElementDescriptor element = new ManifestElementDescriptor(xmlName, uiName, null, sdkUrl,
                null, childrenElements, mandatory);

        return element;
    }

    /**
     * Returns a new ElementDescriptor constructed from its XML local name.
     * <p/>
     * This version creates an element not mandatory.
     */
    private ElementDescriptor createElement(String xmlName) {
        // Creates an element with no child and not mandatory
        return createElement(xmlName, null, false);
    }

    /**
     * Inserts an attribute in this element attribute list if it is not present there yet
     * (based on the attribute XML name.)
     * The attribute is inserted at the beginning of the attribute list.
     */
    private void insertAttribute(ElementDescriptor element, AttributeDescriptor newAttr) {
        AttributeDescriptor[] attributes = element.getAttributes();
        for (AttributeDescriptor attr : attributes) {
            if (attr.getXmlLocalName().equals(newAttr.getXmlLocalName())) {
                return;
            }
        }
        
        AttributeDescriptor[] newArray = new AttributeDescriptor[attributes.length + 1];
        newArray[0] = newAttr;
        System.arraycopy(attributes, 0, newArray, 1, attributes.length);
        element.setAttributes(newArray);
    }

    /**
     * "Inflates" the properties of an {@link ElementDescriptor} from the styleable declaration.
     * <p/>
     * This first creates all the attributes for the given ElementDescriptor.
     * It then finds all children of the descriptor, inflate them recursively and set them
     * as child to this ElementDescriptor.
     * 
     * @param styleMap The input styleable map for manifest elements & attributes.
     * @param overrides A list of attribute overrides (to customize the type of the attribute
     *          descriptors).
     * @param requiredAttributes Set of attributes to be marked as required.
     * @param existingElementDescs A map of already created element descriptors, keyed by
     *          XML local name. This is used to use the static elements created initially by this
     *          class, which are referenced directly by editors (so that reloading an SDK won't
     *          break these references).
     * @param elemDesc The current {@link ElementDescriptor} to inflate.
     * @param styleName The name of the {@link ElementDescriptor} to inflate. Its XML local name
     *          will be guessed automatically from the style name. 
     */
    private void inflateElement(
            Map<String, DeclareStyleableInfo> styleMap,
            Map<String, Object> overrides,
            Set<String> requiredAttributes,
            HashMap<String, ElementDescriptor> existingElementDescs,
            ElementDescriptor elemDesc,
            String styleName) {
        assert elemDesc != null;
        assert styleName != null;
        
        // define attributes
        DeclareStyleableInfo style = styleMap != null ? styleMap.get(styleName) : null;
        if (style != null) {
            ArrayList<AttributeDescriptor> attrDescs = new ArrayList<AttributeDescriptor>();
            DescriptorsUtils.appendAttributes(attrDescs,
                    elemDesc.getXmlLocalName(),
                    SdkConstants.NS_RESOURCES,
                    style.getAttributes(),
                    requiredAttributes,
                    overrides);
            elemDesc.setTooltip(style.getJavaDoc());
            elemDesc.setAttributes(attrDescs.toArray(new AttributeDescriptor[attrDescs.size()]));
        }
        
        // find all elements that have this one as parent
        ArrayList<ElementDescriptor> children = new ArrayList<ElementDescriptor>();
        for (Entry<String, DeclareStyleableInfo> entry : styleMap.entrySet()) {
            DeclareStyleableInfo childStyle = entry.getValue();
            boolean isParent = false;
            String[] parents = childStyle.getParents();
            if (parents != null) {
                for (String parent: parents) {
                    if (styleName.equals(parent)) {
                        isParent = true;
                        break;
                    }
                }
            }
            if (isParent) {
                String childStyleName = entry.getKey();
                String childXmlName = guessXmlName(childStyleName);
                
                // create or re-use element
                ElementDescriptor child = existingElementDescs.get(childXmlName);
                if (child == null) {
                    child = createElement(childXmlName);
                    existingElementDescs.put(childXmlName, child);
                }
                children.add(child);
                
                inflateElement(styleMap,
                        overrides,
                        requiredAttributes,
                        existingElementDescs,
                        child,
                        childStyleName);
            }
        }
        elemDesc.setChildren(children.toArray(new ElementDescriptor[children.size()]));
    }

    /**
     * Get an UI name from the element XML name.
     * <p/>
     * Capitalizes the first letter and replace non-alphabet by a space followed by a capital.
     */
    private String getUiName(String xmlName) {
        StringBuilder sb = new StringBuilder();

        boolean capitalize = true;
        for (char c : xmlName.toCharArray()) {
            if (capitalize && c >= 'a' && c <= 'z') {
                sb.append((char)(c + 'A' - 'a'));
                capitalize = false;
            } else if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) {
                sb.append(' ');
                capitalize = true;
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }

    /**
     * Guesses the style name for a given XML element name.
     * <p/> 
     * The rules are:
     * - capitalize the first letter: 
     * - if there's a dash, skip it and capitalize the next one
     * - prefix AndroidManifest
     * The exception is "manifest" which just becomes AndroidManifest.
     * <p/>
     * Examples:
     * - manifest        => AndroidManifest
     * - application     => AndroidManifestApplication
     * - uses-permission => AndroidManifestUsesPermission
     */
    private String guessStyleName(String xmlName) {
        StringBuilder sb = new StringBuilder();

        if (!xmlName.equals(MANIFEST_NODE_NAME)) {
            boolean capitalize = true;
            for (char c : xmlName.toCharArray()) {
                if (capitalize && c >= 'a' && c <= 'z') {
                    sb.append((char)(c + 'A' - 'a'));
                    capitalize = false;
                } else if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) {
                    // not a letter -- skip the character and capitalize the next one
                    capitalize = true;
                } else {
                    sb.append(c);
                }
            }
        }
        
        sb.insert(0, ANDROID_MANIFEST_STYLEABLE);
        return sb.toString();
    }

    /**
     * This method performs a sanity check to make sure all the styles declared in the
     * manifestMap are actually defined in the actual element descriptors and reachable from
     * the manifestElement root node.
     */
    private void sanityCheck(Map<String, DeclareStyleableInfo> manifestMap,
            ElementDescriptor manifestElement) {
        TreeSet<String> elementsDeclared = new TreeSet<String>();
        findAllElementNames(manifestElement, elementsDeclared);

        TreeSet<String> stylesDeclared = new TreeSet<String>();
        for (String styleName : manifestMap.keySet()) {
            if (styleName.startsWith(ANDROID_MANIFEST_STYLEABLE)) {
                stylesDeclared.add(styleName);
            }
        }
        
        for (Iterator<String> it = elementsDeclared.iterator(); it.hasNext();) {
            String xmlName = it.next();
            String styleName = guessStyleName(xmlName);
            if (stylesDeclared.remove(styleName)) {
                it.remove();
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!stylesDeclared.isEmpty()) {
            sb.append("Warning, ADT/SDK Mismatch! The following elements are declared by the SDK but unknown to ADT: ");
            for (String name : stylesDeclared) {
                name = guessXmlName(name);
                
                if (name != stylesDeclared.last()) {
                    sb.append(", ");    //$NON-NLS-1$
                }
            }
            
            AdtPlugin.log(IStatus.WARNING, "%s", sb.toString());
            AdtPlugin.printToConsole((String)null, sb);
            sb.setLength(0);
        }

        if (!elementsDeclared.isEmpty()) {
            sb.append("Warning, ADT/SDK Mismatch! The following elements are declared by ADT but not by the SDK: ");
            for (String name : elementsDeclared) {
                sb.append(name);
                if (name != elementsDeclared.last()) {
                    sb.append(", ");    //$NON-NLS-1$
                }
            }

            AdtPlugin.log(IStatus.WARNING, "%s", sb.toString());
            AdtPlugin.printToConsole((String)null, sb);
        }
    }

    /**
     * Performs an approximate translation of the style name into a potential
     * xml name. This is more or less the reverse from guessStyleName().
     * 
     * @return The XML local name for a given style name. 
     */
    private String guessXmlName(String name) {
        StringBuilder sb = new StringBuilder();
        if (ANDROID_MANIFEST_STYLEABLE.equals(name)) {
            sb.append(MANIFEST_NODE_NAME);
        } else {
            name = name.replace(ANDROID_MANIFEST_STYLEABLE, "");    //$NON-NLS-1$
            boolean first_char = true;
            for (char c : name.toCharArray()) {
                if (c >= 'A' && c <= 'Z') {
                    if (!first_char) {
                        sb.append('-');
                    }
                    c = (char) (c - 'A' + 'a');
                }
                sb.append(c);
                first_char = false;
            }
        }
        return sb.toString();
    }

    /**
     * Helper method used by {@link #sanityCheck(Map, ElementDescriptor)} to find all the
     * {@link ElementDescriptor} names defined by the tree of descriptors.
     * <p/>
     * Note: this assumes no circular reference in the tree of {@link ElementDescriptor}s.
     */
    private void findAllElementNames(ElementDescriptor element, TreeSet<String> declared) {
        declared.add(element.getXmlName());
        for (ElementDescriptor desc : element.getChildren()) {
            findAllElementNames(desc, declared);
        }
    }


}
