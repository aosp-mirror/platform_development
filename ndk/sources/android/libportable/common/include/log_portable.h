/*
 * ALOG Levels: F - Fatal, E - Error, W - Warning, I - Info, D - Debug, V - Verbose
 *
 * Using them to work within the Android logcat logging mechanism:
 *
 *    % logcat '*:v'                    [To display Verbose Logging]
 *    % logcat 'fcntl_portable:v'       [To display just this fcntl logging]
 *
 * NOTE: This assumes you only use the 'PORTABLE_TAG'; which is the default.
 *       For debugging LTP it's been helpful to include the LTP program being tested;
 *       which is enabled below with #define EXTENDED_LOGGING.
 *
 * Logging routines also support ALOG*_IF() and ASSERT(); For details See:
 *
 *       ${ANDROID_TOP}/system/core/include/cutils/log.h
 * and
 *        http://developer.android.com/tools/debugging/debugging-log.html
 *
 * ALOGV is turned off by release builds: Use the #define below with LOG_NDEBUG=0 to enable.
 *
 * Strace works fine with ALOG out if a large max string size is used via the -s option;
 * Example:
 *
 *   strace -s 132 ./sigaction01
 *
 *   writev(3, [{"\2", 1},
 *      {"./sigaction01`signal_portable\0", 30},
 *      {"sigaction_portable(portable_signum:10:'SIGUSR1_PORTABLE:10', ...
 *      {"map_portable_sigset_to_mips(portable_sigset:0x7fe47a0c, ...
 *      ...
 */

 /*
  * Enable LOG_NDEBUG to have debug code visible in logcat output by default.
  * Also possible via the Lib-Portable Android.mk  file. Example:
  *
  *     # Have logging permanently enable during development.
  *     LOCAL_CFLAGS += -DLOG_NDEBUG=0
  */
// # define LOG_NDEBUG 0


// #define EXTENDED_LOGGING        /* Include the current program name in the LOG_TAG */
#ifdef EXTENDED_LOGGING
/*
 * Inline function to put the current program name
 * and this library into the logcat prefix. Example:
 *
 *    V/./sigaction01`signal_portable(605): sigaction_portable(... ) {
 *      -----------------------------
 *
 * Disabled by default in AOSP, enable by removing the // above.
 * Useful when debugging more than one program; For example LTP has thousands.
 */
#define MAX_TAG_LEN 128
static char my_portable_tag[MAX_TAG_LEN + 1];

static inline char *portable_tag() {
    extern char *__progname;

    if (my_portable_tag[0] == '\000') {
        strncat(&my_portable_tag[0], __progname, MAX_TAG_LEN);
        strncat(&my_portable_tag[0], ".", MAX_TAG_LEN - strlen(my_portable_tag));
        strncat(&my_portable_tag[0], PORTABLE_TAG, MAX_TAG_LEN - strlen(my_portable_tag));
    }
    return my_portable_tag;
}
#define LOG_TAG  portable_tag()
#else /* !EXTENDED_LOGGING */
#define LOG_TAG PORTABLE_TAG
#endif

/*
 * Override LOG_PRI() defined in ${AOSP}/system/core/include/cutils/log.h
 * to preserve the value of errno while logging.
 */
#define LOG_PRI(priority, tag, ...) ({                      \
    int _errno = *REAL(__errno)();                          \
    int _rv = android_printLog(priority, tag, __VA_ARGS__); \
    *REAL(__errno)() = _errno;                              \
    _rv;                   /* Returned to caller */         \
})

#if !defined(__HOST__)
#include <cutils/log.h>

# define PERROR(str)  {                                                                  \
    ALOGE("%s: PERROR('%s'): errno:%d:'%s'", __func__, str, *REAL(__errno)(), strerror(errno)); \
}

# define ASSERT(cond) ALOG_ASSERT(cond, "assertion failed:(%s), file: %s, line: %d:%s",  \
                                 #cond, __FILE__, __LINE__, __func__);
#else
#include <assert.h>
# define PERROR(str) fprintf(stderr, "%s: PERROR('%s'): errno:%d:'%s'", __func__, str, *REAL(__errno)(), strerror(*REAL(__errno)()))
# define ASSERT(cond) assert(cond)
# define ALOGV(a,...)
# define ALOGW(a,...)
# define ALOGE(a,...)

#endif