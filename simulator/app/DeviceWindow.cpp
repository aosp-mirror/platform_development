//
// Copyright 2005 The Android Open Source Project
//
// Displays output from the device.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"   // needed for Windows build
#include "wx/dcbuffer.h"

#include "AssetStream.h"
#include "DeviceWindow.h"
#include "MyApp.h"
#include "Preferences.h"

BEGIN_EVENT_TABLE(DeviceWindow, wxWindow)
    EVT_SIZE(DeviceWindow::OnSize)
    EVT_ERASE_BACKGROUND(DeviceWindow::OnErase)
    EVT_PAINT(DeviceWindow::OnPaint)
    EVT_KEY_DOWN(DeviceWindow::OnKeyDown)
    EVT_KEY_UP(DeviceWindow::OnKeyUp)

    EVT_LEFT_DOWN(DeviceWindow::OnMouseLeftDown)
    EVT_LEFT_DCLICK(DeviceWindow::OnMouseLeftDown)
    EVT_LEFT_UP(DeviceWindow::OnMouseLeftUp)
    EVT_RIGHT_DOWN(DeviceWindow::OnMouseRightDown)
    EVT_RIGHT_DCLICK(DeviceWindow::OnMouseRightDown)
    EVT_RIGHT_UP(DeviceWindow::OnMouseRightUp)
    EVT_MOTION(DeviceWindow::OnMouseMotion)

    EVT_USER_EVENT(DeviceWindow::OnUserEvent)
END_EVENT_TABLE()


/*
 * Create a new DeviceWindow.  This should be a child of PhoneWindow.
 *
 * Note the DeviceManager may not be fully initialized yet.
 */
DeviceWindow::DeviceWindow(wxWindow* parent, DeviceManager* pDM)
    : wxWindow(parent, wxID_ANY, wxDefaultPosition, wxDefaultSize,
        wxNO_BORDER | wxWANTS_CHARS),
      mpDeviceManager(pDM)
{
    //printf("DW: created (parent=%p DM=%p)\n", parent, pDM);

    SetBackgroundStyle(wxBG_STYLE_CUSTOM);

    // create a trivial bitmap so we have something allocated
    mBitmap.Create(1, 1);

}

/*
 * Destructor.
 */
DeviceWindow::~DeviceWindow(void)
{
}

/*
 * We don't want to trap key or mouse events here.
 *
 * event.Skip() didn't seem to do the trick, so we call AddPendingEvent()
 * to add it to the parent's input queue.
 */
void DeviceWindow::OnKeyDown(wxKeyEvent& event)
{
    //printf("DW: down: %d\n", event.GetKeyCode());
    GetParent()->AddPendingEvent(event);
}
void DeviceWindow::OnKeyUp(wxKeyEvent& event)
{
    //printf("DW:   up: %d\n", event.GetKeyCode());
    GetParent()->AddPendingEvent(event);
}

/*
 * Handle mouse events.  We want to pass these up to the PhoneWindow, since
 * that's where the "touch screen" code is.
 */
void DeviceWindow::OnMouseLeftDown(wxMouseEvent& event)
{
    ClampMouse(&event);
    GetParent()->AddPendingEvent(event);
}
void DeviceWindow::OnMouseLeftUp(wxMouseEvent& event)
{
    ClampMouse(&event);
    GetParent()->AddPendingEvent(event);
}
void DeviceWindow::OnMouseRightDown(wxMouseEvent& event)
{
    ClampMouse(&event);
    GetParent()->AddPendingEvent(event);
}
void DeviceWindow::OnMouseRightUp(wxMouseEvent& event)
{
    ClampMouse(&event);
    GetParent()->AddPendingEvent(event);
}
void DeviceWindow::OnMouseMotion(wxMouseEvent& event)
{
    ClampMouse(&event);
    GetParent()->AddPendingEvent(event);
}

/*
 * Clamp the mouse movement to the window bounds.
 */
void DeviceWindow::ClampMouse(wxMouseEvent* pEvent)
{
    wxWindow* pEventWindow = (wxWindow*) pEvent->GetEventObject();
    int width, height;

    pEventWindow->GetSize(&width, &height);
    if (pEvent->m_x < 0)
        pEvent->m_x = 0;
    else if (pEvent->m_x >= width)
        pEvent->m_x = width-1;

    if (pEvent->m_y < 0)
        pEvent->m_y = 0;
    else if (pEvent->m_y >= height)
        pEvent->m_y = height-1;
}


/*
 * Handle a "user event".  We get these when the runtime wants us to
 * know that it has a new frame of graphics to display.
 * 
 */
void DeviceWindow::OnUserEvent(UserEvent& event)
{
    wxBitmap* pBitmap;
    long displayIndex;

    displayIndex = (long) event.GetData();

    //printf("GOT UAE %d\n", displayIndex);

    // a displayIndex of -1 means just update the onion skin
    if (displayIndex >= 0) {
        /* get a newly-allocated bitmap with converted image data */
        pBitmap = mpDeviceManager->GetImageData(displayIndex);
    
        /* do a ptr/refcount assignment to hold the data */
        mBitmap = *pBitmap;
        /* delete the temporary object; does not delete the bitmap storage */
        delete pBitmap;
    }
    
    if (displayIndex >= -1) {
        mHasOnionSkinBitmap = false;
        
        Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
        assert(pPrefs != NULL);
    
        bool overlayOnionSkin;
        char* onionSkinFileName = NULL;
        
        bool overlayOnionSkinExists = pPrefs->GetBool("overlay-onion-skin", &overlayOnionSkin);
        if (overlayOnionSkinExists && overlayOnionSkin) {
            bool fileNameExists = pPrefs->GetString("onion-skin-file-name", &onionSkinFileName);
            if (fileNameExists && *onionSkinFileName) {
                wxImage onionSkinImage(wxString::FromAscii(onionSkinFileName));
                onionSkinImage.SetAlpha(NULL);
                bool hasAlpha = onionSkinImage.HasAlpha();
                int width = onionSkinImage.GetWidth();
                int height = onionSkinImage.GetHeight();
                if (hasAlpha) {
                    unsigned char *alpha = onionSkinImage.GetAlpha();
                    int alphaVal = 127;
                    pPrefs->GetInt("onion-skin-alpha-value", &alphaVal);
                    for (int i = (width * height) - 1; i >= 0; i--) {
                        alpha[i] = alphaVal;
                    } 
                }
                mOnionSkinBitmap = wxBitmap(onionSkinImage);
                mHasOnionSkinBitmap = true;
            }
        }
    }

    /* induce an update */
    Refresh();
}

/*
 * Window has been moved or resized.
 *
 * We get this when the model of phone is changed.
 *
 * FIX: in the future this only happens when the phone is rotated 90deg.
 */
void DeviceWindow::OnSize(wxSizeEvent& WXUNUSED(event))
{
    int width, height;

    GetClientSize(&width, &height);
    printf("Sim: device window resize: %dx%d\n", width, height);

    mBitmap.Create(width, height);

    wxMemoryDC memDC;
    memDC.SelectObject(mBitmap);

    wxColour backColor(96, 122, 121);
    memDC.SetBrush(wxBrush(backColor));
    memDC.SetPen(wxPen(backColor, 1));
    wxRect windowRect(wxPoint(0, 0), GetClientSize());
    memDC.DrawRectangle(windowRect);
}

/*
 * No need to erase the background.
 */
void DeviceWindow::OnErase(wxEraseEvent& WXUNUSED(event))
{
    //printf("erase device\n");
}

/*
 * Repaint the simulator output.
 */
void DeviceWindow::OnPaint(wxPaintEvent& WXUNUSED(event))
{
    wxPaintDC dc(this);

    /* draw background image */
    dc.DrawBitmap(mBitmap, 0, 0, TRUE);

    /* If necessary, draw onion skin image on top */
    if (mHasOnionSkinBitmap) {
        dc.DrawBitmap(mOnionSkinBitmap, 0, 0, TRUE);
    }
    
#if 0
    // debug - draw the corners
    int xoff = 0;
    int yoff = 0;
    int width;
    int height;
    GetClientSize(&width, &height);

    dc.SetPen(*wxGREEN_PEN);
    dc.DrawLine(xoff,           yoff+9,         xoff,           yoff);
    dc.DrawLine(xoff,           yoff,           xoff+10,        yoff);
    dc.DrawLine(xoff+width-10,  yoff,           xoff+width,     yoff);
    dc.DrawLine(xoff+width-1,   yoff,           xoff+width-1,   yoff+10);
    dc.DrawLine(xoff,           yoff+height-10, xoff,           yoff+height);
    dc.DrawLine(xoff,           yoff+height-1,  xoff+10,        yoff+height-1);
    dc.DrawLine(xoff+width-1,   yoff+height-10, xoff+width-1,   yoff+height);
    dc.DrawLine(xoff+width-1,   yoff+height-1,  xoff+width-11,  yoff+height-1);
#endif
}

