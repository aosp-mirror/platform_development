/*
 * Copyright 2007 The Android Open Source Project
 *
 * Initialize the intercepts.
 */
#include "Common.h"

#define __USE_GNU       /* need RTLD_NEXT */
#include <dlfcn.h>

#include <stdlib.h>
#include <pthread.h>
#include <string.h>
#include <errno.h>
#include <assert.h>
#include <sys/stat.h>
#include <sys/types.h>


/*
 * Global state.
 */
struct WrapSimGlobals gWrapSim;
pthread_once_t gWrapSimInitialized = PTHREAD_ONCE_INIT;

/*
 * Initialize our global state.
 */
static void initGlobals(void)
{
    memset(&gWrapSim, 0xdd, sizeof(gWrapSim));
    gWrapSim.logFd = -1;
    gWrapSim.keyMap = NULL;

    /*
     * Find the original version of functions we override.
     */
    _ws_access = dlsym(RTLD_NEXT, "access");
    _ws_open = dlsym(RTLD_NEXT, "open");
    _ws_open64 = dlsym(RTLD_NEXT, "open64");

    _ws_close = dlsym(RTLD_NEXT, "close");
    _ws_dup = dlsym(RTLD_NEXT, "dup");
    _ws_read = dlsym(RTLD_NEXT, "read");
    _ws_readv = dlsym(RTLD_NEXT, "readv");
    _ws_write = dlsym(RTLD_NEXT, "write");
    _ws_writev = dlsym(RTLD_NEXT, "writev");
    _ws_mmap = dlsym(RTLD_NEXT, "mmap");
    _ws_mmap64 = dlsym(RTLD_NEXT, "mmap64");
    _ws_ioctl = dlsym(RTLD_NEXT, "ioctl");

    _ws_chdir = dlsym(RTLD_NEXT, "chdir");
    _ws_chmod = dlsym(RTLD_NEXT, "chmod");
    _ws_chown = dlsym(RTLD_NEXT, "chown");
    _ws_creat = dlsym(RTLD_NEXT, "creat");
    _ws_execve = dlsym(RTLD_NEXT, "execve");
    _ws_getcwd = dlsym(RTLD_NEXT, "getcwd");
    _ws_lchown = dlsym(RTLD_NEXT, "lchown");
    _ws_link = dlsym(RTLD_NEXT, "link");
    _ws_lstat = dlsym(RTLD_NEXT, "lstat");
    _ws_lstat64 = dlsym(RTLD_NEXT, "lstat64");
    _ws___lxstat = dlsym(RTLD_NEXT, "__lxstat");
    _ws___lxstat64 = dlsym(RTLD_NEXT, "__lxstat64");
    _ws_mkdir = dlsym(RTLD_NEXT, "mkdir");
    _ws_readlink = dlsym(RTLD_NEXT, "readlink");
    _ws_rename = dlsym(RTLD_NEXT, "rename");
    _ws_rmdir = dlsym(RTLD_NEXT, "rmdir");
    _ws_stat = dlsym(RTLD_NEXT, "stat");
    _ws_stat64 = dlsym(RTLD_NEXT, "stat64");
    _ws___xstat = dlsym(RTLD_NEXT, "__xstat");
    _ws___xstat64 = dlsym(RTLD_NEXT, "__xstat64");
    _ws_statfs = dlsym(RTLD_NEXT, "statfs");
    _ws_statfs64 = dlsym(RTLD_NEXT, "statfs64");
    _ws_symlink = dlsym(RTLD_NEXT, "symlink");
    _ws_unlink = dlsym(RTLD_NEXT, "unlink");
    _ws_utime = dlsym(RTLD_NEXT, "utime");
    _ws_utimes = dlsym(RTLD_NEXT, "utimes");

    _ws_execl = dlsym(RTLD_NEXT, "execl");
    _ws_execle = dlsym(RTLD_NEXT, "execle");
    _ws_execlp = dlsym(RTLD_NEXT, "execlp");
    _ws_execv = dlsym(RTLD_NEXT, "execv");
    _ws_execvp = dlsym(RTLD_NEXT, "execvp");
    _ws_fopen = dlsym(RTLD_NEXT, "fopen");
    _ws_fopen64 = dlsym(RTLD_NEXT, "fopen64");
    _ws_freopen = dlsym(RTLD_NEXT, "freopen");
    _ws_ftw = dlsym(RTLD_NEXT, "ftw");
    _ws_opendir = dlsym(RTLD_NEXT, "opendir");
    _ws_dlopen = dlsym(RTLD_NEXT, "dlopen");

    _ws_setpriority = dlsym(RTLD_NEXT, "setpriority");
    //_ws_pipe = dlsym(RTLD_NEXT, "pipe");

    const char* logFileName = getenv("WRAPSIM_LOG");
    if (logFileName != NULL ){
        gWrapSim.logFd = _ws_open(logFileName, O_WRONLY|O_APPEND|O_CREAT, 0664);
    }

    /* log messages now work; say hello */
    wsLog("--- initializing sim wrapper ---\n");

    gWrapSim.simulatorFd = -1;

    pthread_mutex_init(&gWrapSim.startLock, NULL);
    pthread_cond_init(&gWrapSim.startCond, NULL);
    gWrapSim.startReady = 0;

    pthread_mutex_init(&gWrapSim.fakeFdLock, NULL);
    gWrapSim.fakeFdMap = wsAllocBitVector(kMaxFakeFdCount, 0);
    memset(gWrapSim.fakeFdList, 0, sizeof(gWrapSim.fakeFdList));

    pthread_mutex_init(&gWrapSim.atomicLock, NULL);

    gWrapSim.numDisplays = 0;

    gWrapSim.keyInputDevice = NULL;

    /*
     * Get target for remapped "/system" and "/data".
     *
     * The ANDROID_PRODUCT_OUT env var *must* be set for rewriting to work.
     */
    const char* outEnv = getenv("ANDROID_PRODUCT_OUT");
    if (outEnv == NULL) {
        gWrapSim.remapBaseDir = NULL;
        wsLog("--- $ANDROID_PRODUCT_OUT not set, "
                "filename remapping disabled\n");
    } else {
        /* grab string and append '/' -- note this never gets freed */
        gWrapSim.remapBaseDirLen = strlen(outEnv);
        gWrapSim.remapBaseDir = strdup(outEnv);
        wsLog("--- name remap to %s\n", gWrapSim.remapBaseDir);
    }

    gWrapSim.initialized = 1;
}

/*
 * Creates a directory, or prints a log message if it fails.
 */
static int createTargetDirectory(const char *path, mode_t mode)
{
    int ret;

    ret = mkdir(path, mode);
    if (ret == 0 || errno == EEXIST) {
        return 0;
    }
    wsLog("--- could not create target directory %s: %s\n",
            path, strerror(errno));
    return ret;
}

/*
 * Any setup that would normally be done by init(8).
 * Note that since the syscall redirects have been installed
 * at this point, we are effectively operating within the
 * simulation context.
 */
static void initGeneral(void)
{
    wsLog("--- preparing system\n");

    /* Try to make sure that certain directories exist.
     * If we fail to create them, the errors will show up in the log,
     * but we keep going.
     */
    createTargetDirectory("/data", 0777);
    createTargetDirectory("/data/dalvik-cache", 0777);
}

/*
 * Initialize all necessary state, and indicate that we're ready to go.
 */
static void initOnce(void)
{
    initGlobals();
    initGeneral();
}

/*
 * Shared object initializer.  glibc guarantees that this function is
 * called before dlopen() returns.  It may be called multiple times.
 */
__attribute__((constructor))
static void initialize(void)
{
    pthread_once(&gWrapSimInitialized, initOnce);
}


