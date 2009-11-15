//
// Copyright 2005 The Android Open Source Project
//
// Create or attach to a named bi-directional channel on the local machine.
//
#ifndef __LIBS_LOCALBICHANNEL_H
#define __LIBS_LOCALBICHANNEL_H

#ifdef HAVE_ANDROID_OS
#error DO NOT USE THIS FILE IN THE DEVICE BUILD
#endif

#include "Pipe.h"

namespace android {

/*
 * This is essentially a wrapper class for UNIX-domain sockets.  The
 * idea is to set one up with create() or attach to one with attach()
 * and then extract the unidirectional read/write Pipes.  These can
 * be used directly or stuffed into a MessageStream.
 *
 * The name for the pipe should be a short filename made up of alphanumeric
 * characters.  Depending on the implementation, we may create a file in
 * /tmp with the specified name, removing any existing copy.
 */
class LocalBiChannel {
public:
    LocalBiChannel(void);
    ~LocalBiChannel(void);

    /* create the "listen" side */
    bool create(const char* name);

    /*
     * Listen for a connection.  When we get one, we create unidirectional
     * read/write pipes and return them.
     */
    bool listen(Pipe** ppReadPipe, Pipe** ppWritePipe);

    /*
     * Attach to a channel created by somebody else.  Returns pipes.
     */
    bool attach(const char* name, Pipe** ppReadPipe, Pipe** ppWritePipe);

private:
    char*       mFileName;
    bool        mIsListener;
    unsigned long mHandle;
};

}; // namespace android

#endif // __LIBS_LOCALBICHANNEL_H
