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

package com.android.sdkmanager.repository;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.InputStream;

import javax.xml.XMLConstants;
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
public class TestXml extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testValidateLocalRepositoryFile() throws Exception {

        InputStream xsdStream = SdkRepositoryConstants.getXsdStream();
        InputStream xmlStream = TestXml.class.getResourceAsStream("repository_sample.xml");

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = factory.newSchema(new StreamSource(xsdStream));

        Validator validator = schema.newValidator();

        CaptureErrorHandler handler = new CaptureErrorHandler();
        validator.setErrorHandler(handler);

        validator.validate(new StreamSource(xmlStream));

        String warnings = handler.getWarnings();
        if (warnings.length() > 0) {
            System.err.println(warnings);
        }

        String errors = handler.getErrors();
        if (errors.length() > 0) {
            System.err.println(errors);
            fail(errors);
        }
    }

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
         * @throws SAXException
         */
        public void error(SAXParseException ex) throws SAXException {
            mErrors += "Error: " + ex.getMessage() + "\n";
        }

        public void fatalError(SAXParseException ex) throws SAXException {
            mErrors += "Fatal Error: " + ex.getMessage() + "\n";
            throw ex;
        }

        /**
         * @throws SAXException
         */
        public void warning(SAXParseException ex) throws SAXException {
            mWarnings += "Warning: " + ex.getMessage() + "\n";
        }

    }

}
