//
// Copyright 2005 The Android Open Source Project
//
// Main window, menu bar, and associated goodies.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/button.h"
#include "wx/help.h"
#include "wx/filedlg.h"
#include "wx/slider.h"
#include "wx/textctrl.h"

#include "MainFrame.h"
#include "MyApp.h"
#include "Resource.h"
#include "PhoneCollection.h"
#include "PhoneData.h"
#include "PhoneWindow.h"
#include "DeviceWindow.h"
#include "UserEventMessage.h"
#include "PrefsDialog.h"

#include "SimRuntime.h"


static wxString kStatusNotRunning = wxT("Idle");
static wxString kStatusRunning = wxT("Run");

static wxString kDeviceMenuString = wxT("&Device");

static const wxString gStdJavaApps[] = {
    wxT(""),
    wxT("com.android.testharness.TestList"),
    wxT("com.android.apps.contacts.ContactsList"),
    wxT("mikeapp")
};


BEGIN_EVENT_TABLE(MainFrame::MainFrame, wxFrame)
    EVT_CLOSE(MainFrame::OnClose)
    EVT_TIMER(kHalfSecondTimerId, MainFrame::OnTimer)
    //EVT_IDLE(MainFrame::OnIdle)
  
    EVT_ACTIVATE(MainFrame::OnActivate)
    EVT_ACTIVATE_APP(MainFrame::OnActivate)
    EVT_COMBOBOX(IDC_MODE_SELECT, MainFrame::OnComboBox)
    EVT_COMBOBOX(IDC_JAVA_VM, MainFrame::OnComboBox)
    EVT_CHECKBOX(IDC_USE_GDB, MainFrame::OnCheckBox)
    EVT_CHECKBOX(IDC_USE_VALGRIND, MainFrame::OnCheckBox)
    EVT_CHECKBOX(IDC_CHECK_JNI, MainFrame::OnCheckBox)
    EVT_CHECKBOX(IDC_OVERLAY_ONION_SKIN, MainFrame::OnCheckBox)
    EVT_TEXT(IDC_JAVA_APP_NAME, MainFrame::OnText)
    EVT_TEXT_ENTER(IDC_ONION_SKIN_FILE_NAME, MainFrame::OnTextEnter)
    EVT_BUTTON(IDC_ONION_SKIN_BUTTON, MainFrame::OnButton)
    EVT_COMMAND_SCROLL(IDC_ONION_SKIN_ALPHA_VAL, MainFrame::OnSliderChange)

    EVT_MENU(IDM_FILE_PREFERENCES, MainFrame::OnFilePreferences)
    EVT_MENU(IDM_FILE_EXIT, MainFrame::OnFileExit)
    EVT_MENU(IDM_RUNTIME_START, MainFrame::OnSimStart)
    EVT_UPDATE_UI(IDM_RUNTIME_START, MainFrame::OnUpdateSimStart)
    EVT_MENU(IDM_RUNTIME_STOP, MainFrame::OnSimStop)
    EVT_UPDATE_UI(IDM_RUNTIME_STOP, MainFrame::OnUpdateSimStop)
    EVT_MENU(IDM_RUNTIME_RESTART, MainFrame::OnSimRestart)
    EVT_UPDATE_UI(IDM_RUNTIME_RESTART, MainFrame::OnUpdateSimRestart)
    EVT_MENU(IDM_RUNTIME_KILL, MainFrame::OnSimKill)
    EVT_UPDATE_UI(IDM_RUNTIME_KILL, MainFrame::OnUpdateSimKill)
    EVT_MENU_RANGE(IDM_DEVICE_SEL0, IDM_DEVICE_SELN,
        MainFrame::OnDeviceSelected)
    EVT_MENU(IDM_DEVICE_RESCAN, MainFrame::OnDeviceRescan)
    EVT_UPDATE_UI(IDM_DEBUG_SHOW_LOG, MainFrame::OnUpdateDebugShowLog)
    EVT_MENU(IDM_DEBUG_SHOW_LOG, MainFrame::OnDebugShowLog)
    EVT_MENU(IDM_HELP_CONTENTS, MainFrame::OnHelpContents)
    EVT_MENU(IDM_HELP_ABOUT, MainFrame::OnHelpAbout)

    EVT_USER_EVENT(MainFrame::OnUserEvent)
END_EVENT_TABLE()


/*
 * Main window constructor.
 *
 * Creates menus and status bar.
 */
MainFrame::MainFrame(const wxString& title, const wxPoint& pos,
    const wxSize& size, long style)
    : wxFrame((wxFrame *)NULL, -1, title, pos, size, style),
      mSimRunning(false),
      mRestartRequested(false),
      mpPhoneWindow(NULL),
      mPhoneWindowPosn(wxDefaultPosition),
      mTimer(this, kHalfSecondTimerId)
{
    mSimAssetPath = ((MyApp*)wxTheApp)->GetSimAssetPath();
    mSimAssetPath += wxT("/simulator/default/default");

    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    int val;

    val = mPhoneWindowPosn.x;
    pPrefs->GetInt("window-device-x", &val);
    mPhoneWindowPosn.x = val;
    val = mPhoneWindowPosn.y;
    pPrefs->GetInt("window-device-y", &val);
    mPhoneWindowPosn.y = val;

    /*
     * Create main menu.
     */
    ConstructMenu();

    /*
     * Create the status bar.
     */
    int widths[2] = { -1, 50 };
    CreateStatusBar(2, wxFULL_REPAINT_ON_RESIZE);   // no wxST_SIZEGRIP
    SetStatusWidths(2, widths);
    SetStatusText(wxT("Ready"));
    SetStatusText(kStatusNotRunning, 1);

    /*
     * Create main window controls.
     */
    ConstructControls();

#if 0
    /*
     * Use the standard window color for the main frame (which usually
     * has a darker color).  This has a dramatic effect under Windows.
     */
    wxColour color = wxSystemSettings::GetColour(wxSYS_COLOUR_WINDOW);
    SetOwnBackgroundColour(color);
#endif

    /*
     * Create the log window.
     */
    wxRect layout = LogWindow::GetPrefWindowRect();
    mpLogWindow = new LogWindow(this);
    mpLogWindow->Move(layout.GetTopLeft());
    mpLogWindow->SetSize(layout.GetSize());
    bool showLogWindow = true;
    pPrefs->GetBool("window-log-show", &showLogWindow);
    if (showLogWindow)
        mpLogWindow->Show();

    /*
     * Set up a frequent timer.  We use this to keep our "run/idle"
     * display up to date.  (Ideally this will go away.)
     */
    mTimer.Start(400);      // arg is delay in ms

    /*
     * Handle auto-power-on by sending ourselves an event.  That way it
     * gets handled after window initialization finishes.
     */
    bool autoPowerOn = false;
    pPrefs->GetBool("auto-power-on", &autoPowerOn);
    if (autoPowerOn) {
        printf("Sim: Auto power-up\n");
        wxCommandEvent startEvent(wxEVT_COMMAND_MENU_SELECTED, IDM_RUNTIME_START);
        this->AddPendingEvent(startEvent);
    }

    /*
     * wxThread wants these to be on the heap -- it will call delete on the
     * object when the thread exits.
     */
    mExternalRuntimeThread = new ExternalRuntime();
    mExternalRuntimeThread->StartThread();
    mPropertyServerThread = new PropertyServer();
    mPropertyServerThread->StartThread();
}

/*
 * Construct the main menu.  Called from the constructor.
 */
void MainFrame::ConstructMenu(void)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();

    /*
     * Scan for available phones.
     */
    PhoneCollection* pCollection = PhoneCollection::GetInstance();
    pCollection->ScanForPhones(mSimAssetPath.ToAscii());

    /*
     * Create the "File" menu.
     */
    wxMenu* menuFile = new wxMenu;

    menuFile->Append(IDM_FILE_PREFERENCES, wxT("&Preferences..."),
        wxT("Edit simulator preferences"));
    menuFile->AppendSeparator();
    menuFile->Append(IDM_FILE_EXIT, wxT("E&xit\tCtrl-Q"),
        wxT("Stop simulator and exit"));

    /*
     * Create the "Runtime" menu.
     */
    wxMenu* menuRuntime = new wxMenu;
    menuRuntime->Append(IDM_RUNTIME_START, wxT("&Power On\tCtrl-G"),
        wxT("Start the device"));
//    menuRuntime->Append(IDM_RUNTIME_STOP, wxT("Power &Off"),
//        wxT("Stop the device"));
    menuRuntime->AppendSeparator();
//    menuRuntime->Append(IDM_RUNTIME_RESTART, wxT("&Restart"),
//        wxT("Restart the device"));
    menuRuntime->Append(IDM_RUNTIME_KILL, wxT("&Kill\tCtrl-K"),
        wxT("Kill the runtime processes"));

    /*
     * Create "Device" menu.
     */
    wxString defaultDevice = wxT("Sooner");
    pPrefs->GetString("default-device", /*ref*/ defaultDevice);
    wxMenu* menuDevice = CreateDeviceMenu(defaultDevice.ToAscii());

    /*
     * Create "Debug" menu.
     */
    wxMenu* menuDebug = new wxMenu;
    menuDebug->AppendCheckItem(IDM_DEBUG_SHOW_LOG, wxT("View &Log Output"),
        wxT("View log output window"));

    /*
     * Create the "Help" menu.
     */
    wxMenu* menuHelp = new wxMenu;
    menuHelp->Append(IDM_HELP_CONTENTS, wxT("&Contents...\tF1"),
        wxT("Simulator help"));
    menuHelp->AppendSeparator();
    menuHelp->Append(IDM_HELP_ABOUT, wxT("&About..."),
        wxT("See the fabulous 'about' box"));

    /*
     * Create the menu bar.
     */
    wxMenuBar *menuBar = new wxMenuBar;
    menuBar->Append(menuFile, wxT("&File"));
    menuBar->Append(menuDevice, kDeviceMenuString);
    menuBar->Append(menuRuntime, wxT("&Runtime"));
    menuBar->Append(menuDebug, wxT("&Debug"));
    menuBar->Append(menuHelp, wxT("&Help"));

    SetMenuBar(menuBar);

}

/*
 * Construct the "device" menu from our phone collection.
 */
wxMenu* MainFrame::CreateDeviceMenu(const char* defaultItemName)
{
    wxMenu* menuDevice = new wxMenu;
    PhoneCollection* pCollection = PhoneCollection::GetInstance();
    int defaultModel = 0;

    for (int i = 0; i < pCollection->GetPhoneCount(); i++) {
        PhoneData* pPhoneData = pCollection->GetPhoneData(i);
        assert(pPhoneData != NULL);

        menuDevice->AppendRadioItem(IDM_DEVICE_SEL0 + i,
            wxString::FromAscii(pPhoneData->GetTitle()));

        // use this one as default if the string matches
        if (strcasecmp(pPhoneData->GetName(), defaultItemName) == 0)
            defaultModel = i;
    }

    menuDevice->Check(IDM_DEVICE_SEL0 + defaultModel, true);

    menuDevice->AppendSeparator();
    menuDevice->Append(IDM_DEVICE_RESCAN, wxT("Re-scan"));

    return menuDevice;
}

/*
 * Create some controls in the main window.
 *
 * The main frame doesn't use the normal background color that you find
 * in dialog windows, so we create a "panel" and put all the controls
 * on that.
 */
void MainFrame::ConstructControls(void)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    wxPanel* base = new wxPanel(this, wxID_ANY);
    wxBoxSizer* masterSizer = new wxBoxSizer(wxVERTICAL);
    wxBoxSizer* tmpSizer;
    wxStaticBoxSizer* displayOptSizer;
    wxStaticBoxSizer* runtimeOptSizer;
    wxStaticBoxSizer* onionSkinOptSizer;
    wxComboBox* pModeSelection;
    wxCheckBox* pUseGDB;
    wxCheckBox* pUseValgrind;
    wxCheckBox* pCheckJni;
    wxCheckBox* pOverlayOnionSkin;
    
    displayOptSizer = new wxStaticBoxSizer(wxHORIZONTAL, base,
        wxT("Configuration"));
    runtimeOptSizer = new wxStaticBoxSizer(wxVERTICAL, base,
        wxT("Runtime Options"));
    onionSkinOptSizer = new wxStaticBoxSizer(wxVERTICAL, base,
        wxT("Onion Skin Options"));

    /*
     * Set up the configuration sizer (nee "display options").
     */
    tmpSizer = new wxBoxSizer(wxHORIZONTAL);
    displayOptSizer->Add(tmpSizer);
    tmpSizer->Add(
            new wxStaticText(base, wxID_ANY, wxT("Device mode:"),
            wxDefaultPosition, wxDefaultSize, wxALIGN_LEFT), 0, wxALIGN_CENTER_VERTICAL);
    pModeSelection = new wxComboBox(base, IDC_MODE_SELECT, wxT(""),
            wxDefaultPosition, wxDefaultSize, 0, NULL, wxCB_READONLY);
    tmpSizer->AddSpacer(kInterSpacing);
    tmpSizer->Add(pModeSelection);

    displayOptSizer->AddSpacer(kInterSpacing);

    /*
     * Configure the runtime options sizer.
     */
    wxComboBox* pJavaAppName;
    tmpSizer = new wxBoxSizer(wxHORIZONTAL);
    pUseGDB = new wxCheckBox(base, IDC_USE_GDB, wxT("Use &debugger"));
    tmpSizer->Add(pUseGDB);
    tmpSizer->AddSpacer(kInterSpacing);
    pUseValgrind = new wxCheckBox(base, IDC_USE_VALGRIND, wxT("Use &valgrind"));
    tmpSizer->Add(pUseValgrind);
    tmpSizer->AddSpacer(kInterSpacing);
    pCheckJni = new wxCheckBox(base, IDC_CHECK_JNI, wxT("Check &JNI"));
    tmpSizer->Add(pCheckJni);

    pJavaAppName = new wxComboBox(base, IDC_JAVA_APP_NAME, wxT(""),
        wxDefaultPosition, wxSize(320, -1), NELEM(gStdJavaApps), gStdJavaApps,
        wxCB_DROPDOWN);
    wxBoxSizer* javaAppSizer = new wxBoxSizer(wxHORIZONTAL);
    javaAppSizer->Add(
            new wxStaticText(base, wxID_ANY,
                wxT("Java app:"),
                wxDefaultPosition, wxDefaultSize,
                wxALIGN_LEFT),
            0, wxALIGN_CENTER_VERTICAL);
    javaAppSizer->AddSpacer(kInterSpacing);
    javaAppSizer->Add(pJavaAppName);

    runtimeOptSizer->Add(tmpSizer);

    runtimeOptSizer->AddSpacer(kInterSpacing);
    runtimeOptSizer->Add(javaAppSizer);
    runtimeOptSizer->AddSpacer(kInterSpacing);

    wxString tmpStr;
    SetCheckFromPref(pUseGDB, "debug", false);
    SetCheckFromPref(pUseValgrind, "valgrind", false);
    SetCheckFromPref(pCheckJni, "check-jni", false);
    if (pPrefs->GetString("java-app-name", /*ref*/ tmpStr))
        pJavaAppName->SetValue(tmpStr);

    /*
     * Configure the onion skin options sizer.
     */
    wxTextCtrl* pOnionSkinFileNameText;
    wxButton* pOnionSkinFileButton;
    wxSlider* pOnionSkinAlphaSlider;
    tmpSizer = new wxBoxSizer(wxHORIZONTAL);
    pOverlayOnionSkin = new wxCheckBox(base, 
        IDC_OVERLAY_ONION_SKIN, wxT("Overlay &onion skin"));
    tmpSizer->Add(pOverlayOnionSkin);

    pOnionSkinFileNameText = new wxTextCtrl(base, 
        IDC_ONION_SKIN_FILE_NAME, wxT(""),
        wxDefaultPosition, wxSize(250, -1),
        wxTE_PROCESS_ENTER);
    pOnionSkinFileButton = new wxButton(base, IDC_ONION_SKIN_BUTTON,
        wxT("Choose"));

    wxBoxSizer* onionSkinFileNameSizer = new wxBoxSizer(wxHORIZONTAL);
    onionSkinFileNameSizer->Add(
        new wxStaticText(base, wxID_ANY,
            wxT("Filename:"),
            wxDefaultPosition, wxDefaultSize,
            wxALIGN_LEFT),
        0, wxALIGN_CENTER_VERTICAL);
    onionSkinFileNameSizer->AddSpacer(kInterSpacing);
    onionSkinFileNameSizer->Add(pOnionSkinFileNameText);
    onionSkinFileNameSizer->Add(pOnionSkinFileButton);

    wxBoxSizer * onionSkinAlphaSizer = new wxBoxSizer(wxHORIZONTAL);
    int initialAlphaVal = 127;
    pPrefs->GetInt("onion-skin-alpha-value", &initialAlphaVal);
    pOnionSkinAlphaSlider = new wxSlider(base, IDC_ONION_SKIN_ALPHA_VAL,
        initialAlphaVal, 0, 255, wxDefaultPosition, wxSize(150, 20));
    onionSkinAlphaSizer->Add(
        new wxStaticText(base, wxID_ANY,
            wxT("Transparency:"),
            wxDefaultPosition, wxDefaultSize,
            wxALIGN_LEFT),
        0, wxALIGN_CENTER_VERTICAL);
    onionSkinAlphaSizer->AddSpacer(kInterSpacing);
    onionSkinAlphaSizer->Add(pOnionSkinAlphaSlider, 1, wxCENTRE | wxALL, 5);

    onionSkinOptSizer->Add(tmpSizer);
    onionSkinOptSizer->AddSpacer(kInterSpacing);
    onionSkinOptSizer->Add(onionSkinFileNameSizer);
    onionSkinOptSizer->Add(onionSkinAlphaSizer);

    wxString tmpStr2;
    SetCheckFromPref(pOverlayOnionSkin, "overlay-onion-skin", false);
    if (pPrefs->GetString("onion-skin-file-name", /*ref*/ tmpStr2))
        pOnionSkinFileNameText->SetValue(tmpStr2);

    /*
     * Add the various components to the master sizer.
     */
    masterSizer->Add(displayOptSizer);
    masterSizer->AddSpacer(kInterSpacing * 2);
    masterSizer->Add(runtimeOptSizer);
    masterSizer->AddSpacer(kInterSpacing * 2);
    masterSizer->Add(onionSkinOptSizer);
    //masterSizer->AddSpacer(kInterSpacing);

    /*
     * I don't see a way to guarantee that the window is wide enough to
     * show the entire menu bar, so just throw some pixels at it.
     */
    wxBoxSizer* minWidthSizer = new wxBoxSizer(wxVERTICAL);
    minWidthSizer->Add(300, kEdgeSpacing);       // forces minimum width
    minWidthSizer->Add(masterSizer);
    minWidthSizer->AddSpacer(kInterSpacing * 2);

    /* move us a few pixels in from the left */
    wxBoxSizer* indentSizer = new wxBoxSizer(wxHORIZONTAL);
    indentSizer->AddSpacer(kEdgeSpacing);
    indentSizer->Add(minWidthSizer);
    indentSizer->AddSpacer(kEdgeSpacing);

    base->SetSizer(indentSizer);

    indentSizer->Fit(this);
    indentSizer->SetSizeHints(this);
}

/*
 * Set the value of a checkbox based on a value from the config file.
 */
void MainFrame::SetCheckFromPref(wxCheckBox* pControl, const char* prefStr,
    bool defaultVal)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    assert(pPrefs != NULL);

    bool val = defaultVal;
    pPrefs->GetBool(prefStr, &val);

    pControl->SetValue(val);
}

/*
 * Destructor.
 */
MainFrame::~MainFrame(void)
{
    PhoneCollection::DestroyInstance();

    delete mExternalRuntimeThread;
    delete mPropertyServerThread;

    // don't touch mpModeSelection -- child of window
}

/*
 * File->Quit or click on close box.
 *
 * If we want an "are you sure you want to quit" box, add it here.
 */
void MainFrame::OnClose(wxCloseEvent& event)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();

/*
    if (event.CanVeto())
        printf("Closing (can veto)\n");
    else
        printf("Closing (mandatory)\n");
*/

    /*
     * Generally speaking, Close() is not guaranteed to close the window.
     * However, we want to use it here because (a) our windows are
     * guaranteed to close, and (b) it provides our windows an opportunity
     * to tell others that they are about to vanish.
     */
    if (mpPhoneWindow != NULL)
        mpPhoneWindow->Close(true);

    /* save position of main window */
    wxPoint pos = GetPosition();
    pPrefs->SetInt("window-main-x", pos.x);
    pPrefs->SetInt("window-main-y", pos.y);

    /* save default device selection */
    int idx = GetSelectedDeviceIndex();
    if (idx >= 0) {
        PhoneCollection* pCollection = PhoneCollection::GetInstance();
        PhoneData* pPhoneData = pCollection->GetPhoneData(idx);
        pPrefs->SetString("default-device", pPhoneData->GetName());
    }

    if (mpLogWindow != NULL)
        mpLogWindow->Close(true);
    Destroy();
}

/*
 * File->Preferences
 */
void MainFrame::OnFilePreferences(wxCommandEvent& WXUNUSED(event))
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    PrefsDialog dialog(this);
    int result;

    result = dialog.ShowModal();
    if (result == wxID_OK) {
        /*
         * The dialog handles writing changes to Preferences, so all we
         * need to deal with here are changes that have an immediate
         * impact on us. (which is currently nothing)
         */
    }
}

/*
 * File->Exit
 */
void MainFrame::OnFileExit(wxCommandEvent& WXUNUSED(event))
{
    Close(FALSE);       // false means "allow veto"
}

/*
 * Decide whether Simulator->Start should be enabled.
 */
void MainFrame::OnUpdateSimStart(wxUpdateUIEvent& event)
{
    if (IsRuntimeRunning())
        event.Enable(FALSE);
    else
        event.Enable(TRUE);
}

/*
 * Simulator->Start
 */
void MainFrame::OnSimStart(wxCommandEvent& WXUNUSED(event))
{
    // keyboard equivalents can still get here even if menu item disabled
    if (IsRuntimeRunning())
        return;

    int id = GetSelectedDeviceIndex();
    if (id < 0) {
        fprintf(stderr, "Sim: could not identify currently selected device\n");
        return;
    }

#if 0
    static int foo = 0;
    foo++;
    if (foo == 2) {
        Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();

        pPrefs->SetBool("debug", true);
    }
#endif

    SetupPhoneUI(id, NULL);
    if (mpPhoneWindow != NULL)
        mpPhoneWindow->GetDeviceManager()->StartRuntime();
}

/*
 * Decide whether Simulator->Stop should be enabled.
 */
void MainFrame::OnUpdateSimStop(wxUpdateUIEvent& event)
{
    if (IsRuntimeRunning())
        event.Enable(TRUE);
    else
        event.Enable(FALSE);
}

/*
 * Simulator->Stop
 */
void MainFrame::OnSimStop(wxCommandEvent& WXUNUSED(event))
{
    if (!IsRuntimeRunning())
        return;
    assert(mpPhoneWindow != NULL);
    mpPhoneWindow->GetDeviceManager()->StopRuntime();
}

/*
 * Decide whether Simulator->Restart should be enabled.
 */
void MainFrame::OnUpdateSimRestart(wxUpdateUIEvent& event)
{
    if (IsRuntimeRunning())
        event.Enable(TRUE);
    else
        event.Enable(FALSE);
}

/*
 * Simulator->Restart - stop then start the device runtime.
 */
void MainFrame::OnSimRestart(wxCommandEvent& WXUNUSED(event))
{
    if (!IsRuntimeRunning())
        return;

    printf("Restart requested\n");
    mpPhoneWindow->GetDeviceManager()->StopRuntime();

    mRestartRequested = true;
}

/*
 * Decide whether Simulator->Kill should be enabled.
 */
void MainFrame::OnUpdateSimKill(wxUpdateUIEvent& event)
{
    if (IsRuntimeKillable())
        event.Enable(TRUE);
    else
        event.Enable(FALSE);
}

/*
 * Simulator->Kill
 */
void MainFrame::OnSimKill(wxCommandEvent& WXUNUSED(event))
{
    if (!IsRuntimeKillable())
        return;
    assert(mpPhoneWindow != NULL);
    mpPhoneWindow->GetDeviceManager()->KillRuntime();
}


/*
 * Device->[select]
 */
void MainFrame::OnDeviceSelected(wxCommandEvent& event)
{
    wxBusyCursor busyc;
    int id = event.GetId() - IDM_DEVICE_SEL0;

    SetupPhoneUI(id, NULL);
}

/*
 * Device->Rescan
 */
void MainFrame::OnDeviceRescan(wxCommandEvent& event)
{
    wxBusyCursor busyc;
    wxMenuBar* pMenuBar;
    PhoneCollection* pCollection;
    wxMenu* pOldMenu;
    wxMenu* pNewMenu;
    const char* curDevName = NULL;
    int idx;
    
    /* figure out the current device name */
    pCollection = PhoneCollection::GetInstance();
    idx = GetSelectedDeviceIndex();
    if (idx >= 0) {
        PhoneData* pPhoneData;

        pPhoneData = pCollection->GetPhoneData(idx);
        curDevName = pPhoneData->GetName();
        printf("--- device name is '%s'\n", (const char*) curDevName);
    }

    /* reconstruct device menu with new data */
#ifdef BEFORE_ASSET
    pCollection->ScanForPhones(mSimAssetPath);
#else
    pCollection->ScanForPhones(NULL);
#endif

    pMenuBar = GetMenuBar();
    idx = pMenuBar->FindMenu(kDeviceMenuString);
    if (idx == wxNOT_FOUND) {
        fprintf(stderr, "Sim: couldn't find %s menu\n", (const char*) kDeviceMenuString.ToAscii());
        return;
    }

    pNewMenu = CreateDeviceMenu(curDevName);

    pOldMenu = pMenuBar->Replace(idx, pNewMenu, kDeviceMenuString);
    delete pOldMenu;

    /* tell the PhoneWindow about it; may cause runtime to exit */
    if (mpPhoneWindow != NULL)
        mpPhoneWindow->DevicesRescanned();
}

/*
 * Set checkbox on menu item.
 */
void MainFrame::OnUpdateDebugShowLog(wxUpdateUIEvent& event)
{
    if (mpLogWindow == NULL) {
        event.Enable(false);
    } else {
        event.Enable(true);
        event.Check(mpLogWindow->IsShown());
    }
}

/*
 * Debug->ShowLog toggle.
 */
void MainFrame::OnDebugShowLog(wxCommandEvent& WXUNUSED(event))
{
    mpLogWindow->Show(!mpLogWindow->IsShown());
}

/*
 * Help->Contents
 */
void MainFrame::OnHelpContents(wxCommandEvent& WXUNUSED(event))
{
    ((MyApp*)wxTheApp)->GetHelpController()->DisplayContents();
}

/*
 * Help->About
 */
void MainFrame::OnHelpAbout(wxCommandEvent& WXUNUSED(event))
{
    wxMessageBox(wxT("Android Simulator v0.1\n"
                     "Copyright 2006 The Android Open Source Project"),
        wxT("About..."), wxOK | wxICON_INFORMATION, this);
}

/*
 * Sent from phonewindow or when activated
 */
void MainFrame::OnActivate(wxActivateEvent& event)
{
#if 0
    if (event.GetActive())
    {
        if (mpPhoneWindow != NULL &&
            mpPhoneWindow->GetDeviceManager()->RefreshRuntime())
        {
            wxString msg;
            int sel;

            msg = wxT("Newer runtime executable found. Would you like to reload the device?");

            sel = wxMessageBox(msg, wxT("Android Safety Patrol"),
                wxYES | wxNO | wxICON_QUESTION, mpPhoneWindow);
            //printf("BUTTON was %d (yes=%d)\n", sel, wxYES);
            if (sel == wxYES)
            {
                mpPhoneWindow->GetDeviceManager()->StopRuntime();
                mpPhoneWindow->Close();
                mpPhoneWindow = NULL;
                mRestartRequested = true;
            }
            else
            {
                mpPhoneWindow->GetDeviceManager()->UserCancelledRefresh();
            }
        }
    }
#endif

    // let wxWidgets do whatever it needs to do
    event.Skip();
}
            

/*
 * Device mode selection box.
 */
void MainFrame::OnComboBox(wxCommandEvent& event)
{
    const char* pref;
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    assert(pPrefs != NULL);

    if (IDC_MODE_SELECT == event.GetId())
    {
        int id = GetSelectedDeviceIndex();
        if (id < 0)
            return;
        //printf("--- mode selected: '%s'\n", (const char*) event.GetString().ToAscii());

        /*
         * Call the phone window's setup function.  Don't call our SetupPhoneUI
         * function from here -- updating the combo box from a combo box callback
         * could cause problems.
         */
        if (mpPhoneWindow != NULL) {
            mpPhoneWindow->SetCurrentMode(event.GetString());
            mpPhoneWindow->Setup(id);
        }
    } else if (event.GetId() == IDC_JAVA_VM) {
        wxComboBox* pBox = (wxComboBox*) FindWindow(IDC_JAVA_VM);
        pPrefs->SetString("java-vm", pBox->GetValue().ToAscii());
    }
}

/*
 * One of our option checkboxes has been changed.
 *
 * We update the prefs database so that the settings are retained when
 * the simulator is next used.
 */
void MainFrame::OnCheckBox(wxCommandEvent& event)
{
    const char* pref;

    switch (event.GetId()) {
    case IDC_USE_GDB:               pref = "debug";                 break;
    case IDC_USE_VALGRIND:          pref = "valgrind";              break;
    case IDC_CHECK_JNI:             pref = "check-jni";             break;
    case IDC_OVERLAY_ONION_SKIN:    pref = "overlay-onion-skin";    break; 
    default:
        printf("Sim: unrecognized checkbox %d in OnCheckBox\n", event.GetId());
        return;
    }

    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    assert(pPrefs != NULL);

    pPrefs->SetBool(pref, (bool) event.GetInt());
    //printf("--- set pref '%s' to %d\n", pref, (bool) event.GetInt());
    if (event.GetId() == IDC_OVERLAY_ONION_SKIN) {
        BroadcastOnionSkinUpdate();
    }
    if (event.GetId() == IDC_CHECK_JNI) {
        const char* val = "0";
        if ((bool) event.GetInt())
            val = "1";
        mPropertyServerThread->SetProperty(PropertyServer::kPropCheckJni, val);

    }
}

void MainFrame::BroadcastOnionSkinUpdate() {
    if (mpPhoneWindow != NULL) {
        // broadcast a user event indicating an onion skin update
        UserEvent uev(0, (void*) -1);
        mpPhoneWindow->GetDeviceManager()->BroadcastEvent(uev);
    }
}

/*
 * A text control on the main page is being updated.
 *
 * The current implementation updates the preferences database on every
 * change, which is a bit silly but is easy to do.
 */
void MainFrame::OnText(wxCommandEvent& event)
{
    const char* pref;

    switch (event.GetId()) {
    case IDC_JAVA_APP_NAME:     pref = "java-app-name"; break;
    default:
        printf("Sim: unrecognized textctrl %d in OnText\n", event.GetId());
        return;
    }

    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    assert(pPrefs != NULL);

    // event.GetString() does not work on Mac -- always blank
    //pPrefs->SetString(pref, event.GetString());
    assert(event.GetId() == IDC_JAVA_APP_NAME); // fix if we add more
    wxComboBox* pBox;
    pBox = (wxComboBox*) FindWindow(IDC_JAVA_APP_NAME);
    pPrefs->SetString(pref, pBox->GetValue().ToAscii());
    //printf("--- set pref '%s' to '%s'\n", pref,(const char*)pBox->GetValue());
}

/*
 * A user pressed enter in a text control on the main page.
 *
 * The current implementation updates the preferences database on every
 * change, which is a bit silly but is easy to do.
 */
void MainFrame::OnTextEnter(wxCommandEvent& event)
{
    const char* pref;

    switch (event.GetId()) {
    case IDC_ONION_SKIN_FILE_NAME:
        pref = "onion-skin-file-name";
        break;
    default:
        printf("Sim: unrecognized textctrl %d in OnTextEnter\n", event.GetId());
        return;
    }

    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    assert(pPrefs != NULL);

    assert(event.GetId() == IDC_ONION_SKIN_FILE_NAME); // fix if we add more
    wxTextCtrl* pTextCtrl;
    pTextCtrl = (wxTextCtrl*) FindWindow(IDC_ONION_SKIN_FILE_NAME);
    wxString onionSkinFileNameWxString = pTextCtrl->GetValue();
    char* onionSkinFileName = "";
    if (onionSkinFileNameWxString.Len() > 0) {
        onionSkinFileName = android::strdupNew(onionSkinFileNameWxString.ToAscii());
    }
    pPrefs->SetString(pref, onionSkinFileName);
    BroadcastOnionSkinUpdate();
}

/*
 * A user pressed a button on the main page
 * 
 */
 void MainFrame::OnButton(wxCommandEvent& event)
 {
    wxWindow* base;
    wxFileDialog* pOnionSkinFileChooser;
    int retVal;
    switch (event.GetId()) {
    case IDC_ONION_SKIN_BUTTON:
        base = FindWindow(IDC_ONION_SKIN_BUTTON)->GetParent();
        pOnionSkinFileChooser = new wxFileDialog(base, 
            wxT("Choose the onion skin image file."), 
            wxT(""), wxT(""), wxT("*.*"),
            wxOPEN | wxFILE_MUST_EXIST);
        retVal = pOnionSkinFileChooser->ShowModal();
        if (retVal == pOnionSkinFileChooser->GetAffirmativeId()) {
            Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
            assert(pPrefs != NULL);
            wxString fileNameWxString = pOnionSkinFileChooser->GetPath();
            const char* fileName = android::strdupNew(fileNameWxString.ToAscii());
            wxTextCtrl* fileTextCtrl = (wxTextCtrl*) FindWindow(IDC_ONION_SKIN_FILE_NAME);
            fileTextCtrl->SetValue(fileNameWxString);
            pPrefs->SetString("onion-skin-file-name", fileName);
            BroadcastOnionSkinUpdate();
        }
        break;
    default:
        printf("Sim: unrecognized button %d in OnButton\n", event.GetId());
        return;
    }     
 }
 
 /*
  * The user moved a slider on the main page
  */
 void MainFrame::OnSliderChange(wxScrollEvent& event)
 {
    wxSlider* pSlider;
    Preferences* pPrefs;
    switch (event.GetId()) {
    case IDC_ONION_SKIN_ALPHA_VAL:
        pSlider = (wxSlider*) FindWindow(IDC_ONION_SKIN_ALPHA_VAL);
        pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
        assert(pPrefs != NULL);
        pPrefs->SetInt("onion-skin-alpha-value", pSlider->GetValue());
        BroadcastOnionSkinUpdate();
        break;
    default:
        printf("Sim: unrecognized scroller or slider %d in OnSliderChange\n", event.GetId());
        return;
    }     
 }

#if 0
/*
 * Idle processing.  Under wxWidgets this only called once after UI
 * activity unless you call event.RequestMore().
 */
void MainFrame::OnIdle(wxIdleEvent& event)
{
    event.Skip();       // let base class handler do stuff
}
#endif

/*
 * Handle the timer.
 *
 * This is being called in the main thread, so multithreading with the
 * rest of MainFrame isn't a concern here.
 */
void MainFrame::OnTimer(wxTimerEvent& event)
{
    bool status;

    /*
     * Check to see if the runtime died without telling us.  This can only
     * happen if we forcibly kill our thread.  We shouldn't really be
     * doing that anymore, but keep this in for now just in case.
     */
    status = IsRuntimeRunning();

    if (mSimRunning != status) {
        if (!status) {
            printf("Sim: fixed mSimRunning=%d actual=%d\n",
                mSimRunning, status);
            mSimRunning = status;

            if (!status)
                HandleRuntimeStop();
        } else {
            /*
             * This was happening when we were shutting down but the
             * device management thread hadn't completely gone away.  The
             * simple IsRunning test passes, so we get a false positive.
             * Ignore it.
             */
        }
    }

    if (gWantToKill) {
        if (IsRuntimeRunning()) {
            printf("Sim: handling kill request\n");
            mpPhoneWindow->GetDeviceManager()->KillRuntime();
        }
        gWantToKill = false;

        /* see if Ctrl-C should kill us too */
        Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
        bool die = false;

        pPrefs->GetBool("trap-sigint-suicide", &die);
        if (die) {
            printf("Sim: goodbye cruel world!\n");
            exit(0);
        }
    }
}

/*
 * Determine whether or not the simulator is running.
 */
bool MainFrame::IsRuntimeRunning(void)
{
    bool result;

    if (mpPhoneWindow == NULL)
        result = false;
    else if (!mpPhoneWindow->IsReady())
        result = false;
    else
        result = mpPhoneWindow->GetDeviceManager()->IsRunning();

    return result;
}

/*
 * Determine whether or not the runtime can be killed.
 */
bool MainFrame::IsRuntimeKillable(void)
{
    bool result;

    result = IsRuntimeRunning();
    if (result)
        result = mpPhoneWindow->GetDeviceManager()->IsKillable();

    return result;
}

/*
 * Determine whether two devices are sufficiently compatible.
 */
bool MainFrame::CompatibleDevices(PhoneData* pData1, PhoneData* pData2)
{
    int displayCount;

    displayCount = pData1->GetNumDisplays();
    if (pData2->GetNumDisplays() != displayCount)
        return false;

    for (int i = 0; i < displayCount; i++) {
        PhoneDisplay* pDisplay1 = pData1->GetPhoneDisplay(i);
        PhoneDisplay* pDisplay2 = pData2->GetPhoneDisplay(i);

        if (!PhoneDisplay::IsCompatible(pDisplay1, pDisplay2))
            return false;
    }

    return true;
}

/*
 * (Re-)arrange the UI for the currently selected phone model.
 *
 * If the simulator is running, and the set of displays for the current
 * device are incompatible with the new device, we need to restart the
 * runtime.  We need to ask for permission first though.
 */
void MainFrame::SetupPhoneUI(int idx, const char* defaultMode)
{
    PhoneCollection* pCollection;
    PhoneData* pPhoneData;
    wxString* choices = NULL;
    int numChoices = 0;
    int numKeyboards = 0;
    bool haveDefaultMode = false;
    wxCharBuffer currentMode;
    int i;

    pCollection = PhoneCollection::GetInstance();
    pPhoneData = pCollection->GetPhoneData(idx);
    if (pPhoneData == NULL) {
        fprintf(stderr, "ERROR: device index %d not valid\n", idx);
        goto bail;
    }

    /*
     * We have a window up.  If the displays aren't compatible, we'll
     * need to recreate it.
     */
    if (mpPhoneWindow != NULL) {
        PhoneData* pCurData = mpPhoneWindow->GetPhoneData();

        if (!CompatibleDevices(pCurData, pPhoneData)) {
            /*
             * We need to trash the window.  This will also kill the
             * runtime.  If it's running, ask permission.
             */
            if (IsRuntimeRunning()) {
                wxString msg;
                int sel;

                msg =  wxT("Switching to the new device requires restarting the");
                msg += wxT(" runtime.  Continue?");

                sel = wxMessageBox(msg, wxT("Android Safety Patrol"),
                    wxOK | wxCANCEL | wxICON_QUESTION, this);
                printf("BUTTON was %d (ok=%d)\n", sel, wxOK);
                if (sel == wxCANCEL)
                    goto bail;

                /* shut it down (politely), ask for an eventual restart */
                mpPhoneWindow->GetDeviceManager()->StopRuntime();
                mpPhoneWindow->Close();
                mpPhoneWindow = NULL;
                mRestartRequested = true;
                goto bail;
            } else {
                /* not running, just trash the window and continue */
                mpPhoneWindow->Close();
                mpPhoneWindow = NULL;
            }
        }
    }

    /*
     * Figure out the set of available modes.
     */

    numChoices = pPhoneData->GetNumModes();
    if (numChoices > 0) {
        choices = new wxString[numChoices];
        for (i = 0; i < numChoices; i++) {
            PhoneMode* pPhoneMode;
            pPhoneMode = pPhoneData->GetPhoneMode(i);
            choices[i] = wxString::FromAscii(pPhoneMode->GetName());
            if (defaultMode != NULL &&
                strcmp(defaultMode, pPhoneMode->GetName()) == 0)
            {
                haveDefaultMode = true;
            }
        }
    }

    if (choices == NULL) {
        /* had a failure earlier; configure UI with default stuff */
        choices = new wxString[1];
        choices[0] = wxT("(none)");
    }

    if (!haveDefaultMode) {
        /*
         * Default mode wasn't found.  If we specify it as the default
         * in the wxComboBox create call it shows up in the combo box
         * under Linux, even if it doesn't exist in the list.  So, we
         * make sure that it doesn't get used if we can't find it.
         */
        if (defaultMode != NULL) {
            printf("Sim: HEY: default mode '%s' not found in list\n",
                defaultMode);
        }
        currentMode = choices[0].ToAscii();
    } else {
        currentMode = defaultMode;
    }


    /*
     * Create the window if necessary.
     */
    if (mpPhoneWindow == NULL) {
        // create, setup, and then show window
        mpPhoneWindow = new PhoneWindow(this, mPhoneWindowPosn);
        mpPhoneWindow->SetCurrentMode((const char*)currentMode);
        if (!mpPhoneWindow->Setup(idx)) {
            delete mpPhoneWindow;
            mpPhoneWindow = NULL;
        }
        if (mpPhoneWindow != NULL) {
            mpPhoneWindow->Show();
            //mpPhoneWindow->CheckPlacement();
        }
    } else {
        // just set up for new device
        mpPhoneWindow->SetCurrentMode((const char*)currentMode);
        if (!mpPhoneWindow->Setup(idx)) {
            // it's in an uncertain state, blow it away
            delete mpPhoneWindow;
            mpPhoneWindow = NULL;
        }
    }

    /*
     * Reconfigure mode selection box.
     */
    wxComboBox* pModeSelection;
    pModeSelection = (wxComboBox*)FindWindow(IDC_MODE_SELECT);
    pModeSelection->Clear();
    for (i = 0; i < numChoices; i++)
        pModeSelection->Append(choices[i]);
    pModeSelection->SetSelection(0);
    pModeSelection->Enable(numChoices > 1);
    
    /*
     * configure qwerty keyboard attribute
     */
    numKeyboards = pPhoneData->GetNumKeyboards();
    if (numKeyboards > 0) {
        // only use the first keyboard for now
        PhoneKeyboard* pPhoneKeyboard;
        pPhoneKeyboard = pPhoneData->GetPhoneKeyboard(0);
        if (pPhoneKeyboard->getQwerty()) {
            printf("Sim: set 'qwerty' env\n");
            setenv("qwerty", "true", true);
        }
    }
    
bail:
    delete[] choices;
}

/*
 * Figure out which device is currently selected.
 *
 * The easiest way to do this is just run down the list of possible IDs
 * and stop when something claims to be checked.
 *
 * Returns -1 if it can't find a checked item (which can happen if no
 * device layouts were found).
 */
int MainFrame::GetSelectedDeviceIndex(void)
{
    wxMenuBar* pMenuBar;
    wxMenu* pMenu;
    int idx;
    
    pMenuBar = GetMenuBar();
    idx = pMenuBar->FindMenu(kDeviceMenuString);
    if (idx == wxNOT_FOUND) {
        fprintf(stderr, "Sim: couldn't find %s menu\n", (const char*) kDeviceMenuString.ToAscii());
        return -1;
    }

    pMenu = pMenuBar->GetMenu(idx);

    //printf("Menu.MenuItemCount = %d\n", pMenu->GetMenuItemCount());
    for (int j = pMenu->GetMenuItemCount() -1; j >= 0; j--) {
        wxMenuItem* pItem;

        pItem = pMenu->FindItemByPosition(j);
        //printf("ITEM %d: %s\n", j, (const char*) pItem->GetLabel());
        if (pItem->IsChecked()) {
            printf("Sim: selected device is '%s'\n",
                (const char*) pItem->GetLabel().ToAscii());
            return j;
        }
    }

    return -1;
}

/*
 * Receive a status message from the runtime thread.
 */
void MainFrame::OnUserEvent(UserEvent& event)
{
    UserEventMessage* pUem;

    pUem = (UserEventMessage*) event.GetData();
    assert(pUem != NULL);

    switch (pUem->GetType()) {
    case UserEventMessage::kRuntimeStarted:
        printf("Sim: runtime thread started!\n");
        HandleRuntimeStart();
        break;
    case UserEventMessage::kRuntimeStopped:
        printf("Sim: runtime thread stopped!\n");
        HandleRuntimeStop();
        break;
    case UserEventMessage::kErrorMessage:
        {
            wxString msg = pUem->GetString();
            wxMessageBox(msg, wxT("Android Runtime Error"),
                wxOK | wxICON_WARNING, this);
        }
        break;
    case UserEventMessage::kLogMessage:
        mpLogWindow->AddLogMessage(pUem->GetLogMessage());
        break;
    case UserEventMessage::kExternalRuntime:
        HandleExternalRuntime(pUem->GetReader(), pUem->GetWriter());
        break;
    default:
        printf("Sim: MESSAGE: unknown UserEventMessage rcvd (type=%d)\n",
            pUem->GetType());
        break;
    }

    delete pUem;
}

/*
 * The device management thread is up, so the runtime should be fully
 * running shortly.
 */
void MainFrame::HandleRuntimeStart(void)
{
    mSimRunning = true;

    SetStatusText(kStatusRunning, 1);
}

/*
 * The device management thread is exiting, so the runtime must be dead.
 */
void MainFrame::HandleRuntimeStop(void)
{
    mSimRunning = false;

    SetStatusText(kStatusNotRunning, 1);

    if (mRestartRequested) {
        printf("Sim: restarting runtime\n");
        mRestartRequested = false;
        SetupPhoneUI(GetSelectedDeviceIndex(), NULL);
        if (mpPhoneWindow != NULL)
            mpPhoneWindow->GetDeviceManager()->StartRuntime();
    }
}

/*
 * Handle a connection from an external runtime.
 */
void MainFrame::HandleExternalRuntime(android::Pipe* reader,
    android::Pipe* writer)
{
    android::MessageStream msgStream;
    android::Message msg;

    if (IsRuntimeRunning()) {
        /*
         * Tell the new guy to go away.
         */
        if (!msgStream.init(reader, writer, true)) {
            fprintf(stderr, "Sim: WARNING: unable to talk to remote runtime\n");
            goto bail;
        }

        printf("Sim: telling external runtime to go away\n");
        msg.setCommand(android::Simulator::kCommandGoAway, 0);
        msgStream.send(&msg);
    } else {
        printf("Sim: new external runtime wants to talk to us\n");

        /*
         * Launch the pieces necessary to talk to this guy.
         */
        int id = GetSelectedDeviceIndex();
        if (id < 0) {
            fprintf(stderr,
                "Sim: could not identify currently selected device\n");
            goto bail;
        }

        /* kill existing window, so it pops up and reclaims focus */
        if (mpPhoneWindow != NULL) {
            Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
            bool okay;

            if (pPrefs->GetBool("refocus-on-restart", &okay) && okay) {
                printf("Sim: inducing phone window refocus\n");
                mpPhoneWindow->Close(TRUE);     // no veto
                mpPhoneWindow = NULL;
            }
        }

        SetupPhoneUI(id, NULL);
        if (mpPhoneWindow != NULL) {
            mpPhoneWindow->GetDeviceManager()->StartRuntime(reader, writer);
        } else {
            fprintf(stderr, "Sim: ERROR: unable to get runtime going\n");
            goto bail;
        }

        // we don't own these anymore
        reader = writer = NULL;
    }

bail:
    delete reader;
    delete writer;
}

/*
 * The phone window is about to destroy itself.  Get rid of our pointer
 * to it, and record its last position so we can create the new one in
 * the same place.
 */
void MainFrame::PhoneWindowClosing(int x, int y)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();

    mpPhoneWindow = NULL;

    mPhoneWindowPosn.x = x;
    mPhoneWindowPosn.y = y;

    pPrefs->SetInt("window-device-x", x);
    pPrefs->SetInt("window-device-y", y);
}

