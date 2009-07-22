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

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Parses the command-line and stores flags needed or requested.
 * <p/>
 * This is a base class. To be useful you want to:
 * <ul>
 * <li>override it.
 * <li>pass an action array to the constructor.
 * <li>define flags for your actions.
 * </ul>
 * <p/>
 * To use, call {@link #parseArgs(String[])} and then
 * call {@link #getValue(String, String, String)}.
 */
public class CommandLineProcessor {

    /** Internal verb name for internally hidden flags. */
    public final static String GLOBAL_FLAG_VERB = "@@internal@@";

    /** String to use when the verb doesn't need any object. */
    public final static String NO_VERB_OBJECT = "";

    /** The global help flag. */
    public static final String KEY_HELP = "help";
    /** The global verbose flag. */
    public static final String KEY_VERBOSE = "verbose";
    /** The global silent flag. */
    public static final String KEY_SILENT = "silent";

    /** Verb requested by the user. Null if none specified, which will be an error. */
    private String mVerbRequested;
    /** Direct object requested by the user. Can be null. */
    private String mDirectObjectRequested;

    /**
     * Action definitions.
     * <p/>
     * Each entry is a string array with:
     * <ul>
     * <li> the verb.
     * <li> a direct object (use #NO_VERB_OBJECT if there's no object).
     * <li> a description.
     * <li> an alternate form for the object (e.g. plural).
     * </ul>
     */
    private final String[][] mActions;

    private static final int ACTION_VERB_INDEX = 0;
    private static final int ACTION_OBJECT_INDEX = 1;
    private static final int ACTION_DESC_INDEX = 2;
    private static final int ACTION_ALT_OBJECT_INDEX = 3;

    /**
     * The map of all defined arguments.
     * <p/>
     * The key is a string "verb/directObject/longName".
     */
    private final HashMap<String, Arg> mArguments = new HashMap<String, Arg>();
    /** Logger */
    private final ISdkLog mLog;

    public CommandLineProcessor(ISdkLog logger, String[][] actions) {
        mLog = logger;
        mActions = actions;

        define(Mode.BOOLEAN, false, GLOBAL_FLAG_VERB, NO_VERB_OBJECT, "v", KEY_VERBOSE,
                "Verbose mode: errors, warnings and informational messages are printed.",
                false);
        define(Mode.BOOLEAN, false, GLOBAL_FLAG_VERB, NO_VERB_OBJECT, "s", KEY_SILENT,
                "Silent mode: only errors are printed out.",
                false);
        define(Mode.BOOLEAN, false, GLOBAL_FLAG_VERB, NO_VERB_OBJECT, "h", KEY_HELP,
                "This help.",
                false);
    }

    /**
     * Indicates if this command-line can work when no verb is specified.
     * The default is false, which generates an error when no verb/object is specified.
     * Derived implementations can set this to true if they can deal with a lack
     * of verb/action.
     */
    public boolean acceptLackOfVerb() {
        return false;
    }


    //------------------
    // Helpers to get flags values

    /** Helper that returns true if --verbose was requested. */
    public boolean isVerbose() {
        return ((Boolean) getValue(GLOBAL_FLAG_VERB, NO_VERB_OBJECT, KEY_VERBOSE)).booleanValue();
    }

    /** Helper that returns true if --silent was requested. */
    public boolean isSilent() {
        return ((Boolean) getValue(GLOBAL_FLAG_VERB, NO_VERB_OBJECT, KEY_SILENT)).booleanValue();
    }

    /** Helper that returns true if --help was requested. */
    public boolean isHelpRequested() {
        return ((Boolean) getValue(GLOBAL_FLAG_VERB, NO_VERB_OBJECT, KEY_HELP)).booleanValue();
    }

    /** Returns the verb name from the command-line. Can be null. */
    public String getVerb() {
        return mVerbRequested;
    }

    /** Returns the direct object name from the command-line. Can be null. */
    public String getDirectObject() {
        return mDirectObjectRequested;
    }

    //------------------

    /**
     * Raw access to parsed parameter values.
     * <p/>
     * The default is to scan all parameters. Parameters that have been explicitly set on the
     * command line are returned first. Otherwise one with a non-null value is returned.
     * <p/>
     * Both a verb and a direct object filter can be specified. When they are non-null they limit
     * the scope of the search.
     * <p/>
     * If nothing has been found, return the last default value seen matching the filter.
     *
     * @param verb The verb name, including {@link #GLOBAL_FLAG_VERB}. If null, all possible
     *             verbs that match the direct object condition will be examined and the first
     *             value set will be used.
     * @param directObject The direct object name, including {@link #NO_VERB_OBJECT}. If null,
     *             all possible direct objects that match the verb condition will be examined and
     *             the first value set will be used.
     * @param longFlagName The long flag name for the given action. Mandatory. Cannot be null.
     * @return The current value object stored in the parameter, which depends on the argument mode.
     */
    public Object getValue(String verb, String directObject, String longFlagName) {

        if (verb != null && directObject != null) {
            String key = verb + "/" + directObject + "/" + longFlagName;
            Arg arg = mArguments.get(key);
            return arg.getCurrentValue();
        }

        Object lastDefault = null;
        for (Arg arg : mArguments.values()) {
            if (arg.getLongArg().equals(longFlagName)) {
                if (verb == null || arg.getVerb().equals(verb)) {
                    if (directObject == null || arg.getDirectObject().equals(directObject)) {
                        if (arg.isInCommandLine()) {
                            return arg.getCurrentValue();
                        }
                        if (arg.getCurrentValue() != null) {
                            lastDefault = arg.getCurrentValue();
                        }
                    }
                }
            }
        }

        return lastDefault;
    }

    /**
     * Internal setter for raw parameter value.
     * @param verb The verb name, including {@link #GLOBAL_FLAG_VERB}.
     * @param directObject The direct object name, including {@link #NO_VERB_OBJECT}.
     * @param longFlagName The long flag name for the given action.
     * @param value The new current value object stored in the parameter, which depends on the
     *              argument mode.
     */
    protected void setValue(String verb, String directObject, String longFlagName, Object value) {
        String key = verb + "/" + directObject + "/" + longFlagName;
        Arg arg = mArguments.get(key);
        arg.setCurrentValue(value);
    }

    /**
     * Parses the command-line arguments.
     * <p/>
     * This method will exit and not return if a parsing error arise.
     *
     * @param args The arguments typically received by a main method.
     */
    public void parseArgs(String[] args) {
        String needsHelp = null;
        String verb = null;
        String directObject = null;

        try {
            int n = args.length;
            for (int i = 0; i < n; i++) {
                Arg arg = null;
                String a = args[i];
                if (a.startsWith("--")) {
                    arg = findLongArg(verb, directObject, a.substring(2));
                } else if (a.startsWith("-")) {
                    arg = findShortArg(verb, directObject, a.substring(1));
                }

                // No matching argument name found
                if (arg == null) {
                    // Does it looks like a dashed parameter?
                    if (a.startsWith("-")) {
                        if (verb == null || directObject == null) {
                            // It looks like a dashed parameter and we don't have a a verb/object
                            // set yet, the parameter was just given too early.

                            needsHelp = String.format(
                                "Flag '%1$s' is not a valid global flag. Did you mean to specify it after the verb/object name?",
                                a);
                            return;
                        } else {
                            // It looks like a dashed parameter and but it is unknown by this
                            // verb-object combination

                            needsHelp = String.format(
                                    "Flag '%1$s' is not valid for '%2$s %3$s'.",
                                    a, verb, directObject);
                            return;
                        }
                    }

                    if (verb == null) {
                        // Fill verb first. Find it.
                        for (String[] actionDesc : mActions) {
                            if (actionDesc[ACTION_VERB_INDEX].equals(a)) {
                                verb = a;
                                break;
                            }
                        }

                        // Error if it was not a valid verb
                        if (verb == null) {
                            needsHelp = String.format(
                                "Expected verb after global parameters but found '%1$s' instead.",
                                a);
                            return;
                        }

                    } else if (directObject == null) {
                        // Then fill the direct object. Find it.
                        for (String[] actionDesc : mActions) {
                            if (actionDesc[ACTION_VERB_INDEX].equals(verb)) {
                                if (actionDesc[ACTION_OBJECT_INDEX].equals(a)) {
                                    directObject = a;
                                    break;
                                } else if (actionDesc.length > ACTION_ALT_OBJECT_INDEX &&
                                        actionDesc[ACTION_ALT_OBJECT_INDEX].equals(a)) {
                                    // if the alternate form exist and is used, we internally
                                    // only memorize the default direct object form.
                                    directObject = actionDesc[ACTION_OBJECT_INDEX];
                                    break;
                                }
                            }
                        }

                        // Error if it was not a valid object for that verb
                        if (directObject == null) {
                            needsHelp = String.format(
                                "Expected verb after global parameters but found '%1$s' instead.",
                                a);
                            return;

                        }
                    }
                } else if (arg != null) {
                    // This argument was present on the command line
                    arg.setInCommandLine(true);

                    // Process keyword
                    String error = null;
                    if (arg.getMode().needsExtra()) {
                        if (++i >= n) {
                            needsHelp = String.format("Missing argument for flag %1$s.", a);
                            return;
                        }

                        error = arg.getMode().process(arg, args[i]);
                    } else {
                        error = arg.getMode().process(arg, null);

                        // If we just toggled help, we want to exit now without printing any error.
                        // We do this test here only when a Boolean flag is toggled since booleans
                        // are the only flags that don't take parameters and help is a boolean.
                        if (isHelpRequested()) {
                            printHelpAndExit(null);
                            // The call above should terminate however in unit tests we override
                            // it so we still need to return here.
                            return;
                        }
                    }

                    if (error != null) {
                        needsHelp = String.format("Invalid usage for flag %1$s: %2$s.", a, error);
                        return;
                    }
                }
            }

            if (needsHelp == null) {
                if (verb == null && !acceptLackOfVerb()) {
                    needsHelp = "Missing verb name.";
                } else if (verb != null) {
                    if (directObject == null) {
                        // Make sure this verb has an optional direct object
                        for (String[] actionDesc : mActions) {
                            if (actionDesc[ACTION_VERB_INDEX].equals(verb) &&
                                    actionDesc[ACTION_OBJECT_INDEX].equals(NO_VERB_OBJECT)) {
                                directObject = NO_VERB_OBJECT;
                                break;
                            }
                        }

                        if (directObject == null) {
                            needsHelp = String.format("Missing object name for verb '%1$s'.", verb);
                            return;
                        }
                    }

                    // Validate that all mandatory arguments are non-null for this action
                    String missing = null;
                    boolean plural = false;
                    for (Entry<String, Arg> entry : mArguments.entrySet()) {
                        Arg arg = entry.getValue();
                        if (arg.getVerb().equals(verb) &&
                                arg.getDirectObject().equals(directObject)) {
                            if (arg.isMandatory() && arg.getCurrentValue() == null) {
                                if (missing == null) {
                                    missing = "--" + arg.getLongArg();
                                } else {
                                    missing += ", --" + arg.getLongArg();
                                    plural = true;
                                }
                            }
                        }
                    }

                    if (missing != null) {
                        needsHelp  = String.format(
                                "The %1$s %2$s must be defined for action '%3$s %4$s'",
                                plural ? "parameters" : "parameter",
                                missing,
                                verb,
                                directObject);
                    }

                    mVerbRequested = verb;
                    mDirectObjectRequested = directObject;
                }
            }
        } finally {
            if (needsHelp != null) {
                printHelpAndExitForAction(verb, directObject, needsHelp);
            }
        }
    }

    /**
     * Finds an {@link Arg} given an action name and a long flag name.
     * @return The {@link Arg} found or null.
     */
    protected Arg findLongArg(String verb, String directObject, String longName) {
        if (verb == null) {
            verb = GLOBAL_FLAG_VERB;
        }
        if (directObject == null) {
            directObject = NO_VERB_OBJECT;
        }
        String key = verb + "/" + directObject + "/" + longName;
        return mArguments.get(key);
    }

    /**
     * Finds an {@link Arg} given an action name and a short flag name.
     * @return The {@link Arg} found or null.
     */
    protected Arg findShortArg(String verb, String directObject, String shortName) {
        if (verb == null) {
            verb = GLOBAL_FLAG_VERB;
        }
        if (directObject == null) {
            directObject = NO_VERB_OBJECT;
        }

        for (Entry<String, Arg> entry : mArguments.entrySet()) {
            Arg arg = entry.getValue();
            if (arg.getVerb().equals(verb) && arg.getDirectObject().equals(directObject)) {
                if (shortName.equals(arg.getShortArg())) {
                    return arg;
                }
            }
        }

        return null;
    }

    /**
     * Prints the help/usage and exits.
     *
     * @param errorFormat Optional error message to print prior to usage using String.format
     * @param args Arguments for String.format
     */
    public void printHelpAndExit(String errorFormat, Object... args) {
        printHelpAndExitForAction(null /*verb*/, null /*directObject*/, errorFormat, args);
    }

    /**
     * Prints the help/usage and exits.
     *
     * @param verb If null, displays help for all verbs. If not null, display help only
     *          for that specific verb. In all cases also displays general usage and action list.
     * @param directObject If null, displays help for all verb objects.
     *          If not null, displays help only for that specific action
     *          In all cases also display general usage and action list.
     * @param errorFormat Optional error message to print prior to usage using String.format
     * @param args Arguments for String.format
     */
    public void printHelpAndExitForAction(String verb, String directObject,
            String errorFormat, Object... args) {
        if (errorFormat != null) {
            stderr(errorFormat, args);
        }

        /*
         * usage should fit in 80 columns
         *   12345678901234567890123456789012345678901234567890123456789012345678901234567890
         */
        stdout("\n" +
            "Usage:\n" +
            "  android [global options] action [action options]\n" +
            "\n" +
            "Global options:");
        listOptions(GLOBAL_FLAG_VERB, NO_VERB_OBJECT);

        if (verb == null || directObject == null) {
            stdout("\nValid actions are composed of a verb and an optional direct object:");
            for (String[] action : mActions) {

                stdout("- %1$6s %2$-7s: %3$s",
                        action[ACTION_VERB_INDEX],
                        action[ACTION_OBJECT_INDEX],
                        action[ACTION_DESC_INDEX]);
            }
        }

        for (String[] action : mActions) {
            if (verb == null || verb.equals(action[ACTION_VERB_INDEX])) {
                if (directObject == null || directObject.equals(action[ACTION_OBJECT_INDEX])) {
                    stdout("\nAction \"%1$s %2$s\":",
                            action[ACTION_VERB_INDEX],
                            action[ACTION_OBJECT_INDEX]);
                    stdout("  %1$s", action[ACTION_DESC_INDEX]);
                    stdout("Options:");
                    listOptions(action[ACTION_VERB_INDEX], action[ACTION_OBJECT_INDEX]);
                }
            }
        }

        exit();
    }

    /**
     * Internal helper to print all the option flags for a given action name.
     */
    protected void listOptions(String verb, String directObject) {
        int numOptions = 0;
        for (Entry<String, Arg> entry : mArguments.entrySet()) {
            Arg arg = entry.getValue();
            if (arg.getVerb().equals(verb) && arg.getDirectObject().equals(directObject)) {

                String value = "";
                String required = "";
                if (arg.isMandatory()) {
                    required = " [required]";

                } else {
                    if (arg.getDefaultValue() instanceof String[]) {
                        for (String v : (String[]) arg.getDefaultValue()) {
                            if (value.length() > 0) {
                                value += ", ";
                            }
                            value += v;
                        }
                    } else if (arg.getDefaultValue() != null) {
                        Object v = arg.getDefaultValue();
                        if (arg.getMode() != Mode.BOOLEAN || v.equals(Boolean.TRUE)) {
                            value = v.toString();
                        }
                    }
                    if (value.length() > 0) {
                        value = " [Default: " + value + "]";
                    }
                }

                stdout("  -%1$s %2$-10s %3$s%4$s%5$s",
                        arg.getShortArg(),
                        "--" + arg.getLongArg(),
                        arg.getDescription(),
                        value,
                        required);
                numOptions++;
            }
        }

        if (numOptions == 0) {
            stdout("  No options");
        }
    }

    //----

    /**
     * The mode of an argument specifies the type of variable it represents,
     * whether an extra parameter is required after the flag and how to parse it.
     */
    static enum Mode {
        /** Argument value is a Boolean. Default value is a Boolean. */
        BOOLEAN {
            @Override
            public boolean needsExtra() {
                return false;
            }
            @Override
            public String process(Arg arg, String extra) {
                // Toggle the current value
                arg.setCurrentValue(! ((Boolean) arg.getCurrentValue()).booleanValue());
                return null;
            }
        },

        /** Argument value is an Integer. Default value is an Integer. */
        INTEGER {
            @Override
            public boolean needsExtra() {
                return true;
            }
            @Override
            public String process(Arg arg, String extra) {
                try {
                    arg.setCurrentValue(Integer.parseInt(extra));
                    return null;
                } catch (NumberFormatException e) {
                    return String.format("Failed to parse '%1$s' as an integer: %2%s",
                            extra, e.getMessage());
                }
            }
        },

        /** Argument value is a String. Default value is a String[]. */
        ENUM {
            @Override
            public boolean needsExtra() {
                return true;
            }
            @Override
            public String process(Arg arg, String extra) {
                StringBuilder desc = new StringBuilder();
                String[] values = (String[]) arg.getDefaultValue();
                for (String value : values) {
                    if (value.equals(extra)) {
                        arg.setCurrentValue(extra);
                        return null;
                    }

                    if (desc.length() != 0) {
                        desc.append(", ");
                    }
                    desc.append(value);
                }

                return String.format("'%1$s' is not one of %2$s", extra, desc.toString());
            }
        },

        /** Argument value is a String. Default value is a null. */
        STRING {
            @Override
            public boolean needsExtra() {
                return true;
            }
            @Override
            public String process(Arg arg, String extra) {
                arg.setCurrentValue(extra);
                return null;
            }
        };

        /**
         * Returns true if this mode requires an extra parameter.
         */
        public abstract boolean needsExtra();

        /**
         * Processes the flag for this argument.
         *
         * @param arg The argument being processed.
         * @param extra The extra parameter. Null if {@link #needsExtra()} returned false.
         * @return An error string or null if there's no error.
         */
        public abstract String process(Arg arg, String extra);
    }

    /**
     * An argument accepted by the command-line, also called "a flag".
     * Arguments must have a short version (one letter), a long version name and a description.
     * They can have a default value, or it can be null.
     * Depending on the {@link Mode}, the default value can be a Boolean, an Integer, a String
     * or a String array (in which case the first item is the current by default.)
     */
    static class Arg {
        /** Verb for that argument. Never null. */
        private final String mVerb;
        /** Direct Object for that argument. Never null, but can be empty string. */
        private final String mDirectObject;
        /** The 1-letter short name of the argument, e.g. -v. */
        private final String mShortName;
        /** The long name of the argument, e.g. --verbose. */
        private final String mLongName;
        /** A description. Never null. */
        private final String mDescription;
        /** A default value. Can be null. */
        private final Object mDefaultValue;
        /** The argument mode (type + process method). Never null. */
        private final Mode mMode;
        /** True if this argument is mandatory for this verb/directobject. */
        private final boolean mMandatory;
        /** Current value. Initially set to the default value. */
        private Object mCurrentValue;
        /** True if the argument has been used on the command line. */
        private boolean mInCommandLine;

        /**
         * Creates a new argument flag description.
         *
         * @param mode The {@link Mode} for the argument.
         * @param mandatory True if this argument is mandatory for this action.
         * @param directObject The action name. Can be #NO_VERB_OBJECT or #INTERNAL_FLAG.
         * @param shortName The one-letter short argument name. Cannot be empty nor null.
         * @param longName The long argument name. Cannot be empty nor null.
         * @param description The description. Cannot be null.
         * @param defaultValue The default value (or values), which depends on the selected {@link Mode}.
         */
        public Arg(Mode mode,
                   boolean mandatory,
                   String verb,
                   String directObject,
                   String shortName,
                   String longName,
                   String description,
                   Object defaultValue) {
            mMode = mode;
            mMandatory = mandatory;
            mVerb = verb;
            mDirectObject = directObject;
            mShortName = shortName;
            mLongName = longName;
            mDescription = description;
            mDefaultValue = defaultValue;
            mInCommandLine = false;
            if (defaultValue instanceof String[]) {
                mCurrentValue = ((String[])defaultValue)[0];
            } else {
                mCurrentValue = mDefaultValue;
            }
        }

        /** Return true if this argument is mandatory for this verb/directobject. */
        public boolean isMandatory() {
            return mMandatory;
        }

        /** Returns the 1-letter short name of the argument, e.g. -v. */
        public String getShortArg() {
            return mShortName;
        }

        /** Returns the long name of the argument, e.g. --verbose. */
        public String getLongArg() {
            return mLongName;
        }

        /** Returns the description. Never null. */
        public String getDescription() {
            return mDescription;
        }

        /** Returns the verb for that argument. Never null. */
        public String getVerb() {
            return mVerb;
        }

        /** Returns the direct Object for that argument. Never null, but can be empty string. */
        public String getDirectObject() {
            return mDirectObject;
        }

        /** Returns the default value. Can be null. */
        public Object getDefaultValue() {
            return mDefaultValue;
        }

        /** Returns the current value. Initially set to the default value. Can be null. */
        public Object getCurrentValue() {
            return mCurrentValue;
        }

        /** Sets the current value. Can be null. */
        public void setCurrentValue(Object currentValue) {
            mCurrentValue = currentValue;
        }

        /** Returns the argument mode (type + process method). Never null. */
        public Mode getMode() {
            return mMode;
        }

        /** Returns true if the argument has been used on the command line. */
        public boolean isInCommandLine() {
            return mInCommandLine;
        }

        /** Sets if the argument has been used on the command line. */
        public void setInCommandLine(boolean inCommandLine) {
            mInCommandLine = inCommandLine;
        }
    }

    /**
     * Internal helper to define a new argument for a give action.
     *
     * @param mode The {@link Mode} for the argument.
     * @param verb The verb name. Can be #INTERNAL_VERB.
     * @param directObject The action name. Can be #NO_VERB_OBJECT or #INTERNAL_FLAG.
     * @param shortName The one-letter short argument name. Cannot be empty nor null.
     * @param longName The long argument name. Cannot be empty nor null.
     * @param description The description. Cannot be null.
     * @param defaultValue The default value (or values), which depends on the selected {@link Mode}.
     */
    protected void define(Mode mode,
            boolean mandatory,
            String verb,
            String directObject,
            String shortName, String longName,
            String description, Object defaultValue) {
        assert(!(mandatory && mode == Mode.BOOLEAN)); // a boolean mode cannot be mandatory

        if (directObject == null) {
            directObject = NO_VERB_OBJECT;
        }

        String key = verb + "/" + directObject + "/" + longName;
        mArguments.put(key, new Arg(mode, mandatory,
                verb, directObject, shortName, longName, description, defaultValue));
    }

    /**
     * Exits in case of error.
     * This is protected so that it can be overridden in unit tests.
     */
    protected void exit() {
        System.exit(1);
    }

    /**
     * Prints a line to stdout.
     * This is protected so that it can be overridden in unit tests.
     *
     * @param format The string to be formatted. Cannot be null.
     * @param args Format arguments.
     */
    protected void stdout(String format, Object...args) {
        mLog.printf(format + "\n", args);
    }

    /**
     * Prints a line to stderr.
     * This is protected so that it can be overridden in unit tests.
     *
     * @param format The string to be formatted. Cannot be null.
     * @param args Format arguments.
     */
    protected void stderr(String format, Object...args) {
        mLog.error(null, format, args);
    }
}
