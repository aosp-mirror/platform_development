//
// Copyright 2005 The Android Open Source Project
//
// Preferences modal dialog.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"
// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif

#include "PrefsDialog.h"
#include "Preferences.h"
#include "MyApp.h"
#include "Resource.h"

BEGIN_EVENT_TABLE(PrefsDialog, wxDialog)
END_EVENT_TABLE()

/*
 * Constructor.
 */
PrefsDialog::PrefsDialog(wxWindow* parent)
    : wxDialog(parent, IDD_PREFS, wxT("Preferences"), wxDefaultPosition,
        wxDefaultSize, wxDEFAULT_DIALOG_STYLE | wxRESIZE_BORDER ),
      mAutoPowerOn(false),
      mGammaCorrection(1.0),
      mEnableSound(true),
      mEnableFakeCamera(true),
      mLogLevel(0)
{
    LoadPreferences();   
    CreateControls();
}

/*
 * Destructor.  Not much to do.
 */
PrefsDialog::~PrefsDialog()
{
}

/*
 * Create all of the pages and add them to the notebook.
 */
void PrefsDialog::CreateControls(void)
{
    wxBoxSizer* mainSizer = new wxBoxSizer(wxVERTICAL);
    wxBoxSizer* okCancelSizer = new wxBoxSizer(wxHORIZONTAL);
    mNotebook.Create(this, wxID_ANY);
    wxPanel* page;

    /* pages added to notebook are owned by notebook */
    page = CreateSimulatorPage(&mNotebook);
    mNotebook.AddPage(page, wxT("Simulator"), true);       // selected page
    page = CreateRuntimePage(&mNotebook);
    mNotebook.AddPage(page, wxT("Runtime"), false);

    wxButton* cancel = new wxButton(this, wxID_CANCEL, wxT("&Cancel"),
        wxDefaultPosition, wxDefaultSize, 0);
    okCancelSizer->Add(cancel, 0, wxALL | wxALIGN_RIGHT, kInterSpacing);

    wxButton* ok = new wxButton(this, wxID_OK, wxT("&OK"),
        wxDefaultPosition, wxDefaultSize, 0);
    okCancelSizer->Add(ok, 0, wxALL | wxALIGN_RIGHT, kInterSpacing);

    mainSizer->Add(&mNotebook, 1, wxEXPAND);
    mainSizer->Add(okCancelSizer, 0, wxALIGN_RIGHT);

    SetSizer(mainSizer);

    mainSizer->Fit(this);           // shrink-to-fit
    mainSizer->SetSizeHints(this);  // define minimum size
}

/*
 * Load preferences from config file
 */
void PrefsDialog::LoadPreferences(void)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    assert(pPrefs != NULL);

    /*
     * Load preferences.
     */
    mConfigFile = ((MyApp*)wxTheApp)->GetConfigFileName();

    pPrefs->GetDouble("gamma", &mGammaCorrection);
    pPrefs->GetString("debugger", /*ref*/ mDebugger);
    pPrefs->GetString("valgrinder", /*ref*/ mValgrinder);
    pPrefs->GetBool("auto-power-on", &mAutoPowerOn);
    pPrefs->GetBool("enable-sound", &mEnableSound);
    pPrefs->GetBool("enable-fake-camera", &mEnableFakeCamera);
}

/*
 * Transfer data from our members to the window controls.
 *
 * First we have to pull the data out of the preferences database.
 * Anything that hasn't already been added with a default value will
 * be given a default here, which may or may not match the default
 * behavior elsewhere.  The best solution to this is to define the
 * default when the preferences file is created or read, so that we're
 * never left guessing here.
 */
bool PrefsDialog::TransferDataToWindow(void)
{
    /*
     * Do standard dialog setup.
     */
    wxTextCtrl* configFileName = (wxTextCtrl*) FindWindow(IDC_SPREFS_CONFIG_NAME);
    wxTextCtrl* debugger = (wxTextCtrl*) FindWindow(IDC_SPREFS_DEBUGGER);
    wxTextCtrl* valgrinder = (wxTextCtrl*) FindWindow(IDC_SPREFS_VALGRINDER);
    wxCheckBox* autoPowerOn = (wxCheckBox*) FindWindow(IDC_SPREFS_AUTO_POWER_ON);
    wxCheckBox* enableSound = (wxCheckBox*) FindWindow(IDC_RPREFS_ENABLE_SOUND);
    wxCheckBox* enableFakeCamera = (wxCheckBox*) FindWindow(IDC_RPREFS_ENABLE_FAKE_CAMERA);

    wxTextCtrl* gamma = (wxTextCtrl*) FindWindow(IDC_RPREFS_GAMMA);

    configFileName->SetValue(mConfigFile);
    debugger->SetValue(mDebugger);
    valgrinder->SetValue(mValgrinder);
    autoPowerOn->SetValue(mAutoPowerOn);
    enableSound->SetValue(mEnableSound);
    enableFakeCamera->SetValue(mEnableFakeCamera);

    wxString tmpStr;
    tmpStr.Printf(wxT("%.3f"), mGammaCorrection);
    gamma->SetValue(tmpStr);

    return true;
}

/*
 * Transfer and validate data from the window controls.
 *
 * This doesn't get called if the user cancels out of the dialog.
 */
bool PrefsDialog::TransferDataFromControls(void)
{
    /*
     * Do standard dialog export.
     *
     * We should error-check all of these.
     */
    // configName is read-only, don't need it here
    wxTextCtrl* debugger = (wxTextCtrl*) FindWindow(IDC_SPREFS_DEBUGGER);
    wxTextCtrl* valgrinder = (wxTextCtrl*) FindWindow(IDC_SPREFS_VALGRINDER);
    wxCheckBox* autoPowerOn = (wxCheckBox*) FindWindow(IDC_SPREFS_AUTO_POWER_ON);
    wxCheckBox* enableSound = (wxCheckBox*) FindWindow(IDC_RPREFS_ENABLE_SOUND);
    wxCheckBox* enableFakeCamera = (wxCheckBox*) FindWindow(IDC_RPREFS_ENABLE_FAKE_CAMERA);

    wxTextCtrl* gamma = (wxTextCtrl*) FindWindow(IDC_RPREFS_GAMMA);

    mDebugger = debugger->GetValue();
    mValgrinder = valgrinder->GetValue();
    mAutoPowerOn = autoPowerOn->GetValue();
    mEnableSound = enableSound->GetValue();
    mEnableFakeCamera = enableFakeCamera->GetValue();

    wxString tmpStr;
    tmpStr = gamma->GetValue();
    bool toDouble = tmpStr.ToDouble(&mGammaCorrection);    // returns 0.0 on err; use strtof()?

    if (!toDouble || mGammaCorrection <= 0.0 || mGammaCorrection > 2.0) {
        wxMessageBox(wxT("Bad value for gamma -- must be > 0.0 and <= 2.0"),
            wxT("Hoser"), wxOK, this);
        return false;
    }

    return true;
}

/*
 * Transfer preferences to config file
 */
bool PrefsDialog::TransferDataFromWindow(void)
{
    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    assert(pPrefs != NULL);

    /*
     * Grab the information from the controls and save in member field
     */

    if (!TransferDataFromControls())
        return false;

    pPrefs->SetString("debugger", mDebugger.ToAscii());
    pPrefs->SetString("valgrinder", mValgrinder.ToAscii());
    pPrefs->SetBool("auto-power-on", mAutoPowerOn);
    pPrefs->SetBool("enable-sound", mEnableSound);
    pPrefs->SetBool("enable-fake-camera", mEnableFakeCamera);

    pPrefs->SetDouble("gamma", mGammaCorrection);

    return true;
}


/*
 * Create the Simulator Preferences page.
 */
wxPanel* PrefsDialog::CreateSimulatorPage(wxBookCtrlBase* parent)
{
    wxPanel* panel = new wxPanel(parent);

    wxStaticText* configNameDescr = new wxStaticText(panel, wxID_STATIC,
        wxT("Config file:"));
    wxTextCtrl* configName = new wxTextCtrl(panel, IDC_SPREFS_CONFIG_NAME,
        wxT(""), wxDefaultPosition, wxDefaultSize, wxTE_READONLY);
    // make it visibly different; unfortunately this kills scroll, copy&paste
    configName->Enable(false);

    wxStaticText* debuggerDescr = new wxStaticText(panel, wxID_STATIC,
        wxT("Debugger:"));
    wxTextCtrl* debugger = new wxTextCtrl(panel, IDC_SPREFS_DEBUGGER);

    wxStaticText* valgrinderDescr = new wxStaticText(panel, wxID_STATIC,
        wxT("Valgrind:"));
    wxTextCtrl* valgrinder = new wxTextCtrl(panel, IDC_SPREFS_VALGRINDER);

    wxCheckBox* autoPowerOn = new wxCheckBox(panel, IDC_SPREFS_AUTO_POWER_ON,
        wxT("Boot runtime when simulator starts"));

    wxBoxSizer* sizerPanel = new wxBoxSizer(wxVERTICAL);
    sizerPanel->Add(kMinWidth, kEdgeSpacing);       // forces minimum width
    sizerPanel->Add(configNameDescr);
    sizerPanel->Add(configName, 0, wxEXPAND);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(debuggerDescr);
    sizerPanel->Add(debugger, 0, wxEXPAND);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(valgrinderDescr);
    sizerPanel->Add(valgrinder, 0, wxEXPAND);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(autoPowerOn);
    sizerPanel->AddSpacer(kInterSpacing);

    wxBoxSizer* horizIndent = new wxBoxSizer(wxHORIZONTAL);
    horizIndent->AddSpacer(kEdgeSpacing);
    horizIndent->Add(sizerPanel, wxSHAPED);
    horizIndent->AddSpacer(kEdgeSpacing);
    panel->SetSizer(horizIndent);

    return panel;
}

/*
 * Create the Runtime Preferences page.
 */
wxPanel* PrefsDialog::CreateRuntimePage(wxBookCtrlBase* parent)
{
    wxPanel* panel = new wxPanel(parent);

    wxStaticText* gammaStrDescr = new wxStaticText(panel, wxID_STATIC,
        wxT("Gamma correction:"));
    wxTextCtrl* gammaStr = new wxTextCtrl(panel, IDC_RPREFS_GAMMA);

    wxBoxSizer* gammaSizer = new wxBoxSizer(wxHORIZONTAL);
    gammaSizer->Add(gammaStrDescr, 0, wxALIGN_CENTER_VERTICAL);
    gammaSizer->AddSpacer(kInterSpacing);
    gammaSizer->Add(gammaStr);

    wxBoxSizer* sizerPanel = new wxBoxSizer(wxVERTICAL);
    sizerPanel->Add(kMinWidth, kEdgeSpacing);       // forces minimum width
    sizerPanel->Add(gammaSizer);
    sizerPanel->AddSpacer(kInterSpacing);

    wxCheckBox* enableSound = new wxCheckBox(panel, IDC_RPREFS_ENABLE_SOUND,
        wxT("Enable Sound"));
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(enableSound);

    wxCheckBox* enableFakeCamera = new wxCheckBox(panel, IDC_RPREFS_ENABLE_FAKE_CAMERA,
        wxT("Enable Fake Camera"));
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(enableFakeCamera);

    wxBoxSizer* horizIndent = new wxBoxSizer(wxHORIZONTAL);
    horizIndent->AddSpacer(kEdgeSpacing);
    horizIndent->Add(sizerPanel, wxEXPAND);
    horizIndent->AddSpacer(kEdgeSpacing);
    panel->SetSizer(horizIndent);

    return panel;
}

