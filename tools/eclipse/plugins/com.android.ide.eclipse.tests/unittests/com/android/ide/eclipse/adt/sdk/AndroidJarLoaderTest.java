/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.adt.sdk;

import com.android.ide.eclipse.adt.sdk.IAndroidClassLoader.IClassDescriptor;
import com.android.ide.eclipse.tests.AdtTestData;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.TestCase;

/**
 * Unit Test for {@link AndroidJarLoader}.
 * 
 * Uses the classes jar.example.Class1/Class2 stored in tests/data/jar_example.jar.
 */
public class AndroidJarLoaderTest extends TestCase {

    private AndroidJarLoader mFrameworkClassLoader;

    /** Creates an instance of {@link AndroidJarLoader} on our test data JAR */ 
    @Override
    public void setUp() throws Exception {
        String jarfilePath = AdtTestData.getInstance().getTestFilePath("jar_example.jar");  //$NON-NLS-1$
        mFrameworkClassLoader = new AndroidJarLoader(jarfilePath);
    }

    @Override
    public void tearDown() throws Exception {
        mFrameworkClassLoader = null;
        System.gc();
    }

    /** Preloads classes. They should load just fine. */
    public final void testPreLoadClasses() throws Exception {
        mFrameworkClassLoader.preLoadClasses("jar.example.", null, null); //$NON-NLS-1$
        HashMap<String, Class<?>> map = getPrivateClassCache();
        assertEquals(0, map.size());
        HashMap<String,byte[]> data = getPrivateEntryCache();
        assertTrue(data.containsKey("jar.example.Class1"));                    //$NON-NLS-1$
        assertTrue(data.containsKey("jar.example.Class2"));                    //$NON-NLS-1$
        assertTrue(data.containsKey("jar.example.Class1$InnerStaticClass1"));  //$NON-NLS-1$
        assertTrue(data.containsKey("jar.example.Class1$InnerClass2"));  //$NON-NLS-1$
        assertEquals(4, data.size());
    }

    /** Preloads a class not in the JAR. Preloading does nothing in this case. */
    public final void testPreLoadClasses_classNotFound() throws Exception {
        mFrameworkClassLoader.preLoadClasses("not.a.package.", null, null);  //$NON-NLS-1$
        HashMap<String, Class<?>> map = getPrivateClassCache();
        assertEquals(0, map.size());
        HashMap<String,byte[]> data = getPrivateEntryCache();
        assertEquals(0, data.size());
    }

    /** Finds a class we just preloaded. It should work. */
    public final void testFindClass_classFound() throws Exception {
        Class<?> c = _findClass(mFrameworkClassLoader, "jar.example.Class2");  //$NON-NLS-1$
        assertEquals("jar.example.Class2", c.getName());              //$NON-NLS-1$
        HashMap<String, Class<?>> map = getPrivateClassCache();
        assertTrue(map.containsKey("jar.example.Class1"));            //$NON-NLS-1$
        assertTrue(map.containsKey("jar.example.Class2"));            //$NON-NLS-1$
        assertEquals(2, map.size());
    }
    
    /** call the protected method findClass */
    private Class<?> _findClass(AndroidJarLoader jarLoader, String name) throws Exception {
        Method findClassMethod = AndroidJarLoader.class.getDeclaredMethod(
                "findClass", String.class);  //$NON-NLS-1$
        findClassMethod.setAccessible(true);
        try {
            return (Class<?>)findClassMethod.invoke(jarLoader, name);
        }
        catch (InvocationTargetException e) {
           throw (Exception)e.getCause();
        }
    }

    /** Trying to find a class that we fail to preload should throw a CNFE. */
    public final void testFindClass_classNotFound() throws Exception {
        try {
            // Will throw ClassNotFoundException
            _findClass(mFrameworkClassLoader, "not.a.valid.ClassName");  //$NON-NLS-1$
        } catch (ClassNotFoundException e) {
            // check the message in the CNFE
            assertEquals("not.a.valid.ClassName", e.getMessage());  //$NON-NLS-1$
            return;
        }
        // Exception not thrown - this is a failure
        fail("Expected ClassNotFoundException not thrown");
    }
    
    public final void testFindClassesDerivingFrom() throws Exception {
        HashMap<String, ArrayList<IClassDescriptor>> found =
            mFrameworkClassLoader.findClassesDerivingFrom("jar.example.", new String[] {  //$NON-NLS-1$
                "jar.example.Class1",       //$NON-NLS-1$
                "jar.example.Class2" });    //$NON-NLS-1$

        assertTrue(found.containsKey("jar.example.Class1"));  //$NON-NLS-1$
        assertTrue(found.containsKey("jar.example.Class2"));  //$NON-NLS-1$
        assertEquals(2, found.size());  
        // Only Class2 derives from Class1..
        // Class1 and Class1$InnerStaticClass1 derive from Object and are thus ignored.
        // Class1$InnerClass2 should never be seen either.
        assertEquals("jar.example.Class2",  //$NON-NLS-1$
                found.get("jar.example.Class1").get(0).getCanonicalName());  //$NON-NLS-1$
        assertEquals(1, found.get("jar.example.Class1").size());      //$NON-NLS-1$
        assertEquals(0, found.get("jar.example.Class2").size());      //$NON-NLS-1$
    }

    // --- Utilities ---
    
    /**
     * Retrieves the private mFrameworkClassLoader.mClassCache field using reflection.
     * 
     * @throws NoSuchFieldException 
     * @throws SecurityException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, Class<?> > getPrivateClassCache()
            throws SecurityException, NoSuchFieldException,
                IllegalArgumentException, IllegalAccessException {
        Field field = AndroidJarLoader.class.getDeclaredField("mClassCache");  //$NON-NLS-1$
        field.setAccessible(true);
        return (HashMap<String, Class<?>>) field.get(mFrameworkClassLoader);
    }

    /**
     * Retrieves the private mFrameworkClassLoader.mEntryCache field using reflection.
     * 
     * @throws NoSuchFieldException 
     * @throws SecurityException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @SuppressWarnings("unchecked")
    private HashMap<String,byte[]> getPrivateEntryCache()
            throws SecurityException, NoSuchFieldException,
                IllegalArgumentException, IllegalAccessException {
        Field field = AndroidJarLoader.class.getDeclaredField("mEntryCache");  //$NON-NLS-1$
        field.setAccessible(true);
        return (HashMap<String, byte[]>) field.get(mFrameworkClassLoader);
    }
}
