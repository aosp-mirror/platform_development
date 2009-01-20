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
 * To use, call {@link #parseArgs(String[])} and then call {@link #getValue(String, String)}.
 */
public class CommandLineProcessor {
    
    /** Internal action name for all global flags. */
    public final static String GLOBAL_FLAG = "global";
    /** Internal action name for internally hidden flags.
     *  This is currently used to store the requested action name. */
    public final static String INTERNAL_FLAG = "internal";

    /** The global help flag. */ 
    public static final String KEY_HELP = "help";
    /** The global verbose flag. */
    public static final String KEY_VERBOSE = "verbose";
    /** The global silent flag. */
    public static final String KEY_SILENT = "silent";
    /** The internal action flag. */
    public static final String KEY_ACTION = "action";

    /** List of available actions.
     * <p/>
     * Each entry must be a 2-string array with first the action name and then
     * a description.
     */
    private final String[][] mActions;
    /** The hash of all defined arguments.
     * <p/>
     * The key is a string "action/longName".
     */
    private final HashMap<String, Arg> mArguments = new HashMap<String, Arg>();
    private final ISdkLog mLog;
    
    public CommandLineProcessor(ISdkLog logger, String[][] actions) {
        mLog = logger;
        mActions = actions;

        define(MODE.STRING, false, INTERNAL_FLAG, null, KEY_ACTION,
                "Selected Action", null);

        define(MODE.BOOLEAN, false, GLOBAL_FLAG, "v", KEY_VERBOSE,
                "Verbose mode: errors, warnings and informational messages are printed.",
                false);
        define(MODE.BOOLEAN, false, GLOBAL_FLAG, "s", KEY_SILENT,
                "Silent mode: only errors are printed out.",
                false);
        define(MODE.BOOLEAN, false, GLOBAL_FLAG, "h", KEY_HELP,
                "This help.",
                false);
    }
    
    //------------------
    // Helpers to get flags values

    /** Helper that returns true if --verbose was requested. */
    public boolean isVerbose() {
        return ((Boolean) getValue(GLOBAL_FLAG, KEY_VERBOSE)).booleanValue();
    }

    /** Helper that returns true if --silent was requested. */
    public boolean isSilent() {
        return ((Boolean) getValue(GLOBAL_FLAG, KEY_SILENT)).booleanValue();
    }

    /** Helper that returns true if --help was requested. */
    public boolean isHelpRequested() {
        return ((Boolean) getValue(GLOBAL_FLAG, KEY_HELP)).booleanValue();
    }

    /** Helper that returns the requested action name. */
    public String getActionRequested() {
        return (String) getValue(INTERNAL_FLAG, KEY_ACTION);
    }
    
    //------------------
    
    /**
     * Raw access to parsed parameter values.
     * @param action The action name, including {@link #GLOBAL_FLAG} and {@link #INTERNAL_FLAG}
     * @param longFlagName The long flag name for the given action.
     * @return The current value object stored in the parameter, which depends on the argument mode.
     */
    public Object getValue(String action, String longFlagName) {
        String key = action + "/" + longFlagName;
        Arg arg = mArguments.get(key);
        return arg.getCurrentValue();
    }

    /**
     * Internal setter for raw parameter value.
     * @param action The action name, including {@link #GLOBAL_FLAG} and {@link #INTERNAL_FLAG}
     * @param longFlagName The long flag name for the given action.
     * @param value The new current value object stored in the parameter, which depends on the
     *              argument mode.
     */
    protected void setValue(String action, String longFlagName, Object value) {
        String key = action + "/" + longFlagName;
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
        String action = null;
        
        int n = args.length;
        for (int i = 0; i < n; i++) {
            Arg arg = null;
            String a = args[i];
            if (a.startsWith("--")) {
                arg = findLongArg(action, a.substring(2));
            } else if (a.startsWith("-")) {
                arg = findShortArg(action, a.substring(1));
            }
            
            // Not a keyword and we don't have an action yet, this should be an action
            if (arg == null && action == null) {

                if (a.startsWith("-")) {
                    // Got a keyword but not valid for global flags
                    needsHelp = String.format(
                            "Flag '%1$s' is not a valid global flag. Did you mean to specify it after the action name?",
                            a, action);
                    break;
                }

                for (String[] actionDesc : mActions) {
                    if (actionDesc[0].equals(a)) {
                        action = a;
                        break;
                    }
                }
                
                if (action == null) {
                    needsHelp = String.format(
                            "Expected action name after global parameters but found %1$s instead.",
                            a);
                    break;
                }
            } else if (arg == null && action != null) {
                // Got a keyword but not valid for the current action
                needsHelp = String.format(
                        "Flag '%1$s' is not valid for action '%2$s'.",
                        a, action);
                break;
                
            } else if (arg != null) {
                // Process keyword
                String error = null;
                if (arg.getMode().needsExtra()) {
                    if (++i >= n) {
                        needsHelp = String.format("Missing argument for flag %1$s.", a);
                        break;
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
                    break;
                }
            }
        }
        
        if (needsHelp == null) {
            if (action == null) {
                needsHelp = "Missing action name.";
            } else {
                // Validate that all mandatory arguments are non-null for this action
                String missing = null;
                boolean plural = false;
                for (Entry<String, Arg> entry : mArguments.entrySet()) {
                    Arg arg = entry.getValue();
                    if (arg.getAction().equals(action)) {
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
                    needsHelp  = String.format("The %1$s %2$s must be defined for action '%3$s'",
                            plural ? "parameters" : "parameter",
                            missing,
                            action);
                }

                setValue(INTERNAL_FLAG, KEY_ACTION, action);
            }
        }

        if (needsHelp != null) {
            printHelpAndExitForAction(action, needsHelp);
        }
    }
    
    /**
     * Finds an {@link Arg} given an action name and a long flag name.
     * @return The {@link Arg} found or null.
     */
    protected Arg findLongArg(String action, String longName) {
        if (action == null) {
            action = GLOBAL_FLAG;
        }
        String key = action + "/" + longName;
        return mArguments.get(key);
    }

    /**
     * Finds an {@link Arg} given an action name and a short flag name.
     * @return The {@link Arg} found or null.
     */
    protected Arg findShortArg(String action, String shortName) {
        if (action == null) {
            action = GLOBAL_FLAG;
        }

        for (Entry<String, Arg> entry : mArguments.entrySet()) {
            Arg arg = entry.getValue();
            if (arg.getAction().equals(action)) {
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
        printHelpAndExitForAction(null /*actionFilter*/, errorFormat, args);
    }
    
    /**
     * Prints the help/usage and exits.
     * 
     * @param actionFilter If null, displays help for all actions. If not null, display help only
     *          for that specific action. In all cases also display general usage and action list.
     * @param errorFormat Optional error message to print prior to usage using String.format 
     * @param args Arguments for String.format
     */
    public void printHelpAndExitForAction(String actionFilter, String errorFormat, Object... args) {
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
        listOptions(GLOBAL_FLAG);

        stdout("\nValid actions:");
        for (String[] action : mActions) {
            String filler = "";
            int len = action[0].length();
            if (len < 10) {
                filler = "          ".substring(len);
            }
            
            stdout("- %1$s:%2$s %3$s", action[0], filler, action[1]);
        }
        
        for (String[] action : mActions) {
            if (actionFilter == null || actionFilter.equals(action[0])) {
                stdout("\nAction \"%1$s\":", action[0]);
                stdout("  %1$s", action[1]);
                stdout("Options:");
                listOptions(action[0]);
            }
        }
        
        exit();
    }

    /**
     * Internal helper to print all the option flags for a given action name.
     */
    protected void listOptions(String action) {
        int numOptions = 0;
        for (Entry<String, Arg> entry : mArguments.entrySet()) {
            Arg arg = entry.getValue();
            if (arg.getAction().equals(action)) {
                
                String value = "";
                if (arg.getDefaultValue() instanceof String[]) {
                    for (String v : (String[]) arg.getDefaultValue()) {
                        if (value.length() > 0) {
                            value += ", ";
                        }
                        value += v;
                    }
                } else if (arg.getDefaultValue() != null) {
                    value = arg.getDefaultValue().toString();
                }
                if (value.length() > 0) {
                    value = " (" + value + ")";
                }
                
                String required = arg.isMandatory() ? " [required]" : "";

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
    static enum MODE {
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
     * Depending on the {@link MODE}, the default value can be a Boolean, an Integer, a String
     * or a String array (in which case the first item is the current by default.)  
     */
    static class Arg {
        private final String mAction;
        private final String mShortName;
        private final String mLongName;
        private final String mDescription;
        private final Object mDefaultValue;
        private Object mCurrentValue;
        private final MODE mMode;
        private final boolean mMandatory;

        /**
         * Creates a new argument flag description.
         * 
         * @param mode The {@link MODE} for the argument.
         * @param mandatory True if this argument is mandatory for this action. 
         * @param action The action name. Can be #GLOBAL_FLAG or #INTERNAL_FLAG.
         * @param shortName The one-letter short argument name. Cannot be empty nor null.
         * @param longName The long argument name. Cannot be empty nor null.
         * @param description The description. Cannot be null.
         * @param defaultValue The default value (or values), which depends on the selected {@link MODE}.
         */
        public Arg(MODE mode,
                   boolean mandatory,
                   String action,
                   String shortName,
                   String longName,
                   String description,
                   Object defaultValue) {
            mMode = mode;
            mMandatory = mandatory;
            mAction = action;
            mShortName = shortName;
            mLongName = longName;
            mDescription = description;
            mDefaultValue = defaultValue;
            if (defaultValue instanceof String[]) {
                mCurrentValue = ((String[])defaultValue)[0];
            } else {
                mCurrentValue = mDefaultValue;
            }
        }
        
        public boolean isMandatory() {
            return mMandatory;
        }
        
        public String getShortArg() {
            return mShortName;
        }
        
        public String getLongArg() {
            return mLongName;
        }
        
        public String getDescription() {
            return mDescription;
        }
        
        public String getAction() {
            return mAction;
        }
        
        public Object getDefaultValue() {
            return mDefaultValue;
        }
        
        public Object getCurrentValue() {
            return mCurrentValue;
        }

        public void setCurrentValue(Object currentValue) {
            mCurrentValue = currentValue;
        }
        
        public MODE getMode() {
            return mMode;
        }
    }
    
    /**
     * Internal helper to define a new argument for a give action.
     * 
     * @param mode The {@link MODE} for the argument.
     * @param action The action name. Can be #GLOBAL_FLAG or #INTERNAL_FLAG.
     * @param shortName The one-letter short argument name. Cannot be empty nor null.
     * @param longName The long argument name. Cannot be empty nor null.
     * @param description The description. Cannot be null.
     * @param defaultValue The default value (or values), which depends on the selected {@link MODE}.
     */
    protected void define(MODE mode,
            boolean mandatory,
            String action,
            String shortName, String longName,
            String description, Object defaultValue) {
        assert(mandatory || mode == MODE.BOOLEAN); // a boolean mode cannot be mandatory
        
        String key = action + "/" + longName;
        mArguments.put(key, new Arg(mode, mandatory,
                action, shortName, longName, description, defaultValue));
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
