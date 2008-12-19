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

package com.android.jarutils;

import com.android.jarutils.SignedJarBuilder.IZipEntryFilter;

/**
 * A basic implementation of {@link IZipEntryFilter} to filter out anything that is not a
 * java resource.
 */
public class JavaResourceFilter implements IZipEntryFilter {

    public boolean checkEntry(String name) {
        // split the path into segments.
        String[] segments = name.split("/");

        // empty path? skip to next entry.
        if (segments.length == 0) {
            return false;
        }

        // Check each folders to make sure they should be included.
        // Folders like CVS, .svn, etc.. should already have been excluded from the
        // jar file, but we need to exclude some other folder (like /META-INF) so
        // we check anyway.
        for (int i = 0 ; i < segments.length - 1; i++) {
            if (checkFolderForPackaging(segments[i]) == false) {
                return false;
            }
        }

        // get the file name from the path
        String fileName = segments[segments.length-1];
        
        return checkFileForPackaging(fileName);
    }
    
    /**
     * Checks whether a folder and its content is valid for packaging into the .apk as
     * standard Java resource.
     * @param folderName the name of the folder.
     */
    public static boolean checkFolderForPackaging(String folderName) {
        return folderName.equals("CVS") == false &&
            folderName.equals(".svn") == false &&
            folderName.equals("SCCS") == false &&
            folderName.equals("META-INF") == false &&
            folderName.startsWith("_") == false;
    }

    /**
     * Checks a file to make sure it should be packaged as standard resources.
     * @param fileName the name of the file (including extension)
     * @return true if the file should be packaged as standard java resources.
     */
    public static boolean checkFileForPackaging(String fileName) {
        String[] fileSegments = fileName.split("\\.");
        String fileExt = "";
        if (fileSegments.length > 1) {
            fileExt = fileSegments[fileSegments.length-1];
        }

        return checkFileForPackaging(fileName, fileExt);
    }

    /**
     * Checks a file to make sure it should be packaged as standard resources.
     * @param fileName the name of the file (including extension)
     * @param extension the extension of the file (excluding '.')
     * @return true if the file should be packaged as standard java resources.
     */
    public static boolean checkFileForPackaging(String fileName, String extension) {
        return "aidl".equalsIgnoreCase(extension) == false &&
            "java".equalsIgnoreCase(extension) == false &&
            "class".equalsIgnoreCase(extension) == false &&
            "package.html".equalsIgnoreCase(fileName) == false &&
            "overview.html".equalsIgnoreCase(fileName) == false &&
            ".cvsignore".equalsIgnoreCase(fileName) == false &&
            ".DS_Store".equals(fileName) == false && 
            fileName.charAt(fileName.length()-1) != '~';
    }
}
