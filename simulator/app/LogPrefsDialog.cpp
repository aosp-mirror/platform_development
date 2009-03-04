//
// Copyright 2005 The Android Open Source Project
//
// Log preferences modal dialog.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"
// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif

#include "LogPrefsDialog.h"
#include "Preferences.h"
#include "Resource.h"
#include "utils.h"

BEGIN_EVENT_TABLE(LogPrefsDialog, wxDialog)
    EVT_CHECKBOX(IDC_LOG_PREFS_WRITE_FILE, LogPrefsDialog::OnWriteFile)
END_EVENT_TABLE()

static const wxString gSpacerChoices[] = { 
    wxT("0"), wxT("1"), wxT("2") 
};
static const wxString gPointSizes[] = { 
    wxT("4"), wxT("6"), wxT("8"), wxT("10"), wxT("12"), wxT("14"), wxT("16") 
};


/*
 * Constructor.
 */
LogPrefsDialog::LogPrefsDialog(wxWindow* parent)
    : wxDialog(parent, IDD_LOG_PREFS, wxT("Log Preferences"), wxDefaultPosition,
        wxDefaultSize, wxDEFAULT_DIALOG_STYLE),
      mHeaderFormat(kHFFull), mSingleLine(false), mExtraSpacing(0),
      mUseColor(false), mFontMonospace(false), mDisplayMax(0), mPoolSizeKB(0)
{
    CreateControls();
}


/*
 * Destructor.  Not much to do.
 */
LogPrefsDialog::~LogPrefsDialog(void)
{
}

/*
 * Create all of the pages and add them to the notebook.
 */
void LogPrefsDialog::CreateControls(void)
{
    wxBoxSizer* mainSizer = new wxBoxSizer(wxVERTICAL);
    wxBoxSizer* okCancelSizer = new wxBoxSizer(wxHORIZONTAL);
    mNotebook.Create(this, wxID_ANY);
    wxPanel* page;

    page = CreateFormatPage(&mNotebook);
    mNotebook.AddPage(page, wxT("Format"), true);
    page = CreateLimitsPage(&mNotebook);
    mNotebook.AddPage(page, wxT("Limits"), false);
    page = CreateFilesPage(&mNotebook);
    mNotebook.AddPage(page, wxT("Files"), false);

    // note to self: could use CreateButtonSizer here?
    wxButton* cancel = new wxButton(this, wxID_CANCEL, wxT("&Cancel"),
        wxDefaultPosition, wxDefaultSize, 0);
    okCancelSizer->Add(cancel, 0, wxALL, kInterSpacing);

    wxButton* ok = new wxButton(this, wxID_OK, wxT("&OK"),
        wxDefaultPosition, wxDefaultSize, 0);
    okCancelSizer->Add(ok, 0, wxALL, kInterSpacing);

    mainSizer->Add(&mNotebook);
    mainSizer->Add(okCancelSizer, 0, wxALIGN_RIGHT);

    SetSizer(mainSizer);

    mainSizer->Fit(this);           // shrink-to-fit
    mainSizer->SetSizeHints(this);  // define minimum size
}

/*
 * Transfer data from our members to the window controls.
 */
bool LogPrefsDialog::TransferDataToWindow(void)
{
    /*
     * Do standard dialog setup.
     */
    wxRadioButton* fmtFull = (wxRadioButton*) FindWindow(IDC_LOG_PREFS_FMT_FULL);
    wxRadioButton* fmtBrief = (wxRadioButton*) FindWindow(IDC_LOG_PREFS_FMT_BRIEF);
    wxRadioButton* fmtMinimal = (wxRadioButton*) FindWindow(IDC_LOG_PREFS_FMT_MINIMAL);
    wxCheckBox* singleLine = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_SINGLE_LINE);
    wxComboBox* extraSpacing = (wxComboBox*) FindWindow(IDC_LOG_PREFS_EXTRA_SPACING);
    wxComboBox* pointSize = (wxComboBox*) FindWindow(IDC_LOG_PREFS_POINT_SIZE);
    wxCheckBox* useColor = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_USE_COLOR);
    wxCheckBox* fontMono = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_FONT_MONO);
    // -
    wxTextCtrl* displayMax = (wxTextCtrl*) FindWindow(IDC_LOG_PREFS_DISPLAY_MAX);
    wxTextCtrl* poolSize = (wxTextCtrl*) FindWindow(IDC_LOG_PREFS_POOL_SIZE);
    // -
    wxCheckBox* writeFile = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_WRITE_FILE);
    wxTextCtrl* fileName = (wxTextCtrl*) FindWindow(IDC_LOG_PREFS_FILENAME);
    wxCheckBox* truncateOld = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_TRUNCATE_OLD);
    // -

    fmtFull->SetValue(mHeaderFormat == kHFFull);
    fmtBrief->SetValue(mHeaderFormat == kHFBrief);
    fmtMinimal->SetValue(mHeaderFormat == kHFMinimal);
    singleLine->SetValue(mSingleLine);
    if (mExtraSpacing < 0 || mExtraSpacing > NELEM(gSpacerChoices))
        mExtraSpacing = 0;
    extraSpacing->SetSelection(mExtraSpacing);

    pointSize->SetSelection(0);
    for (int i = 0; i < NELEM(gPointSizes); i++) {
        if (atoi(gPointSizes[i].ToAscii()) == mPointSize) {
            pointSize->SetSelection(i);
            break;
        }
    }
    useColor->SetValue(mUseColor);
    fontMono->SetValue(mFontMonospace);

    wxString tmpStr;
    tmpStr.Printf(wxT("%d"), mDisplayMax);
    displayMax->SetValue(tmpStr);
    tmpStr.Printf(wxT("%d"), mPoolSizeKB);
    poolSize->SetValue(tmpStr);

    writeFile->SetValue(mWriteFile);
    fileName->SetValue(mFileName);
    truncateOld->SetValue(mTruncateOld);

    EnableFileControls(mWriteFile);

    return true;
}

/*
 * Convert a string to a number.  The number is expected to be unsigned.
 * Returns < 0 on failure.
 */
static long ConvertUnsigned(const wxString& str)
{
    long val;
    if (!str.ToLong(&val))
        return -1;
    return val;
}

/*
 * Transfer and validate data from the window controls.
 *
 * This doesn't get called if the user cancels out of the dialog.
 */
bool LogPrefsDialog::TransferDataFromWindow(void)
{
    /*
     * Do standard dialog export.
     */
    //wxRadioButton* fmtFull = (wxRadioButton*) FindWindow(IDC_LOG_PREFS_FMT_FULL);
    wxRadioButton* fmtBrief = (wxRadioButton*) FindWindow(IDC_LOG_PREFS_FMT_BRIEF);
    wxRadioButton* fmtMinimal = (wxRadioButton*) FindWindow(IDC_LOG_PREFS_FMT_MINIMAL);
    wxCheckBox* singleLine = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_SINGLE_LINE);
    wxComboBox* extraSpacing = (wxComboBox*) FindWindow(IDC_LOG_PREFS_EXTRA_SPACING);
    wxComboBox* pointSize = (wxComboBox*) FindWindow(IDC_LOG_PREFS_POINT_SIZE);
    wxCheckBox* useColor = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_USE_COLOR);
    wxCheckBox* fontMono = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_FONT_MONO);
    // -
    wxTextCtrl* displayMax = (wxTextCtrl*) FindWindow(IDC_LOG_PREFS_DISPLAY_MAX);
    wxTextCtrl* poolSize = (wxTextCtrl*) FindWindow(IDC_LOG_PREFS_POOL_SIZE);
    // -
    wxCheckBox* writeFile = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_WRITE_FILE);
    wxTextCtrl* fileName = (wxTextCtrl*) FindWindow(IDC_LOG_PREFS_FILENAME);
    wxCheckBox* truncateOld = (wxCheckBox*) FindWindow(IDC_LOG_PREFS_TRUNCATE_OLD);
    // -

    mHeaderFormat = kHFFull;
    if (fmtBrief->GetValue())
        mHeaderFormat = kHFBrief;
    else if (fmtMinimal->GetValue())
        mHeaderFormat = kHFMinimal;

    wxString tmpStr;

    mSingleLine = (singleLine->GetValue() != 0);
    mExtraSpacing = extraSpacing->GetSelection();
    mPointSize = ConvertUnsigned(pointSize->GetValue());
    mUseColor = useColor->GetValue();
    mFontMonospace = fontMono->GetValue();

    tmpStr = displayMax->GetValue();
    mDisplayMax = ConvertUnsigned(tmpStr);
    if (mDisplayMax <= 0 || mDisplayMax > 1000 * 1000) {
        wxMessageBox(wxT("Bad value for display max -- must be > 0 and <= 1,000,000"),
            wxT("Hoser"), wxOK, this);
        return false;
    }

    tmpStr = poolSize->GetValue();
    mPoolSizeKB = ConvertUnsigned(tmpStr);
    if (mDisplayMax <= 0 || mDisplayMax > 1048576) {
        wxMessageBox(wxT("Bad value for pool size -- must be > 0 and <= 1048576"),
            wxT("Hoser"), wxOK, this);
        return false;
    }

    mWriteFile = (writeFile->GetValue() != 0);
    mFileName = fileName->GetValue();
    mTruncateOld = (truncateOld->GetValue() != 0);
    if (mWriteFile && mFileName.IsEmpty()) {
        wxMessageBox(wxT("Log filename may not be blank"),
            wxT("Hoser"), wxOK, this);
        return false;
    }

    return true;
}


/*
 * Create the log Format page.
 */
wxPanel* LogPrefsDialog::CreateFormatPage(wxBookCtrlBase* parent)
{
    wxPanel* panel = new wxPanel(parent);

    wxStaticBoxSizer* headerOpts = new wxStaticBoxSizer(wxVERTICAL, panel,
        wxT("Header"));
    headerOpts->Add(new wxRadioButton(panel, IDC_LOG_PREFS_FMT_FULL,
            wxT("Full header"), wxDefaultPosition, wxDefaultSize,
            wxRB_GROUP));
    headerOpts->Add(new wxRadioButton(panel, IDC_LOG_PREFS_FMT_BRIEF,
            wxT("Brief header")));
    headerOpts->Add(new wxRadioButton(panel, IDC_LOG_PREFS_FMT_MINIMAL,
            wxT("Minimal, integrated header")));

    wxCheckBox* singleLine = new wxCheckBox(panel, IDC_LOG_PREFS_SINGLE_LINE,
        wxT("Put headers and message on same line"));

    wxStaticText* extraSpacingDescr = new wxStaticText(panel, wxID_STATIC,
        wxT("Extra line spacing:"));
    wxComboBox* extraSpacing = new wxComboBox(panel,
        IDC_LOG_PREFS_EXTRA_SPACING, wxT("blah"),
        wxDefaultPosition, wxDefaultSize, NELEM(gSpacerChoices),
        gSpacerChoices, wxCB_READONLY);
    wxBoxSizer* extraSpacingSizer = new wxBoxSizer(wxHORIZONTAL);
    extraSpacingSizer->Add(extraSpacingDescr, 0, wxALIGN_CENTER_VERTICAL);
    extraSpacingSizer->AddSpacer(kInterSpacing);
    extraSpacingSizer->Add(extraSpacing);

    wxStaticBoxSizer* textOpts = new wxStaticBoxSizer(wxVERTICAL, panel,
        wxT("Text"));
    textOpts->Add(
            new wxStaticText(panel, wxID_STATIC, wxT("Point size:")) );
    textOpts->AddSpacer(kInterSpacing);
    textOpts->Add(
        new wxComboBox(panel,
            IDC_LOG_PREFS_POINT_SIZE, wxT("blah"),
            wxDefaultPosition, wxDefaultSize, NELEM(gPointSizes),
            gPointSizes, wxCB_READONLY) );
    textOpts->AddSpacer(kInterSpacing);
    textOpts->Add(
            new wxCheckBox(panel, IDC_LOG_PREFS_USE_COLOR,
                wxT("Colorful messages")) );
    textOpts->AddSpacer(kInterSpacing);
    textOpts->Add(
            new wxCheckBox(panel, IDC_LOG_PREFS_FONT_MONO,
                wxT("Use monospace font")) );


    wxBoxSizer* sizerPanel = new wxBoxSizer(wxVERTICAL);
    sizerPanel->Add(kMinWidth, kEdgeSpacing);       // forces minimum width
    sizerPanel->Add(headerOpts);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(singleLine);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(extraSpacingSizer);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(textOpts);
    sizerPanel->AddSpacer(kInterSpacing);

    wxBoxSizer* horizIndent = new wxBoxSizer(wxHORIZONTAL);
    horizIndent->AddSpacer(kEdgeSpacing);
    horizIndent->Add(sizerPanel);
    horizIndent->AddSpacer(kEdgeSpacing);
    panel->SetSizer(horizIndent);

    return panel;
}

/*
 * Create the log Limits page.
 */
wxPanel* LogPrefsDialog::CreateLimitsPage(wxBookCtrlBase* parent)
{
    wxPanel* panel = new wxPanel(parent);

    wxBoxSizer* displayMaxSizer = new wxBoxSizer(wxHORIZONTAL);
    displayMaxSizer->Add(
            new wxStaticText(panel, wxID_ANY,
                wxT("Maximum entries in log window:"),
                wxDefaultPosition, wxDefaultSize,
                wxALIGN_LEFT),
            0, wxALIGN_CENTER_VERTICAL);
    displayMaxSizer->AddSpacer(kInterSpacing);
    displayMaxSizer->Add(
            new wxTextCtrl(panel, IDC_LOG_PREFS_DISPLAY_MAX));

    wxBoxSizer* poolSizeSizer = new wxBoxSizer(wxHORIZONTAL);
    poolSizeSizer->Add(
            new wxStaticText(panel, wxID_ANY,
                wxT("Size of the log pool (KB):"),
                wxDefaultPosition, wxDefaultSize,
                wxALIGN_LEFT),
            0, wxALIGN_CENTER_VERTICAL);
    poolSizeSizer->AddSpacer(kInterSpacing);
    poolSizeSizer->Add(
            new wxTextCtrl(panel, IDC_LOG_PREFS_POOL_SIZE));


    wxBoxSizer* sizerPanel = new wxBoxSizer(wxVERTICAL);
    sizerPanel->Add(kMinWidth, kEdgeSpacing);       // forces minimum width
    sizerPanel->Add(displayMaxSizer);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(poolSizeSizer);
    sizerPanel->AddSpacer(kInterSpacing);

    wxBoxSizer* horizIndent = new wxBoxSizer(wxHORIZONTAL);
    horizIndent->AddSpacer(kEdgeSpacing);
    horizIndent->Add(sizerPanel);
    horizIndent->AddSpacer(kEdgeSpacing);
    panel->SetSizer(horizIndent);

    return panel;
}

/*
 * Create the log Files page.
 */
wxPanel* LogPrefsDialog::CreateFilesPage(wxBookCtrlBase* parent)
{
    wxPanel* panel = new wxPanel(parent);
    wxStaticBoxSizer* logOpts = new wxStaticBoxSizer(wxVERTICAL, panel,
        wxT("Log File"));

    wxCheckBox* writeCopy =
            new wxCheckBox(panel, IDC_LOG_PREFS_WRITE_FILE,
                wxT("Write a copy of log output to a file"));

    logOpts->AddSpacer(kInterSpacing);
    logOpts->Add(
            new wxStaticText(panel, wxID_ANY,
                wxT("Filename:"),
                wxDefaultPosition, wxDefaultSize,
                wxALIGN_LEFT));
    logOpts->AddSpacer(kInterSpacing);
    logOpts->Add(
            new wxTextCtrl(panel, IDC_LOG_PREFS_FILENAME), 0, wxEXPAND);
    logOpts->AddSpacer(kInterSpacing);
    logOpts->Add(
            new wxCheckBox(panel, IDC_LOG_PREFS_TRUNCATE_OLD,
                wxT("Truncate the file if more than 8 hours old ")) );


    wxBoxSizer* sizerPanel = new wxBoxSizer(wxVERTICAL);
    sizerPanel->Add(kMinWidth, kEdgeSpacing);       // forces minimum width
    sizerPanel->Add(writeCopy);
    sizerPanel->AddSpacer(kInterSpacing);
    sizerPanel->Add(logOpts);
    sizerPanel->AddSpacer(kInterSpacing);

    wxBoxSizer* horizIndent = new wxBoxSizer(wxHORIZONTAL);
    horizIndent->AddSpacer(kEdgeSpacing);
    horizIndent->Add(sizerPanel);
    horizIndent->AddSpacer(kEdgeSpacing);
    panel->SetSizer(horizIndent);

    return panel;
}


/*
 * Handle clicks on the "write file" checkbox.
 */
void LogPrefsDialog::OnWriteFile(wxCommandEvent& event)
{
    EnableFileControls(event.GetInt());
}

/*
 * Enable or disable some of the controls on the "file" page.
 */
void LogPrefsDialog::EnableFileControls(bool enable)
{
    FindWindow(IDC_LOG_PREFS_FILENAME)->Enable(enable);
    FindWindow(IDC_LOG_PREFS_TRUNCATE_OLD)->Enable(enable);
}

