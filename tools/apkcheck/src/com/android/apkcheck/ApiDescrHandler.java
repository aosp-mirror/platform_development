/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apkcheck;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;


/**
 * Provides implementation for SAX parser.
 */
class ApiDescrHandler extends DefaultHandler {
    /*
     * Uber-container.
     */
    private ApiList mApiList;

    /*
     * Temporary objects, used as containers while we accumulate the
     * innards.
     */
    private PackageInfo mCurrentPackage = null;
    private ClassInfo mCurrentClass = null;
    private MethodInfo mCurrentMethod = null;

    /**
     * Constructs an ApiDescrHandler.
     *
     * @param fileName Source file name, used for debugging.
     */
    public ApiDescrHandler(ApiList apiList) {
        mApiList = apiList;
    }

    /**
     * Returns the ApiList in its current state.  Generally only
     * makes sense to call here after parsing is completed.
     */
    public ApiList getApiList() {
        return mApiList;
    }

    /**
     * Processes start tags.  If the file is malformed we will likely
     * NPE, but this is captured by the caller.
     *
     * We currently assume that packages and classes only appear once,
     * so all classes associated with a package are wrapped in a singular
     * instance of &lt;package&gt;.  We may want to remove this assumption
     * by attempting to find an existing package/class with the same name.
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) {

        if (qName.equals("package")) {
            /* top-most element */
            mCurrentPackage = mApiList.getOrCreatePackage(
                    attributes.getValue("name"));
        } else if (qName.equals("class") || qName.equals("interface")) {
            /* get class, gather fields/methods and interfaces */
            mCurrentClass = mCurrentPackage.getOrCreateClass(
                    attributes.getValue("name"),
                    attributes.getValue("extends"),
                    attributes.getValue("static"));
        } else if (qName.equals("implements")) {
            /* add name of interface to current class */
            mCurrentClass.addInterface(attributes.getValue("name"));
        } else if (qName.equals("method")) {
            /* hold object while we gather parameters */
            mCurrentMethod = new MethodInfo(attributes.getValue("name"),
                attributes.getValue("return"));
        } else if (qName.equals("constructor")) {
            /* like "method", but has no name or return type */
            mCurrentMethod = new MethodInfo("<init>", "void");

            /*
             * If this is a non-static inner class, we want to add the
             * "hidden" outer class parameter as the first parameter.
             * We can tell if it's an inner class because the class name
             * will include a '$' (it has been normalized already).
             */
            String staticClass = mCurrentClass.getStatic();
            if (staticClass == null) {
                /*
                 * We're parsing an APK file, which means we can't know
                 * if the class we're referencing is static or not.  We
                 * also already have the "secret" first parameter
                 * represented in the method parameter list, so we don't
                 * need to insert it here.
                 */
            } else if ("false".equals(staticClass)) {
                String className = mCurrentClass.getName();
                int dollarIndex = className.indexOf('$');
                if (dollarIndex >= 0) {
                    String outerClass = className.substring(0, dollarIndex);
                    //System.out.println("--- inserting " +
                    //    mCurrentPackage.getName() + "." + outerClass +
                    //    " into constructor for " + className);
                    mCurrentMethod.addParameter(mCurrentPackage.getName() +
                        "." + outerClass);
                }
            }
        } else if (qName.equals("field")) {
            /* add to current class */
            FieldInfo fInfo = new FieldInfo(attributes.getValue("name"),
                    attributes.getValue("type"));
            mCurrentClass.addField(fInfo);
        } else if (qName.equals("parameter")) {
            /* add to current method */
            mCurrentMethod.addParameter(attributes.getValue("type"));
        }
    }

    /**
     * Processes end tags.  Generally these add the under-construction
     * item to the appropriate container.
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("method") || qName.equals("constructor")) {
            /* add method to class */
            mCurrentClass.addMethod(mCurrentMethod);
            mCurrentMethod = null;
        } else if (qName.equals("class") || qName.equals("interface")) {
            mCurrentClass = null;
        } else if (qName.equals("package")) {
            mCurrentPackage = null;
        }
    }
}

