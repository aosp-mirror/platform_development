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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

/**
 * Represents the different intent flags related to activities and tasks.
 */
enum IntentFlag {
    SINGLE_TOP ("FLAG_ACTIVITY_SINGLE_TOP", emptySet(), emptySet(), emptySet()),
    BROUGHT_TO_FRONT ("FLAG_ACTIVITY_BROUGHT_TO_FRONT", emptySet(), emptySet(), emptySet()),
    NEW_TASK ("FLAG_ACTIVITY_NEW_TASK", emptySet(), emptySet(), emptySet()),
    CLEAR_TASK ("FLAG_ACTIVITY_CLEAR_TASK", emptySet(), emptySet(), setOf(NEW_TASK)),
    CLEAR_TOP ("FLAG_ACTIVITY_CLEAR_TOP", setOf(SINGLE_TOP, NEW_TASK), emptySet(), emptySet()),
    MATCH_EXTERNAL("FLAG_ACTIVITY_MATCH_EXTERNAL", emptySet(), emptySet(), emptySet()),
    MULTIPLE_TASK ("FLAG_ACTIVITY_MULTIPLE_TASK", emptySet(), emptySet(), setOf(NEW_TASK)),
    NEW_DOCUMENT ("FLAG_ACTIVITY_NEW_DOCUMENT", setOf(MULTIPLE_TASK), emptySet(),
            emptySet()),
    RETAIN_IN_RECENTS ("FLAG_ACTIVITY_RETAIN_IN_RECENTS", setOf(NEW_DOCUMENT),
            emptySet(), emptySet()),
    CLEAR_WHEN_TASK_RESET ("FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET", emptySet(), emptySet(),
            emptySet()),
    EXCLUDE_FROM_RECENTS ("FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS", emptySet(), setOf(RETAIN_IN_RECENTS),
            emptySet()),
    FORWARD_RESULT ("FLAG_ACTIVITY_FORWARD_RESULT", emptySet(), emptySet(), emptySet()),
    LAUNCHED_FROM_HISTORY ("FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY", emptySet(), emptySet(),
            emptySet()),
    LAUNCH_ADJACENT ("FLAG_ACTIVITY_LAUNCH_ADJACENT", setOf(MULTIPLE_TASK), emptySet(),
            setOf(NEW_TASK)),
    NO_ANIMATION ("FLAG_ACTIVITY_NO_ANIMATION", emptySet(), emptySet(), emptySet()),
    NO_HISTORY ("FLAG_ACTIVITY_NO_HISTORY", emptySet(), emptySet(), emptySet()),
    NO_USER_ACTION ("FLAG_ACTIVITY_NO_USER_ACTION", emptySet(), emptySet(), emptySet()),
    PREVIOUS_IS_TOP ("FLAG_ACTIVITY_PREVIOUS_IS_TOP", emptySet(), emptySet(), emptySet()),
    REORDER_TO_FRONT ("FLAG_ACTIVITY_REORDER_TO_FRONT", emptySet(), setOf(CLEAR_TOP), emptySet()),
    RESET_TASK_IF_NEEDED ("FLAG_ACTIVITY_RESET_TASK_IF_NEEDED", emptySet(), emptySet(), emptySet()),
    TASK_ON_HOME ("FLAG_ACTIVITY_TASK_ON_HOME", emptySet(), emptySet(), setOf(NEW_TASK));

    public String name;
    private Set<IntentFlag> mComplements = new HashSet<>();
    private Set<IntentFlag> mConflicts = new HashSet<>();
    private Set<IntentFlag> mRequests = new HashSet<>();

    IntentFlag(String name, Set<IntentFlag> complements, Set<IntentFlag> conflicts,
               Set<IntentFlag> requests) {
        this.name = name;
        this.mComplements = complements;
        this.mConflicts = conflicts;
        this.mRequests = requests;
    }

    /**
     * @return A set of flags that complement the action of this flag.
     */
    public Set<IntentFlag> getComplements() {
        return mComplements;
    }

    /**
     * @return A set of flags that conflict with the action of this flag.
     */
    public Set<IntentFlag> getConflicts() {
        return mConflicts;
    }

    /**
     * @return A set of flags that are necessary for the action of this flag.
     */
    public Set<IntentFlag> getRequests() {
        return mRequests;
    }

    public String getName() {
        return name;
    }

    /**
     * Convenience method to create a set of intent flags.
     */
    protected static Set<IntentFlag> setOf(IntentFlag... flags) {
        return Arrays.stream(flags).collect(Collectors.toSet());
    }
}

