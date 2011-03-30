#ifndef _ERROR_LOG_H_
#define _ERROR_LOG_H_

#if (HAVE_ANDROID_OS == 1)
#    include <cutils/log.h>
#    define ERR(...)    LOGE(__VA_ARGS__)
#    define DBG(...)    LOGD(__VA_ARGS__)
#else
#     include <stdio.h>
#    define ERR(...)    fprintf(stderr, __VA_ARGS__)
#    define DBG(...)    fprintf(stderr, __VA_ARGS__)
#endif

#endif
