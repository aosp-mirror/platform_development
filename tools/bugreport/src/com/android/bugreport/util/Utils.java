/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.bugreport.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;

/**
 * Random collection of stuff. Mostly for parsing.
 */
public class Utils {
    /**
     * UTC Time Zone.
     */
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    
    /**
     * Regex for a date/time, without milliseconds.
     */
    public static final String DATE_TIME_MS_PATTERN
            = "(?:(\\d\\d\\d\\d)-)?(\\d\\d)-(\\d\\d)\\s+(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)";

    /**
     * Regex for a date/time, without milliseconds.
     */
    public static final String DATE_TIME_PATTERN
            = "(?:(\\d\\d\\d\\d)-)?(\\d\\d)-(\\d\\d)\\s+(\\d\\d):(\\d\\d):(\\d\\d)";

    /**
     * Returns whether text matches the matcher.  The matcher can be used afterwards
     * to get the groups.
     */
    public static boolean matches(Matcher matcher, String text) {
        matcher.reset(text);
        return matcher.matches();
    }

    /**
     * Returns the matcher if it matches the text, null otherwise.
     */
    public static Matcher match(Matcher matcher, String text) {
        matcher.reset(text);
        if (matcher.matches()) {
            return matcher;
        } else {
            return null;
        }
    }

    /**
     * Gets a group from the matcher, and if it was set returns that
     * value as an int.  Otherwise returns def.
     */
    public static int getInt(Matcher matcher, int group, int def) {
        final String str = matcher.group(group);
        if (str == null || str.length() == 0) {
            return def;
        } else {
            return Integer.parseInt(str);
        }
    }
    
    /**
     * Gets the date time groups from the matcher and returns a GregorianCalendar.
     * The year is optional.
     * 
     * @param Matcher a matcher
     * @param startGroup the index of the first group to use
     * @param milliseconds whether to expect the millisecond group.
     *
     * @see #DATE_TIME_MS_PATTERN
     * @see #DATE_TIME_PATTERN
     */
    public static GregorianCalendar parseCalendar(Matcher matcher, int startGroup,
            boolean milliseconds) {
        final GregorianCalendar result = new GregorianCalendar(UTC);

        if (matcher.group(startGroup+0) != null) {
            result.set(Calendar.YEAR, Integer.parseInt(matcher.group(startGroup + 0)));
        }
        result.set(Calendar.MONTH, Integer.parseInt(matcher.group(startGroup + 1)));
        result.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(startGroup + 2)));
        result.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(startGroup + 3)));
        result.set(Calendar.MINUTE, Integer.parseInt(matcher.group(startGroup + 4)));
        result.set(Calendar.SECOND, Integer.parseInt(matcher.group(startGroup + 5)));
        if (milliseconds) {
            result.set(Calendar.MILLISECOND, Integer.parseInt(matcher.group(startGroup + 6)));
        }

        return result;
    }
}
