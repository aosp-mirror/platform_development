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
#ifndef ANDROID_EMULATOR_CONSOLE_H
#define ANDROID_EMULATOR_CONSOLE_H

#include "iolooper.h"
#include "sockets.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct EmulatorConsole  EmulatorConsole;

/* Create a new EmulatorConsole object to connect asynchronously to
 * a given emulator port. Note that this always succeeds since the
 * connection is asynchronous.
 */
EmulatorConsole* emulatorConsole_new(int port, IoLooper* looper);

/* Call this after an iolooper_poll() or iolooper_wait() to check
 * the status of the console's socket and act upon it.
 *
 * Returns 0 on success, or -1 on error (which indicates disconnection!)
 */
int  emulatorConsole_poll( EmulatorConsole*  console );

/* Send a message to the console asynchronously. Any answer will be
 * ignored. */
void emulatorConsole_send( EmulatorConsole*  console, const char* command );

void emulatorConsole_sendMouseDown( EmulatorConsole* con, int x, int y );
void emulatorConsole_sendMouseMotion( EmulatorConsole* con, int x, int y );
void emulatorConsole_sendMouseUp( EmulatorConsole* con, int x, int y );

void emulatorConsole_sendKey( EmulatorConsole* con, int keycode, int down );

#ifdef __cplusplus
}
#endif

#endif /* ANDROID_EMULATOR_CONSOLE_H */
