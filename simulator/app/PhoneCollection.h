//
// Copyright 2005 The Android Open Source Project
//
// Our collection of devices.
//
#ifndef _SIM_PHONE_COLLECTION_H
#define _SIM_PHONE_COLLECTION_H

#include <stdlib.h>
#include "PhoneData.h"

/*
 * Only one instance of this class exists.  It contains a list of all
 * known devices, and methods for scanning for devices.
 */
class PhoneCollection {
public:
    /* get the global instance */
    static PhoneCollection* GetInstance(void) {
        if (mpInstance == NULL)
            mpInstance = new PhoneCollection;
        return mpInstance;
    }
    /* destroy the global instance when shutting down */
    static void DestroyInstance(void) {
        delete mpInstance;
        mpInstance = NULL;
    }

    /* scan for phones in subdirectories of "directory" */
    void ScanForPhones(const char* directory);

    /* get phone data */
    int GetPhoneCount(void) const { return mPhoneList.size(); } // slow
    PhoneData* GetPhoneData(int idx);
    PhoneData* GetPhoneData(const char* name);

    /* layout.xml filename -- a string constant used in various places */
    static const char* kLayoutFile;

private:
    PhoneCollection(void) {}
    ~PhoneCollection(void) {}

    /* the phone data; make this a Vector someday */
    android::List<PhoneData>    mPhoneList;

    /* storage for global instance pointer */
    static PhoneCollection* mpInstance;
};

#endif // _SIM_PHONE_COLLECTION_H
