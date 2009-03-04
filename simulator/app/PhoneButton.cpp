//
// Copyright 2005 The Android Open Source Project
//
// Simulated device data.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"   // needed for Windows build

#include "LinuxKeys.h"
#include "PhoneButton.h"

using namespace android;


/*
 * Create a PhoneButton without a backing image.
 */
bool PhoneButton::Create(const char* label)
{
    assert(!mHasImage);     // quick check for re-use

    mKeyCode = LookupKeyCode(label);
    if (mKeyCode == kKeyCodeUnknown) {
        fprintf(stderr, "WARNING: key code '%s' not recognized\n", label);
        // keep going
    }

    return true;
}

/*
 * Create a PhoneButton with an associated image.  Don't load the image yet.
 */
bool PhoneButton::Create(const char* label, const char* imageFileName,
    int x, int y)
{
    if (!Create(label))
        return false;

    if (mSelectedImage.Create(imageFileName, x, y))
        mHasImage = true;
    else
        fprintf(stderr, "Warning: image create (%s, %d, %d) failed\n",
            imageFileName, x, y);

    return true;
}

/*
 * Load the image, if any.
 */
bool PhoneButton::LoadResources(void)
{
    if (!mHasImage)
        return true;        // no image associated with this button

    bool result = mSelectedImage.LoadResources();
    if (result)
        CreateHighlightedBitmap();
    return result;
}

/*
 * Unload the image if we loaded one.
 */
bool PhoneButton::UnloadResources(void)
{
    if (!mHasImage)
        return true;

    return mSelectedImage.UnloadResources();
}

/* use an inline instead of macro so we don't evaluate args multiple times */
static inline int MinVal(int a, int b) { return (a < b ? a : b); }

/*
 * Create the "highlighted" bitmap from the "selected" image.
 */
void PhoneButton::CreateHighlightedBitmap(void)
{
    wxBitmap* src = mSelectedImage.GetBitmap();
    assert(src != NULL);
    wxImage tmpImage = src->ConvertToImage();

    unsigned char* pRGB = tmpImage.GetData();       // top-left RGBRGB...
    int x, y;

    /*
     * Modify the color used for the "highlight" image.
     */
    for (y = tmpImage.GetHeight()-1; y >= 0; --y) {
        for (x = tmpImage.GetWidth()-1; x >= 0; --x) {
            *(pRGB)   = MinVal(*(pRGB)   + *(pRGB) / 8, 255);
            *(pRGB+1) = MinVal(*(pRGB+1) + *(pRGB+1) / 8, 255);
            *(pRGB+2) = *(pRGB+2) * 5 / 8;

            pRGB += 3;
        }
    }

    mHighlightedBitmap = wxBitmap(tmpImage);
}

/*
 * Check to see if the button "collides" with the specified point.
 *
 * This is currently a simple rectangle check, but could be modified
 * to take image transparency into account.
 */
bool PhoneButton::CheckCollision(int x, int y) const
{
    if (!mHasImage)
        return false;

    return (x >= mSelectedImage.GetX() &&
            x < mSelectedImage.GetX() + mSelectedImage.GetWidth() &&
            y >= mSelectedImage.GetY() &&
            y < mSelectedImage.GetY() + mSelectedImage.GetHeight());
}

/*
 * Look up a key code based on a string.
 *
 * Returns kKeyCodeUnknown if the label doesn't match anything.
 */
KeyCode PhoneButton::LookupKeyCode(const char* label) const
{
    static const struct {
        const char* label;
        int keyCode;
    } codeList[] = {
        { "soft-left",      KEY_MENU },
        { "soft-right",     KEY_KBDILLUMUP },
        { "home",           KEY_HOME },
        { "back",           KEY_BACK },
        { "call",           KEY_F3 },
        { "phone-dial",     KEY_F3 },
        { "end-call",       KEY_F4 },
        { "phone-hangup",   KEY_F4 },
        { "0",              KEY_0 },
        { "1",              KEY_1 },
        { "2",              KEY_2 },
        { "3",              KEY_3 },
        { "4",              KEY_4 },
        { "5",              KEY_5 },
        { "6",              KEY_6 },
        { "7",              KEY_7 },
        { "8",              KEY_8 },
        { "9",              KEY_9 },
        { "star",           KEY_SWITCHVIDEOMODE },
        { "pound",          KEY_KBDILLUMTOGGLE },
        { "dpad-up",        KEY_UP },
        { "dpad-down",      KEY_DOWN },
        { "dpad-left",      KEY_LEFT },
        { "dpad-right",     KEY_RIGHT },
        { "dpad-center",    KEY_REPLY },
        { "volume-up",      KEY_VOLUMEUP },
        { "volume-down",    KEY_VOLUMEDOWN },
        { "power",          KEY_POWER },
        { "camera",         KEY_CAMERA },
        //{ "clear",          kKeyCodeClear },
    };
    const int numCodes = sizeof(codeList) / sizeof(codeList[0]);

    for (int i = 0; i < numCodes; i++) {
        if (strcmp(label, codeList[i].label) == 0)
            return (KeyCode) codeList[i].keyCode;
    }

    return kKeyCodeUnknown;
};

