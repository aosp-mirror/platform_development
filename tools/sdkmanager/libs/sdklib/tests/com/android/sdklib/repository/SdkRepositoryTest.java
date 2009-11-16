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
public class SdkRepositoryTest extends TestCase {

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
    private Validator getValidator(int version, CaptureErrorHandler handler) throws SAXException {
        InputStream xsdStream = SdkRepository.getXsdStream(version);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(xsdStream));
        Validator validator = schema.newValidator();
        if (handler != null) {
            validator.setErrorHandler(handler);
        }

        return validator;
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

    /** Validate a valid sample using namespace version 1 using an InputStream */
    public void testValidateLocalRepositoryFile1() throws Exception {
        InputStream xmlStream = this.getClass().getResourceAsStream(
                    "/com/android/sdklib/testdata/repository_sample_1.xml");
        Source source = new StreamSource(xmlStream);

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = getValidator(1, handler);
        validator.validate(source);
        handler.verify();
    }

    /** Validate a valid sample using namespace version 2 using an InputStream */
    public void testValidateLocalRepositoryFile2() throws Exception {
        InputStream xmlStream = this.getClass().getResourceAsStream(
                    "/com/android/sdklib/testdata/repository_sample_2.xml");
        Source source = new StreamSource(xmlStream);

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = getValidator(2, handler);
        validator.validate(source);
        handler.verify();
    }

    /** Test that validating a v2 file using the v1 schema fails. */
    public void testValidateFile2UsingNs1() throws Exception {
        InputStream xmlStream = this.getClass().getResourceAsStream(
                    "/com/android/sdklib/testdata/repository_sample_2.xml");
        Source source = new StreamSource(xmlStream);

        Validator validator = getValidator(1, null); // validate v2 against v1... fail!

        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            // We expect to get this specific exception message
            assertRegex("cvc-elt.1: Cannot find the declaration of element 'sdk:sdk-repository'.*", e.getMessage());
            return;
        }
        // We shouldn't get here
        fail();
    }

    /** A document should at least have a root to be valid */
    public void testEmptyXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>";

        Source source = new StreamSource(new StringReader(document));

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = getValidator(SdkRepository.XSD_LATEST_VERSION, handler);

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

    private static String OPEN_TAG =
        "<r:sdk-repository xmlns:r=\"http://schemas.android.com/sdk/android/repository/" +
        Integer.toString(SdkRepository.XSD_LATEST_VERSION) +
        "\">";

    private static String CLOSE_TAG = "</r:sdk-repository>";

    /** A document with a root element containing no platform, addon, etc., is valid. */
    public void testEmptyRootXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            OPEN_TAG +
            CLOSE_TAG;

        Source source = new StreamSource(new StringReader(document));

        CaptureErrorHandler handler = new CaptureErrorHandler();
        Validator validator = getValidator(SdkRepository.XSD_LATEST_VERSION, handler);
        validator.validate(source);
        handler.verify();
    }

    /** A document with an unknown element. */
    public void testUnknownContentXml() throws Exception {
        String document = "<?xml version=\"1.0\"?>" +
            OPEN_TAG +
            "<r:unknown />" +
            CLOSE_TAG;

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(SdkRepository.XSD_LATEST_VERSION, null);
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
            OPEN_TAG +
            "<r:platform> <r:api-level>1</r:api-level> <r:libs /> </r:platform>" +
            CLOSE_TAG;

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(SdkRepository.XSD_LATEST_VERSION, null);
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
            OPEN_TAG +
            "<r:platform> <r:api-level>NotAnInteger</r:api-level> <r:libs /> </r:platform>" +
            CLOSE_TAG;

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(SdkRepository.XSD_LATEST_VERSION, null);
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
            OPEN_TAG +
            "<r:license id=\"lic1\"> some license </r:license> " +
            "<r:tool> <r:uses-license ref=\"lic2\" /> <r:revision>1</r:revision> " +
            "<r:archives> <r:archive os=\"any\"> <r:size>1</r:size> <r:checksum>2822ae37115ebf13412bbef91339ee0d9454525e</r:checksum> " +
            "<r:url>url</r:url> </r:archive> </r:archives> </r:tool>" +
            CLOSE_TAG;

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(SdkRepository.XSD_LATEST_VERSION, null);
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
            OPEN_TAG +
            "<r:extra> <r:revision>1</r:revision> <r:path>path/cannot\\contain\\segments</r:path> " +
            "<r:archives> <r:archive os=\"any\"> <r:size>1</r:size> <r:checksum>2822ae37115ebf13412bbef91339ee0d9454525e</r:checksum> " +
            "<r:url>url</r:url> </r:archive> </r:archives> </r:extra>" +
            CLOSE_TAG;

        Source source = new StreamSource(new StringReader(document));

        // don't capture the validator errors, we want it to fail and catch the exception
        Validator validator = getValidator(SdkRepository.XSD_LATEST_VERSION, null);
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
