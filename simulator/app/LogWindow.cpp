//
// Copyright 2005 The Android Open Source Project
//
// Display runtime log output.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"   // needed for Windows build
#include "wx/dcbuffer.h"

#include "LogWindow.h"
#include "LogMessage.h"
#include "LogPrefsDialog.h"
#include "MyApp.h"
#include "Preferences.h"
#include "Resource.h"
#include "UserEventMessage.h"

#include <errno.h>

static int android_snprintfBuffer(char** pBuf, int bufLen, const char* format, ...);
static int android_vsnprintfBuffer(char** pBuf, int bufLen, const char* format, va_list args);


using namespace android;

#if 0   // experiment -- works on Win32, but not with GTK
class MyTextCtrl : public wxTextCtrl {
public:
    MyTextCtrl(wxWindow* parent, wxWindowID id, const wxString& value,
        const wxPoint& pos, const wxSize& size, int style = 0)
        : wxTextCtrl(parent, id, value, pos, size, style)
        {
            printf("***************** MyTextCtrl!\n");
        }

    void OnScroll(wxScrollWinEvent& event);
    void OnScrollBottom(wxScrollWinEvent& event);

private:
    DECLARE_EVENT_TABLE()
};

BEGIN_EVENT_TABLE(MyTextCtrl, wxTextCtrl)
    EVT_SCROLLWIN(MyTextCtrl::OnScroll)
    EVT_SCROLLWIN_BOTTOM(MyTextCtrl::OnScrollBottom)
END_EVENT_TABLE()

void MyTextCtrl::OnScroll(wxScrollWinEvent& event)
{
    printf("OnScroll!\n");
}

void MyTextCtrl::OnScrollBottom(wxScrollWinEvent& event)
{
    printf("OnScrollBottom!\n");
}
#endif


BEGIN_EVENT_TABLE(LogWindow, wxDialog)
    EVT_CLOSE(LogWindow::OnClose)
    EVT_MOVE(LogWindow::OnMove)
    EVT_COMBOBOX(IDC_LOG_LEVEL, LogWindow::OnLogLevel)
    EVT_BUTTON(IDC_LOG_CLEAR, LogWindow::OnLogClear)
    EVT_BUTTON(IDC_LOG_PAUSE, LogWindow::OnLogPause)
    EVT_BUTTON(IDC_LOG_PREFS, LogWindow::OnLogPrefs)
END_EVENT_TABLE()

/*
 * Information about log levels.
 *
 * Each entry here corresponds to an entry in the combo box.  The first
 * letter of each name should be unique.
 */
static const struct {
    wxString    name;
    android_LogPriority priority;
} gLogLevels[] = {
    { wxT("Verbose"),    ANDROID_LOG_VERBOSE },
    { wxT("Debug"),      ANDROID_LOG_DEBUG },
    { wxT("Info"),       ANDROID_LOG_INFO },
    { wxT("Warn"),       ANDROID_LOG_WARN },
    { wxT("Error"),      ANDROID_LOG_ERROR }
};


/*
 * Create a new LogWindow.  This should be a child of the main frame.
 */
LogWindow::LogWindow(wxWindow* parent)
    : wxDialog(parent, wxID_ANY, wxT("Log Output"), wxDefaultPosition,
        wxDefaultSize,
        wxCAPTION | wxSYSTEM_MENU | wxCLOSE_BOX | wxRESIZE_BORDER),
      mDisplayArray(NULL), mMaxDisplayMsgs(0), mPaused(false),
      mMinPriority(ANDROID_LOG_VERBOSE),
      mHeaderFormat(LogPrefsDialog::kHFFull),
      mSingleLine(false), mExtraSpacing(0), mPointSize(10), mUseColor(true),
      mFontMonospace(true), mWriteFile(false), mTruncateOld(true), mLogFp(NULL),
      mNewlyShown(false), mLastPosition(wxDefaultPosition), mVisible(false)
{
    ConstructControls();

    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();

    int poolSize = 10240;       // 10MB
    pPrefs->GetInt("log-pool-size-kbytes", &poolSize);
    assert(poolSize > 0);
    mPool.Resize(poolSize * 1024);

    mMaxDisplayMsgs = 1000;
    pPrefs->GetInt("log-display-msg-count", &mMaxDisplayMsgs);
    assert(mMaxDisplayMsgs > 0);
    mDisplayArray = new LogMessage*[mMaxDisplayMsgs];
    memset(mDisplayArray, 0, sizeof(LogMessage*) * mMaxDisplayMsgs);
    mTopPtr = -1;
    mNextPtr = 0;

    int tmpInt = (int) mHeaderFormat;
    pPrefs->GetInt("log-header-format", &tmpInt);
    mHeaderFormat = (LogPrefsDialog::HeaderFormat) tmpInt;
    pPrefs->GetBool("log-single-line", &mSingleLine);
    pPrefs->GetInt("log-extra-spacing", &mExtraSpacing);
    pPrefs->GetInt("log-point-size", &mPointSize);
    pPrefs->GetBool("log-use-color", &mUseColor);
    pPrefs->SetBool("log-font-monospace", &mFontMonospace);
    SetTextStyle();

    mFileName = wxT("/tmp/android-log.txt");
    pPrefs->GetBool("log-write-file", &mWriteFile);
    pPrefs->GetString("log-filename", /*ref*/mFileName);
    pPrefs->GetBool("log-truncate-old", &mTruncateOld);

    PrepareLogFile();
}

/*
 * Destroy everything we own.
 */
LogWindow::~LogWindow(void)
{
    ClearDisplay();
    delete[] mDisplayArray;

    if (mLogFp != NULL)
        fclose(mLogFp);
}

/*
 * Set the text style, based on our preferences.
 */
void LogWindow::SetTextStyle(void)
{
    wxTextCtrl* pTextCtrl;
    pTextCtrl = (wxTextCtrl*) FindWindow(IDC_LOG_TEXT);
    wxTextAttr style;
    style = pTextCtrl->GetDefaultStyle();

    if (mFontMonospace) {
        wxFont font(mPointSize, wxFONTFAMILY_MODERN, wxFONTSTYLE_NORMAL,
            wxFONTWEIGHT_NORMAL);
        style.SetFont(font);
    } else {
        wxFont font(mPointSize, wxFONTFAMILY_SWISS, wxFONTSTYLE_NORMAL,
            wxFONTWEIGHT_NORMAL);
        style.SetFont(font);
    }

    pTextCtrl->SetDefaultStyle(style);
}

/*
 * Set up the goodies in the window.
 *
 * Also initializes mMinPriority.
 */
void LogWindow::ConstructControls(void)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    wxPanel* base = new wxPanel(this, wxID_ANY);
    wxBoxSizer* masterSizer = new wxBoxSizer(wxVERTICAL);
    wxBoxSizer* indentSizer = new wxBoxSizer(wxHORIZONTAL);
    wxBoxSizer* configPrioritySizer = new wxBoxSizer(wxHORIZONTAL);
    wxGridSizer* configSizer = new wxGridSizer(4, 1);

    /*
     * Configure log level combo box.
     */
    wxComboBox* logLevel;
    int defaultLogLevel = 1;
    pPrefs->GetInt("log-display-level", &defaultLogLevel);
    logLevel = new wxComboBox(base, IDC_LOG_LEVEL, wxT(""),
        wxDefaultPosition, wxDefaultSize, 0, NULL,
        wxCB_READONLY /*| wxSUNKEN_BORDER*/);
    for (int i = 0; i < NELEM(gLogLevels); i++) {
        logLevel->Append(gLogLevels[i].name);
        logLevel->SetClientData(i, (void*) gLogLevels[i].priority);
    }
    logLevel->SetSelection(defaultLogLevel);
    mMinPriority = gLogLevels[defaultLogLevel].priority;

    /*
     * Set up stuff at the bottom, starting with the options
     * at the bottom left.
     */
    configPrioritySizer->Add(new wxStaticText(base, wxID_ANY, wxT("Log level:"),
            wxDefaultPosition, wxDefaultSize, wxALIGN_LEFT),
        0, wxALIGN_CENTER_VERTICAL);
    configPrioritySizer->AddSpacer(kInterSpacing);
    configPrioritySizer->Add(logLevel);

    wxButton* clear = new wxButton(base, IDC_LOG_CLEAR, wxT("&Clear"),
        wxDefaultPosition, wxDefaultSize, 0);
    wxButton* pause = new wxButton(base, IDC_LOG_PAUSE, wxT("&Pause"),
        wxDefaultPosition, wxDefaultSize, 0);
    wxButton* prefs = new wxButton(base, IDC_LOG_PREFS, wxT("C&onfigure"),
        wxDefaultPosition, wxDefaultSize, 0);

    configSizer->Add(configPrioritySizer, 0, wxALIGN_LEFT);
    configSizer->Add(clear, 0, wxALIGN_CENTER);
    configSizer->Add(pause, 0, wxALIGN_CENTER);
    configSizer->Add(prefs, 0, wxALIGN_RIGHT);

    /*
     * Create text ctrl.
     */
    wxTextCtrl* pTextCtrl;
    pTextCtrl = new wxTextCtrl(base, IDC_LOG_TEXT, wxT(""),
        wxDefaultPosition, wxDefaultSize,
        wxTE_MULTILINE | wxTE_READONLY | wxTE_RICH2 | wxTE_NOHIDESEL |
            wxHSCROLL);

    /*
     * Add components to master sizer.
     */
    masterSizer->AddSpacer(kEdgeSpacing);
    masterSizer->Add(pTextCtrl, 1, wxEXPAND);
    masterSizer->AddSpacer(kInterSpacing);
    masterSizer->Add(configSizer, 0, wxEXPAND);
    masterSizer->AddSpacer(kEdgeSpacing);

    /*
     * Indent from sides.
     */
    indentSizer->AddSpacer(kEdgeSpacing);
    indentSizer->Add(masterSizer, 1, wxEXPAND);
    indentSizer->AddSpacer(kEdgeSpacing);

    base->SetSizer(indentSizer);

    indentSizer->Fit(this);             // shrink-to-fit
    indentSizer->SetSizeHints(this);    // define minimum size
}

/*
 * In some cases, this means the user has clicked on our "close" button.
 * We don't really even want one, but both WinXP and KDE put one on our
 * window whether we want it or not.  So, we make it work as a "hide"
 * button instead.
 *
 * This also gets called when the app is shutting down, and we do want
 * to destroy ourselves then, saving various information about our state.
 */
void LogWindow::OnClose(wxCloseEvent& event)
{
    /* just hide the window, unless we're shutting down */
    if (event.CanVeto()) {
        event.Veto();
        Show(false);
        return;
    }

    /*
     * Save some preferences.
     */
    SaveWindowPrefs();

    /* if we can't veto the Close(), destroy ourselves */
    Destroy();
}

/*
 * Save all of our preferences to the config file.
 */
void LogWindow::SaveWindowPrefs(void)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();

    /*
     * Save shown/hidden state.
     */
    pPrefs->SetBool("window-log-show", IsShown());

    /*
     * Limits and formatting prefs.
     */
    pPrefs->SetInt("log-display-msg-count", mMaxDisplayMsgs);
    pPrefs->SetInt("log-pool-size-kbytes", mPool.GetMaxSize() / 1024);

    pPrefs->SetInt("log-header-format", mHeaderFormat);
    pPrefs->SetBool("log-single-line", mSingleLine);
    pPrefs->SetInt("log-extra-spacing", mExtraSpacing);
    pPrefs->SetInt("log-point-size", mPointSize);
    pPrefs->SetBool("log-use-color", mUseColor);
    pPrefs->SetBool("log-font-monospace", mFontMonospace);

    pPrefs->SetBool("log-write-file", mWriteFile);
    pPrefs->SetString("log-filename", mFileName.ToAscii());
    pPrefs->SetBool("log-truncate-old", mTruncateOld);

    /*
     * Save window size and position.
     */
    wxPoint posn;
    wxSize size;

    assert(pPrefs != NULL);

    posn = GetPosition();
    size = GetSize();

    pPrefs->SetInt("window-log-x", posn.x);
    pPrefs->SetInt("window-log-y", posn.y);
    pPrefs->SetInt("window-log-width", size.GetWidth());
    pPrefs->SetInt("window-log-height", size.GetHeight());

    /*
     * Save current setting of debug level combo box.
     */
    wxComboBox* pCombo;
    int selection;
    pCombo = (wxComboBox*) FindWindow(IDC_LOG_LEVEL);
    selection = pCombo->GetSelection();
    pPrefs->SetInt("log-display-level", selection);
}

/*
 * Return the desired position and size.
 */
/*static*/ wxRect LogWindow::GetPrefWindowRect(void)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    int x, y, width, height;

    assert(pPrefs != NULL);

    x = y = 10;
    width = 500;
    height = 200;

    /* these don't modify the arg if the pref doesn't exist */
    pPrefs->GetInt("window-log-x", &x);
    pPrefs->GetInt("window-log-y", &y);
    pPrefs->GetInt("window-log-width", &width);
    pPrefs->GetInt("window-log-height", &height);

    return wxRect(x, y, width, height);
}

/*
 * Under Linux+GTK, the first time you show the window, it appears where
 * it's supposed to.  If you then hide it and show it again, it gets
 * moved on top of the parent window.  After that, you can reposition it
 * and it remembers its position across hide/show.
 *
 * To counter this annoyance, we save the position when we hide, and
 * reset the position after a show.  The "newly shown" flag ensures that
 * we only reposition the window as the result of a Show(true) call.
 *
 * Sometimes, something helpful will shift the window over if it's
 * partially straddling a seam between two monitors.  I don't see an easy
 * way to block this, and I'm not sure I want to anyway.
 */
void LogWindow::OnMove(wxMoveEvent& event)
{
    wxPoint point;
    point = event.GetPosition();
    //printf("Sim: log window is at (%d,%d) (new=%d)\n", point.x, point.y,
    //    mNewlyShown);

    if (mNewlyShown) {
        if (mLastPosition == wxDefaultPosition) {
            //printf("Sim: no last position established\n");
        } else {
            Move(mLastPosition);
        }

        mNewlyShown = false;
    }
}

/*
 * Set the "newly shown" flag.
 */
bool LogWindow::Show(bool show)
{
    if (show) {
        mNewlyShown = true;
        Redisplay();
    } else {
        mLastPosition = GetPosition();
    }

    mVisible = show;
    return wxDialog::Show(show);
}

/*
 * User has adjusted the log level.  Update the display appropriately.
 *
 * This is a wxEVT_COMMAND_COMBOBOX_SELECTED event.
 */
void LogWindow::OnLogLevel(wxCommandEvent& event)
{
    int selection;
    android_LogPriority priority;

    selection = event.GetInt();
    wxComboBox* pCombo = (wxComboBox*) FindWindow(IDC_LOG_LEVEL);
    priority = (android_LogPriority) (long)pCombo->GetClientData(event.GetInt());

    printf("Sim: log level selected: %d (%s)\n", (int) priority,
        (const char*) gLogLevels[selection].name.ToAscii());
    mMinPriority = priority;
    Redisplay();
}

/*
 * Clear out the log.
 */
void LogWindow::OnLogClear(wxCommandEvent& event)
{
    ClearDisplay();
    mPool.Clear();
}

/*
 * Handle the pause/resume button.
 *
 * If we're un-pausing, we need to get caught up.
 */
void LogWindow::OnLogPause(wxCommandEvent& event)
{
    mPaused = !mPaused;

    wxButton* pButton = (wxButton*) FindWindow(IDC_LOG_PAUSE);
    if (mPaused) {
        pButton->SetLabel(wxT("&Resume"));

        mPool.SetBookmark();
    } else {
        pButton->SetLabel(wxT("&Pause"));

        LogMessage* pMsg = mPool.GetBookmark();
        if (pMsg == NULL) {
            /* bookmarked item fell out of pool */
            printf("--- bookmark was lost, redisplaying\n");
            Redisplay();
        } else {
            /*
             * The bookmark points to the last item added to the display.
             * We want to chase its "prev" pointer to walk toward the head
             * of the list, adding items from oldest to newest.
             */
            pMsg = pMsg->GetPrev();
            while (pMsg != NULL) {
                if (FilterMatches(pMsg))
                    AddToDisplay(pMsg);
                pMsg = pMsg->GetPrev();
            }
        }
    }
}

/*
 * Open log preferences dialog.
 */
void LogWindow::OnLogPrefs(wxCommandEvent& event)
{
    LogPrefsDialog dialog(this);

    /*
     * Set up the dialog.
     */
    dialog.mHeaderFormat = mHeaderFormat;
    dialog.mSingleLine = mSingleLine;
    dialog.mExtraSpacing = mExtraSpacing;
    dialog.mPointSize = mPointSize;
    dialog.mUseColor = mUseColor;
    dialog.mFontMonospace = mFontMonospace;

    dialog.mDisplayMax = mMaxDisplayMsgs;
    dialog.mPoolSizeKB = mPool.GetMaxSize() / 1024;

    dialog.mWriteFile = mWriteFile;
    dialog.mFileName = mFileName;
    dialog.mTruncateOld = mTruncateOld;

    /*
     * Show it.  If they hit "OK", copy the updated values out, and
     * re-display the log output.
     */
    if (dialog.ShowModal() == wxID_OK) {
        /* discard old display arra */
        ClearDisplay();
        delete[] mDisplayArray;

        mHeaderFormat = dialog.mHeaderFormat;
        mSingleLine = dialog.mSingleLine;
        mExtraSpacing = dialog.mExtraSpacing;
        mPointSize = dialog.mPointSize;
        mUseColor = dialog.mUseColor;
        mFontMonospace = dialog.mFontMonospace;

        assert(dialog.mDisplayMax > 0);
        assert(dialog.mPoolSizeKB > 0);
        mMaxDisplayMsgs = dialog.mDisplayMax;
        mPool.Resize(dialog.mPoolSizeKB * 1024);

        mWriteFile = dialog.mWriteFile;
        if (mLogFp != NULL && mFileName != dialog.mFileName) {
            printf("--- log file name changed, closing\n");
            fclose(mLogFp);
            mLogFp = NULL;
        }
        mFileName = dialog.mFileName;
        mTruncateOld = dialog.mTruncateOld;

        mDisplayArray = new LogMessage*[mMaxDisplayMsgs];
        memset(mDisplayArray, 0, sizeof(LogMessage*) * mMaxDisplayMsgs);
        Redisplay();

        PrepareLogFile();
    }
}

/*
 * Handle a log message "user event".  This should only be called in
 * the main UI thread.
 *
 * We take ownership of "*pLogMessage".
 */
void LogWindow::AddLogMessage(LogMessage* pLogMessage)
{
    mPool.Add(pLogMessage);

    if (!mPaused && mVisible && FilterMatches(pLogMessage)) {
        /*
         * Thought: keep a reference to the previous message.  If it
         * matches in most fields (all except timestamp?), hold it and
         * increment a counter.  If we get a message that doesn't match,
         * or a timer elapses, synthesize a "previous message repeated N
         * times" string.
         */
        AddToDisplay(pLogMessage);
    }

    // release the initial ref caused by allocation
    pLogMessage->Release();

    if (mLogFp != NULL)
        LogToFile(pLogMessage);
}

/*
 * Clear out the display, releasing any log messages held in the display
 * array.
 */
void LogWindow::ClearDisplay(void)
{
    wxTextCtrl* pTextCtrl;
    pTextCtrl = (wxTextCtrl*) FindWindow(IDC_LOG_TEXT);
    pTextCtrl->Clear();

    /*
     * Just run through the entire array.
     */
    for (int i = 0; i < mMaxDisplayMsgs; i++) {
        if (mDisplayArray[i] != NULL) {
            mDisplayArray[i]->Release();
            mDisplayArray[i] = NULL;
        }
    }
    mTopPtr = -1;
    mNextPtr = 0;
}

/*
 * Clear the current display and regenerate it from the log pool.  We need
 * to do this whenever we change filters or log message formatting.
 */
void LogWindow::Redisplay(void)
{
    /*
     * Freeze output rendering so it doesn't flash during update.  Doesn't
     * seem to help for GTK, and it leaves garbage on the screen in WinXP,
     * so I'm leaving it commented out.
     */
    //wxTextCtrl* pText = (wxTextCtrl*) FindWindow(IDC_LOG_TEXT);
    //pText->Freeze();

    //printf("--- redisplay\n");
    ClearDisplay();

    /*
     * Set up the default wxWidgets text style stuff.
     */
    SetTextStyle();

    /*
     * Here's the plan:
     * - Start at the head of the pool (where the most recently added
     *   items are).
     * - Check to see if the current item passes our filter.  If it does,
     *   increment the "found count".
     * - Continue in this manner until we run out of pool or have
     *   sufficient items to fill the screen.
     * - Starting from the current position, walk back toward the head,
     *   adding the items that meet the current filter criteria.
     *
     * Don't forget that the log pool could be empty.
     */
    LogMessage* pMsg = mPool.GetHead();

    if (pMsg != NULL) {
        int foundCount = 0;

        // note this stops before it runs off the end
        while (pMsg->GetNext() != NULL && foundCount < mMaxDisplayMsgs) {
            if (FilterMatches(pMsg))
                foundCount++;
            pMsg = pMsg->GetNext();
        }

        while (pMsg != NULL) {
            if (FilterMatches(pMsg))
                AddToDisplay(pMsg);
            pMsg = pMsg->GetPrev();
        }
    }

    //pText->Thaw();
}


/*
 * Returns "true" if the currently specified filters would allow this
 * message to be shown.
 */
bool LogWindow::FilterMatches(const LogMessage* pLogMessage)
{
    if (pLogMessage->GetPriority() >= mMinPriority)
        return true;
    else
        return false;
}

/*
 * Realloc the array of pointers, and remove anything from the display
 * that should no longer be there.
 */
void LogWindow::SetMaxDisplayMsgs(int max)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();

    pPrefs->SetInt("log-display-msg-count", max);
}

/*
 * Add the message to the display array and to the screen.
 */
void LogWindow::AddToDisplay(LogMessage* pLogMessage)
{
    wxTextCtrl* pTextCtrl;
    pTextCtrl = (wxTextCtrl*) FindWindow(IDC_LOG_TEXT);

    if (mNextPtr == mTopPtr) {
        /*
         * The display array is full.
         *
         * We need to eliminate the topmost entry.  This requires removing
         * it from the array and removing the text from the wxTextCtrl.
         */
        pTextCtrl->Remove(0, mDisplayArray[mTopPtr]->GetTextCtrlLen());
        mDisplayArray[mTopPtr]->Release();
        mTopPtr = (mTopPtr + 1) % mMaxDisplayMsgs;
    }

    /*
     * Add formatted text to the text ctrl.  Track how much actual space
     * is required.  The space may be different on Win32 (CRLF-based) vs.
     * GTK (LF-based), so we need to measure it, not compute it from the
     * text string.
     */
    long lastBefore, lastAfter;
    //long insertBefore;
    //insertBefore = pTextCtrl->GetInsertionPoint();
    lastBefore = pTextCtrl->GetLastPosition();
    FormatMessage(pLogMessage, pTextCtrl);
    lastAfter = pTextCtrl->GetLastPosition();
    pLogMessage->SetTextCtrlLen(lastAfter - lastBefore);

    /*
     * If we restore the old insertion point, we will be glued to where
     * we were.  This is okay until we start deleting text from the top,
     * at which point we need to adjust it to retain our position.
     *
     * If we set the insertion point to the bottom, we effectively
     * implement "scroll to bottom on output".
     *
     * If we don't set it at all, we get slightly strange behavior out
     * of GTK, which seems to be par for the course here.
     */
    //pTextCtrl->SetInsertionPoint(insertBefore);     // restore insertion pt
    pTextCtrl->SetInsertionPoint(lastAfter);

    /* add it to array, claim ownership */
    mDisplayArray[mNextPtr] = pLogMessage;
    pLogMessage->Acquire();

    /* adjust pointers */
    if (mTopPtr < 0)        // first time only
        mTopPtr = 0;
    mNextPtr = (mNextPtr + 1) % mMaxDisplayMsgs;
}


/*
 * Return a human-readable string for the priority level.  Always returns
 * a valid string.
 */
static const wxCharBuffer GetPriorityString(android_LogPriority priority)
{
    int idx;

    idx = (int) priority - (int) ANDROID_LOG_VERBOSE;
    if (idx < 0 || idx >= NELEM(gLogLevels))
        return "?unknown?";
    return gLogLevels[idx].name.ToAscii();
}

/*
 * Format a message and write it to the text control.
 */
void LogWindow::FormatMessage(const LogMessage* pLogMessage, 
    wxTextCtrl* pTextCtrl)
{
#if defined(HAVE_LOCALTIME_R)
    struct tm tmBuf;
#endif
    struct tm* ptm;
    char timeBuf[32];
    char msgBuf[256];
    int msgLen = 0;
    char* outBuf;
    char priChar;
    LogPrefsDialog::HeaderFormat headerFmt;

    headerFmt = mHeaderFormat;
    if (pLogMessage->GetInternal())
        headerFmt = LogPrefsDialog::kHFInternal;

    priChar = ((const char*)GetPriorityString(pLogMessage->GetPriority()))[0];

    /*
     * Get the current date/time in pretty form
     *
     * It's often useful when examining a log with "less" to jump to
     * a specific point in the file by searching for the date/time stamp.
     * For this reason it's very annoying to have regexp meta characters
     * in the time stamp.  Don't use forward slashes, parenthesis,
     * brackets, asterisks, or other special chars here.
     */
    time_t when = pLogMessage->GetWhen();
    const char* fmt = NULL;
#if defined(HAVE_LOCALTIME_R)
    ptm = localtime_r(&when, &tmBuf);
#else
    ptm = localtime(&when);
#endif
    switch (headerFmt) {
    case LogPrefsDialog::kHFFull:
    case LogPrefsDialog::kHFInternal:
        fmt = "%m-%d %H:%M:%S";
        break;
    case LogPrefsDialog::kHFBrief:
    case LogPrefsDialog::kHFMinimal:
        fmt = "%H:%M:%S";
        break;
    default:
        break;
    }
    if (fmt != NULL)
        strftime(timeBuf, sizeof(timeBuf), fmt, ptm);
    else
        strcpy(timeBuf, "-");

    const int kMaxExtraNewlines = 2;
    char hdrNewline[2];
    char finalNewlines[kMaxExtraNewlines+1 +1];

    if (mSingleLine)
        hdrNewline[0] = ' ';
    else
        hdrNewline[0] = '\n';
    hdrNewline[1] = '\0';

    assert(mExtraSpacing <= kMaxExtraNewlines);
    int i;
    for (i = 0; i < mExtraSpacing+1; i++)
        finalNewlines[i] = '\n';
    finalNewlines[i] = '\0';

    wxTextAttr msgColor;
    switch (pLogMessage->GetPriority()) {
    case ANDROID_LOG_WARN:
        msgColor.SetTextColour(*wxBLUE);
        break;
    case ANDROID_LOG_ERROR:
        msgColor.SetTextColour(*wxRED);
        break;
    case ANDROID_LOG_VERBOSE:
    case ANDROID_LOG_DEBUG:
    case ANDROID_LOG_INFO:
    default:
        msgColor.SetTextColour(*wxBLACK);
        break;
    }
    if (pLogMessage->GetInternal())
        msgColor.SetTextColour(*wxGREEN);

    /*
     * Construct a buffer containing the log header.
     */
    bool splitHeader = true;
    outBuf = msgBuf;
    switch (headerFmt) {
    case LogPrefsDialog::kHFFull:
        splitHeader = true;
        msgLen = android_snprintfBuffer(&outBuf, sizeof(msgBuf),
                    "[ %s %5d %c/%-6.6s]%s",
                    timeBuf, pLogMessage->GetPid(), priChar,
                    pLogMessage->GetTag(), hdrNewline);
        break;
    case LogPrefsDialog::kHFBrief:
        splitHeader = true;
        msgLen = android_snprintfBuffer(&outBuf, sizeof(msgBuf),
                    "[%s %5d]%s",
                    timeBuf, pLogMessage->GetPid(), hdrNewline);
        break;
    case LogPrefsDialog::kHFMinimal:
        splitHeader = false;
        msgLen = android_snprintfBuffer(&outBuf, sizeof(msgBuf),
                    "%s %5d- %s",
                    timeBuf, pLogMessage->GetPid(), pLogMessage->GetMsg());
        break;
    case LogPrefsDialog::kHFInternal:
        splitHeader = false;
        msgLen = android_snprintfBuffer(&outBuf, sizeof(msgBuf),
                    "[%s] %s", timeBuf, pLogMessage->GetMsg());
        break;
    default:
        fprintf(stderr, "Sim: unexpected header format %d\n", headerFmt);
        assert(false);
        break;
    }

    if (msgLen < 0) {
        fprintf(stderr, "WHOOPS\n");
        assert(outBuf == msgBuf);
        return;
    }

    if (splitHeader) {
        if (mUseColor)
            pTextCtrl->SetDefaultStyle(wxTextAttr(*wxLIGHT_GREY));
        pTextCtrl->AppendText(wxString::FromAscii(outBuf));
        if (mUseColor)
            pTextCtrl->SetDefaultStyle(msgColor);
        pTextCtrl->AppendText(wxString::FromAscii(pLogMessage->GetMsg()));
        if (mUseColor)
            pTextCtrl->SetDefaultStyle(*wxBLACK);
        pTextCtrl->AppendText(wxString::FromAscii(finalNewlines));
    } else {
        if (mUseColor)
            pTextCtrl->SetDefaultStyle(msgColor);
        pTextCtrl->AppendText(wxString::FromAscii(outBuf));
        if (mUseColor)
            pTextCtrl->SetDefaultStyle(*wxBLACK);
        pTextCtrl->AppendText(wxString::FromAscii(finalNewlines));
    }

    /* if we allocated storage for this message, free it */
    if (outBuf != msgBuf)
        free(outBuf);
}

/*
 * Write the message to the log file.
 *
 * We can't just do this in FormatMessage(), because that re-writes all
 * messages on the display whenever the output format or filter changes.
 *
 * Use a one-log-per-line format here to make "grep" useful.
 */
void LogWindow::LogToFile(const LogMessage* pLogMessage)
{
#if defined(HAVE_LOCALTIME_R)
    struct tm tmBuf;
#endif
    struct tm* ptm;
    char timeBuf[32];
    char msgBuf[256];
    int msgLen;
    char* outBuf;
    char priChar;

    assert(mLogFp != NULL);

    time_t when = pLogMessage->GetWhen();
#if defined(HAVE_LOCALTIME_R)
    ptm = localtime_r(&when, &tmBuf);
#else
    ptm = localtime(&when);
#endif

    strftime(timeBuf, sizeof(timeBuf), "%m-%d %H:%M:%S", ptm);
    priChar = ((const char*)GetPriorityString(pLogMessage->GetPriority()))[0];

    outBuf = msgBuf;
    if (pLogMessage->GetInternal()) {
        msgLen = android_snprintfBuffer(&outBuf, sizeof(msgBuf),
                    "[%s %5d *] %s\n",
                    timeBuf, pLogMessage->GetPid(), pLogMessage->GetMsg());
    } else {
        msgLen = android_snprintfBuffer(&outBuf, sizeof(msgBuf),
                    "[%s %5d %c] %s)\n",
                    timeBuf, pLogMessage->GetPid(), priChar,
                    pLogMessage->GetMsg());
    }
    if (fwrite(outBuf, msgLen, 1, mLogFp) != 1)
        fprintf(stderr, "Sim: WARNING: partial log write\n");
    fflush(mLogFp);

    /* if we allocated storage for this message, free it */
    if (outBuf != msgBuf)
        free(outBuf);
}

/*
 * Get the modification date of a file.
 */
static bool GetFileModDate(const char* fileName, time_t* pModWhen)
{
    struct stat sb;

    if (stat(fileName, &sb) < 0)
        return false;

    *pModWhen = sb.st_mtime;
    return true;
}

/*
 * Open or close the log file as appropriate.
 */
void LogWindow::PrepareLogFile(void)
{
    const int kLogFileMaxAge = 8 * 60 * 60;     // 8 hours

    if (!mWriteFile && mLogFp != NULL) {
        printf("Sim: closing log file\n");
        fclose(mLogFp);
        mLogFp = NULL;
    } else if (mWriteFile && mLogFp == NULL) {
        printf("Sim: opening log file '%s'\n", (const char*)mFileName.ToAscii());
        time_t now, modWhen = 0;
        const char* openFlags;

        now = time(NULL);
        if (!mTruncateOld ||
            (GetFileModDate(mFileName.ToAscii(), &modWhen) &&
             modWhen + kLogFileMaxAge > now))
        {
            if (modWhen != 0) {
                printf("--- log file is %.3f hours old, appending\n",
                    (now - modWhen) / 3600.0);
            }
            openFlags = "a";        // open for append (text mode)
        } else {
            if (modWhen != 0) {
                printf("--- log file is %.3f hours old, truncating\n",
                    (now - modWhen) / 3600.0);
            }
            openFlags = "w";        // open for writing, truncate (text mode)
        }

        mLogFp = fopen(mFileName.ToAscii(), openFlags);
        if (mLogFp == NULL) {
            fprintf(stderr, "Sim: failed opening log file '%s': %s\n",
                (const char*) mFileName.ToAscii(), strerror(errno));
        } else {
            fprintf(mLogFp, "\n\n");
            fflush(mLogFp);
        }
    }
}

/*
 * Add a new log message.
 *
 * This function can be called from any thread.  It makes a copy of the
 * stuff in "*pBundle" and sends it to the main UI thread.
 */
/*static*/ void LogWindow::PostLogMsg(const android_LogBundle* pBundle)
{
    LogMessage* pNewMessage = LogMessage::Create(pBundle);

    SendToWindow(pNewMessage);
}

/*
 * Post a simple string to the log.
 */
/*static*/ void LogWindow::PostLogMsg(const char* msg)
{
    LogMessage* pNewMessage = LogMessage::Create(msg);

    SendToWindow(pNewMessage);
}

/*
 * Post a simple wxString to the log.
 */
/*static*/ void LogWindow::PostLogMsg(const wxString& msg)
{
    LogMessage* pNewMessage = LogMessage::Create(msg.ToAscii());

    SendToWindow(pNewMessage);
}

/*
 * Send a log message to the log window.
 */
/*static*/ void LogWindow::SendToWindow(LogMessage* pMessage)
{
    if (pMessage != NULL) {
        wxWindow* pMainFrame = ((MyApp*)wxTheApp)->GetMainFrame();
        UserEventMessage* pUem = new UserEventMessage;
        pUem->CreateLogMessage(pMessage);

        UserEvent uev(0, (void*) pUem);

        pMainFrame->AddPendingEvent(uev);
    } else {
        fprintf(stderr, "Sim: failed to add new log message\n");
    }
}


/*
 * This is a sanity check.  We need to stop somewhere to avoid trashing
 * the system on bad input.
 */
#define kMaxLen 65536

#define VSNPRINTF vsnprintf     // used to worry about _vsnprintf


/*
 * Print a formatted message into a buffer.  Pass in a buffer to try to use.
 *
 * If the buffer isn't big enough to hold the message, allocate storage
 * with malloc() and return that instead.  The caller is responsible for
 * freeing the storage.
 *
 * Returns the length of the string, or -1 if the printf call failed.
 */
static int android_vsnprintfBuffer(char** pBuf, int bufLen, const char* format, va_list args)
{
    int charsOut;
    char* localBuf = NULL;

    assert(pBuf != NULL && *pBuf != NULL);
    assert(bufLen > 0);
    assert(format != NULL);

    while (1) {
        /*
         * In some versions of libc, vsnprintf only returns 0 or -1, where
         * -1 indicates the the buffer wasn't big enough.  In glibc 2.1
         * and later, it returns the actual size needed.
         *
         * MinGW is just returning -1, so we have to retry there.
         */
        char* newBuf;

        charsOut = VSNPRINTF(*pBuf, bufLen, format, args);

        if (charsOut >= 0 && charsOut < bufLen)
            break;

        //fprintf(stderr, "EXCEED: %d vs %d\n", charsOut, bufLen);
        if (charsOut < 0) {
            /* exact size not known, double previous size */
            bufLen *= 2;
            if (bufLen > kMaxLen)
                goto fail;
        } else {
            /* exact size known, just use that */

            bufLen = charsOut + 1;
        }
        //fprintf(stderr, "RETRY at %d\n", bufLen);

        newBuf = (char*) realloc(localBuf, bufLen);
        if (newBuf == NULL)
            goto fail;
        *pBuf = localBuf = newBuf;
    }

    // On platforms where snprintf() doesn't return the number of
    // characters output, we would need to call strlen() here.

    return charsOut;

fail:
    if (localBuf != NULL) {
        free(localBuf);
        *pBuf = NULL;
    }
    return -1;
}

/*
 * Variable-arg form of the above.
 */
static int android_snprintfBuffer(char** pBuf, int bufLen, const char* format, ...)
{
    va_list args;
    int result;

    va_start(args, format);
    result = android_vsnprintfBuffer(pBuf, bufLen, format, args);
    va_end(args);

    return result;
}


