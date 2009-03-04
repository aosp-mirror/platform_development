//
// Copyright 2005 The Android Open Source Project
//
// Log preferences modal dialog.
//
#ifndef _SIM_LOG_PREFS_DIALOG_H
#define _SIM_LOG_PREFS_DIALOG_H

/*
 * Declaration of log preferences dialog.  This class defines the outer
 * wrapper as well as all of the pages.
 */
class LogPrefsDialog : public wxDialog {
    DECLARE_EVENT_TABLE()

public:
    LogPrefsDialog(wxWindow* parent);
    virtual ~LogPrefsDialog(void);

    void CreateControls(void);

    /* these correspond to radio buttons */
    typedef enum HeaderFormat {
        kHFFull = 0,
        kHFBrief,
        kHFMinimal,
        kHFInternal,        // special -- used for internally generated msgs
    };

    /*
     * Values edited in the preference pages.  By Windows convention,
     * these are public.
     */
    /* format options */
    HeaderFormat mHeaderFormat;
    bool        mSingleLine;        // put whole message on one line?
    int         mExtraSpacing;      // double/triple-space messages?
    int         mPointSize;         // text size
    bool        mUseColor;          // colorful messages?
    bool        mFontMonospace;     // use monospace font?

    /* limit options */
    int         mDisplayMax;
    int         mPoolSizeKB;

    /* file options */
    bool        mWriteFile;
    wxString    mFileName;
    bool        mTruncateOld;

private:
    bool TransferDataToWindow(void);
    bool TransferDataFromWindow(void);

    wxPanel* CreateFormatPage(wxBookCtrlBase* parent);
    wxPanel* CreateLimitsPage(wxBookCtrlBase* parent);
    wxPanel* CreateFilesPage(wxBookCtrlBase* parent);

    void OnWriteFile(wxCommandEvent& event);
    void EnableFileControls(bool enable);

    /* main notebook; for aesthetic reasons we may want a Choicebook */
    wxNotebook    mNotebook;

    enum {
        kMinWidth = 300,        // minimum prefs dialog width, in pixels
    };
};

#endif // _SIM_LOG_PREFS_DIALOG_H
