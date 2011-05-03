#include <stdlib.h>
#include "ApiInitializer.h"
#include <dlfcn.h>
#include "gl_wrapper_context.h"

extern "C" {
    gl_wrapper_context_t *createFromLib(void *solib, gl_wrapper_context_t *(*accessor)());
}

gl_wrapper_context_t * createFromLib(void *solib, gl_wrapper_context_t *(accessor)())
{
    gl_wrapper_context_t *ctx = new gl_wrapper_context_t;
    if (ctx == NULL) {
        return NULL;
    }
    ApiInitializer *initializer = new ApiInitializer(solib);
    ctx->initDispatchByName(ApiInitializer::s_getProc, initializer);
    gl_wrapper_context_t::setContextAccessor(accessor);
    delete initializer;
    return ctx;
}


