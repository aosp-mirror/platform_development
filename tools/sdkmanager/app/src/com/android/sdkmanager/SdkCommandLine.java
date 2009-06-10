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
    public final static String VERB_MOVE   = "move";
    public final static String VERB_DELETE = "delete";
    public final static String VERB_UPDATE = "update";

    public static final String OBJECT_AVD      = "avd";
    public static final String OBJECT_AVDS     = "avds";
    public static final String OBJECT_TARGET   = "target";
    public static final String OBJECT_TARGETS  = "targets";
    public static final String OBJECT_PROJECT  = "project";
    public static final String OBJECT_ADB      = "adb";

    public static final String ARG_ALIAS       = "alias";
    public static final String ARG_ACTIVITY    = "activity";

    public static final String KEY_ACTIVITY    = ARG_ACTIVITY;
    public static final String KEY_PACKAGE     = "package";
    public static final String KEY_MODE        = "mode";
    public static final String KEY_TARGET_ID   = OBJECT_TARGET;
    public static final String KEY_NAME        = "name";
    public static final String KEY_PATH        = "path";
    public static final String KEY_FILTER      = "filter";
    public static final String KEY_SKIN        = "skin";
    public static final String KEY_SDCARD      = "sdcard";
    public static final String KEY_FORCE       = "force";
    public static final String KEY_RENAME      = "rename";
    public static final String KEY_SUBPROJECTS = "subprojects";

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
            { VERB_LIST, NO_VERB_OBJECT,
                "Lists existing targets or virtual devices." },
            { VERB_LIST, OBJECT_AVD,
                "Lists existing Android Virtual Devices.",
                OBJECT_AVDS },
            { VERB_LIST, OBJECT_TARGET,
                "Lists existing targets.",
                OBJECT_TARGETS },

            { VERB_CREATE, OBJECT_AVD,
                "Creates a new Android Virtual Device." },
            { VERB_MOVE, OBJECT_AVD,
                "Moves or renames an Android Virtual Device." },
            { VERB_DELETE, OBJECT_AVD,
                "Deletes an Android Virtual Device." },
            { VERB_UPDATE, OBJECT_AVD,
                "Updates an Android Virtual Device to match the folders of a new SDK." },

            { VERB_CREATE, OBJECT_PROJECT,
                "Creates a new Android Project." },
            { VERB_UPDATE, OBJECT_PROJECT,
                "Updates an Android Project (must have an AndroidManifest.xml)." },

            { VERB_UPDATE, OBJECT_ADB,
                "Updates adb to support the USB devices declared in the SDK add-ons." },
        };

    public SdkCommandLine(ISdkLog logger) {
        super(logger, ACTIONS);

        // --- create avd ---

        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_AVD, "p", KEY_PATH,
                "Location path of the directory where the new AVD will be created", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_AVD, "n", KEY_NAME,
                "Name of the new AVD", null);
        define(Mode.INTEGER, true,
                VERB_CREATE, OBJECT_AVD, "t", KEY_TARGET_ID,
                "Target id of the new AVD", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_AVD, "s", KEY_SKIN,
                "Skin of the new AVD", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_AVD, "c", KEY_SDCARD,
                "Path to a shared SD card image, or size of a new sdcard for the new AVD", null);
        define(Mode.BOOLEAN, false,
                VERB_CREATE, OBJECT_AVD, "f", KEY_FORCE,
                "Force creation (override an existing AVD)", false);

        // --- delete avd ---

        define(Mode.STRING, true,
                VERB_DELETE, OBJECT_AVD, "n", KEY_NAME,
                "Name of the AVD to delete", null);

        // --- move avd ---

        define(Mode.STRING, true,
                VERB_MOVE, OBJECT_AVD, "n", KEY_NAME,
                "Name of the AVD to move or rename", null);
        define(Mode.STRING, false,
                VERB_MOVE, OBJECT_AVD, "r", KEY_RENAME,
                "New name of the AVD to rename", null);
        define(Mode.STRING, false,
                VERB_MOVE, OBJECT_AVD, "p", KEY_PATH,
                "New location path of the directory where to move the AVD", null);

        // --- update avd ---

        define(Mode.STRING, true,
                VERB_UPDATE, OBJECT_AVD, "n", KEY_NAME,
                "Name of the AVD to update", null);

        // --- create project ---

        /* Disabled for ADT 0.9 / Cupcake SDK 1.5_r1 release. [bug #1795718].
           This currently does not work, the alias build rules need to be fixed.

        define(Mode.ENUM, true,
                VERB_CREATE, OBJECT_PROJECT, "m", KEY_MODE,
                "Project mode", new String[] { ARG_ACTIVITY, ARG_ALIAS });
        */
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_PROJECT,
                "p", KEY_PATH,
                "Location path of new project", null);
        define(Mode.INTEGER, true,
                VERB_CREATE, OBJECT_PROJECT, "t", KEY_TARGET_ID,
                "Target id of the new project", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_PROJECT, "k", KEY_PACKAGE,
                "Package name", null);
        define(Mode.STRING, true,
                VERB_CREATE, OBJECT_PROJECT, "a", KEY_ACTIVITY,
                "Activity name", null);
        define(Mode.STRING, false,
                VERB_CREATE, OBJECT_PROJECT, "n", KEY_NAME,
                "Project name", null);

        // --- update project ---

        define(Mode.STRING, true,
                VERB_UPDATE, OBJECT_PROJECT,
                "p", KEY_PATH,
                "Location path of the project", null);
        define(Mode.INTEGER, true,
                VERB_UPDATE, OBJECT_PROJECT,
                "t", KEY_TARGET_ID,
                "Target id to set for the project", -1);
        define(Mode.STRING, false,
                VERB_UPDATE, OBJECT_PROJECT,
                "n", KEY_NAME,
                "Project name", null);
        define(Mode.BOOLEAN, false,
                VERB_UPDATE, OBJECT_PROJECT,
                "s", KEY_SUBPROJECTS,
                "Also update any projects in sub-folders, such as test projects.", false);
    }

    @Override
    public boolean acceptLackOfVerb() {
        return true;
    }

    // -- some helpers for generic action flags

    /** Helper to retrieve the --path value. */
    public String getParamLocationPath() {
        return ((String) getValue(null, null, KEY_PATH));
    }

    /** Helper to retrieve the --target id value. */
    public int getParamTargetId() {
        return ((Integer) getValue(null, null, KEY_TARGET_ID)).intValue();
    }

    /** Helper to retrieve the --name value. */
    public String getParamName() {
        return ((String) getValue(null, null, KEY_NAME));
    }

    /** Helper to retrieve the --skin value. */
    public String getParamSkin() {
        return ((String) getValue(null, null, KEY_SKIN));
    }

    /** Helper to retrieve the --sdcard value. */
    public String getParamSdCard() {
        return ((String) getValue(null, null, KEY_SDCARD));
    }

    /** Helper to retrieve the --force flag. */
    public boolean getFlagForce() {
        return ((Boolean) getValue(null, null, KEY_FORCE)).booleanValue();
    }

    // -- some helpers for avd action flags

    /** Helper to retrieve the --rename value for a move verb. */
    public String getParamMoveNewName() {
        return ((String) getValue(VERB_MOVE, null, KEY_RENAME));
    }


    // -- some helpers for project action flags

    /** Helper to retrieve the --package value. */
    public String getParamProjectPackage() {
        return ((String) getValue(null, OBJECT_PROJECT, KEY_PACKAGE));
    }

    /** Helper to retrieve the --activity for any project action. */
    public String getParamProjectActivity() {
        return ((String) getValue(null, OBJECT_PROJECT, KEY_ACTIVITY));
    }

    /** Helper to retrieve the --subprojects for any project action. */
    public boolean getParamSubProject() {
        return ((Boolean) getValue(null, OBJECT_PROJECT, KEY_SUBPROJECTS)).booleanValue();
    }
}
