//
// Copyright 2005 The Android Open Source Project
//
// Window with simulated phone.
//
#ifndef _SIM_DEVICE_WINDOW_H
#define _SIM_DEVICE_WINDOW_H

#include "UserEvent.h"
#include "DeviceManager.h"

/*
 * This window displays the device output.
 */
class DeviceWindow : public wxWindow {
public:
    DeviceWindow(wxWindow* parent, DeviceManager* pDM);
    virtual ~DeviceWindow(void);

#if 0       // can't work -- can't create bitmaps in other threads
    /* this gets tucked into a user event */
    class FrameData {
    public:
        FrameData(void)
            : mDisplayIndex(-1), mpBitmap(NULL)
            {}
        ~FrameData(void) {
            delete mpBitmap;
        }

        void Create(int displayIndex, wxBitmap* pBitmap) {
            mDisplayIndex = displayIndex;
            mpBitmap = pBitmap;
        }

        int GetDisplayIndex(void) const { return mDisplayIndex; }
        wxBitmap* GetBitmap(void) const { return mpBitmap; }

    private:
        int         mDisplayIndex;
        wxBitmap*   mpBitmap;
    };
#endif

    void DeviceManagerClosing(void) { mpDeviceManager = NULL; }

private:
    void OnKeyDown(wxKeyEvent& event);
    void OnKeyUp(wxKeyEvent& event);
    void OnMouseLeftDown(wxMouseEvent& event);
    void OnMouseLeftUp(wxMouseEvent& event);
    void OnMouseRightDown(wxMouseEvent& event);
    void OnMouseRightUp(wxMouseEvent& event);
    void OnMouseMotion(wxMouseEvent& event);
    void OnSize(wxSizeEvent& WXUNUSED(event));
    void OnErase(wxEraseEvent& event);
    void OnPaint(wxPaintEvent& WXUNUSED(event));
    void OnUserEvent(UserEvent& event);

    void ClampMouse(wxMouseEvent* pEvent);

    DeviceManager*  mpDeviceManager;
    wxBitmap    mBitmap;
    wxBitmap	mOnionSkinBitmap;
    bool        mHasOnionSkinBitmap;

    DECLARE_EVENT_TABLE()
};

#endif // _SIM_DEVICE_WINDOW_H
