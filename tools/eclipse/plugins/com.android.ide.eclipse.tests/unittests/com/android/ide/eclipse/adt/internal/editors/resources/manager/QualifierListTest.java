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

package com.android.ide.eclipse.adt.internal.editors.resources.manager;

import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.ResourceQualifier;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;

import java.lang.reflect.Field;

import junit.framework.TestCase;

public class QualifierListTest extends TestCase {
    
    private ResourceManager mManager;

    @Override
    public void setUp()  throws Exception {
        super.setUp();
        
        mManager = ResourceManager.getInstance();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mManager = null;
    }
    
    public void testQualifierList() {
        try {
            // get the list of qualifier in the resource manager
            Field qualifierListField = ResourceManager.class.getDeclaredField("mQualifiers");
            assertNotNull(qualifierListField);
            qualifierListField.setAccessible(true);
            
            // get the actual list.
            ResourceQualifier[] qualifierList =
                (ResourceQualifier[])qualifierListField.get(mManager);
            
            // now get the number of qualifier in the FolderConfiguration
            Field qualCountField = FolderConfiguration.class.getDeclaredField("INDEX_COUNT");
            assertNotNull(qualCountField);
            qualCountField.setAccessible(true);
            
            // get the constant value
            Integer count = (Integer)qualCountField.get(null);
            
            // now compare
            assertEquals(count.intValue(), qualifierList.length);
        } catch (SecurityException e) {
            assertTrue(false);
        } catch (NoSuchFieldException e) {
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertTrue(false);
        } catch (IllegalAccessException e) {
            assertTrue(false);
        }
    }
}

