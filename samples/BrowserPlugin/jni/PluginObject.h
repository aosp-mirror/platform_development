/*
 IMPORTANT:  This Apple software is supplied to you by Apple Computer, Inc. ("Apple") in
 consideration of your agreement to the following terms, and your use, installation,
 modification or redistribution of this Apple software constitutes acceptance of these
 terms.  If you do not agree with these terms, please do not use, install, modify or
 redistribute this Apple software.

 In consideration of your agreement to abide by the following terms, and subject to these
 terms, Apple grants you a personal, non-exclusive license, under Apple�s copyrights in
 this original Apple software (the "Apple Software"), to use, reproduce, modify and
 redistribute the Apple Software, with or without modifications, in source and/or binary
 forms; provided that if you redistribute the Apple Software in its entirety and without
 modifications, you must retain this notice and the following text and disclaimers in all
 such redistributions of the Apple Software.  Neither the name, trademarks, service marks
 or logos of Apple Computer, Inc. may be used to endorse or promote products derived from
 the Apple Software without specific prior written permission from Apple. Except as expressly
 stated in this notice, no other rights or licenses, express or implied, are granted by Apple
 herein, including but not limited to any patent rights that may be infringed by your
 derivative works or by other works in which the Apple Software may be incorporated.

 The Apple Software is provided by Apple on an "AS IS" basis.  APPLE MAKES NO WARRANTIES,
 EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION THE IMPLIED WARRANTIES OF NON-INFRINGEMENT,
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS
 USE AND OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.

 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
          OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE,
 REPRODUCTION, MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED AND
 WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE), STRICT LIABILITY OR
 OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef PluginObject__DEFINED
#define PluginObject__DEFINED

#include "main.h"
#include <jni.h>

enum CustomEventTypes {
    kSurfaceCreated_CustomEvent     = 0,
    kSurfaceChanged_CustomEvent     = 1,
    kSurfaceDestroyed_CustomEvent   = 2,
};
typedef int32_t CustomEventType;

class SubPlugin {
public:
    SubPlugin(NPP inst) : m_inst(inst) {}
    virtual ~SubPlugin() {}
    virtual int16_t handleEvent(const ANPEvent* evt) = 0;
    virtual bool supportsDrawingModel(ANPDrawingModel) = 0;

    int getPluginWidth();
    int getPluginHeight();

    NPP inst() const { return m_inst; }

private:
    NPP m_inst;
};

class SurfaceSubPlugin : public SubPlugin {
public:
    SurfaceSubPlugin(NPP inst) : SubPlugin(inst) { m_context = NULL; }
    virtual ~SurfaceSubPlugin();
    virtual jobject getSurface() = 0;
    virtual bool supportsDrawingModel(ANPDrawingModel);

    void setContext(jobject context);

    jobject m_context;
};

enum PluginTypes {
    kAnimation_PluginType  = 1,
    kAudio_PluginType      = 2,
    kBackground_PluginType = 3,
    kForm_PluginType       = 4,
    kText_PluginType       = 5,
    kPaint_PluginType      = 6,
    kVideo_PluginType      = 7,
    kNavigation_PluginType = 8,
};
typedef uint32_t PluginType;

typedef struct PluginObject {
    NPObject header;
    NPP npp;
    NPWindow* window;

    PluginType pluginType;
    SubPlugin* activePlugin;

} PluginObject;

NPClass *getPluginClass(void);

#endif // PluginObject__DEFINED
