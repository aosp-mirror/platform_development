#ifndef GLES_MACROS_H
#define GLES_MACROS_H

#define GET_THREAD()                                                         \
            ThreadInfo* thrd = NULL;                                         \
            if(s_eglIface) {                                                 \
                thrd = s_eglIface->getThreadInfo();                          \
            } else {                                                         \
                fprintf(stderr,"Context wasn't initialized yet \n");         \
            }


#define GET_CTX()                                                                \
            GET_THREAD();                                                        \
            if(!thrd) return;                                                    \
            GLEScontext *ctx = static_cast<GLEScontext*>(thrd->glesContext);     \
            if(!ctx) return;

#define GET_CTX_CM()                                                             \
            GET_THREAD();                                                        \
            if(!thrd) return;                                                    \
            GLEScmContext *ctx = static_cast<GLEScmContext*>(thrd->glesContext); \
            if(!ctx) return;

#define GET_CTX_V2()                                                             \
            GET_THREAD();                                                        \
            if(!thrd) return;                                                    \
            GLESv2Context *ctx = static_cast<GLESv2Context*>(thrd->glesContext); \
            if(!ctx) return;

#define GET_CTX_RET(failure_ret)                                                 \
            GET_THREAD();                                                        \
            if(!thrd) return failure_ret;                                        \
            GLEScontext *ctx = static_cast<GLEScontext*>(thrd->glesContext);     \
            if(!ctx) return failure_ret;

#define GET_CTX_CM_RET(failure_ret)                                              \
            GET_THREAD();                                                        \
            if(!thrd) return failure_ret;                                        \
            GLEScmContext *ctx = static_cast<GLEScmContext*>(thrd->glesContext); \
            if(!ctx) return failure_ret;

#define GET_CTX_V2_RET(failure_ret)                                              \
            GET_THREAD();                                                        \
            if(!thrd) return failure_ret;                                        \
            GLESv2Context *ctx = static_cast<GLESv2Context*>(thrd->glesContext); \
            if(!ctx) return failure_ret;


#define SET_ERROR_IF(condition,err) if((condition)) {                            \
                        ctx->setGLerror(err);                                    \
                        return;                                                  \
                    }


#define RET_AND_SET_ERROR_IF(condition,err,ret) if((condition)) {                \
                        ctx->setGLerror(err);                                    \
                        return ret;                                              \
                    }

#endif
