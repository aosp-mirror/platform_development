//
// Copyright 2005 The Android Open Source Project
//
// Application entry point.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"   // needed for Windows build
#include "wx/fs_zip.h"

#include "MainFrame.h"
#include "MyApp.h"
#include "executablepath.h"

#include <stdio.h>
#include <unistd.h>
#include <getopt.h>
#include <signal.h>

#if defined(HAVE_WINDOWS_PATHS)
# include <windows.h>
#endif


/* the name of our config file */
static wxString kConfigFileName = wxT(".android.cf");

#ifdef HAVE_WINDOWS_PATHS
static wxString kExeSuffix = wxT(".exe");
#else
static wxString kExeSuffix = wxT("");
#endif

/* do we want to kill the runtime? */
bool gWantToKill = false;

/*
 * Signal handler for Ctrl-C.  Under Linux we seem to get hit twice,
 * possibly once for each thread.
 *
 * Avoid using LOG here -- it's not reentrant.  Actually, just avoid doing
 * anything here.
 *
 * Cygwin will ignore the signal but doesn't seem to call the signal
 * handler.  MinGW just kills the process.
 */
static void SignalHandler(int sigNum)
{
    printf("Sim: received signal %d (%s)\n", sigNum,
        sigNum == SIGINT ? "SIGINT" : "???");
    gWantToKill = true;
}


/* wxWidgets magic; creates appropriate main entry function */
IMPLEMENT_APP(MyApp)

/*
 * Application entry point.
 */
bool MyApp::OnInit()
{
    static wxString helpFilePath = wxT("simulator/help/unnamed.htb");

    /*
     * Parse args.
     */

    SetDefaults();
    
    char** cargv = (char**)malloc(argc * sizeof(char*));
    for (int i=0; i<argc; i++) {
	wxCharBuffer tmp = wxString(argv[i]).ToAscii();
        cargv[i] = tmp.release();
    }
    if (!ParseArgs(argc, cargv)) {
	for (int i=0; i<argc; i++)
	    free(cargv[i]);
	free(cargv);
        return FALSE;
    }
    for (int i=0; i<argc; i++)
        free(cargv[i]);
    free(cargv);
    
    if (!ProcessConfigFile())
        return FALSE;

    /*
     * (Try to) catch SIGINT (Ctrl-C).
     */
    bool trapInt = false;
    mPrefs.GetBool("trap-sigint", &trapInt);
    if (trapInt) {
        printf("Sim: catching SIGINT\n");
        signal(SIGINT, SignalHandler);
    }

    signal(SIGPIPE, SIG_IGN);

    /*
     * Set stdout to unbuffered.  This is needed for MinGW/MSYS.
     * Set stderr while we're at it.
     */
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);

    /*
     * Initialize asset manager.
     */
    mpAssetManager = NULL;
    printf("Sim: looking in '%s' for my assets\n", (const char*) mSimAssetPath.ToAscii());
    ChangeAssetDirectory(mSimAssetPath);

    /*
     * Add JPEG and PNG image handlers.
     */
    ::wxInitAllImageHandlers();

    /*
     * Set up the help file browser.  We're using wxHtmlHelpController
     * because it seems to be the only "portable" version other than
     * the "use external browser" version.
     */
    wxFileSystem::AddHandler(new wxZipFSHandler);
    mHelpController = new wxHtmlHelpController;

    wxString helpFileName;
    helpFileName = mSimAssetPath;
    helpFileName += '/';
    helpFileName += helpFilePath;
    mHelpController->Initialize(helpFileName);

    /*
     * Create the main window, which just holds some of our UI.
     */
    wxPoint pos(wxDefaultPosition);
    mPrefs.GetInt("window-main-x", &pos.x);
    mPrefs.GetInt("window-main-y", &pos.y);
    mpMainFrame = new MainFrame(wxT("Android Simulator"), pos, wxDefaultSize,
        wxDEFAULT_FRAME_STYLE);
    mpMainFrame->Show(TRUE);
    SetTopWindow(mpMainFrame);

    return TRUE;
}

/*
 * Change our asset directory.  This requires deleting the existing
 * AssetManager and creating a new one.  Note that any open Assets will
 * still be valid.
 */
void MyApp::ChangeAssetDirectory(const wxString& dir)
{
    delete mpAssetManager;
    mpAssetManager = new android::AssetManager;
    android::String8 path(dir.ToAscii());
    path.appendPath("simulator.zip");
    mpAssetManager->addAssetPath(path, NULL);
    // mpAssetManager->setLocale(xxx);
    mpAssetManager->setVendor("google");
}


/*
 * App is shutting down.  Save the config file.
 */
int MyApp::OnExit(void)
{
    if (mPrefs.GetDirty()) {
        printf("Sim: writing config file to '%s'\n",
            (const char*) mConfigFile.ToAscii());
        if (!mPrefs.Save(mConfigFile.ToAscii())) {
            fprintf(stderr, "Sim: ERROR: prefs save to '%s' failed\n",
                (const char*) mConfigFile.ToAscii());
        }
    }

    return 0;
}

static ssize_t
find_last_slash(const wxString& s)
{
    int slash = s.Last('/');
    if (slash < 0) {
        slash = s.Last('\\');
    }
    return slash;
}


/*
 * Set some default parameters
 */
void MyApp::SetDefaults()
{
    mDebuggerOption = false;

    /* Get the path to this executable, which should
     * end in something like "/host/linux-x86/bin/simulator".
     * (The full path may begin with something like "out"
     * or "out/debug".)
     */
    char exepath[PATH_MAX];
    executablepath(exepath);
    wxString out = wxString::FromAscii(exepath);

    /* Get the path to the root host directory;  e.g., "out/host".
     * We can do this by removing the last three slashes
     * and everything after/between them ("/linux-x86/bin/simulator").
     */
    for (int i = 0; i < 3; i++) {
        int slash = find_last_slash(out);
        assert(slash >= 0);
        out.Truncate(slash);
    }

    /* Get the location of the assets directory; something like
     * "out/host/common/sim-assets"
     */
    mSimAssetPath = out;
    mSimAssetPath.Append(wxT("/common/sim-assets"));

    /* Get the location of the simulated device filesystem.
     * We can't reliably predict this based on the executable
     * location, so try to get it from the environment.
     */
    char *envOut = getenv("ANDROID_PRODUCT_OUT");
    if (envOut == NULL) {
        fprintf(stderr,
                "WARNING: $ANDROID_PRODUCT_OUT not set in environment\n");
        envOut = "";
    }

    // the root of the android stuff
    mAndroidRoot = wxString::FromAscii(envOut);
    mAndroidRoot.Append(wxT("/system"));
    
    // where runtime is
    mRuntimeExe = mAndroidRoot;
    mRuntimeExe.Append(wxT("/bin/runtime"));
    mRuntimeExe.Append(kExeSuffix);
    
    printf("mAndroidRoot='%s'\n", (const char*) mAndroidRoot.ToAscii());
    printf("mSimAssetPath='%s'\n", (const char*) mSimAssetPath.ToAscii());
}


/*
 * Parse command-line arguments.
 *
 * Returns "false" if we have a parsing error.
 */
bool MyApp::ParseArgs(int argc, char** argv)
{
    int ic;

    opterr = 0;     // don't complain about unrecognized options

    if (false) {
        printf("MyApp args:\n");
        for (int i = 0; i < argc; i++)
            printf("  %2d: '%s'\n", i, (const char*) argv[i]);
    }

    while (1) {
        ic = getopt(argc, argv, "tj:da:f:rx:");
        if (ic < 0)
            break;

        switch (ic) {
        case 'j':
            mAutoRunApp = wxString::FromAscii(optarg);
            break;
        case 't':
            mAutoRunApp = wxT("com.android.testharness.RunAll");
            break;
        case 'd':
            mDebuggerOption = true;
            break;
        case 'x':
            mDebuggerScript = wxString::FromAscii(optarg);
            mDebuggerOption = true;     // force debug if a script is being used
            break;
        case 'a':       // simulator asset dir
            mSimAssetPath = wxString::FromAscii(optarg);
            break;
        case 'f':       // simulator config file
            mConfigFile = wxString::FromAscii(optarg);
            break;
        case 'r':       // reset path-based options to defaults
            mResetPaths = true;
            break;
        default:
            fprintf(stderr, "WARNING: unknown sim option '%c'\n", ic);
            break;
        }
    }

    return true;
}


/*
 * Convert a path to absolute form, if needed.
 *
 * String manipulation would be more efficient than system calls, but
 * less reliable.
 *
 * We need to use GetCurrentDirectory() under Windows because, under
 * Cygwin, some wxWidgets features require "C:" paths rather than
 * local-rooted paths.  Probably needed for stand-alone MinGW too.
 */
void MyApp::AbsifyPath(wxString& dir)
{
    char oldDir[512], newDir[512];
    wxString newDirStr;

    // We still need to do this under Cygwin even if the path is
    // already absolute.
    //if (dir[0] == '/' || dir[0] == '\\')
    //    return;

    if (getcwd(oldDir, sizeof(oldDir)) == NULL) {
        fprintf(stderr, "getcwd() failed\n");
        return;
    }

    if (chdir(dir.ToAscii()) == 0) {
#if defined(HAVE_WINDOWS_PATHS)
        DWORD dwRet;
        dwRet = GetCurrentDirectory(sizeof(newDir), newDir);
        if (dwRet == 0 || dwRet > sizeof(newDir))
            sprintf(newDir, "GET_DIR_FAILED %lu", dwRet);
#else
        if (getcwd(newDir, sizeof(newDir)) == NULL)
            strcpy(newDir, "GET_DIR_FAILED");
#endif
        newDirStr = wxString::FromAscii(newDir);
        chdir(oldDir);
    } else {
        fprintf(stderr, "WARNING: unable to chdir to '%s' from '%s'\n",
            (const char*) dir.ToAscii(), oldDir);
        newDirStr = dir;
    }

    //dir = "c:/dev/cygwin";
    //dir += newDirStr;
    dir = newDirStr;
}


/*
 * Load and process our configuration file.
 */
bool MyApp::ProcessConfigFile(void)
{
    wxString homeConfig;
    bool configLoaded = false;

    if (getenv("HOME") != NULL) {
        homeConfig = wxString::FromAscii(getenv("HOME"));
        homeConfig += '/';
        homeConfig += kConfigFileName;
    } else {
        homeConfig = wxT("./");
        homeConfig += kConfigFileName;
    }

    /*
     * Part 1: read the config file.
     */

    if (mConfigFile.Length() > 0) {
        /*
         * Read from specified config file.  We absolutify the path
         * first so that we're guaranteed to be hitting the same file
         * even if the cwd changes.
         */
        if (access(mConfigFile.ToAscii(), R_OK) != 0) {
            fprintf(stderr, "ERROR: unable to open '%s'\n",
                (const char*) mConfigFile.ToAscii());
            return false;
        }
        if (!mPrefs.Load(mConfigFile.ToAscii())) {
            fprintf(stderr, "Failed loading config file '%s'\n",
                (const char*) mConfigFile.ToAscii());
            return false;
        } else {
            configLoaded = true;
        }
    } else {
        /*
         * Try ./android.cf, then $HOME/android.cf.  If we find one and
         * read it successfully, save the name in mConfigFile.
         */
        {
            wxString fileName;

            fileName = wxT(".");
            AbsifyPath(fileName);
            fileName += wxT("/");
            fileName += kConfigFileName;

            if (access(fileName.ToAscii(), R_OK) == 0) {
                if (mPrefs.Load(fileName.ToAscii())) {
                    mConfigFile = fileName;
                    configLoaded = true;
                } else {
                    /* damaged config files are always fatal */
                    fprintf(stderr, "Failed loading config file '%s'\n",
                        (const char*) fileName.ToAscii());
                    return false;
                }
            }
        }
        if (!configLoaded) {
            if (homeConfig.Length() > 0) {
                if (access(homeConfig.ToAscii(), R_OK) == 0) {
                    if (mPrefs.Load(homeConfig.ToAscii())) {
                        mConfigFile = homeConfig;
                        configLoaded = true;
                    } else {
                        /* damaged config files are always fatal */
                        fprintf(stderr, "Failed loading config file '%s'\n",
                            (const char*) homeConfig.ToAscii());
                        return false;
                    }
                }
            }
        }

    }

    /* if we couldn't find one to load, create a new one in $HOME */
    if (!configLoaded) {
        mConfigFile = homeConfig;
        if (!mPrefs.Create()) {
            fprintf(stderr, "prefs creation failed\n");
            return false;
        }
    }

    /*
     * Part 2: reset some entries if requested.
     *
     * If you want to reset local items (like paths to binaries) without
     * disrupting other options, specifying the "reset" flag will cause
     * some entries to be removed, and new defaults generated below.
     */

    if (mResetPaths) {
        if (mPrefs.RemovePref("debugger"))
            printf("  removed pref 'debugger'\n");
        if (mPrefs.RemovePref("valgrinder"))
            printf("  removed pref 'valgrinder'\n");
    }

    /*
     * Find GDB.
     */
    if (!mPrefs.Exists("debugger")) {
        static wxString paths[] = {
            wxT("/bin"), wxT("/usr/bin"), wxString()
        };
        wxString gdbPath;

        FindExe(wxT("gdb"), paths, wxT("/usr/bin/gdb"), &gdbPath);
        mPrefs.SetString("debugger", gdbPath.ToAscii());
    }


    /*
     * Find Valgrind.  It currently only exists in Linux, and is installed
     * in /usr/bin/valgrind by default on our systems.  The default version
     * is old and sometimes fails, so look for a newer version.
     */
    if (!mPrefs.Exists("valgrinder")) {
        static wxString paths[] = {
            wxT("/home/fadden/local/bin"), wxT("/usr/bin"), wxString()
        };
        wxString valgrindPath;

        FindExe(wxT("valgrind"), paths, wxT("/usr/bin/valgrind"), &valgrindPath);
        mPrefs.SetString("valgrinder", valgrindPath.ToAscii());
    }

    /*
     * Set misc options.
     */
    if (!mPrefs.Exists("auto-power-on"))
        mPrefs.SetBool("auto-power-on", true);
    if (!mPrefs.Exists("gamma"))
        mPrefs.SetDouble("gamma", 1.0);

    if (mPrefs.GetDirty()) {
        printf("Sim: writing config file to '%s'\n",
            (const char*) mConfigFile.ToAscii());
        if (!mPrefs.Save(mConfigFile.ToAscii())) {
            fprintf(stderr, "Sim: ERROR: prefs save to '%s' failed\n",
                (const char*) mConfigFile.ToAscii());
        }
    }

    return true;
}

/*
 * Find an executable by searching in several places.
 */
/*static*/ void MyApp::FindExe(const wxString& exeName, const wxString paths[],
    const wxString& defaultPath, wxString* pOut)
{
    wxString exePath;
    wxString slashExe;

    slashExe = wxT("/");
    slashExe += exeName;
    slashExe += kExeSuffix;

    while (!(*paths).IsNull()) {
        wxString tmp;

        tmp = *paths;
        tmp += slashExe;
        if (access(tmp.ToAscii(), X_OK) == 0) {
            printf("Sim: Found '%s' in '%s'\n", (const char*) exeName.ToAscii(), 
                    (const char*) tmp.ToAscii());
            *pOut = tmp;
            return;
        }

        paths++;
    }

    printf("Sim: Couldn't find '%s', defaulting to '%s'\n",
        (const char*) exeName.ToAscii(), (const char*) defaultPath.ToAscii());
    *pOut = defaultPath;
}

