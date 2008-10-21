/**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


package com.android.commands.monkey;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IActivityWatcher;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Debug;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.server.data.CrashData;
import android.view.Display;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManagerImpl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Application that injects random key events and other actions into the system.
 */
public class Monkey {
    
    /**
     * Monkey Debugging/Dev Support
     * 
     * All values should be zero when checking in.
     */
    private final static int DEBUG_ALLOW_ANY_STARTS = 0;
    private final static int DEBUG_ALLOW_ANY_RESTARTS = 0;

    /** Key events that move around the UI. */
    private final int[] NAV_KEYS = {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
    };
    /**
     * Key events that perform major navigation options (so shouldn't be sent
     * as much).
     */
    private final int[] MAJOR_NAV_KEYS = {
        KeyEvent.KEYCODE_MENU, /*KeyEvent.KEYCODE_SOFT_RIGHT,*/
        KeyEvent.KEYCODE_DPAD_CENTER,
    };
    /** Key events that perform system operations. */
    private final int[] SYS_KEYS = {
        KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
    };
    /** Nice names for all key events. */
    private final String[] KEY_NAMES = {
        "KEYCODE_UNKNOWN",
        "KEYCODE_MENU",
        "KEYCODE_SOFT_RIGHT",
        "KEYCODE_HOME",
        "KEYCODE_BACK",
        "KEYCODE_CALL",
        "KEYCODE_ENDCALL",
        "KEYCODE_0",
        "KEYCODE_1",
        "KEYCODE_2",
        "KEYCODE_3",
        "KEYCODE_4",
        "KEYCODE_5",
        "KEYCODE_6",
        "KEYCODE_7",
        "KEYCODE_8",
        "KEYCODE_9",
        "KEYCODE_STAR",
        "KEYCODE_POUND",
        "KEYCODE_DPAD_UP",
        "KEYCODE_DPAD_DOWN",
        "KEYCODE_DPAD_LEFT",
        "KEYCODE_DPAD_RIGHT",
        "KEYCODE_DPAD_CENTER",
        "KEYCODE_VOLUME_UP",
        "KEYCODE_VOLUME_DOWN",
        "KEYCODE_POWER",
        "KEYCODE_CAMERA",
        "KEYCODE_CLEAR",
        "KEYCODE_A",
        "KEYCODE_B",
        "KEYCODE_C",
        "KEYCODE_D",
        "KEYCODE_E",
        "KEYCODE_F",
        "KEYCODE_G",
        "KEYCODE_H",
        "KEYCODE_I",
        "KEYCODE_J",
        "KEYCODE_K",
        "KEYCODE_L",
        "KEYCODE_M",
        "KEYCODE_N",
        "KEYCODE_O",
        "KEYCODE_P",
        "KEYCODE_Q",
        "KEYCODE_R",
        "KEYCODE_S",
        "KEYCODE_T",
        "KEYCODE_U",
        "KEYCODE_V",
        "KEYCODE_W",
        "KEYCODE_X",
        "KEYCODE_Y",
        "KEYCODE_Z",
        "KEYCODE_COMMA",
        "KEYCODE_PERIOD",
        "KEYCODE_ALT_LEFT",
        "KEYCODE_ALT_RIGHT",
        "KEYCODE_SHIFT_LEFT",
        "KEYCODE_SHIFT_RIGHT",
        "KEYCODE_TAB",
        "KEYCODE_SPACE",
        "KEYCODE_SYM",
        "KEYCODE_EXPLORER",
        "KEYCODE_ENVELOPE",
        "KEYCODE_ENTER",
        "KEYCODE_DEL",
        "KEYCODE_GRAVE",
        "KEYCODE_MINUS",
        "KEYCODE_EQUALS",
        "KEYCODE_LEFT_BRACKET",
        "KEYCODE_RIGHT_BRACKET",
        "KEYCODE_BACKSLASH",
        "KEYCODE_SEMICOLON",
        "KEYCODE_APOSTROPHE",
        "KEYCODE_SLASH",
        "KEYCODE_AT",
        "KEYCODE_NUM",
        "KEYCODE_HEADSETHOOK",
        "KEYCODE_FOCUS",
        "KEYCODE_PLUS",
        "KEYCODE_MENU",
        "KEYCODE_NOTIFICATION",
        "KEYCODE_SEARCH",
        
        "TAG_LAST_KEYCODE"      // EOL.  used to keep the lists in sync
    };

    private IActivityManager mAm;
    private IWindowManager mWm;
    private IPackageManager mPm;

    /** Command line arguments */
    private String[] mArgs;
    /** Current argument being parsed */
    private int mNextArg;
    /** Data of current argument */
    private String mCurArgData;

    /** Running in verbose output mode? 1= verbose, 2=very verbose */
    private int mVerbose;

    /** Ignore any application crashes while running? */
    private boolean mIgnoreCrashes;

    /** Ignore any not responding timeouts while running? */
    private boolean mIgnoreTimeouts;
    
    /** Ignore security exceptions when launching activities */
    /** (The activity launch still fails, but we keep pluggin' away) */
    private boolean mIgnoreSecurityExceptions;
    
    /** Monitor /data/tombstones and stop the monkey if new files appear. */
    private boolean mMonitorNativeCrashes;
    
    /** Send no events.  Use with long throttle-time to watch user operations */
    private boolean mSendNoEvents;

    /** This is set when we would like to abort the running of the monkey. */
    private boolean mAbort;
    
    /** This is set by the ActivityWatcher thread to request collection of ANR trace files */
    private boolean mRequestAnrTraces = false;

    /** This is set by the ActivityWatcher thread to request a "dumpsys meminfo" */
    private boolean mRequestDumpsysMemInfo = false;

    /** Kill the process after a timeout or crash. */
    private boolean mKillProcessAfterError;
    
    /** Generate hprof reports before/after monkey runs */
    private boolean mGenerateHprof;

    /** Packages we are allowed to run, or empty if no restriction. */
    private HashSet<String> mValidPackages = new HashSet<String>();
    /** Categories we are allowed to launch **/
    ArrayList<String> mMainCategories = new ArrayList<String>();
    /** Applications we can switch to. */
    private ArrayList<ComponentName> mMainApps = new ArrayList<ComponentName>();
    
    /** The delay between event inputs **/
    long mThrottle = 0;
    
    /** The number of iterations **/
    int mCount = 1000;
    
    /** The random number seed **/
    long mSeed = 0;
    
    /** Dropped-event statistics **/
    long mDroppedKeyEvents = 0;
    long mDroppedPointerEvents = 0;
    long mDroppedTrackballEvents = 0;
    
    /** percentages for each type of event.  These will be remapped to working
     * values after we read any optional values.
     **/
    public static final int FACTOR_TOUCH        = 0;
    public static final int FACTOR_MOTION       = 1;
    public static final int FACTOR_TRACKBALL    = 2;
    public static final int FACTOR_NAV          = 3;
    public static final int FACTOR_MAJORNAV     = 4;
    public static final int FACTOR_SYSOPS       = 5;
    public static final int FACTOR_APPSWITCH    = 6;
    public static final int FACTOR_ANYTHING     = 7;
    
    public static final int FACTORZ_COUNT       = 8;    // should be last+1
    
    float[] mFactors = new float[FACTORZ_COUNT];
    
    private static final File TOMBSTONES_PATH = new File("/data/tombstones");
    private HashSet<String> mTombstones = null;

    /**
     * Monitor operations happening in the system.
     */
    private class ActivityWatcher extends IActivityWatcher.Stub {
        public boolean activityStarting(Intent intent, String pkg) {
            boolean allow = checkEnteringPackage(pkg) || (DEBUG_ALLOW_ANY_STARTS != 0);
            if (mVerbose > 0) {
                System.out.println("    // " + (allow ? "Allowing" : "Rejecting")
                        + " start of " + intent + " in package " + pkg);
            }
            return allow;
        }
        
        public boolean activityResuming(String pkg) {
            System.out.println("    // activityResuming(" + pkg + ")");
            boolean allow = checkEnteringPackage(pkg) || (DEBUG_ALLOW_ANY_RESTARTS != 0);
            if (!allow) {
                if (mVerbose > 0) {
                    System.out.println("    // " + (allow ? "Allowing" : "Rejecting")
                            + " resume of package " + pkg);
                }
            }
            return allow;
        }
        
        private boolean checkEnteringPackage(String pkg) {
            if (pkg == null) {
                return true;
            }
            // preflight the hash lookup to avoid the cost of hash key generation
            if (mValidPackages.size() == 0) {
                return true;
            } else {
                return mValidPackages.contains(pkg);
            }
        }
        
        public boolean appCrashed(String processName, int pid, String shortMsg,
                String longMsg, byte[] crashData) {
            System.err.println("// CRASH: " + processName + " (pid " + pid
                    + ")");
            System.err.println("// Short Msg: " + shortMsg);
            System.err.println("// Long Msg: " + longMsg);
            if (crashData != null) {
                try {
                    CrashData cd = new CrashData(new DataInputStream(
                            new ByteArrayInputStream(crashData)));
                    System.err.println("// Build Label: "
                            + cd.getBuildData().getFingerprint());
                    System.err.println("// Build Changelist: "
                            + cd.getBuildData().getIncrementalVersion());
                    System.err.println("// Build Time: "
                            + cd.getBuildData().getTime());
                    System.err.println("// ID: " + cd.getId());
                    System.err.println("// Tag: " + cd.getActivity());
                    System.err.println(cd.getThrowableData().toString(
                            "// "));
                } catch (IOException e) {
                    System.err.println("// BAD STACK CRAWL");
                }
            }

            if (!mIgnoreCrashes) {
                synchronized (Monkey.this) {
                    mAbort = true;
                }

                return !mKillProcessAfterError;
            }
            return false;
        }

        public int appNotResponding(String processName, int pid,
                String processStats) {
            System.err.println("// NOT RESPONDING: " + processName
                    + " (pid " + pid + ")");
            System.err.println(processStats);
            reportProcRank();
            synchronized (Monkey.this) {
                mRequestAnrTraces = true;
                mRequestDumpsysMemInfo = true;
            }
            if (!mIgnoreTimeouts) {
                synchronized (Monkey.this) {
                    mAbort = true;
                }
                return (mKillProcessAfterError) ? -1 : 1;
            }
            return 1;
        }
    }
    
    /**
     * Run the procrank tool to insert system status information into the debug report.
     */
    private void reportProcRank() {
      commandLineReport("procrank", "procrank");
    }
    
    /**
     * Run "cat /data/anr/traces.txt".  Wait about 5 seconds first, to let the asynchronous
     * report writing complete.
     */
    private void reportAnrTraces() {
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) { 
        }
        commandLineReport("anr traces", "cat /data/anr/traces.txt");
    }
    
    /**
     * Run "dumpsys meminfo"
     * 
     * NOTE:  You cannot perform a dumpsys call from the ActivityWatcher callback, as it will
     * deadlock.  This should only be called from the main loop of the monkey.
     */
    private void reportDumpsysMemInfo() {
        commandLineReport("meminfo", "dumpsys meminfo");
    }
    
    /**
     * Print report from a single command line.
     * @param reportName Simple tag that will print before the report and in various annotations.
     * @param command Command line to execute.
     * TODO: Use ProcessBuilder & redirectErrorStream(true) to capture both streams (might be
     * important for some command lines)
     */
    private void commandLineReport(String reportName, String command) {
        System.err.println(reportName + ":");
        Runtime rt = Runtime.getRuntime();
        try {
            // Process must be fully qualified here because android.os.Process is used elsewhere
            java.lang.Process p = Runtime.getRuntime().exec(command);
            
            // pipe everything from process stdout -> System.err
            InputStream inStream = p.getInputStream();
            InputStreamReader inReader = new InputStreamReader(inStream);
            BufferedReader inBuffer = new BufferedReader(inReader);
            String s;
            while ((s = inBuffer.readLine()) != null) {
                System.err.println(s);
            }
            
            int status = p.waitFor();
            System.err.println("// " + reportName + " status was " + status);
        } catch (Exception e) {
            System.err.println("// Exception from " + reportName + ":");
            System.err.println(e.toString());
        }
    }

    /**
     * Command-line entry point.
     *
     * @param args The command-line arguments
     */
    public static void main(String[] args) {
        int resultCode = (new Monkey()).run(args);
        System.exit(resultCode);
    }

    /**
     * Run the command!
     *
     * @param args The command-line arguments
     * @return Returns a posix-style result code.  0 for no error.
     */
    private int run(String[] args) {
        // Super-early debugger wait
        for (String s : args) {
            if ("--wait-dbg".equals(s)) {
                Debug.waitForDebugger();
            }
        }
        
        // Default values for some command-line options
        mVerbose = 0;
        mCount = 1000;
        mSeed = 0;
        mThrottle = 0;
        
        // default values for random distributions
        // note, these are straight percentages, to match user input (cmd line args)
        // but they will be converted to 0..1 values before the main loop runs.
        mFactors[FACTOR_TOUCH] = 15.0f;
        mFactors[FACTOR_MOTION] = 10.0f;
        mFactors[FACTOR_TRACKBALL] = 15.0f;
        mFactors[FACTOR_NAV] = 25.0f;
        mFactors[FACTOR_MAJORNAV] = 15.0f;
        mFactors[FACTOR_SYSOPS] = 2.0f;
        mFactors[FACTOR_APPSWITCH] = 2.0f;
        mFactors[FACTOR_ANYTHING] = 16.0f;

        // prepare for command-line processing
        mArgs = args;
        mNextArg = 0;
        if (!processOptions()) {
            return -1;
        }
        
        // now set up additional data in preparation for launch
        if (mMainCategories.size() == 0) {
            mMainCategories.add(Intent.CATEGORY_LAUNCHER);
            mMainCategories.add(Intent.CATEGORY_MONKEY);
        }

        if (mVerbose > 0) {
            System.out.println(":Monkey: seed=" + mSeed + " count=" + mCount);
            if (mValidPackages.size() > 0) {
                Iterator<String> it = mValidPackages.iterator();
                while (it.hasNext()) {
                    System.out.println(":AllowPackage: " + it.next());
                }
            }
            if (mMainCategories.size() != 0) {
                Iterator<String> it = mMainCategories.iterator();
                while (it.hasNext()) {
                    System.out.println(":IncludeCategory: " + it.next());
                }
            }
        }
        
        if (!checkInternalConfiguration()) {
            return -2;
        }
        
        if (!getSystemInterfaces()) {
            return -3;
        }

        if (!getMainApps()) {
            return -4;
        }
        
        if (!adjustEventFactors()) {
            return -5;
        }

        // Java's Random doesn't scramble well at all on seeding, so we'll use
        // the better random source here.
        SecureRandom random = new SecureRandom();
        random.setSeed((mSeed == 0) ? -1 : mSeed);
        if (mVerbose >= 2) {    // check seeding performance
            System.out.println("// Seeded: " + mSeed + " and pulling: " + random.nextFloat());
        }
        
        // If we're profiling, do it immediately before/after the main monkey loop
        if (mGenerateHprof) {
            signalPersistentProcesses();
        }
        
        int crashedAtCycle = runMonkeyCycles(random);

        synchronized (this) {
            if (mRequestAnrTraces) {
                reportAnrTraces();
                mRequestAnrTraces = false;
            }
            if (mRequestDumpsysMemInfo) {
                reportDumpsysMemInfo();
                mRequestDumpsysMemInfo = false;
            }
        }

        if (mGenerateHprof) {
            signalPersistentProcesses();
            if (mVerbose > 0) {
                System.out.println("// Generated profiling reports in /data/misc");
            }
        }
        
        try {
            mAm.setActivityWatcher(null);
        } catch (RemoteException e) {
            // just in case this was latent (after mCount cycles), make sure
            // we report it
            if (crashedAtCycle >= mCount) {
                crashedAtCycle = mCount -1;
            }
        }
        
        // report dropped event stats
        if (mVerbose > 0) {
            System.out.print(":Dropped: keys=");
            System.out.print(mDroppedKeyEvents);
            System.out.print(" pointers=");
            System.out.print(mDroppedPointerEvents);
            System.out.print(" trackballs=");
            System.out.println(mDroppedTrackballEvents);
        }

        if (crashedAtCycle < mCount) {
            System.err.println("** System appears to have crashed at event "
                    + crashedAtCycle + " of " + mCount + " using seed " + mSeed);
           return crashedAtCycle;
        } else {
            if (mVerbose > 0) {
                System.out.println("// Monkey finished");
            }
            return 0;
        }
    }
    
    /**
     * Process the command-line options
     * 
     * @return Returns true if options were parsed with no apparent errors.
     */
    private boolean processOptions() {
        // quick (throwaway) check for unadorned command
        if (mArgs.length < 1) {
            showUsage();
            return false;
        }

        try {
            String opt;
            while ((opt=nextOption()) != null) {
                if (opt.equals("-s")) {
                    mSeed = nextOptionLong("Seed");
                } else if (opt.equals("-p")) {
                    mValidPackages.add(nextOptionData());
                } else if (opt.equals("-c")) {
                    mMainCategories.add(nextOptionData());
                } else if (opt.equals("-v")) {
                    mVerbose += 1;
                } else if (opt.equals("--ignore-crashes")) {
                    mIgnoreCrashes = true;
                } else if (opt.equals("--ignore-timeouts")) {
                    mIgnoreTimeouts = true;
                } else if (opt.equals("--ignore-security-exceptions")) {
                    mIgnoreSecurityExceptions = true;
                } else if (opt.equals("--monitor-native-crashes")) {
                    mMonitorNativeCrashes = true;
                } else if (opt.equals("--kill-process-after-error")) {
                    mKillProcessAfterError = true;
                } else if (opt.equals("--hprof")) {
                    mGenerateHprof = true;
                } else if (opt.equals("--pct-touch")) {
                    mFactors[FACTOR_TOUCH] = -nextOptionLong("touch events percentage");
                } else if (opt.equals("--pct-motion")) {
                    mFactors[FACTOR_MOTION] = -nextOptionLong("motion events percentage");
                } else if (opt.equals("--pct-trackball")) {
                    mFactors[FACTOR_TRACKBALL] = -nextOptionLong("trackball events percentage");
                } else if (opt.equals("--pct-syskeys")) {
                    mFactors[FACTOR_SYSOPS] = -nextOptionLong("system key events percentage");
                } else if (opt.equals("--pct-nav")) {
                    mFactors[FACTOR_NAV] = -nextOptionLong("nav events percentage");
                } else if (opt.equals("--pct-majornav")) {
                    mFactors[FACTOR_MAJORNAV] = -nextOptionLong("major nav events percentage");
                } else if (opt.equals("--pct-appswitch")) {
                    mFactors[FACTOR_APPSWITCH] = -nextOptionLong("app switch events percentage");
                } else if (opt.equals("--pct-anyevent")) {
                    mFactors[FACTOR_ANYTHING] = -nextOptionLong("any events percentage");
                } else if (opt.equals("--throttle")) {
                  mThrottle = nextOptionLong("delay (in milliseconds) to wait between events");
                } else if (opt.equals("--wait-dbg")) {
                    // do nothing - it's caught at the very start of run()
                } else if (opt.equals("--dbg-no-events")) {
                    mSendNoEvents = true;
                } else if (opt.equals("-h")) {
                    showUsage();
                    return false;
                } else {
                    System.err.println("** Error: Unknown option: " + opt);
                    showUsage();
                    return false;
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("** Error: " + ex.toString());
            showUsage();
            return false;
        }

        String countStr = nextArg();
        if (countStr == null) {
            System.err.println("** Error: Count not specified");
            showUsage();
            return false;
        }

        try {
            mCount = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            System.err.println("** Error: Count is not a number");
            showUsage();
            return false;
        }
        
        return true;
    }
    
    /**
     * Check for any internal configuration (primarily build-time) errors.
     * 
     * @return Returns true if ready to rock.
     */
    private boolean checkInternalConfiguration() {
        // Check KEYCODE name array, make sure it's up to date.
        
        String lastKeyName = null;
        try {
            lastKeyName = KEY_NAMES[KeyEvent.MAX_KEYCODE+1];
        } catch (RuntimeException e) {
        }
        if (! "TAG_LAST_KEYCODE".equals(lastKeyName)) {
            System.err.println("** Error: Key names array malformed (internal error).");
            return false;
        }

        return true;
    }

    /**
     * Attach to the required system interfaces.
     * 
     * @return Returns true if all system interfaces were available.
     */
    private boolean getSystemInterfaces() {
        mAm = ActivityManagerNative.getDefault();
        if (mAm == null) {
            System.err.println("** Error: Unable to connect to activity manager; is the system running?");
            return false;
        }

        mWm = IWindowManager.Stub.asInterface(
                ServiceManager.getService("window"));
        if (mWm == null) {
            System.err.println("** Error: Unable to connect to window manager; is the system running?");
            return false;
        }

        mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (mPm == null) {
            System.err.println("** Error: Unable to connect to package manager; is the system running?");
            return false;
        }
        
        try {
            mAm.setActivityWatcher(new ActivityWatcher());
        } catch (RemoteException e) {
            System.err.println("** Failed talking with activity manager!");
            return false;
        }

        return true;
    }
    
    /**
     * Using the restrictions provided (categories & packages), generate a list of activities
     * that we can actually switch to.
     * 
     * @return Returns true if it could successfully build a list of target activities
     */
    private boolean getMainApps() {
        try {
            final int N = mMainCategories.size();
            for (int i=0; i<N; i++) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                String category = mMainCategories.get(i);
                if (category.length() > 0) {
                    intent.addCategory(category);
                }
                List<ResolveInfo> mainApps = mPm.queryIntentActivities(intent, null, 0);
                if (mainApps == null || mainApps.size() == 0) {
                    System.err.println("// Warning: no activities found for category " + category);
                    continue;
                }
                if (mVerbose >= 2) {     // very verbose
                    System.out.println("// Selecting main activities from category " + category);
                }
                final int NA = mainApps.size();
                for (int a=0; a<NA; a++) {
                    ResolveInfo r = mainApps.get(a);
                    if (mValidPackages.size() == 0 || 
                            mValidPackages.contains(r.activityInfo.applicationInfo.packageName)) {
                        if (mVerbose >= 2) {     // very verbose
                            System.out.println("//   + Using main activity "
                                    + r.activityInfo.name
                                    + " (from package "
                                    + r.activityInfo.applicationInfo.packageName
                                    + ")");
                        }
                        mMainApps.add(new ComponentName(
                                r.activityInfo.applicationInfo.packageName,
                                r.activityInfo.name));
                    } else {
                        if (mVerbose >= 3) {     // very very verbose
                            System.out.println("//   - NOT USING main activity "
                                    + r.activityInfo.name
                                    + " (from package "
                                    + r.activityInfo.applicationInfo.packageName
                                    + ")");
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            System.err.println("** Failed talking with package manager!");
            return false;
        }

        if (mMainApps.size() == 0) {
            System.out.println("** No activities found to run, monkey aborted.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Adjust the percentages (after applying user values) and then normalize to a 0..1 scale.
     */
    private boolean adjustEventFactors() {
        // go through all values and compute totals for user & default values
        float userSum = 0.0f;
        float defaultSum = 0.0f;
        int defaultCount = 0;
        for (int i = 0; i < FACTORZ_COUNT; ++i) {
            if (mFactors[i] <= 0.0f) {   // user values are zero or negative
                userSum -= mFactors[i];
            } else {
                defaultSum += mFactors[i];
                ++defaultCount;
            }            
        }
        
        // if the user request was > 100%, reject it
        if (userSum > 100.0f) {
            System.err.println("** Event weights > 100%");
            showUsage();
            return false;
        }
        
        // if the user specified all of the weights, then they need to be 100%
        if (defaultCount == 0 && (userSum < 99.9f || userSum > 100.1f)) {
            System.err.println("** Event weights != 100%");
            showUsage();
            return false;
        }
        
        // compute the adjustment necessary
        float defaultsTarget = (100.0f - userSum);
        float defaultsAdjustment = defaultsTarget / defaultSum;
        
        // fix all values, by adjusting defaults, or flipping user values back to >0
        for (int i = 0; i < FACTORZ_COUNT; ++i) {
            if (mFactors[i] <= 0.0f) {   // user values are zero or negative
                mFactors[i] = -mFactors[i];
            } else {
                mFactors[i] *= defaultsAdjustment;
            }
        }
        
        // if verbose, show factors
        if (mVerbose > 0) {
            System.out.println("// Event percentages:");
            for (int i = 0; i < FACTORZ_COUNT; ++i) {
                System.out.println("//   " + i + ": " + mFactors[i] + "%");
            }
        }
        
        // finally, normalize and convert to running sum
        float sum = 0.0f;
        for (int i = 0; i < FACTORZ_COUNT; ++i) {
            sum += mFactors[i] / 100.0f;
            mFactors[i] = sum;
        }
        
        return true;
    }
    
    /**
     * Run mCount cycles and see if we hit any crashers.
     * 
     * TODO:  Meta state on keys
     * 
     * @param random The random source to use
     * @return Returns the last cycle which executed.  If the value == mCount, no errors detected.
     */
    private int runMonkeyCycles(Random random) {
        int i = 0;
        int lastKey = 0;

        boolean systemCrashed = false;

        if (!startRandomActivity(random)) {
            systemCrashed = true;
        }

        while (!systemCrashed && i < mCount) {
            synchronized (this) {
                if (mRequestAnrTraces) {
                    reportAnrTraces();
                    mRequestAnrTraces = false;
                }
                if (mRequestDumpsysMemInfo) {
                    reportDumpsysMemInfo();
                    mRequestDumpsysMemInfo = false;
                }
                if (mMonitorNativeCrashes) {
                    // first time through, when i == 0, just set up the watcher (ignore the error)
                    if (checkNativeCrashes() && (i > 0)) {
                        System.out.println("** New native crash detected.");
                        mAbort = mAbort || mKillProcessAfterError;
                    }
                }
                if (mAbort) {
                    System.out.println("** Monkey aborted due to error.");
                    System.out.println("Events injected: " + i);
                    return i;
                }
            }
            
            try {
              Thread.sleep(mThrottle);
            } catch (InterruptedException e1) {
               System.out.println("** Monkey interrupted in sleep.");
               return i;
            }
            
            // In this debugging mode, we never send any events.  This is primarily
            // here so you can manually test the package or category limits, while manually
            // exercising the system.
            if (mSendNoEvents) {
                i++;
                continue;
            }

            if ((mVerbose > 0) && (i%100) == 0 && i != 0 && lastKey == 0) {
                System.out.println("    // Sending event #" + i);
            }

            // if the last event was a keydown, then this event is a key-up
            if (lastKey != 0) {
                if (mVerbose > 1) {
                    try {
                        System.out.println(":SendKey (ACTION_UP):" + lastKey + "    // " + KEY_NAMES[lastKey]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println(":SendKey (ACTION_UP): " + lastKey + "    // Unknown key event");
                    }
                }
                try {
                    if (! mWm.injectKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, lastKey), false)) {
                        mDroppedKeyEvents++;
                    }
                } catch (RemoteException ex) {
                    systemCrashed = true;
                    break;
                }
                lastKey = 0;
                i++;
                continue;
            }
            
            // otherwise begin a new event cycle
            float cls = random.nextFloat();

            boolean touchEvent = cls < mFactors[FACTOR_TOUCH];
            boolean motionEvent = !touchEvent && (cls < mFactors[FACTOR_MOTION]);
            if (touchEvent || motionEvent) {
                try {
                    generateMotionEvent(random, motionEvent);
                } catch (RemoteException ex) {
                    systemCrashed = true;
                    break;
                }
                i++;
                continue;
            }
            
            if (cls < mFactors[FACTOR_TRACKBALL]) {
                try {
                    generateTrackballEvent(random);
                } catch (RemoteException ex) {
                    systemCrashed = true;
                    break;
                }
                i++;
                continue;
            }

            // The remaining event categories are injected as key events
            if (cls < mFactors[FACTOR_NAV]) {
                lastKey = NAV_KEYS[random.nextInt(NAV_KEYS.length)];
            } else if (cls < mFactors[FACTOR_MAJORNAV]) {
                lastKey = MAJOR_NAV_KEYS[random.nextInt(MAJOR_NAV_KEYS.length)];
            } else if (cls < mFactors[FACTOR_SYSOPS]) {
                lastKey = SYS_KEYS[random.nextInt(SYS_KEYS.length)];
            } else if (cls < mFactors[FACTOR_APPSWITCH]) {
                if (!startRandomActivity(random)) {
                    systemCrashed = true;
                    break;
                }
                i++;
                continue;
            } else {
                lastKey = 1 + random.nextInt(KeyEvent.MAX_KEYCODE - 1);
            }
            if (mVerbose > 0) {
                try {
                    System.out.println(":SendKey: " + lastKey + "    // " + KEY_NAMES[lastKey]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(":SendKey: " + lastKey + "    // Unknown key event");
                }
            }
            try {
                if (! mWm.injectKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, lastKey), false)) {
                    mDroppedKeyEvents++;
                }
            } catch (RemoteException ex) {
                systemCrashed = true;
                break;
            }
        }
        
        // If we got this far, we succeeded!
        return mCount;
    }

    /**
     * Generates a random motion event. This method counts a down, move, and up as one event.
     * 
     * TODO:  Test & fix the selectors when non-zero percentages
     * TODO:  Longpress.
     * TODO:  Fling.
     * TODO:  Meta state
     * TODO:  More useful than the random walk here would be to pick a single random direction
     * and distance, and divvy it up into a random number of segments.  (This would serve to
     * generate fling gestures, which are important).
     * 
     * @param random Random number source for positioning
     * @param motionEvent If false, touch/release.  If true, touch/move/release.
     * 
     * @throws RemoteException
     */
    private void generateMotionEvent(Random random, boolean motionEvent) throws RemoteException {
        Display display = WindowManagerImpl.getDefault().getDefaultDisplay();

        float x = Math.abs(random.nextInt() % display.getWidth());
        float y = Math.abs(random.nextInt() % display.getHeight());
        long downAt = SystemClock.uptimeMillis();
        boolean drop = false;
        drop = sendMotionEvent(MotionEvent.ACTION_DOWN, x, y, downAt, "Pointer ACTION_DOWN", false,
                true);

        // sometimes we'll move during the touch
        if (motionEvent) {
            int count = random.nextInt(10);
            for (int i = 0 ; i < count ; i++) {
                // generate some slop in the up event
                x = (x + (random.nextInt() % 10)) % display.getWidth();
                y = (y + (random.nextInt() % 10)) % display.getHeight();
                drop |= sendMotionEvent(MotionEvent.ACTION_MOVE, x, y, downAt, 
                        "Pointer ACTION_MOVE", true, true);
            }
        }

        // TODO generate some slop in the up event
        drop |= sendMotionEvent(MotionEvent.ACTION_UP, x, y, downAt, "Pointer ACTION_UP", false, 
                true);
        
        if (drop) {
            mDroppedPointerEvents++;
        }
    }

    /**
     * Sends a single motion event, either as a pointer or a trackball.
     * 
     * @param action Must be one of the ACTION values defined in {@link MotionEvent}
     * @param x The position, or movement, in the X axis
     * @param y The position, or movement, in the Y axis
     * @param downAt The time of the first DOWN must be sent here, and the same value must
     * be sent for all subsequent events that are related (through the eventual UP event), or
     * -1 to simply send the current time as the downTime.
     * @param note This will be displayed when in verbose mode
     * @param intermediateNote If true, this is an intermediate step (more verbose logging, only)
     * @param isPointer Use true to send a pointer event, and false to send a trackball event
     * 
     * @return Returns false if event was dispatched, true if it was dropped for any reason
     * 
     * @throws RemoteException
     */
    private boolean sendMotionEvent(int action, float x, float y, long downAt, final String note, 
            boolean intermediateNote, boolean isPointer) throws RemoteException {
        if ((mVerbose > 0 && !intermediateNote) || mVerbose > 1) {
            System.out.println(":Sending " + note + " x=" + x + " y=" + y);
        }
        long eventTime = SystemClock.uptimeMillis();
        if (downAt == -1) {
            downAt = eventTime;
        }
        final MotionEvent evt = MotionEvent.obtain(downAt, eventTime, action, x, y, 0);
        if (isPointer) {
            return ! mWm.injectPointerEvent(evt, false);
        } else {
            return ! mWm.injectTrackballEvent(evt, false);
        }
    }

    /**
     * Generates a random trackball event. This consists of a sequence of small moves, followed by
     * an optional single click.
     * 
     * TODO:  Longpress.
     * TODO:  Meta state
     * TODO:  Parameterize the % clicked
     * TODO:  More useful than the random walk here would be to pick a single random direction
     * and distance, and divvy it up into a random number of segments.  (This would serve to
     * generate fling gestures, which are important).
     * 
     * @param random Random number source for positioning
     * 
     * @throws RemoteException
     */
    private void generateTrackballEvent(Random random) throws RemoteException {
        Display display = WindowManagerImpl.getDefault().getDefaultDisplay();

        boolean drop = false;
        int count = random.nextInt(10);
        for (int i = 0; i < 10; ++i) {
            // generate a small random step
            int dX = random.nextInt(10) - 5;
            int dY = random.nextInt(10) - 5;
            drop |= sendMotionEvent(MotionEvent.ACTION_MOVE, dX, dY, -1, "Trackball ACTION_MOVE", 
                    (i > 0), false);
        }
        
        // 10% of trackball moves end with a click
        if (0 == random.nextInt(10)) {
            long downAt = SystemClock.uptimeMillis();
            drop |= sendMotionEvent(MotionEvent.ACTION_DOWN, 0, 0, downAt, "Trackball ACTION_DOWN",
                    true, false);
            drop |= sendMotionEvent(MotionEvent.ACTION_UP, 0, 0, downAt, "Trackball ACTION_UP", 
                    false, false);
        }
        
        if (drop) {
            mDroppedTrackballEvents++;
        }
    }

    /**
     * Send SIGNAL_USR1 to all processes.  This will generate large (5mb) profiling reports
     * in data/misc, so use with care.
     */
    private void signalPersistentProcesses() {
        try {
            mAm.signalPersistentProcesses(Process.SIGNAL_USR1);

            synchronized (this) {
                wait(2000);
            }
        } catch (RemoteException e) {
            System.err.println("** Failed talking with activity manager!");
        } catch (InterruptedException e) {
        }
    }

    /**
     * Have the activity manager start a new activity.
     *
     * @param random Random number source
     * 
     * @return Returns true on success, false if there was an error calling
     * the activity manager.
     */
    private boolean startRandomActivity(Random random) {
        int numApps = mMainApps.size();
        int which = random.nextInt(numApps);
        ComponentName app = mMainApps.get(which);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(app);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (mVerbose > 0) {
            System.out.println(":Switch: " + intent.toURI());
        }
        try {
            mAm.startActivity(null, intent, null, null, 0, null, null, 0, false, false);
        } catch (RemoteException e) {
            System.err.println("** Failed talking with activity manager!");
            return false;
        } catch (SecurityException e) {
            if (mVerbose > 0) {
                System.out.println("** Permissions error starting activity " + intent.toURI());
            }
            return mIgnoreSecurityExceptions;   // true = "launched ok" (pretend)
        }
        return true;
    }
    
    /**
     * Watch for appearance of new tombstone files, which indicate native crashes.
     * 
     * @return Returns true if new files have appeared in the list
     */
    private boolean checkNativeCrashes() {
        String[] tombstones = TOMBSTONES_PATH.list();
        
        // shortcut path for usually empty directory, so we don't waste even more objects
        if ((tombstones == null) || (tombstones.length == 0)) {
            mTombstones = null;
            return false;
        }
        
        // use set logic to look for new files
        HashSet<String> newStones = new HashSet<String>();
        for (String x : tombstones) {
            newStones.add(x);
        }

        boolean result = (mTombstones == null) || !mTombstones.containsAll(newStones);

        // keep the new list for the next time
        mTombstones = newStones;

        return result;
    }

    /**
     * Return the next command line option.  This has a number of special cases which
     * closely, but not exactly, follow the POSIX command line options patterns:
     * 
     * -- means to stop processing additional options
     * -z means option z
     * -z ARGS means option z with (non-optional) arguments ARGS
     * -zARGS means option z with (optional) arguments ARGS
     * --zz means option zz
     * --zz ARGS means option zz with (non-optional) arguments ARGS
     * 
     * Note that you cannot combine single letter options;  -abc != -a -b -c
     *
     * @return Returns the option string, or null if there are no more options.
     */
    private String nextOption() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        if (!arg.startsWith("-")) {
            return null;
        }
        mNextArg++;
        if (arg.equals("--")) {
            return null;
        }
        if (arg.length() > 1 && arg.charAt(1) != '-') {
            if (arg.length() > 2) {
                mCurArgData = arg.substring(2);
                return arg.substring(0, 2);
            } else {
                mCurArgData = null;
                return arg;
            }
        }
        mCurArgData = null;
        return arg;
    }

    /**
     * Return the next data associated with the current option.
     *
     * @return Returns the data string, or null of there are no more arguments.
     */
    private String nextOptionData() {
        if (mCurArgData != null) {
            return mCurArgData;
        }
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String data = mArgs[mNextArg];
        mNextArg++;
        return data;
    }
    
    /**
     * Returns a long converted from the next data argument, with error handling if not available.
     * 
     * @param opt The name of the option.
     * @return Returns a long converted from the argument.
     */
    private long nextOptionLong(final String opt) {
        long result;
        try {
            result = Long.parseLong(nextOptionData());
        } catch (NumberFormatException e) {
            System.err.println("** Error: " + opt + " is not a number");
            throw e;
        }
        return result;
    }

    /**
     * Return the next argument on the command line.
     *
     * @return Returns the argument string, or null if we have reached the end.
     */
    private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }

    /**
     * Print how to use this command.
     */
    private void showUsage() {
        System.err.println("usage: monkey [-p ALLOWED_PACKAGE [-p ALLOWED_PACKAGE] ...]");
        System.err.println("              [-c MAIN_CATEGORY [-c MAIN_CATEGORY] ...]");
        System.err.println("              [--ignore-crashes] [--ignore-timeouts]");
        System.err.println("              [--ignore-security-exceptions] [--monitor-native-crashes]");
        System.err.println("              [--kill-process-after-error] [--hprof]");
        System.err.println("              [--pct-touch PERCENT] [--pct-motion PERCENT]");
        System.err.println("              [--pct-trackball PERCENT] [--pct-syskeys PERCENT]");
        System.err.println("              [--pct-nav PERCENT] [--pct-majornav PERCENT]");
        System.err.println("              [--pct-appswitch PERCENT] [--pct-anyevent PERCENT]");
        System.err.println("              [--wait-dbg] [--dbg-no-events]");
        System.err.println("              [-s SEED] [-v [-v] ...] [--throttle MILLISEC]");
        System.err.println("              COUNT");
    }
}
