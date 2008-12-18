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

import java.util.regex.Pattern;
import java.util.List;

/**
 * Decides whether or not to exclude certain paths.
 */
public class Excludes {

    private final List<Pattern> patterns;

    /**
     * Constructs a set of excludes matching the given patterns.
     */
    public Excludes(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    /**
     * Returns true if the given path should be excluded.
     */
    public boolean exclude(String path) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(path).find()) {
                return true;
            }
        }
        return false;
    }
}
