/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.sdklib.project;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.project.ProjectProperties.PropertyType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates the basic files needed to get an Android project up and running. Also
 * allows creation of IntelliJ project files.
 *
 * @hide
 */
public class ProjectCreator {
    
    private final static String PH_JAVA_FOLDER = "PACKAGE_PATH";
    private final static String PH_PACKAGE = "PACKAGE";
    private final static String PH_ACTIVITY_NAME = "ACTIVITY_NAME";
    
    private final static String FOLDER_TESTS = "tests";
    
    public enum OutputLevel {
        SILENT, NORMAL, VERBOSE;
    }

    private static class ProjectCreateException extends Exception {
        /** default UID. This will not be serialized anyway. */
        private static final long serialVersionUID = 1L;
        
        ProjectCreateException(String message) {
            super(message);
        }
        
        ProjectCreateException(Throwable t, String format, Object... args) {
            super(format != null ? String.format(format, args) : format, t);
        }

        ProjectCreateException(String format, Object... args) {
            super(String.format(format, args));
        }
    }
    
    private final OutputLevel mLevel;

    private final ISdkLog mLog;
    private final String mSdkFolder;
    
    public ProjectCreator(String sdkFolder, OutputLevel level, ISdkLog log) {
        mSdkFolder = sdkFolder;
        mLevel = level;
        mLog = log;
    }
    
    /**
     * Creates a new project.
     * @param folderPath the folder of the project to create. This folder must exist.
     * @param projectName the name of the project.
     * @param packageName the package of the project.
     * @param activityName the activity of the project as it will appear in the manifest.
     * @param target the project target.
     * @param isTestProject whether the project to create is a test project.
     */
    public void createProject(String folderPath, String projectName,
            String packageName, String activityName, IAndroidTarget target,
            boolean isTestProject) {
        
        // check project folder exists.
        File projectFolder = new File(folderPath);
        if (projectFolder.isDirectory() == false) {
            mLog.error(null, "Folder '%s' does not exist. Aborting...", folderPath);
            return;
        }
        
        try {
            // first create the project properties.

            // location of the SDK goes in localProperty
            ProjectProperties localProperties = ProjectProperties.create(folderPath,
                    PropertyType.LOCAL);
            localProperties.setProperty(ProjectProperties.PROPERTY_SDK, mSdkFolder);
            localProperties.save();

            // target goes in default properties
            ProjectProperties defaultProperties = ProjectProperties.create(folderPath,
                    PropertyType.DEFAULT);
            defaultProperties.setAndroidTarget(target);
            defaultProperties.save();

            // create the map for place-holders of values to replace in the templates
            final HashMap<String, String> keywords = new HashMap<String, String>();

            // create the required folders.
            // compute src folder path
            final String packagePath =
                stripString(packageName.replace(".", File.separator),
                        File.separatorChar);

            // put this path in the place-holder map for project files that needs to list
            // files manually.
            keywords.put(PH_JAVA_FOLDER, packagePath);

            keywords.put(PH_PACKAGE, packageName);
            if (activityName != null) {
                keywords.put(PH_ACTIVITY_NAME, activityName);
            }

            // create the source folder and the java package folders.
            final String srcFolderPath = SdkConstants.FD_SOURCES + File.separator + packagePath;
            File sourceFolder = createDirs(projectFolder, srcFolderPath);
            String javaTemplate = "java_file.template";
            String activityFileName = activityName + ".java";
            if (isTestProject) {
                javaTemplate = "java_tests_file.template";
                activityFileName = activityName + "Test.java";
            }
            installTemplate(javaTemplate, new File(sourceFolder, activityFileName),
                    keywords, target);

            // create other useful folders
            File resourceFodler = createDirs(projectFolder, SdkConstants.FD_RESOURCES);
            createDirs(projectFolder, SdkConstants.FD_OUTPUT);
            createDirs(projectFolder, SdkConstants.FD_NATIVE_LIBS);

            if (isTestProject == false) {
                /* Make res files only for non test projects */
                File valueFolder = createDirs(resourceFodler, SdkConstants.FD_VALUES);
                installTemplate("strings.template", new File(valueFolder, "strings.xml"),
                        keywords, target);

                File layoutFolder = createDirs(resourceFodler, SdkConstants.FD_LAYOUT);
                installTemplate("layout.template", new File(layoutFolder, "main.xml"),
                        keywords, target);
            }

            /* Make AndroidManifest.xml and build.xml files */
            String manifestTemplate = "AndroidManifest.template";
            if (isTestProject) {
                manifestTemplate = "AndroidManifest.tests.template"; 
            }

            installTemplate(manifestTemplate, new File(projectFolder, "AndroidManifest.xml"),
                    keywords, target);
            
            installTemplate("build.template", new File(projectFolder, "build.xml"), keywords);

            // if this is not a test project, then we create one.
            if (isTestProject == false) {
                // create the test project folder.
                createDirs(projectFolder, FOLDER_TESTS);
                File testProjectFolder = new File(folderPath, FOLDER_TESTS);
                
                createProject(testProjectFolder.getAbsolutePath(), projectName, packageName,
                        activityName, target, true /*isTestProject*/);
            }
        } catch (ProjectCreateException e) {
            mLog.error(e, null);
        } catch (IOException e) {
            mLog.error(e, null);
        }
    }
    
    /**
     * Installs a new file that is based on a template file provided by a given target.
     * Each match of each key from the place-holder map in the template will be replaced with its
     * corresponding value in the created file.
     * 
     * @param templateName the name of to the template file
     * @param dest the path to the destination file, relative to the project
     * @param placeholderMap a map of (place-holder, value) to create the file from the template.
     * @param target the Target of the project that will be providing the template.
     * @throws ProjectCreateException 
     */
    private void installTemplate(String templateName, File destFile,
            Map<String, String> placeholderMap, IAndroidTarget target)
            throws ProjectCreateException {
        // query the target for its template directory
        String templateFolder = target.getPath(IAndroidTarget.TEMPLATES);
        final String sourcePath = templateFolder + File.separator + templateName;

        installFullPathTemplate(sourcePath, destFile, placeholderMap);
    }

    /**
     * Installs a new file that is based on a template file provided by the tools folder.
     * Each match of each key from the place-holder map in the template will be replaced with its
     * corresponding value in the created file.
     * 
     * @param templateName the name of to the template file
     * @param dest the path to the destination file, relative to the project
     * @param placeholderMap a map of (place-holder, value) to create the file from the template.
     * @throws ProjectCreateException 
     */
    private void installTemplate(String templateName, File destFile,
            Map<String, String> placeholderMap)
            throws ProjectCreateException {
        // query the target for its template directory
        String templateFolder = mSdkFolder + File.separator + SdkConstants.OS_SDK_TOOLS_LIB_FOLDER;
        final String sourcePath = templateFolder + File.separator + templateName;

        installFullPathTemplate(sourcePath, destFile, placeholderMap);
    }

    /**
     * Installs a new file that is based on a template.
     * Each match of each key from the place-holder map in the template will be replaced with its
     * corresponding value in the created file.
     * 
     * @param sourcePath the full path to the source template file
     * @param destFile the destination file
     * @param placeholderMap a map of (place-holder, value) to create the file from the template.
     * @throws ProjectCreateException 
     */
    private void installFullPathTemplate(String sourcePath, File destFile,
            Map<String, String> placeholderMap) throws ProjectCreateException {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(destFile));
            BufferedReader in = new BufferedReader(new FileReader(sourcePath));
            String line;
            
            while ((line = in.readLine()) != null) {
                for (String key : placeholderMap.keySet()) {
                    line = line.replace(key, placeholderMap.get(key));
                }
                
                out.write(line);
                out.newLine();
            }
            
            out.close();
            in.close();
        } catch (Exception e) {
            throw new ProjectCreateException(e, "Could not access %1$s: %2$s",
                    destFile, e.getMessage());
        }
        
        println("Added file %1$s", destFile);
    }

    
    /**
     * Prints a message unless silence is enabled.
     * @param format Format for String.format
     * @param args Arguments for String.format
     */
    private void println(String format, Object... args) {
        if (mLevel == OutputLevel.VERBOSE) {
            System.out.println(String.format(format, args));
        }
    }

    /**
     * Creates a new folder, along with any parent folders that do not exists.
     * 
     * @param parent the parent folder
     * @param name the name of the directory to create.
     * @throws ProjectCreateException 
     */
    private File createDirs(File parent, String name) throws ProjectCreateException {
        final File newFolder = new File(parent, name);
        boolean existedBefore = true;

        if (!newFolder.exists()) {
            if (!newFolder.mkdirs()) {
                throw new ProjectCreateException("Could not create directory: %1$s", newFolder);
            }
            existedBefore = false;
        }

        if (newFolder.isDirectory()) {
            if (!newFolder.canWrite()) {
                throw new ProjectCreateException("Path is not writable: %1$s", newFolder);
            }
        } else {
            throw new ProjectCreateException("Path is not a directory: %1$s", newFolder);
        }

        if (!existedBefore) {
            try {
                println("Created directory %1$s", newFolder.getCanonicalPath());
            } catch (IOException e) {
                throw new ProjectCreateException(
                        "Could not determine canonical path of created directory", e);
            }
        }
        
        return newFolder;
    }

    /**
     * Strips the string of beginning and trailing characters (multiple
     * characters will be stripped, example stripString("..test...", '.')
     * results in "test";
     * 
     * @param s the string to strip
     * @param strip the character to strip from beginning and end
     * @return the stripped string or the empty string if everything is stripped.
     */
    private static String stripString(String s, char strip) {
        final int sLen = s.length();
        int newStart = 0, newEnd = sLen - 1;
        
        while (newStart < sLen && s.charAt(newStart) == strip) {
          newStart++;
        }
        while (newEnd >= 0 && s.charAt(newEnd) == strip) {
          newEnd--;
        }
        
        /*
         * newEnd contains a char we want, and substring takes end as being
         * exclusive
         */
        newEnd++;
        
        if (newStart >= sLen || newEnd < 0) {
            return "";
        }
        
        return s.substring(newStart, newEnd);
    }
}
