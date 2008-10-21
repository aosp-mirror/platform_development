//
// Copyright 2005 The Android Open Source Project
//
// Phone button image holder.
//
#ifndef _SIM_PHONE_BUTTON_H
#define _SIM_PHONE_BUTTON_H

#include "LoadableImage.h"
#include <ui/KeycodeLabels.h>

/*
 * One button on a phone.  Position, size, and a highlight graphic.  The
 * coordinates are relative to the device graphic.
 *
 * We now have a "highlighted" graphic for mouse-overs and a "selected"
 * graphic for button presses.  We assume they have the same dimensions.
 * We currently assume that either both or neither exist, because we
 * generate one from the other.
 */
class PhoneButton {
public:
    PhoneButton(void)
        : mHasImage(false), mKeyCode(kKeyCodeUnknown)
        {}
    virtual ~PhoneButton(void) {}
    PhoneButton(const PhoneButton& src)
        : mHasImage(false), mKeyCode(kKeyCodeUnknown)
    {
        CopyMembers(src);
    }
    PhoneButton& operator=(const PhoneButton& src) {
        if (this != &src) {
            // Unload any resources in case we're using operator= to
            // assign to an existing object.
            mSelectedImage.UnloadResources();
            // Copy fields.
            CopyMembers(src);
        }
        return *this;
    }
    void CopyMembers(const PhoneButton& src) {
        mSelectedImage = src.mSelectedImage;
        mHighlightedBitmap = src.mHighlightedBitmap;
        mHasImage = src.mHasImage;
        mKeyCode = src.mKeyCode;
    }

    /* finish construction of PhoneButton, with or without an image */
    bool Create(const char* label);
    bool Create(const char* label, const char* imageFileName, int x, int y);

    int GetX(void) const { return mSelectedImage.GetX(); }
    int GetY(void) const { return mSelectedImage.GetY(); }
    int GetWidth(void) const { return mSelectedImage.GetWidth(); }
    int GetHeight(void) const { return mSelectedImage.GetHeight(); }
    wxBitmap* GetHighlightedBitmap(void) { return &mHighlightedBitmap; }
    wxBitmap* GetSelectedBitmap(void) const {
        return mSelectedImage.GetBitmap();
    }

    bool CheckCollision(int x, int y) const;
    KeyCode GetKeyCode(void) const { return mKeyCode; }

    // load or unload the image bitmap, if any
    bool LoadResources(void);
    bool UnloadResources(void);

private:
    void CreateHighlightedBitmap(void);
    KeyCode LookupKeyCode(const char* label) const;

    LoadableImage       mSelectedImage;
    wxBitmap            mHighlightedBitmap;
    bool                mHasImage;          // both exist or neither exist

    KeyCode    mKeyCode;
};

#endif // _SIM_PHONE_BUTTON_H
