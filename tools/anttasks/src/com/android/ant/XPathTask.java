/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ant;

import com.android.sdklib.xml.AndroidXPathFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.xml.sax.InputSource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

/**
 * Android specific XPath task.
 * The goal is to get the result of an XPath expression on Android XML files. The android namespace
 * (http://schemas.android.com/apk/res/android) must be associated to the "android" prefix.
 */
public class XPathTask extends Task {

    private Path mManifestFile;
    private String mProperty;
    private String mExpression;

    public void setInput(Path manifestFile) {
        mManifestFile = manifestFile;
    }

    public void setOutput(String property) {
        mProperty = property;
    }

    public void setExpression(String expression) {
        mExpression = expression;
    }

    @Override
    public void execute() throws BuildException {
        try {
            if (mManifestFile == null || mManifestFile.list().length == 0) {
                throw new BuildException("input attribute is missing!");
            }

            if (mProperty == null) {
                throw new BuildException("output attribute is missing!");
            }

            if (mExpression == null) {
                throw new BuildException("expression attribute is missing!");
            }

            XPath xpath = AndroidXPathFactory.newXPath();

            String file = mManifestFile.list()[0];
            String result = xpath.evaluate(mExpression, new InputSource(new FileInputStream(file)));

            getProject().setProperty(mProperty, result);
        } catch (XPathExpressionException e) {
            throw new BuildException(e);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        }
    }
}
