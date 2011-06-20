#include "EGLClientIface.h"
#include "HostConnection.h"
#include "GLEncoder.h"
#include "GLES/gl.h"
#include "GLES/glext.h"

//XXX: fix this macro to get the context from fast tls path
#define GET_CONTEXT gl_client_context_t * ctx = HostConnection::get()->glEncoder();

#include "gl_entry.cpp"

//The functions table
#include "gl_ftable.h"

static EGLClient_eglInterface * s_egl = NULL;
static EGLClient_glesInterface * s_gl = NULL;

void * getProcAddress(const char * procname)
{
    // search in GL function table
    for (int i=0; i<gl_num_funcs; i++) {
        if (!strcmp(gl_funcs_by_name[i].name, procname)) {
            return gl_funcs_by_name[i].proc;
        }
    }
    return NULL;
}

void finish()
{
    glFinish();
}

EGLClient_glesInterface * init_emul_gles(EGLClient_eglInterface *eglIface)
{
    s_egl = eglIface;

    if (!s_gl) {
        s_gl = new EGLClient_glesInterface();
        s_gl->getProcAddress = getProcAddress;
        s_gl->finish = finish;
    }

    return s_gl;
}

