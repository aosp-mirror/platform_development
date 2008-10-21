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

package com.android.ide.eclipse.adt.resources;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.resources.AttrsXmlParser;
import com.android.ide.eclipse.common.resources.DeclareStyleableInfo;
import com.android.ide.eclipse.common.resources.FrameworkResourceManager;
import com.android.ide.eclipse.common.resources.ResourceItem;
import com.android.ide.eclipse.common.resources.ResourceType;
import com.android.ide.eclipse.common.resources.ViewClassInfo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InvalidAttributeValueException;

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
public final class FrameworkResourceParser {
    
    private static final String TAG = "Framework Resource Parser";

    /**
     * Creates a framework resource parser.
     */
    public FrameworkResourceParser() {
    }
    
    /**
     * Parses the framework, collects all interesting information and stores them in the
     * {@link FrameworkResourceManager} given to the constructor.
     * 
     * @param osSdkPath the OS path of the SDK directory.
     * @param resourceManager the {@link FrameworkResourceManager} that will store the parsed
     * resources.
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     * @return True if the SDK path was valid and parsing has been attempted.
     */
    public boolean parse(String osSdkPath, FrameworkResourceManager resourceManager,
            IProgressMonitor monitor) {    
        if (osSdkPath == null || osSdkPath.length() == 0) {
            return false;
        }

        try {
            SubMonitor progress = SubMonitor.convert(monitor, 100);
            
            AndroidJarLoader classLoader =
                new AndroidJarLoader(osSdkPath + AndroidConstants.FN_FRAMEWORK_LIBRARY);
            
            progress.subTask("Preloading");
            preload(classLoader, progress.newChild(40));
            progress.setWorkRemaining(60);
            
            if (progress.isCanceled()) {
                return false;
            }
            
            // get the resource Ids.
            progress.subTask("Resource IDs");
            FrameworkResourceRepository systemResourceRepository = new FrameworkResourceRepository(
                    collectResourceIds(classLoader));
            progress.worked(5);

            if (progress.isCanceled()) {
                return false;
            }

            // get the permissions
            progress.subTask("Permissions");
            String[] permissionValues = collectPermissions(classLoader);
            progress.worked(5);

            if (progress.isCanceled()) {
                return false;
            }

            String osLibPath = osSdkPath + AndroidConstants.OS_SDK_LIBS_FOLDER;

            // get the action and category values for the Intents.
            progress.subTask("Intents");
            ArrayList<String> activity_actions = new ArrayList<String>();
            ArrayList<String> broadcast_actions = new ArrayList<String>();
            ArrayList<String> service_actions = new ArrayList<String>();
            ArrayList<String> categories = new ArrayList<String>();
            collectIntentFilterActionsAndCategories(osLibPath,
                    activity_actions, broadcast_actions, service_actions, categories);
            progress.worked(5);

            if (progress.isCanceled()) {
                return false;
            }

            progress.subTask("Layouts");
            AttrsXmlParser attrsXmlParser = new AttrsXmlParser(
                    osSdkPath + AndroidConstants.OS_SDK_ATTRS_XML);
            attrsXmlParser.preload();

            AttrsXmlParser attrsManifestXmlParser = new AttrsXmlParser(
                    osSdkPath + AndroidConstants.OS_SDK_ATTRS_MANIFEST_XML,
                    attrsXmlParser);
            attrsManifestXmlParser.preload();

            Collection<ViewClassInfo> mainList = new ArrayList<ViewClassInfo>();
            Collection<ViewClassInfo> groupList = new ArrayList<ViewClassInfo>();

            collectLayoutClasses(osLibPath, classLoader, attrsXmlParser, mainList, groupList,
                    progress.newChild(40));
            
            if (progress.isCanceled()) {
                return false;
            }

            ViewClassInfo[] layoutViewsInfo = mainList.toArray(new ViewClassInfo[mainList.size()]);
            ViewClassInfo[] layoutGroupsInfo = groupList.toArray(
                    new ViewClassInfo[groupList.size()]);
            
            mainList.clear();
            groupList.clear();
            collectPreferenceClasses(classLoader, attrsXmlParser, mainList, groupList,
                    progress.newChild(5));

            if (progress.isCanceled()) {
                return false;
            }

            ViewClassInfo[] preferencesInfo = mainList.toArray(new ViewClassInfo[mainList.size()]);
            ViewClassInfo[] preferenceGroupsInfo = groupList.toArray(
                    new ViewClassInfo[groupList.size()]);

            Map<String, DeclareStyleableInfo> xmlMenuMap = collectMenuDefinitions(attrsXmlParser);
            Map<String, DeclareStyleableInfo> xmlSearchableMap = collectSearchableDefinitions(
                    attrsXmlParser);
            Map<String, DeclareStyleableInfo> manifestMap = collectManifestDefinitions(
                                                                            attrsManifestXmlParser);
            Map<String, Map<String, Integer>> enumValueMap = attrsXmlParser.getEnumFlagValues();

            if (progress.isCanceled()) {
                return false;
            }
            
            String docBaseUrl = getDocumentationBaseUrl(
                    osSdkPath + AndroidConstants.OS_SDK_DOCS_FOLDER);

            FrameworkResourceManager.getInstance().setResources(systemResourceRepository,
                    layoutViewsInfo,
                    layoutGroupsInfo,
                    preferencesInfo,
                    preferenceGroupsInfo,
                    xmlMenuMap,
                    xmlSearchableMap,
                    manifestMap,
                    enumValueMap,
                    permissionValues,
                    activity_actions.toArray(new String[activity_actions.size()]),
                    broadcast_actions.toArray(new String[broadcast_actions.size()]),
                    service_actions.toArray(new String[service_actions.size()]),
                    categories.toArray(new String[categories.size()]),
                    docBaseUrl);

            return true;
        } catch (Exception e) {
            AdtPlugin.logAndPrintError(e, TAG, "SDK parser failed"); //$NON-NLS-1$
        }
        
        return false;
    }

    /**
     * Preloads all "interesting" classes from the framework SDK jar.
     * <p/>
     * Currently this preloads all classes from the framework jar
     * 
     * @param classLoader The framework SDK jar classloader
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    private void preload(AndroidJarLoader classLoader, IProgressMonitor monitor) {
        try {
            classLoader.preLoadClasses("" /* all classes */, monitor);  //$NON-NLS-1$
        } catch (InvalidAttributeValueException e) {
            AdtPlugin.log(e, "Problem preloading classes"); //$NON-NLS-1$
        } catch (IOException e) {
            AdtPlugin.log(e, "Problem preloading classes"); //$NON-NLS-1$
        }
    }

    /**
     * Collects the resources IDs found in the SDK.
     * 
     * @param classLoader The framework SDK jar classloader
     * @return a map of the resources, or null if it failed.
     */
    private Map<ResourceType, List<ResourceItem>> collectResourceIds(
            AndroidJarLoader classLoader) {
        try {
            Class<?> r = classLoader.loadClass(AndroidConstants.CLASS_R);
            
            if (r != null) {
                return parseRClass(r);
            }
        } catch (ClassNotFoundException e) {
            AdtPlugin.logAndPrintError(e, TAG,
                    "Collect resource IDs failed, class %1$s not found in %2$s", //$NON-NLS-1$
                    AndroidConstants.CLASS_R, 
                    classLoader.getSource());
        }
        
        return null;
    }
    
    /**
     * Parse the R class and build the resource map.
     * 
     * @param rClass the Class object representing the Resources.
     * @return a map of the resource or null
     */
    private Map<ResourceType, List<ResourceItem>> parseRClass(Class<?> rClass) {
        // get the sub classes.
        Class<?>[] classes = rClass.getClasses();
        
        if (classes.length > 0) {
            HashMap<ResourceType, List<ResourceItem>> map =
                new HashMap<ResourceType, List<ResourceItem>>();

            // get the fields of each class.
            for (int c = 0 ; c < classes.length ; c++) {
                Class<?> subClass = classes[c];
                String name = subClass.getSimpleName();
                
                // get the matching ResourceType
                ResourceType type = ResourceType.getEnum(name);
                if (type != null) {
                    List<ResourceItem> list = new ArrayList<ResourceItem>();
                    map.put(type, list);
                    
                    Field[] fields = subClass.getFields();
                    
                    for (Field f : fields) {
                        list.add(new ResourceItem(f.getName()));
                    }
                }
            }
            
            return map;
        }
        
        return null;
    }

    /**
     * Loads, collects and returns the list of default permissions from the framework.
     * 
     * @param classLoader The framework SDK jar classloader
     * @return a non null (but possibly empty) array containing the permission values.
     */
    private String[] collectPermissions(AndroidJarLoader classLoader) {
        try {
            Class<?> permissionClass =
                classLoader.loadClass(AndroidConstants.CLASS_MANIFEST_PERMISSION);
            
            if (permissionClass != null) {
                ArrayList<String> list = new ArrayList<String>();

                Field[] fields = permissionClass.getFields();
                
                for (Field f : fields) {
                    int modifiers = f.getModifiers();
                    if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) &&
                            Modifier.isPublic(modifiers)) {
                        try {
                            Object value = f.get(null);
                            if (value instanceof String) {
                                list.add((String)value);
                            }
                        } catch (IllegalArgumentException e) {
                            // since we provide null this should not happen
                        } catch (IllegalAccessException e) {
                            // if the field is inaccessible we ignore it.
                        } catch (NullPointerException npe) {
                            // looks like this is not a static field. we can ignore.
                        } catch (ExceptionInInitializerError  eiie) {
                            // lets just ignore the field again
                        }
                    }
                }
                
                return list.toArray(new String[list.size()]);
            }
        } catch (ClassNotFoundException e) {
            AdtPlugin.logAndPrintError(e, TAG,
                    "Collect permissions failed, class %1$s not found in %2$s", //$NON-NLS-1$
                    AndroidConstants.CLASS_MANIFEST_PERMISSION, 
                    classLoader.getSource());
        }
        
        return new String[0];
    }
    
    /**
     * Loads and collects the action and category default values from the framework.
     * The values are added to the <code>actions</code> and <code>categories</code> lists.
     * 
     * @param osLibPath The OS path to the SDK tools/lib folder, ending with a separator.
     * @param activityActions the list which will receive the activity action values.
     * @param broadcastActions the list which will receive the broadcast action values.
     * @param serviceActions the list which will receive the service action values.
     * @param categories the list which will receive the category values.
     */
    private void collectIntentFilterActionsAndCategories(String osLibPath,
            ArrayList<String> activityActions, ArrayList<String> broadcastActions,
            ArrayList<String> serviceActions, ArrayList<String> categories)  {
        collectValues(osLibPath + "activity_actions.txt" , activityActions);
        collectValues(osLibPath + "broadcast_actions.txt" , broadcastActions);
        collectValues(osLibPath + "service_actions.txt" , serviceActions);
        collectValues(osLibPath + "categories.txt" , categories);
    }

    /**
     * Collects values from a text file located in the SDK
     * @param osFilePath The path to the text file.
     * @param values the {@link ArrayList} to fill with the values.
     */
    private void collectValues(String osFilePath, ArrayList<String> values) {
        FileReader fr = null;
        BufferedReader reader = null;
        try {
            fr = new FileReader(osFilePath);
            reader = new BufferedReader(fr);

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.startsWith("#") == false) { //$NON-NLS-1$
                    values.add(line);
                }
            }
        } catch (IOException e) {
            AdtPlugin.log(e, "Failed to read SDK values"); //$NON-NLS-1$
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                AdtPlugin.log(e, "Failed to read SDK values"); //$NON-NLS-1$
            }

            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException e) {
                AdtPlugin.log(e, "Failed to read SDK values"); //$NON-NLS-1$
            }
        }
    }

    /**
     * Collects all layout classes information from the class loader and the
     * attrs.xml and sets the corresponding structures in the resource manager.
     * 
     * @param osLibPath The OS path to the SDK tools/lib folder, ending with a separator.
     * @param classLoader The framework SDK jar classloader
     * @param attrsXmlParser The parser of the attrs.xml file
     * @param mainList the Collection to receive the main list of {@link ViewClassInfo}.
     * @param groupList the Collection to receive the group list of {@link ViewClassInfo}.
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    private void collectLayoutClasses(String osLibPath,
            AndroidJarLoader classLoader,
            AttrsXmlParser attrsXmlParser,
            Collection<ViewClassInfo> mainList, Collection<ViewClassInfo> groupList, 
            IProgressMonitor monitor) {
        LayoutParamsParser ldp = null;
        try {
            WidgetListLoader loader = new WidgetListLoader(osLibPath + "widgets.txt");
            if (loader.parseWidgetList(monitor)) {
                ldp = new LayoutParamsParser(loader, attrsXmlParser);
            }
            // if the parsing failed, we'll use the old loader below.
        } catch (FileNotFoundException e) {
            AdtPlugin.log(e, "Android Framework Parser"); //$NON-NLS-1$
            // the file does not exist, we'll use the old loader below.
        }

        if (ldp == null) {
            ldp = new LayoutParamsParser(classLoader, attrsXmlParser);
        }
        ldp.parseLayoutClasses(monitor);
        
        List<ViewClassInfo> views = ldp.getViews();
        List<ViewClassInfo> groups = ldp.getGroups();

        if (views != null && groups != null) {
            mainList.addAll(views);
            groupList.addAll(groups);
        }
    }

    /**
     * Collects all preferences definition information from the attrs.xml and
     * sets the corresponding structures in the resource manager.
     * 
     * @param classLoader The framework SDK jar classloader
     * @param attrsXmlParser The parser of the attrs.xml file
     * @param mainList the Collection to receive the main list of {@link ViewClassInfo}.
     * @param groupList the Collection to receive the group list of {@link ViewClassInfo}.
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    private void collectPreferenceClasses(AndroidJarLoader classLoader,
            AttrsXmlParser attrsXmlParser, Collection<ViewClassInfo> mainList,
            Collection<ViewClassInfo> groupList, IProgressMonitor monitor) {
        LayoutParamsParser ldp = new LayoutParamsParser(classLoader, attrsXmlParser);
        
        try {
            ldp.parsePreferencesClasses(monitor);
            
            List<ViewClassInfo> prefs = ldp.getViews();
            List<ViewClassInfo> groups = ldp.getGroups();
    
            if (prefs != null && groups != null) {
                mainList.addAll(prefs);
                groupList.addAll(groups);
            }
        } catch (NoClassDefFoundError e) {
            AdtPlugin.logAndPrintError(e, TAG,
                    "Collect preferences failed, class %1$s not found in %2$s", //$NON-NLS-1$
                    e.getMessage(), 
                    classLoader.getSource());
        } catch (Throwable e) {
            AdtPlugin.log(e, "Android Framework Parser: failed to collect preference classes"); //$NON-NLS-1$
            AdtPlugin.printErrorToConsole("Android Framework Parser", "failed to collect preference classes");
        }
    }

    /**
     * Collects all menu definition information from the attrs.xml and returns it.
     * 
     * @param attrsXmlParser The parser of the attrs.xml file
     */
    private Map<String, DeclareStyleableInfo> collectMenuDefinitions(
            AttrsXmlParser attrsXmlParser) {
        Map<String, DeclareStyleableInfo> map = attrsXmlParser.getDeclareStyleableList();
        Map<String, DeclareStyleableInfo> map2 = new HashMap<String, DeclareStyleableInfo>();
        for (String key : new String[] { "Menu",        //$NON-NLS-1$
                                         "MenuItem",        //$NON-NLS-1$
                                         "MenuGroup" }) {   //$NON-NLS-1$
            if (map.containsKey(key)) {
                map2.put(key, map.get(key));
            } else {
                AdtPlugin.log(IStatus.WARNING,
                        "Menu declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath());
                AdtPlugin.printErrorToConsole("Android Framework Parser", 
                        String.format("Menu declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath()));
            }
        }
        
        return Collections.unmodifiableMap(map2);
    }

    /**
     * Collects all searchable definition information from the attrs.xml and returns it.
     * 
     * @param attrsXmlParser The parser of the attrs.xml file
     */
    private Map<String, DeclareStyleableInfo> collectSearchableDefinitions(
            AttrsXmlParser attrsXmlParser) {
        Map<String, DeclareStyleableInfo> map = attrsXmlParser.getDeclareStyleableList();
        Map<String, DeclareStyleableInfo> map2 = new HashMap<String, DeclareStyleableInfo>();
        for (String key : new String[] { "Searchable",              //$NON-NLS-1$
                                         "SearchableActionKey" }) { //$NON-NLS-1$
            if (map.containsKey(key)) {
                map2.put(key, map.get(key));
            } else {
                AdtPlugin.log(IStatus.WARNING,
                        "Searchable declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath());
                AdtPlugin.printErrorToConsole("Android Framework Parser",
                        String.format("Searchable declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath()));
            }
        }

        return Collections.unmodifiableMap(map2);
    }

    /**
     * Collects all manifest definition information from the attrs_manifest.xml and returns it.
     */
    private Map<String, DeclareStyleableInfo> collectManifestDefinitions(
            AttrsXmlParser attrsXmlParser) {

        return attrsXmlParser.getDeclareStyleableList();
    }

    /**
     * Returns the URL to the local documentation.
     * Can return null if no documentation is found in the current SDK.
     * 
     * @param osDocsPath Path to the documentation folder in the current SDK.
     *  The folder may not actually exist.
     * @return A file:// URL on the local documentation folder if it exists or null.
     */
    private String getDocumentationBaseUrl(String osDocsPath) {
        File f = new File(osDocsPath);

        if (f.isDirectory()) {
            try {
                // Note: to create a file:// URL, one would typically use something like
                // f.toURI().toURL().toString(). However this generates a broken path on
                // Windows, namely "C:\\foo" is converted to "file:/C:/foo" instead of
                // "file:///C:/foo" (i.e. there should be 3 / after "file:"). So we'll
                // do the correct thing manually.
                
                String path = f.getAbsolutePath();
                if (File.separatorChar != '/') {
                    path = path.replace(File.separatorChar, '/');
                }
                
                // For some reason the URL class doesn't add the mandatory "//" after
                // the "file:" protocol name, so it has to be hacked into the path.
                URL url = new URL("file", null, "//" + path);  //$NON-NLS-1$ //$NON-NLS-2$
                String result = url.toString();
                return result;
            } catch (MalformedURLException e) {
                // ignore malformed URLs
            }
        }

        return null;
    }

}
