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
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;

import junit.framework.TestCase;

/**
 * Tests local validation of a Layout-Devices sample XMLs using an XML Schema validator.
 */
public class TestLayoutDevicesXsd extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * A SAX error handler that captures the errors and warnings.
     * This allows us to capture *all* errors and just not get an exception on the first one.
     */
    private static class CaptureErrorHandler implements ErrorHandler {

        private String mWarnings = "";
        private String mErrors = "";

        @SuppressWarnings("unused")
        public String getErrors() {
            return mErrors;
        }

        @SuppressWarnings("unused")
        public String getWarnings() {
            return mWarnings;
        }

        /**
         * Verifies if the handler captures some errors or warnings.
         * Prints them on stderr.
         * Also fails the unit test if any error was generated.
         */
        public void verify() {
            if (mWarnings.length() > 0) {
                System.err.println(mWarnings);
            }

            if (mErrors.length() > 0) {
                System.err.println(mErrors);
                fail(mErrors);
            }
        }

        /**
         * @throws SAXException
         */
        public void error(SAXParseException ex) throws SAXException {
            mErrors += "Error: " + ex.getMessage() + "\n";
        }

        /**
         * @throws SAXException
         */
        public void fatalError(SAXParseException ex) throws SAXException {
            mErrors += "Fatal Error: " + ex.getMessage() + "\n";
        }

        /**
         * @throws SAXException
         */
        public void warning(SAXParseException ex) throws SAXException {
            mWarnings += "Warning: " + ex.getMessage() + "\n";
        }

    }

    // --- Helpers ------------

    /** An helper that validates a string against an expected regexp. */
    private void assertRegex(String expectedRegexp, String actualString) {
        assertNotNull(actualString);
        assertTrue(
                String.format("Regexp Assertion Failed:\nExpected: %s\nActual: %s\n",
                        expectedRegexp, actualString),
                actualString.matches(expectedRegexp));
    }

    public void checkFailure(String document, String regexp) throws Exception {
        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = LayoutDevicesXsd.getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse expression referring to this grammar rule
            assertRegex(regexp, e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    public void checkSuccess(String document) throws Exception {
        Source source = new StreamSource(new StringReader(document));

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = LayoutDevicesXsd.getValidator(null);
        validator.validate(source);
        handler.verify();
    }

    // --- Tests ------------

    /** Validate a valid sample using an InputStream */
    public void testValidateLocalRepositoryFile() throws Exception {

        InputStream xmlStream =
            TestLayoutDevicesXsd.class.getResourceAsStream("config_sample.xml");
        Source source = new StreamSource(xmlStream);

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = LayoutDevicesXsd.getValidator(handler);
        validator.validate(source);
        handler.verify();
    }

    /** A document should at least have a root to be valid */
    public void testEmptyXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>",

                // expected failure
                "Premature end of file.*");
    }

    /** A document with an unknown element. */
    public void testUnknownContentXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:unknown />" +
                "</d:layout-devices>",

                // expected failure
                "cvc-complex-type.2.4.a: Invalid content was found.*");
    }

    /** A document with an missing attribute in a device element. */
    public void testIncompleteContentXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device />" +
                "</d:layout-devices>",

                // expected failure
                "cvc-complex-type.4: Attribute 'name' must appear on element 'd:device'.");
    }

    /** A document with a root element containing no device element is not valid. */
    public void testEmptyRootXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" />",

                // expected failure
                "cvc-complex-type.2.4.b: The content of element 'd:layout-devices' is not complete.*");
    }

    /** A document with an empty device element is not valid. */
    public void testEmptyDeviceXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\"/>" +
                "</d:layout-devices>",

                // expected failure
                "cvc-complex-type.2.4.b: The content of element 'd:device' is not complete.*");
    }

    /** A document with two default elements in a device element is not valid. */
    public void testTwoDefaultsXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\">" +
                "  <d:default />" +
                "  <d:default />" +
                "</d:device>" +
                "</d:layout-devices>",

                // expected failure
                "cvc-complex-type.2.4.a: Invalid content was found starting with element 'd:default'.*");
    }

    /** The default elements must be defined before the config one. It's invalid if after. */
    public void testDefaultConfigOrderXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\">" +
                "  <d:config name=\"must-be-after-default\" />" +
                "  <d:default />" +
                "</d:device>" +
                "</d:layout-devices>",

                // expected failure
                "cvc-complex-type.2.4.a: Invalid content was found starting with element 'd:default'.*");
    }

    /** Screen dimension cannot be 0. */
    public void testScreenDimZeroXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\">" +
                "  <d:default>" +
                "    <d:screen-dimension> <d:size>0</d:size> <d:size>1</d:size> </d:screen-dimension>" +
                "  </d:default>" +
                "</d:device>" +
                "</d:layout-devices>",

                // expected failure
                "cvc-minInclusive-valid: Value '0' is not facet-valid with respect to minInclusive '1'.*");
    }

    /** Screen dimension cannot be negative. */
    public void testScreenDimNegativeXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\">" +
                "  <d:default>" +
                "    <d:screen-dimension> <d:size>-5</d:size> <d:size>1</d:size> </d:screen-dimension>" +
                "  </d:default>" +
                "</d:device>" +
                "</d:layout-devices>",

                // expected failure
                "cvc-minInclusive-valid: Value '-5' is not facet-valid with respect to minInclusive '1'.*");
    }

    /** X/Y dpi cannot be 0. */
    public void testXDpiZeroXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\">" +
                "  <d:default>" +
                "    <d:xdpi>0</d:xdpi>" +
                "  </d:default>" +
                "</d:device>" +
                "</d:layout-devices>",

                // expected failure
                "cvc-minExclusive-valid: Value '0' is not facet-valid with respect to minExclusive '0.0E1'.*");
    }


    /** X/Y dpi cannot be negative. */
    public void testXDpiNegativeXml() throws Exception {
        checkFailure(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\">" +
                "  <d:default>" +
                "    <d:xdpi>-3.1415926538</d:xdpi>" +
                "  </d:default>" +
                "</d:device>" +
                "</d:layout-devices>",

                // expected failure
                "cvc-minExclusive-valid: Value '-3.1415926538' is not facet-valid with respect to minExclusive '0.0E1'.*");
    }

    /** WHitespace around token is accepted by the schema. */
    public void testTokenWhitespaceXml() throws Exception {
        checkSuccess(
                // document
                "<?xml version=\"1.0\"?>" +
                "<d:layout-devices xmlns:d=\"http://schemas.android.com/sdk/android/layout-devices/1\" >" +
                "<d:device name=\"foo\">" +
                "  <d:config name='foo'>" +
                "    <d:screen-ratio>  \n long \r </d:screen-ratio>" +
                "  </d:config>" +
                "</d:device>" +
                "</d:layout-devices>");
    }

}

