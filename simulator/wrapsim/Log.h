/*
 * Copyright 2007 The Android Open Source Project
 *
 * Logging.
 */
#ifndef _WRAPSIM_LOG_H
#define _WRAPSIM_LOG_H


/*
 * Log debug info.
 */
void wsLog(const char* format, ...)
    #if defined(__GNUC__)
        __attribute__ ((format(printf, 1, 2)))
    #endif
    ;

#endif /*_WRAPSIM_LOG_H*/
