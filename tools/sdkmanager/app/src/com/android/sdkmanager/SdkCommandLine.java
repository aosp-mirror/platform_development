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

package com.android.sdkmanager;

import com.android.sdklib.SdkManager;


/**
 * Specific command-line flags for the {@link SdkManager}.
 */
public class SdkCommandLine extends CommandLineProcessor {

    public static final String ARG_ALIAS = "alias";
    public static final String ARG_ACTIVITY = "activity";
    public static final String ARG_VM = "vm";
    public static final String ARG_TARGET = "target";
    public static final String ARG_ALL = "all";
    
    public static final String KEY_IN = "in";
    public static final String KEY_ACTIVITY = ARG_ACTIVITY;
    public static final String KEY_PACKAGE = "package";
    public static final String KEY_MODE = "mode";
    public static final String KEY_TARGET_ID = ARG_TARGET;
    public static final String KEY_NAME = "name";
    public static final String KEY_OUT = "out";
    public static final String KEY_FILTER = "filter";

    public final static String ACTION_LIST = "list";
    public final static String ACTION_NEW_VM = ARG_VM;
    public final static String ACTION_NEW_PROJECT = "project";
    public final static String ACTION_UPDATE_PROJECT = "update";
    
    private final static String[][] ACTIONS = {
        { ACTION_LIST,
            "Lists existing targets or VMs." },
        { ACTION_NEW_VM,
            "Creates a new VM." },
        { ACTION_NEW_PROJECT,
            "Creates a new project using a template." },
        { ACTION_UPDATE_PROJECT,
            "Updates a new project from existing source (must have an AndroidManifest.xml)." },
        };
    
    public SdkCommandLine() {
        super(ACTIONS);

        define(MODE.ENUM, false, ACTION_LIST, "f", KEY_FILTER,
                "List filter", new String[] { ARG_ALL, ARG_TARGET, ARG_VM });

        define(MODE.STRING, false, ACTION_NEW_VM, "o", KEY_OUT,
                "Location path of new VM", null);
        define(MODE.STRING, true, ACTION_NEW_VM, "n", KEY_NAME,
                "Name of the new VM", null);
        define(MODE.INTEGER, true, ACTION_NEW_VM, "t", KEY_TARGET_ID,
                "Target id of the new VM", null);

        define(MODE.ENUM, true, ACTION_NEW_PROJECT, "m", KEY_MODE,
                "Project mode", new String[] { ARG_ACTIVITY, ARG_ALIAS });
        define(MODE.STRING, false, ACTION_NEW_PROJECT, "o", KEY_OUT,
                "Location path of new project", null);
        define(MODE.STRING, true, ACTION_NEW_PROJECT, "n", KEY_NAME,
                "Name of the new project", null);
        define(MODE.INTEGER, true, ACTION_NEW_PROJECT, "t", KEY_TARGET_ID,
                "Target id of the new project", null);
        define(MODE.STRING, true, ACTION_NEW_PROJECT, "p", KEY_PACKAGE,
                "Package name", null);
        define(MODE.STRING, true, ACTION_NEW_PROJECT, "a", KEY_ACTIVITY,
                "Activity name", null);

        define(MODE.STRING, false, ACTION_UPDATE_PROJECT, "i", KEY_IN,
                "Directory location of the project", null);
        define(MODE.STRING, true, ACTION_UPDATE_PROJECT, "t", KEY_TARGET_ID,
                "Target id to set for the project", null);
    }
    
    // -- some helpers for list action flags
    
    /** Helper to retrieve the --filter for the list action. */
    public String getListFilter() {
        return ((String) getValue(ACTION_LIST, KEY_FILTER));
    }

    // -- some helpers for vm action flags
    
    /** Helper to retrieve the --out location for the new vm action. */
    public String getNewVmLocation() {
        return ((String) getValue(ACTION_NEW_VM, KEY_OUT));
    }
    
    /** Helper to retrieve the --target id for the new vm action. */
    public int getNewVmTargetId() {
        return ((Integer) getValue(ACTION_NEW_VM, KEY_TARGET_ID)).intValue();
    }

    /** Helper to retrieve the --name for the new vm action. */
    public String getNewVmName() {
        return ((String) getValue(ACTION_NEW_VM, KEY_NAME));
    }

    // -- some helpers for project action flags
    
    /** Helper to retrieve the --out location for the new project action. */
    public String getNewProjectLocation() {
        return ((String) getValue(ACTION_NEW_PROJECT, KEY_OUT));
    }
    
    /** Helper to retrieve the --target id for the new project action. */
    public int getNewProjectTargetId() {
        return ((Integer) getValue(ACTION_NEW_PROJECT, KEY_TARGET_ID)).intValue();
    }

    /** Helper to retrieve the --name for the new project action. */
    public String getNewProjectName() {
        return ((String) getValue(ACTION_NEW_PROJECT, KEY_NAME));
    }

    /** Helper to retrieve the --package for the new project action. */
    public String getNewProjectPackage() {
        return ((String) getValue(ACTION_NEW_PROJECT, KEY_PACKAGE));
    }

    /** Helper to retrieve the --activity for the new project action. */
    public String getNewProjectActivity() {
        return ((String) getValue(ACTION_NEW_PROJECT, KEY_ACTIVITY));
    }

    // -- some helpers for update action flags
    
    /** Helper to retrieve the --out location for the update project action. */
    public String getUpdateProjectLocation() {
        return ((String) getValue(ACTION_UPDATE_PROJECT, KEY_OUT));
    }
    
    /** Helper to retrieve the --target id for the update project action. */
    public int getUpdateProjectTargetId() {
        return ((Integer) getValue(ACTION_UPDATE_PROJECT, KEY_TARGET_ID)).intValue();
    }
}
