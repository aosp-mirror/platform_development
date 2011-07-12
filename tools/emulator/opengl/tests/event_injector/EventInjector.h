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
/* Event redirector is used to inject user events from a GL window
 * into the emulated program.
 */
#ifndef EVENT_INJECTOR_H
#define EVENT_INJECTOR_H

class EventInjectorPrivate;

class EventInjector
{
public:
    EventInjector(int consolePort);
    virtual ~EventInjector();

    void wait( int timeout_ms );
    void poll( void );

    void sendMouseDown( int x, int y );
    void sendMouseUp( int x, int y );
    void sendMouseMotion( int x, int y );
    void sendKeyDown( int keycode );
    void sendKeyUp( int keycode );

    /* Keycode values expected by the Linux kernel, and the emulator */
    enum {
        KEY_BACK = 158,
        KEY_HOME = 102,
        KEY_SOFT1 = 229,
        KEY_LEFT = 105,
        KEY_UP   = 103,
        KEY_DOWN =  108,
        KEY_RIGHT = 106,
        KEY_VOLUMEUP = 115,
        KEY_VOLUMEDOWN = 114,
        KEY_SEND = 231,
        KEY_END = 107,
        KEY_ENTER = 28,
    };

private:
    EventInjectorPrivate* mPrivate;
};

#endif /* EVENT_INJECTOR_H */
