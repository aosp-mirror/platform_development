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
#include "RenderServer.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "FrameBuffer.h"

static void printUsage(const char *progName)
{
    fprintf(stderr, "Usage: %s -windowid <windowid> [options]\n", progName);
    fprintf(stderr, "    -windowid <windowid>   - window id to render into\n");
    fprintf(stderr, "    -port <portNum>        - listening TCP port number\n");
    fprintf(stderr, "    -x <num>               - render subwindow x position\n");
    fprintf(stderr, "    -y <num>               - render subwindow y position\n");
    fprintf(stderr, "    -width <num>           - render subwindow width\n");
    fprintf(stderr, "    -height <num>          - render subwindow height\n");
    exit(-1);
}

int main(int argc, char *argv[])
{
    int portNum = 4141;
    int winX = 0;
    int winY = 0;
    int winWidth = 320;
    int winHeight = 480;
    FBNativeWindowType windowId = NULL;
    int iWindowId  = 0;

    //
    // Parse command line arguments
    //
    for (int i=1; i<argc; i++) {
        if (!strcmp(argv[i], "-windowid")) {
            if (++i >= argc || sscanf(argv[i],"%d", &iWindowId) != 1) {
                printUsage(argv[0]);
            }
        }
        else if (!strncmp(argv[i], "-port", 5)) {
            if (++i >= argc || sscanf(argv[i],"%d", &portNum) != 1) {
                printUsage(argv[0]);
            }
        }
        else if (!strncmp(argv[i], "-x", 2)) {
            if (++i >= argc || sscanf(argv[i],"%d", &winX) != 1) {
                printUsage(argv[0]);
            }
        }
        else if (!strncmp(argv[i], "-y", 2)) {
            if (++i >= argc || sscanf(argv[i],"%d", &winY) != 1) {
                printUsage(argv[0]);
            }
            i++;
        }
        else if (!strncmp(argv[i], "-width", 6)) {
            if (++i >= argc || sscanf(argv[i],"%d", &winWidth) != 1) {
                printUsage(argv[0]);
            }
        }
        else if (!strncmp(argv[i], "-height", 7)) {
            if (++i >= argc || sscanf(argv[i],"%d", &winHeight) != 1) {
                printUsage(argv[0]);
            }
        }
    }

    windowId = (FBNativeWindowType)iWindowId;
    if (!windowId) {
        // window id must be provided
        printUsage(argv[0]);
    }

    //
    // initialize Framebuffer
    //
    bool inited = FrameBuffer::initialize(windowId,
                                          winX, winY, winWidth, winHeight);
    if (!inited) {
        fprintf(stderr,"Failed to initialize Framebuffer\n");
        return -1;
    }

    //
    // Create and run a render server listening to the givven port number
    //
    RenderServer *server = RenderServer::create(portNum);
    if (!server) {
        fprintf(stderr,"Cannot initialize render server\n");
        return -1;
    }

    server->Main(); // never returns

    return 0;
}
