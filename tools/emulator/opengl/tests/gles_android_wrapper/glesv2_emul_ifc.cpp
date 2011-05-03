#include <stdlib.h>
#include "ApiInitializer.h"
#include <dlfcn.h>
#include "gl2_wrapper_context.h"

extern "C" {
    gl2_wrapper_context_t *createFromLib(void *solib, gl2_wrapper_context_t *(*accessor)());
}

gl2_wrapper_context_t * createFromLib(void *solib, gl2_wrapper_context_t *(*accessor)())
{
    gl2_wrapper_context_t *ctx = new gl2_wrapper_context_t;
    if (ctx == NULL) {
        return NULL;
    }
    ApiInitializer *initializer = new ApiInitializer(solib);
    ctx->initDispatchByName(ApiInitializer::s_getProc, initializer);
    gl2_wrapper_context_t::setContextAccessor(accessor);
    delete initializer;
    return ctx;
}



