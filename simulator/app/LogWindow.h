//
// Copyright 2005 The Android Open Source Project
//
// Window with log output.
//
#ifndef _SIM_LOG_WINDOW_H
#define _SIM_LOG_WINDOW_H

#include "PhoneData.h"
#include "UserEvent.h"
#include "LogMessage.h"
#include "LogPool.h"
#include "LogPrefsDialog.h"


/*
 * Display log output from runtime process.
 *
 * We receive the messages broken into components (date, log level, tag,
 * function name, etc.) and do the formatting ourselves.  We receive all
 * messages regardless of log level, and provide filter controls in the
 * window.
 *
 * Messages are stored in a "log pool", which has a fixed memory footprint.
 * The messages that are currently visible in the log output window are
 * also pointed at from a fixed-size display array.  Changes to output
 * format cause us to clear the display and re-show everything in the
 * display array, while changes to the output filter cause us to
 * re-evaluate the contents of the display pool.
 */
class LogWindow : public wxDialog {
public:
    LogWindow(wxWindow* parent);
    virtual ~LogWindow(void);

    /* we override this, to cope with annoying GTK behavior */
    virtual bool Show(bool show = true);

    /* return preferred size and position */
    static wxRect GetPrefWindowRect(void);

    /* handle a log message "user event" */
    void AddLogMessage(LogMessage* pLogMessage);

    /* resize the display messages array */
    void SetMaxDisplayMsgs(int max);

    /* post a message to the log; may be called from non-main thread */
    static void PostLogMsg(const android_LogBundle* pBundle);
    static void PostLogMsg(const wxString& msg);
    static void PostLogMsg(const char* msg);

private:
    void OnMove(wxMoveEvent& event);
    void OnClose(wxCloseEvent& event);
    void OnLogLevel(wxCommandEvent& event);
    void OnLogClear(wxCommandEvent& event);
    void OnLogPause(wxCommandEvent& event);
    void OnLogPrefs(wxCommandEvent& event);

    /* handle incoming log message */
    void OnUserEvent(UserEvent& event);

    void SaveWindowPrefs(void);
    void ConstructControls(void);

    void AddToDisplay(LogMessage* pLogMessage);
    void ClearDisplay(void);
    void Redisplay(void);
    void SetTextStyle(void);

    bool FilterMatches(const LogMessage* pLogMessage);

    void FormatMessage(const LogMessage* pLogMessage, 
        wxTextCtrl* pTextCtrl);

    void LogToFile(const LogMessage* pLogMessage);
    void PrepareLogFile(void);
    static void SendToWindow(LogMessage* pMessage);

    /*
     * Message pool.
     */
    LogPool     mPool;

    /*
     * Display array.  This is a fixed-size circular array that holds
     * pointers to the log messages currently displayed on screen.
     */
    LogMessage**    mDisplayArray;      // ptrs to messages currently displayed
    int         mMaxDisplayMsgs;        // max #of messages
    int         mTopPtr;                // index of top message
    int         mNextPtr;               // index of next empty slot

    bool        mPaused;                // is output paused for review?

    /*
     * Current filter.
     */
    android_LogPriority mMinPriority;   // messages at or above are shown

    /* format options */
    LogPrefsDialog::HeaderFormat mHeaderFormat;
    bool        mSingleLine;            // put whole message on one line?
    int         mExtraSpacing;          // double/triple-space messages?
    int         mPointSize;             // text point size;
    bool        mUseColor;              // colorful messages?
    bool        mFontMonospace;         // use monospace font?

    /* log file options */
    bool        mWriteFile;
    wxString    mFileName;
    bool        mTruncateOld;

    FILE*       mLogFp;

    /*
     * Window position stuff.
     */
    bool        mNewlyShown;
    wxPoint     mLastPosition;
    bool        mVisible;

    DECLARE_EVENT_TABLE()
};

#endif // _SIM_LOG_WINDOW_H
