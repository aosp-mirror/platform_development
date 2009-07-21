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

package com.android.sdklib.repository;

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
 * Tests local validation of an SDK Repository sample XMLs using an XML Schema validator.
 *
 * References:
 * http://www.ibm.com/developerworks/xml/library/x-javaxmlvalidapi.html
 */
public class TestSdkRepository extends TestCase {

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

        public String getErrors() {
            return mErrors;
        }

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
        InputStream xsdStream = SdkRepository.getXsdStream();
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
            TestSdkRepository.class.getResourceAsStream("repository_sample.xml");
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

    /** A document with a root element containing no platform, addon, etc., is valid. */
    public void testEmptyRootXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<r:sdk-repository xmlns:r=\"http://schemas.android.com/sdk/android/repository/1\" />";

        Source source = new StreamSource(new StringReader(document));

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = getValidator(handler);
        validator.validate(source);
        handler.verify();
    }

    /** A document with an unknown element. */
    public void testUnknownContentXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<r:sdk-repository xmlns:r=\"http://schemas.android.com/sdk/android/repository/1\" >" +
            "<r:unknown />" +
            "</r:sdk-repository>";

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

    /** A document with an incomplete element. */
    public void testIncompleteContentXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<r:sdk-repository xmlns:r=\"http://schemas.android.com/sdk/android/repository/1\" >" +
            "<r:platform> <r:api-level>1</r:api-level> <r:libs /> </r:platform>" +
            "</r:sdk-repository>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-complex-type.2.4.a: Invalid content was found.*", e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** A document with a wrong type element. */
    public void testWrongTypeContentXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            "<r:sdk-repository xmlns:r=\"http://schemas.android.com/sdk/android/repository/1\" >" +
            "<r:platform> <r:api-level>NotAnInteger</r:api-level> <r:libs /> </r:platform>" +
            "</r:sdk-repository>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-datatype-valid.1.2.1: 'NotAnInteger' is not a valid value.*",
                    e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** A document an unknown license id. */
    public void testLicenseIdNotFound() throws Exception {
        // we define a license named "lic1" and then reference "lic2" instead
        String document = "<?xml version=\"1.0\"?>" +
            "<r:sdk-repository xmlns:r=\"http://schemas.android.com/sdk/android/repository/1\" >" +
            "<r:license id=\"lic1\"> some license </r:license> " +
            "<r:tool> <r:uses-license ref=\"lic2\" /> <r:revision>1</r:revision> " +
            "<r:archives> <r:archive os=\"any\"> <r:size>1</r:size> <r:checksum>2822ae37115ebf13412bbef91339ee0d9454525e</r:checksum> " +
            "<r:url>url</r:url> </r:archive> </r:archives> </r:tool>" +
            "</r:sdk-repository>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-id.1: There is no ID/IDREF binding for IDREF 'lic2'.*",
                    e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }

    /** A document a slash in an extra path. */
    public void testExtraPathWithSlash() throws Exception {
        // we define a license named "lic1" and then reference "lic2" instead
        String document = "<?xml version=\"1.0\"?>" +
            "<r:sdk-repository xmlns:r=\"http://schemas.android.com/sdk/android/repository/1\" >" +
            "<r:extra> <r:revision>1</r:revision> <r:path>path/cannot\\contain\\segments</r:path> " +
            "<r:archives> <r:archive os=\"any\"> <r:size>1</r:size> <r:checksum>2822ae37115ebf13412bbef91339ee0d9454525e</r:checksum> " +
            "<r:url>url</r:url> </r:archive> </r:archives> </r:extra>" +
            "</r:sdk-repository>";

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(null);
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect a parse error referring to this grammar rule
            assertRegex("cvc-pattern-valid: Value 'path/cannot\\\\contain\\\\segments' is not facet-valid with respect to pattern.*",
                    e.getMessage());
            return;
        }
        // If we get here, the validator has not failed as we expected it to.
        fail();
    }
}
