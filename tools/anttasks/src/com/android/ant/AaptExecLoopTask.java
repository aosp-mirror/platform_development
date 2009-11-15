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

import com.android.sdklib.internal.project.ApkConfigurationHelper;
import com.android.sdklib.internal.project.ApkSettings;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Task able to run an Exec task on aapt several times.
 * It does not follow the exec task format, instead it has its own parameters, which maps
 * directly to aapt.
 *
 */
public final class AaptExecLoopTask extends Task {

    private String mExecutable;
    private String mCommand;
    private String mManifest;
    private String mResources;
    private String mAssets;
    private String mAndroidJar;
    private String mOutFolder;
    private String mBaseName;

    /**
     * Sets the value of the "executable" attribute.
     * @param executable the value.
     */
    public void setExecutable(String executable) {
        mExecutable = executable;
    }

    /**
     * Sets the value of the "command" attribute.
     * @param command the value.
     */
    public void setCommand(String command) {
        mCommand = command;
    }

    /**
     * Sets the value of the "manifest" attribute.
     * @param manifest the value.
     */
    public void setManifest(Path manifest) {
        mManifest = manifest.toString();
    }

    /**
     * Sets the value of the "resources" attribute.
     * @param resources the value.
     */
    public void setResources(Path resources) {
        mResources = resources.toString();
    }

    /**
     * Sets the value of the "assets" attribute.
     * @param assets the value.
     */
    public void setAssets(Path assets) {
        mAssets = assets.toString();
    }

    /**
     * Sets the value of the "androidjar" attribute.
     * @param androidJar the value.
     */
    public void setAndroidjar(Path androidJar) {
        mAndroidJar = androidJar.toString();
    }

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

    /*
     * (non-Javadoc)
     *
     * Executes the loop. Based on the values inside default.properties, this will
     * create alternate temporary ap_ files.
     *
     * @see org.apache.tools.ant.Task#execute()
     */
    @Override
    public void execute() throws BuildException {
        Project taskProject = getProject();

        // first do a full resource package
        createPackage(null /*configName*/, null /*resourceFilter*/);

        // now see if we need to create file with filtered resources.
        // Get the project base directory.
        File baseDir = taskProject.getBaseDir();
        ProjectProperties properties = ProjectProperties.load(baseDir.getAbsolutePath(),
                PropertyType.DEFAULT);


        ApkSettings apkSettings = ApkConfigurationHelper.getSettings(properties);
        if (apkSettings != null) {
            Map<String, String> apkFilters = apkSettings.getResourceFilters();
            if (apkFilters.size() > 0) {
                for (Entry<String, String> entry : apkFilters.entrySet()) {
                    createPackage(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Creates a resource package.
     * @param configName the name of the filter config. Can be null in which case a full resource
     * package will be generated.
     * @param resourceFilter the resource configuration filter to pass to aapt (if configName is
     * non null)
     */
    private void createPackage(String configName, String resourceFilter) {
        Project taskProject = getProject();

        if (configName == null || resourceFilter == null) {
            System.out.println("Creating full resource package...");
        } else {
            System.out.println(String.format(
                    "Creating resource package for config '%1$s' (%2$s)...",
                    configName, resourceFilter));
        }

        // create a task for the default apk.
        ExecTask task = new ExecTask();
        task.setExecutable(mExecutable);
        task.setFailonerror(true);

        // aapt command. Only "package" is supported at this time really.
        task.createArg().setValue(mCommand);

        // filters if needed
        if (configName != null && resourceFilter != null) {
            task.createArg().setValue("-c");
            task.createArg().setValue(resourceFilter);
        }

        // force flag
        task.createArg().setValue("-f");

        // manifest location
        task.createArg().setValue("-M");
        task.createArg().setValue(mManifest);

        // resources location. This may not exists, and aapt doesn't like it, so we check first.
        File res = new File(mResources);
        if (res.isDirectory()) {
            task.createArg().setValue("-S");
            task.createArg().setValue(mResources);
        }

        // assets location. This may not exists, and aapt doesn't like it, so we check first.
        File assets = new File(mAssets);
        if (assets.isDirectory()) {
            task.createArg().setValue("-A");
            task.createArg().setValue(mAssets);
        }

        // android.jar
        task.createArg().setValue("-I");
        task.createArg().setValue(mAndroidJar);

        // out file. This is based on the outFolder, baseName, and the configName (if applicable)
        String filename;
        if (configName != null && resourceFilter != null) {
            filename = mBaseName + "-" + configName + ".ap_";
        } else {
            filename = mBaseName + ".ap_";
        }

        File file = new File(mOutFolder, filename);
        task.createArg().setValue("-F");
        task.createArg().setValue(file.getAbsolutePath());

        // final setup of the task
        task.setProject(taskProject);
        task.setOwningTarget(getOwningTarget());

        // execute it.
        task.execute();
    }
}
