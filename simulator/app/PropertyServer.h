//
// Copyright 2007 The Android Open Source Project
//
// Serve properties to the simulated runtime.
//
#ifndef _SIM_PROPERTY_SERVER_H
#define _SIM_PROPERTY_SERVER_H

#include "cutils/properties.h"
#include "utils/List.h"

/*
 * Define a thread that responds to requests from clients to get/set/list
 * system properties.
 */
class PropertyServer : public wxThread {
public:
    PropertyServer(void) : mListenSock(-1) {}
    virtual ~PropertyServer(void);

    /* start the thread running */
    bool StartThread(void);

    /* thread entry point */
    virtual void* Entry(void);

    /* clear out all properties */
    void ClearProperties(void);

    /* add some default values */
    void SetDefaultProperties(void);

    /* copy a property into valueBuf; returns false if property not found */
    bool GetProperty(const char* key, char* valueBuf);

    /* set the property, replacing it if it already exists */
    bool SetProperty(const char* key, const char* value);

    /* property name constants */
    static const char* kPropCheckJni;

private:
    /* one property entry */
    typedef struct Property {
        char    key[PROPERTY_KEY_MAX];
        char    value[PROPERTY_VALUE_MAX];
    } Property;

    /* create the UNIX-domain socket we listen on */
    bool CreateSocket(const char* fileName);

    /* serve up properties */
    void ServeProperties(void);

    /* handle a client request */
    bool HandleRequest(int fd);

    /* listen here for new connections */
    int     mListenSock;

    /* list of connected fds to scan */
    android::List<int>      mClientList;

    /* set of known properties */
    android::List<Property> mPropList;
};

#endif // PROPERTY_SERVER_H
