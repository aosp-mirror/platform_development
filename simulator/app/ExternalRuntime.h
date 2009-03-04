//
// Copyright 2005 The Android Open Source Project
//
// Class that manages the simulated device.
//
#ifndef _SIM_EXTERNAL_RUNTIME_H
#define _SIM_EXTERNAL_RUNTIME_H

/*
 * Define a thread that listens for the launch of an external runtime.
 * When we spot one we notify the main thread, which can choose to
 * accept or reject it.
 */
class ExternalRuntime : public wxThread {
public:
    ExternalRuntime(void) {}
    virtual ~ExternalRuntime(void);

    /* start the thread running */
    bool StartThread(void);

    /* thread entry point */
    virtual void* Entry(void);
};

#endif // _SIM_EXTERNAL_RUNTIME_H
