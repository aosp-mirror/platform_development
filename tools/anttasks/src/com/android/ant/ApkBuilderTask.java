/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ant;

import com.android.apkbuilder.ApkBuilder;
import com.android.apkbuilder.ApkBuilder.ApkFile;
import com.android.sdklib.project.ApkConfigurationHelper;
import com.android.sdklib.project.ProjectProperties;
import com.android.sdklib.project.ProjectProperties.PropertyType;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class ApkBuilderTask extends Task {
    
    /**
     * Class to represent nested elements. Since they all have only one attribute ('path'), the
     * same class can be used for all the nested elements (zip, file, sourcefolder, jarfolder,
     * nativefolder).
     */
    public final static class Value extends ProjectComponent {
        String mPath;
        
        /**
         * Sets the value of the "path" attribute.
         * @param path the value.
         */
        public void setPath(Path path) {
            mPath = path.toString();
        }
    }

    private String mOutFolder;
    private String mBaseName;
    private boolean mVerbose = false;
    private boolean mSigned = true;
    
    private final ArrayList<Value> mZipList = new ArrayList<Value>();
    private final ArrayList<Value> mFileList = new ArrayList<Value>();
    private final ArrayList<Value> mSourceList = new ArrayList<Value>();
    private final ArrayList<Value> mJarList = new ArrayList<Value>();
    private final ArrayList<Value> mNativeList = new ArrayList<Value>();

    private final ArrayList<FileInputStream> mZipArchives = new ArrayList<FileInputStream>();
    private final ArrayList<File> mArchiveFiles = new ArrayList<File>();
    private final ArrayList<ApkFile> mJavaResources = new ArrayList<ApkFile>();
    private final ArrayList<FileInputStream> mResourcesJars = new ArrayList<FileInputStream>();
    private final ArrayList<ApkFile> mNativeLibraries = new ArrayList<ApkFile>();

    /**
     * Sets the value of the "outfolder" attribute.
     * @param outFolder the value.
     */
    public void setOutfolder(Path outFolder) {
        mOutFolder = outFolder.toString();
    }
    
    /**
     * Sets the value of the "basename" attribute.
     * @param baseName the value.
     */
    public void setBasename(String baseName) {
        mBaseName = baseName;
    }
    
    /**
     * Sets the value of the "verbose" attribute.
     * @param verbose the value.
     */
    public void setVerbose(boolean verbose) {
        mVerbose = verbose;
    }
    
    /**
     * Sets the value of the "signed" attribute.
     * @param signed the value.
     */
    public void setSigned(boolean signed) {
        mSigned = signed;
    }
    
    /**
     * Returns an object representing a nested <var>zip</var> element.
     */
    public Object createZip() {
        Value zip = new Value();
        mZipList.add(zip);
        return zip;
    }
    
    /**
     * Returns an object representing a nested <var>file</var> element.
     */
    public Object createFile() {
        Value file = new Value();
        mFileList.add(file);
        return file;
    }

    /**
     * Returns an object representing a nested <var>sourcefolder</var> element.
     */
    public Object createSourcefolder() {
        Value file = new Value();
        mSourceList.add(file);
        return file;
    }

    /**
     * Returns an object representing a nested <var>jarfolder</var> element.
     */
    public Object createJarfolder() {
        Value file = new Value();
        mJarList.add(file);
        return file;
    }
    
    /**
     * Returns an object representing a nested <var>nativefolder</var> element.
     */
    public Object createNativefolder() {
        Value file = new Value();
        mNativeList.add(file);
        return file;
    }
    
    @Override
    public void execute() throws BuildException {
        Project taskProject = getProject();
        
        ApkBuilder apkBuilder = new ApkBuilder();
        apkBuilder.setVerbose(mVerbose);
        apkBuilder.setSignedPackage(mSigned);
        
        try {
            // setup the list of everything that needs to go in the archive.
            
            // go through the list of zip files to add. This will not include
            // the resource package, which is handled separaly for each apk to create.
            for (Value v : mZipList) {
                FileInputStream input = new FileInputStream(v.mPath);
                mZipArchives.add(input);
            }
            
            // now go through the list of file to directly add the to the list.
            for (Value v : mFileList) {
                mArchiveFiles.add(ApkBuilder.getInputFile(v.mPath));
            }
            
            // now go through the list of file to directly add the to the list.
            for (Value v : mSourceList) {
                ApkBuilder.processSourceFolderForResource(v.mPath, mJavaResources);
            }
            
            // now go through the list of jar folders.
            for (Value v : mJarList) {
                ApkBuilder.processJarFolder(v.mPath, mResourcesJars);
            }
            
            // now the native lib folder.
            for (Value v : mNativeList) {
                String parameter = v.mPath;
                File f = new File(parameter);
                
                // compute the offset to get the relative path
                int offset = parameter.length();
                if (parameter.endsWith(File.separator) == false) {
                    offset++;
                }

                ApkBuilder.processNativeFolder(offset, f, mNativeLibraries);
            }

            
            // first do a full resource package
            createApk(apkBuilder, null /*configName*/, null /*resourceFilter*/);
    
            // now see if we need to create file with filtered resources.
            // Get the project base directory.
            File baseDir = taskProject.getBaseDir();
            ProjectProperties properties = ProjectProperties.load(baseDir.getAbsolutePath(),
                    PropertyType.DEFAULT);
            
            Map<String, String> apkConfigs = ApkConfigurationHelper.getConfigs(properties);
            if (apkConfigs.size() > 0) {
                Set<Entry<String, String>> entrySet = apkConfigs.entrySet();
                for (Entry<String, String> entry : entrySet) {
                    createApk(apkBuilder, entry.getKey(), entry.getValue());
                }
            }
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        } catch (IllegalArgumentException e) {
            throw new BuildException(e);
        }
    }
    
    /**
     * Creates an application package.
     * @param apkBuilder 
     * @param configName the name of the filter config. Can be null in which case a full resource
     * package will be generated.
     * @param resourceFilter the resource configuration filter to pass to aapt (if configName is
     * non null)
     * @throws FileNotFoundException 
     */
    private void createApk(ApkBuilder apkBuilder, String configName, String resourceFilter)
            throws FileNotFoundException {
        // All the files to be included in the archive have already been prep'ed up, except
        // the resource package.
        // figure out its name.
        String filename;
        if (configName != null && resourceFilter != null) {
            filename = mBaseName + "-" + configName + ".ap_";
        } else {
            filename = mBaseName + ".ap_";
        }
        
        // now we add it to the list of zip archive (it's just a zip file).
        
        // it's used as a zip archive input
        FileInputStream resoucePackageZipFile = new FileInputStream(new File(mOutFolder, filename));
        mZipArchives.add(resoucePackageZipFile);
        
        // prepare the filename to generate. Same thing as the resource file.
        if (configName != null && resourceFilter != null) {
            filename = mBaseName + "-" + configName;
        } else {
            filename = mBaseName;
        }
        
        if (mSigned) {
            filename = filename + "-debug.apk";
        } else {
            filename = filename + "-unsigned.apk";
        }

        if (configName == null || resourceFilter == null) {
            if (mSigned) {
                System.out.println(String.format(
                        "Creating %s and signing it with a debug key...", filename));
            } else {
                System.out.println(String.format(
                        "Creating %s for release...", filename));
            }
        } else {
            if (mSigned) {
                System.out.println(String.format(
                        "Creating %1$s (with %2$s) and signing it with a debug key...",
                        filename, resourceFilter));
            } else {
                System.out.println(String.format(
                        "Creating %1$s (with %2$s) for release...",
                        filename, resourceFilter));
            }
        }
        
        File f = new File(mOutFolder, filename);
        
        // and generate the apk
        apkBuilder.createPackage(f.getAbsoluteFile(), mZipArchives,
                mArchiveFiles, mJavaResources, mResourcesJars, mNativeLibraries);
        
        // we are done. We need to remove the resource package from the list of zip archives
        // in case we have another apk to generate.
        mZipArchives.remove(resoucePackageZipFile);
    }
}
