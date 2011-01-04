/*
 IMPORTANT:  This Apple software is supplied to you by Apple Computer, Inc. ("Apple") in
 consideration of your agreement to the following terms, and your use, installation,
 modification or redistribution of this Apple software constitutes acceptance of these
 terms.  If you do not agree with these terms, please do not use, install, modify or
 redistribute this Apple software.

 In consideration of your agreement to abide by the following terms, and subject to these
 terms, Apple grants you a personal, non-exclusive license, under AppleÕs copyrights in
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

#include <stdlib.h>
#include "main.h"
#include "PluginObject.h"

int SubPlugin::getPluginWidth() {
    PluginObject *obj = (PluginObject*) inst()->pdata;
    return obj->window->width;
}

int SubPlugin::getPluginHeight() {
    PluginObject *obj = (PluginObject*) inst()->pdata;
    return obj->window->height;
}

SurfaceSubPlugin::~SurfaceSubPlugin() {
    setContext(NULL);
}

bool SurfaceSubPlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kSurface_ANPDrawingModel);
}

void SurfaceSubPlugin::setContext(jobject context) {
    JNIEnv* env = NULL;
    if (gVM->GetEnv((void**) &env, JNI_VERSION_1_4) == JNI_OK) {

        // if one exists then free its global reference
        if (m_context) {
            env->DeleteGlobalRef(m_context);
            m_context = NULL;
        }

        // create a new global ref
        if (context) {
            context = env->NewGlobalRef(context);
        }

        // set the value
        m_context = context;
    }
}

static void pluginInvalidate(NPObject *obj);
static bool pluginHasProperty(NPObject *obj, NPIdentifier name);
static bool pluginHasMethod(NPObject *obj, NPIdentifier name);
static bool pluginGetProperty(NPObject *obj, NPIdentifier name, NPVariant *variant);
static bool pluginSetProperty(NPObject *obj, NPIdentifier name, const NPVariant *variant);
static bool pluginInvoke(NPObject *obj, NPIdentifier name, const NPVariant *args, uint32_t argCount, NPVariant *result);
static bool pluginInvokeDefault(NPObject *obj, const NPVariant *args, uint32_t argCount, NPVariant *result);
static NPObject *pluginAllocate(NPP npp, NPClass *theClass);
static void pluginDeallocate(NPObject *obj);
static bool pluginRemoveProperty(NPObject *npobj, NPIdentifier name);
static bool pluginEnumerate(NPObject *npobj, NPIdentifier **value, uint32_t *count);



static NPClass pluginClass = {
    NP_CLASS_STRUCT_VERSION,
    pluginAllocate,
    pluginDeallocate,
    pluginInvalidate,
    pluginHasMethod,
    pluginInvoke,
    pluginInvokeDefault,
    pluginHasProperty,
    pluginGetProperty,
    pluginSetProperty,
    pluginRemoveProperty,
    pluginEnumerate
};

NPClass *getPluginClass(void)
{
    return &pluginClass;
}

static bool identifiersInitialized = false;

#define ID_TESTFILE_PROPERTY            0
#define NUM_PROPERTY_IDENTIFIERS        1

static NPIdentifier pluginPropertyIdentifiers[NUM_PROPERTY_IDENTIFIERS];
static const NPUTF8 *pluginPropertyIdentifierNames[NUM_PROPERTY_IDENTIFIERS] = {
    "testfile"
};

#define ID_GETTESTFILE_METHOD                   0
#define NUM_METHOD_IDENTIFIERS                  1

static NPIdentifier pluginMethodIdentifiers[NUM_METHOD_IDENTIFIERS];
static const NPUTF8 *pluginMethodIdentifierNames[NUM_METHOD_IDENTIFIERS] = {
    "getTestFile"
};

static void initializeIdentifiers(void)
{
    browser->getstringidentifiers(pluginPropertyIdentifierNames, NUM_PROPERTY_IDENTIFIERS, pluginPropertyIdentifiers);
    browser->getstringidentifiers(pluginMethodIdentifierNames, NUM_METHOD_IDENTIFIERS, pluginMethodIdentifiers);
}

static bool pluginHasProperty(NPObject *obj, NPIdentifier name)
{
    int i;
    for (i = 0; i < NUM_PROPERTY_IDENTIFIERS; i++)
        if (name == pluginPropertyIdentifiers[i])
            return true;
    return false;
}

static bool pluginHasMethod(NPObject *obj, NPIdentifier name)
{
    int i;
    for (i = 0; i < NUM_METHOD_IDENTIFIERS; i++)
        if (name == pluginMethodIdentifiers[i])
            return true;
    return false;
}

static bool pluginGetProperty(NPObject *obj, NPIdentifier name, NPVariant *variant)
{
    PluginObject *plugin = (PluginObject *)obj;
    if (name == pluginPropertyIdentifiers[ID_TESTFILE_PROPERTY]) {
        BOOLEAN_TO_NPVARIANT(true, *variant);
        return true;
    }
    return false;
}

static bool pluginSetProperty(NPObject *obj, NPIdentifier name, const NPVariant *variant)
{
    return false;
}

static bool pluginInvoke(NPObject *obj, NPIdentifier name, const NPVariant *args, uint32_t argCount, NPVariant *result)
{
    PluginObject *plugin = (PluginObject *)obj;
    if (name == pluginMethodIdentifiers[ID_GETTESTFILE_METHOD]) {
        return true;
    }
    return false;
}

static bool pluginInvokeDefault(NPObject *obj, const NPVariant *args, uint32_t argCount, NPVariant *result)
{
    return false;
}

static void pluginInvalidate(NPObject *obj)
{
    // Release any remaining references to JavaScript objects.
}

static NPObject *pluginAllocate(NPP npp, NPClass *theClass)
{
    PluginObject *newInstance = (PluginObject*) malloc(sizeof(PluginObject));
    newInstance->header._class = theClass;
    newInstance->header.referenceCount = 1;

    if (!identifiersInitialized) {
        identifiersInitialized = true;
        initializeIdentifiers();
    }

    newInstance->npp = npp;

    return &newInstance->header;
}

static void pluginDeallocate(NPObject *obj)
{
    free(obj);
}

static bool pluginRemoveProperty(NPObject *npobj, NPIdentifier name)
{
    return false;
}

static bool pluginEnumerate(NPObject *npobj, NPIdentifier **value, uint32_t *count)
{
    return false;
}
