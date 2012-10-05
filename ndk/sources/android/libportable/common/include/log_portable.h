/*
 * ALOG Levels: F - Fatal, E - Error, W - Warning, I - Info, D - Debug, V - Verbose
 *
 * Using them to work within the Android logcat logging mechanism:
 *
 *    % logcat '*:v'                    [To display Verbose Logging]
 *    % logcat 'fcntl_portable:v'       [To display just this fcntl logging]
 *
 * NOTE: This assumes you only use the portable TAG; which is the default.
 *       For debugging LTP it's been helpful to include the LTP program being tested.
 *
 * Logging routines also support ALOG*_IF() and ASSERT(); For details See:
 *
 *       ${ANDROID_TOP}/system/core/include/cutils/log.h
 * and
 *        http://developer.android.com/tools/debugging/debugging-log.html
 *
 * ALOGV is turned off by release builds: Use the #define below with LOG_NDEBUG=0 to enable.
 *
 * Strace works fine with ALOG out if a large max string size is used via the -s option; Ex:
 *
 *      strace -s 132 ./sigaction01
 *
 *          writev(3, [{"\2", 1},
 *                 {"./sigaction01`signal_portable\0", 30},
 *                      {"sigaction_portable(portable_signum:10:'SIGUSR1_PORTABLE:10', act:0x7fe47a08, oldact:0x0) {\0", 91}], 3) = 122
 *                      {"map_portable_sigset_to_mips(portable_sigset:0x7fe47a0c, mips_sigset:0x7fe479b8) {\0", 82}], 3) = 113
 *                      ...
 */

/*
 * Remove the // below to have debug code visible in logcat output by default.
 * It's Also possible via libportable/Android.mk:
 *    LOCAL_CFLAGS += -DLOG_NDEBUG=0
 */
// # define LOG_NDEBUG 0



// #define EXTENDED_LOGGING
#ifdef EXTENDED_LOGGING
/*
 * Inline function to put the current LTP program and this library into the logcat prefix; Ex:
 *
 *    V/./sigaction01`signal_portable(605): sigaction_portable(portable_signum:10:'SIGUSR1_PORTABLE:10', act:0x7fe47a08, oldact:0x0) {
 *      -----------------------------
 *
 * Disabled by default, enable by removing the // above. Useful when debugging more than one program; Ex: LTP has thousands.
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
    return(my_portable_tag);
}
#define LOG_TAG  portable_tag()
#else /* !EXTENDED_LOGGING */
#define LOG_TAG PORTABLE_TAG
#endif

# include <cutils/log.h>

# define PERROR(str)  { ALOGE("%s: PERROR('%s'): errno:%d:'%s'", __func__, str, errno, strerror(errno)); }

# define ASSERT(cond) ALOG_ASSERT(cond, "assertion failed:(%s), file: %s, line: %d:%s",  \
                                 #cond, __FILE__, __LINE__, __func__);

