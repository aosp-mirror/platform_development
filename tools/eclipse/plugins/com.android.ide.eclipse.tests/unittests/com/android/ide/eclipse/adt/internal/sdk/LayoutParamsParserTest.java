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

package com.android.ide.eclipse.adt.internal.sdk;

import com.android.ide.eclipse.adt.internal.resources.AttrsXmlParser;
import com.android.ide.eclipse.adt.internal.resources.ViewClassInfo;
import com.android.ide.eclipse.adt.internal.resources.ViewClassInfo.LayoutParamsInfo;
import com.android.ide.eclipse.adt.internal.sdk.AndroidJarLoader.ClassWrapper;
import com.android.ide.eclipse.adt.internal.sdk.IAndroidClassLoader.IClassDescriptor;
import com.android.ide.eclipse.tests.AdtTestData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import junit.framework.TestCase;

/**
 * Test the inner private methods of PlatformDataParser.
 *
 * Convention: method names that start with an underscore are actually local wrappers
 * that call private methods from {@link AndroidTargetParser} using reflection.
 * This is inspired by the Python coding rule which mandates underscores prefixes for
 * "private" methods.
 */
public class LayoutParamsParserTest extends TestCase {

    private static final String MOCK_DATA_PATH =
        "com/android/ide/eclipse/testdata/mock_attrs.xml"; //$NON-NLS-1$

    private static class MockFrameworkClassLoader extends AndroidJarLoader {
        MockFrameworkClassLoader() {
            super(null /* osFrameworkLocation */);
        }

        @Override
        public HashMap<String, ArrayList<IClassDescriptor>> findClassesDerivingFrom(
                String rootPackage, String[] superClasses) throws ClassFormatError {
            return new HashMap<String, ArrayList<IClassDescriptor>>();
        }
    }

    private static class MockLayoutParamsParser extends LayoutParamsParser {
        public MockLayoutParamsParser() {
            super(new MockFrameworkClassLoader(),
                  new AttrsXmlParser(
                          AdtTestData.getInstance().getTestFilePath(MOCK_DATA_PATH)).preload());

            mTopViewClass = new ClassWrapper(mock_android.view.View.class);
            mTopGroupClass = new ClassWrapper(mock_android.view.ViewGroup.class);
            mTopLayoutParamsClass = new ClassWrapper(mock_android.view.ViewGroup.LayoutParams.class);

            mViewList = new ArrayList<IClassDescriptor>();
            mGroupList = new ArrayList<IClassDescriptor>();
            mViewMap = new TreeMap<String, ExtViewClassInfo>();
            mGroupMap = new TreeMap<String, ExtViewClassInfo>();
            mLayoutParamsMap = new HashMap<String, LayoutParamsInfo>();
        }
    }

    private MockLayoutParamsParser mParser;

    @Override
    public void setUp() throws Exception {
        mParser = new MockLayoutParamsParser();
    }

    @Override
    public void tearDown() throws Exception {
    }

    public final void testFindLayoutParams() throws Exception {
        assertEquals(mock_android.view.ViewGroup.LayoutParams.class,
            ((ClassWrapper)_findLayoutParams(mock_android.view.ViewGroup.class)).wrappedClass());

        assertEquals(mock_android.widget.LinearLayout.LayoutParams.class,
            ((ClassWrapper)_findLayoutParams(mock_android.widget.LinearLayout.class)).wrappedClass());

        assertEquals(mock_android.widget.TableLayout.LayoutParams.class,
            ((ClassWrapper)_findLayoutParams(mock_android.widget.TableLayout.class)).wrappedClass());
    }

    public final void testGetLayoutParamsInfo() throws Exception {
        LayoutParamsInfo info1 = _getLayoutParamsInfo(
                mock_android.view.ViewGroup.LayoutParams.class);
        assertNotNull(info1);
        // ViewGroup.LayoutData has Object for superClass, which we don't map
        assertNull(info1.getSuperClass());

        LayoutParamsInfo info2 = _getLayoutParamsInfo(
                mock_android.widget.LinearLayout.LayoutParams.class);
        assertNotNull(info2);
        // LinearLayout.LayoutData links to ViewGroup.LayoutParams
        assertSame(info1, info2.getSuperClass());

        LayoutParamsInfo info3 = _getLayoutParamsInfo(
                mock_android.widget.TableLayout.LayoutParams.class);
        assertNotNull(info3);
        // TableLayout.LayoutData does not link to ViewGroup.LayoutParams nor
        // LinearLayout.LayoutParams
        assertNotSame(info1, info3.getSuperClass());
        assertNotSame(info2, info3.getSuperClass());
        // TableLayout.LayoutParams => ViewGroup.MarginLayoutParams => ViewGroup.LayoutParams
        assertSame(info1, info3.getSuperClass().getSuperClass());
    }

    public final void testGetLayoutClasses() throws Exception {
        // _getLayoutClasses();
    }

    //---- access to private methods

    /** Calls the private constructor of the parser */
    @SuppressWarnings("unused")
    private AndroidTargetParser _Constructor(String osJarPath) throws Exception {
        Constructor<AndroidTargetParser> constructor =
            AndroidTargetParser.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(osJarPath);
    }

    /** calls the private getLayoutClasses() of the parser */
    @SuppressWarnings("unused")
    private void _getLayoutClasses() throws Exception {
        Method method = AndroidTargetParser.class.getDeclaredMethod("getLayoutClasses");  //$NON-NLS-1$
        method.setAccessible(true);
        method.invoke(mParser);
    }

    /** calls the private addGroup() of the parser */
    @SuppressWarnings("unused")
    private ViewClassInfo _addGroup(Class<?> groupClass) throws Exception {
        Method method = LayoutParamsParser.class.getDeclaredMethod("addGroup",  //$NON-NLS-1$
                IClassDescriptor.class);
        method.setAccessible(true);
        return (ViewClassInfo) method.invoke(mParser, new ClassWrapper(groupClass));
    }

    /** calls the private addLayoutParams() of the parser */
    @SuppressWarnings("unused")
    private LayoutParamsInfo _addLayoutParams(Class<?> groupClass) throws Exception {
        Method method = LayoutParamsParser.class.getDeclaredMethod("addLayoutParams",   //$NON-NLS-1$
                IClassDescriptor.class);
        method.setAccessible(true);
        return (LayoutParamsInfo) method.invoke(mParser, new ClassWrapper(groupClass));
    }

    /** calls the private getLayoutParamsInfo() of the parser */
    private LayoutParamsInfo _getLayoutParamsInfo(Class<?> layoutParamsClass) throws Exception {
        Method method = LayoutParamsParser.class.getDeclaredMethod("getLayoutParamsInfo",   //$NON-NLS-1$
                IClassDescriptor.class);
        method.setAccessible(true);
        return (LayoutParamsInfo) method.invoke(mParser, new ClassWrapper(layoutParamsClass));
    }

    /** calls the private findLayoutParams() of the parser */
    private IClassDescriptor _findLayoutParams(Class<?> groupClass) throws Exception {
        Method method = LayoutParamsParser.class.getDeclaredMethod("findLayoutParams",  //$NON-NLS-1$
                IClassDescriptor.class);
        method.setAccessible(true);
        return (IClassDescriptor) method.invoke(mParser, new ClassWrapper(groupClass));
    }

}
