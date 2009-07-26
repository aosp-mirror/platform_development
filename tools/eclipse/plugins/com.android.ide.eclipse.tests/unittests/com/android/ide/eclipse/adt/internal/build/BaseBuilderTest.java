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

package com.android.ide.eclipse.adt.internal.build;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class BaseBuilderTest extends TestCase {

    public void testParseAaptOutput() {
        Pattern p = Pattern.compile( "^(.+):(\\d+):\\s(.+)$"); //$NON-NLS-1$
        String s = "C:\\java\\workspace-android\\AndroidApp\\res\\values\\strings.xml:11: WARNING: empty 'some warning text";

        Matcher m = p.matcher(s);
        assertEquals(true, m.matches());
        assertEquals("C:\\java\\workspace-android\\AndroidApp\\res\\values\\strings.xml", m.group(1));
        assertEquals("11", m.group(2));
        assertEquals("WARNING: empty 'some warning text", m.group(3));
    }

}
