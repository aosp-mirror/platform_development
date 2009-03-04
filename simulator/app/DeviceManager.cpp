//
// Copyright 2005 The Android Open Source Project
//
// Management of the simulated device.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"

#include "DeviceManager.h"
#include "MyApp.h"
#include "DeviceWindow.h"
#include "LogWindow.h"
#include "UserEvent.h"
#include "UserEventMessage.h"

#include "SimRuntime.h"
#include "utils.h"

#include <unistd.h>
#include <signal.h>
#include <errno.h>

#if !defined(SIGKILL)      // doesn't exist in MinGW
# if defined(SIGBREAK)
#  define SIGKILL   SIGBREAK        // intended for Ctrl-Break
# else
#  define SIGKILL   SIGABRT
# endif
#endif


/*
 * Constructor.
 */
DeviceManager::DeviceManager(void)
    : mThread(NULL), mDisplay(NULL), mNumDisplays(0), mKeyMap(NULL),
      mpStatusWindow(NULL)
{
    //printf("--- DeviceManager constructor\n");
}

/*
 * Destructor.  Snuff the thread if it's still kicking.
 */
DeviceManager::~DeviceManager(void)
{
    //printf("--- DeviceManager destructor\n");

    if (mThread != NULL && mThread->IsRunning()) {
        mThread->KillChildProcesses();
    }
    if (mThread != NULL) {
        wxThread::ExitCode code;

        printf("Sim: Waiting for old runtime thread..."); fflush(stdout);
        code = mThread->Wait();        // join the old thread
        printf("done (code=%ld)\n", (long) code);
    }
    delete mThread;
    mThread = NULL;

    delete[] mDisplay;
    free((void*)mKeyMap);
}

/*
 * Initialize the device configuration.
 *
 * "statusWindow" is where message boxes with failure messages go, usually
 * the main frame.
 */
bool DeviceManager::Init(int numDisplays, wxWindow* statusWindow)
{
    //if (IsRunning()) {
    //    fprintf(stderr, "ERROR: tried to Configure device while running\n");
    //    return false;
    //}
    assert(mDisplay == NULL);
    assert(numDisplays > 0);

    //if (mDisplay != NULL)
    //     delete[] mDisplay;

    mDisplay = new Display[numDisplays];
    mNumDisplays = numDisplays;

    mpStatusWindow = statusWindow;

    return true;
}

/*
 * Have we been initialized already?
 */
bool DeviceManager::IsInitialized(void) const
{
    return (mDisplay != NULL);
}

#if 0
/*
 * Return the Nth display.
 */
int DeviceManager::GetShmemKey(int displayIndex)
{
    assert(displayIndex >= 0 && displayIndex < mNumDisplays);
    return mDisplay[displayIndex].GetShmemKey();
}
#endif

/*
 * Define mapping between the device's display and a wxWidgets window.
 */
bool DeviceManager::SetDisplayConfig(int displayIndex, wxWindow* window,
    int width, int height, android::PixelFormat format, int refresh)
{
    assert(displayIndex >= 0 && displayIndex < mNumDisplays);

    if (!mDisplay[displayIndex].Create(displayIndex, window, width, height,
        format, refresh))
    {
        fprintf(stderr, "Sim: ERROR: unable to configure display %d\n",
            displayIndex);
        return false;
    } else {
        printf("Sim: configured display %d (w=%d h=%d f=%d re=%d)\n",
            displayIndex, width, height, format, refresh);
        return true;
    }
}

/*
 * Define the keyboard
 */
bool DeviceManager::SetKeyboardConfig(const char *keymap) {
    free((void*)mKeyMap);
    mKeyMap = strdup(keymap);
    return true;
}

/*
 * Called before the phone window dialog destroys itself.  The goal here
 * is to prevent the runtime thread from trying to draw after the phone
 * window has closed for business but before the device manager destructor
 * gets called.
 */
void DeviceManager::WindowsClosing(void)
{
    int i;

    for (i = 0; i < mNumDisplays; i++)
        mDisplay[i].Uncreate();
}

/*
 * Launch a new runtime process.  If there is an existing device manager
 * thread, we assume that it is in the process of shutting down.
 */
bool DeviceManager::StartRuntime(void)
{
    return DeviceManager::DeviceThread::LaunchProcess(mpStatusWindow);
}

/*
 * Start the runtime management thread when a runtime connects to us.  If
 * there is an existing thread, we assume that it is in the process of
 * shutting down.
 */
bool DeviceManager::StartRuntime(android::Pipe* reader, android::Pipe* writer)
{
    if (mThread != NULL) {
        wxThread::ExitCode code;

        if (mThread->IsRunning()) {
            fprintf(stderr,
                "Sim: ERROR: start requested, but thread running\n");
            return false;
        }

        // clean up old thread
        printf("Sim: Waiting for old runtime thread..."); fflush(stdout);
        code = mThread->Wait();        // join the old thread
        printf("done (code=%ld)\n", (long) code);

        delete mThread;
        mThread = NULL;
    }

    assert(mpStatusWindow != NULL);
    mThread = new DeviceThread(this, mpStatusWindow, reader, writer);
    if (mThread->Create() != wxTHREAD_NO_ERROR) {
        fprintf(stderr, "Sim: ERROR: can't create thread\n");
        return false;
    }
    mThread->Run();

    return true;
}

/*
 * Get the message stream.  Returns NULL if it doesn't exist.
 */
android::MessageStream* DeviceManager::GetStream(void)
{
    if (!IsRunning()) {
        fprintf(stderr, "Sim: ERROR: runtime thread not active\n");
        return NULL;
    }

    assert(mThread != NULL);
    android::MessageStream* pStream = mThread->GetStream();
    assert(pStream != NULL);

    if (!pStream->isReady()) {
        fprintf(stderr, "Sim: NOTE: connection to runtime not ready\n");
        return NULL;
    }

    return pStream;
}

/*
 * Stop the runtime, politely.
 *
 * We don't clean up the thread here, because it might not exit immediately.
 */
bool DeviceManager::StopRuntime(void)
{
    android::MessageStream* pStream = GetStream();
    if (pStream == NULL)
        return false;

    printf("Sim: Sending quit command\n");

    android::Message msg;
    msg.setCommand(android::Simulator::kCommandQuit, 0);
    pStream->send(&msg);
    return true;
}

/*
 * Kill the runtime as efficiently as possible.
 */
void DeviceManager::KillRuntime(void)
{
    if (mThread != NULL && mThread->IsRunning())
        mThread->KillChildProcesses();
}

#if 0
/*
 * Check if the modified time is newer than mLastModified
 */
bool DeviceManager::RefreshRuntime(void)
{
    return (IsRunning() && mThread->IsRuntimeNew());
}

/*
 * Tells the device manager that the user does not want to update
 * the runtime
 */
void DeviceManager::UserCancelledRefresh(void)
{
    mThread->UpdateLastModified();
}
#endif

/*
 * Send an event to the runtime.
 *
 * The events are defined in display_device.h.
 */
void DeviceManager::SendKeyEvent(KeyCode keyCode, bool down)
{
    android::MessageStream* pStream = GetStream();
    if (pStream == NULL)
        return;

    int event = down ? android::Simulator::kCommandKeyDown :
                       android::Simulator::kCommandKeyUp;

    //printf("Sim: sending key-%s %d\n", down ? "down" : "up", keyCode);

    android::Message msg;
    msg.setCommand(event, keyCode);
    pStream->send(&msg);
}

/*
 * Send a "touch screen" event to the runtime.
 *
 * "mode" can be "down" (we're pressing), "up" (we're lifting our finger
 * off) or "drag".
 */
void DeviceManager::SendTouchEvent(android::Simulator::TouchMode mode,
    int x, int y)
{
    android::MessageStream* pStream = GetStream();
    if (pStream == NULL)
        return;

    //printf("Sim: sending touch-%d x=%d y=%d\n", (int) mode, x, y);

    android::Message msg;
    msg.setCommandExt(android::Simulator::kCommandTouch, mode, x, y);
    pStream->send(&msg);
}

/*
 * The runtime has sent us a new frame of stuff to display.
 *
 * NOTE: we're still in the runtime management thread.  We have to pass the
 * bitmap through AddPendingEvent to get it over to the main thread.
 *
 * We have to make a copy of the data from the runtime; the easiest
 * way to do that is to convert it to a bitmap here.  However, X11 gets
 * all worked up about calls being made from multiple threads, so we're
 * better off just copying it into a buffer.
 *
 * Because we're decoupled from the runtime, there is a chance that we
 * could drop frames.  Buffering them up is probably worse, since it
 * creates the possibility that we could stall and run out of memory.
 * We could save a copy by handing the runtime a pointer to our buffer,
 * but then we'd have to mutex the runtime against the simulator window
 * Paint function.
 */
void DeviceManager::ShowFrame(int displayIndex)
{
    assert(displayIndex >= 0 && displayIndex < mNumDisplays);

    // copy the data to local storage and convert
    mDisplay[displayIndex].CopyFromShared();

    // create a user event and send it to the window
    UserEvent uev(0, (void*) displayIndex);

    wxWindow* pEventWindow = mDisplay[displayIndex].GetWindow();
    if (pEventWindow != NULL) {
        //printf("runtime has image, passing up\n");
        pEventWindow->AddPendingEvent(uev);
    } else {
        fprintf(stderr, "NOTE: runtime has image, display not available\n");
    }
}

void DeviceManager::Vibrate(int vibrateOn)
{
	((MyApp*)wxTheApp)->Vibrate(vibrateOn);
}

/*
 * Get the display data from the specified display.
 */
wxBitmap* DeviceManager::GetImageData(int displayIndex)
{
    assert(displayIndex >= 0 && displayIndex < mNumDisplays);
    return mDisplay[displayIndex].GetImageData();
}

/*
 * Send an event to all device windows
 */
void DeviceManager::BroadcastEvent(UserEvent& userEvent) {
    int numDisplays = GetNumDisplays();
    for (int i = 0; i < numDisplays; i++) {
        wxWindow* pEventWindow = mDisplay[i].GetWindow();
        if (pEventWindow != NULL) {
            pEventWindow->AddPendingEvent(userEvent);
        }
    }
}


/*
 * ===========================================================================
 *      DeviceManager::Display
 * ===========================================================================
 */

/*
 * Fill out the various interesting fields based on the parameters.
 */
bool DeviceManager::Display::Create(int displayNum, wxWindow* window,
    int width, int height, android::PixelFormat format, int refresh)
{
    //printf("DeviceManager::Display constructor\n");

    assert(window != NULL);
    if (mImageData != NULL) {
        assert(false);              // no re-init
        return false;
    }

    mDisplayNum = displayNum;
    mDisplayWindow = window;
    mWidth = width;
    mHeight = height;
    mFormat = format;
    mRefresh = refresh;

    // use a fixed key for now
    mShmemKey = GenerateKey(displayNum);
    // allocate 24bpp for now
    mpShmem = new android::Shmem;
    if (!mpShmem->create(mShmemKey, width * height * 3, true))
        return false;
    //printf("--- CREATED shmem, key=0x%08x addr=%p\n",
    //    mShmemKey, mpShmem->getAddr());

    mImageData = new unsigned char[width * height * 3];
    if (mImageData == NULL)
        return false;

    return true;
}

/*
 * The UI components are starting to shut down.  We need to do away with
 * our wxWindow pointer so that the runtime management thread doesn't try
 * to send it display update events.
 *
 * We also need to let go of our side of the shared memory, because a
 * new DeviceManager may get started up before our destructor gets called,
 * and we may be re-using the key.
 */
void DeviceManager::Display::Uncreate(void)
{
    wxMutexLocker locker(mImageDataLock);

    //printf("--- Uncreate\n");

    mDisplayWindow = NULL;

    // the "locker" mutex keeps this from hosing CopyFromShared()
    if (mpShmem != NULL) {
        //printf("--- DELETING shmem, addr=%p\n", mpShmem->getAddr());
        delete mpShmem;
        mpShmem = NULL;
    }
}

/*
 * Make a local copy of the image data.  The UI grabs this data from a
 * different thread, so we have to mutex it.
 */
void DeviceManager::Display::CopyFromShared(void)
{
    wxMutexLocker locker(mImageDataLock);

    if (mpShmem == NULL) {
        //printf("Sim: SKIP CopyFromShared\n");
        return;
    }

    //printf("Display %d: copying data from %p to %p\n",
    //    mDisplayNum, mpShmem->getAddr(), mImageData);

    /* data is always 24bpp RGB */
    mpShmem->lock();        // avoid tearing
    memcpy(mImageData, mpShmem->getAddr(), mWidth * mHeight * 3);
    mpShmem->unlock();
}

/*
 * Get the image data in the form of a newly-allocated bitmap.
 *
 * This MUST be called from the UI thread.  Creating wxBitmaps in the
 * runtime management thread will cause X11 failures (e.g.
 * "Xlib: unexpected async reply").
 */
wxBitmap* DeviceManager::Display::GetImageData(void)
{
    wxMutexLocker locker(mImageDataLock);

    assert(mImageData != NULL);

    //printf("HEY: creating tmpImage, w=%d h=%d data=%p\n",
    //    mWidth, mHeight, mImageData);

    /* create a temporary wxImage; it does not own the data */
    wxImage tmpImage(mWidth, mHeight, (unsigned char*) mImageData, true);

    /* return a new bitmap with the converted-for-display data */
    return new wxBitmap(tmpImage);
}


/*
 * ===========================================================================
 *      DeviceManager::DeviceThread
 * ===========================================================================
 */

/*
 * Some notes on process management under Linux/Mac OS X:
 *
 * We want to put the runtime into its own process group.  That way we
 * can send SIGKILL to the entire group to guarantee that we kill it and
 * all of its children.  Simply killing the sim's direct descendant doesn't
 * do what we want.  If it's a debugger, we will just orphan the runtime
 * without killing it.  Even if the runtime is our child, the children of
 * the runtime might outlive it.
 *
 * We want to be able to run the child under GDB or Valgrind, both
 * of which take input from the tty.  They need to be in the "foreground"
 * process group.  We might be debugging or valgrinding the simulator,
 * or operating in a command-line-only "headless" mode, so in that case
 * the sim front-end should actually be in the foreground group.
 *
 * Putting the runtime in the background group means it can't read input
 * from the tty (not an issue) and will generate SIGTTOU signals when it
 * writes output to the tty (easy to ignore).  The trick, then, is to
 * have the simulator and gdb/valgrind in the foreground pgrp while the
 * runtime itself is in a different group.  This group needs to be known
 * to the simulator so that it can send signals to the appropriate place.
 *
 * The solution is to have the runtime process change its process group
 * after it starts but before it creates any new processes, and then send
 * the process group ID back to the simulator.  The sim can then send
 * signals to the pgrp to ensure that we don't end up with zombies.  Any
 * "pre-launch" processes, like GDB, stay in the sim's pgrp.  This also
 * allows a consistent API for platforms that don't have fork/exec
 * (e.g. MinGW).
 *
 * This doesn't help us with interactive valgrind (e.g. --db-attach=yes),
 * because valgrind is an LD_PRELOAD shared library rather than a
 * separate process.  For that, we actually need to use termios(3) to
 * change the terminal's pgrp, or the interactive stuff just doesn't work.
 * We don't want to do that every time or attempting to debug the simulator
 * front-end will have difficulties.
 *
 * Making this even more entertaining is the fact that the simulator
 * front-end could itself be launched in the background.  It's essential
 * that we be careful about assigning a process group to the foreground,
 * and that we don't restore ourselves unless we were in the foreground to
 * begin with.
 *
 *
 * Some notes on process management under Windows (Cygwin, MinGW):
 *
 * Signals cannot be caught or ignored under MinGW.  All signals are fatal.
 *
 * Signals can be ignored under Cygwin, but not caught.
 *
 * Windows has some process group stuff (e.g. CREATE_NEW_PROCESS_GROUP flag
 * and GenerateConsoleCtrlEvent()).  Need to explore.
 *
 *
 * UPDATE: we've abandoned Mac OS and MinGW, so we now launch the runtime in
 * a separate xterm.  This avoids all tty work on our side.  We still need
 * to learn the pgrp from the child during the initial communication
 * handshake so we can do necessary cleanup.
 */


/*
 * Convert a space-delimited string into an argument vector.
 *
 * "arg" is the current arg offset.
 */
static int stringToArgv(char* mangle, const char** argv, int arg, int maxArgs)
{
    bool first = true;

    while (*mangle != '\0') {
        assert(arg < maxArgs);
        if (first) {
            argv[arg++] = mangle;
            first = false;
        }
        if (*mangle == ' ') {
            *mangle = '\0';
            first = true;
        }
        mangle++;
    }

    return arg;
}

/*
 * Launch the runtime process in its own terminal window.  Start by setting
 * up the argument vector to the runtime process.
 *
 * The last entry in the vector will be a NULL pointer.
 *
 * This is awkward and annoying because the wxWidgets strings are current
 * configured for UNICODE.
 */
/*static*/ bool DeviceManager::DeviceThread::LaunchProcess(wxWindow* statusWindow)
{
    static const char* kLaunchWrapper = "launch-wrapper";
    const int kMaxArgs = 64;
    Preferences* pPrefs;
    wxString errMsg;
    wxString runtimeExe;
    wxString debuggerExe;
	wxString debuggerScript;
    wxString valgrinderExe;
    wxString launchWrapperExe;
    wxString launchWrapperArgs;
    wxString javaAppName;
    wxString termCmd;
    wxString tmpStr;
    char gammaVal[8];
    //bool bval;
    double dval;
    bool result = false;
    bool doDebug, doValgrind, doCheckJni, doEnableSound, doEnableFakeCamera;
    const char** argv = NULL;
    int arg;
    wxCharBuffer runtimeExeTmp;
    wxCharBuffer debuggerExeTmp;
	wxCharBuffer debuggerScriptTmp;
    wxCharBuffer javaAppNameTmp;
    wxCharBuffer valgrinderExeTmp;
    wxCharBuffer termCmdTmp;
    wxCharBuffer launchWrapperExeTmp;
    wxCharBuffer launchWrapperArgsTmp;
    
    pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    if (pPrefs == NULL) {
        errMsg = wxT("Preferences were not loaded.");
        goto bail;
    }

    /*
     * Set environment variables.  This stuff should be passed through as
     * arguments, but the runtime binary currently has a disconnect
     * between main() and the VM initilization.
     *
     * TODO: remove this in favor of system properties
     */
#if 0
    // TODO: restore this
    doCheckJni = false;
    pPrefs->GetBool("check-jni", &doCheckJni);
#endif

    tmpStr.Empty();
    pPrefs->GetString("ld-assume-kernel", /*ref*/ tmpStr);
    if (tmpStr.IsEmpty()) {
        unsetenv("LD_ASSUME_KERNEL");
    } else {
        setenv("LD_ASSUME_KERNEL", tmpStr.ToAscii(), 1);
    }

    doEnableSound = false; 
    pPrefs->GetBool("enable-sound", &doEnableSound);
    if (doEnableSound)
        setenv("ANDROIDSOUND", "1", 1);

    doEnableFakeCamera = false; 
    pPrefs->GetBool("enable-fake-camera", &doEnableFakeCamera);
    if (doEnableFakeCamera)
        setenv("ANDROIDFAKECAMERA", "1", 1);

    /*
     * Set the Dalvik bootstrap class path.  Normally this is set by "init".
     */
    setenv("BOOTCLASSPATH",
        "/system/framework/core.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar",
        1);

    /*
     * Figure out where the "runtime" binary lives.
     */
    runtimeExe = ((MyApp*)wxTheApp)->GetRuntimeExe();
    assert(!runtimeExe.IsEmpty());

    //UpdateLastModified();

    /*
     * Initialize argv.
     */
    argv = new const char*[kMaxArgs];
    if (argv == NULL)
        goto bail;
    arg = 0;

    /*
     * We want to launch the runtime in its own terminal window so we don't
     * have to fight over who gets access to the controlling tty.  We allow
     * the user to specify the command they want to use to perform the
     * launch.  Here we cut it into pieces for argv.
     *
     * To make life easier here, we require that the launch command be
     * all one piece, i.e. it's not "xterm -e <stuff> -geom blah" with our
     * stuff in the middle.
     */
    termCmd.Empty();
    pPrefs->GetString("launch-command", /*ref*/ termCmd);
    if (termCmd.IsEmpty()) {
        fprintf(stderr, "Sim: WARNING: launch-command is empty\n");
    } else {
        termCmdTmp = termCmd.ToAscii();
        char* mangle = strdup(termCmdTmp);
        arg = stringToArgv(mangle, argv, arg, kMaxArgs);
    }

    /*
     * The "launch-wrapper" binary lives in the same place as the runtime.
     * This sets up LD_PRELOAD and some other environment variables.
     */
    int charIdx;

    charIdx = runtimeExe.Find('/', true);
    if (charIdx == -1) {
        launchWrapperExe = wxString::FromAscii(kLaunchWrapper);
    } else {
        launchWrapperExe = runtimeExe.Mid(0, charIdx+1);
        launchWrapperExe.Append(wxString::FromAscii(kLaunchWrapper));
    }
    printf("Sim launch wrapper: %s\n", (const char*)launchWrapperExe.ToAscii());

    argv[arg++] = launchWrapperExeTmp = launchWrapperExe.ToAscii();

    launchWrapperArgs.Empty();
    pPrefs->GetString("launch-wrapper-args", /*ref*/ launchWrapperArgs);
    if (!launchWrapperArgs.IsEmpty()) {
        launchWrapperArgsTmp = launchWrapperArgs.ToAscii();
        char* mangle = strdup(launchWrapperArgsTmp);
        arg = stringToArgv(mangle, argv, arg, kMaxArgs);
    }

    /*
     * If we're launching under GDB or valgrind, set that up.
     */
    doDebug = doValgrind = false;
    pPrefs->GetBool("debug", &doDebug);
    if (((MyApp*)wxTheApp)->GetDebuggerOption()) {
        doDebug = true;
    }
	debuggerScript = ((MyApp*)wxTheApp)->GetDebuggerScript();

    pPrefs->GetBool("valgrind", &doValgrind);
    if (doDebug || doValgrind) {

        pPrefs->GetString("debugger", /*ref*/ debuggerExe);
        pPrefs->GetString("valgrinder", /*ref*/ valgrinderExe);

        // check for empty or undefined preferences
        if (doDebug && debuggerExe.IsEmpty()) {
            errMsg = wxT("Debugger not defined.");
            goto bail;
        }
        if (doValgrind && valgrinderExe.IsEmpty()) {
            errMsg = wxT("Valgrinder not defined.");
            goto bail;
        }

        if (doValgrind) {
            argv[arg++] = valgrinderExeTmp = valgrinderExe.ToAscii();
            //argv[arg++] = "--tool=callgrind";
            argv[arg++] = "--tool=memcheck";
            argv[arg++] = "--leak-check=yes";       // check for leaks too
            argv[arg++] = "--leak-resolution=med";  // increase from 2 to 4
            argv[arg++] = "--num-callers=8";        // reduce from 12 to 8
            //argv[arg++] = "--show-reachable=yes";   // show still-reachable
            if (doDebug) {
                //mTerminalFollowsChild = true;   // interactive
                argv[arg++] = "--db-attach=yes";
            }
            //mSlowExit = true;
        } else /*doDebug*/ {
            argv[arg++] = debuggerExeTmp = debuggerExe.ToAscii();
			if (!debuggerScript.IsEmpty()) {
				argv[arg++] = "-x";
				argv[arg++] = debuggerScriptTmp = debuggerScript.ToAscii();
			}
            argv[arg++] = runtimeExeTmp = runtimeExe.ToAscii();
            argv[arg++] = "--args";
        }
    }

    /*
     * Get runtime args.
     */

    argv[arg++] = runtimeExeTmp = (const char*) runtimeExe.ToAscii();

    javaAppName = ((MyApp*)wxTheApp)->GetAutoRunApp();
    if (javaAppName.IsEmpty()) {
        if (!pPrefs->GetString("java-app-name", /*ref*/ javaAppName)) {
            javaAppName = wxT("");
        }
    }

    if (!javaAppName.IsEmpty())
    {
        argv[arg++] = "-j";
        argv[arg++] = javaAppNameTmp = (const char*) javaAppName.ToAscii();
    }

    if (pPrefs->GetDouble("gamma", &dval) && dval != 1.0) {
        snprintf(gammaVal, sizeof(gammaVal), "%.3f", dval);
        argv[arg++] = "-g";
        argv[arg++] = gammaVal;
    }

    /* finish arg set */
    argv[arg++] = NULL;

    assert(arg <= kMaxArgs);

#if 1
    printf("ARGS:\n");
    for (int i = 0; i < arg; i++)
        printf(" %d: '%s'\n", i, argv[i]);
#endif

    if (fork() == 0) {
        execvp(argv[0], (char* const*) argv);
        fprintf(stderr, "execvp '%s' failed: %s\n", argv[0], strerror(errno));
        exit(1);
    }

    /*
     * We assume success; if it didn't succeed we'll just sort of hang
     * out waiting for a connection.  There are ways to fix this (create
     * a non-close-on-exec pipe and watch to see if the other side closes),
     * but at this stage it's not worthwhile.
     */
    result = true;

    tmpStr = wxT("=== launched ");
    tmpStr += runtimeExe;
    LogWindow::PostLogMsg(tmpStr);

    assert(errMsg.IsEmpty());

bail:
    if (!errMsg.IsEmpty()) {
        assert(result == false);

        UserEventMessage* pUem = new UserEventMessage;
        pUem->CreateErrorMessage(errMsg);

        UserEvent uev(0, (void*) pUem);

        assert(statusWindow != NULL);
        statusWindow->AddPendingEvent(uev);
    }
    delete[] argv;
    return result;
}

/*
 * This is the entry point for the device thread.  The thread launches the
 * runtime process and monitors it.  When the runtime exits, the thread
 * exits.
 *
 * Because this isn't running in the UI thread, any user interaction has
 * to be channeled through "user events" to the appropriate window.
 */
void* DeviceManager::DeviceThread::Entry(void)
{
    //android::MessageStream stream;
    android::Message msg;
    wxString errMsg;
    char statusBuf[64] = "(no status)";
    int result = 1;

    /* print this so we can make sense of log messages */
    LOG(LOG_DEBUG, "", "Sim: device management thread starting (pid=%d)\n",
        getpid());

    assert(mReader != NULL && mWriter != NULL);

    /*
     * Tell the main thread that we're running.  If something fails here,
     * we'll send them a "stopped running" immediately afterward.
     */
    {
        UserEventMessage* pUem = new UserEventMessage;
        pUem->CreateRuntimeStarted();

        UserEvent uev(0, (void*) pUem);

        assert(mpStatusWindow != NULL);
        mpStatusWindow->AddPendingEvent(uev);
    }
    LogWindow::PostLogMsg(
            "==============================================================");
    LogWindow::PostLogMsg("=== runtime starting");

    /*
     * Establish contact with runtime.
     */
    if (!mStream.init(mReader, mWriter, true)) {
        errMsg = wxT("ERROR: Unable to establish communication with runtime.\n");
        goto bail;
    }

    /*
     * Tell the runtime to put itself into a new process group and set
     * itself up as the foreground process.  The latter is only really
     * necessary to make valgrind+gdb work.
     */
    msg.setCommand(android::Simulator::kCommandNewPGroup, true);
    mStream.send(&msg);

    printf("Sim: Sending hardware configuration\n");

    /*
     * Send display config.
     *
     * Right now we're just shipping a big binary blob over.
     */
    assert(android::Simulator::kValuesPerDisplay >= 5);
    int buf[1 + 1 + mpDeviceManager->GetNumDisplays() *
                    android::Simulator::kValuesPerDisplay];
    buf[0] = android::Simulator::kDisplayConfigMagic;
    buf[1] = mpDeviceManager->GetNumDisplays();
    for (int i = 0; i < mpDeviceManager->GetNumDisplays(); i++) {
        DeviceManager::Display* pDisplay = mpDeviceManager->GetDisplay(i);
        int* pBuf = &buf[2 + android::Simulator::kValuesPerDisplay * i];

        pBuf[0] = pDisplay->GetWidth();
        pBuf[1] = pDisplay->GetHeight();
        pBuf[2] = pDisplay->GetFormat();
        pBuf[3] = pDisplay->GetRefresh();
        pBuf[4] = pDisplay->GetShmemKey();
    }
    msg.setRaw((const unsigned char*)buf, sizeof(buf),
        android::Message::kCleanupNoDelete);
    mStream.send(&msg);

    /*
     * Send other hardware config.
     *
     * Examples:
     * - Available input devices.
     * - Set of buttons on device.
     * - External devices (Bluetooth, etc).
     * - Initial mode (e.g. "flipped open" vs. "flipped closed").
     */

    msg.setConfig("keycharmap", mpDeviceManager->GetKeyMap());
    mStream.send(&msg);

    /*
     * Done with config.
     */
    msg.setCommand(android::Simulator::kCommandConfigDone, 0);
    mStream.send(&msg);

    /*
     * Sit forever, waiting for messages from the runtime process.
     */
    while (1) {
        if (!mStream.recv(&msg, true)) {
            /*
             * The read failed.  This usually means the child has died.
             */
            printf("Sim: runtime process has probably died\n");
            break;
        }

        if (msg.getType() == android::Message::kTypeCommand) {
            int cmd, arg;

            if (!msg.getCommand(&cmd, &arg)) {
                fprintf(stderr, "Sim: Warning: failed unpacking command\n");
                /* keep going? */
            } else {
                switch (cmd) {
                case android::Simulator::kCommandNewPGroupCreated:
                    // runtime has moved into a separate process group
                    // (not expected for external)
                    printf("Sim: child says it's now in pgrp %d\n", arg);
                    mRuntimeProcessGroup = arg;
                    break;
                case android::Simulator::kCommandRuntimeReady:
                    // sim is up and running, do late init
                    break;
                case android::Simulator::kCommandUpdateDisplay:
                    // new frame of graphics is ready
                    //printf("RCVD display update %d\n", arg);
                    mpDeviceManager->ShowFrame(arg);
                    break;
                case android::Simulator::kCommandVibrate:
                    // vibrator on or off
                    //printf("RCVD vibrator update %d\n", arg);
                    mpDeviceManager->Vibrate(arg);
                    break;
                default:
                    printf("Sim: got unknown command %d/%d\n", cmd, arg);
                    break;
                }
            }
        } else if (msg.getType() == android::Message::kTypeLogBundle) {
            android_LogBundle bundle;

            if (!msg.getLogBundle(&bundle)) {
                fprintf(stderr, "Sim: Warning: failed unpacking logBundle\n");
                /* keep going? */
            } else {
                LogWindow::PostLogMsg(&bundle);
            }
        } else {
            printf("Sim: got unknown message type=%d\n", msg.getType());
        }
    }

    result = 0;

bail:
    printf("Sim: DeviceManager thread preparing to exit\n");

    /* kill the comm channel; should encourage runtime to die */
    mStream.close();
    delete mReader;
    delete mWriter;
    mReader = mWriter = NULL;

    /*
     * We never really did get a "friendly death" working, so just slam
     * the thing if we have the process group.
     */
    if (mRuntimeProcessGroup != 0) {
        /* kill the group, not our immediate child */
        printf("Sim: killing pgrp %d\n", (int) mRuntimeProcessGroup);
        kill(-mRuntimeProcessGroup, 9);
    }

    if (!errMsg.IsEmpty()) {
        UserEventMessage* pUem = new UserEventMessage;
        pUem->CreateErrorMessage(errMsg);

        UserEvent uev(0, (void*) pUem);
        mpStatusWindow->AddPendingEvent(uev);
    }

    /* notify the main window that the runtime has stopped */
    {
        UserEventMessage* pUem = new UserEventMessage;
        pUem->CreateRuntimeStopped();

        UserEvent uev(0, (void*) pUem);
        mpStatusWindow->AddPendingEvent(uev);
    }

    /* show exit status in log file */
    wxString exitMsg;
    exitMsg.Printf(wxT("=== runtime exiting - %s"), statusBuf);
    LogWindow::PostLogMsg(exitMsg);
    LogWindow::PostLogMsg(
        "==============================================================\n");

    /*
     * Reset system properties for future runs.
     */
    ResetProperties();

    return (void*) result;
}


/*
 * Wait for a little bit to see if the thread will exit.
 *
 * "delay" is in 0.1s increments.
 */
void DeviceManager::DeviceThread::WaitForDeath(int delay)
{
    const int kDelayUnit = 100000;
    int i;

    for (i = 0; i < delay; i++) {
        if (!IsRunning())
            return;
        usleep(kDelayUnit);
    }
}


/*
 * Kill the runtime process.  The goal is to cause our local runtime
 * management thread to exit.  If it doesn't, this will kill the thread
 * before it returns.
 */
void DeviceManager::DeviceThread::KillChildProcesses(void)
{
    if (!this->IsRunning())
        return;

    /* clear "slow exit" flag -- we're forcefully killing this thing */
    //this->mSlowExit = false;

    /*
     * Use the ChildProcess object in the thread to send signals.  There's
     * a risk that the DeviceThread will exit and destroy the object while
     * we're using it.  Using a mutex here gets a little awkward because
     * we can't put it in DeviceThread.  It's easier to make a copy of
     * ChildProcess and operate on the copy, but we have to do that very
     * carefully to avoid interfering with the communcation pipes.
     *
     * For now, we just hope for the best.  FIX this someday.
     *
     * We broadcast to the process group, which will ordinarily kill
     * everything.  If we're running with valgrind+GDB everything is in our
     * pgrp and we can't do the broadcast; if GDB alone, then only GDB is
     * in our pgrp, so the broadcast will hit everything except it.  We
     * hit the group and then hit our child for good measure.
     */
    if (mRuntimeProcessGroup != 0) {
        /* kill the group, not our immediate child */
        printf("Sim: killing pgrp %d\n", (int) mRuntimeProcessGroup);
        kill(-mRuntimeProcessGroup, 9);
        WaitForDeath(15);
    }

    /*
     * Close the communication channel.  This should cause our thread
     * to snap out of its blocking read and the runtime thread to bail
     * out the next time it tries to interact with us.  We should only
     * get here if somebody other than our direct descendant has the
     * comm channel open and our broadcast didn't work, which should
     * no longer be possible.
     */
    if (this->IsRunning()) {
        printf("Sim: killing comm channel\n");
        mStream.close();
        delete mReader;
        delete mWriter;
        mReader = mWriter = NULL;
        WaitForDeath(15);
    }

    /*
     * At this point it's possible that our DeviceThread is just wedged.
     * Kill it.
     *
     * Using the thread Kill() function can orphan resources, including
     * locks and semaphores.  There is some risk that the simulator will
     * be hosed after this.
     */
    if (this->IsRunning()) {
        fprintf(stderr, "Sim: WARNING: killing runtime thread (%ld)\n",
            (long) GetId());
        this->Kill();
        WaitForDeath(15);
    }

    /*
     * Now I'm scared.
     */
    if (this->IsRunning()) {
        fprintf(stderr, "Sim: thread won't die!\n");
    }
}


/*
 * Configure system properties for the simulated device.
 *
 * Property requests can arrive *before* the full connection to the
 * simulator is established, so we want to reset these during cleanup.
 */
void DeviceManager::DeviceThread::ResetProperties(void)
{
	wxWindow* mainFrame = ((MyApp*)wxTheApp)->GetMainFrame();
    PropertyServer* props = ((MainFrame*)mainFrame)->GetPropertyServer();

    props->ClearProperties();
    props->SetDefaultProperties();
}


#if 0
/*
 * Return true if the executable found is newer than
 * what is currently running
 */
bool DeviceManager::DeviceThread::IsRuntimeNew(void)
{
    if (mLastModified == 0) {
        /*
         * Haven't called UpdateLastModified yet, or called it but
         * couldn't stat() the executable.
         */
        return false;
    }

    struct stat status;
    if (stat(mRuntimeExe.ToAscii(), &status) == 0) {
        return (status.st_mtime > mLastModified);
    } else {
        // doesn't exist, so it can't be newer
        fprintf(stderr, "Sim: unable to stat '%s': %s\n",
            (const char*) mRuntimeExe.ToAscii(), strerror(errno));
        return false;
    }
}

/*
 * Updates mLastModified to reflect the current executables mtime
 */
void DeviceManager::DeviceThread::UpdateLastModified(void)
{
    struct stat status;
    if (stat(mRuntimeExe.ToAscii(), &status) == 0) {
        mLastModified = status.st_mtime;
    } else {
        fprintf(stderr, "Sim: unable to stat '%s': %s\n",
            (const char*) mRuntimeExe.ToAscii(), strerror(errno));
        mLastModified = 0;
    }
}
#endif

