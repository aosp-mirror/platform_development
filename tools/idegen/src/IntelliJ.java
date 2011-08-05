/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

/**
 * Generates an IntelliJ project.
 */
public class IntelliJ {

    private static final String IDEA_IML = "android.iml";
    private static final String IDEA_IPR = "android.ipr";

    /**
     * Generates IntelliJ configuration files from the given configuration.
     */
    public static void generateFrom(Configuration c) throws IOException {
        File templatesDirectory = new File(c.toolDirectory, "templates");
        String ipr = Files.toString(new File(templatesDirectory, IDEA_IPR));
        Files.toFile(ipr, new File(IDEA_IPR));

        String iml = Files.toString(new File(templatesDirectory, IDEA_IML));

        StringBuilder sourceRootsXml = new StringBuilder();
        for (File sourceRoot : c.sourceRoots) {
            sourceRootsXml.append("<sourceFolder url=\"file://$MODULE_DIR$/")
                .append(sourceRoot.getPath())
                .append("\" isTestSource=\"").append(isTests(sourceRoot))
                .append("\"/>\n");
        }

        /*
         * IntelliJ excludes are module-wide. We explicitly exclude directories
         * under source roots but leave the rest in so you can still pull
         * up random non-Java files.
         */
        StringBuilder excludeXml = new StringBuilder();
        for (File excludedDir : c.excludesUnderSourceRoots()) {
            sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/")
                .append(excludedDir.getPath())
                .append("\"/>\n");
        }

        // Exclude Eclipse's output directory.
        sourceRootsXml.append("<excludeFolder "
                + "url=\"file://$MODULE_DIR$/out/eclipse\"/>\n");

        // Exclude some other directories that take a long time to scan.
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/.repo\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/external/bluetooth\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/external/chromium\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/external/icu4c\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/external/webkit\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/frameworks/base/docs\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/out/host\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/out/target/common/docs\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/out/target/common/obj/JAVA_LIBRARIES/android_stubs_current_intermediates\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/out/target/product\"/>\n");
        sourceRootsXml.append("<excludeFolder url=\"file://$MODULE_DIR$/prebuilt\"/>\n");

        StringBuilder jarsXml = new StringBuilder();
        for (File jar : c.jarFiles) {
            jarsXml.append("<orderEntry type=\"module-library\">"
                    + "<library><CLASSES><root url=\"jar://$MODULE_DIR$/")
                .append(jar.getPath())
            .append("!/\"/></CLASSES><JAVADOC/><SOURCES/></library>"
                    + "</orderEntry>\n");
        }

        iml = iml.replace("SOURCE_FOLDERS",
                sourceRootsXml.toString() + excludeXml.toString());
        iml = iml.replace("JAR_ENTRIES", jarsXml.toString());

        Files.toFile(iml, new File(IDEA_IML));
    }

    private static boolean isTests(File file) {
        String path = file.getPath();

        // test-runner is testing infrastructure, not test code.
        if (path.contains("test-runner")) {
            return false;
        }

        return path.toUpperCase().contains("TEST");
    }
}
