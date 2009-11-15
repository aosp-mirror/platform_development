//
// Copyright 2005 The Android Open Source Project
//
// Simple class to hold an image that can be loaded and unloaded.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"   // needed for Windows build

#include "LoadableImage.h"
#include "AssetStream.h"
#include "MyApp.h"

#include "utils.h"

#include <stdio.h>


/*
 * Load the image.
 */
bool LoadableImage::Create(const char* fileName, int x, int y)
{
    if (fileName == NULL || x < 0 || y < 0) {
        fprintf(stderr, "bad params to %s\n", __PRETTY_FUNCTION__);
        return false;
    }

    delete[] mName;
    mName = android::strdupNew(fileName);

    mX = x;
    mY = y;

    return true;
}

/*
 * Load the bitmap.
 */
bool LoadableImage::LoadResources(void)
{
    if (mName == NULL)
        return false;

    if (mpBitmap != NULL)       // already loaded?
        return true;

    //printf("LoadResources: '%s'\n", (const char*) mName);
#ifdef BEFORE_ASSET
    wxImage img(mName);
#else
    android::AssetManager* pAssetMgr = ((MyApp*)wxTheApp)->GetAssetManager();
    android::Asset* pAsset;

    pAsset = pAssetMgr->open(mName, android::Asset::ACCESS_RANDOM);
    if (pAsset == NULL) {
        fprintf(stderr, "ERROR: unable to load '%s'\n", mName);
        return false;
    } else {
        //printf("--- opened asset '%s'\n",
        //    (const char*) pAsset->getAssetSource());
    }
    AssetStream astr(pAsset);

    wxImage img(astr);
#endif

    mWidth = img.GetWidth();
    mHeight = img.GetHeight();
    if (mWidth <= 0 || mHeight <= 0) {
        /* image failed to load or decode */
        fprintf(stderr, "ERROR: unable to load/decode '%s'\n", mName);
        //delete img;
        return false;
    }

    mpBitmap = new wxBitmap(img);

    //delete img;

    return true;
}

/*
 * Unload the bitmap.
 */
bool LoadableImage::UnloadResources(void)
{
    delete mpBitmap;
    mpBitmap = NULL;
    return true;
}

