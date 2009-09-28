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

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import junit.framework.TestCase;

/**
 * Tests local validation of a Layout-Configs sample XMLs using an XML Schema validator.
 */
public class TestLayoutConfisXsd extends TestCase {

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

    /** Helper method that returns a validator for our XSD */
    private Validator getValidator(CaptureErrorHandler handler) throws SAXException {
        InputStream xsdStream = LayoutConfigsXsd.getXsdStream();
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(xsdStream));
        Validator validator = schema.newValidator();
        if (handler != null) {
            validator.setErrorHandler(handler);
        }

        return validator;
    }

    /** Validate a valid sample using an InputStream */
    public void testValidateLocalRepositoryFile() throws Exception {

        InputStream xmlStream =
            TestLayoutConfisXsd.class.getResourceAsStream("config_sample.xml");
        Source source = new StreamSource(xmlStream);

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = getValidator(handler);
        validator.validate(source);
        handler.verify();
    }

    /** An helper that validates a string against an expected regexp. */
    private void assertRegex(String expectedRegexp, String actualString) {
        assertNotNull(actualString);
        assertTrue(
                String.format("Regexp Assertion Failed:\nExpected: %s\nActual: %s\n",
                        expectedRegexp, actualString),
                actualString.matches(expectedRegexp));
    }

    // --- Tests ------------

    /** A document should at least have a root to be valid */
    public void testEmptyXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>";

        Source source = new StreamSource(new StringReader(document));

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = getValidator(handler);

        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect to get this specific exception message
            assertRegex("Premature end of file.*", e.getMessage());
            return;
        }
        // We shouldn't get here
        handler.verify();
        fail();
    }

    /** A document with an unknown element. */
    public void testUnknownContentXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<d:layout-configs xmlns:d=\"http://schemas.android.com/sdk/android/layout-configs/1\" >" +
            "<d:unknown />" +
            "</d:layout-configs>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse expression referring to this grammar rule
            assertRegex("cvc-complex-type.2.4.a: Invalid content was found.*", e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** A document with an missing attribute in a device element. */
    public void testIncompleteContentXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<d:layout-configs xmlns:d=\"http://schemas.android.com/sdk/android/layout-configs/1\" >" +
            "<d:device />" +
            "</d:layout-configs>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-complex-type.4: Attribute 'name' must appear on element 'd:device'.", e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** A document with a root element containing no device element is not valid. */
    public void testEmptyRootXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<d:layout-configs xmlns:d=\"http://schemas.android.com/sdk/android/layout-configs/1\" />";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse expression referring to this grammar rule
            assertRegex("cvc-complex-type.2.4.b: The content of element 'd:layout-configs' is not complete.*", e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** A document with an empty device element is not valid. */
    public void testEmptyDeviceXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<d:layout-configs xmlns:d=\"http://schemas.android.com/sdk/android/layout-configs/1\" >" +
            "<d:device name=\"foo\"/>" +
            "</d:layout-configs>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-complex-type.2.4.b: The content of element 'd:device' is not complete.*", e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** A document with two default elements in a device element is not valid. */
    public void testTwoDefaultsXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<d:layout-configs xmlns:d=\"http://schemas.android.com/sdk/android/layout-configs/1\" >" +
            "<d:device name=\"foo\">" +
            "  <d:default />" +
            "  <d:default />" +
            "</d:device>" +
            "</d:layout-configs>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-complex-type.2.4.a: Invalid content was found starting with element 'd:default'.*", e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** The default elements must be defined before the config one. It's invalid if after. */
    public void testDefaultConfigOrderXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<d:layout-configs xmlns:d=\"http://schemas.android.com/sdk/android/layout-configs/1\" >" +
            "<d:device name=\"foo\">" +
            "  <d:config name=\"must-be-after-default\" />" +
            "  <d:default />" +
            "</d:device>" +
            "</d:layout-configs>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-complex-type.2.4.a: Invalid content was found starting with element 'd:default'.*", e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }
}
