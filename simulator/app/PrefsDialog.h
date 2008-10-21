//
// Copyright 2005 The Android Open Source Project
//
// Prefs modal dialog.
//
#ifndef _SIM_PREFS_DIALOG_H
#define _SIM_PREFS_DIALOG_H

/*
 * Declaration of preferences dialog.  This class defines the outer
 * wrapper as well as all of the pages.
 */
class PrefsDialog : public wxDialog {
    //DECLARE_CLASS(PrefsDialog)    // shown in book, but causes link problems
    DECLARE_EVENT_TABLE()

public:
    PrefsDialog(wxWindow* parent);
    virtual ~PrefsDialog();

    void CreateControls(void);

    wxString    mConfigFile;

private:
    bool TransferDataToWindow(void);
    bool TransferDataFromWindow(void);
    bool TransferDataFromControls(void);
    void LoadPreferences(void);
    
    wxPanel* CreateSimulatorPage(wxBookCtrlBase* parent);
    wxPanel* CreateRuntimePage(wxBookCtrlBase* parent);

    /* main notebook; for aesthetic reasons we may want a Choicebook */
    wxNotebook    mNotebook;

    /* Global simulator options */
    wxString    mDebugger;
    wxString    mValgrinder;
    bool        mAutoPowerOn;
    // log window size?

    /* Global runtime options */
    double      mGammaCorrection;
    bool        mEnableSound;
    bool        mEnableFakeCamera;
    int         mLogLevel;

    enum {
        kMinWidth = 300,        // minimum prefs dialog width, in pixels
    };
};

#endif // _SIM_PREFS_DIALOG_H
