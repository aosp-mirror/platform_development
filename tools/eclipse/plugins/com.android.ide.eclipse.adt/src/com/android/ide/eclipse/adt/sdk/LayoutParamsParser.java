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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.sdk.IAndroidClassLoader.IClassDescriptor;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.resources.AttrsXmlParser;
import com.android.ide.eclipse.common.resources.ViewClassInfo;
import com.android.ide.eclipse.common.resources.ViewClassInfo.LayoutParamsInfo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.InvalidAttributeValueException;

/*
 * TODO: refactor this. Could use some cleanup.
 */

/**
 * Parser for the framework library.
 * <p/>
 * This gather the following information:
 * <ul>
 * <li>Resource ID from <code>android.R</code></li>
 * <li>The list of permissions values from <code>android.Manifest$permission</code></li>
 * <li></li>
 * </ul> 
 */
public class LayoutParamsParser {
    
    /**
     * Class extending {@link ViewClassInfo} by adding the notion of instantiability.
     * {@link LayoutParamsParser#getViews()} and {@link LayoutParamsParser#getGroups()} should
     * only return classes that can be instantiated.
     */
    final static class ExtViewClassInfo extends ViewClassInfo {

        private boolean mIsInstantiable;

        ExtViewClassInfo(boolean instantiable, boolean isLayout, String canonicalClassName,
                String shortClassName) {
            super(isLayout, canonicalClassName, shortClassName);
            mIsInstantiable = instantiable;
        }
        
        boolean isInstantiable() {
            return mIsInstantiable;
        }
    }
    
    /* Note: protected members/methods are overridden in unit tests */
    
    /** Reference to android.view.View */
    protected IClassDescriptor mTopViewClass;
    /** Reference to android.view.ViewGroup */
    protected IClassDescriptor mTopGroupClass;
    /** Reference to android.view.ViewGroup$LayoutParams */
    protected IClassDescriptor mTopLayoutParamsClass;
    
    /** Input list of all classes deriving from android.view.View */
    protected ArrayList<IClassDescriptor> mViewList;
    /** Input list of all classes deriving from android.view.ViewGroup */
    protected ArrayList<IClassDescriptor> mGroupList;
    
    /** Output map of FQCN => info on View classes */
    protected TreeMap<String, ExtViewClassInfo> mViewMap;
    /** Output map of FQCN => info on ViewGroup classes */
    protected TreeMap<String, ExtViewClassInfo> mGroupMap;
    /** Output map of FQCN => info on LayoutParams classes */
    protected HashMap<String, LayoutParamsInfo> mLayoutParamsMap;
    
    /** The attrs.xml parser */
    protected AttrsXmlParser mAttrsXmlParser;

    /** The android.jar class loader */
    protected IAndroidClassLoader mClassLoader;

    /**
     * Instantiate a new LayoutParamsParser.
     * @param classLoader The android.jar class loader
     * @param attrsXmlParser The parser of the attrs.xml file
     */
    public LayoutParamsParser(IAndroidClassLoader classLoader,
            AttrsXmlParser attrsXmlParser) {
        mClassLoader = classLoader;
        mAttrsXmlParser = attrsXmlParser;
    }
    
    /** Returns the map of FQCN => info on View classes */
    public List<ViewClassInfo> getViews() {
        return getInstantiables(mViewMap);
    }

    /** Returns the map of FQCN => info on ViewGroup classes */
    public List<ViewClassInfo> getGroups() {
        return getInstantiables(mGroupMap);
    }
    
    /**
     * TODO: doc here.
     * <p/>
     * Note: on output we should have NO dependency on {@link IClassDescriptor},
     * otherwise we wouldn't be able to unload the class loader later.
     * <p/>
     * Note on Vocabulary: FQCN=Fully Qualified Class Name (e.g. "my.package.class$innerClass")
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    public void parseLayoutClasses(IProgressMonitor monitor) {
        parseClasses(monitor,
                AndroidConstants.CLASS_VIEW,
                AndroidConstants.CLASS_VIEWGROUP,
                AndroidConstants.CLASS_VIEWGROUP_LAYOUTPARAMS);
    }

    public void parsePreferencesClasses(IProgressMonitor monitor) {
        parseClasses(monitor,
                AndroidConstants.CLASS_PREFERENCE,
                AndroidConstants.CLASS_PREFERENCEGROUP,
                null /* paramsClassName */ );
    }
    
    private void parseClasses(IProgressMonitor monitor,
            String rootClassName,
            String groupClassName,
            String paramsClassName) {
        try {
            SubMonitor progress = SubMonitor.convert(monitor, 100);

            String[] superClasses = new String[2 + (paramsClassName == null ? 0 : 1)];
            superClasses[0] = groupClassName;
            superClasses[1] = rootClassName;
            if (paramsClassName != null) {
                superClasses[2] = paramsClassName;
            }
            HashMap<String, ArrayList<IClassDescriptor>> found =
                    mClassLoader.findClassesDerivingFrom("android.", superClasses);  //$NON-NLS-1$
            mTopViewClass = mClassLoader.getClass(rootClassName);
            mTopGroupClass = mClassLoader.getClass(groupClassName);
            if (paramsClassName != null) {
                mTopLayoutParamsClass = mClassLoader.getClass(paramsClassName);
            }

            mViewList = found.get(rootClassName);
            mGroupList = found.get(groupClassName);

            mViewMap = new TreeMap<String, ExtViewClassInfo>();
            mGroupMap = new TreeMap<String, ExtViewClassInfo>();
            if (mTopLayoutParamsClass != null) {
                mLayoutParamsMap = new HashMap<String, LayoutParamsInfo>();
            }
            
            // Add top classes to the maps since by design they are not listed in classes deriving
            // from themselves.
            addGroup(mTopGroupClass);
            addView(mTopViewClass);

            // ViewGroup derives from View
            mGroupMap.get(groupClassName).setSuperClass(mViewMap.get(rootClassName));

            progress.setWorkRemaining(mGroupList.size() + mViewList.size());
            
            for (IClassDescriptor groupChild : mGroupList) {
                addGroup(groupChild);
                progress.worked(1);
            }

            for (IClassDescriptor viewChild : mViewList) {
                if (viewChild != mTopGroupClass) {
                    addView(viewChild);
                }
                progress.worked(1);
            }
        } catch (ClassNotFoundException e) {
            AdtPlugin.log(e, "Problem loading class %1$s or %2$s",  //$NON-NLS-1$
                    rootClassName, groupClassName);
        } catch (InvalidAttributeValueException e) {
            AdtPlugin.log(e, "Problem loading classes"); //$NON-NLS-1$
        } catch (ClassFormatError e) {
            AdtPlugin.log(e, "Problem loading classes"); //$NON-NLS-1$
        } catch (IOException e) {
            AdtPlugin.log(e, "Problem loading classes"); //$NON-NLS-1$
        }
    }

    /**
     * Parses a View class and adds a ExtViewClassInfo for it in mViewMap.
     * It calls itself recursively to handle super classes which are also Views.
     */
    private ExtViewClassInfo addView(IClassDescriptor viewClass) {
        String fqcn = viewClass.getCanonicalName();
        if (mViewMap.containsKey(fqcn)) {
            return mViewMap.get(fqcn);
        } else if (mGroupMap.containsKey(fqcn)) {
            return mGroupMap.get(fqcn);
        }

        ExtViewClassInfo info = new ExtViewClassInfo(viewClass.isInstantiable(),
                false /* layout */, fqcn, viewClass.getSimpleName());
        mViewMap.put(fqcn, info);

        // All view classes derive from mTopViewClass by design.
        // Do not lookup the super class for mTopViewClass itself.
        if (viewClass.equals(mTopViewClass) == false) {
            IClassDescriptor superClass = viewClass.getSuperclass(); 
            ExtViewClassInfo superClassInfo = addView(superClass);
            info.setSuperClass(superClassInfo);
        }

        mAttrsXmlParser.loadViewAttributes(info);
        return info;
    }

    /**
     * Parses a ViewGroup class and adds a ExtViewClassInfo for it in mGroupMap.
     * It calls itself recursively to handle super classes which are also ViewGroups.
     */
    private ExtViewClassInfo addGroup(IClassDescriptor groupClass) {
        String fqcn = groupClass.getCanonicalName();
        if (mGroupMap.containsKey(fqcn)) {
            return mGroupMap.get(fqcn);
        }

        ExtViewClassInfo info = new ExtViewClassInfo(groupClass.isInstantiable(),
                true /* layout */, fqcn, groupClass.getSimpleName());
        mGroupMap.put(fqcn, info);

        // All groups derive from android.view.ViewGroup, which in turns derives from
        // android.view.View (i.e. mTopViewClass here). So the only group that can have View as
        // its super class is the ViewGroup base class and we don't try to resolve it since groups
        // are loaded before views.
        IClassDescriptor superClass = groupClass.getSuperclass(); 
        
        // Assertion: at this point, we should have
        //   superClass != mTopViewClass || fqcn.equals(AndroidConstants.CLASS_VIEWGROUP);

        if (superClass != null && superClass.equals(mTopViewClass) == false) {
            ExtViewClassInfo superClassInfo = addGroup(superClass);
            
            // Assertion: we should have superClassInfo != null && superClassInfo != info;
            if (superClassInfo != null && superClassInfo != info) {
                info.setSuperClass(superClassInfo);
            }
        }

        mAttrsXmlParser.loadViewAttributes(info);
        if (mTopLayoutParamsClass != null) {
            info.setLayoutParams(addLayoutParams(groupClass));
        }
        return info;
    }
    
    /**
     * Parses a ViewGroup class and returns an info object on its inner LayoutParams.
     * 
     * @return The {@link LayoutParamsInfo} for the ViewGroup class or null.
     */
    private LayoutParamsInfo addLayoutParams(IClassDescriptor groupClass) {

        // Is there a LayoutParams in this group class?
        IClassDescriptor layoutParamsClass = findLayoutParams(groupClass);

        // if there's no layout data in the group class, link to the one from the
        // super class.
        if (layoutParamsClass == null) {
            for (IClassDescriptor superClass = groupClass.getSuperclass();
                    layoutParamsClass == null &&
                        superClass != null &&
                        superClass.equals(mTopViewClass) == false;
                    superClass = superClass.getSuperclass()) {
                layoutParamsClass = findLayoutParams(superClass);
            }
        }

        if (layoutParamsClass != null) {
            return getLayoutParamsInfo(layoutParamsClass);
        }
        
        return null;
    }

    /**
     * Parses a LayoutParams class and returns a LayoutParamsInfo object for it.
     * It calls itself recursively to handle the super class of the LayoutParams.
     */
    private LayoutParamsInfo getLayoutParamsInfo(IClassDescriptor layoutParamsClass) {
        String fqcn = layoutParamsClass.getCanonicalName();
        LayoutParamsInfo layoutParamsInfo = mLayoutParamsMap.get(fqcn);

        if (layoutParamsInfo != null) {
            return layoutParamsInfo;
        }
        
        // Find the link on the LayoutParams super class 
        LayoutParamsInfo superClassInfo = null;
        if (layoutParamsClass.equals(mTopLayoutParamsClass) == false) {
            IClassDescriptor superClass = layoutParamsClass.getSuperclass(); 
            superClassInfo = getLayoutParamsInfo(superClass);
        }
        
        // Find the link on the enclosing ViewGroup
        ExtViewClassInfo enclosingGroupInfo = addGroup(layoutParamsClass.getEnclosingClass());

        layoutParamsInfo = new ExtViewClassInfo.LayoutParamsInfo(
                enclosingGroupInfo, layoutParamsClass.getSimpleName(), superClassInfo);
        mLayoutParamsMap.put(fqcn, layoutParamsInfo);

        mAttrsXmlParser.loadLayoutParamsAttributes(layoutParamsInfo);

        return layoutParamsInfo;
    }

    /**
     * Given a ViewGroup-derived class, looks for an inner class named LayoutParams
     * and if found returns its class definition.
     * <p/>
     * This uses the actual defined inner classes and does not look at inherited classes.
     *  
     * @param groupClass The ViewGroup derived class
     * @return The Class of the inner LayoutParams or null if none is declared.
     */
    private IClassDescriptor findLayoutParams(IClassDescriptor groupClass) {
        IClassDescriptor[] innerClasses = groupClass.getDeclaredClasses();
        for (IClassDescriptor innerClass : innerClasses) {
            if (innerClass.getSimpleName().equals(AndroidConstants.CLASS_NAME_LAYOUTPARAMS)) {
                return innerClass;
            }
        }
        return null;
    }
    
    /**
     * Computes and return a list of ViewClassInfo from a map by filtering out the class that
     * cannot be instantiated.
     */
    private List<ViewClassInfo> getInstantiables(SortedMap<String, ExtViewClassInfo> map) {
        Collection<ExtViewClassInfo> values = map.values();
        ArrayList<ViewClassInfo> list = new ArrayList<ViewClassInfo>();
        
        for (ExtViewClassInfo info : values) {
            if (info.isInstantiable()) {
                list.add(info);
            }
        }
        
        return list;
    }
}
