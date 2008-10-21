//
// Copyright 2005 The Android Open Source Project
//  
// Management of the simulated device.
//  
    
// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"
    
// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"
    
#include "ExternalRuntime.h"
#include "MyApp.h"
#include "UserEvent.h"
#include "UserEventMessage.h"

#include "SimRuntime.h"
#include "LocalBiChannel.h"
#include "utils.h"


using namespace android;

/*
 * Destructor.
 */
ExternalRuntime::~ExternalRuntime(void)
{
    if (IsRunning()) {
        // TODO: cause thread to stop, then Wait for it
    }
    printf("Sim: in ~ExternalRuntime()\n");
}

/*
 * Create and run the thread.
 */
bool ExternalRuntime::StartThread(void)
{
    if (Create() != wxTHREAD_NO_ERROR) {
        fprintf(stderr, "Sim: ERROR: can't create ExternalRuntime thread\n");
        return false;
    }

    Run();
    return true;
}

/*
 * Thread entry point.
 *
 * This just sits and waits for a new connection.  It hands it off to the
 * main thread and then goes back to waiting.
 *
 * There is currently no "polite" way to shut this down.
 */
void* ExternalRuntime::Entry(void)
{
    LocalBiChannel lbic;
    Pipe* reader;
    Pipe* writer;

    reader = writer = NULL;

    if (!lbic.create(ANDROID_PIPE_NAME)) {
        fprintf(stderr, "Sim: failed creating named pipe '%s'\n",
            ANDROID_PIPE_NAME);
        return NULL;
    }

    while (lbic.listen(&reader, &writer)) {
        /*
         * Throw it over the wall.
         */
        wxWindow* pMainFrame = ((MyApp*)wxTheApp)->GetMainFrame();

        UserEventMessage* pUem = new UserEventMessage;
        pUem->CreateExternalRuntime(reader, writer);

        UserEvent uev(0, (void*) pUem);
        pMainFrame->AddPendingEvent(uev);

        reader = writer = NULL;
    }

    printf("Sim: ExternalRuntime thread wants to bail\n");

    return NULL;
}

