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
import android.app.IActivityController;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Debug;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.server.data.CrashData;
import android.view.IWindowManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
    
    /** This is set by the ActivityController thread to request collection of ANR trace files */
    private boolean mRequestAnrTraces = false;

    /** This is set by the ActivityController thread to request a "dumpsys meminfo" */
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
    long mDroppedFlipEvents = 0;

    /** a filename to the script (if any) **/
    private String mScriptFileName = null;
    
    private static final File TOMBSTONES_PATH = new File("/data/tombstones");
    private HashSet<String> mTombstones = null;
    
    float[] mFactors = new float[MonkeySourceRandom.FACTORZ_COUNT];    
    MonkeyEventSource mEventSource;
    private MonkeyNetworkMonitor mNetworkMonitor = new MonkeyNetworkMonitor();
    
    /**
     * Monitor operations happening in the system.
     */
    private class ActivityController extends IActivityController.Stub {
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
     * NOTE:  You cannot perform a dumpsys call from the ActivityController callback, as it will
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
        
        // prepare for command-line processing
        mArgs = args;
        mNextArg = 0;
        
        //set a positive value, indicating none of the factors is provided yet
        for (int i = 0; i < MonkeySourceRandom.FACTORZ_COUNT; i++) {
            mFactors[i] = 1.0f;
        }
        
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
        
        if (mScriptFileName != null) {
            // script mode, ignore other options
            mEventSource = new MonkeySourceScript(mScriptFileName, mThrottle);
            mEventSource.setVerbose(mVerbose);
        } else {
            // random source by default
            if (mVerbose >= 2) {    // check seeding performance
                System.out.println("// Seeded: " + mSeed);
            }
            mEventSource = new MonkeySourceRandom(mSeed, mMainApps, mThrottle);
            mEventSource.setVerbose(mVerbose);
            //set any of the factors that has been set
            for (int i = 0; i < MonkeySourceRandom.FACTORZ_COUNT; i++) {
                if (mFactors[i] <= 0.0f) {
                    ((MonkeySourceRandom) mEventSource).setFactors(i, mFactors[i]);
                }
            }
            
            //in random mode, we start with a random activity
            ((MonkeySourceRandom) mEventSource).generateActivity();
        }

        //validate source generator
        if (!mEventSource.validate()) {
            return -5;
        }
        
        if (mScriptFileName != null) {
            // in random mode, count is the number of single events
            // while in script mode, count is the number of repetition
            // for a sequence of events, so we need do multiply the length of
            // that sequence
            mCount = mCount * ((MonkeySourceScript) mEventSource)
                .getOneRoundEventCount();
        }
        
        // If we're profiling, do it immediately before/after the main monkey loop
        if (mGenerateHprof) {
            signalPersistentProcesses();
        }
        
        mNetworkMonitor.start();
        int crashedAtCycle = runMonkeyCycles();
        mNetworkMonitor.stop();

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
            mAm.setActivityController(null);
            mNetworkMonitor.unregister(mAm);
        } catch (RemoteException e) {
            // just in case this was latent (after mCount cycles), make sure
            // we report it
            if (crashedAtCycle >= mCount) {
                crashedAtCycle = mCount - 1;
            }
        }
        
        // report dropped event stats
        if (mVerbose > 0) {
            System.out.print(":Dropped: keys=");
            System.out.print(mDroppedKeyEvents);
            System.out.print(" pointers=");
            System.out.print(mDroppedPointerEvents);
            System.out.print(" trackballs=");
            System.out.print(mDroppedTrackballEvents);
            System.out.print(" flips=");
            System.out.println(mDroppedFlipEvents);
        }
        
        // report network stats
        mNetworkMonitor.dump();

        if (crashedAtCycle < mCount - 1) {
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
            while ((opt = nextOption()) != null) {
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
                    mFactors[MonkeySourceRandom.FACTOR_TOUCH] = 
                        -nextOptionLong("touch events percentage");
                } else if (opt.equals("--pct-motion")) {
                    mFactors[MonkeySourceRandom.FACTOR_MOTION] = 
                        -nextOptionLong("motion events percentage");
                } else if (opt.equals("--pct-trackball")) {
                    mFactors[MonkeySourceRandom.FACTOR_TRACKBALL] = 
                        -nextOptionLong("trackball events percentage");
                } else if (opt.equals("--pct-nav")) {
                    mFactors[MonkeySourceRandom.FACTOR_NAV] = 
                        -nextOptionLong("nav events percentage");
                } else if (opt.equals("--pct-majornav")) {
                    mFactors[MonkeySourceRandom.FACTOR_MAJORNAV] = 
                        -nextOptionLong("major nav events percentage");
                } else if (opt.equals("--pct-appswitch")) {
                    mFactors[MonkeySourceRandom.FACTOR_APPSWITCH] = 
                        -nextOptionLong("app switch events percentage");
                } else if (opt.equals("--pct-flip")) {
                    mFactors[MonkeySourceRandom.FACTOR_FLIP] =
                        -nextOptionLong("keyboard flip percentage");
                } else if (opt.equals("--pct-anyevent")) {
                    mFactors[MonkeySourceRandom.FACTOR_ANYTHING] = 
                        -nextOptionLong("any events percentage");
                } else if (opt.equals("--throttle")) {
                    mThrottle = nextOptionLong("delay (in milliseconds) to wait between events");
                } else if (opt.equals("--wait-dbg")) {
                    // do nothing - it's caught at the very start of run()
                } else if (opt.equals("--dbg-no-events")) {
                    mSendNoEvents = true;
                } else  if (opt.equals("-f")) {
                    mScriptFileName = nextOptionData();
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
            lastKeyName = MonkeySourceRandom.getLastKeyName();
        } catch (RuntimeException e) {
        }
        if (!"TAG_LAST_KEYCODE".equals(lastKeyName)) {
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

        mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
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
            mAm.setActivityController(new ActivityController());
            mNetworkMonitor.register(mAm);
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
            for (int i = 0; i< N; i++) {
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
                for (int a = 0; a < NA; a++) {
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
     * Run mCount cycles and see if we hit any crashers.
     * 
     * TODO: Meta state on keys
     * 
     * @return Returns the last cycle which executed. If the value == mCount, no errors detected.
     */
    private int runMonkeyCycles() {
        int i = 0;
        int lastKey = 0;

        boolean systemCrashed = false;

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

            // In this debugging mode, we never send any events.  This is primarily
            // here so you can manually test the package or category limits, while manually
            // exercising the system.
            if (mSendNoEvents) {
                i++;
                continue;
            }

            if ((mVerbose > 0) && (i % 100) == 0 && i != 0 && lastKey == 0) {
                System.out.println("    // Sending event #" + i);
            }

            MonkeyEvent ev = mEventSource.getNextEvent();
            if (ev != null) {
                // We don't want to count throttling as an event.
                if (!(ev instanceof MonkeyThrottleEvent)) {
                    i++;
                }
                int injectCode = ev.injectEvent(mWm, mAm, mVerbose);
                if (injectCode == MonkeyEvent.INJECT_FAIL) {
                    if (ev instanceof MonkeyKeyEvent) {
                        mDroppedKeyEvents++;
                    } else if (ev instanceof MonkeyMotionEvent) {
                        mDroppedPointerEvents++;
                    } else if (ev instanceof MonkeyFlipEvent) {
                        mDroppedFlipEvents++;
                    }
                } else if (injectCode == MonkeyEvent.INJECT_ERROR_REMOTE_EXCEPTION) {
                    systemCrashed = true;
                } else if (injectCode == MonkeyEvent.INJECT_ERROR_SECURITY_EXCEPTION) {
                    systemCrashed = !mIgnoreSecurityExceptions;
                }
            }
        }

        // If we got this far, we succeeded!
        return mCount;
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
      System.err.println("              [--pct-appswitch PERCENT] [--pct-flip PERCENT]");
      System.err.println("              [--pct-anyevent PERCENT]");
      System.err.println("              [--wait-dbg] [--dbg-no-events] [-f scriptfile]");
      System.err.println("              [-s SEED] [-v [-v] ...] [--throttle MILLISEC]");
      System.err.println("              COUNT");
  }
}
