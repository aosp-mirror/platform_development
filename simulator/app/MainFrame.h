//
// Copyright 2005 The Android Open Source Project
//
// Main window declaration.
//
#ifndef _SIM_MAINFRAME_H
#define _SIM_MAINFRAME_H

#include "PhoneWindow.h"
#include "DeviceWindow.h"
#include "LogWindow.h"
#include "ExternalRuntime.h"
#include "PropertyServer.h"

/*
 * Main window.
 */
class MainFrame : public wxFrame {
public:
    /* define a constructor so we can set up menus */
    MainFrame(const wxString& title, const wxPoint& pos, const wxSize& size,
        long style);
    virtual ~MainFrame(void);

    /* called by modeless phone window dialog when it closes */
    void PhoneWindowClosing(int x, int y);

    void Vibrate(int vibrateOn) { mpPhoneWindow->Vibrate(vibrateOn); }

    PropertyServer* GetPropertyServer(void) { return mPropertyServerThread; }

private:
    void ConstructMenu(void);
    void ConstructControls(void);

    void OnClose(wxCloseEvent& event);
    void OnTimer(wxTimerEvent& event);
    //void OnIdle(wxIdleEvent& event);
    void OnActivate(wxActivateEvent& event);
    void OnButton(wxCommandEvent& event);
    void OnComboBox(wxCommandEvent& event);
    void OnCheckBox(wxCommandEvent& event);
    void OnText(wxCommandEvent& event);
    void OnTextEnter(wxCommandEvent& event);
    void OnUserEvent(UserEvent& event);
    void OnSliderChange(wxScrollEvent& event);

    void OnFilePreferences(wxCommandEvent& event);
    void OnFileExit(wxCommandEvent& event);
    void OnUpdateSimStart(wxUpdateUIEvent& event);
    void OnSimStart(wxCommandEvent& event);
    void OnUpdateSimStop(wxUpdateUIEvent& event);
    void OnSimStop(wxCommandEvent& event);
    void OnUpdateSimReload(wxUpdateUIEvent& event);
    void OnSimReload(wxCommandEvent& event);
    void OnUpdateSimRestart(wxUpdateUIEvent& event);
    void OnSimRestart(wxCommandEvent& event);
    void OnUpdateSimKill(wxUpdateUIEvent& event);
    void OnSimKill(wxCommandEvent& event);
    void OnDeviceSelected(wxCommandEvent& event);
    void OnDeviceRescan(wxCommandEvent& event);
    void OnUpdateDebugShowLog(wxUpdateUIEvent& event);
    void OnDebugShowLog(wxCommandEvent& event);
    void OnHelpContents(wxCommandEvent& event);
    void OnHelpAbout(wxCommandEvent& event);

    wxMenu* CreateDeviceMenu(const char* defaultItemName);
    void SetCheckFromPref(wxCheckBox* pControl, const char* prefStr,
        bool defaultVal);

    void UpdateRuntimeExeStr(void);

    /* prep the phone UI; "defaultMode" may be NULL */
    void SetupPhoneUI(int idx, const char* defaultMode);

    bool CompatibleDevices(PhoneData* pData1, PhoneData* pData2);

    void HandleRuntimeStart(void);
    void HandleRuntimeStop(void);
    void HandleExternalRuntime(android::Pipe* reader, android::Pipe* writer);

    int GetSelectedDeviceIndex(void);
    bool IsRuntimeRunning(void);
    bool IsRuntimeKillable(void);

    void BroadcastOnionSkinUpdate(void);
    
    bool    mSimRunning;
    bool    mRestartRequested;

    enum { kHalfSecondTimerId = 1000 };

    wxString        mSimAssetPath;

    /* if we have a phone running, this points to its state */
    PhoneWindow*    mpPhoneWindow;

    /* phone window position */
    wxPoint         mPhoneWindowPosn;

    /* window that captures log output */
    LogWindow*      mpLogWindow;

    wxTimer         mTimer;

    /* watches for connection from runtime */
    ExternalRuntime* mExternalRuntimeThread;

    /* serve up system properties */
    PropertyServer*  mPropertyServerThread;

    DECLARE_EVENT_TABLE()
};

#endif // _SIM_MAINFRAME_H
