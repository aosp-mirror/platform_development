#ifndef __GL_ERROR_LOG_H__
#define __GL_ERROR_LOG_H__

#include "ErrorLog.h"

#ifdef CHECK_GL_ERROR
void dbg(){}
#define GET_GL_ERROR(gl)  \
    {   \
        int err = gl.glGetError();    \
        if (err) { dbg(); ERR("Error: 0x%X in %s (%s:%d)\n", err, __FUNCTION__, __FILE__, __LINE__); }  \
    }

#else
#define GET_GL_ERROR(gl)
#endif

#endif //__GL_ERROR_LOG_H__
