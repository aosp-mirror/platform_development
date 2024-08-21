/*
 * Copyright 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.monkey;

import static android.view.InputDevice.SOURCE_TOUCHSCREEN;
import static android.view.MotionEvent.TOOL_TYPE_FINGER;

import android.app.ActivityManager;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.input.VirtualTouchEvent;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.view.Display;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Application that injects random key events and other actions into the system.
 */
public class Monkey {
    static {
        System.loadLibrary("monkey_jni");
    }

    private static native IBinder createNativeService(int width, int height);

    /**
     * Monkey Debugging/Dev Support
     * <p>
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

    /** Ignore any native crashes while running? */
    private boolean mIgnoreNativeCrashes;

    /** Send no events. Use with long throttle-time to watch user operations */
    private boolean mSendNoEvents;

    /** This is set when we would like to abort the running of the monkey. */
    private boolean mAbort;

    /**
     * Count each event as a cycle. Set to false for scripts so that each time
     * through the script increments the count.
     */
    private boolean mCountEvents = true;

    /**
     * This is set by the ActivityController thread to request collection of ANR
     * trace files
     */
    private boolean mRequestAnrTraces = false;

    /**
     * This is set by the ActivityController thread to request a
     * "dumpsys meminfo"
     */
    private boolean mRequestDumpsysMemInfo = false;

    /**
     * This is set by the ActivityController thread to request a
     * bugreport after ANR
     */
    private boolean mRequestAnrBugreport = false;

    /**
     * This is set by the ActivityController thread to request a
     * bugreport after a system watchdog report
     */
    private boolean mRequestWatchdogBugreport = false;

    /**
     * Synchronization for the ActivityController callback to block
     * until we are done handling the reporting of the watchdog error.
     */
    private boolean mWatchdogWaiting = false;

    /**
     * This is set by the ActivityController thread to request a
     * bugreport after java application crash
     */
    private boolean mRequestAppCrashBugreport = false;

    /**Request the bugreport based on the mBugreportFrequency. */
    private boolean mGetPeriodicBugreport = false;

    /**
     * Request the bugreport based on the mBugreportFrequency.
     */
    private boolean mRequestPeriodicBugreport = false;

    /** Bugreport frequency. */
    private long mBugreportFrequency = 10;

    /** Failure process name */
    private String mReportProcessName;

    /**
     * This is set by the ActivityController thread to request a "procrank"
     */
    private boolean mRequestProcRank = false;

    /** Kill the process after a timeout or crash. */
    private boolean mKillProcessAfterError;

    /** Generate hprof reports before/after monkey runs */
    private boolean mGenerateHprof;

    /** If set, only match error if this text appears in the description text. */
    private String mMatchDescription;

    /** Package denylist file. */
    private String mPkgBlacklistFile;

    /** Package allowlist file. */
    private String mPkgWhitelistFile;

    /** Categories we are allowed to launch **/
    private ArrayList<String> mMainCategories = new ArrayList<String>();

    /**
     * Applications we can switch to, as well as their corresponding categories.
     */
    private HashMap<ComponentName, String> mMainApps = new HashMap<>();

    /** The delay between event inputs **/
    long mThrottle = 0;

    /**
     * Whether to randomize each throttle (0-mThrottle ms) inserted between
     * events.
     */
    boolean mRandomizeThrottle = false;

    /** The number of iterations **/
    int mCount = 1000;

    /** The random number seed **/
    long mSeed = 0;

    /** The random number generator **/
    Random mRandom = null;

    private final IMonkey mMonkeyService = createMonkeyService();

    /** Dropped-event statistics **/
    long mDroppedKeyEvents = 0;

    long mDroppedPointerEvents = 0;

    long mDroppedTrackballEvents = 0;

    long mDroppedFlipEvents = 0;

    long mDroppedRotationEvents = 0;

    /** The delay between user actions. This is for the scripted monkey. **/
    long mProfileWaitTime = 5000;

    /** Device idle time. This is for the scripted monkey. **/
    long mDeviceSleepTime = 30000;

    boolean mRandomizeScript = false;

    boolean mScriptLog = false;

    /** Capture bugreprot whenever there is a crash. **/
    private boolean mRequestBugreport = false;

    /** a filename to the setup script (if any) */
    private String mSetupFileName = null;

    /** filenames of the script (if any) */
    private ArrayList<String> mScriptFileNames = new ArrayList<String>();

    /** a TCP port to listen on for remote commands. */
    private int mServerPort = -1;

    private static final File TOMBSTONES_PATH = new File("/data/tombstones");

    private static final String TOMBSTONE_PREFIX = "tombstone_";

    private static int NUM_READ_TOMBSTONE_RETRIES = 5;

    private HashSet<Long> mTombstones = null;

    float[] mFactors = new float[MonkeySourceRandom.FACTORZ_COUNT];

    MonkeyEventSource mEventSource;

    private MonkeyNetworkMonitor mNetworkMonitor = new MonkeyNetworkMonitor();

    private boolean mPermissionTargetSystem = false;

    // information on the current activity.
    public static Intent currentIntent;

    public static String currentPackage;

    private static IMonkey createMonkeyService() {
        // Get the width and height of the touchscreen on the default display
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(
                Display.DEFAULT_DISPLAY);
        final int width = display.getWidth();
        final int height = display.getHeight();
        return IMonkey.Stub.asInterface(createNativeService(width, height));
    }

    /**
     * Monitor operations happening in the system.
     */
    private class ActivityController extends IActivityController.Stub {
        public boolean activityStarting(Intent intent, String pkg) {
            final boolean allow = isActivityStartingAllowed(intent, pkg);
            if (mVerbose > 0) {
                // StrictMode's disk checks end up catching this on
                // userdebug/eng builds due to PrintStream going to a
                // FileOutputStream in the end (perhaps only when
                // redirected to a file?)  So we allow disk writes
                // around this region for the monkey to minimize
                // harmless dropbox uploads from monkeys.
                StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
                Logger.out.println("    // " + (allow ? "Allowing" : "Rejecting")
                        + " start of " + intent + " in package " + pkg);
                StrictMode.setThreadPolicy(savedPolicy);
            }
            currentPackage = pkg;
            currentIntent = intent;
            return allow;
        }

        private boolean isActivityStartingAllowed(Intent intent, String pkg) {
            if (MonkeyUtils.getPackageFilter().checkEnteringPackage(pkg)) {
                return true;
            }
            if (DEBUG_ALLOW_ANY_STARTS != 0) {
                return true;
            }
            // In case the activity is launching home and the default launcher
            // package is disabled, allow anyway to prevent ANR (see b/38121026)
            final Set<String> categories = intent.getCategories();
            if (intent.getAction() == Intent.ACTION_MAIN && categories != null
                    && categories.contains(Intent.CATEGORY_HOME)) {
                try {
                    final ResolveInfo resolveInfo = mPm.resolveIntent(
                            intent, intent.getType(), 0, ActivityManager.getCurrentUser());
                    final String launcherPackage = resolveInfo.activityInfo.packageName;
                    if (pkg.equals(launcherPackage)) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Logger.err.println("** Failed talking with package manager!");
                    return false;
                }
            }
            return false;
        }

        public boolean activityResuming(String pkg) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger.out.println("    // activityResuming(" + pkg + ")");
            boolean allow =
                    MonkeyUtils.getPackageFilter().checkEnteringPackage(pkg)
                            || (DEBUG_ALLOW_ANY_RESTARTS != 0);
            if (!allow) {
                if (mVerbose > 0) {
                    Logger.out.println("    // " + (allow ? "Allowing" : "Rejecting")
                            + " resume of package " + pkg);
                }
            }
            currentPackage = pkg;
            StrictMode.setThreadPolicy(savedPolicy);
            return allow;
        }

        public boolean appCrashed(String processName, int pid, String shortMsg,
                                String longMsg, long timeMillis,
                                String stackTrace) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger.err.println("// CRASH: " + processName + " (pid " + pid + ")");
            Logger.err.println("// Short Msg: " + shortMsg);
            Logger.err.println("// Long Msg: " + longMsg);
            Logger.err.println("// Build Label: " + Build.FINGERPRINT);
            Logger.err.println("// Build Changelist: " + Build.VERSION.INCREMENTAL);
            Logger.err.println("// Build Time: " + Build.TIME);
            Logger.err.println("// " + stackTrace.replace("\n", "\n// "));
            StrictMode.setThreadPolicy(savedPolicy);

            if (mMatchDescription == null
                    || shortMsg.contains(mMatchDescription)
                    || longMsg.contains(mMatchDescription)
                    || stackTrace.contains(mMatchDescription)) {
                if (!mIgnoreCrashes || mRequestBugreport) {
                    synchronized (Monkey.this) {
                        if (!mIgnoreCrashes) {
                            mAbort = true;
                        }
                        if (mRequestBugreport) {
                            mRequestAppCrashBugreport = true;
                            mReportProcessName = processName;
                        }
                    }
                    return !mKillProcessAfterError;
                }
            }
            return false;
        }

        public int appEarlyNotResponding(String processName, int pid, String annotation) {
            return 0;
        }

        public int appNotResponding(String processName, int pid,
                                    String processStats) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger.err.println("// NOT RESPONDING: " + processName + " (pid " + pid + ")");
            Logger.err.println(processStats);
            StrictMode.setThreadPolicy(savedPolicy);

            if (mMatchDescription == null || processStats.contains(mMatchDescription)) {
                synchronized (Monkey.this) {
                    mRequestAnrTraces = true;
                    mRequestDumpsysMemInfo = true;
                    mRequestProcRank = true;
                    if (mRequestBugreport) {
                        mRequestAnrBugreport = true;
                        mReportProcessName = processName;
                    }
                }
                if (!mIgnoreTimeouts) {
                    synchronized (Monkey.this) {
                        mAbort = true;
                    }
                }
            }

            return (mKillProcessAfterError) ? -1 : 1;
        }

        public int systemNotResponding(String message) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger.err.println("// WATCHDOG: " + message);
            StrictMode.setThreadPolicy(savedPolicy);

            synchronized (Monkey.this) {
                if (mMatchDescription == null || message.contains(mMatchDescription)) {
                    if (!mIgnoreCrashes) {
                        mAbort = true;
                    }
                    if (mRequestBugreport) {
                        mRequestWatchdogBugreport = true;
                    }
                }
                mWatchdogWaiting = true;
            }
            synchronized (Monkey.this) {
                while (mWatchdogWaiting) {
                    try {
                        Monkey.this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return (mKillProcessAfterError) ? -1 : 1;
        }
    }

    /**
     * Run the procrank tool to insert system status information into the debug
     * report.
     */
    private void reportProcRank() {
        commandLineReport("procrank", "procrank");
    }

    /**
     * Dump the most recent ANR trace. Wait about 5 seconds first, to let the
     * asynchronous report writing complete.
     */
    private void reportAnrTraces() {
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
        }

        // The /data/anr directory might have multiple files, dump the most
        // recent of those files.
        File[] recentTraces = new File("/data/anr/").listFiles();
        if (recentTraces != null) {
            File mostRecent = null;
            long mostRecentMtime = 0;
            for (File trace : recentTraces) {
                final long mtime = trace.lastModified();
                if (mtime > mostRecentMtime) {
                    mostRecentMtime = mtime;
                    mostRecent = trace;
                }
            }

            if (mostRecent != null) {
                commandLineReport("anr traces", "cat " + mostRecent.getAbsolutePath());
            }
        }
    }

    /**
     * Run "dumpsys meminfo"
     * <p>
     * NOTE: You cannot perform a dumpsys call from the ActivityController
     * callback, as it will deadlock. This should only be called from the main
     * loop of the monkey.
     */
    private void reportDumpsysMemInfo() {
        commandLineReport("meminfo", "dumpsys meminfo");
    }

    /**
     * Print report from a single command line.
     * <p>
     * TODO: Use ProcessBuilder & redirectErrorStream(true) to capture both
     * streams (might be important for some command lines)
     *
     * @param reportName Simple tag that will print before the report and in
     *            various annotations.
     * @param command Command line to execute.
     */
    private void commandLineReport(String reportName, String command) {
        Logger.err.println(reportName + ":");
        Runtime rt = Runtime.getRuntime();

         try (Writer logOutput = mRequestBugreport ?
            new BufferedWriter(new FileWriter(new File(Environment
                    .getLegacyExternalStorageDirectory(), reportName), true)) : null) {
            // Process must be fully qualified here because android.os.Process
            // is used elsewhere
            java.lang.Process p = Runtime.getRuntime().exec(command);

            // pipe everything from process stdout -> System.err
            InputStream inStream = p.getInputStream();
            InputStreamReader inReader = new InputStreamReader(inStream);
            BufferedReader inBuffer = new BufferedReader(inReader);
            String s;
            while ((s = inBuffer.readLine()) != null) {
                if (mRequestBugreport) {
                    try {
                        // When no space left on the device the write will
                        // occurs an I/O exception, so we needed to catch it
                        // and continue to read the data of the sync pipe to
                        // aviod the bugreport hang forever.
                        logOutput.write(s);
                        logOutput.write("\n");
                    } catch (IOException e) {
                        while(inBuffer.readLine() != null) {}
                        Logger.err.println(e.toString());
                        break;
                    }
                } else {
                    Logger.err.println(s);
                }
            }

            int status = p.waitFor();
            Logger.err.println("// " + reportName + " status was " + status);
        } catch (Exception e) {
            Logger.err.println("// Exception from " + reportName + ":");
            Logger.err.println(e.toString());
        }
    }

    // Write the numbe of iteration to the log
    private void writeScriptLog(int count) {
        // TO DO: Add the script file name to the log.
        try {
            Writer output = new BufferedWriter(new FileWriter(new File(
                    Environment.getLegacyExternalStorageDirectory(), "scriptlog.txt"), true));
            output.write("iteration: " + count + " time: "
                    + MonkeyUtils.toCalendarTime(System.currentTimeMillis()) + "\n");
            output.close();
        } catch (IOException e) {
            Logger.err.println(e.toString());
        }
    }

    // Write the bugreport to the sdcard.
    private void getBugreport(String reportName) {
        reportName += MonkeyUtils.toCalendarTime(System.currentTimeMillis());
        String bugreportName = reportName.replaceAll("[ ,:]", "_");
        commandLineReport(bugreportName + ".txt", "bugreport");
    }

    // UncaughtExceptionHandler set by RuntimeInit will report crash to system_server, which
    // is not necessary for monkey and even causes deadlock. So we override it.
    private static class KillSelfHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }

    /**
     * Command-line entry point.
     *
     * @param args The command-line arguments
     */
    public static void main(String[] args) {
        // Set the process name showing in "ps" or "top"
        Process.setArgV0("com.android.commands.monkey");

        Thread.setDefaultUncaughtExceptionHandler(new KillSelfHandler());
        Logger.err.println("args: " + Arrays.toString(args));
        int resultCode = (new Monkey()).run(args);
        System.exit(resultCode);
    }

    /**
     * Run the command!
     *
     * @param args The command-line arguments
     * @return Returns a posix-style result code. 0 for no error.
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
        for (String a: args) {
            Logger.err.println(" arg: \"" + a + "\"");
        }
        mNextArg = 0;

        // set a positive value, indicating none of the factors is provided yet
        for (int i = 0; i < MonkeySourceRandom.FACTORZ_COUNT; i++) {
            mFactors[i] = 1.0f;
        }

        if (!processOptions()) {
            return -1;
        }

        if (!loadPackageLists()) {
            return -1;
        }

        // now set up additional data in preparation for launch
        if (mMainCategories.size() == 0) {
            mMainCategories.add(Intent.CATEGORY_LAUNCHER);
            mMainCategories.add(Intent.CATEGORY_MONKEY);
        }

        if (mSeed == 0) {
            mSeed = System.currentTimeMillis() + System.identityHashCode(this);
        }

        if (mVerbose > 0) {
            Logger.out.println(":Monkey: seed=" + mSeed + " count=" + mCount);
            MonkeyUtils.getPackageFilter().dump();
            if (mMainCategories.size() != 0) {
                Iterator<String> it = mMainCategories.iterator();
                while (it.hasNext()) {
                    Logger.out.println(":IncludeCategory: " + it.next());
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

        mRandom = new Random(mSeed);

        if (mScriptFileNames != null && mScriptFileNames.size() == 1) {
            // script mode, ignore other options
            mEventSource = new MonkeySourceScript(mRandom, mScriptFileNames.get(0), mThrottle,
                    mRandomizeThrottle, mProfileWaitTime, mDeviceSleepTime);
            mEventSource.setVerbose(mVerbose);

            mCountEvents = false;
        } else if (mScriptFileNames != null && mScriptFileNames.size() > 1) {
            if (mSetupFileName != null) {
                mEventSource = new MonkeySourceRandomScript(mSetupFileName,
                        mScriptFileNames, mThrottle, mRandomizeThrottle, mRandom,
                        mProfileWaitTime, mDeviceSleepTime, mRandomizeScript);
                mCount++;
            } else {
                mEventSource = new MonkeySourceRandomScript(mScriptFileNames,
                        mThrottle, mRandomizeThrottle, mRandom,
                        mProfileWaitTime, mDeviceSleepTime, mRandomizeScript);
            }
            mEventSource.setVerbose(mVerbose);
            mCountEvents = false;
        } else if (mServerPort != -1) {
            try {
                mEventSource = new MonkeySourceNetwork(mServerPort);
            } catch (IOException e) {
                Logger.out.println("Error binding to network socket.");
                return -5;
            }
            mCount = Integer.MAX_VALUE;
        } else {
            // random source by default
            if (mVerbose >= 2) { // check seeding performance
                Logger.out.println("// Seeded: " + mSeed);
            }
            mEventSource = new MonkeySourceRandom(mRandom, mMainApps,
                    mThrottle, mRandomizeThrottle, mPermissionTargetSystem);
            mEventSource.setVerbose(mVerbose);
            // set any of the factors that has been set
            for (int i = 0; i < MonkeySourceRandom.FACTORZ_COUNT; i++) {
                if (mFactors[i] <= 0.0f) {
                    ((MonkeySourceRandom) mEventSource).setFactors(i, mFactors[i]);
                }
            }

            // in random mode, we start with a random activity
            ((MonkeySourceRandom) mEventSource).generateActivity();
        }

        // validate source generator
        if (!mEventSource.validate()) {
            return -5;
        }

        // If we're profiling, do it immediately before/after the main monkey
        // loop
        if (mGenerateHprof) {
            signalPersistentProcesses();
        }

        mNetworkMonitor.start();
        int crashedAtCycle = 0;
        try {
            crashedAtCycle = runMonkeyCycles();
        } finally {
            // Release the rotation lock if it's still held and restore the
            // original orientation.
            new MonkeyRotationEvent(Surface.ROTATION_0, false).injectEvent(
                mWm, mAm, mVerbose);
        }
        mNetworkMonitor.stop();

        synchronized (this) {
            if (mRequestAnrTraces) {
                reportAnrTraces();
                mRequestAnrTraces = false;
            }
            if (mRequestAnrBugreport){
                Logger.out.println("Print the anr report");
                getBugreport("anr_" + mReportProcessName + "_");
                mRequestAnrBugreport = false;
            }
            if (mRequestWatchdogBugreport) {
                Logger.out.println("Print the watchdog report");
                getBugreport("anr_watchdog_");
                mRequestWatchdogBugreport = false;
            }
            if (mRequestAppCrashBugreport){
                getBugreport("app_crash" + mReportProcessName + "_");
                mRequestAppCrashBugreport = false;
            }
            if (mRequestDumpsysMemInfo) {
                reportDumpsysMemInfo();
                mRequestDumpsysMemInfo = false;
            }
            if (mRequestPeriodicBugreport){
                getBugreport("Bugreport_");
                mRequestPeriodicBugreport = false;
            }
            if (mWatchdogWaiting) {
                mWatchdogWaiting = false;
                notifyAll();
            }
        }

        if (mGenerateHprof) {
            signalPersistentProcesses();
            if (mVerbose > 0) {
                Logger.out.println("// Generated profiling reports in /data/misc");
            }
        }

        try {
            mAm.setActivityController(null, true);
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
            Logger.out.println(":Dropped: keys=" + mDroppedKeyEvents
                    + " pointers=" + mDroppedPointerEvents
                    + " trackballs=" + mDroppedTrackballEvents
                    + " flips=" + mDroppedFlipEvents
                    + " rotations=" + mDroppedRotationEvents);
        }

        // report network stats
        mNetworkMonitor.dump();

        if (crashedAtCycle < mCount - 1) {
            Logger.err.println("** System appears to have crashed at event " + crashedAtCycle
                    + " of " + mCount + " using seed " + mSeed);
            return crashedAtCycle;
        } else {
            if (mVerbose > 0) {
                Logger.out.println("// Monkey finished");
            }
            return 0;
        }
    }

    private int injectEvent(MonkeyEvent ev) {
        if (ev instanceof MonkeyMotionEvent motion) {
            final MotionEvent motionEvent = motion.getMotionEventForInjection();
            if (motionEvent.isFromSource(SOURCE_TOUCHSCREEN)) {
                return injectTouchEvent(motionEvent);
            }
        }
        return ev.injectEvent(mWm, mAm, mVerbose);
    }

    private boolean writeTouchEvent(MotionEvent motion, int pointerIndex,
                                    int action) throws RemoteException {
        int pointerId = motion.getPointerId(pointerIndex);
        return mMonkeyService.writeTouchEvent(
            pointerId, TOOL_TYPE_FINGER, action, motion.getX(pointerIndex),
            motion.getY(pointerIndex), motion.getPressure(pointerIndex),
            motion.getTouchMajor(pointerIndex), motion.getEventTime());
    }

    private int injectTouchEvent(MotionEvent event) {
        try {
            boolean success = true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    // Send a single write
                    int pointerIndex = event.getActionIndex();
                    success &= writeTouchEvent(event, pointerIndex, VirtualTouchEvent.ACTION_DOWN);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    // Iterate through pointers and send them all!
                    // This would currently result in multiple motion events to be sent, because
                    // there's an EV_SYN being sent after each "writeTouchEvent". To avoid this,
                    // we would ideally only send EV_SYN at the last call to "writeTouchEvent".
                    // However, the current approach should be good enough, and batching
                    // should help lump the events together, anyways.
                    for (int pointerIndex = 0; pointerIndex < event.getPointerCount();
                            pointerIndex++) {
                        success &= writeTouchEvent(event, pointerIndex,
                                VirtualTouchEvent.ACTION_MOVE);
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP: {
                    // Send a single write
                    int pointerIndex = event.getActionIndex();
                    int resolvedAction = VirtualTouchEvent.ACTION_UP;
                    if ((event.getFlags() & MotionEvent.FLAG_CANCELED) != 0) {
                        resolvedAction = VirtualTouchEvent.ACTION_CANCEL;
                    }
                    success &= writeTouchEvent(event, pointerIndex, resolvedAction);
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    // Cancel all pointers!
                    for (int pointerIndex = 0; pointerIndex < event.getPointerCount();
                            pointerIndex++) {
                        success &= writeTouchEvent(event, pointerIndex,
                                                    VirtualTouchEvent.ACTION_CANCEL);
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Unhandled action " + event);
            }
            return success ? MonkeyEvent.INJECT_SUCCESS : MonkeyEvent.INJECT_FAIL;
        } catch (RemoteException exc) {
            exc.rethrowAsRuntimeException();
        }
        return MonkeyEvent.INJECT_FAIL;
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
            Set<String> validPackages = new HashSet<>();
            while ((opt = nextOption()) != null) {
                if (opt.equals("-s")) {
                    mSeed = nextOptionLong("Seed");
                } else if (opt.equals("-p")) {
                    validPackages.add(nextOptionData());
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
                } else if (opt.equals("--ignore-native-crashes")) {
                    mIgnoreNativeCrashes = true;
                } else if (opt.equals("--kill-process-after-error")) {
                    mKillProcessAfterError = true;
                } else if (opt.equals("--hprof")) {
                    mGenerateHprof = true;
                } else if (opt.equals("--match-description")) {
                    mMatchDescription = nextOptionData();
                } else if (opt.equals("--pct-touch")) {
                    int i = MonkeySourceRandom.FACTOR_TOUCH;
                    mFactors[i] = -nextOptionLong("touch events percentage");
                } else if (opt.equals("--pct-motion")) {
                    int i = MonkeySourceRandom.FACTOR_MOTION;
                    mFactors[i] = -nextOptionLong("motion events percentage");
                } else if (opt.equals("--pct-trackball")) {
                    int i = MonkeySourceRandom.FACTOR_TRACKBALL;
                    mFactors[i] = -nextOptionLong("trackball events percentage");
                } else if (opt.equals("--pct-rotation")) {
                    int i = MonkeySourceRandom.FACTOR_ROTATION;
                    mFactors[i] = -nextOptionLong("screen rotation events percentage");
                } else if (opt.equals("--pct-syskeys")) {
                    int i = MonkeySourceRandom.FACTOR_SYSOPS;
                    mFactors[i] = -nextOptionLong("system (key) operations percentage");
                } else if (opt.equals("--pct-nav")) {
                    int i = MonkeySourceRandom.FACTOR_NAV;
                    mFactors[i] = -nextOptionLong("nav events percentage");
                } else if (opt.equals("--pct-majornav")) {
                    int i = MonkeySourceRandom.FACTOR_MAJORNAV;
                    mFactors[i] = -nextOptionLong("major nav events percentage");
                } else if (opt.equals("--pct-appswitch")) {
                    int i = MonkeySourceRandom.FACTOR_APPSWITCH;
                    mFactors[i] = -nextOptionLong("app switch events percentage");
                } else if (opt.equals("--pct-flip")) {
                    int i = MonkeySourceRandom.FACTOR_FLIP;
                    mFactors[i] = -nextOptionLong("keyboard flip percentage");
                } else if (opt.equals("--pct-anyevent")) {
                    int i = MonkeySourceRandom.FACTOR_ANYTHING;
                    mFactors[i] = -nextOptionLong("any events percentage");
                } else if (opt.equals("--pct-pinchzoom")) {
                    int i = MonkeySourceRandom.FACTOR_PINCHZOOM;
                    mFactors[i] = -nextOptionLong("pinch zoom events percentage");
                } else if (opt.equals("--pct-permission")) {
                    int i = MonkeySourceRandom.FACTOR_PERMISSION;
                    mFactors[i] = -nextOptionLong("runtime permission toggle events percentage");
                } else if (opt.equals("--pkg-blacklist-file")) {
                    mPkgBlacklistFile = nextOptionData();
                } else if (opt.equals("--pkg-whitelist-file")) {
                    mPkgWhitelistFile = nextOptionData();
                } else if (opt.equals("--throttle")) {
                    mThrottle = nextOptionLong("delay (in milliseconds) to wait between events");
                } else if (opt.equals("--randomize-throttle")) {
                    mRandomizeThrottle = true;
                } else if (opt.equals("--wait-dbg")) {
                    // do nothing - it's caught at the very start of run()
                } else if (opt.equals("--dbg-no-events")) {
                    mSendNoEvents = true;
                } else if (opt.equals("--port")) {
                    mServerPort = (int) nextOptionLong("Server port to listen on for commands");
                } else if (opt.equals("--setup")) {
                    mSetupFileName = nextOptionData();
                } else if (opt.equals("-f")) {
                    mScriptFileNames.add(nextOptionData());
                } else if (opt.equals("--profile-wait")) {
                    mProfileWaitTime = nextOptionLong("Profile delay" +
                                " (in milliseconds) to wait between user action");
                } else if (opt.equals("--device-sleep-time")) {
                    mDeviceSleepTime = nextOptionLong("Device sleep time" +
                                                      "(in milliseconds)");
                } else if (opt.equals("--randomize-script")) {
                    mRandomizeScript = true;
                } else if (opt.equals("--script-log")) {
                    mScriptLog = true;
                } else if (opt.equals("--bugreport")) {
                    mRequestBugreport = true;
                } else if (opt.equals("--periodic-bugreport")){
                    mGetPeriodicBugreport = true;
                    mBugreportFrequency = nextOptionLong("Number of iterations");
                } else if (opt.equals("--permission-target-system")){
                    mPermissionTargetSystem = true;
                } else if (opt.equals("-h")) {
                    showUsage();
                    return false;
                } else {
                    Logger.err.println("** Error: Unknown option: " + opt);
                    showUsage();
                    return false;
                }
            }
            MonkeyUtils.getPackageFilter().addValidPackages(validPackages);
        } catch (RuntimeException ex) {
            Logger.err.println("** Error: " + ex.toString());
            showUsage();
            return false;
        }

        // If a server port hasn't been specified, we need to specify
        // a count
        if (mServerPort == -1) {
            String countStr = nextArg();
            if (countStr == null) {
                Logger.err.println("** Error: Count not specified");
                showUsage();
                return false;
            }

            try {
                mCount = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                Logger.err.println("** Error: Count is not a number: \"" + countStr + "\"");
                showUsage();
                return false;
            }
        }

        return true;
    }

    /**
     * Load a list of package names from a file.
     *
     * @param fileName The file name, with package names separated by new line.
     * @param list The destination list.
     * @return Returns false if any error occurs.
     */
    private static boolean loadPackageListFromFile(String fileName, Set<String> list) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String s;
            while ((s = reader.readLine()) != null) {
                s = s.trim();
                if ((s.length() > 0) && (!s.startsWith("#"))) {
                    list.add(s);
                }
            }
        } catch (IOException ioe) {
            Logger.err.println("" + ioe);
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    Logger.err.println("" + ioe);
                }
            }
        }
        return true;
    }

    /**
     * Load package denylist or allowlist (if specified).
     *
     * @return Returns false if any error occurs.
     */
    private boolean loadPackageLists() {
        if (((mPkgWhitelistFile != null) || (MonkeyUtils.getPackageFilter().hasValidPackages()))
                && (mPkgBlacklistFile != null)) {
            Logger.err.println("** Error: you can not specify a package blacklist "
                    + "together with a whitelist or individual packages (via -p).");
            return false;
        }
        Set<String> validPackages = new HashSet<>();
        if ((mPkgWhitelistFile != null)
                && (!loadPackageListFromFile(mPkgWhitelistFile, validPackages))) {
            return false;
        }
        MonkeyUtils.getPackageFilter().addValidPackages(validPackages);
        Set<String> invalidPackages = new HashSet<>();
        if ((mPkgBlacklistFile != null)
                && (!loadPackageListFromFile(mPkgBlacklistFile, invalidPackages))) {
            return false;
        }
        MonkeyUtils.getPackageFilter().addInvalidPackages(invalidPackages);
        return true;
    }

    /**
     * Check for any internal configuration (primarily build-time) errors.
     *
     * @return Returns true if ready to rock.
     */
    private boolean checkInternalConfiguration() {
        return true;
    }

    /**
     * Attach to the required system interfaces.
     *
     * @return Returns true if all system interfaces were available.
     */
    private boolean getSystemInterfaces() {
        mAm = ActivityManager.getService();
        if (mAm == null) {
            Logger.err.println("** Error: Unable to connect to activity manager; is the system "
                    + "running?");
            return false;
        }

        mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        if (mWm == null) {
            Logger.err.println("** Error: Unable to connect to window manager; is the system "
                    + "running?");
            return false;
        }

        mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (mPm == null) {
            Logger.err.println("** Error: Unable to connect to package manager; is the system "
                    + "running?");
            return false;
        }

        try {
            mAm.setActivityController(new ActivityController(), true);
            mNetworkMonitor.register(mAm);
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
            return false;
        }

        return true;
    }

    /**
     * Using the restrictions provided (categories & packages), generate a list
     * of activities that we can actually switch to.
     *
     * @return Returns true if it could successfully build a list of target
     *         activities
     */
    private boolean getMainApps() {
        try {
            final int N = mMainCategories.size();
            for (int i = 0; i < N; i++) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                String category = mMainCategories.get(i);
                if (category.length() > 0) {
                    intent.addCategory(category);
                }
                List<ResolveInfo> mainApps = mPm.queryIntentActivities(intent, null, 0,
                        ActivityManager.getCurrentUser()).getList();
                if (mainApps == null || mainApps.size() == 0) {
                    Logger.err.println("// Warning: no activities found for category " + category);
                    continue;
                }
                if (mVerbose >= 2) { // very verbose
                    Logger.out.println("// Selecting main activities from category " + category);
                }
                final int NA = mainApps.size();
                for (int a = 0; a < NA; a++) {
                    ResolveInfo r = mainApps.get(a);
                    String packageName = r.activityInfo.applicationInfo.packageName;
                    if (MonkeyUtils.getPackageFilter().checkEnteringPackage(packageName)) {
                        if (mVerbose >= 2) { // very verbose
                            Logger.out.println("//   + Using main activity " + r.activityInfo.name
                                    + " (from package " + packageName + ")");
                        }
                        mMainApps.put(
                            new ComponentName(packageName, r.activityInfo.name), category);
                    } else {
                        if (mVerbose >= 3) { // very very verbose
                            Logger.out.println("//   - NOT USING main activity "
                                    + r.activityInfo.name + " (from package " + packageName + ")");
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with package manager!");
            return false;
        }

        if (mMainApps.size() == 0) {
            Logger.out.println("** No activities found to run, monkey aborted.");
            return false;
        }

        return true;
    }

    /**
     * Run mCount cycles and see if we hit any crashers.
     * <p>
     * TODO: Meta state on keys
     *
     * @return Returns the last cycle which executed. If the value == mCount, no
     *         errors detected.
     */
    private int runMonkeyCycles() {
        int eventCounter = 0;
        int cycleCounter = 0;

        boolean shouldReportAnrTraces = false;
        boolean shouldReportDumpsysMemInfo = false;
        boolean shouldAbort = false;
        boolean systemCrashed = false;

        try {
            // TO DO : The count should apply to each of the script file.
            while (!systemCrashed && cycleCounter < mCount) {
                synchronized (this) {
                    if (mRequestProcRank) {
                        reportProcRank();
                        mRequestProcRank = false;
                    }
                    if (mRequestAnrTraces) {
                        mRequestAnrTraces = false;
                        shouldReportAnrTraces = true;
                    }
                    if (mRequestAnrBugreport){
                        getBugreport("anr_" + mReportProcessName + "_");
                        mRequestAnrBugreport = false;
                    }
                    if (mRequestWatchdogBugreport) {
                        Logger.out.println("Print the watchdog report");
                        getBugreport("anr_watchdog_");
                        mRequestWatchdogBugreport = false;
                    }
                    if (mRequestAppCrashBugreport){
                        getBugreport("app_crash" + mReportProcessName + "_");
                        mRequestAppCrashBugreport = false;
                    }
                    if (mRequestPeriodicBugreport){
                        getBugreport("Bugreport_");
                        mRequestPeriodicBugreport = false;
                    }
                    if (mRequestDumpsysMemInfo) {
                        mRequestDumpsysMemInfo = false;
                        shouldReportDumpsysMemInfo = true;
                    }
                    if (mMonitorNativeCrashes) {
                        // first time through, when eventCounter == 0, just set up
                        // the watcher (ignore the error)
                        if (checkNativeCrashes() && (eventCounter > 0)) {
                            Logger.out.println("** New native crash detected.");
                            if (mRequestBugreport) {
                                getBugreport("native_crash_");
                            }
                            mAbort = mAbort || !mIgnoreNativeCrashes || mKillProcessAfterError;
                        }
                    }
                    if (mAbort) {
                        shouldAbort = true;
                    }
                    if (mWatchdogWaiting) {
                        mWatchdogWaiting = false;
                        notifyAll();
                    }
                }

                // Report ANR, dumpsys after releasing lock on this.
                // This ensures the availability of the lock to Activity controller's appNotResponding
                if (shouldReportAnrTraces) {
                    shouldReportAnrTraces = false;
                    reportAnrTraces();
                }

                if (shouldReportDumpsysMemInfo) {
                    shouldReportDumpsysMemInfo = false;
                    reportDumpsysMemInfo();
                }

                if (shouldAbort) {
                    shouldAbort = false;
                    Logger.out.println("** Monkey aborted due to error.");
                    Logger.out.println("Events injected: " + eventCounter);
                    return eventCounter;
                }

                // In this debugging mode, we never send any events. This is
                // primarily here so you can manually test the package or category
                // limits, while manually exercising the system.
                if (mSendNoEvents) {
                    eventCounter++;
                    cycleCounter++;
                    continue;
                }

                if ((mVerbose > 0) && (eventCounter % 100) == 0 && eventCounter != 0) {
                    String calendarTime = MonkeyUtils.toCalendarTime(System.currentTimeMillis());
                    long systemUpTime = SystemClock.elapsedRealtime();
                    Logger.out.println("    //[calendar_time:" + calendarTime + " system_uptime:"
                            + systemUpTime + "]");
                    Logger.out.println("    // Sending event #" + eventCounter);
                }

                MonkeyEvent ev = mEventSource.getNextEvent();
                if (ev != null) {
                    final int injectCode = injectEvent(ev);
                    if (injectCode == MonkeyEvent.INJECT_FAIL) {
                        Logger.out.println("    // Injection Failed");
                        if (ev instanceof MonkeyKeyEvent) {
                            mDroppedKeyEvents++;
                        } else if (ev instanceof MonkeyMotionEvent) {
                            mDroppedPointerEvents++;
                        } else if (ev instanceof MonkeyFlipEvent) {
                            mDroppedFlipEvents++;
                        } else if (ev instanceof MonkeyRotationEvent) {
                            mDroppedRotationEvents++;
                        }
                    } else if (injectCode == MonkeyEvent.INJECT_ERROR_REMOTE_EXCEPTION) {
                        systemCrashed = true;
                        Logger.err.println("** Error: RemoteException while injecting event.");
                    } else if (injectCode == MonkeyEvent.INJECT_ERROR_SECURITY_EXCEPTION) {
                        systemCrashed = !mIgnoreSecurityExceptions;
                        if (systemCrashed) {
                            Logger.err.println("** Error: SecurityException while injecting event.");
                        }
                    }

                    // Don't count throttling as an event.
                    if (!(ev instanceof MonkeyThrottleEvent)) {
                        eventCounter++;
                        if (mCountEvents) {
                            cycleCounter++;
                        }
                    }
                } else {
                    if (!mCountEvents) {
                        cycleCounter++;
                        writeScriptLog(cycleCounter);
                        //Capture the bugreport after n iteration
                        if (mGetPeriodicBugreport) {
                            if ((cycleCounter % mBugreportFrequency) == 0) {
                                mRequestPeriodicBugreport = true;
                            }
                        }
                    } else {
                        // Event Source has signaled that we have no more events to process
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            Logger.error("** Error: A RuntimeException occurred:", e);
        }
        Logger.out.println("Events injected: " + eventCounter);
        return eventCounter;
    }

    /**
     * Send SIGNAL_USR1 to all processes. This will generate large (5mb)
     * profiling reports in data/misc, so use with care.
     */
    private void signalPersistentProcesses() {
        try {
            mAm.signalPersistentProcesses(Process.SIGNAL_USR1);

            synchronized (this) {
                wait(2000);
            }
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
        } catch (InterruptedException e) {
        }
    }

    /**
     * Watch for appearance of new tombstone files, which indicate native
     * crashes.
     *
     * @return Returns true if new files have appeared in the list
     */
    private boolean checkNativeCrashes() {
        String[] tombstones = TOMBSTONES_PATH.list();

        // shortcut path for usually empty directory, so we don't waste even
        // more objects
        if (tombstones == null || tombstones.length == 0) {
            mTombstones = null;
            return false;
        }

        boolean result = false;

        // use set logic to look for new files
        HashSet<Long> newStones = new HashSet<Long>();
        for (String t : tombstones) {
            if (t.startsWith(TOMBSTONE_PREFIX)) {
                File f = new File(TOMBSTONES_PATH, t);
                newStones.add(f.lastModified());
                if (mTombstones == null || !mTombstones.contains(f.lastModified())) {
                    result = true;
                    waitForTombstoneToBeWritten(Paths.get(TOMBSTONES_PATH.getPath(), t));
                    Logger.out.println("** New tombstone found: " + f.getAbsolutePath()
                                       + ", size: " + f.length());
                }
            }
        }

        // keep the new list for the next time
        mTombstones = newStones;

        return result;
    }

    /**
     * Wait for the given tombstone file to be completely written.
     *
     * @param path The path of the tombstone file.
     */
    private void waitForTombstoneToBeWritten(Path path) {
        boolean isWritten = false;
        try {
            // Ensure file is done writing by sleeping and comparing the previous and current size
            for (int i = 0; i < NUM_READ_TOMBSTONE_RETRIES; i++) {
                long size = Files.size(path);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { }
                if (size > 0 && Files.size(path) == size) {
                    //File size is bigger than 0 and hasn't changed
                    isWritten = true;
                    break;
                }
            }
        } catch (IOException e) {
            Logger.err.println("Failed to get tombstone file size: " + e.toString());
        }
        if (!isWritten) {
            Logger.err.println("Incomplete tombstone file.");
            return;
        }
    }

    /**
     * Return the next command line option. This has a number of special cases
     * which closely, but not exactly, follow the POSIX command line options
     * patterns:
     *
     * <pre>
     * -- means to stop processing additional options
     * -z means option z
     * -z ARGS means option z with (non-optional) arguments ARGS
     * -zARGS means option z with (optional) arguments ARGS
     * --zz means option zz
     * --zz ARGS means option zz with (non-optional) arguments ARGS
     * </pre>
     *
     * Note that you cannot combine single letter options; -abc != -a -b -c
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
        Logger.err.println("arg=\"" + arg + "\" mCurArgData=\"" + mCurArgData + "\" mNextArg="
                + mNextArg + " argwas=\"" + mArgs[mNextArg-1] + "\"" + " nextarg=\"" +
                mArgs[mNextArg] + "\"");
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
        Logger.err.println("data=\"" + data + "\"");
        mNextArg++;
        return data;
    }

    /**
     * Returns a long converted from the next data argument, with error handling
     * if not available.
     *
     * @param opt The name of the option.
     * @return Returns a long converted from the argument.
     */
    private long nextOptionLong(final String opt) {
        long result;
        try {
            result = Long.parseLong(nextOptionData());
        } catch (NumberFormatException e) {
            Logger.err.println("** Error: " + opt + " is not a number");
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
        StringBuffer usage = new StringBuffer();
        usage.append("usage: monkey [-p ALLOWED_PACKAGE [-p ALLOWED_PACKAGE] ...]\n");
        usage.append("              [-c MAIN_CATEGORY [-c MAIN_CATEGORY] ...]\n");
        usage.append("              [--ignore-crashes] [--ignore-timeouts]\n");
        usage.append("              [--ignore-security-exceptions]\n");
        usage.append("              [--monitor-native-crashes] [--ignore-native-crashes]\n");
        usage.append("              [--kill-process-after-error] [--hprof]\n");
        usage.append("              [--match-description TEXT]\n");
        usage.append("              [--pct-touch PERCENT] [--pct-motion PERCENT]\n");
        usage.append("              [--pct-trackball PERCENT] [--pct-syskeys PERCENT]\n");
        usage.append("              [--pct-nav PERCENT] [--pct-majornav PERCENT]\n");
        usage.append("              [--pct-appswitch PERCENT] [--pct-flip PERCENT]\n");
        usage.append("              [--pct-anyevent PERCENT] [--pct-pinchzoom PERCENT]\n");
        usage.append("              [--pct-permission PERCENT]\n");
        usage.append("              [--pkg-blacklist-file PACKAGE_BLACKLIST_FILE]\n");
        usage.append("              [--pkg-whitelist-file PACKAGE_WHITELIST_FILE]\n");
        usage.append("              [--wait-dbg] [--dbg-no-events]\n");
        usage.append("              [--setup scriptfile] [-f scriptfile [-f scriptfile] ...]\n");
        usage.append("              [--port port]\n");
        usage.append("              [-s SEED] [-v [-v] ...]\n");
        usage.append("              [--throttle MILLISEC] [--randomize-throttle]\n");
        usage.append("              [--profile-wait MILLISEC]\n");
        usage.append("              [--device-sleep-time MILLISEC]\n");
        usage.append("              [--randomize-script]\n");
        usage.append("              [--script-log]\n");
        usage.append("              [--bugreport]\n");
        usage.append("              [--periodic-bugreport]\n");
        usage.append("              [--permission-target-system]\n");
        usage.append("              COUNT\n");
        Logger.err.println(usage.toString());
    }
}
