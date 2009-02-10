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

import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkManager;


/**
 * Specific command-line flags for the {@link SdkManager}.
 */
public class SdkCommandLine extends CommandLineProcessor {

    public final static String VERB_LIST   = "list";
    public final static String VERB_CREATE = "create";
    public final static String VERB_RENAME = "rename";
    public final static String VERB_MOVE   = "move";
    public final static String VERB_DELETE = "delete";
    public final static String VERB_UPDATE = "update";

    public static final String OBJECT_AVD      = "avd";
    public static final String OBJECT_AVDS     = "avds";
    public static final String OBJECT_TARGET   = "target";
    public static final String OBJECT_TARGETS  = "targets";
    public static final String OBJECT_PROJECT  = "project";

    public static final String ARG_ALIAS    = "alias";
    public static final String ARG_ACTIVITY = "activity";

    public static final String KEY_ACTIVITY  = ARG_ACTIVITY;
    public static final String KEY_PACKAGE   = "package";
    public static final String KEY_MODE      = "mode";
    public static final String KEY_TARGET_ID = OBJECT_TARGET;
    public static final String KEY_NAME      = "name";
    public static final String KEY_PATH      = "path";
    public static final String KEY_FILTER    = "filter";
    public static final String KEY_SKIN      = "skin";
    public static final String KEY_SDCARD    = "sdcard";
    public static final String KEY_FORCE     = "force";

    /**
     * Action definitions for SdkManager command line.
     * <p/>
     * Each entry is a string array with:
     * <ul>
     * <li> the verb.
     * <li> an object (use #NO_VERB_OBJECT if there's no object).
     * <li> a description.
     * <li> an alternate form for the object (e.g. plural).
     * </ul>
     */
    private final static String[][] ACTIONS = {
            { VERB_LIST,
                NO_VERB_OBJECT,
                "Lists existing targets or virtual devices." },
            { VERB_LIST,
                OBJECT_AVD,
                "Lists existing Android Virtual Devices.",
                OBJECT_AVDS },
            { VERB_LIST,
                OBJECT_TARGET,
                "Lists existing targets.",
                OBJECT_TARGETS },
    
            { VERB_CREATE,
                OBJECT_AVD,
                "Creates a new Android Virtual Device." },
            { VERB_RENAME,
                OBJECT_AVD,
                "Renames a new Android Virtual Device." },
            { VERB_MOVE,
                OBJECT_AVD,
                "Moves a new Android Virtual Device." },
            { VERB_DELETE,
                OBJECT_AVD,
                "Deletes a new Android Virtual Device." },
    
            { VERB_CREATE,
                OBJECT_PROJECT,
                "Creates a new Android Project." },
            { VERB_UPDATE,
                OBJECT_PROJECT,
                "Updates an Android Project (must have an AndroidManifest.xml)." },
        };
    
    public SdkCommandLine(ISdkLog logger) {
        super(logger, ACTIONS);

        define(MODE.STRING, false, 
                VERB_CREATE, OBJECT_AVD,
                "p", KEY_PATH,
                "Location path of the parent directory where the new AVD will be created", null);
        define(MODE.STRING, true, 
                VERB_CREATE, OBJECT_AVD,
                "n", KEY_NAME,
                "Name of the new AVD", null);
        define(MODE.INTEGER, true, 
                VERB_CREATE, OBJECT_AVD,
                "t", KEY_TARGET_ID,
                "Target id of the new AVD", null);
        define(MODE.STRING, true, 
                VERB_CREATE, OBJECT_AVD,
                "s", KEY_SKIN,
                "Skin of the new AVD", null);
        define(MODE.STRING, false, 
                VERB_CREATE, OBJECT_AVD,
                "c", KEY_SDCARD,
                "Path to a shared SD card image, or size of a new sdcard for the new AVD", null);
        define(MODE.BOOLEAN, false, 
                VERB_CREATE, OBJECT_AVD,
                "f", KEY_FORCE,
                "Force creation (override an existing AVD)", false);

        define(MODE.ENUM, true, 
                VERB_CREATE, OBJECT_PROJECT,
                "m", KEY_MODE,
                "Project mode", new String[] { ARG_ACTIVITY, ARG_ALIAS });
        define(MODE.STRING, true, 
                VERB_CREATE, OBJECT_PROJECT,
                "p", KEY_PATH,
                "Location path of new project", null);
        define(MODE.INTEGER, true, 
                VERB_CREATE, OBJECT_PROJECT,
                "t", KEY_TARGET_ID,
                "Target id of the new project", null);
        define(MODE.STRING, true, 
                VERB_CREATE, OBJECT_PROJECT,
                "k", KEY_PACKAGE,
                "Package name", null);
        define(MODE.STRING, true, 
                VERB_CREATE, OBJECT_PROJECT,
                "a", KEY_ACTIVITY,
                "Activity name", null);
        define(MODE.STRING, false, 
                VERB_CREATE, OBJECT_PROJECT,
                "n", KEY_NAME,
                "Project name", null);

        define(MODE.STRING, true, 
                VERB_UPDATE, OBJECT_PROJECT,
                "p", KEY_PATH,
                "Location path of the project", null);
        define(MODE.INTEGER, true, 
                VERB_UPDATE, OBJECT_PROJECT,
                "t", KEY_TARGET_ID,
                "Target id to set for the project", -1);
        define(MODE.STRING, false, 
                VERB_UPDATE, OBJECT_PROJECT,
                "n", KEY_NAME,
                "Project name", null);
    }
    
    // -- some helpers for AVD action flags
    
    /** Helper to retrieve the --out location for the new AVD action. */
    public String getCreateAvdLocation() {
        return ((String) getValue(VERB_CREATE, OBJECT_AVD, KEY_PATH));
    }
    
    /** Helper to retrieve the --target id for the new AVD action. */
    public int getCreateAvdTargetId() {
        return ((Integer) getValue(VERB_CREATE, OBJECT_AVD, KEY_TARGET_ID)).intValue();
    }

    /** Helper to retrieve the --name for the new AVD action. */
    public String getCreateAvdName() {
        return ((String) getValue(VERB_CREATE, OBJECT_AVD, KEY_NAME));
    }
    
    /** Helper to retrieve the --skin name for the new AVD action. */
    public String getCreateAvdSkin() {
        return ((String) getValue(VERB_CREATE, OBJECT_AVD, KEY_SKIN));
    }

    /** Helper to retrieve the --sdcard data for the new AVD action. */
    public String getCreateAvdSdCard() {
        return ((String) getValue(VERB_CREATE, OBJECT_AVD, KEY_SDCARD));
    }
    
    public boolean getCreateAvdForce() {
        return ((Boolean) getValue(VERB_CREATE, OBJECT_AVD, KEY_FORCE)).booleanValue();
    }


    // -- some helpers for project action flags
    
    /** Helper to retrieve the --out location for the new project action. */
    public String getCreateProjectLocation() {
        return ((String) getValue(VERB_CREATE, OBJECT_PROJECT, KEY_PATH));
    }
    
    /** Helper to retrieve the --target id for the new project action. */
    public int getCreateProjectTargetId() {
        return ((Integer) getValue(VERB_CREATE, OBJECT_PROJECT, KEY_TARGET_ID)).intValue();
    }

    /** Helper to retrieve the --name for the new project action. */
    public String getCreateProjectName() {
        return ((String) getValue(VERB_CREATE, OBJECT_PROJECT, KEY_NAME));
    }

    /** Helper to retrieve the --package for the new project action. */
    public String getCreateProjectPackage() {
        return ((String) getValue(VERB_CREATE, OBJECT_PROJECT, KEY_PACKAGE));
    }

    /** Helper to retrieve the --activity for the new project action. */
    public String getCreateProjectActivity() {
        return ((String) getValue(VERB_CREATE, OBJECT_PROJECT, KEY_ACTIVITY));
    }

    // -- some helpers for update action flags
    
    /** Helper to retrieve the --out location for the update project action. */
    public String getUpdateProjectLocation() {
        return ((String) getValue(VERB_UPDATE, OBJECT_PROJECT, KEY_PATH));
    }
    
    /** Helper to retrieve the --target id for the update project action. */
    public int getUpdateProjectTargetId() {
        return ((Integer) getValue(VERB_UPDATE, OBJECT_PROJECT, KEY_TARGET_ID)).intValue();
    }

    /** Helper to retrieve the --name for the update project action. */
    public String getUpdateProjectName() {
        return ((String) getValue(VERB_UPDATE, OBJECT_PROJECT, KEY_NAME));
    }
}
