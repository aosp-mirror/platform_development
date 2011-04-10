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
#undef HAVE_MALLOC_H
#include <SDL.h>
#include <SDL_syswm.h>
#include <stdio.h>
#include <string.h>
#include "libOpenglRender/render_api.h"

#ifdef _WIN32
int WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd)
#else
int main(int argc, char *argv[])
#endif
{
    int portNum = 4141;
    int winWidth = 320;
    int winHeight = 480;
    FBNativeWindowType windowId = NULL;

    //
    // Inialize SDL window
    //
    if (SDL_Init(SDL_INIT_NOPARACHUTE | SDL_INIT_VIDEO)) {
        fprintf(stderr,"SDL init failed: %s\n", SDL_GetError());
        return -1;
    }

    SDL_Surface *surface = SDL_SetVideoMode(winWidth, winHeight, 32, SDL_SWSURFACE);
    if (surface == NULL) {
        fprintf(stderr,"Failed to set video mode: %s\n", SDL_GetError());
        return -1;
    }

    SDL_SysWMinfo  wminfo;
    memset(&wminfo, 0, sizeof(wminfo));
    SDL_GetWMInfo(&wminfo);
#ifdef _WIN32
    windowId = wminfo.window;
#else
    windowId = wminfo.info.x11.window;
#endif

    printf("initializing renderer process\n");

    //
    // initialize OpenGL renderer to render in our window
    //
    bool inited = initOpenGLRenderer(windowId, 0, 0,
                                     winWidth, winHeight, portNum);
    if (!inited) {
        return -1;
    }
    printf("renderer process started\n");

    // Just wait until the window is closed
    SDL_Event ev;
    while( SDL_WaitEvent(&ev) ) {
        if (ev.type == SDL_QUIT) {
            break;
        }
    }

    //
    // stop the renderer
    //
    printf("stopping the renderer process\n");
    stopOpenGLRenderer();

    return 0;
}
