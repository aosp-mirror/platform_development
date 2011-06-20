#include "EGLClientIface.h"
#include "HostConnection.h"
#include "GLEncoder.h"


//XXX: fix this macro to get the context from fast tls path
#define GET_CONTEXT gl_client_context_t * ctx = HostConnection::get()->glEncoder();

#include "gl_entry.cpp"

static EGLClient_eglInterface * s_egl = NULL;
static EGLClient_glesInterface * s_gl = NULL;

void finish()
{

}

EGLClient_glesInterface * init_emul_gles(EGLClient_eglInterface *eglIface)
{
    s_egl = eglIface;

    if (!s_gl) {
        s_gl = new EGLClient_glesInterface();
        s_gl->getProcAddress = NULL; //TODO: what goes here?
        s_gl->finish = finish;
    }

    return s_gl;
}

