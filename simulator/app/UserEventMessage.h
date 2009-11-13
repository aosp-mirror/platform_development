//
// Copyright 2005 The Android Open Source Project
//
// Contents of the "user event" sent from the device thread.
//
#ifndef _SIM_USER_EVENT_MESSAGE_H
#define _SIM_USER_EVENT_MESSAGE_H

#include "utils.h"
#include "LogMessage.h"

/*
 * This gets stuffed into a UserEvent, which is posted to the main thread
 * from a worker thread.
 *
 * The object does NOT own anything you stuff into it.  It's just a vehicle
 * for carting data from one thread to another in a wxWidgets-safe manner,
 * usually as pointers to data that can be shared between threads.
 */
class UserEventMessage {
public:
    /*
     * What type of message is this?
     */
    typedef enum UEMType {
        kUnknown = 0,

        kRuntimeStarted,
        kRuntimeStopped,
        kErrorMessage,      // message in mString
        kLogMessage,        // ptr to heap-allocated LogMessage
        kExternalRuntime,   // external runtime wants to party
    } UEMType;

    UserEventMessage(void)
        : mType(kUnknown), mpLogMessage(NULL)
        {}
    ~UserEventMessage(void) {
    }

    /*
     * Create one of our various messages.
     */
    void CreateRuntimeStarted(void) {
        mType = kRuntimeStarted;
    }
    void CreateRuntimeStopped(void) {
        mType = kRuntimeStopped;
    }
    void CreateErrorMessage(wxString& str) {
        mType = kErrorMessage;
        mString = str;
    }
    void CreateLogMessage(LogMessage* pLogMessage) {
        mType = kLogMessage;
        mpLogMessage = pLogMessage;
    }
    void CreateExternalRuntime(android::Pipe* reader, android::Pipe* writer) {
        mType = kExternalRuntime;
        mReader = reader;
        mWriter = writer;
    }

    /*
     * Accessors.
     */
    UEMType GetType(void) const { return mType; }
    const wxString& GetString(void) const { return mString; }
    LogMessage* GetLogMessage(void) const { return mpLogMessage; }
    android::Pipe* GetReader(void) const { return mReader; }
    android::Pipe* GetWriter(void) const { return mWriter; }

private:
    UserEventMessage& operator=(const UserEventMessage&);   // not implemented
    UserEventMessage(const UserEventMessage&);              // not implemented

    UEMType     mType;
    wxString    mString;            // for kErrorMessage
    LogMessage* mpLogMessage;       // for kLogMessage
    android::Pipe*  mReader;        // for kExternalRuntime
    android::Pipe*  mWriter;        // for kExternalRuntime
};

#endif // _SIM_USER_EVENT_MESSAGE_H
