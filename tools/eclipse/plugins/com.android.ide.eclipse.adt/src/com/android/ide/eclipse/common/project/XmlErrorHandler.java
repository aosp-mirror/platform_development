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

package com.android.ide.eclipse.common.project;

import com.android.ide.eclipse.common.AndroidConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML error handler used by the parser to report errors/warnings.
 */
public class XmlErrorHandler extends DefaultHandler {

    /** file being parsed */
    private IFile mFile;

    /** link to the delta visitor, to set the xml error flag */
    private XmlErrorListener mErrorListener;
    
    /**
     * Classes which implement this interface provide a method that deals
     * with XML errors.
     */
    public interface XmlErrorListener {
        /**
         * Sent when an XML error is detected.
         */
        public void errorFound();
    }
    
    public static class BasicXmlErrorListener implements XmlErrorListener {
        public boolean mHasXmlError = false;
        
        public void errorFound() {
            mHasXmlError = true;
        }
    }

    public XmlErrorHandler(IFile file, XmlErrorListener errorListener) {
        mFile = file;
        mErrorListener = errorListener;
    }

    /**
     * Xml Error call back
     * @param exception the parsing exception
     * @throws SAXException 
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        handleError(exception, exception.getLineNumber());
    }

    /**
     * Xml Fatal Error call back
     * @param exception the parsing exception
     * @throws SAXException 
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        handleError(exception, exception.getLineNumber());
    }

    /**
     * Xml Warning call back
     * @param exception the parsing exception
     * @throws SAXException 
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        if (mFile != null) {
            BaseProjectHelper.addMarker(mFile,
                    AndroidConstants.MARKER_XML,
                    exception.getMessage(),
                    exception.getLineNumber(),
                    IMarker.SEVERITY_WARNING);
        }
    }
    
    protected final IFile getFile() {
        return mFile;
    }
    
    /**
     * Handles a parsing error and an optional line number.
     * @param exception
     * @param lineNumber
     */
    protected void handleError(Exception exception, int lineNumber) {
        if (mErrorListener != null) {
            mErrorListener.errorFound();
        }
        
        if (mFile != null) {
            if (lineNumber != -1) {
                BaseProjectHelper.addMarker(mFile,
                        AndroidConstants.MARKER_XML,
                        exception.getMessage(),
                        lineNumber,
                        IMarker.SEVERITY_ERROR);
            } else {
                BaseProjectHelper.addMarker(mFile,
                        AndroidConstants.MARKER_XML,
                        exception.getMessage(),
                        IMarker.SEVERITY_ERROR);
            }
        }
    }
}
