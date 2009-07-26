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

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.TextAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.UiElementPullParser;
import com.android.ide.eclipse.adt.internal.editors.mock.MockXmlNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.sdklib.SdkConstants;

import org.w3c.dom.Node;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.HashMap;

import junit.framework.TestCase;

public class UiElementPullParserTest extends TestCase {

    private UiElementNode ui;
    private HashMap<String, String> button1Map;
    private HashMap<String, String> button2Map;
    private HashMap<String, String> textMap;

    @Override
    protected void setUp() throws Exception {
        // set up some basic descriptors.
        // We have button, textview, linear layout, relative layout.
        // only the layouts have children (all 4 descriptors possible)
        // Also add some dummy attributes.
        ElementDescriptor buttonDescriptor = new ElementDescriptor("Button", "Button", "", "",
                new AttributeDescriptor[] {
                    new TextAttributeDescriptor("name", "name", SdkConstants.NS_RESOURCES, ""),
                    new TextAttributeDescriptor("text", "text", SdkConstants.NS_RESOURCES, ""),
                    },
                new ElementDescriptor[] {}, false);

        ElementDescriptor textDescriptor = new ElementDescriptor("TextView", "TextView", "", "",
                new AttributeDescriptor[] {
                new TextAttributeDescriptor("name", "name", SdkConstants.NS_RESOURCES, ""),
                new TextAttributeDescriptor("text", "text", SdkConstants.NS_RESOURCES, ""), },
                new ElementDescriptor[] {}, false);

        ElementDescriptor linearDescriptor = new ElementDescriptor("LinearLayout", "Linear Layout",
                "", "",
                new AttributeDescriptor[] {
                    new TextAttributeDescriptor("orientation", "orientation",
                            SdkConstants.NS_RESOURCES, ""),
                },
                new ElementDescriptor[] { }, false);

        ElementDescriptor relativeDescriptor = new ElementDescriptor("RelativeLayout",
                "Relative Layout", "", "",
                new AttributeDescriptor[] {
                    new TextAttributeDescriptor("orientation", "orientation",
                            SdkConstants.NS_RESOURCES, ""),
                },
                new ElementDescriptor[] { }, false);

        ElementDescriptor[] a = new ElementDescriptor[] {
                buttonDescriptor, textDescriptor, linearDescriptor, relativeDescriptor
        };
        
        linearDescriptor.setChildren(a);
        relativeDescriptor.setChildren(a);

        // document descriptor
        ElementDescriptor rootDescriptor = new ElementDescriptor("root", "", "", "",
                new AttributeDescriptor[] { }, a, false);

        
        ui = new UiElementNode(rootDescriptor);
        
        /* create a dummy XML file.
         * <LinearLayout android:orientation="vertical">
         *      <Button android:name="button1" android:text="button1text"/>
         *      <RelativeLayout android:orientation="toto">
         *          <Button android:name="button2" android:text="button2text"/>
         *          <TextView android:name="text1" android:text="text1text"/>
         *      </RelativeLayout>
         * </LinearLayout>
         */
        MockXmlNode button1 = new MockXmlNode(null /* namespace */, "Button", Node.ELEMENT_NODE,
                null);
        button1.addAttributes(SdkConstants.NS_RESOURCES, "name", "button1");
        button1.addAttributes(SdkConstants.NS_RESOURCES, "text", "button1text");
        
        // create a map of the attributes we add to the multi-attribute nodes so that
        // we can more easily test the values when we parse the XML.
        // This is due to some attributes showing in a certain order for a node and in a different
        // order in another node. Since the order doesn't matter, we just simplify the test.
        button1Map = new HashMap<String, String>();
        button1Map.put("name", "button1");
        button1Map.put("text", "button1text");

        MockXmlNode button2 = new MockXmlNode(null /* namespace */, "Button", Node.ELEMENT_NODE,
                null);
        button2.addAttributes(SdkConstants.NS_RESOURCES, "name", "button2");
        button2.addAttributes(SdkConstants.NS_RESOURCES, "text", "button2text");

        button2Map = new HashMap<String, String>();
        button2Map.put("name", "button2");
        button2Map.put("text", "button2text");
        
        MockXmlNode text = new MockXmlNode(null /* namespace */, "TextView", Node.ELEMENT_NODE,
                null);
        text.addAttributes(SdkConstants.NS_RESOURCES, "name", "text1");
        text.addAttributes(SdkConstants.NS_RESOURCES, "text", "text1text");

        textMap = new HashMap<String, String>();
        textMap.put("name", "text1");
        textMap.put("text", "text1text");

        MockXmlNode relative = new MockXmlNode(null /* namespace */, "RelativeLayout",
                Node.ELEMENT_NODE, new MockXmlNode[] { button2, text });
        relative.addAttributes(SdkConstants.NS_RESOURCES, "orientation", "toto");
        
        MockXmlNode linear = new MockXmlNode(null /* namespace */, "LinearLayout",
                Node.ELEMENT_NODE, new MockXmlNode[] { button1, relative });
        linear.addAttributes(SdkConstants.NS_RESOURCES, "orientation", "vertical");
        
        MockXmlNode root = new MockXmlNode(null /* namespace */, "root", Node.ELEMENT_NODE,
                new MockXmlNode[] { linear });
        
        // put the namespace/prefix in place
        root.setPrefix(SdkConstants.NS_RESOURCES, "android");

        // load the xml into the UiElementNode
        ui.loadFromXmlNode(root);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testParser() {
        try {
            // wrap the parser around the ui element node, and start parsing
            UiElementPullParser parser = new UiElementPullParser(ui);
            
            assertEquals(XmlPullParser.START_DOCUMENT, parser.getEventType());
            
            // top level Linear layout
            assertEquals(XmlPullParser.START_TAG, parser.next());
            assertEquals("LinearLayout", parser.getName());
            assertEquals(1, parser.getAttributeCount());
            assertEquals("orientation", parser.getAttributeName(0));
            assertEquals(SdkConstants.NS_RESOURCES, parser.getAttributeNamespace(0));
            assertEquals("android", parser.getAttributePrefix(0));
            assertEquals("vertical", parser.getAttributeValue(0));
            
            // Button
            assertEquals(XmlPullParser.START_TAG, parser.next());
            assertEquals("Button", parser.getName());
            assertEquals(2, parser.getAttributeCount());
            check(parser, 0, button1Map);
            check(parser, 1, button1Map);
            // end of button
            assertEquals(XmlPullParser.END_TAG, parser.next());

            // Relative Layout
            assertEquals(XmlPullParser.START_TAG, parser.next());
            assertEquals("RelativeLayout", parser.getName());
            assertEquals(1, parser.getAttributeCount());
            assertEquals("orientation", parser.getAttributeName(0));
            assertEquals(SdkConstants.NS_RESOURCES, parser.getAttributeNamespace(0));
            assertEquals("android", parser.getAttributePrefix(0));
            assertEquals("toto", parser.getAttributeValue(0));

            // Button
            assertEquals(XmlPullParser.START_TAG, parser.next());
            assertEquals("Button", parser.getName());
            assertEquals(2, parser.getAttributeCount());
            check(parser, 0, button2Map);
            check(parser, 1, button2Map);
            // end of button
            assertEquals(XmlPullParser.END_TAG, parser.next());

            // TextView
            assertEquals(XmlPullParser.START_TAG, parser.next());
            assertEquals("TextView", parser.getName());
            assertEquals(2, parser.getAttributeCount());
            check(parser, 0, textMap);
            check(parser, 1, textMap);
            // end of TextView
            assertEquals(XmlPullParser.END_TAG, parser.next());
            
            // end of RelativeLayout
            assertEquals(XmlPullParser.END_TAG, parser.next());

            
            // end of top level linear layout
            assertEquals(XmlPullParser.END_TAG, parser.next());
            
            assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Receives a {@link XmlPullParser} at the START_TAG level, and checks the i-th attribute
     * to be present in the {@link HashMap} with the proper (name, value)
     * @param parser
     * @param i
     * @param map
     */
    private void check(UiElementPullParser parser, int i, HashMap<String, String> map) {
        String name = parser.getAttributeName(i);
        String value = parser.getAttributeValue(i);
        
        String referenceValue = map.get(name);
        assertNotNull(referenceValue);
        assertEquals(referenceValue, value);
        
        assertEquals(SdkConstants.NS_RESOURCES, parser.getAttributeNamespace(i));
        assertEquals("android", parser.getAttributePrefix(i));
    }

}
