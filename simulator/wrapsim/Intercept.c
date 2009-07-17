/*
 * Copyright 2007 The Android Open Source Project
 *
 * Syscall and library intercepts.
 */

/* don't remap open() to open64() */
#undef _FILE_OFFSET_BITS

#define CREATE_FUNC_STORAGE
#include "Common.h"

#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/uio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <utime.h>
#include <limits.h>
#include <ftw.h>
#include <assert.h>


#if defined _FILE_OFFSET_BITS && _FILE_OFFSET_BITS == 64
#warning "big"
#endif

//#define CALLTRACE(format, ...)   wsLog(format, ##__VA_ARGS__)
#define CALLTRACE(format, ...)   ((void)0)

//#define CALLTRACEV(format, ...)  wsLog(format, ##__VA_ARGS__)
#define CALLTRACEV(format, ...)  ((void)0)

/*
When opening certain files, we need to simulate the contents.  For example,
we can pretend to open the frame buffer, and respond to ioctl()s by
returning fake data or telling the front-end to render new data.

We want to intercept specific files in /dev.  In some cases we want to
intercept and reject, e.g. to indicate that a standard Linux device does
not exist.

Some things we're not going to intercept:
  /etc/... (e.g. /etc/timezone) -- std. Linux version should be okay
  /proc/... (e.g. /proc/stat) -- we're showing real pid, so real proc will work

For the device drivers we need to intercept:

  close(), ioctl(), mmap(), open()/open64(), read(), readv(), write(),
  writev()

May also need stat().  We don't need all fd calls, e.g. fchdir() is
not likely to succeed on a device driver.  The expected uses of mmap()
shouldn't require intercepting related calls like madvise() -- we will
provide an actual mapping of the requested size.  In some cases we will
want to return a "real" fd so the app can poll() or select() on it.


We also need to consider:
  getuid/setuid + variations -- fake out multi-user-id stuff


We also want to translate filenames, effectively performing a "chroot"
without all the baggage that comes with it.  The mapping looks like:

  /system/... --> $ANDROID_PRODUCT_OUT/system/...
  /data/... --> $ANDROID_PRODUCT_OUT/data/...

Translating pathnames requires interception of additional system calls,
substituting a new path.  Calls include:

  access(), chdir(), chmod(), chown(), creat(), execve(), getcwd(),
  lchown(), link(), lstat()/lstat64(), mkdir(), open()/open64(),
  readlink(), rename(), rmdir(), stat()/stat64(), statfs/statfs64(),
  symlink(), unlink(), utimes(),

Possibly also mknod(), mount(), umount().

The "at" family, notably openat(), should just work since the root comes
from an open directory fd.

We also need these libc calls, because LD_LIBRARY_PATH substitutes at
the libc link level, not the syscall layer:

  execl(), execlp(), execle(), execv(), execvp(), fopen(), ftw(), getwd(),
  opendir(), dlopen()

It is possible for the cwd to leak out.  Some possible leaks:
  - /proc/[self]/exe
  - /proc/[self]/cwd
  - LD_LIBRARY_PATH (which may be awkward to work around)


To provide a replacement for the dirent functions -- only required if we
want to show "fake" directory contents -- we would need:

  closedir(), dirfd() readdir(), rewinddir(), scandir(), seekdir(),
  telldir()


*/


/*
 * ===========================================================================
 *      Filename remapping
 * ===========================================================================
 */

/*
 * If appropriate, rewrite the path to point to a different location.
 *
 * Returns either "pathBuf" or "origPath" depending on whether or not we
 * chose to rewrite the path.  "origPath" must be a buffer capable of
 * holding an extended pathname; for best results use PATH_MAX.
 */
static const char* rewritePath(const char* func, char* pathBuf,
    const char* origPath)
{
    /*
     * Rewrite paths that start with "/system/" or "/data/"
     */
    if (origPath[0] != '/')
        goto skip_rewrite;
    while (origPath[1] == '/') origPath++; // some apps like to use paths like '//data/data/....'
    if (memcmp(origPath+1, "system", 6) == 0 &&
        (origPath[7] == '/' || origPath[7] == '\0'))
            goto do_rewrite;
    if (memcmp(origPath+1, "data", 4) == 0 &&
        (origPath[5] == '/' || origPath[5] == '\0'))
            goto do_rewrite;

skip_rewrite:
    /* check to see if something is side-stepping the rewrite */
    if (memcmp(origPath, gWrapSim.remapBaseDir, gWrapSim.remapBaseDirLen) == 0)
    {
        wsLog("NOTE: full path used: %s(%s)\n", func, origPath);
    }

    CALLTRACE("rewrite %s('%s') --> (not rewritten)\n", func, origPath);
    return origPath;

do_rewrite:
    memcpy(pathBuf, gWrapSim.remapBaseDir, gWrapSim.remapBaseDirLen);
    strcpy(pathBuf + gWrapSim.remapBaseDirLen, origPath);
    CALLTRACE("rewrite %s('%s') --> '%s'\n", func, origPath, pathBuf);
    return pathBuf;
}

/*
 * This works if the pathname is the first argument to the function, and
 * the function returns "int".
 */
#define PASS_THROUGH_DECL(_fname, _rtype, ...)                              \
    _rtype _fname( __VA_ARGS__ )
#define PASS_THROUGH_BODY(_fname, _patharg, ...)                            \
    {                                                                       \
        CALLTRACEV("%s(%s)\n", __FUNCTION__, _patharg);                     \
        char pathBuf[PATH_MAX];                                             \
        return _ws_##_fname(rewritePath(#_fname, pathBuf, _patharg),        \
            ##__VA_ARGS__);                                                 \
    }


PASS_THROUGH_DECL(chdir, int, const char* path)
PASS_THROUGH_BODY(chdir, path)

PASS_THROUGH_DECL(chmod, int, const char* path, mode_t mode)
PASS_THROUGH_BODY(chmod, path, mode)

PASS_THROUGH_DECL(chown, int, const char* path, uid_t owner, gid_t group)
PASS_THROUGH_BODY(chown, path, owner, group)

PASS_THROUGH_DECL(creat, int, const char* path, mode_t mode)
PASS_THROUGH_BODY(creat, path, mode)

PASS_THROUGH_DECL(execve, int, const char* path, char* const argv[],
    char* const envp[])
PASS_THROUGH_BODY(execve, path, argv, envp)

PASS_THROUGH_DECL(lchown, int, const char* path, uid_t owner, gid_t group)
PASS_THROUGH_BODY(lchown, path, owner, group)

PASS_THROUGH_DECL(lstat, int, const char* path, struct stat* buf)
PASS_THROUGH_BODY(lstat, path, buf)

PASS_THROUGH_DECL(lstat64, int, const char* path, struct stat* buf)
PASS_THROUGH_BODY(lstat64, path, buf)

PASS_THROUGH_DECL(mkdir, int, const char* path, mode_t mode)
PASS_THROUGH_BODY(mkdir, path, mode)

PASS_THROUGH_DECL(readlink, ssize_t, const char* path, char* buf, size_t bufsiz)
PASS_THROUGH_BODY(readlink, path, buf, bufsiz)

PASS_THROUGH_DECL(rmdir, int, const char* path)
PASS_THROUGH_BODY(rmdir, path)

PASS_THROUGH_DECL(stat, int, const char* path, struct stat* buf)
PASS_THROUGH_BODY(stat, path, buf)

PASS_THROUGH_DECL(stat64, int, const char* path, struct stat* buf)
PASS_THROUGH_BODY(stat64, path, buf)

PASS_THROUGH_DECL(statfs, int, const char* path, struct statfs* buf)
PASS_THROUGH_BODY(statfs, path, buf)

PASS_THROUGH_DECL(statfs64, int, const char* path, struct statfs* buf)
PASS_THROUGH_BODY(statfs64, path, buf)

PASS_THROUGH_DECL(unlink, int, const char* path)
PASS_THROUGH_BODY(unlink, path)

PASS_THROUGH_DECL(utime, int, const char* path, const struct utimbuf* buf)
PASS_THROUGH_BODY(utime, path, buf)

PASS_THROUGH_DECL(utimes, int, const char* path, const struct timeval times[2])
PASS_THROUGH_BODY(utimes, path, times)


PASS_THROUGH_DECL(fopen, FILE*, const char* path, const char* mode)
PASS_THROUGH_BODY(fopen, path, mode)

PASS_THROUGH_DECL(fopen64, FILE*, const char* path, const char* mode)
PASS_THROUGH_BODY(fopen64, path, mode)

PASS_THROUGH_DECL(freopen, FILE*, const char* path, const char* mode,
    FILE* stream)
PASS_THROUGH_BODY(freopen, path, mode, stream)

PASS_THROUGH_DECL(ftw, int, const char* dirpath,
          int (*fn) (const char* fpath, const struct stat* sb, int typeflag),
          int nopenfd)
PASS_THROUGH_BODY(ftw, dirpath, fn, nopenfd)

PASS_THROUGH_DECL(opendir, DIR*, const char* path)
PASS_THROUGH_BODY(opendir, path)

PASS_THROUGH_DECL(dlopen, void*, const char* path, int flag)
PASS_THROUGH_BODY(dlopen, path, flag)

/*
 * Opposite of path translation -- remove prefix.
 *
 * It looks like BSD allows you to pass a NULL value for "buf" to inspire
 * getcwd to allocate storage with malloc() (as an extension to the POSIX
 * definition, which doesn't specify this).  getcwd() is a system call
 * under Linux, so this doesn't work, but that doesn't stop gdb from
 * trying to use it anyway.
 */
char* getcwd(char* buf, size_t size)
{
    CALLTRACEV("%s %p %d\n", __FUNCTION__, buf, size);

    char* result = _ws_getcwd(buf, size);
    if (buf != NULL && result != NULL) {
        if (memcmp(buf, gWrapSim.remapBaseDir,
                    gWrapSim.remapBaseDirLen) == 0)
        {
            memmove(buf, buf + gWrapSim.remapBaseDirLen,
                strlen(buf + gWrapSim.remapBaseDirLen)+1);
            CALLTRACE("rewrite getcwd() -> %s\n", result);
        } else {
            CALLTRACE("not rewriting getcwd(%s)\n", result);
        }
    }
    return result;
}

/*
 * Need to tweak both pathnames.
 */
int link(const char* oldPath, const char* newPath)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf1[PATH_MAX];
    char pathBuf2[PATH_MAX];
    return _ws_link(rewritePath("link-1", pathBuf1, oldPath),
                    rewritePath("link-2", pathBuf2, newPath));
}

/*
 * Need to tweak both pathnames.
 */
int rename(const char* oldPath, const char* newPath)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf1[PATH_MAX];
    char pathBuf2[PATH_MAX];
    return _ws_rename(rewritePath("rename-1", pathBuf1, oldPath),
                      rewritePath("rename-2", pathBuf2, newPath));
}

/*
 * Need to tweak both pathnames.
 */
int symlink(const char* oldPath, const char* newPath)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf1[PATH_MAX];
    char pathBuf2[PATH_MAX];
    return _ws_symlink(rewritePath("symlink-1", pathBuf1, oldPath),
                       rewritePath("symlink-2", pathBuf2, newPath));
}

/*
 * glibc stat turns into this (32-bit).
 */
int __xstat(int version, const char* path, struct stat* sbuf)
{
    CALLTRACEV("%s\n", __FUNCTION__);
    char pathBuf[PATH_MAX];
    return _ws___xstat(version, rewritePath("__xstat", pathBuf, path),
        sbuf);
}

/*
 * glibc stat turns into this (64-bit).
 */
int __xstat64(int version, const char* path, struct stat* sbuf)
{
    CALLTRACEV("%s\n", __FUNCTION__);
    char pathBuf[PATH_MAX];
    return _ws___xstat64(version, rewritePath("__xstat64", pathBuf, path),
        sbuf);
}

/*
 * glibc lstat turns into this (32-bit).
 */
int __lxstat(int version, const char* path, struct stat* sbuf)
{
    CALLTRACEV("%s\n", __FUNCTION__);
    char pathBuf[PATH_MAX];
    return _ws___lxstat(version, rewritePath("__lxstat", pathBuf, path),
        sbuf);
}

/*
 * glibc lstat turns into this (64-bit).
 */
int __lxstat64(int version, const char* path, struct stat* sbuf)
{
    CALLTRACEV("%s\n", __FUNCTION__);
    char pathBuf[PATH_MAX];
    return _ws___lxstat64(version, rewritePath("__lxstat64", pathBuf, path),
        sbuf);
}

/*
 * Copy the argument list out of varargs for execl/execlp/execle.  This
 * leaves the argc value in _argc, and a NULL-terminated array of character
 * pointers in _argv.  We stop at the first NULL argument, so we shouldn't
 * end up copying "envp" out.
 *
 * We could use gcc __builtin_apply_args to just pass stuff through,
 * but that may not get along with the path rewriting.  It's unclear
 * whether we want to rewrite the first argument (i.e. the string that
 * becomes argv[0]); it only makes sense if the exec'ed program is also
 * getting remapped.
 */
#define COPY_EXEC_ARGLIST(_first, _argc, _argv)                             \
    int _argc = 0;                                                          \
    {                                                                       \
        va_list vargs;                                                      \
        va_start(vargs, _first);                                            \
        while (1) {                                                         \
            _argc++;                                                        \
            const char* val = va_arg(vargs, const char*);                   \
            if (val == NULL)                                                \
                break;                                                      \
        }                                                                   \
        va_end(vargs);                                                      \
    }                                                                       \
    const char* _argv[_argc+1];                                             \
    _argv[0] = _first;                                                      \
    {                                                                       \
        va_list vargs;                                                      \
        int i;                                                              \
        va_start(vargs, _first);                                            \
        for (i = 1; i < _argc; i++) {                                       \
            _argv[i] = va_arg(vargs, const char*);                          \
        }                                                                   \
        va_end(vargs);                                                      \
    }                                                                       \
    _argv[_argc] = NULL;

/*
 * Debug dump.
 */
static void dumpExecArgs(const char* callName, const char* path,
    int argc, const char* argv[], char* const envp[])
{
    int i;

    CALLTRACE("Calling %s '%s' (envp=%p)\n", callName, path, envp);
    for (i = 0; i <= argc; i++)
        CALLTRACE("  %d: %s\n", i, argv[i]);
}

/*
 * Extract varargs, convert paths, hand off to execv.
 */
int execl(const char* path, const char* arg, ...)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf[PATH_MAX];

    COPY_EXEC_ARGLIST(arg, argc, argv);
    dumpExecArgs("execl", path, argc, argv, NULL);
    path = rewritePath("execl", pathBuf, path);
    return _ws_execv(path, (char* const*) argv);
}

/*
 * Extract varargs, convert paths, hand off to execve.
 *
 * The execle prototype in the man page isn't valid C -- it shows the
 * "envp" argument after the "...".  We have to pull it out with the rest
 * of the varargs.
 */
int execle(const char* path, const char* arg, ...)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf[PATH_MAX];

    COPY_EXEC_ARGLIST(arg, argc, argv);

    /* run through again and find envp */
    char* const* envp;

    va_list vargs;
    va_start(vargs, arg);
    while (1) {
        const char* val = va_arg(vargs, const char*);
        if (val == NULL) {
            envp = va_arg(vargs, char* const*);
            break;
        }
    }
    va_end(vargs);

    dumpExecArgs("execle", path, argc, argv, envp);
    path = rewritePath("execl", pathBuf, path);

    return _ws_execve(path, (char* const*) argv, envp);
}

/*
 * Extract varargs, convert paths, hand off to execvp.
 */
int execlp(const char* file, const char* arg, ...)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf[PATH_MAX];

    COPY_EXEC_ARGLIST(arg, argc, argv);
    dumpExecArgs("execlp", file, argc, argv, NULL);
    file = rewritePath("execlp", pathBuf, file);
    return _ws_execvp(file, (char* const*) argv);
}

/*
 * Update path, forward to execv.
 */
int execv(const char* path, char* const argv[])
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf[PATH_MAX];

    path = rewritePath("execv", pathBuf, path);
    return _ws_execv(path, argv);
}

/*
 * Shouldn't need to do anything unless they specified a full path to execvp.
 */
int execvp(const char* file, char* const argv[])
{
    CALLTRACEV("%s\n", __FUNCTION__);

    char pathBuf[PATH_MAX];

    file = rewritePath("execvp", pathBuf, file);
    return _ws_execvp(file, argv);
}


/*
 * ===========================================================================
 *      Device fakery
 * ===========================================================================
 */

/*
 * Need to do filesystem translation and show fake devices.
 */
int access(const char* pathName, int mode)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    int status = wsInterceptDeviceAccess(pathName, mode);
    if (status == 0)
        return 0;
    else if (status == -2)
        return -1;          // errno already set
    else {
        char pathBuf[PATH_MAX];
        return _ws_access(rewritePath("access", pathBuf, pathName), mode);
    }
}

/*
 * Common handler for open().
 */
int openCommon(const char* pathName, int flags, mode_t mode)
{
    char pathBuf[PATH_MAX];
    int fd;

    assert(gWrapSim.initialized);

    fd = wsInterceptDeviceOpen(pathName, flags);
    if (fd >= 0) {
        return fd;
    } else if (fd == -2) {
        /* errno should be set */
        return -1;
    }

    if ((flags & O_CREAT) != 0) {
        fd = _ws_open(rewritePath("open", pathBuf, pathName), flags, mode);
        CALLTRACE("open(%s, 0x%x, 0%o) = %d\n", pathName, flags, mode, fd);
    } else {
        fd = _ws_open(rewritePath("open", pathBuf, pathName), flags, 0);
        CALLTRACE("open(%s, 0x%x) = %d\n", pathName, flags, fd);
    }
    return fd;
}

/*
 * Replacement open() and variants.
 *
 * We have to use the vararg decl for the standard call so it matches
 * the definition in fcntl.h.
 */
int open(const char* pathName, int flags, ...)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    mode_t mode = 0;
    if ((flags & O_CREAT) != 0) {
        va_list args;

        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }

    return openCommon(pathName, flags, mode);
}
int __open(const char* pathName, int flags, mode_t mode)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    return openCommon(pathName, flags, mode);
}

/*
 * Common handler for open64().
 */
int open64Common(const char* pathName, int flags, mode_t mode)
{
    char pathBuf[PATH_MAX];
    int fd;

    assert(gWrapSim.initialized);

    fd = wsInterceptDeviceOpen(pathName, flags);
    if (fd >= 0) {
        return fd;
    }

    if ((flags & O_CREAT) != 0) {
        fd = _ws_open64(rewritePath("open64", pathBuf, pathName), flags, mode);
        CALLTRACE("open64(%s, 0x%x, 0%o) = %d\n", pathName, flags, mode, fd);
    } else {
        fd = _ws_open64(rewritePath("open64", pathBuf, pathName), flags, 0);
        CALLTRACE("open64(%s, 0x%x) = %d\n", pathName, flags, fd);
    }
    return fd;
}

/*
 * Replacement open64() and variants.
 *
 * We have to use the vararg decl for the standard call so it matches
 * the definition in fcntl.h.
 */
int open64(const char* pathName, int flags, ...)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    mode_t mode = 0;
    if ((flags & O_CREAT) != 0) {
        va_list args;

        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }
    return open64Common(pathName, flags, mode);
}
int __open64(const char* pathName, int flags, mode_t mode)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    return open64Common(pathName, flags, mode);
}


int dup(int fd)
{
    CALLTRACEV("%s(%d)\n", __FUNCTION__, fd);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        FakeDev* newDev = dev->dup(dev, fd);
        if (newDev != NULL) {
            /*
             * Now that the device entry is ready, add it to the list.
             */
            wsLog("## dup'ed fake dev %d: '%s' %p\n",
                newDev->fd, newDev->debugName, newDev->state);
            gWrapSim.fakeFdList[newDev->fd - kFakeFdBase] = newDev;
            return newDev->fd;
        }
        return -1;
    } else {
        CALLTRACE("dup(%d)\n", fd);
        return _ws_dup(fd);
    }
}


/*
 * Close a file descriptor.
 */
int close(int fd)
{
    CALLTRACEV("%s(%d)\n", __FUNCTION__, fd);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        int result = dev->close(dev, fd);
        wsFreeFakeDev(dev);
        return result;
    } else {
        CALLTRACE("close(%d)\n", fd);
        return _ws_close(fd);
    }
}

/*
 * Map a region.
 */
void* mmap(void* start, size_t length, int prot, int flags, int fd,
    __off_t offset)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        return dev->mmap(dev, start, length, prot, flags, fd, offset);
    } else {
        CALLTRACE("mmap(%p, %d, %d, %d, %d, %d)\n",
            start, (int) length, prot, flags, fd, (int) offset);
        return _ws_mmap(start, length, prot, flags, fd, offset);
    }
}

/*
 * Map a region.
 */
void* mmap64(void* start, size_t length, int prot, int flags, int fd,
    __off64_t offset)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        return dev->mmap(dev, start, length, prot, flags, fd, (__off_t) offset);
    } else {
        CALLTRACE("mmap64(%p, %d, %d, %d, %d, %d)\n",
            start, (int) length, prot, flags, fd, (int) offset);
        return _ws_mmap(start, length, prot, flags, fd, offset);
    }
}

/*
 * The Linux headers show this with a vararg header, but as far as I can
 * tell the kernel always expects 3 args.
 */
int ioctl(int fd, int request, ...)
{
    CALLTRACEV("%s(%d, %d, ...)\n", __FUNCTION__, fd, request);

    FakeDev* dev = wsFakeDevFromFd(fd);
    va_list args;
    void* argp;

    /* extract argp from varargs */
    va_start(args, request);
    argp = va_arg(args, void*);
    va_end(args);

    if (dev != NULL) {
        return dev->ioctl(dev, fd, request, argp);
    } else {
        CALLTRACE("ioctl(%d, 0x%x, %p)\n", fd, request, argp);
        return _ws_ioctl(fd, request, argp);
    }
}

/*
 * Read data.
 */
ssize_t read(int fd, void* buf, size_t count)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        return dev->read(dev, fd, buf, count);
    } else {
        CALLTRACE("read(%d, %p, %u)\n", fd, buf, count);
        return _ws_read(fd, buf, count);
    }
}

/*
 * Write data.
 */
ssize_t write(int fd, const void* buf, size_t count)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        return dev->write(dev, fd, buf, count);
    } else {
        CALLTRACE("write(%d, %p, %u)\n", fd, buf, count);
        return _ws_write(fd, buf, count);
    }
}

/*
 * Read a data vector.
 */
ssize_t readv(int fd, const struct iovec* vector, int count)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        return dev->readv(dev, fd, vector, count);
    } else {
        CALLTRACE("readv(%d, %p, %u)\n", fd, vector, count);
        return _ws_readv(fd, vector, count);
    }
}

/*
 * Write a data vector.
 */
ssize_t writev(int fd, const struct iovec* vector, int count)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    FakeDev* dev = wsFakeDevFromFd(fd);
    if (dev != NULL) {
        return dev->writev(dev, fd, vector, count);
    } else {
        CALLTRACE("writev(%d, %p, %u)\n", fd, vector, count);
        return _ws_writev(fd, vector, count);
    }
}

/*
 * Set the scheduling priority.  The sim doesn't run as root, so we have
 * to fake this out.
 *
 * For now, do some basic verification of the which and who parameters,
 * but otherwise return success.  In the future we may want to track
 * these so getpriority works.
 */
int setpriority(__priority_which_t which, id_t who, int what)
{
    CALLTRACEV("%s\n", __FUNCTION__);

    if (which != PRIO_PROCESS &&
        which != PRIO_PGRP &&
        which != PRIO_USER) {
        return EINVAL;
    }

    if ((int)who < 0) {
        return ESRCH;
    }

    return 0;
}

/*
 * Pretend to be running as root, so the Android framework
 * doesn't complain about permission problems all over the
 * place.
 */
uid_t getuid(void)
{
    return 0;
}

#if 0
/*
 * Create a pipe.  (Only needed for debugging an fd leak.)
 */
int pipe(int filedes[2])
{
    CALLTRACEV("%s\n", __FUNCTION__);

    int result = _ws_pipe(filedes);
    if (result == 0)
        CALLTRACE("pipe(%p) -> %d,%d\n", filedes, filedes[0], filedes[1]);
    if (filedes[0] == 83)
        abort();
    return result;
}
#endif
