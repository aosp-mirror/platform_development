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

package com.android.ide.eclipse.adt.internal.editors.manifest.model;

import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.mock.MockXmlNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.w3c.dom.Node;

import java.util.Iterator;

import junit.framework.TestCase;

public class UiElementNodeTest extends TestCase {

    private ElementDescriptor e;
    private UiElementNode ui;

    @Override
    protected void setUp() throws Exception {
        e = new ElementDescriptor("manifest", new ElementDescriptor[] {
                new ElementDescriptor("application", new ElementDescriptor[] {
                    new ElementDescriptor("provider"), 
                    new ElementDescriptor("activity", new ElementDescriptor[] {
                        new ElementDescriptor("intent-filter")
                    }), 
                }), 
                new ElementDescriptor("permission")
            });
        
        ui = new UiElementNode(e);
        
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // pass
    }

    /**
     * Check initialization values for ui node
     */
    public void testInit() {
        assertSame(e, ui.getDescriptor());
        assertNull(ui.getUiParent());
        assertEquals(0, ui.getUiChildren().size());
        assertEquals(0, ui.getUiAttributes().size());
    }
    
    /**
     * loadFrom() does nothing if the root node doesn't match what's expected
     */
    public void testLoadFrom_InvalidRoot() {
        assertEquals(0, ui.getUiChildren().size());
        MockXmlNode root = new MockXmlNode(null /* namespace */, "blah", Node.ELEMENT_NODE, null);
        ui.loadFromXmlNode(root);
        assertEquals(0, ui.getUiChildren().size());
    }

    /**
     * UiElementNode.loadFrom should be used to populate an empty ui node from an
     * existing XML node tree.
     */
    public void testLoadFrom_NewTree_1_Node() {
        MockXmlNode root = new MockXmlNode(null /* namespace */, "manifest", Node.ELEMENT_NODE,
            new MockXmlNode[] {
                new MockXmlNode(null /* namespace */, "application", Node.ELEMENT_NODE, null)
            });
        
        // get /manifest
        ui.loadFromXmlNode(root);
        assertEquals("manifest", ui.getDescriptor().getXmlName());
        assertEquals(1, ui.getUiChildren().size());
        assertEquals(0, ui.getUiAttributes().size());

        // get /manifest/application
        Iterator<UiElementNode> ui_child_it = ui.getUiChildren().iterator();
        UiElementNode application = ui_child_it.next();
        assertEquals("application", application.getDescriptor().getXmlName());
        assertEquals(0, application.getUiChildren().size());
        assertEquals(0, application.getUiAttributes().size());
    }


    public void testLoadFrom_NewTree_2_Nodes() {
        MockXmlNode root = new MockXmlNode(null /* namespace */, "manifest", Node.ELEMENT_NODE,
            new MockXmlNode[] {
                new MockXmlNode(null /* namespace */, "application", Node.ELEMENT_NODE, null),
                new MockXmlNode(null /* namespace */, "permission", Node.ELEMENT_NODE, null),
            });
        
        // get /manifest
        ui.loadFromXmlNode(root);
        assertEquals("manifest", ui.getDescriptor().getXmlName());
        assertEquals(2, ui.getUiChildren().size());
        assertEquals(0, ui.getUiAttributes().size());

        // get /manifest/application
        Iterator<UiElementNode> ui_child_it = ui.getUiChildren().iterator();
        UiElementNode application = ui_child_it.next();
        assertEquals("application", application.getDescriptor().getXmlName());
        assertEquals(0, application.getUiChildren().size());
        assertEquals(0, application.getUiAttributes().size());

        // get /manifest/permission
        UiElementNode first_permission = ui_child_it.next();
        assertEquals("permission", first_permission.getDescriptor().getXmlName());
        assertEquals(0, first_permission.getUiChildren().size());
        assertEquals(0, first_permission.getUiAttributes().size());
    }

    public void testLoadFrom_NewTree_N_Nodes() {
        MockXmlNode root = new MockXmlNode(null /* namespace */, "manifest", Node.ELEMENT_NODE,
            new MockXmlNode[] {
                new MockXmlNode(null /* namespace */, "application", Node.ELEMENT_NODE,
                    new MockXmlNode[] {
                        new MockXmlNode(null /* namespace */, "activity", Node.ELEMENT_NODE,
                            null),
                        new MockXmlNode(null /* namespace */, "activity", Node.ELEMENT_NODE,
                            new MockXmlNode[] {
                                new MockXmlNode(null /* namespace */, "intent-filter",
                                        Node.ELEMENT_NODE, null),
                            }),
                        new MockXmlNode(null /* namespace */, "provider", Node.ELEMENT_NODE,
                                null),
                        new MockXmlNode(null /* namespace */, "provider", Node.ELEMENT_NODE,
                                null),
                    }),
                new MockXmlNode(null /* namespace */, "permission", Node.ELEMENT_NODE,
                        null),
                new MockXmlNode(null /* namespace */, "permission", Node.ELEMENT_NODE,
                        null),
            });
        
        // get /manifest
        ui.loadFromXmlNode(root);
        assertEquals("manifest", ui.getDescriptor().getXmlName());
        assertEquals(3, ui.getUiChildren().size());
        assertEquals(0, ui.getUiAttributes().size());

        // get /manifest/application
        Iterator<UiElementNode> ui_child_it = ui.getUiChildren().iterator();
        UiElementNode application = ui_child_it.next();
        assertEquals("application", application.getDescriptor().getXmlName());
        assertEquals(4, application.getUiChildren().size());
        assertEquals(0, application.getUiAttributes().size());

        // get /manifest/application/activity #1
        Iterator<UiElementNode> app_child_it = application.getUiChildren().iterator();
        UiElementNode first_activity = app_child_it.next();
        assertEquals("activity", first_activity.getDescriptor().getXmlName());
        assertEquals(0, first_activity.getUiChildren().size());
        assertEquals(0, first_activity.getUiAttributes().size());

        // get /manifest/application/activity #2
        UiElementNode second_activity = app_child_it.next();
        assertEquals("activity", second_activity.getDescriptor().getXmlName());
        assertEquals(1, second_activity.getUiChildren().size());
        assertEquals(0, second_activity.getUiAttributes().size());

        // get /manifest/application/activity #2/intent-filter #1
        Iterator<UiElementNode> activity_child_it = second_activity.getUiChildren().iterator();
        UiElementNode intent_filter = activity_child_it.next();
        assertEquals("intent-filter", intent_filter.getDescriptor().getXmlName());
        assertEquals(0, intent_filter.getUiChildren().size());
        assertEquals(0, intent_filter.getUiAttributes().size());

        // get /manifest/application/provider #1
        UiElementNode first_provider = app_child_it.next();
        assertEquals("provider", first_provider.getDescriptor().getXmlName());
        assertEquals(0, first_provider.getUiChildren().size());
        assertEquals(0, first_provider.getUiAttributes().size());

        // get /manifest/application/provider #2
        UiElementNode second_provider = app_child_it.next();
        assertEquals("provider", second_provider.getDescriptor().getXmlName());
        assertEquals(0, second_provider.getUiChildren().size());
        assertEquals(0, second_provider.getUiAttributes().size());
        
        // get /manifest/permission #1
        UiElementNode first_permission = ui_child_it.next();
        assertEquals("permission", first_permission.getDescriptor().getXmlName());
        assertEquals(0, first_permission.getUiChildren().size());
        assertEquals(0, first_permission.getUiAttributes().size());

        // get /manifest/permission #1
        UiElementNode second_permission = ui_child_it.next();
        assertEquals("permission", second_permission.getDescriptor().getXmlName());
        assertEquals(0, second_permission.getUiChildren().size());
        assertEquals(0, second_permission.getUiAttributes().size());
    }
    
    
}
