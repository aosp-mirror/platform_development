/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.intentplayground;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Static utility functions to query intent and activity manifest flags.
 */
class FlagUtils {
    private static Class<Intent> sIntentClass = Intent.class;
    private static List<ActivityInfo> sActivityInfos = null;
    private static Intent sIntent = new Intent();
    static final String INTENT_FLAG_PREFIX = "FLAG_ACTIVITY";
    private static final String ACTIVITY_INFO_FLAG_PREFIX = "FLAG";
    
    /**
     * Returns a String list of flags active on this intent.
     * @param intent The intent on which to query flags.
     * @return A list of flags active on this intent.
     */
    public static List<String> discoverFlags(Intent intent) {
        int flags = intent.getFlags();
        return Arrays.stream(intent.getClass().getDeclaredFields()) // iterate over Intent members
                .filter(f -> f.getName().startsWith(INTENT_FLAG_PREFIX)) // filter FLAG_ fields
                .filter(f -> {
                    try {
                        return (flags & f.getInt(intent)) > 0;
                    } catch (IllegalAccessException e) {
                        // Should never happen, the fields we are reading are public
                        throw new RuntimeException(e);
                    }
                })  // filter fields that are present in intent
                .map(Field::getName) // map present Fields to their string names
                .collect(Collectors.toList());
    }

    /**
     * Returns a full list of flags available to be set on an intent.
     * @return A string list of all intent flags.
     */
    public static List<String> getIntentFlagsAsString() {
        return Arrays.stream(sIntentClass.getDeclaredFields())
                .filter(f -> f.getName().startsWith(INTENT_FLAG_PREFIX))
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get all defined {@link IntentFlag}s.
     * @return All defined IntentFlags.
     */
    public static List<IntentFlag> getAllIntentFlags() {
        return Arrays.asList(IntentFlag.values());
    }

    /**
     * Get intent flags by category/
     * @return List of string flags (value) organized by category/function (key).
     */
    public static Map<String, List<String>> intentFlagsByCategory() {
        Map<String, List<String>> categories = new HashMap<>();
        List<String> allFlags = getIntentFlagsAsString();
        List<String> nonUser = new LinkedList<>();
        List<String> recentsAndUi = new LinkedList<>();
        List<String> newTask = new LinkedList<>();
        List<String> clearTask = new LinkedList<>();
        List<String> rearrangeTask = new LinkedList<>();
        List<String> other = new LinkedList<>();
        allFlags.forEach(flag -> {
            if (hasSuffix(flag, "BROUGHT_TO_FRONT", "LAUNCHED_FROM_HISTORY")) {
                nonUser.add(flag);
            } else if (hasSuffix(flag, "RECENTS", "LAUNCH_ADJACENT", "NO_ANIMATION", "NO_HISTORY",
                    "RETAIN_IN_RECENTS")) {
                recentsAndUi.add(flag);
            } else if (hasSuffix(flag, "MULTIPLE_TASK", "NEW_TASK", "NEW_DOCUMENT",
                    "RESET_TASK_IF_NEEDED")) {
                newTask.add(flag);
            } else if (hasSuffix(flag, "CLEAR_TASK", "CLEAR_TOP", "CLEAR_WHEN_TASK_RESET")) {
                clearTask.add(flag);
            } else if (hasSuffix(flag, "REORDER_TO_FRONT", "SINGLE_TOP", "TASK_ON_HOME")) {
                rearrangeTask.add(flag);
            } else {
                other.add(flag);
            }
        });
        categories.put("Non-user", nonUser);
        categories.put("Recents and UI", recentsAndUi);
        categories.put("New Task", newTask);
        categories.put("Clear Task", clearTask);
        categories.put("Rearrange Task", rearrangeTask);
        categories.put("Other", other);
        return categories;
    }

    /**
     * Checks the target string for any of the listed suffixes.
     * @param target The string to test for suffixes.
     * @param suffixes The suffixes to test the string for.
     * @return True if the target string has any of the suffixes, false if not.
     */
    private static boolean hasSuffix(String target, String... suffixes) {
        for (String suffix: suffixes) {
            if (target.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the integer value of an intent flag.
     * @param flagName The field name of the flag.
     */
    public static int flagValue(String flagName)  {
        try {
            return sIntentClass.getField(flagName).getInt(sIntent);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Checks if the passed intent has the specified flag.
     * @param intent The intent of which to examine the flags.
     * @param flagName The string name of the intent flag to check for.
     * @return True if the flag is present, false if not.
     */
    public static boolean hasIntentFlag(Intent intent, String flagName) {
        return (intent.getFlags() & flagValue(flagName)) > 0;
    }

    /**
     * Checks if the passed intent has the specified flag.
     * @param intent The intent of which to examine the flags.
     * @param flag The corresponding enum {@link IntentFlag} of the intent flag to check for.
     * @return True if the flag is present, false if not.
     */
    public static boolean hasIntentFlag(Intent intent, IntentFlag flag) {
        return hasIntentFlag(intent, flag.getName());
    }

    /**
     * Checks if the passed activity has the specified flag set in its manifest.
     * @param context A context from this application (used to access {@link PackageManager}.
     * @param activity The activity of which to examine the flags.
     * @param flag The corresponding enum {@link ActivityFlag} of the activity flag to check for.
     * @return True if the flag is present, false if not.
     */
    public static boolean hasActivityFlag(Context context, ComponentName activity,
                                          ActivityFlag flag) {
        return getActivityFlags(context, activity).contains(flag);
    }

    /**
     * Returns an {@link EnumSet} of {@link ActivityFlag} corresponding to activity manifest flags
     * activity on the specified activity.
     * @param context A context from this application (used to access {@link PackageManager}.
     * @param activity The activity of which to examine the flags.
     * @return  A set of ActivityFlags corresponding to activity manifest flags.
     */
    public static EnumSet<ActivityFlag> getActivityFlags(Context context, ComponentName activity) {
        loadActivityInfo(context);
        EnumSet<ActivityFlag> flags = EnumSet.noneOf(ActivityFlag.class);
        Optional<ActivityInfo> infoOptional = sActivityInfos.stream()
                .filter(i-> i.name.equals(activity.getClassName()))
                .findFirst();
        if (!infoOptional.isPresent()) {
            return flags;
        }
        ActivityInfo info = infoOptional.get();
        if ((info.flags & ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH) > 0) {
            flags.add(ActivityFlag.CLEAR_TASK_ON_LAUNCH);
        }
        if ((info.flags & ActivityInfo.FLAG_ALLOW_TASK_REPARENTING) > 0) {
            flags.add(ActivityFlag.ALLOW_TASK_REPARENTING);
        }
        switch (info.launchMode) {
            case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                flags.add(ActivityFlag.LAUNCH_MODE_SINGLE_INSTANCE);
                break;
            case ActivityInfo.LAUNCH_SINGLE_TASK:
                flags.add(ActivityFlag.LAUNCH_MODE_SINGLE_TASK);
                break;
            case ActivityInfo.LAUNCH_SINGLE_TOP:
                flags.add(ActivityFlag.LAUNCH_MODE_SINGLE_TOP);
                break;
            case ActivityInfo.LAUNCH_MULTIPLE:
            default:
                flags.add(ActivityFlag.LAUNCH_MODE_STANDARD);
                break;
        }
        switch(info.documentLaunchMode) {
            case ActivityInfo.DOCUMENT_LAUNCH_INTO_EXISTING:
                flags.add(ActivityFlag.DOCUMENT_LAUNCH_MODE_INTO_EXISTING);
                break;
            case ActivityInfo.DOCUMENT_LAUNCH_ALWAYS:
                flags.add(ActivityFlag.DOCUMENT_LAUNCH_MODE_ALWAYS);
                break;
            case ActivityInfo.DOCUMENT_LAUNCH_NEVER:
                flags.add(ActivityFlag.DOCUMENT_LAUNCH_MODE_NEVER);
                break;
            case ActivityInfo.DOCUMENT_LAUNCH_NONE:
            default:
                flags.add(ActivityFlag.DOCUMENT_LAUNCH_MODE_NONE);
                break;
        }
        return flags;
    }

    private static void loadActivityInfo(Context context) {
        if (sActivityInfos == null) {
            PackageInfo packInfo;

            // Retrieve activities and their manifest flags
            PackageManager pm = context.getPackageManager();
            try {
                packInfo = pm.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            sActivityInfos = Arrays.asList(packInfo.activities);
        }
    }

    /**
     * Discover which flags on the specified {@link ActivityInfo} are enabled,
     * and return them as a list of strings.
     * @param activity The activity from which you want to find flags.
     * @return A list of flags.
     */
    public static List<String> getActivityFlags(ActivityInfo activity) {
        int flags = activity.flags;
        List<String> flagStrings = Arrays.stream(activity.getClass().getDeclaredFields())
                .filter(f -> f.getName().startsWith(ACTIVITY_INFO_FLAG_PREFIX))
                .filter(f -> {
                    try {
                        return (flags & f.getInt(activity)) > 0;
                    } catch (IllegalAccessException e) {
                        // Should never happen, the fields we are reading are public
                        throw new RuntimeException(e);
                    }
                })  // filter fields that are present in intent
                .map(Field::getName) // map present Fields to their string names
                .map(name -> camelify(name.substring(ACTIVITY_INFO_FLAG_PREFIX.length())))
                .map(s -> s.concat("=true"))
                .collect(Collectors.toList());
        // check for launchMode
        if (activity.launchMode != 0) {
            String lm = "launchMode=";
            switch(activity.launchMode) {
                case ActivityInfo.LAUNCH_SINGLE_INSTANCE:
                    lm += "singleInstance";
                    break;
                case ActivityInfo.LAUNCH_SINGLE_TASK:
                    lm += "singleTask";
                    break;
                case ActivityInfo.LAUNCH_SINGLE_TOP:
                    lm += "singleTop";
                    break;
                case ActivityInfo.LAUNCH_MULTIPLE:
                default:
                    lm += "standard";
                    break;
            }
            flagStrings.add(lm);
        }
        // check for documentLaunchMode
        if (activity.documentLaunchMode != 0) {
            String dlm = "documentLaunchMode=";
            switch(activity.documentLaunchMode) {
                case ActivityInfo.DOCUMENT_LAUNCH_INTO_EXISTING:
                    dlm += "intoExisting";
                    break;
                case ActivityInfo.DOCUMENT_LAUNCH_ALWAYS:
                    dlm += "always";
                    break;
                case ActivityInfo.DOCUMENT_LAUNCH_NEVER:
                    dlm += "never";
                    break;
                case ActivityInfo.DOCUMENT_LAUNCH_NONE:
                default:
                    dlm += "none";
                    break;
            }
            flagStrings.add(dlm);
        }
        if (activity.taskAffinity != null) {
            flagStrings.add("taskAffinity="+ activity.taskAffinity);
        }
        return flagStrings;
    }

    /**
     * Takes a snake_case and converts to CamelCase.
     * @param snake A snake_case string.
     * @return A camelified string.
     */
    public static String camelify(String snake) {
        List<String> words = Arrays.asList(snake.split("_"));
        StringBuilder output = new StringBuilder(words.get(0).toLowerCase());
        words.subList(1,words.size()).forEach(s -> {
            String first = s.substring(0,1).toUpperCase();
            String rest = s.substring(1).toLowerCase();
            output.append(first).append(rest);
        });
        return output.toString();
    }

    /**
     * Retrieves the corresponding enum {@link IntentFlag} for the string flag.
     * @param stringFlag the name of the intent flag.
     * @return The corresponding IntentFlag.
     */
    public static IntentFlag getFlagForString(String stringFlag) {
        return getAllIntentFlags().stream().filter(flag -> flag.getName().equals(stringFlag)).findAny()
                .orElse(null);
    }
}

