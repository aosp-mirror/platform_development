//
// Copyright 2005 The Android Open Source Project
//
// Simulated device definition.
//
#ifndef _SIM_LOADABLE_IMAGE_H
#define _SIM_LOADABLE_IMAGE_H

#include "utils.h"

/*
 * Holds an image that may or may not be loaded at present.  The image
 * has an (x,y) offset.
 */
class LoadableImage {
public:
    LoadableImage(void)
        : mName(NULL), mpBitmap(NULL), mX(-1), mY(-1), mWidth(-1), mHeight(-1)
        {}
    virtual ~LoadableImage(void) {
        delete[] mName;
        delete mpBitmap;
    }
    LoadableImage(const LoadableImage& src)
        : mName(NULL), mpBitmap(NULL)
    {
        CopyMembers(src);
    }
    LoadableImage& operator=(const LoadableImage& src) {
        if (this != &src)       // self-assignment
            CopyMembers(src);
        return *this;
    }
    void CopyMembers(const LoadableImage& src) {
        // Need to delete resources in case we're using operator= and
        // assigning into an object that already holds some.
        delete mName;
        delete mpBitmap;
        mName = android::strdupNew(src.mName);
        if (src.mpBitmap == NULL)
            mpBitmap = NULL;
        else
            mpBitmap = new wxBitmap(*(src.mpBitmap));
        mX = src.mX;
        mY = src.mY;
        mWidth = src.mWidth;
        mHeight = src.mHeight;
    }

    virtual bool Create(const char* fileName, int x, int y);

    // load or unload the bitmap
    bool LoadResources(void);
    bool UnloadResources(void);

    // accessors
    int GetX(void) const { return mX; }
    int GetY(void) const { return mY; }
    int GetWidth(void) const { return mWidth; }
    int GetHeight(void) const { return mHeight; }
    wxBitmap* GetBitmap(void) const { return mpBitmap; }

private:
    char*       mName;
    wxBitmap*   mpBitmap;

    int         mX;         // position relative to phone image
    int         mY;
    int         mWidth;     // from image (cached values)
    int         mHeight;
};

#endif // _SIM_LOADABLE_IMAGE_H
