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

package com.android.ide.eclipse.adt.internal.sdk;


import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Public constants for the layout device description XML Schema.
 */
public class LayoutDevicesXsd {

    /** The XML namespace of the layout-configs XML. */
    public static final String NS_LAYOUT_DEVICE_XSD =
        "http://schemas.android.com/sdk/android/layout-devices/1";                  //$NON-NLS-1$

    /**
     * The "layout-devices" element is the root element of this schema.
     *
     * It must contain one or more "device" elements that each define the configurations
     * available for a given device.
     *
     * These definitions are used in the Graphical Layout Editor in the
     * Android Development Tools (ADT) plugin for Eclipse.
     */
    public static final String NODE_LAYOUT_DEVICES = "layout-devices";              //$NON-NLS-1$

    /**
     * A device element must contain at most one "default" element followed
     * by one or more ""config" elements.
     *
     * The "default" element defines all the default parameters inherited
     * by the following "config" elements. Each "config" element can override
     * the default values, if any.
     *
     * A "device" element also has a required "name" attribute that represents
     * the user-interface name of this device.
     */
    public static final String NODE_DEVICE = "device";                              //$NON-NLS-1$

    /**
     * The "default" element contains zero or more of all the parameter elements
     * listed below. It defines all the parameters that are common to all
     * declared "config" elements.
     */
    public static final String NODE_DEFAULT = "default";                            //$NON-NLS-1$

    /**
     * The "config" element contains zero or more of all the parameter elements
     * listed below. The parameters from the "default" element (if present) are
     * automatically inherited and can be overridden.
     */
    public static final String NODE_CONFIG = "config";                              //$NON-NLS-1$


    public static final String NODE_COUNTRY_CODE = "country-code";                  //$NON-NLS-1$

    public static final String NODE_NETWORK_CODE = "network-code";                  //$NON-NLS-1$

    public static final String NODE_SCREEN_SIZE = "screen-size";                    //$NON-NLS-1$

    public static final String NODE_SCREEN_RATIO = "screen-ratio";                  //$NON-NLS-1$

    public static final String NODE_SCREEN_ORIENTATION = "screen-orientation";      //$NON-NLS-1$

    public static final String NODE_PIXEL_DENSITY = "pixel-density";                //$NON-NLS-1$

    public static final String NODE_TOUCH_TYPE = "touch-type";                      //$NON-NLS-1$

    public static final String NODE_KEYBOARD_STATE = "keyboard-state";              //$NON-NLS-1$

    public static final String NODE_TEXT_INPUT_METHOD = "text-input-method";        //$NON-NLS-1$

    public static final String NODE_NAV_METHOD = "nav-method";                      //$NON-NLS-1$

    public static final String NODE_SCREEN_DIMENSION = "screen-dimension";          //$NON-NLS-1$

    /** The screen-dimension element has 2 size element children. */
    public static final String NODE_SIZE = "size";                                  //$NON-NLS-1$

    public static final String NODE_XDPI = "xdpi";                                  //$NON-NLS-1$

    public static final String NODE_YDPI = "ydpi";                                  //$NON-NLS-1$

    /**
     * The "name" attribute, used by both the "device" and the "config"
     * elements. It represents the user-interface name of these objects.
     */
    public static final String ATTR_NAME = "name";                                  //$NON-NLS-1$

    /**
     * Helper to get an input stream of the layout config XML schema.
     */
    public static InputStream getXsdStream() {
        return LayoutDevicesXsd.class.getResourceAsStream("layout-devices.xsd");    //$NON-NLS-1$
    }

    /** Helper method that returns a {@link Validator} for our XSD */
    public static Validator getValidator(ErrorHandler handler) throws SAXException {
        InputStream xsdStream = getXsdStream();
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(xsdStream));
        Validator validator = schema.newValidator();
        if (handler != null) {
            validator.setErrorHandler(handler);
        }

        return validator;
    }

}
