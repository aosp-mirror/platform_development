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

package com.android.ide.eclipse.common.resources;

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.CommonPlugin;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/**
 * This is a communication between different plugins that don't know each other. It allows one
 * plugin to provide detailed information about the framework resources to another plugin.
 */
public class FrameworkResourceManager {

    private static String[] sBooleanValues = new String[] {
        "true",     //$NON-NLS-1$
        "false",    //$NON-NLS-1$
    };
    
    private static FrameworkResourceManager sThis = new FrameworkResourceManager();
    
    private ArrayList<Runnable> mResourcesChangeListeners = new ArrayList<Runnable>();
    
    private Hashtable<String, String[]> mAttributeValues;
    
    private IResourceRepository mSystemResourceRepository;

    private ViewClassInfo[] mLayoutViewsInfo;
    private ViewClassInfo[] mLayoutGroupsInfo;
    private ViewClassInfo[] mPreferencesInfo;
    private ViewClassInfo[] mPreferenceGroupsInfo;
    
    private Map<String, DeclareStyleableInfo> mXmlMenuMap;
    private Map<String, DeclareStyleableInfo> mXmlSearchableMap;
    private Map<String, DeclareStyleableInfo> mManifestMap;
    
    /** Flags indicating whether we have some resources */
    private boolean mHasResources = false;

    private String mLayoutLibLocation;

    private String mFrameworkResourcesLocation;

    private Map<String, Map<String, Integer>> mEnumValueMap;

    private String mFrameworkFontsLocation;

    private String mDocBaseUrl;
    
    /**
     * Creates a new Framework Resource Manager.
     * 
     * mAttributeValues is a map { key => list [ values ] }.
     * The key for the map is "(element-xml-name,attribute-namespace:attribute-xml-local-name)".
     * The attribute namespace prefix must be:
     * - "android" for AndroidConstants.NS_RESOURCES
     * - "xmlns" for the XMLNS URI.
     */
    private FrameworkResourceManager() {
        /* TODO Attempt to load those values from android.jar */
        mAttributeValues = new Hashtable<String, String[]>();
        mAttributeValues.put("(manifest,xmlns:android)", new String[] { //$NON-NLS-1$
                AndroidConstants.NS_RESOURCES
                });
        mAttributeValues.put("(permission,android:protectionLevel)", new String[] { //$NON-NLS-1$
                "application",                                      //$NON-NLS-1$
                "system"                                            //$NON-NLS-1$
                });
        mAttributeValues.put("(application,android:persistent)", new String[] { //$NON-NLS-1$
                "true",                                             //$NON-NLS-1$
                "false",                                            //$NON-NLS-1$
                });
        mAttributeValues.put("(activity,android:clearOnBackground)", sBooleanValues); //$NON-NLS-1$
        mAttributeValues.put("(activity,android:configChanges)", new String[] { //$NON-NLS-1$
                "fontScale",                                        //$NON-NLS-1$
                "mcc",                                              //$NON-NLS-1$
                "mnc",                                              //$NON-NLS-1$
                "locale",                                           //$NON-NLS-1$
                "touchscreen",                                      //$NON-NLS-1$
                "keyboard",                                         //$NON-NLS-1$
                "keyboardHidden",                                   //$NON-NLS-1$
                "navigation",                                       //$NON-NLS-1$
                "orientation",                                      //$NON-NLS-1$
                });
        mAttributeValues.put("(activity,android:launchMode)", new String[] { //$NON-NLS-1$
                "multiple",                                         //$NON-NLS-1$
                "singleTop",                                        //$NON-NLS-1$
                "singleTask",                                       //$NON-NLS-1$
                "singleInstance"                                    //$NON-NLS-1$
                });
        mAttributeValues.put("(activity,android:stateNotNeeded)", sBooleanValues); //$NON-NLS-1$
        mAttributeValues.put("(provider,android:syncable)", sBooleanValues); //$NON-NLS-1$
        mAttributeValues.put("(provider,android:multiprocess)", sBooleanValues); //$NON-NLS-1$
        mAttributeValues.put("(instrumentation,android:functionalTest)", sBooleanValues); //$NON-NLS-1$
        mAttributeValues.put("(instrumentation,android:handleProfiling)", sBooleanValues); //$NON-NLS-1$
        
    }

    /**
     * Returns the {@link FrameworkResourceManager} instance.
     */
    public static FrameworkResourceManager getInstance() {
        return sThis;
    }

    /**
     * Sets the resources and notifies the listeners
     * @param documentationBaseUrl 
     */
    public synchronized void setResources(IResourceRepository systemResourceRepository,
            ViewClassInfo[] layoutViewsInfo,
            ViewClassInfo[] layoutGroupsInfo,
            ViewClassInfo[] preferencesInfo,
            ViewClassInfo[] preferenceGroupsInfo,
            Map<String, DeclareStyleableInfo> xmlMenuMap,
            Map<String, DeclareStyleableInfo> xmlSearchableMap,
            Map<String, DeclareStyleableInfo> manifestMap,
            Map<String, Map<String, Integer>> enumValueMap,
            String[] permissionValues,
            String[] activityIntentActionValues,
            String[] broadcastIntentActionValues,
            String[] serviceIntentActionValues,
            String[] intentCategoryValues,
            String documentationBaseUrl) {
        mSystemResourceRepository = systemResourceRepository;
        
        mLayoutViewsInfo = layoutViewsInfo;
        mLayoutGroupsInfo = layoutGroupsInfo; 

        mPreferencesInfo = preferencesInfo;
        mPreferenceGroupsInfo = preferenceGroupsInfo;
        
        mXmlMenuMap = xmlMenuMap;
        mXmlSearchableMap = xmlSearchableMap;
        mManifestMap = manifestMap;
        mEnumValueMap = enumValueMap;
        mDocBaseUrl = documentationBaseUrl;

        setPermissions(permissionValues);
        setIntentFilterActionsAndCategories(activityIntentActionValues, broadcastIntentActionValues,
                serviceIntentActionValues, intentCategoryValues);

        mHasResources = true;

        notifyFrameworkResourcesChangeListeners();
    }
    
    public synchronized IResourceRepository getSystemResources() {
        return mSystemResourceRepository;
    }

    public synchronized String[] getValues(String elementName, String attributeName) {
        String key = String.format("(%1$s,%2$s)", elementName, attributeName); //$NON-NLS-1$
        return mAttributeValues.get(key);
    }

    public synchronized String[] getValues(String elementName, String attributeName,
            String greatGrandParentElementName) {
        if (greatGrandParentElementName != null) {
            String key = String.format("(%1$s,%2$s,%3$s)", greatGrandParentElementName, //$NON-NLS-1$
                    elementName, attributeName); 
            String[] values = mAttributeValues.get(key);
            if (values != null) {
                return values;
            }
        }
        
        return getValues(elementName, attributeName);
    }

    public synchronized String[] getValues(String key) {
        return mAttributeValues.get(key);
    }
    
    public synchronized ViewClassInfo[] getLayoutViewsInfo() {
        return mLayoutViewsInfo;
    }

    public synchronized ViewClassInfo[] getLayoutGroupsInfo() {
        return mLayoutGroupsInfo;
    }

    public synchronized ViewClassInfo[] getPreferencesInfo() {
        return mPreferencesInfo;
    }

    public synchronized ViewClassInfo[] getPreferenceGroupsInfo() {
        return mPreferenceGroupsInfo;
    }
    
    public synchronized Map<String, DeclareStyleableInfo> getXmlMenuDefinitions() {
        return mXmlMenuMap;
    }

    public synchronized Map<String, DeclareStyleableInfo> getXmlSearchableDefinitions() {
        return mXmlSearchableMap;
    }

    public synchronized Map<String, DeclareStyleableInfo> getManifestDefinitions() {
        return mManifestMap;
    }

    public String getDocumentationBaseUrl() {
        return mDocBaseUrl == null ? AndroidConstants.CODESITE_BASE_URL : mDocBaseUrl;
    }
    
    /**
     * Sets the permission values
     * @param permissionValues the list of permissions
     */
    private void setPermissions(String[] permissionValues) {
        setValues("(uses-permission,android:name)", permissionValues); //$NON-NLS-1$
        setValues("(application,android:permission)", permissionValues); //$NON-NLS-1$
        setValues("(activity,android:permission)", permissionValues); //$NON-NLS-1$
        setValues("(receiver,android:permission)", permissionValues); //$NON-NLS-1$
        setValues("(service,android:permission)", permissionValues); //$NON-NLS-1$
        setValues("(provider,android:permission)", permissionValues); //$NON-NLS-1$
    }
    
    private void setIntentFilterActionsAndCategories(String[] activityIntentActions,
            String[] broadcastIntentActions, String[] serviceIntentActions,
            String[] intentCategoryValues) {
        setValues("(activity,action,android:name)", activityIntentActions); //$NON-NLS-1$
        setValues("(receiver,action,android:name)", broadcastIntentActions); //$NON-NLS-1$
        setValues("(service,action,android:name)", serviceIntentActions); //$NON-NLS-1$
        setValues("(category,android:name)", intentCategoryValues); //$NON-NLS-1$
    }

    /**
     * Sets a (name, values) pair in the hash map.
     * <p/>
     * If the name is already present in the map, it is first removed.
     * @param name the name associated with the values.
     * @param values The values to add.
     */
    private void setValues(String name, String[] values) {
        mAttributeValues.remove(name);
        mAttributeValues.put(name, values);
    }


    /**
     * Called by the ADT plugin when the SDK path has changed.
     * This stores the path locally and then notifies all attached listeners.
     */
    private void notifyFrameworkResourcesChangeListeners() {
        for (Runnable listener : mResourcesChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                CommonPlugin.log(e, "IPathChangedListener failed."); //$NON-NLS-1$
            }
        }
    }

    /** Adds a new listener that listens to framework resources changes.
     * <p/>If resources have already been set, then the listener is automatically notified. */
    public synchronized void addFrameworkResourcesChangeListener(Runnable listener) {
        if (listener != null && mResourcesChangeListeners.indexOf(listener) == -1) {
            mResourcesChangeListeners.add(listener);
            
            if (mHasResources) {
                listener.run();
            }
        }
    }

    /** Removes a framework resources changes listener.
     * <p/>Safe to call with null or with the same value. */
    public synchronized void removeFrameworkResourcesChangeListener(Runnable listener) {
        mResourcesChangeListeners.remove(listener);
    }

    public void setLayoutLibLocation(String osLocation) {
        mLayoutLibLocation = osLocation;
    }
    
    public String getLayoutLibLocation() {
        return mLayoutLibLocation;
    }
    
    public void setFrameworkResourcesLocation(String osLocation) {
        mFrameworkResourcesLocation = osLocation;
    }

    public String getFrameworkResourcesLocation() {
        return mFrameworkResourcesLocation;
    }
    
    public Map<String, Map<String, Integer>> getEnumValueMap() {
        return mEnumValueMap;
    }

    public void setFrameworkFontLocation(String osLocation) {
        mFrameworkFontsLocation = osLocation;
    }
    
    public String getFrameworkFontLocation() {
        return mFrameworkFontsLocation;
    }
}
