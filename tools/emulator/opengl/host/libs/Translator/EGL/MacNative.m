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
#include <stdio.h>
#include <Cocoa/Cocoa.h>
#include <OpenGL/OpenGL.h>
#include "MacPixelFormatsAttribs.h"


int getNumPixelFormats(){
    int size;
    NSOpenGLPixelFormatAttribute** attrib_lists = getPixelFormatsAttributes(&size);
    return size;
}

void* getPixelFormat(int i){
    int size;
    NSOpenGLPixelFormatAttribute** attrib_lists = getPixelFormatsAttributes(&size);
    return [[NSOpenGLPixelFormat alloc] initWithAttributes:attrib_lists[i]];
}

void getPixelFormatAttrib(void* pixelFormat,int attrib,int* val){
    NSOpenGLPixelFormat *frmt = (NSOpenGLPixelFormat *)pixelFormat;
    [frmt getValues:val forAttribute:attrib forVirtualScreen:0]; 
}

void* nsCreateContext(void* format,void* share){
    NSOpenGLPixelFormat* frmt = (NSOpenGLPixelFormat*)format;
    return [[NSOpenGLContext alloc] initWithFormat:frmt shareContext:share];
}

void  nsPBufferMakeCurrent(void* context,void* nativePBuffer,int level){
    NSOpenGLContext* ctx = (NSOpenGLContext *)context;
    NSOpenGLPixelBuffer* pbuff = (NSOpenGLPixelBuffer *)nativePBuffer;
    if(ctx == nil){
        [NSOpenGLContext clearCurrentContext];
    } else {
        if(pbuff != nil){
            [ctx clearDrawable];
            [ctx setPixelBuffer:pbuff cubeMapFace:0 mipMapLevel:level currentVirtualScreen:0];
            [ctx makeCurrentContext];
        }
    }
}

void nsWindowMakeCurrent(void* context,void* nativeWin){
    NSOpenGLContext* ctx = (NSOpenGLContext *)context;
    NSView* win = (NSView *)nativeWin;
    if(ctx == nil){
        [NSOpenGLContext clearCurrentContext];
    } else {
        if(win != nil){
            [ctx clearDrawable];
            [ctx setView: win];
            [ctx makeCurrentContext];
        }
    }
}

void nsSwapBuffers(){
    NSOpenGLContext* ctx = [NSOpenGLContext currentContext];
    if(ctx != nil){
        [ctx flushBuffer];
    }
}

void nsSwapInterval(int *interval){
    NSOpenGLContext* ctx = [NSOpenGLContext currentContext];
    if( ctx != nil){
        [ctx setValues:interval forParameter:NSOpenGLCPSwapInterval];
    }
}


void nsDestroyContext(void* context){
    NSOpenGLContext *ctx = (NSOpenGLContext*)context;
    if(ctx != nil){
        [ctx release];
    }
}


void* nsCreatePBuffer(GLenum target,GLenum format,int maxMip,int width,int height){
    return [[NSOpenGLPixelBuffer alloc] initWithTextureTarget:target 
                                        textureInternalFormat:format 
                                        textureMaxMipMapLevel:maxMip 
                                        pixelsWide:width pixelsHigh:height];
    
}

void nsDestroyPBuffer(void* pbuffer){
    NSOpenGLPixelBuffer *pbuf = (NSOpenGLPixelBuffer*)pbuffer;
    if(pbuf != nil){
        [pbuf release];
    }
}

bool nsGetWinDims(void* win,unsigned int* width,unsigned int* height){
    NSView* view = (NSView*)win;
    if(view != nil){
        NSRect rect = [view bounds];
        *width  = rect.size.width;
        *height = rect.size.height;
        return true;
    }
    return false;
}

bool  nsCheckColor(void* win,int colorSize){
    NSView* view = (NSView*)win;
   if(view != nil){
       NSWindow* wnd = [view window];
       if(wnd != nil){
           NSWindowDepth limit = [wnd depthLimit];
           NSWindowDepth defaultLimit = [NSWindow defaultDepthLimit];

           int depth = (limit != 0) ? NSBitsPerPixelFromDepth(limit):
                                      NSBitsPerPixelFromDepth(defaultLimit);
           return depth >= colorSize;
 
       }
   }
   return false;

}
