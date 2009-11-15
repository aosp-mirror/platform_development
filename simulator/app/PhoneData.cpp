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


#include "PhoneData.h"
#include "PhoneButton.h"
#include "PhoneCollection.h"
#include "MyApp.h"

#include "utils.h"
#include <utils/AssetManager.h>
#include <utils/String8.h>

#include "tinyxml.h"

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>

using namespace android;

/* image relative path hack */
static const char* kRelPathMagic = "::/";


/*
 * ===========================================================================
 *      PhoneKeyboard
 * ===========================================================================
 */

/*
 * Load a <keyboard> chunk.
 */
bool PhoneKeyboard::ProcessAndValidate(TiXmlNode* pNode)
{
    //TiXmlNode* pChild;
    TiXmlElement* pElem;
    int qwerty = 0;
    
    assert(pNode->Type() == TiXmlNode::ELEMENT);

    pElem = pNode->ToElement();
    pElem->Attribute("qwerty", &qwerty);
    const char *kmap = pElem->Attribute("keycharmap");

    if (qwerty == 1) {
        printf("############## PhoneKeyboard::ProcessAndValidate: qwerty = true!\n");
        mQwerty = true;
    }

    if (kmap != NULL) {
        printf("############## PhoneKeyboard::ProcessAndValidate: keycharmap = %s\n", kmap);
        mKeyMap = strdup(kmap);
    }
    
    return true;
}


/*
 * ===========================================================================
 *      PhoneDisplay
 * ===========================================================================
 */

/*
 * Load a <display> chunk.
 */
bool PhoneDisplay::ProcessAndValidate(TiXmlNode* pNode)
{
    //TiXmlNode* pChild;
    TiXmlElement* pElem;
    const char* name;
    const char* format;

    assert(pNode->Type() == TiXmlNode::ELEMENT);

    /*
     * Process attributes.  Right now they're all mandatory, but some of
     * them could be defaulted (e.g. "rotate").
     *
     * [We should do some range-checking here.]
     */
    pElem = pNode->ToElement();
    name = pElem->Attribute("name");
    if (name == NULL)
        goto missing;
    if (pElem->Attribute("width", &mWidth) == NULL)
        goto missing;
    if (pElem->Attribute("height", &mHeight) == NULL)
        goto missing;
    if (pElem->Attribute("refresh", &mRefresh) == NULL)
        goto missing;
    format = pElem->Attribute("format");
    if (format == NULL)
        goto missing;

    delete[] mName;
    mName = strdupNew(name);

    if (strcasecmp(format, "rgb565") == 0) {
        mFormat = android::PIXEL_FORMAT_RGB_565;
    } else {
        fprintf(stderr, "SimCFG: unexpected value for display format\n");
        return false;
    }

    return true;

missing:
    fprintf(stderr,
        "SimCFG: <display> requires name/width/height/format/refresh\n");
    return false;
}


/*
 * Returns "true" if the two displays are compatible, "false" if not.
 *
 * Compatibility means they have the same resolution, format, refresh
 * rate, and so on.  Anything transmitted to the runtime as part of the
 * initial configuration setup should be tested.
 */
/*static*/ bool PhoneDisplay::IsCompatible(PhoneDisplay* pDisplay1,
    PhoneDisplay* pDisplay2)
{
    return (pDisplay1->mWidth == pDisplay2->mWidth &&
            pDisplay1->mHeight == pDisplay2->mHeight &&
            pDisplay1->mFormat == pDisplay2->mFormat &&
            pDisplay1->mRefresh == pDisplay2->mRefresh);
}


/*
 * ===========================================================================
 *      PhoneView
 * ===========================================================================
 */

/*
 * Load a <view> chunk.
 */
bool PhoneView::ProcessAndValidate(TiXmlNode* pNode, const char* directory)
{
    TiXmlNode* pChild;
    TiXmlElement* pElem;
    int rotate;
    const char* displayName;

    assert(pNode->Type() == TiXmlNode::ELEMENT);

    /*
     * Process attributes.  Right now they're all mandatory, but some of
     * them could be defaulted (e.g. "rotate").
     *
     * [We should do some range-checking here.]
     */
    pElem = pNode->ToElement();
    displayName = pElem->Attribute("display");
    if (displayName == NULL)
        goto missing;
    if (pElem->Attribute("x", &mXOffset) == NULL)
        goto missing;
    if (pElem->Attribute("y", &mYOffset) == NULL)
        goto missing;
    if (pElem->Attribute("rotate", &rotate) == NULL)
        goto missing;

    switch (rotate) {
    case 0:     mRotation = kRot0;      break;
    case 90:    mRotation = kRot90;     break;
    case 180:   mRotation = kRot180;    break;
    case 270:   mRotation = kRot270;    break;
    default:
                fprintf(stderr, "SimCFG: unexpected value for rotation\n");
                mRotation = kRotUnknown;
                return false;
    }

    delete[] mDisplayName;
    mDisplayName = android::strdupNew(displayName);

    /*
     * Process elements.
     */
    for (pChild = pNode->FirstChild(); pChild != NULL;
        pChild = pChild->NextSibling())
    {
        if (pChild->Type() == TiXmlNode::COMMENT)
            continue;

        if (pChild->Type() == TiXmlNode::ELEMENT) {
            if (strcasecmp(pChild->Value(), "image") == 0) {
                if (!ProcessImage(pChild, directory))
                    return false;
            } else if (strcasecmp(pChild->Value(), "button") == 0) {
                if (!ProcessButton(pChild, directory))
                    return false;
            } else {
                fprintf(stderr,
                    "SimCFG: Warning: unexpected elements in <display>\n");
            }
        } else {
            fprintf(stderr, "SimCFG: Warning: unexpected stuff in <display>\n");
        }
    }

    return true;

missing:
    fprintf(stderr,
        "SimCFG: <view> requires display/x/y/rotate\n");
    return false;
}

/*
 * Handle <image src="zzz" x="123" y="123"/>.
 */
bool PhoneView::ProcessImage(TiXmlNode* pNode, const char* directory)
{
    TiXmlNode* pChild;
    TiXmlElement* pElem;
    int x, y;
    const char* src;
    LoadableImage tmpLimg;
    android::String8 fileName;

    pChild = pNode->FirstChild();
    if (pChild != NULL) {
        fprintf(stderr, "SimCFG: <image> is funky\n");
        return false;
    }

    /*
     * All attributes are mandatory.
     */
    pElem = pNode->ToElement();
    src = pElem->Attribute("src");
    if (src == NULL)
        goto missing;
    if (pElem->Attribute("x", &x) == NULL)
        goto missing;
    if (pElem->Attribute("y", &y) == NULL)
        goto missing;

    if (strncmp(src, kRelPathMagic, strlen(kRelPathMagic)) == 0) {
        fileName = src + strlen(kRelPathMagic);
    } else {
        fileName = directory;
        fileName += "/";
        fileName += src;
    }

    tmpLimg.Create(fileName, x, y);
    mImageList.push_back(tmpLimg);

    return true;

missing:
    fprintf(stderr, "SimCFG: <image> requires src/x/y\n");
    return false;
}

/*
 * Handle <button keyCode="zzz" src="zzz" x="123" y="123"/> and
 * <button keyCode="zzz"/>.
 */
bool PhoneView::ProcessButton(TiXmlNode* pNode, const char* directory)
{
    TiXmlNode* pChild;
    TiXmlElement* pElem;
    int x, y;
    const char* keyCode;
    const char* src;
    PhoneButton tmpButton;
    android::String8 fileName;

    pChild = pNode->FirstChild();
    if (pChild != NULL) {
        fprintf(stderr, "SimCFG: button is funky\n");
        return false;
    }

    /*
     * Only keyCode is mandatory.  If they specify "src", then "x" and "y"
     * are also required.
     */
    pElem = pNode->ToElement();
    keyCode = pElem->Attribute("keyCode");
    if (keyCode == NULL)
        goto missing;

    src = pElem->Attribute("src");
    if (src != NULL) {
        if (pElem->Attribute("x", &x) == NULL)
            goto missing;
        if (pElem->Attribute("y", &y) == NULL)
            goto missing;
    }

    if (src == NULL)
        tmpButton.Create(keyCode);
    else {
        if (strncmp(src, kRelPathMagic, strlen(kRelPathMagic)) == 0) {
            fileName = src + strlen(kRelPathMagic);
        } else {
            fileName = directory;
            fileName += "/";
            fileName += src;
        }
        tmpButton.Create(keyCode, fileName, x, y);
    }

    mButtonList.push_back(tmpButton);

    return true;

missing:
    fprintf(stderr, "SimCFG: <button> requires keycode and may have src/x/y\n");
    return false;
}


/*
 * Load all resources associated with the display.
 */
bool PhoneView::LoadResources(void)
{
    typedef List<LoadableImage>::iterator LIter;
    typedef List<PhoneButton>::iterator BIter;

    for (LIter ii = mImageList.begin(); ii != mImageList.end(); ++ii)
        (*ii).LoadResources();
    for (BIter ii = mButtonList.begin(); ii != mButtonList.end(); ++ii)
        (*ii).LoadResources();
    return true;
}

/*
 * Unload all resources associated with the display.
 */
bool PhoneView::UnloadResources(void)
{
    typedef List<LoadableImage>::iterator LIter;
    typedef List<PhoneButton>::iterator BIter;

    for (LIter ii = mImageList.begin(); ii != mImageList.end(); ++ii)
        (*ii).UnloadResources();
    for (BIter ii = mButtonList.begin(); ii != mButtonList.end(); ++ii)
        (*ii).UnloadResources();
    return true;
}


/*
 * Get the #of images.
 */
int PhoneView::GetBkgImageCount(void) const
{
    return mImageList.size();
}

/*
 * Return the Nth entry.
 */
const LoadableImage* PhoneView::GetBkgImage(int idx) const
{
    typedef List<LoadableImage>::const_iterator Iter;

    for (Iter ii = mImageList.begin(); ii != mImageList.end(); ++ii) {
        if (!idx)
            return &(*ii);
        --idx;
    }

    return NULL;
}


/*
 * Find the first button that covers the specified coordinates.
 *
 * The coordinates must be relative to the upper left corner of the
 * phone image.
 */
PhoneButton* PhoneView::FindButtonHit(int x, int y)
{
    typedef List<PhoneButton>::iterator Iter;

    for (Iter ii = mButtonList.begin(); ii != mButtonList.end(); ++ii) {
        if ((*ii).CheckCollision(x, y))
            return &(*ii);
    }

    return NULL;
}

/*
 * Find the first button with a matching key code.
 */
PhoneButton* PhoneView::FindButtonByKey(KeyCode keyCode)
{
    typedef List<PhoneButton>::iterator Iter;

    for (Iter ii = mButtonList.begin(); ii != mButtonList.end(); ++ii) {
        if ((*ii).GetKeyCode() == keyCode)
            return &(*ii);
    }

    return NULL;
}


/*
 * ===========================================================================
 *      PhoneMode
 * ===========================================================================
 */

/*
 * Process a <mode name="zzz"> chunk.
 */
bool PhoneMode::ProcessAndValidate(TiXmlNode* pNode, const char* directory)
{
    TiXmlNode* pChild;
    const char* name;

    assert(pNode->Type() == TiXmlNode::ELEMENT);

    name = pNode->ToElement()->Attribute("name");
    if (name == NULL) {
        fprintf(stderr, "SimCFG: <mode> requires name attrib\n");
        return false;
    }
    SetName(name);

    for (pChild = pNode->FirstChild(); pChild != NULL;
        pChild = pChild->NextSibling())
    {
        if (pChild->Type() == TiXmlNode::COMMENT)
            continue;

        if (pChild->Type() == TiXmlNode::ELEMENT &&
            strcasecmp(pChild->Value(), "view") == 0)
        {
            PhoneView tmpDisplay;
            bool result;

            result = tmpDisplay.ProcessAndValidate(pChild, directory);
            if (!result)
                return false;

            mViewList.push_back(tmpDisplay);
        } else {
            fprintf(stderr, "SimCFG: Warning: unexpected stuff in <mode>\n");
        }
    }

    if (mViewList.size() == 0) {
        fprintf(stderr, "SimCFG: no <view> entries found\n");
        return false;
    }

    return true;
}


/*
 * Load all resources associated with the phone.
 */
bool PhoneMode::LoadResources(void)
{
    typedef List<PhoneView>::iterator Iter;

    for (Iter ii = mViewList.begin(); ii != mViewList.end(); ++ii)
        (*ii).LoadResources();
    return true;
}

/*
 * Unload all resources associated with the phone.
 */
bool PhoneMode::UnloadResources(void)
{
    typedef List<PhoneView>::iterator Iter;

    for (Iter ii = mViewList.begin(); ii != mViewList.end(); ++ii)
        (*ii).UnloadResources();
    return true;
}


/*
 * Return the Nth entry.  [make this a Vector?]
 */
PhoneView* PhoneMode::GetPhoneView(int viewNum)
{
    typedef List<PhoneView>::iterator Iter;

    for (Iter ii = mViewList.begin(); ii != mViewList.end(); ++ii) {
        if (viewNum == 0)
            return &(*ii);
        --viewNum;
    }
    return NULL;
}


/*
 * ===========================================================================
 *      PhoneData
 * ===========================================================================
 */


/*
 * Look for a "layout.xml" in the specified directory.  If found, parse
 * the contents out.
 *
 * Returns "true" on success, "false" on failure.
 */
bool PhoneData::Create(const char* directory)
{
    android::String8 fileName;

    SetDirectory(directory);

    fileName = directory;
    fileName += "/";
    fileName += PhoneCollection::kLayoutFile;

#ifdef BEFORE_ASSET
    TiXmlDocument doc(fileName);
    if (!doc.LoadFile())
#else
    android::AssetManager* pAssetMgr = ((MyApp*)wxTheApp)->GetAssetManager();
    TiXmlDocument doc;
    android::Asset* pAsset;
    bool result;

    pAsset = pAssetMgr->open(fileName, Asset::ACCESS_STREAMING);
    if (pAsset == NULL) {
        fprintf(stderr, "Unable to open asset '%s'\n", (const char*) fileName);
        return false;
    } else {
        //printf("--- opened asset '%s'\n",
        //    (const char*) pAsset->getAssetSource());
    }

    /* TinyXml insists that the buffer be NULL-terminated... ugh */
    char* buf = new char[pAsset->getLength() +1];
    pAsset->read(buf, pAsset->getLength());
    buf[pAsset->getLength()] = '\0';

    delete pAsset;
    result = doc.Parse(buf);
    delete[] buf;

    if (!result)
#endif
    {
        fprintf(stderr, "SimCFG: ERROR: failed parsing '%s'\n",
            (const char*) fileName);
        if (doc.ErrorRow() != 0)
            fprintf(stderr, "    XML: %s (row=%d col=%d)\n",
                doc.ErrorDesc(), doc.ErrorRow(), doc.ErrorCol());
        else
            fprintf(stderr, "    XML: %s\n", doc.ErrorDesc());
        return false;
    }

    if (!ProcessAndValidate(&doc)) {
        fprintf(stderr, "SimCFG: ERROR: failed analyzing '%s'\n",
            (const char*) fileName);
        return false;
    }

    printf("SimCFG: loaded data from '%s'\n", (const char*) fileName);

    return true;
}

/*
 * TinyXml has loaded and parsed the XML document for us.  We need to
 * run through the DOM tree, pull out the interesting bits, and make
 * sure the stuff we need is present.
 *
 * Returns "true" on success, "false" on failure.
 */
bool PhoneData::ProcessAndValidate(TiXmlDocument* pDoc)
{
    bool deviceFound = false;
    TiXmlNode* pChild;

    assert(pDoc->Type() == TiXmlNode::DOCUMENT);

    for (pChild = pDoc->FirstChild(); pChild != NULL;
        pChild = pChild->NextSibling())
    {
        /*
         * Find the <device> entry.  There should be exactly one.
         */
        if (pChild->Type() == TiXmlNode::ELEMENT) {
            if (strcasecmp(pChild->Value(), "device") != 0) {
                fprintf(stderr,
                    "SimCFG: Warning: unexpected element '%s' at top level\n",
                    pChild->Value());
                continue;
            }
            if (deviceFound) {
                fprintf(stderr, "SimCFG: one <device> per customer\n");
                return false;
            }

            bool result = ProcessDevice(pChild);
            if (!result)
                return false;
            deviceFound = true;
        }
    }

    if (!deviceFound) {
        fprintf(stderr, "SimCFG: no <device> section found\n");
        return false;
    }

    return true;
}

/*
 * Process a <device name="zzz"> chunk.
 */
bool PhoneData::ProcessDevice(TiXmlNode* pNode)
{
    TiXmlNode* pChild;
    const char* name;

    assert(pNode->Type() == TiXmlNode::ELEMENT);

    name = pNode->ToElement()->Attribute("name");
    if (name == NULL) {
        fprintf(stderr, "SimCFG: <device> requires name attrib\n");
        return false;
    }
    SetName(name);

    /*
     * Walk through the children and find interesting stuff.
     *
     * Might be more correct to process all <display> entries and
     * then process all <view> entries, since <view> has "pointers"
     * to <display>.  We're deferring the lookup until later, though,
     * so for now it doesn't really matter.
     */
    for (pChild = pNode->FirstChild(); pChild != NULL;
        pChild = pChild->NextSibling())
    {
        bool result;

        if (pChild->Type() == TiXmlNode::COMMENT)
            continue;

        if (pChild->Type() == TiXmlNode::ELEMENT &&
            strcasecmp(pChild->Value(), "title") == 0)
        {
            result = ProcessTitle(pChild);
            if (!result)
                return false;
        } else if (pChild->Type() == TiXmlNode::ELEMENT &&
            strcasecmp(pChild->Value(), "display") == 0)
        {
            PhoneDisplay tmpDisplay;

            result = tmpDisplay.ProcessAndValidate(pChild);
            if (!result)
                return false;

            mDisplayList.push_back(tmpDisplay);
        } else if (pChild->Type() == TiXmlNode::ELEMENT &&
            strcasecmp(pChild->Value(), "keyboard") == 0)
        {
            PhoneKeyboard tmpKeyboard;
            result = tmpKeyboard.ProcessAndValidate(pChild);
            if (!result)
                return false;
                
            mKeyboardList.push_back(tmpKeyboard);           
        } else if (pChild->Type() == TiXmlNode::ELEMENT &&
            strcasecmp(pChild->Value(), "mode") == 0)
        {
            PhoneMode tmpMode;

            result = tmpMode.ProcessAndValidate(pChild, mDirectory);
            if (!result)
                return false;

            mModeList.push_back(tmpMode);
        } else {
            fprintf(stderr, "SimCFG: Warning: unexpected stuff in <device>\n");
        }
    }

    if (mDisplayList.size() == 0) {
        fprintf(stderr, "SimCFG: no <display> entries found\n");
        return false;
    }
    if (mModeList.size() == 0) {
        fprintf(stderr, "SimCFG: no <mode> entries found\n");
        return false;
    }

    return true;
}

/*
 * Handle <title>.
 */
bool PhoneData::ProcessTitle(TiXmlNode* pNode)
{
    TiXmlNode* pChild;

    pChild = pNode->FirstChild();
    if (pChild->Type() != TiXmlNode::TEXT) {
        fprintf(stderr, "SimCFG: title is funky\n");
        return false;
    }

    SetTitle(pChild->Value());
    return true;
}


/*
 * Load all resources associated with the phone.
 */
bool PhoneData::LoadResources(void)
{
    typedef List<PhoneMode>::iterator Iter;

    for (Iter ii = mModeList.begin(); ii != mModeList.end(); ++ii)
        (*ii).LoadResources();
    return true;
}

/*
 * Unload all resources associated with the phone.
 */
bool PhoneData::UnloadResources(void)
{
    typedef List<PhoneMode>::iterator Iter;

    for (Iter ii = mModeList.begin(); ii != mModeList.end(); ++ii)
        (*ii).UnloadResources();
    return true;
}


/*
 * Return the PhoneMode entry with the matching name.
 *
 * Returns NULL if no match was found.
 */
PhoneMode* PhoneData::GetPhoneMode(const char* modeName)
{
    typedef List<PhoneMode>::iterator Iter;

    for (Iter ii = mModeList.begin(); ii != mModeList.end(); ++ii) {
        if (strcmp((*ii).GetName(), modeName) == 0)
            return &(*ii);
    }
    return NULL;
}

/*
 * Return the Nth phone mode entry.
 */
PhoneMode* PhoneData::GetPhoneMode(int idx)
{
    typedef List<PhoneMode>::iterator Iter;

    for (Iter ii = mModeList.begin(); ii != mModeList.end(); ++ii) {
        if (!idx)
            return &(*ii);
        --idx;
    }
    return NULL;
}


/*
 * Return the PhoneDisplay entry with the matching name.
 *
 * Returns NULL if no match was found.
 */
PhoneDisplay* PhoneData::GetPhoneDisplay(const char* dispName)
{
    typedef List<PhoneDisplay>::iterator Iter;

    for (Iter ii = mDisplayList.begin(); ii != mDisplayList.end(); ++ii) {
        if (strcmp((*ii).GetName(), dispName) == 0)
            return &(*ii);
    }
    return NULL;
}

/*
 * Return the Nth phone mode entry.
 */
PhoneDisplay* PhoneData::GetPhoneDisplay(int idx)
{
    typedef List<PhoneDisplay>::iterator Iter;

    for (Iter ii = mDisplayList.begin(); ii != mDisplayList.end(); ++ii) {
        if (!idx)
            return &(*ii);
        --idx;
    }
    return NULL;
}

/*
 * Find the PhoneDisplay entry with the matching name, and return its index.
 *
 * Returns -1 if the entry wasn't found.
 */
int PhoneData::GetPhoneDisplayIndex(const char* dispName)
{
    typedef List<PhoneDisplay>::iterator Iter;
    int idx = 0;

    for (Iter ii = mDisplayList.begin(); ii != mDisplayList.end(); ++ii) {
        if (strcmp((*ii).GetName(), dispName) == 0)
            return idx;
        idx++;
    }
    return -1;
}


/*
 * Return the Nth phone keyboard entry.
 */
PhoneKeyboard* PhoneData::GetPhoneKeyboard(int idx)
{
    typedef List<PhoneKeyboard>::iterator Iter;

    for (Iter ii = mKeyboardList.begin(); ii != mKeyboardList.end(); ++ii) {
        if (!idx)
            return &(*ii);
        --idx;
    }
    return NULL;
}
