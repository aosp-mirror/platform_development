/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "EventInjector.h"
#include "emulator-console.h"

#define PRIVATE  EventInjectorPrivate

class PRIVATE
{
public:
    IoLooper*         mLooper;
    EmulatorConsole*  mConsole;

    EventInjectorPrivate(int port) {
        mLooper = iolooper_new();
        mConsole = emulatorConsole_new(port, mLooper);
    }
};

EventInjector::EventInjector(int consolePort)
{
    mPrivate = new PRIVATE(consolePort);
}

EventInjector::~EventInjector()
{
    delete mPrivate;
}

void EventInjector::wait(int timeout_ms)
{
    iolooper_wait(mPrivate->mLooper, timeout_ms);
}

void EventInjector::poll(void)
{
    emulatorConsole_poll(mPrivate->mConsole);
}

void EventInjector::sendMouseDown( int x, int y )
{
    emulatorConsole_sendMouseDown(mPrivate->mConsole, x, y);
}

void EventInjector::sendMouseUp( int x, int y )
{
    emulatorConsole_sendMouseUp(mPrivate->mConsole, x, y);
}

void EventInjector::sendMouseMotion( int x, int y )
{
    emulatorConsole_sendMouseMotion(mPrivate->mConsole, x, y);
}

void EventInjector::sendKeyDown( int keycode )
{
    emulatorConsole_sendKey(mPrivate->mConsole, keycode, 1);
}

void EventInjector::sendKeyUp( int keycode )
{
    emulatorConsole_sendKey(mPrivate->mConsole, keycode, 0);
}
