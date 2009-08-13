/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.ide.eclipse.tests.functests.layoutRendering;

import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetParser;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData.LayoutBridge;
import com.android.ide.eclipse.tests.FuncTestCase;
import com.android.layoutlib.api.ILayoutResult;
import com.android.layoutlib.api.IProjectCallback;
import com.android.layoutlib.api.IResourceValue;
import com.android.layoutlib.api.IXmlPullParser;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class ApiDemosRenderingTest extends FuncTestCase {

    /**
     * Custom parser that implements {@link IXmlPullParser} (which itself extends
     * {@link XmlPullParser}).
     */
    private final static class TestParser extends KXmlParser implements IXmlPullParser {
        /**
         * Since we're not going to go through the result of the rendering/layout, we can return
         * null for the View Key.
         */
        public Object getViewKey() {
            return null;
        }
    }

    private final static class ProjectCallBack implements IProjectCallback {
        // resource id counter.
        // We start at 0x7f000000 to avoid colliding with the framework id
        // since we have no access to the project R.java and we need to generate them automatically.
        private int mIdCounter = 0x7f000000;

        // in some cases, the id that getResourceValue(String type, String name) returns
        // will be sent back to get the type/name. This map stores the id/type/name we generate
        // to be able to do the reverse resolution.
        private Map<Integer, String[]> mResourceMap = new HashMap<Integer, String[]>();

        private boolean mCustomViewAttempt = false;

        public String getNamespace() {
            // TODO: read from the ApiDemos manifest.
            return "com.example.android.apis";
        }

        public Integer getResourceValue(String type, String name) {
            Integer result = ++mIdCounter;
            mResourceMap.put(result, new String[] { name, type });
            return result;
        }

        @SuppressWarnings("unchecked")
        public Object loadView(String name, Class[] constructorSignature, Object[] constructorArgs)
                throws ClassNotFoundException, Exception {
            mCustomViewAttempt = true;
            return null;
        }

        public String[] resolveResourceValue(int id) {
            return mResourceMap.get(id);
        }

        public String resolveResourceValue(int[] id) {
            return null;
        }

    }

    private Sdk mSdk;

    public void testApiDemos() throws IOException, XmlPullParserException {
        loadSdk();
        findApiDemos();
    }

    /**
     * Loads the {@link Sdk}.
     */
    private void loadSdk() {
        mSdk = Sdk.loadSdk(this.getOsSdkLocation());

        int n = mSdk.getTargets().length;
        if (n > 0) {
            for (IAndroidTarget target : mSdk.getTargets()) {
                IStatus status = new AndroidTargetParser(target).run(new NullProgressMonitor());
                if (status.getCode() != IStatus.OK) {
                    fail("Failed to parse targets data");
                }
            }
        }
    }

    private void findApiDemos() throws IOException, XmlPullParserException {
        IAndroidTarget[] targets = mSdk.getTargets();

        for (IAndroidTarget target : targets) {
            String path = target.getPath(IAndroidTarget.SAMPLES);
            File samples = new File(path);
            if (samples.isDirectory()) {
                File[] files = samples.listFiles();
                for (File file : files) {
                    if ("apidemos".equalsIgnoreCase(file.getName())) {
                        testSample(target, file);
                        return;
                    }
                }
            }
        }

        fail("Failed to find ApiDemos!");
    }

    private void testSample(IAndroidTarget target, File sampleProject) throws IOException, XmlPullParserException {
        AndroidTargetData data = mSdk.getTargetData(target);
        if (data == null) {
            fail("No AndroidData!");
        }

        LayoutBridge bridge = data.getLayoutBridge();
        if (bridge.status != LoadStatus.LOADED || bridge.bridge == null) {
            fail("Fail to load the bridge");
        }

        File resFolder = new File(sampleProject, SdkConstants.FD_RES);
        if (resFolder.isDirectory() == false) {
            fail("Sample project has no res folder!");
        }

        // look for the layout folder
        File layoutFolder = new File(resFolder, SdkConstants.FD_LAYOUT);
        if (layoutFolder.isDirectory() == false) {
            fail("Sample project has no layout folder!");
        }

        // first load the project's target framework resource
        ProjectResources framework = ResourceManager.getInstance().loadFrameworkResources(target);

        // now load the project resources
        ProjectResources project = new ProjectResources(false /* isFrameworkRepository */);
        ResourceManager.getInstance().loadResources(project, resFolder);


        // Create a folder configuration that will be used for the rendering:
        FolderConfiguration config = getConfiguration();

        // get the configured resources
        Map<String, Map<String, IResourceValue>> configuredFramework =
                framework.getConfiguredResources(config);
        Map<String, Map<String, IResourceValue>> configuredProject =
                project.getConfiguredResources(config);

        boolean saveFiles = System.getenv("save_file") != null;

        // loop on the layouts and render them
        File[] layouts = layoutFolder.listFiles();
        for (File layout : layouts) {
            // create a parser for the layout file
            TestParser parser = new TestParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(new FileReader(layout));

            System.out.println("Rendering " + layout.getName());

            ProjectCallBack projectCallBack = new ProjectCallBack();

            ILayoutResult result = bridge.bridge.computeLayout(
                    parser,
                    null /*projectKey*/,
                    320,
                    480,
                    160, //density
                    160, //xdpi
                    160, // ydpi
                    "Theme", //themeName
                    false, //isProjectTheme
                    configuredProject,
                    configuredFramework,
                    projectCallBack,
                    null //logger
                    );

            if (result.getSuccess() != ILayoutResult.SUCCESS) {
                if (projectCallBack.mCustomViewAttempt == false) {
                    System.out.println("FAILED");
                    fail(String.format("Rendering %1$s: %2$s", layout.getName(),
                            result.getErrorMessage()));
                } else {
                    System.out.println("Ignore custom views for now");
                }
            } else {
                if (saveFiles) {
                    File tmp = File.createTempFile(layout.getName(), ".png");
                    ImageIO.write(result.getImage(), "png", tmp);
                }
                System.out.println("Success!");
            }
        }
    }

    private FolderConfiguration getConfiguration() {
        FolderConfiguration config = new FolderConfiguration();

        return config;
    }
}
