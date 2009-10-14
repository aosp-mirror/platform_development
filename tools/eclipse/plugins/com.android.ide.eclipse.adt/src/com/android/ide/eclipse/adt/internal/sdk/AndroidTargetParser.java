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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.LayoutDescriptors;
import com.android.ide.eclipse.adt.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.adt.internal.editors.menu.descriptors.MenuDescriptors;
import com.android.ide.eclipse.adt.internal.editors.xml.descriptors.XmlDescriptors;
import com.android.ide.eclipse.adt.internal.resources.AttrsXmlParser;
import com.android.ide.eclipse.adt.internal.resources.DeclareStyleableInfo;
import com.android.ide.eclipse.adt.internal.resources.IResourceRepository;
import com.android.ide.eclipse.adt.internal.resources.ResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.ViewClassInfo;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData.LayoutBridge;
import com.android.layoutlib.api.ILayoutBridge;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.InvalidAttributeValueException;

/**
 * Parser for the platform data in an SDK.
 * <p/>
 * This gather the following information:
 * <ul>
 * <li>Resource ID from <code>android.R</code></li>
 * <li>The list of permissions values from <code>android.Manifest$permission</code></li>
 * <li></li>
 * </ul>
 */
public final class AndroidTargetParser {

    private static final String TAG = "Framework Resource Parser";
    private final IAndroidTarget mAndroidTarget;

    /**
     * Creates a platform data parser.
     */
    public AndroidTargetParser(IAndroidTarget platformTarget) {
        mAndroidTarget = platformTarget;
    }

    /**
     * Parses the framework, collects all interesting information and stores them in the
     * {@link IAndroidTarget} given to the constructor.
     *
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     * @return True if the SDK path was valid and parsing has been attempted.
     */
    public IStatus run(IProgressMonitor monitor) {
        try {
            SubMonitor progress = SubMonitor.convert(monitor,
                    String.format("Parsing SDK %1$s", mAndroidTarget.getName()),
                    14);

            AndroidTargetData targetData = new AndroidTargetData(mAndroidTarget);

            // load DX.
            DexWrapper dexWrapper = new DexWrapper();
            IStatus res = dexWrapper.loadDex(mAndroidTarget.getPath(IAndroidTarget.DX_JAR));
            if (res != Status.OK_STATUS) {
                return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        String.format("dx.jar loading failed for target '%1$s'",
                                mAndroidTarget.getFullName()));
            }

            // we have loaded dx.
            targetData.setDexWrapper(dexWrapper);
            progress.worked(1);

            // parse the rest of the data.

            AndroidJarLoader classLoader =
                new AndroidJarLoader(mAndroidTarget.getPath(IAndroidTarget.ANDROID_JAR));

            preload(classLoader, progress.newChild(40, SubMonitor.SUPPRESS_NONE));

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // get the resource Ids.
            progress.subTask("Resource IDs");
            IResourceRepository frameworkRepository = collectResourceIds(classLoader);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // get the permissions
            progress.subTask("Permissions");
            String[] permissionValues = collectPermissions(classLoader);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // get the action and category values for the Intents.
            progress.subTask("Intents");
            ArrayList<String> activity_actions = new ArrayList<String>();
            ArrayList<String> broadcast_actions = new ArrayList<String>();
            ArrayList<String> service_actions = new ArrayList<String>();
            ArrayList<String> categories = new ArrayList<String>();
            collectIntentFilterActionsAndCategories(activity_actions, broadcast_actions,
                    service_actions, categories);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // gather the attribute definition
            progress.subTask("Attributes definitions");
            AttrsXmlParser attrsXmlParser = new AttrsXmlParser(
                    mAndroidTarget.getPath(IAndroidTarget.ATTRIBUTES));
            attrsXmlParser.preload();
            progress.worked(1);

            progress.subTask("Manifest definitions");
            AttrsXmlParser attrsManifestXmlParser = new AttrsXmlParser(
                    mAndroidTarget.getPath(IAndroidTarget.MANIFEST_ATTRIBUTES),
                    attrsXmlParser);
            attrsManifestXmlParser.preload();
            progress.worked(1);

            Collection<ViewClassInfo> mainList = new ArrayList<ViewClassInfo>();
            Collection<ViewClassInfo> groupList = new ArrayList<ViewClassInfo>();

            // collect the layout/widgets classes
            progress.subTask("Widgets and layouts");
            collectLayoutClasses(classLoader, attrsXmlParser, mainList, groupList,
                    progress.newChild(1));

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            ViewClassInfo[] layoutViewsInfo = mainList.toArray(new ViewClassInfo[mainList.size()]);
            ViewClassInfo[] layoutGroupsInfo = groupList.toArray(
                    new ViewClassInfo[groupList.size()]);

            // collect the preferences classes.
            mainList.clear();
            groupList.clear();
            collectPreferenceClasses(classLoader, attrsXmlParser, mainList, groupList,
                    progress.newChild(1));

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
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

            Map<String, DeclareStyleableInfo> xmlAppWidgetMap = null;
            if (mAndroidTarget.getVersion().getApiLevel() >= 3) {
                xmlAppWidgetMap = collectAppWidgetDefinitions(attrsXmlParser);
            }

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // From the information that was collected, create the pieces that will be put in
            // the PlatformData object.
            AndroidManifestDescriptors manifestDescriptors = new AndroidManifestDescriptors();
            manifestDescriptors.updateDescriptors(manifestMap);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            LayoutDescriptors layoutDescriptors = new LayoutDescriptors();
            layoutDescriptors.updateDescriptors(layoutViewsInfo, layoutGroupsInfo);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            MenuDescriptors menuDescriptors = new MenuDescriptors();
            menuDescriptors.updateDescriptors(xmlMenuMap);
            progress.worked(1);

            if (progress.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            XmlDescriptors xmlDescriptors = new XmlDescriptors();
            xmlDescriptors.updateDescriptors(
                    xmlSearchableMap,
                    xmlAppWidgetMap,
                    preferencesInfo,
                    preferenceGroupsInfo);
            progress.worked(1);

            // load the framework resources.
            ProjectResources resources = ResourceManager.getInstance().loadFrameworkResources(
                    mAndroidTarget);
            progress.worked(1);

            // now load the layout lib bridge
            LayoutBridge layoutBridge = loadLayoutBridge();
            progress.worked(1);

            // and finally create the PlatformData with all that we loaded.
            targetData.setExtraData(frameworkRepository,
                    manifestDescriptors,
                    layoutDescriptors,
                    menuDescriptors,
                    xmlDescriptors,
                    enumValueMap,
                    permissionValues,
                    activity_actions.toArray(new String[activity_actions.size()]),
                    broadcast_actions.toArray(new String[broadcast_actions.size()]),
                    service_actions.toArray(new String[service_actions.size()]),
                    categories.toArray(new String[categories.size()]),
                    mAndroidTarget.getPlatformLibraries(),
                    mAndroidTarget.getOptionalLibraries(),
                    resources,
                    layoutBridge);

            Sdk.getCurrent().setTargetData(mAndroidTarget, targetData);

            return Status.OK_STATUS;
        } catch (Exception e) {
            AdtPlugin.logAndPrintError(e, TAG, "SDK parser failed"); //$NON-NLS-1$
            AdtPlugin.printToConsole("SDK parser failed", e.getMessage());
            return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, "SDK parser failed", e);
        }
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
            classLoader.preLoadClasses("" /* all classes */,        //$NON-NLS-1$
                    mAndroidTarget.getName(),                       // monitor task label
                    monitor);
        } catch (InvalidAttributeValueException e) {
            AdtPlugin.log(e, "Problem preloading classes"); //$NON-NLS-1$
        } catch (IOException e) {
            AdtPlugin.log(e, "Problem preloading classes"); //$NON-NLS-1$
        }
    }

    /**
     * Creates an IResourceRepository for the framework resources.
     *
     * @param classLoader The framework SDK jar classloader
     * @return a map of the resources, or null if it failed.
     */
    private IResourceRepository collectResourceIds(
            AndroidJarLoader classLoader) {
        try {
            Class<?> r = classLoader.loadClass(AndroidConstants.CLASS_R);

            if (r != null) {
                Map<ResourceType, List<ResourceItem>> map = parseRClass(r);
                if (map != null) {
                    return new FrameworkResourceRepository(map);
                }
            }
        } catch (ClassNotFoundException e) {
            AdtPlugin.logAndPrintError(e, TAG,
                    "Collect resource IDs failed, class %1$s not found in %2$s", //$NON-NLS-1$
                    AndroidConstants.CLASS_R,
                    mAndroidTarget.getPath(IAndroidTarget.ANDROID_JAR));
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
                    mAndroidTarget.getPath(IAndroidTarget.ANDROID_JAR));
        }

        return new String[0];
    }

    /**
     * Loads and collects the action and category default values from the framework.
     * The values are added to the <code>actions</code> and <code>categories</code> lists.
     *
     * @param activityActions the list which will receive the activity action values.
     * @param broadcastActions the list which will receive the broadcast action values.
     * @param serviceActions the list which will receive the service action values.
     * @param categories the list which will receive the category values.
     */
    private void collectIntentFilterActionsAndCategories(ArrayList<String> activityActions,
            ArrayList<String> broadcastActions,
            ArrayList<String> serviceActions, ArrayList<String> categories)  {
        collectValues(mAndroidTarget.getPath(IAndroidTarget.ACTIONS_ACTIVITY),
                activityActions);
        collectValues(mAndroidTarget.getPath(IAndroidTarget.ACTIONS_BROADCAST),
                broadcastActions);
        collectValues(mAndroidTarget.getPath(IAndroidTarget.ACTIONS_SERVICE),
                serviceActions);
        collectValues(mAndroidTarget.getPath(IAndroidTarget.CATEGORIES),
                categories);
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
     * @param classLoader The framework SDK jar classloader in case we cannot get the widget from
     * the platform directly
     * @param attrsXmlParser The parser of the attrs.xml file
     * @param mainList the Collection to receive the main list of {@link ViewClassInfo}.
     * @param groupList the Collection to receive the group list of {@link ViewClassInfo}.
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     */
    private void collectLayoutClasses(AndroidJarLoader classLoader,
            AttrsXmlParser attrsXmlParser,
            Collection<ViewClassInfo> mainList, Collection<ViewClassInfo> groupList,
            IProgressMonitor monitor) {
        LayoutParamsParser ldp = null;
        try {
            WidgetClassLoader loader = new WidgetClassLoader(
                    mAndroidTarget.getPath(IAndroidTarget.WIDGETS));
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
                    "Collect preferences failed, class %1$s not found in %2$s",
                    e.getMessage(),
                    classLoader.getSource());
        } catch (Throwable e) {
            AdtPlugin.log(e, "Android Framework Parser: failed to collect preference classes"); //$NON-NLS-1$
            AdtPlugin.printErrorToConsole("Android Framework Parser",
                    "failed to collect preference classes");
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
     * Collects all appWidgetProviderInfo definition information from the attrs.xml and returns it.
     *
     * @param attrsXmlParser The parser of the attrs.xml file
     */
    private Map<String, DeclareStyleableInfo> collectAppWidgetDefinitions(
            AttrsXmlParser attrsXmlParser) {
        Map<String, DeclareStyleableInfo> map = attrsXmlParser.getDeclareStyleableList();
        Map<String, DeclareStyleableInfo> map2 = new HashMap<String, DeclareStyleableInfo>();
        for (String key : new String[] { "AppWidgetProviderInfo" }) {  //$NON-NLS-1$
            if (map.containsKey(key)) {
                map2.put(key, map.get(key));
            } else {
                AdtPlugin.log(IStatus.WARNING,
                        "AppWidget declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
                        key, attrsXmlParser.getOsAttrsXmlPath());
                AdtPlugin.printErrorToConsole("Android Framework Parser",
                        String.format("AppWidget declare-styleable %1$s not found in file %2$s", //$NON-NLS-1$
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
     * Loads the layout bridge from the dynamically loaded layoutlib.jar
     */
    private LayoutBridge loadLayoutBridge() {
        LayoutBridge layoutBridge = new LayoutBridge();

        try {
            // get the URL for the file.
            File f = new File(mAndroidTarget.getPath(IAndroidTarget.LAYOUT_LIB));
            if (f.isFile() == false) {
                AdtPlugin.log(IStatus.ERROR, "layoutlib.jar is missing!"); //$NON-NLS-1$
            } else {
                URL url = f.toURL();

                // create a class loader. Because this jar reference interfaces
                // that are in the editors plugin, it's important to provide
                // a parent class loader.
                layoutBridge.classLoader = new URLClassLoader(new URL[] { url },
                        this.getClass().getClassLoader());

                // load the class
                Class<?> clazz = layoutBridge.classLoader.loadClass(AndroidConstants.CLASS_BRIDGE);
                if (clazz != null) {
                    // instantiate an object of the class.
                    Constructor<?> constructor = clazz.getConstructor();
                    if (constructor != null) {
                        Object bridge = constructor.newInstance();
                        if (bridge instanceof ILayoutBridge) {
                            layoutBridge.bridge = (ILayoutBridge)bridge;
                        }
                    }
                }

                if (layoutBridge.bridge == null) {
                    layoutBridge.status = LoadStatus.FAILED;
                    AdtPlugin.log(IStatus.ERROR, "Failed to load " + AndroidConstants.CLASS_BRIDGE); //$NON-NLS-1$
                } else {
                    // get the api level
                    try {
                        layoutBridge.apiLevel = layoutBridge.bridge.getApiLevel();
                    } catch (AbstractMethodError e) {
                        // the first version of the api did not have this method
                        layoutBridge.apiLevel = 1;
                    }

                    // and mark the lib as loaded.
                    layoutBridge.status = LoadStatus.LOADED;
                }
            }
        } catch (Throwable t) {
            layoutBridge.status = LoadStatus.FAILED;
            // log the error.
            AdtPlugin.log(t, "Failed to load the LayoutLib");
        }

        return layoutBridge;
    }
}
