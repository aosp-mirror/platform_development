//
// Copyright 2005 The Android Open Source Project
//
// Our collection of devices.
//

// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"

// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
//#include "wx/image.h"   // needed for Windows build


#include "PhoneCollection.h"
#include "PhoneData.h"
#include "MyApp.h"

#include "utils.h"

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <dirent.h>
#include <assert.h>

using namespace android;

/*static*/ PhoneCollection* PhoneCollection::mpInstance = NULL;

/*static*/ const char* PhoneCollection::kLayoutFile = "layout.xml";


/*
 * (Re-)scan the specified directory for phones.  We register a hit if we can
 * see a file called "<directory>/layout.xml".
 */
void PhoneCollection::ScanForPhones(const char* directory)
{
    /*
     * Scan through the directory and find everything that looks like it
     * might hold phone data.
     */
    StringArray strArr;

#ifdef BEFORE_ASSET
    DIR* dirp;
    struct dirent* entp;

    dirp = opendir(directory);
    if (dirp == NULL) {
        char buf[512];
        fprintf(stderr, "ERROR: unable to scan directory '%s' for phone data\n",
            directory);
        fprintf(stderr, "Current dir is %s\n", getcwd(buf, sizeof(buf)));
        return;
    }

    while (1) {
        wxString dirName;
        wxString fileName;

        entp = readdir(dirp);
        if (entp == NULL)
            break;              // done with scan
        dirName = directory;
        dirName += '/';
        dirName += entp->d_name;
        fileName = dirName;
        fileName += '/';
        fileName += kLayoutFile;

        if (access(fileName, R_OK) == 0) {
            strArr.push_back(dirName);
            //printf("--- examining '%s'\n", (const char*) fileName);
        }
    }
    closedir(dirp);
#else
    android::AssetManager* pAssetMgr = ((MyApp*)wxTheApp)->GetAssetManager();
    android::AssetDir* pDir;
    int i, count;

    pDir = pAssetMgr->openDir("");
    assert(pDir != NULL);
    count = pDir->getFileCount();

    for (i = 0; i < count; i++) {
        android::String8 layoutPath;

        if (pDir->getFileType(i) != kFileTypeDirectory)
            continue;

        layoutPath = pDir->getFileName(i);
        layoutPath.appendPath(kLayoutFile);

        if (pAssetMgr->getFileType(layoutPath.string()) == kFileTypeRegular) {
            strArr.push_back(pDir->getFileName(i).string());
            printf("--- examining '%s'\n", layoutPath.string());
        }
    }

    delete pDir;
#endif

    if (strArr.size() == 0) {
        fprintf(stderr, "ERROR: no phone data found in '%s'\n", directory);
        return;
    }

    /*
     * Found some candidates.  If they parse successfully, add them to
     * our list.
     *
     * We sort them first, because it's nice when everybody's user
     * interface looks the same.  Note we're sorting the directory name,
     * so it's possible to define a sort order in the filesystem that
     * doesn't require messing up the phone's title string.
     */
    mPhoneList.clear();
    strArr.sort(StringArray::cmpAscendingAlpha);

    for (int i = 0; i < strArr.size(); i++) {
        PhoneData tmpPhone;

        if (!tmpPhone.Create(strArr.getEntry(i))) {
            fprintf(stderr, "Sim: Abandoning phone '%s'\n", strArr.getEntry(i));
            //strArr.erase(i);
            //i--;
        } else {
            if (GetPhoneData(tmpPhone.GetName()) != NULL) {
                fprintf(stderr, "Sim: ERROR: duplicate name '%s' in '%s'\n",
                    tmpPhone.GetName(), strArr.getEntry(i));
            } else {
                mPhoneList.push_back(tmpPhone);
            }
        }
    }
}


/*
 * Return the Nth member of the phone data array.  (Replace w/Vector.)
 */
PhoneData* PhoneCollection::GetPhoneData(int idx)
{
    typedef List<PhoneData>::iterator Iter;

    for (Iter ii = mPhoneList.begin(); ii != mPhoneList.end(); ++ii) {
        if (idx == 0)
            return &(*ii);
        --idx;
    }
    return NULL;
}

/*
 * Return the entry whose phone data name matches "name".
 */
PhoneData* PhoneCollection::GetPhoneData(const char* name)
{
    typedef List<PhoneData>::iterator Iter;

    for (Iter ii = mPhoneList.begin(); ii != mPhoneList.end(); ++ii) {
        if (strcasecmp((*ii).GetName(), name) == 0)
            return &(*ii);
    }
    return NULL;
}

