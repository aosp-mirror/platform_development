//
// Copyright 2005 The Android Open Source Project
//
// Simulated device definition.
//
// The "root" of the data structures here is PhoneCollection, which may
// discard the entire set if the user asks to re-scan the phone definitions.
// These structures should be considered read-only.
//
// PhoneCollection (single global instance)
//    -->PhoneData
//       -->PhoneDisplay
//       -->PhoneMode
//          -->PhoneView
//
#ifndef _SIM_PHONE_DATA_H
#define _SIM_PHONE_DATA_H

#include <stdio.h>
#include "tinyxml.h"

#include "PhoneButton.h"
#include "LoadableImage.h"
#include <ui/PixelFormat.h>
#include "utils.h"


/*
 * This represents the keyboard type of the simulated device
 */
class PhoneKeyboard {
public:
    PhoneKeyboard(void)
        : mQwerty(false), mKeyMap(NULL)
        {}       
    ~PhoneKeyboard(void) {
        free((void*)mKeyMap);
    }    
 
    PhoneKeyboard(const PhoneKeyboard& src)
        : mQwerty(false), mKeyMap(NULL)
    {
        CopyMembers(src);
    }
    PhoneKeyboard& operator=(const PhoneKeyboard& src) {
        if (this != &src)       // self-assignment
            CopyMembers(src);
        return *this;
    }
    void CopyMembers(const PhoneKeyboard& src) {
        mQwerty = src.mQwerty;
        mKeyMap = src.mKeyMap ? strdup(src.mKeyMap) : NULL;
    }
    
    bool ProcessAndValidate(TiXmlNode* pNode);
    
    bool getQwerty() { return mQwerty; }    

    const char *getKeyMap() { return mKeyMap; }
private:
    bool    mQwerty;
    const char * mKeyMap;
};

/*
 * This represents a single display device, usually an LCD screen.
 * It also includes an optional surrounding graphic, usually a picture of
 * the device itself.
 */
class PhoneDisplay {
public:
    PhoneDisplay(void)
        : mName(NULL)
        {}
    ~PhoneDisplay(void) {
        delete[] mName;
    }

    PhoneDisplay(const PhoneDisplay& src)
        : mName(NULL)
    {
        CopyMembers(src);
    }
    PhoneDisplay& operator=(const PhoneDisplay& src) {
        if (this != &src)       // self-assignment
            CopyMembers(src);
        return *this;
    }
    void CopyMembers(const PhoneDisplay& src) {
        // Can't memcpy and member-copy the container classes, because the
        // containers have already been constructed, and for operator= they
        // might even have stuff in them.
        delete[] mName;
        mName = android::strdupNew(src.mName);
        mWidth = src.mWidth;
        mHeight = src.mHeight;
        mFormat = src.mFormat;
        mRefresh = src.mRefresh;
    }

    bool ProcessAndValidate(TiXmlNode* pNode);

    const char* GetName(void) const { return mName; }
    int GetWidth(void) const { return mWidth; }
    int GetHeight(void) const { return mHeight; }
    android::PixelFormat GetFormat(void) const { return mFormat; }
    int GetRefresh(void) const { return mRefresh; }

    static bool IsCompatible(PhoneDisplay* pDisplay1, PhoneDisplay* pDisplay2);

private:
    char*           mName;

    // display dimensions, in pixels
    int             mWidth;
    int             mHeight;

    // frame buffer format
    android::PixelFormat mFormat;

    // display refresh rate, in fps
    int             mRefresh;
};

/*
 * This is a "view" of a device, which includes the display, a background
 * image, and perhaps some clickable keys for input.
 *
 * Because the key graphics are associated with a particular display, we
 * hold a list of keys here.  (It also allows the possibility of handling
 * a situation where the same key shows up in multiple background images,
 * e.g. a flip phone with a "volume" key on the side.  If we include the
 * key in both places, we can highlight it on both displays.)
 */
class PhoneView {
public:
    PhoneView(void)
        : mDisplayName(NULL)
        {}
    ~PhoneView(void) {
        delete[] mDisplayName;
    }

    PhoneView(const PhoneView& src) {
        CopyMembers(src);
    }
    PhoneView& operator=(const PhoneView& src) {
        if (this != &src)       // self-assignment
            CopyMembers(src);
        return *this;
    }
    void CopyMembers(const PhoneView& src) {
        // Can't memcpy and member-copy the container classes, because the
        // containers have already been constructed, and for operator= they
        // might even have stuff in them.
        mImageList = src.mImageList;
        mButtonList = src.mButtonList;
        mDisplayName = android::strdupNew(src.mDisplayName);
        mXOffset = src.mXOffset;
        mYOffset = src.mYOffset;
        mRotation = src.mRotation;
    }

    // load or unload resources, e.g. wxBitmaps from image files
    bool LoadResources(void);
    bool UnloadResources(void);

    // simple accessors
    int GetXOffset(void) const { return mXOffset; }
    int GetYOffset(void) const { return mYOffset; }
    const char* GetDisplayName(void) const { return mDisplayName; }

    // image list access
    int GetBkgImageCount(void) const;
    const LoadableImage* GetBkgImage(int idx) const;

    // find the first button that covers the specified coords
    PhoneButton* FindButtonHit(int x, int y);

    // find the first button with a matching key code
    PhoneButton* FindButtonByKey(KeyCode keyCode);

    bool ProcessAndValidate(TiXmlNode* pNode, const char* directory);
    bool ProcessImage(TiXmlNode* pNode, const char* directory);
    bool ProcessButton(TiXmlNode* pNode, const char* directory);

private:
    // background images for the phone picture that surrounds the display
    android::List<LoadableImage> mImageList;

    // list of accessible buttons, some of which have highlight graphics
    android::List<PhoneButton>  mButtonList;

    char*           mDisplayName;

    // these determine where in the image the display output goes
    int             mXOffset;
    int             mYOffset;

    // clockwise rotation of the output; sim must rotate in opposite direction
    typedef enum Rotation {
        kRotUnknown = 0,
        kRot0,
        kRot90,
        kRot180,
        kRot270,
    } Rotation;
    Rotation        mRotation;
};

/*
 * One mode of a phone.  Simple devices only have one mode.  Flip phones
 * have two (opened and closed).  Other devices might have more.  The
 * mode is communicated to the runtime because it may need to process
 * input events differently.
 */
class PhoneMode {
public:
    PhoneMode(void)
        : mName(NULL)
        {}
    ~PhoneMode(void) {
        delete[] mName;
    }

    PhoneMode(const PhoneMode& src)
        : mName(NULL)
    {
        CopyMembers(src);
    }
    PhoneMode& operator=(const PhoneMode& src) {
        if (this != &src)       // self-assignment
            CopyMembers(src);
        return *this;
    }
    void CopyMembers(const PhoneMode& src) {
        delete[] mName;
        mName = android::strdupNew(src.mName);
        mViewList = src.mViewList;
    }


    // load or unload resources for this object and all members of mViewList
    bool LoadResources(void);
    bool UnloadResources(void);

    // get the #of views
    int GetNumViews(void) const { return mViewList.size(); }
    // get the Nth display
    PhoneView* GetPhoneView(int viewNum);

    const char* GetName(void) const { return mName; }
    void SetName(const char* name) {
        delete[] mName;
        mName = android::strdupNew(name);
    }

    // load the <mode> section from the config file
    bool ProcessAndValidate(TiXmlNode* pNode, const char* directory);

private:
    char*           mName;

    android::List<PhoneView> mViewList;
};

/*
 * This holds the data for one device.
 *
 * Each device may have multiple "modes", e.g. a flip-phone that can be
 * open or shut.  Each mode has different configurations for the visible
 * displays and active keys.
 */
class PhoneData {
public:
    PhoneData(void) :
        mName(NULL), mTitle(NULL), mDirectory(NULL)
        {}
    virtual ~PhoneData(void) {
        delete[] mName;
        delete[] mTitle;
        delete[] mDirectory;
    }

    PhoneData(const PhoneData& src)
        : mName(NULL), mTitle(NULL), mDirectory(NULL)
    {
        CopyMembers(src);
    }
    PhoneData& operator=(const PhoneData& src) {
        if (this != &src)       // self-assignment
            CopyMembers(src);
        return *this;
    }
    void CopyMembers(const PhoneData& src) {
        delete[] mName;
        delete[] mTitle;
        delete[] mDirectory;
        mName = android::strdupNew(src.mName);
        mTitle = android::strdupNew(src.mTitle);
        mDirectory = android::strdupNew(src.mDirectory);
        mModeList = src.mModeList;
        mDisplayList = src.mDisplayList;
        mKeyboardList = src.mKeyboardList;
    }

    // initialize the object with the phone data in the specified dir
    bool Create(const char* directory);

    // load or unload resources, e.g. wxBitmaps from image files
    bool LoadResources(void);
    bool UnloadResources(void);

    // simple accessors
    const char* GetName(void) const { return mName; }
    void SetName(const char* name) {
        delete[] mName;
        mName = android::strdupNew(name);
    }
    const char* GetTitle(void) const { return mTitle; }
    void SetTitle(const char* title) {
        delete[] mTitle;
        mTitle = android::strdupNew(title);
    }
    const char* GetDirectory(void) const { return mDirectory; }
    void SetDirectory(const char* dir) {
        delete[] mDirectory;
        mDirectory = android::strdupNew(dir);
    }

    
    // get number of modes
    int GetNumModes(void) const { return mModeList.size(); }
    // get the specified mode object
    PhoneMode* GetPhoneMode(int idx);
    PhoneMode* GetPhoneMode(const char* modeName);

    // get number of displays
    int GetNumDisplays(void) const { return mDisplayList.size(); }
    // get the specified display object
    PhoneDisplay* GetPhoneDisplay(int idx);
    PhoneDisplay* GetPhoneDisplay(const char* displayName);
    // get the index of the matching display
    int GetPhoneDisplayIndex(const char* displayName);

    // get number of keyboards
    int GetNumKeyboards(void) const { return mKeyboardList.size(); }
    // get the specified display object
    PhoneKeyboard* GetPhoneKeyboard(int idx);
    
private:
    bool ProcessAndValidate(TiXmlDocument* pDoc);
    bool ProcessDevice(TiXmlNode* pNode);
    bool ProcessTitle(TiXmlNode* pNode);

    char*           mName;
    char*           mTitle;
    char*           mDirectory;

    android::List<PhoneMode> mModeList;
    android::List<PhoneDisplay> mDisplayList;
    android::List<PhoneKeyboard> mKeyboardList;
};

#endif // _SIM_PHONE_DATA_H
