//
// Copyright 2005 The Android Open Source Project
//
// Application class.
//
#ifndef _SIM_APPMAIN_H
#define _SIM_APPMAIN_H

#include "wx/help.h"
#include "wx/html/helpctrl.h"

#include "MainFrame.h"
#include "DeviceManager.h"
#include "Preferences.h"

#include <utils/AssetManager.h>

/* flag set from signal handler */
extern bool gWantToKill;

/*
 * Class representing the application.
 */
class MyApp : public wxApp {
public:
    MyApp(void)
        : mHelpController(NULL), mpMainFrame(NULL), mpAssetManager(NULL),
          mResetPaths(false)        // configurable; reset prefs with paths
        {}
    ~MyApp(void) 
    { 
        delete mpAssetManager;
        delete mHelpController; 
    }

    virtual bool OnInit(void);
    virtual int OnExit(void);

    wxHtmlHelpController* GetHelpController(void) const {
        return mHelpController;
    }

    Preferences* GetPrefs(void)                 { return &mPrefs; }

    /* return a pointer to the main window */
    wxWindow* GetMainFrame(void) { return mpMainFrame; }

    /* get a pointer to our Asset Manager */
    android::AssetManager* GetAssetManager(void) { return mpAssetManager; }

    /* change the asset dir; requires re-creating Asset Manager */
    void ChangeAssetDirectory(const wxString& dir);

    const wxString& GetConfigFileName(void) const { return mConfigFile; }

    wxString GetSimAssetPath()                  { return mSimAssetPath; }
    wxString GetAndroidRoot()                   { return mAndroidRoot; }
    wxString GetRuntimeExe()                    { return mRuntimeExe; }
    bool GetDebuggerOption()                    { return mDebuggerOption; }
    wxString GetDebuggerScript()                { return mDebuggerScript; }
    wxString GetAutoRunApp()                    { return mAutoRunApp; }

    void Vibrate(int vibrateOn)                 { ((MainFrame*)mpMainFrame)->Vibrate(vibrateOn); }

private:
    void SetDefaults();
    bool ParseArgs(int argc, char** argv);
    void AbsifyPath(wxString& dir);
    bool ProcessConfigFile(void);
    static void FindExe(const wxString& exeName, const wxString paths[],
        const wxString& defaultPath, wxString* pOut);

    wxHtmlHelpController*   mHelpController;

    wxWindow*       mpMainFrame;

    android::AssetManager*  mpAssetManager;

    wxString        mAndroidRoot;
    wxString        mSimAssetPath;
    wxString        mRuntimeExe;

    /* command-line options */
    wxString        mConfigFile;
    bool            mResetPaths;
    bool            mDebuggerOption;
	wxString		mDebuggerScript;
    wxString        mAutoRunApp;

    Preferences     mPrefs;
};

#endif // _SIM_APPMAIN_H
