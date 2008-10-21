//
// Copyright 2005 The Android Open Source Project
//
// Displays the phone image and handles user input.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"   // needed for Windows build
#include "wx/dcbuffer.h"

#include "LinuxKeys.h"
#include "PhoneWindow.h"
#include "DeviceWindow.h"
#include "PhoneData.h"
#include "PhoneCollection.h"
#include "MainFrame.h"
#include "MyApp.h"

using namespace android;

BEGIN_EVENT_TABLE(PhoneWindow, wxWindow)    // NOT wxDialog
    EVT_ACTIVATE(PhoneWindow::OnActivate)
    //EVT_ACTIVATE_APP(PhoneWindow::OnActivate)
    EVT_CLOSE(PhoneWindow::OnClose)
    EVT_MOVE(PhoneWindow::OnMove)
    EVT_ERASE_BACKGROUND(PhoneWindow::OnErase)
    EVT_PAINT(PhoneWindow::OnPaint)

    EVT_KEY_DOWN(PhoneWindow::OnKeyDown)
    EVT_KEY_UP(PhoneWindow::OnKeyUp)
    EVT_LEFT_DOWN(PhoneWindow::OnMouseLeftDown)
    EVT_LEFT_DCLICK(PhoneWindow::OnMouseLeftDown)
    EVT_LEFT_UP(PhoneWindow::OnMouseLeftUp)
    EVT_RIGHT_DOWN(PhoneWindow::OnMouseRightDown)
    EVT_RIGHT_DCLICK(PhoneWindow::OnMouseRightDown)
    EVT_RIGHT_UP(PhoneWindow::OnMouseRightUp)
    EVT_MOTION(PhoneWindow::OnMouseMotion)
    EVT_LEAVE_WINDOW(PhoneWindow::OnMouseLeaveWindow)
    EVT_TIMER(kVibrateTimerId, PhoneWindow::OnTimer)
END_EVENT_TABLE()


/*
 * Create a new PhoneWindow.  This should be a child of the main frame.
 */
PhoneWindow::PhoneWindow(wxWindow* parent, const wxPoint& posn)
    : wxDialog(parent, wxID_ANY, wxT("Device"), posn, wxDefaultSize,
        wxDEFAULT_DIALOG_STYLE),
      mpMOHViewIndex(-1),
      mpMOHButton(NULL),
      mMouseKeySent(kKeyCodeUnknown),
      mpViewInfo(NULL),
      mNumViewInfo(0),
      mpDeviceWindow(NULL),
      mNumDeviceWindows(0),
      mPhoneModel(-1),
      mCurrentMode(wxT("(unknown)")),
      mPlacementChecked(false),
      mpParent((MainFrame*)parent),
      mTimer(this, kVibrateTimerId),
      mTrackingTouch(false)
{
    SetBackgroundColour(*wxLIGHT_GREY);
    SetBackgroundStyle(wxBG_STYLE_CUSTOM);

    //SetCursor(wxCursor(wxCURSOR_HAND));     // a bit distracting (pg.276)
}

/*
 * Destroy everything we own.
 *
 * This might be called well after we've been closed and another
 * PhoneWindow has been created, because wxWidgets likes to defer things.
 */
PhoneWindow::~PhoneWindow(void)
{
    //printf("--- ~PhoneWindow %p\n", this);
    delete[] mpViewInfo;
    if (mpDeviceWindow != NULL) {
        for (int i = 0; i < mNumDeviceWindows; i++) {
            /* make sure they don't try to use our member */
            mpDeviceWindow[i]->DeviceManagerClosing();
            /* make sure the child window gets destroyed -- not necessary? */
            mpDeviceWindow[i]->Destroy();
        }

        /* delete our array of pointers */
        delete[] mpDeviceWindow;
    }
}

/*
 * Check for an updated runtime when window becomes active
 */
void PhoneWindow::OnActivate(wxActivateEvent& event)
{
    /*
     * DO NOT do this.  Under Windows, it causes the parent window to get
     * an activate event, which causes our parent to get the focus.  With
     * this bit of code active it is impossible for the phone window to
     * receive user input.
     */
    //GetParent()->AddPendingEvent(event);

    // If we are being deactivated, go ahead and send key up events so that the
    // runtime doesn't think we are holding down the key. Issue #685750
    if (!event.GetActive()) {
        ListIter iter;
        for (iter = mPressedKeys.begin(); iter != mPressedKeys.end(); ) {
            KeyCode keyCode = (*iter).GetKeyCode();
            GetDeviceManager()->SendKeyEvent(keyCode, false);
            iter = mPressedKeys.erase(iter);
        }
    }
}

/*
 * Close the phone window.
 */
void PhoneWindow::OnClose(wxCloseEvent& event)
{
    //printf("--- PhoneWindow::OnClose %p\n", this);
#if 0
    if (mDeviceManager.IsRunning() && !mDeviceManager.IsKillable()) {
        printf("Sim: refusing to close window on external runtime\n");
        event.Veto();
        return;
    }
#endif

    wxRect rect = GetRect();
    printf("Sim: Closing phone window (posn=(%d,%d))\n", rect.x, rect.y);

    /* notify others */
    mpParent->PhoneWindowClosing(rect.x, rect.y);
    mDeviceManager.WindowsClosing();

    /* end it all */
    Destroy();
}

/*
 * Prep the PhoneWindow to display a specific phone model.  Pass in the
 * model index.
 *
 * This gets called whenever the display changes.  This could be a new
 * device with identical characteristics, or a different mode for the same
 * device.
 *
 * The window can be re-used so long as the display characteristics are
 * the same.  If the display characteristics are different, we have to
 * restart the device.
 */
bool PhoneWindow::Setup(int phoneIdx)
{
    wxString fileName;
    PhoneCollection* pCollection = PhoneCollection::GetInstance();

    if (phoneIdx < 0 || phoneIdx >= pCollection->GetPhoneCount()) {
        fprintf(stderr, "Bogus phone index %d\n", phoneIdx);
        return false;
    }

    /*
     * Clear these out so that failure here is noticeable to caller.  We
     * regenerate the ViewInfo array every time, because the set of Views
     * is different for every Mode.
     */
    delete[] mpViewInfo;
    mpViewInfo = NULL;
    mNumViewInfo = -1;

    PhoneData* pPhoneData;
    PhoneMode* pPhoneMode;
    PhoneView* pPhoneView;

    pPhoneData = pCollection->GetPhoneData(phoneIdx);

    pPhoneMode = pPhoneData->GetPhoneMode(GetCurrentMode().ToAscii());
    if (pPhoneMode == NULL) {
        fprintf(stderr, "current mode (%s) not known\n",
            (const char*) GetCurrentMode().ToAscii());
        return false;
    }

    int numViews = pPhoneMode->GetNumViews();
    if (numViews == 0) {
        fprintf(stderr, "Phone %d mode %s has no views\n",
            phoneIdx, pPhoneMode->GetName());
        return false;
    }

    const int kBorder = 2;
    int i;
    int maxHeight = 0;
    int fullWidth = kBorder;
    ViewInfo* pViewInfo;

    pViewInfo = new ViewInfo[numViews];

    /* figure out individual and overall dimensions */
    for (i = 0; i < numViews; i++) {
        pPhoneView = pPhoneMode->GetPhoneView(i);
        if (pPhoneView == NULL) {
            fprintf(stderr, "view %d not found\n", i);
            return false;
        }

        if (!GetDimensions(pPhoneData, pPhoneView, &pViewInfo[i]))
            return false;

        if (maxHeight < pViewInfo[i].GetHeight())
            maxHeight = pViewInfo[i].GetHeight();
        fullWidth += pViewInfo[i].GetWidth() + kBorder;
    }

    /* create the device windows if we don't already have them */
    if (mpDeviceWindow == NULL) {
        mNumDeviceWindows = pPhoneData->GetNumDisplays();
        mpDeviceWindow = new DeviceWindow*[mNumDeviceWindows];
        if (mpDeviceWindow == NULL)
            return false;

        for (i = 0; i < mNumDeviceWindows; i++) {
            mpDeviceWindow[i] = new DeviceWindow(this, &mDeviceManager);
        }
    } else {
        assert(pPhoneData->GetNumDisplays() == mNumDeviceWindows);
    }

    /*
     * Position device windows within their views, taking into account
     * border areas.
     */
    int shift = kBorder;
    for (i = 0; i < numViews; i++) {
        int displayIdx;
        PhoneDisplay* pPhoneDisplay;

        displayIdx = pViewInfo[i].GetDisplayIndex();
        pPhoneDisplay = pPhoneData->GetPhoneDisplay(displayIdx);
        //printf("View %d: display %d\n", i, displayIdx);

        pViewInfo[i].SetX(shift);
        pViewInfo[i].SetY(kBorder);

        mpDeviceWindow[displayIdx]->SetSize(
            pViewInfo[i].GetX() + pViewInfo[i].GetDisplayX(),
            pViewInfo[i].GetY() + pViewInfo[i].GetDisplayY(),
            pPhoneDisplay->GetWidth(), pPhoneDisplay->GetHeight());

        // incr by width of view
        shift += pViewInfo[i].GetWidth() + kBorder;
    }

    /* configure the device manager if it's not already running */
    if (!mDeviceManager.IsInitialized()) {
        mDeviceManager.Init(pPhoneData->GetNumDisplays(), mpParent);

        for (i = 0; i < pPhoneData->GetNumDisplays(); i++) {
            PhoneDisplay* pPhoneDisplay;
            bool res;

            pPhoneDisplay = pPhoneData->GetPhoneDisplay(i);

            res = mDeviceManager.SetDisplayConfig(i, mpDeviceWindow[i],
                pPhoneDisplay->GetWidth(), pPhoneDisplay->GetHeight(),
                pPhoneDisplay->GetFormat(), pPhoneDisplay->GetRefresh());
            if (!res) {
                fprintf(stderr, "Sim: ERROR: could not configure device mgr\n");
                return false;
            }
        }
        const char *kmap = pPhoneData->GetPhoneKeyboard(0)->getKeyMap();
        mDeviceManager.SetKeyboardConfig(kmap);
    } else {
        assert(pPhoneData->GetNumDisplays() == mDeviceManager.GetNumDisplays());
    }

    /*
     * Success.  Finish up.
     */
    mPhoneModel = phoneIdx;
    mpViewInfo = pViewInfo;
    mNumViewInfo = numViews;

    /* set up our window */
    SetClientSize(fullWidth, maxHeight + kBorder * 2);
    SetBackgroundColour(*wxLIGHT_GREY);
    //SetBackgroundColour(*wxBLUE);
    SetTitle(wxString::FromAscii(pPhoneData->GetTitle()));

    SetFocus();     // set keyboard input focus

    return true;
}

/*
 * The device table has been reloaded.  We need to throw out any pointers
 * we had into it and possibly reload some stuff.
 */
void PhoneWindow::DevicesRescanned(void)
{
    mpMOHButton = NULL;
    mpMOHViewIndex = -1;

    /*
     * Re-evaluate phone definition.  There is an implicit assumption
     * that the re-scanned version is compatible with the previous
     * version (i.e. it still exists and has the same screen size).
     *
     * We're also currently assuming that no phone definitions have been
     * added or removed, which is bad -- we should get the new index for
     * for phone by searching for it by name.
     *
     * TODO: don't make these assumptions.
     */
    Setup(mPhoneModel);
}

/*
 * Check the initial placement of the window.  We get one of these messages
 * when the window is first placed, and every time it's moved thereafter.
 *
 * Right now we're just trying to make sure wxWidgets doesn't shove it off
 * the top of the screen under Linux.  Might want to change this to
 * remember the previous placement and put the window back.
 */
void PhoneWindow::OnMove(wxMoveEvent& event)
{
    if (mPlacementChecked)
        return;

    wxPoint point;
    point = event.GetPosition();
    if (point.y < 0) {
        printf("Sim: window is at (%d,%d), adjusting\n", point.x, point.y);
        point.y = 0;
        Move(point);
    }

    mPlacementChecked = true;
}

/*
 * Figure out the dimensions required to contain the specified view.
 *
 * This is usually the size of the background image, but if we can't
 * load it or it's too small just create a trivial window.
 */
bool PhoneWindow::GetDimensions(PhoneData* pPhoneData, PhoneView* pPhoneView,
    ViewInfo* pInfo)
{
    PhoneDisplay* pPhoneDisplay;
    int xoff=0, yoff=0, width, height;
    int displayIdx;

    displayIdx = pPhoneData->GetPhoneDisplayIndex(pPhoneView->GetDisplayName());
    if (displayIdx < 0)
        return false;

    pPhoneDisplay = pPhoneData->GetPhoneDisplay(displayIdx);
    if (pPhoneDisplay == NULL) {
        fprintf(stderr, "display '%s' not found in device '%s'\n",
            pPhoneView->GetDisplayName(), pPhoneData->GetName());
        return false;
    }

    // load images for this phone
    (void) pPhoneView->LoadResources();

    width = height = 0;

    // by convention, the background bitmap is the first image in the list
    if (pPhoneView->GetBkgImageCount() > 0) {
        wxBitmap* pBitmap = pPhoneView->GetBkgImage(0)->GetBitmap();
        if (pBitmap != NULL) {
            // size window to match bitmap
            xoff = pPhoneView->GetXOffset();
            yoff = pPhoneView->GetYOffset();
            width = pBitmap->GetWidth();
            height = pBitmap->GetHeight();
        }
    }

    // no bitmap, or bitmap is smaller than display
    if (width < pPhoneDisplay->GetWidth() ||
        height < pPhoneDisplay->GetHeight())
    {
        // create window to just hold display
        xoff = yoff = 0;
        width = pPhoneDisplay->GetWidth();
        height = pPhoneDisplay->GetHeight();
    }
    if (width <= 0 || height <= 0) {
        fprintf(stderr, "ERROR: couldn't determine display size\n");
        return false;
    }

    pInfo->SetX(0);
    pInfo->SetY(0);             // another function determines these
    pInfo->SetDisplayX(xoff);
    pInfo->SetDisplayY(yoff);
    pInfo->SetWidth(width);
    pInfo->SetHeight(height);
    pInfo->SetDisplayIndex(displayIdx);

    //printf("xoff=%d yoff=%d width=%d height=%d index=%d\n",
    //    pInfo->GetDisplayX(), pInfo->GetDisplayY(),
    //    pInfo->GetWidth(), pInfo->GetHeight(), pInfo->GetDisplayIndex());

    return true;
}

/*
 * Return PhoneData pointer for the current phone model.
 */
PhoneData* PhoneWindow::GetPhoneData(void) const
{
    PhoneCollection* pCollection = PhoneCollection::GetInstance();
    return pCollection->GetPhoneData(mPhoneModel);
}

/*
 * Convert a wxWidgets key code into a device key code.
 *
 * Someday we may want to make this configurable.
 *
 * NOTE: we need to create a mapping between simulator key and desired
 * function.  The "return" key should always mean "select", whether
 * it's a "select" button or pressing in on the d-pad.  Ditto for
 * the arrow keys, whether we have a joystick, d-pad, or four buttons.
 * Each key here should have a set of things that it could possibly be,
 * and we match it up with the set of buttons actually defined for the
 * phone.  [for convenience, need to ensure that buttons need not have
 * an associated image]
 */
int PhoneWindow::ConvertKeyCode(int wxKeyCode) const
{
    switch (wxKeyCode) {
    case WXK_NUMPAD_INSERT:
    case WXK_NUMPAD0:
    case '0':                   return KEY_0;
    case WXK_NUMPAD_HOME:
    case WXK_NUMPAD1:
    case '1':                   return KEY_1;
    case WXK_NUMPAD_UP:
    case WXK_NUMPAD2:
    case '2':                   return KEY_2;
    case WXK_NUMPAD_PRIOR:
    case WXK_NUMPAD3:
    case '3':                   return KEY_3;
    case WXK_NUMPAD_LEFT:
    case WXK_NUMPAD4:
    case '4':                   return KEY_4;
    case WXK_NUMPAD_BEGIN:
    case WXK_NUMPAD5:
    case '5':                   return KEY_5;
    case WXK_NUMPAD_RIGHT:
    case WXK_NUMPAD6:
    case '6':                   return KEY_6;
    case WXK_NUMPAD_END:
    case WXK_NUMPAD7:
    case '7':                   return KEY_7;
    case WXK_NUMPAD_DOWN:
    case WXK_NUMPAD8:
    case '8':                   return KEY_8;
    case WXK_NUMPAD_NEXT:
    case WXK_NUMPAD9:
    case '9':                   return KEY_9;
    case WXK_NUMPAD_MULTIPLY:   return KEY_SWITCHVIDEOMODE; //kKeyCodeStar;
    case WXK_LEFT:              return KEY_LEFT;
    case WXK_RIGHT:             return KEY_RIGHT;
    case WXK_UP:                return KEY_UP;
    case WXK_DOWN:              return KEY_DOWN;
    case WXK_NUMPAD_ENTER:      return KEY_REPLY; //kKeyCodeDpadCenter;
    case WXK_HOME:              return KEY_HOME;
    case WXK_PRIOR:
    case WXK_PAGEUP:            return KEY_MENU; //kKeyCodeSoftLeft;
    case WXK_NEXT:
    case WXK_PAGEDOWN:          return KEY_KBDILLUMUP; //kKeyCodeSoftRight;
    case WXK_DELETE:            
    case WXK_BACK:              return KEY_BACKSPACE; //kKeyCodeDel;
    case WXK_ESCAPE:
    case WXK_END:               return KEY_BACK; //kKeyCodeBack;
    case WXK_NUMPAD_DELETE:
    case WXK_NUMPAD_DECIMAL:    return KEY_KBDILLUMTOGGLE; //kKeyCodePound;
    case WXK_SPACE:             return KEY_SPACE; //kKeyCodeSpace;
    case WXK_RETURN:            return KEY_ENTER; //kKeyCodeNewline;
    case WXK_F3:                return KEY_F3; //kKeyCodeCall;
    case WXK_F4:                return KEY_F4; //kKeyCodeEndCall;
    case WXK_NUMPAD_ADD:
    case WXK_F5:                return KEY_VOLUMEUP;
    case WXK_NUMPAD_SUBTRACT:
    case WXK_F6:                return KEY_VOLUMEDOWN;
    case WXK_F7:                return KEY_POWER;
    case WXK_F8:                return KEY_CAMERA;
    case 'A':                   return KEY_A;
    case 'B':                   return KEY_B;
    case 'C':                   return KEY_C;
    case 'D':                   return KEY_D;
    case 'E':                   return KEY_E;
    case 'F':                   return KEY_F;
    case 'G':                   return KEY_G;
    case 'H':                   return KEY_H;
    case 'I':                   return KEY_I;
    case 'J':                   return KEY_J;
    case 'K':                   return KEY_K;
    case 'L':                   return KEY_L;
    case 'M':                   return KEY_M;
    case 'N':                   return KEY_N;
    case 'O':                   return KEY_O;
    case 'P':                   return KEY_P;
    case 'Q':                   return KEY_Q;
    case 'R':                   return KEY_R;
    case 'S':                   return KEY_S;
    case 'T':                   return KEY_T;
    case 'U':                   return KEY_U;
    case 'V':                   return KEY_V;
    case 'W':                   return KEY_W;
    case 'X':                   return KEY_X;
    case 'Y':                   return KEY_Y;
    case 'Z':                   return KEY_Z;
    case ',':                   return KEY_COMMA;
    case '.':                   return KEY_DOT;
    case '<':                   return KEY_COMMA;
    case '>':                   return KEY_DOT;
    case '`':                   return KEY_GREEN; /*KEY_GRAVE;*/
    case '-':                   return KEY_MINUS;
    case '=':                   return KEY_EQUAL;
    case '[':                   return KEY_LEFTBRACE;
    case ']':                   return KEY_RIGHTBRACE;
    case '\\':                  return KEY_BACKSLASH;
    case ';':                   return KEY_SEMICOLON;
    case '\'':                  return KEY_APOSTROPHE;
    case '/':                   return KEY_SLASH;
    case WXK_SHIFT:             return KEY_LEFTSHIFT;
    case WXK_CONTROL:
    case WXK_ALT:               return KEY_LEFTALT;
    case WXK_TAB:               return KEY_TAB;
    // don't show "ignoring key" message for these
    case WXK_MENU:
        break;
    default:
        printf("(ignoring key %d)\n", wxKeyCode);
        break;
    }

    return kKeyCodeUnknown;
}


/*
 * Keyboard handling.  These get converted into Android-defined key
 * constants here.
 *
 * NOTE: would be nice to handle menu keyboard accelerators here.
 * Simply stuffing the key events into MainFrame with AddPendingEvent
 * didn't seem to do the trick.
 */
void PhoneWindow::OnKeyDown(wxKeyEvent& event)
{
    KeyCode keyCode;

    keyCode = (KeyCode) ConvertKeyCode(event.GetKeyCode());
    if (keyCode != kKeyCodeUnknown) {
        if (!IsKeyPressed(keyCode)) {
            //printf("PW: down: key %d\n", keyCode);
            GetDeviceManager()->SendKeyEvent(keyCode, true);
            AddPressedKey(keyCode);
        }
    } else {
        //printf("PW: down: %d\n", event.GetKeyCode());
        event.Skip();       // not handled by us
    }
}

/*
 * Pass key-up events to runtime.
 */
void PhoneWindow::OnKeyUp(wxKeyEvent& event)
{
    KeyCode keyCode;

    keyCode = (KeyCode) ConvertKeyCode(event.GetKeyCode());
    if (keyCode != kKeyCodeUnknown) {
        // Send the key event if we already have this key pressed.
        if (IsKeyPressed(keyCode)) {
            //printf("PW:   up: key %d\n", keyCode);
            GetDeviceManager()->SendKeyEvent(keyCode, false);
            RemovePressedKey(keyCode);
        }
    } else {
        //printf("PW:   up: %d\n", event.GetKeyCode());
        event.Skip();       // not handled by us
    }
}

/*
 * Mouse handling.
 *
 * Unlike more conventional button tracking, we highlight on mouse-over
 * and send the key on mouse-down.  This behavior may be confusing for
 * people expecting standard behavior, but it allows us to simulate the
 * effect of holding a key down.
 *
 * We want to catch both "down" and "double click" events; otherwise
 * fast clicking results in a lot of discarded events.
 */
void PhoneWindow::OnMouseLeftDown(wxMouseEvent& event)
{
    if (mpMOHButton != NULL) {
        //printf("PW: left down\n");
        KeyCode keyCode = mpMOHButton->GetKeyCode();
        GetDeviceManager()->SendKeyEvent(keyCode, true);
        mMouseKeySent = keyCode;
        AddPressedKey(keyCode);
    } else {
        int screenX, screenY;

        if (GetTouchPosition(event, &screenX, &screenY)) {
            //printf("TOUCH at %d,%d\n", screenX, screenY);
            mTrackingTouch = true;
            mTouchX = screenX;
            mTouchY = screenY;
            GetDeviceManager()->SendTouchEvent(Simulator::kTouchDown,
                mTouchX, mTouchY);
        } else {
            //printf("(ignoring left click)\n");
        }
    }
}

/*
 * Left button has been released.  Do something clever.
 *
 * On some platforms we will lose this if the mouse leaves the window.
 */
void PhoneWindow::OnMouseLeftUp(wxMouseEvent& WXUNUSED(event))
{
    if (mMouseKeySent != kKeyCodeUnknown) {
        //printf("PW: left up\n");
        GetDeviceManager()->SendKeyEvent(mMouseKeySent, false);
        RemovePressedKey(mMouseKeySent);
    } else {
        if (mTrackingTouch) {
            //printf("TOUCH release (last was %d,%d)\n", mTouchX, mTouchY);
            mTrackingTouch = false;
            GetDeviceManager()->SendTouchEvent(Simulator::kTouchUp,
                mTouchX, mTouchY);
        } else {
            //printf("(ignoring left-up)\n");
        }
    }
    mMouseKeySent = kKeyCodeUnknown;
}

void PhoneWindow::OnMouseRightDown(wxMouseEvent& event)
{
    //printf("(ignoring right-down)\n");
}
void PhoneWindow::OnMouseRightUp(wxMouseEvent& event)
{
    //printf("(ignoring right-up)\n");
}

/*
 * Track mouse motion so we can do mouse-over button highlighting.
 */
void PhoneWindow::OnMouseMotion(wxMouseEvent& event)
{
    /*
     * If the mouse motion event occurred inside the device window,
     * we treat it differently than mouse movement over the picture of
     * the device.
     */
    if (event.GetEventObject() == mpDeviceWindow[0]) {
        if (mpMOHViewIndex >= 0) {
            /* can happen if the mouse moves fast enough */
            //printf("Mouse now in dev window, clearing button highlight\n");
            mpMOHViewIndex = -1;
            mpMOHButton = NULL;
            Refresh();
        }

        if (!event.LeftIsDown() && event.RightIsDown()) {
            /* right-button movement */
            //printf("(ignoring right-drag)\n");
            return;
        }

        //printf("moveto: %d,%d\n", event.m_x, event.m_y);

        int screenX, screenY;
        if (mTrackingTouch) {
            if (GetTouchPosition(event, &screenX, &screenY)) {
                //printf("TOUCH moved to %d,%d\n", screenX, screenY);
                mTouchX = screenX;
                mTouchY = screenY;
                GetDeviceManager()->SendTouchEvent(Simulator::kTouchDrag,
                    mTouchX, mTouchY);
            } else {
                //printf("TOUCH moved off screen\n");
            }
        }

        return;
    }

    PhoneData* pPhoneData = GetPhoneData();
    if (pPhoneData == NULL)
        return;

    /*
     * Check to see if we're on top of a button.  If our "on top of
     * something" state has changed, force a redraw.
     *
     * We have to run through the list of Views and check all of the
     * buttons in each.
     */
    PhoneMode* pMode = pPhoneData->GetPhoneMode(GetCurrentMode().ToAscii());
    if (pMode == NULL)
        return;

    int viewIndex = -1;
    PhoneButton* pHighlight = NULL;
    int i;

    for (i = pMode->GetNumViews()-1; i >= 0; i--) {
        PhoneView* pView = pMode->GetPhoneView(i);
        assert(pView != NULL);

        /* convert from window-relative to view-relative */
        pHighlight = pView->FindButtonHit(event.m_x - mpViewInfo[i].GetX(),
                                          event.m_y - mpViewInfo[i].GetY());
        if (pHighlight != NULL) {
            viewIndex = i;
            break;
        }
    }

    if (viewIndex == mpMOHViewIndex && pHighlight == mpMOHButton) {
        /* still hovering over same button */
    } else {
        /* mouse has moved, possibly to a new button */

        mpMOHViewIndex = viewIndex;
        mpMOHButton = pHighlight;

        /* force refresh */
        Refresh();
    }
}

/*
 * Mouse has left the building.  All keys and mouse buttons up.
 *
 * We get one of these if the mouse moves over a child window, such as
 * our DeviceWindow, so it is not the case that we no longer receive
 * key input after getting this event.
 */
void PhoneWindow::OnMouseLeaveWindow(wxMouseEvent& WXUNUSED(event))
{
    //printf("--- mouse is GONE\n");
    ClearPressedKeys();
}

/*
 * Determine device touch screen (x,y) based on window position.
 *
 * Returns "true" if the click corresponds to a location on the display.
 *
 * TODO: should return display index as well -- currently this only
 * supports touch on the main display.
 */
bool PhoneWindow::GetTouchPosition(const wxMouseEvent& event, int* pScreenX,
    int* pScreenY)
{
    /*
     * If the click came from our device window, treat it as a touch.
     */
    if (event.GetEventObject() != mpDeviceWindow[0])
        return false;

    *pScreenX = event.m_x;
    *pScreenY = event.m_y;
    return true;
}

/*
 * We don't want to erase the background now, because it causes flicker
 * under Windows.
 */
void PhoneWindow::OnErase(wxEraseEvent& WXUNUSED(event))
{
    //printf("erase\n");
}

/*
 * Paint the phone and any highlighted buttons.
 *
 * The device output is drawn by DeviceWindow.
 */
void PhoneWindow::OnPaint(wxPaintEvent& WXUNUSED(event))
{
    int view;

    /*
     * Under Mac OS X, the parent window is redrawn every time the child
     * window is redrawn.  This causes poor performance in the simulator.
     * If we're being asked to update a region that corresponds exactly
     * to one of the device output windows, skip the redraw.
     */
    assert(mpViewInfo != NULL);
    for (view = 0; view < mNumViewInfo; view++) {
        int displayIndex;

        displayIndex = mpViewInfo[view].GetDisplayIndex();
        assert(displayIndex >= 0);
        DeviceWindow* pDeviceWindow = mpDeviceWindow[displayIndex];
        assert(pDeviceWindow != NULL);

        wxRect displayRect = pDeviceWindow->GetRect();
        wxRect updateRect = GetUpdateClientRect();

        if (displayRect == updateRect) {
            //printf("(skipping redraw)\n");
            return;
        }
    }

    wxBufferedPaintDC dc(this);

    /*
     * Erase the background to the currently-specified background color.
     */
    wxColour backColor = GetBackgroundColour();
    dc.SetBrush(wxBrush(backColor));
    dc.SetPen(wxPen(backColor, 1));
    wxRect windowRect(wxPoint(0, 0), GetClientSize());
    dc.DrawRectangle(windowRect);

    PhoneData* pPhoneData = GetPhoneData();
    if (pPhoneData == NULL) {
        fprintf(stderr, "OnPaint: no phone data\n");
        return;
    }

    PhoneMode* pPhoneMode;
    PhoneView* pPhoneView;
    int numImages;

    pPhoneMode = pPhoneData->GetPhoneMode(GetCurrentMode().ToAscii());
    if (pPhoneMode == NULL) {
        fprintf(stderr, "current mode (%s) not known\n",
            (const char*) GetCurrentMode().ToAscii());
        return;
    }

    for (view = 0; view < pPhoneMode->GetNumViews(); view++) {
        pPhoneView = pPhoneMode->GetPhoneView(view);
        if (pPhoneView == NULL) {
            fprintf(stderr, "view %d not found\n", view);
            return;
        }

        /* draw background image and "button patches" */
        numImages = pPhoneView->GetBkgImageCount();
        for (int i = 0; i < numImages; i++) {
            const LoadableImage* pLimg = pPhoneView->GetBkgImage(i);
            wxBitmap* pBitmap = pLimg->GetBitmap();
            if (pBitmap != NULL)
                dc.DrawBitmap(*pBitmap,
                    mpViewInfo[view].GetX() + pLimg->GetX(),
                    mpViewInfo[view].GetY() + pLimg->GetY(),
                    TRUE);
        }
    }


    /*
     * Draw button mouse-over highlight.
     *
     * Currently we don't do anything different when the button is held down.
     */
    if (mpMOHViewIndex >= 0 && mpMOHButton != NULL) {
        // button must have graphic, or hit-testing wouldn't have worked
        assert(mpMOHButton->GetHighlightedBitmap() != NULL);
        dc.DrawBitmap(*mpMOHButton->GetHighlightedBitmap(),
            mpViewInfo[mpMOHViewIndex].GetX() + mpMOHButton->GetX(),
            mpViewInfo[mpMOHViewIndex].GetY() + mpMOHButton->GetY(),
            TRUE);
    }

    /*
     * Highlight pressed keys.  We want to do this in all views, because
     * some buttons on the side of the phone might be visible in more
     * than one view.
     */
    for (view = 0; view < pPhoneMode->GetNumViews(); view++) {
        pPhoneView = pPhoneMode->GetPhoneView(view);
        assert(pPhoneView != NULL);

        ListIter iter;
        for (iter = mPressedKeys.begin(); iter != mPressedKeys.end(); ++iter) {
            KeyCode keyCode;
            PhoneButton* pButton;

            keyCode = (*iter).GetKeyCode();
            pButton = pPhoneView->FindButtonByKey(keyCode);
            if (pButton != NULL) {
                wxBitmap* pBitmap = pButton->GetSelectedBitmap();
                if (pBitmap != NULL) {
                    dc.DrawBitmap(*pBitmap,
                        mpViewInfo[view].GetX() + pButton->GetX(),
                        mpViewInfo[view].GetY() + pButton->GetY(),
                        TRUE);
                }
            }
        }
    }
}


/*
 * Press a key on the device.
 *
 * Schedules a screen refresh if the set of held-down keys changes.
 */
void PhoneWindow::AddPressedKey(KeyCode keyCode)
{
    /*
     * See if the key is already down.  This usually means that the key
     * repeat has kicked into gear.  It could also mean that we
     * missed the key-up event, or the user has hit the same device
     * key with both mouse and keyboard.  Either way, we don't add it
     * a second time.  This way, if we did lose a key-up somehow, they
     * can "clear" the stuck key by hitting it again.
     */
    if (keyCode == kKeyCodeUnknown) {
        //printf("--- not adding kKeyCodeUnknown!\n");
        return;
    }

    ListIter iter;
    for (iter = mPressedKeys.begin(); iter != mPressedKeys.end(); ++iter) {
        if ((*iter).GetKeyCode() == keyCode)
            break;
    }
    if (iter == mPressedKeys.end()) {
        KeyInfo newInfo;
        newInfo.SetKeyCode(keyCode);
        mPressedKeys.push_back(newInfo);
        //printf("---  added down=%d\n", keyCode);
        Refresh();      // redraw w/ highlight
    } else {
        //printf("---  already have down=%d\n", keyCode);
    }
}

/*
 * Release a key on the device.
 *
 * Schedules a screen refresh if the set of held-down keys changes.
 */
void PhoneWindow::RemovePressedKey(KeyCode keyCode)
{
    /*
     * Release the key.  If it's not in the list, we either missed a
     * key-down event, or the user used both mouse and keyboard and we
     * removed the key when the first device went up.
     */
    ListIter iter;
    for (iter = mPressedKeys.begin(); iter != mPressedKeys.end(); ++iter) {
        if ((*iter).GetKeyCode() == keyCode) {
            mPressedKeys.erase(iter);
            //printf("---  removing down=%d\n", keyCode);
            Refresh();      // redraw w/o highlight
            break;
        }
    }
    if (iter == mPressedKeys.end()) {
        //printf("---  didn't find down=%d\n", keyCode);
    }
}

/*
 * Clear the set of keys that we think are being held down.
 */
void PhoneWindow::ClearPressedKeys(void)
{
    //printf("--- All keys up (count=%d)\n", mPressedKeys.size());

    if (!mPressedKeys.empty()) {
        ListIter iter = mPressedKeys.begin();
        while (iter != mPressedKeys.end()) {
            KeyCode keyCode = (*iter).GetKeyCode();
            GetDeviceManager()->SendKeyEvent(keyCode, false);
            iter = mPressedKeys.erase(iter);
        }
        Refresh();
    }
}

/*
 * Returns "true" if the specified key is currently pressed.
 */
bool PhoneWindow::IsKeyPressed(KeyCode keyCode)
{
    ListIter iter;
    for (iter = mPressedKeys.begin(); iter != mPressedKeys.end(); ++iter) {
        if ((*iter).GetKeyCode() == keyCode)
            return true;
    }
    return false;
}

void PhoneWindow::Vibrate(int vibrateOn)
{
    wxRect rect = GetRect();
    if(vibrateOn)
    {
        mVibrateX = 0;
        mTimer.Start(25);      // arg is delay in ms
        Move(rect.x-2,rect.y);
    }
    else if(mTimer.IsRunning())
    {
        mTimer.Stop();
        if(mVibrateX&1)
            Move(rect.x-2,rect.y);
        else
            Move(rect.x+2,rect.y);
    }
}

void PhoneWindow::OnTimer(wxTimerEvent& event)
{
    wxRect rect = GetRect();
    mVibrateX++;
    if(mVibrateX&1)
        Move(rect.x+4,rect.y);
    else
        Move(rect.x-4,rect.y);
}
