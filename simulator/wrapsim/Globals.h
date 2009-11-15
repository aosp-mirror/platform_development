/*
 * Copyright 2007 The Android Open Source Project
 *
 * Sim wrapper global state.
 */
#ifndef _WRAPSIM_GLOBALS_H
#define _WRAPSIM_GLOBALS_H

#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <sys/time.h>
#include <sys/vfs.h>
#include <utime.h>
#include <fcntl.h>
#include <dirent.h>
#include <pthread.h>

/*
 * Type declarations for the functions we're replacing.
 *
 * For syscalls this matches the syscall definition, not the libc definition,
 * e.g. no varargs for open() or ioctl().
 */
typedef int     (*Func_access)(const char*, int);
typedef int     (*Func_open)(const char*, int, mode_t);
typedef int     (*Func_open64)(const char*, int, mode_t);

typedef int     (*Func_close)(int);
typedef int     (*Func_dup)(int);
typedef ssize_t (*Func_read)(int, void*, size_t);
typedef ssize_t (*Func_readv)(int, const struct iovec*, int);
typedef ssize_t (*Func_write)(int, const void*, size_t);
typedef ssize_t (*Func_writev)(int, const struct iovec*, int);
typedef void*   (*Func_mmap)(void*, size_t, int, int, int, __off_t);
typedef void*   (*Func_mmap64)(void*, size_t, int, int, int, __off64_t);
typedef int     (*Func_ioctl)(int, int, void*);

typedef int     (*Func_chdir)(const char*);
typedef int     (*Func_chmod)(const char*, mode_t);
typedef int     (*Func_chown)(const char*, uid_t, uid_t);
typedef int     (*Func_creat)(const char*, mode_t);
typedef int     (*Func_execve)(const char*, char* const[], char* const[]);
typedef char*   (*Func_getcwd)(char* buf, size_t size);
typedef int     (*Func_lchown)(const char*, uid_t, uid_t);
typedef int     (*Func_link)(const char*, const char*);
typedef int     (*Func_lstat)(const char*, struct stat*);
typedef int     (*Func_lstat64)(const char*, struct stat*);
typedef int     (*Func___lxstat)(int version, const char*, struct stat*);
typedef int     (*Func___lxstat64)(int version, const char*, struct stat*);
typedef int     (*Func_mkdir)(const char*, mode_t mode);
typedef ssize_t (*Func_readlink)(const char*, char*, size_t);
typedef int     (*Func_rename)(const char*, const char*);
typedef int     (*Func_rmdir)(const char*);
typedef int     (*Func_stat)(const char*, struct stat*);
typedef int     (*Func_stat64)(const char*, struct stat*);
typedef int     (*Func___xstat)(int version, const char*, struct stat*);
typedef int     (*Func___xstat64)(int version, const char*, struct stat*);
typedef int     (*Func_statfs)(const char*, struct statfs*);
typedef int     (*Func_statfs64)(const char*, struct statfs*);
typedef int     (*Func_symlink)(const char*, const char*);
typedef int     (*Func_unlink)(const char*);
typedef int     (*Func_utime)(const char*, const struct utimbuf*);
typedef int     (*Func_utimes)(const char*, const struct timeval []);

typedef int     (*Func_execl)(const char*, const char*, ...);
typedef int     (*Func_execle)(const char*, const char*, ...);
typedef int     (*Func_execlp)(const char*, const char*, ...);
typedef int     (*Func_execv)(const char*, char* const []);
typedef int     (*Func_execvp)(const char*, char* const []);
typedef FILE*   (*Func_fopen)(const char*, const char*);
typedef FILE*   (*Func_fopen64)(const char*, const char*);
typedef FILE*   (*Func_freopen)(const char*, const char*, FILE*);
typedef int     (*Func_ftw)(const char*,
                    int (*fn) (const char*, const struct stat*, int),
                    int);
typedef DIR*    (*Func_opendir)(const char* path);
typedef void*   (*Func_dlopen)(const char*, int);

typedef int     (*Func_setpriority)(int, int, int);
//typedef int     (*Func_pipe)(int [2]);


/*
 * Pointers to the actual implementations.
 */
#ifndef CREATE_FUNC_STORAGE
# define EXTERN_FUNC extern
#else
# define EXTERN_FUNC
#endif
EXTERN_FUNC Func_access _ws_access;
EXTERN_FUNC Func_open _ws_open;
EXTERN_FUNC Func_open64 _ws_open64;

EXTERN_FUNC Func_close _ws_close;
EXTERN_FUNC Func_dup _ws_dup;
EXTERN_FUNC Func_read _ws_read;
EXTERN_FUNC Func_readv _ws_readv;
EXTERN_FUNC Func_write _ws_write;
EXTERN_FUNC Func_writev _ws_writev;
EXTERN_FUNC Func_mmap _ws_mmap;
EXTERN_FUNC Func_mmap64 _ws_mmap64;
EXTERN_FUNC Func_ioctl _ws_ioctl;

EXTERN_FUNC Func_chdir _ws_chdir;
EXTERN_FUNC Func_chmod _ws_chmod;
EXTERN_FUNC Func_chown _ws_chown;
EXTERN_FUNC Func_creat _ws_creat;
EXTERN_FUNC Func_execve _ws_execve;
EXTERN_FUNC Func_getcwd _ws_getcwd;
EXTERN_FUNC Func_lchown _ws_lchown;
EXTERN_FUNC Func_link _ws_link;
EXTERN_FUNC Func_lstat _ws_lstat;
EXTERN_FUNC Func_lstat64 _ws_lstat64;
EXTERN_FUNC Func___lxstat _ws___lxstat;
EXTERN_FUNC Func___lxstat64 _ws___lxstat64;
EXTERN_FUNC Func_mkdir _ws_mkdir;
EXTERN_FUNC Func_readlink _ws_readlink;
EXTERN_FUNC Func_rename _ws_rename;
EXTERN_FUNC Func_rmdir _ws_rmdir;
EXTERN_FUNC Func_stat _ws_stat;
EXTERN_FUNC Func_stat64 _ws_stat64;
EXTERN_FUNC Func___xstat _ws___xstat;
EXTERN_FUNC Func___xstat64 _ws___xstat64;
EXTERN_FUNC Func_statfs _ws_statfs;
EXTERN_FUNC Func_statfs64 _ws_statfs64;
EXTERN_FUNC Func_symlink _ws_symlink;
EXTERN_FUNC Func_unlink _ws_unlink;
EXTERN_FUNC Func_utime _ws_utime;
EXTERN_FUNC Func_utimes _ws_utimes;

EXTERN_FUNC Func_execl _ws_execl;
EXTERN_FUNC Func_execle _ws_execle;
EXTERN_FUNC Func_execlp _ws_execlp;
EXTERN_FUNC Func_execv _ws_execv;
EXTERN_FUNC Func_execvp _ws_execvp;
EXTERN_FUNC Func_fopen _ws_fopen;
EXTERN_FUNC Func_fopen64 _ws_fopen64;
EXTERN_FUNC Func_freopen _ws_freopen;
EXTERN_FUNC Func_ftw _ws_ftw;
EXTERN_FUNC Func_opendir _ws_opendir;
EXTERN_FUNC Func_dlopen _ws_dlopen;

EXTERN_FUNC Func_setpriority _ws_setpriority;
//EXTERN_FUNC Func_pipe _ws_pipe;

#define kMaxDisplays 4

/*
 * Global values.  Must be initialized in initGlobals(), which is executed
 * the first time somebody calls dlopen on the wrapper lib.
 */
struct WrapSimGlobals {
    volatile int    initialized;

    /* descriptor where we write log messages */
    int         logFd;

    /* socket for communicating with simulator front-end */
    int         simulatorFd;

    /* coordinate thread startup */
    pthread_mutex_t startLock;
    pthread_cond_t  startCond;
    int             startReady;
    int             simulatorInitFailed;

    /* base directory for filename remapping */
    char*       remapBaseDir;
    int         remapBaseDirLen;

    /*
     * Display characteristics.
     *
     * TODO: this is retrieved from the simulator during initial config.
     * It needs to be visible to whatever process holds the surfaceflinger,
     * which may or may not be the initial process in multi-process mode.
     * We probably want to get the display config via a query, performed at
     * intercepted-ioctl time, rather than a push from the sim at startup.
     */
    struct {
        int     width;
        int     height;

        int     shmemKey;
        int     shmid;
        void*   addr;
        long    length;
        int     semid;
    } display[kMaxDisplays];
    int     numDisplays;

    /*
     * Input device.
     */
    FakeDev*    keyInputDevice;
    const char *keyMap;

    /* fake file descriptor allocation map */
    pthread_mutex_t fakeFdLock;
    BitVector*  fakeFdMap;
    FakeDev*    fakeFdList[kMaxFakeFdCount];

    /* used for wsAtomicAdd */
    pthread_mutex_t atomicLock;
};

extern struct WrapSimGlobals gWrapSim;

#endif /*_WRAPSIM_GLOBALS_H*/
