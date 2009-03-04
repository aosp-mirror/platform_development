//
// Copyright 2005 The Android Open Source Project
//
// Window with simulated phone.
//
#ifndef _SIM_PHONE_WINDOW_H
#define _SIM_PHONE_WINDOW_H

#include "PhoneData.h"
#include "DeviceManager.h"
#include "DeviceWindow.h"
#include <ui/KeycodeLabels.h>

class MainFrame;

/*
 * This window displays the simulated phone views, and handles keyboard and
 * mouse input.
 *
 * If we switch to a different "mode", we may display different "views",
 * but the set of "displays" remains the same.  (Got that?)
 *
 * We can't just do these things in the main frame because we can't easily
 * grab the keyboard input.
 */
class PhoneWindow : public wxDialog {
public:
    PhoneWindow(wxWindow* parent, const wxPoint& posn);
    virtual ~PhoneWindow(void);

    /* call this initially, and after a mode change */
    bool Setup(int phoneIdx);

    bool IsReady(void) const {
        return (mNumViewInfo > 0 && mpViewInfo != NULL);
    }

    PhoneData* GetPhoneData(void) const;

    const wxString& GetCurrentMode(void) const { return mCurrentMode; }
    void SetCurrentMode(const wxString& mode) { mCurrentMode = mode; }
    void SetCurrentMode(const char* mode) { mCurrentMode = wxString::FromAscii(mode); }

    DeviceManager* GetDeviceManager(void) { return &mDeviceManager; }

    /* this is called when the phone data is reloaded */
    void DevicesRescanned(void);

    void Vibrate(int vibrateOn);

private:
    /*
     * Hold some information about the "views" being shown in our window.
     */
    class ViewInfo {
    public:
        ViewInfo(void)
            : mX(-1), mY(-1), mDisplayX(-1), mDisplayY(-1),
              mWidth(-1), mHeight(-1), mDisplayIndex(-1)
        {}
        ~ViewInfo(void) {}

        int GetX(void) const { return mX; }
        int GetY(void) const { return mY; }
        int GetDisplayX(void) const { return mDisplayX; }
        int GetDisplayY(void) const { return mDisplayY; }
        int GetWidth(void) const { return mWidth; }
        int GetHeight(void) const { return mHeight; }
        int GetDisplayIndex(void) const { return mDisplayIndex; }

        void SetX(int val) { mX = val; }
        void SetY(int val) { mY = val; }
        void SetDisplayX(int val) { mDisplayX = val; }
        void SetDisplayY(int val) { mDisplayY = val; }
        void SetWidth(int val) { mWidth = val; }
        void SetHeight(int val) { mHeight = val; }
        void SetDisplayIndex(int val) { mDisplayIndex = val; }

    private:
        int     mX, mY;                 // view offset within PhoneWindow
        int     mDisplayX, mDisplayY;   // display offset within view
        int     mWidth, mHeight;        // view dimensions

        int     mDisplayIndex;          // index into mpDeviceWindow
    };

    /*
     * Hold information about currently pressed keys.
     */
    class KeyInfo {
    public:
        KeyInfo(void) : mKeyCode(kKeyCodeUnknown) {}
        KeyInfo(const KeyInfo& src) {
            mKeyCode = src.mKeyCode;
        }
        ~KeyInfo(void) {}

        KeyInfo& operator=(const KeyInfo& src) {
            if (this != &src) {
                mKeyCode = src.mKeyCode;
            }
            return *this;
        }

        KeyCode GetKeyCode(void) const { return mKeyCode; }
        void SetKeyCode(KeyCode keyCode) { mKeyCode = keyCode; }

        //PhoneButton* GetPhoneButton(void) const { return mpButton; }
        //void SetPhoneButton(PhoneButton* pButton) { mpButton = pButton; }

    private:
        KeyCode    mKeyCode;
        //PhoneButton*        mpButton;
    };

    void OnActivate(wxActivateEvent& event);
    void OnMove(wxMoveEvent& event);
    void OnClose(wxCloseEvent& event);
    void OnTimer(wxTimerEvent& event);
    void OnKeyDown(wxKeyEvent& event);
    void OnKeyUp(wxKeyEvent& event);
    void OnErase(wxEraseEvent& event);
    void OnPaint(wxPaintEvent& WXUNUSED(event));
    void OnMouseLeftDown(wxMouseEvent& event);
    void OnMouseLeftUp(wxMouseEvent& event);
    void OnMouseRightDown(wxMouseEvent& event);
    void OnMouseRightUp(wxMouseEvent& event);
    void OnMouseMotion(wxMouseEvent& event);
    void OnMouseLeaveWindow(wxMouseEvent& WXUNUSED(event));
    bool GetTouchPosition(const wxMouseEvent& event, int* pScreenX,
        int* pScreenY);

    bool GetDimensions(PhoneData* pPhoneData, PhoneView* pPhoneView,
        ViewInfo* pDim);
    int ConvertKeyCode(int wxKeyCode) const;

    /* press a key on the device */
    void AddPressedKey(KeyCode keyCode);
    /* release a key on the device */
    void RemovePressedKey(KeyCode keyCode);
    /* "raise" all keys */
    void ClearPressedKeys(void);
    /* determine whether a key is down */
    bool IsKeyPressed(KeyCode keyCode);

    /* manage the device runtime */
    DeviceManager   mDeviceManager;

    /* button mouse-over highlight handling */
    int             mpMOHViewIndex;     // mouse is in this view
    PhoneButton*    mpMOHButton;        //   over this button
    KeyCode         mMouseKeySent;     // to handle "key up" for mouse button

    /* handle multiple simultaneous key presses */
    android::List<KeyInfo>  mPressedKeys;
    typedef android::List<KeyInfo>::iterator ListIter;

    /* ViewInfos, 1:1 with PhoneView entries for the current mode */
    ViewInfo*       mpViewInfo;         // array of view data
    int             mNumViewInfo;       // #of elements in mpViewInfo

    /* DeviceWindows, 1:1 with PhoneDisplay entries for this device */
    DeviceWindow**  mpDeviceWindow;     // array of pointers to device windows
    int             mNumDeviceWindows;  // #of device windows

    /* state */
    int             mPhoneModel;        // index into model list
    wxString        mCurrentMode;

    bool            mPlacementChecked;  // leave it offscreen if they want

    MainFrame*      mpParent;           // retain pointer to parent window

    enum { kVibrateTimerId = 1010 };
    wxTimer         mTimer;
    int             mVibrateX;

    /* touchscreen simulation */
    bool            mTrackingTouch;
    int             mTouchX;
    int             mTouchY;

    DECLARE_EVENT_TABLE()
};

#endif // _SIM_PHONE_WINDOW_H
