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
#include "eglDisplay.h"
#include "HostConnection.h"
#include <dlfcn.h>

static const int systemEGLVersionMajor = 1;
static const int systemEGLVersionMinor = 4;
static const char *systemEGLVendor = "Google Android emulator";

// list of extensions supported by this EGL implementation
//  NOTE that each extension name should be suffixed with space
static const char *systemStaticEGLExtensions =
            "EGL_ANDROID_image_native_buffer ";

// list of extensions supported by this EGL implementation only if supported
// on the host implementation.
//  NOTE that each extension name should be suffixed with space
static const char *systemDynamicEGLExtensions =
            "EGL_KHR_image_base "
            "EGL_KHR_gl_texture_2d_image ";


static void *s_gles_lib = NULL;
static void *s_gles2_lib = NULL;

// The following function will be called when we (libEGL)
// gets unloaded
// At this point we want to unload the gles libraries we
// might have loaded during initialization
static void __attribute__ ((destructor)) do_on_unload(void)
{
    if (s_gles_lib) {
        dlclose(s_gles_lib);
    }

    if (s_gles2_lib) {
        dlclose(s_gles2_lib);
    }
}

eglDisplay::eglDisplay() :
    m_initialized(false),
    m_major(0),
    m_minor(0),
    m_hostRendererVersion(0),
    m_numConfigs(0),
    m_numConfigAttribs(0),
    m_attribs(DefaultKeyedVector<EGLint, EGLint>(ATTRIBUTE_NONE)),
    m_configs(NULL),
    m_gles_iface(NULL),
    m_gles2_iface(NULL),
    m_versionString(NULL),
    m_vendorString(NULL),
    m_extensionString(NULL)
{
    pthread_mutex_init(&m_lock, NULL);
}

bool eglDisplay::initialize(EGLClient_eglInterface *eglIface)
{
    pthread_mutex_lock(&m_lock);
    if (!m_initialized) {

        //
        // load GLES client API
        //
        m_gles_iface = loadGLESClientAPI("/system/lib/egl/libGLESv1_CM_emulation.so",
                                         eglIface,
                                         &s_gles_lib);
        if (!m_gles_iface) {
            pthread_mutex_unlock(&m_lock);
            return false;
        }

#ifdef WITH_GLES2
        m_gles2_iface = loadGLESClientAPI("/system/lib/egl/libGLESv2_emulation.so",
                                          eglIface,
                                          &s_gles2_lib);
        // Note that if loading gles2 failed, we can still run with no
        // GLES2 support, having GLES2 is not mandatory.
#endif

        //
        // establish connection with the host
        //
        HostConnection *hcon = HostConnection::get();
        if (!hcon) {
            pthread_mutex_unlock(&m_lock);
            return false;
        }

        //
        // get renderControl encoder instance
        //
        renderControl_encoder_context_t *rcEnc = hcon->rcEncoder();
        if (!rcEnc) {
            pthread_mutex_unlock(&m_lock);
            return false;
        }

        //
        // Query host reneder and EGL version
        //
        m_hostRendererVersion = rcEnc->rcGetRendererVersion(rcEnc);
        EGLint status = rcEnc->rcGetEGLVersion(rcEnc, &m_major, &m_minor);
        if (status != EGL_TRUE) {
            // host EGL initialization failed !!
            pthread_mutex_unlock(&m_lock);
            return false;
        }

        //
        // Take minimum version beween what we support and what the host support
        //
        if (m_major > systemEGLVersionMajor) {
            m_major = systemEGLVersionMajor;
            m_minor = systemEGLVersionMinor;
        }
        else if (m_major == systemEGLVersionMajor &&
                 m_minor > systemEGLVersionMinor) {
            m_minor = systemEGLVersionMinor;
        }

        //
        // Query the host for the set of configs
        //
        m_numConfigs = rcEnc->rcGetNumConfigs(rcEnc, (uint32_t*)&m_numConfigAttribs);
        if (m_numConfigs <= 0 || m_numConfigAttribs <= 0) {
            // just sanity check - should never happen
            pthread_mutex_unlock(&m_lock);
            return false;
        }

        uint32_t nInts = m_numConfigAttribs * (m_numConfigs + 1);
        EGLint tmp_buf[nInts];
        m_configs = new EGLint[nInts-1];
        if (!m_configs) {
            pthread_mutex_unlock(&m_lock);
            return false;
        }

        //EGLint n = rcEnc->rcGetConfigs(rcEnc, nInts*sizeof(EGLint), m_configs);
        EGLint n = rcEnc->rcGetConfigs(rcEnc, nInts*sizeof(EGLint), (GLuint*)tmp_buf);
        if (n != m_numConfigs) {
            pthread_mutex_unlock(&m_lock);
            return false;
        }

        //Fill the attributes vector.
        //The first m_numConfigAttribs values of tmp_buf are the actual attributes enums.
        for (int i=0; i<m_numConfigAttribs; i++)
        {
            m_attribs.add(tmp_buf[i], i);
        }

        //Copy the actual configs data to m_configs
        memcpy(m_configs, tmp_buf + m_numConfigAttribs*sizeof(EGLint), m_numConfigs*sizeof(EGLint));

        m_initialized = true;
    }
    pthread_mutex_unlock(&m_lock);

    return true;
}

void eglDisplay::terminate()
{
    pthread_mutex_lock(&m_lock);
    if (m_initialized) {
        m_initialized = false;
        delete [] m_configs;
        m_configs = NULL;

        if (m_versionString) {
            free(m_versionString);
            m_versionString = NULL;
        }
        if (m_vendorString) {
            free(m_vendorString);
            m_vendorString = NULL;
        }
        if (m_extensionString) {
            free(m_extensionString);
            m_extensionString = NULL;
        }
    }
    pthread_mutex_unlock(&m_lock);
}

EGLClient_glesInterface *eglDisplay::loadGLESClientAPI(const char *libName,
                                                       EGLClient_eglInterface *eglIface,
                                                       void **libHandle)
{
    void *lib = dlopen(libName, RTLD_NOW);
    if (!lib) {
        return NULL;
    }

    init_emul_gles_t init_gles_func = (init_emul_gles_t)dlsym(lib,"init_emul_gles");
    if (!init_gles_func) {
        dlclose((void*)libName);
        return NULL;
    }

    *libHandle = lib;
    return (*init_gles_func)(eglIface);
}

static char *queryHostEGLString(EGLint name)
{
    HostConnection *hcon = HostConnection::get();
    if (hcon) {
        renderControl_encoder_context_t *rcEnc = hcon->rcEncoder();
        if (rcEnc) {
            int n = rcEnc->rcQueryEGLString(rcEnc, name, NULL, 0);
            if (n < 0) {
                // allocate space for the string with additional
                // space charachter to be suffixed at the end.
                char *str = (char *)malloc(-n+2);
                n = rcEnc->rcQueryEGLString(rcEnc, name, str, -n);
                if (n > 0) {
                    // add extra space at end of string which will be
                    // needed later when filtering the extension list.
                    strcat(str, " ");
                    return str;
                }

                free(str);
            }
        }
    }

    return NULL;
}

static bool findExtInList(const char* token, int tokenlen, const char* list)
{
    const char* p = list;
    while (*p != '\0') {
        const char* q = strchr(p, ' ');
        if (q == NULL) {
            /* should not happen, list must be space-terminated */
            break;
        }
        if (tokenlen == (q - p) && !memcmp(token, p, tokenlen)) {
            return true;  /* found it */
        }
        p = q+1;
    }
    return false;  /* not found */
}

static char *buildExtensionString()
{
    //Query host extension string
    char *hostExt = queryHostEGLString(EGL_EXTENSIONS);
    if (!hostExt || (hostExt[1] == '\0')) {
        // no extensions on host - only static extension list supported
        return strdup(systemStaticEGLExtensions);
    }

    //
    // Filter host extension list to include only extensions
    // we can support (in the systemDynamicEGLExtensions list)
    //
    char *ext = (char *)hostExt;
    char *c = ext;
    char *insert = ext;
    while(*c != '\0') {
        if (*c == ' ') {
            int len = c - ext;
            if (findExtInList(ext, len, systemDynamicEGLExtensions)) {
                if (ext != insert) {
                    memcpy(insert, ext, len+1); // including space
                }
                insert += (len + 1);
            }
            ext = c + 1;
        }
        c++;
    }
    *insert = '\0';

    int n = strlen(hostExt);
    if (n > 0) {
        char *str;
        asprintf(&str,"%s%s", systemStaticEGLExtensions, hostExt);
        free((char*)hostExt);
        return str;
    }
    else {
        free((char*)hostExt);
        return strdup(systemStaticEGLExtensions);
    }
}

const char *eglDisplay::queryString(EGLint name)
{
    if (name == EGL_CLIENT_APIS) {
        return "OpenGL_ES";
    }
    else if (name == EGL_VERSION) {
        pthread_mutex_lock(&m_lock);
        if (m_versionString) {
            pthread_mutex_unlock(&m_lock);
            return m_versionString;
        }

        // build version string
        asprintf(&m_versionString, "%d.%d", m_major, m_minor);
        pthread_mutex_unlock(&m_lock);

        return m_versionString;
    }
    else if (name == EGL_VENDOR) {
        pthread_mutex_lock(&m_lock);
        if (m_vendorString) {
            pthread_mutex_unlock(&m_lock);
            return m_vendorString;
        }

        // build vendor string
        const char *hostVendor = queryHostEGLString(EGL_VENDOR);

        if (hostVendor) {
            asprintf(&m_vendorString, "%s Host: %s",
                                     systemEGLVendor, hostVendor);
            free((char*)hostVendor);
        }
        else {
            m_vendorString = (char *)systemEGLVendor;
        }
        pthread_mutex_unlock(&m_lock);

        return m_vendorString;
    }
    else if (name == EGL_EXTENSIONS) {
        pthread_mutex_lock(&m_lock);
        if (m_extensionString) {
            pthread_mutex_unlock(&m_lock);
            return m_extensionString;
        }

        // build extension string
        m_extensionString = buildExtensionString();
        pthread_mutex_unlock(&m_lock);

        return m_extensionString;
    }
    else {
        LOGE("[%s] Unknown name %d\n", __FUNCTION__, name);
        return NULL;
    }
}

EGLBoolean eglDisplay::getAttribValue(EGLConfig config, EGLint attribIdx, EGLint * value)
{
    if (attribIdx == ATTRIBUTE_NONE)
    {
        LOGE("[%s] Bad attribute idx\n", __FUNCTION__);
        return EGL_FALSE;
    }
    *value = *(m_configs + (int)config*m_numConfigAttribs + attribIdx);
    return EGL_TRUE;
}

EGLBoolean eglDisplay::getConfigAttrib(EGLConfig config, EGLint attrib, EGLint * value)
{
    return getAttribValue(config, m_attribs.valueFor(attrib), value);
}

EGLBoolean eglDisplay::getConfigPixelFormat(EGLConfig config, GLenum * format)
{
    EGLint redSize, blueSize, greenSize, alphaSize;

    if ( !(getAttribValue(config, m_attribs.valueFor(EGL_RED_SIZE), &redSize) &&
        getAttribValue(config, m_attribs.valueFor(EGL_BLUE_SIZE), &blueSize) &&
        getAttribValue(config, m_attribs.valueFor(EGL_GREEN_SIZE), &greenSize) &&
        getAttribValue(config, m_attribs.valueFor(EGL_ALPHA_SIZE), &alphaSize)) )
    {
        LOGE("Couldn't find value for one of the pixel format attributes");
        return EGL_FALSE;
    }

    //calculate the GL internal format
    if ((redSize==8)&&(blueSize==8)&&(greenSize==8)&&(alphaSize==8)) *format = GL_RGBA;
    else if ((redSize==8)&&(blueSize==8)&&(greenSize==8)&&(alphaSize==0)) *format = GL_RGB;
    else if ((redSize==5)&&(blueSize==6)&&(greenSize==5)&&(alphaSize==0)) *format = GL_RGB565_OES;
    else if ((redSize==5)&&(blueSize==5)&&(greenSize==5)&&(alphaSize==1)) *format = GL_RGB5_A1_OES;
    else if ((redSize==4)&&(blueSize==4)&&(greenSize==4)&&(alphaSize==4)) *format = GL_RGBA4_OES;
    else return EGL_FALSE;

    return EGL_TRUE;
}
