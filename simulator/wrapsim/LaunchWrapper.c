/*
 * Copyright 2007 The Android Open Source Project
 *
 * Launch the specified program and, if "-wait" was specified, wait for it
 * to exit.
 *
 * When in "wait mode", print a message indicating the exit status, then
 * wait for Ctrl-C before we exit.  This is useful if we were launched
 * with "xterm -e", because it lets us see the output before the xterm bails.
 *
 * We want to ignore signals while waiting, so Ctrl-C kills the child rather
 * than us, but we need to configure the signals *after* the fork() so we
 * don't block them for the child too.
 */
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <limits.h>
#include <fcntl.h>
#include <errno.h>
#include <assert.h>

/*
 * This is appended to $ANDROID_PRODUCT_OUT,
 * e.g. "/work/device/out/debug/host/linux-x8x/product/sim".
 */
static const char* kWrapLib = "/system/lib/libwrapsim.so";


/*
 * Configure LD_PRELOAD if possible.
 *
 * Returns newly-allocated storage with the preload path.
 */
static char* configurePreload(void)
{
    const char* outEnv = getenv("ANDROID_PRODUCT_OUT");
    const char* preloadEnv = getenv("LD_PRELOAD");
    char* preload = NULL;

    if (preloadEnv != NULL) {
        /* TODO: append our stuff to existing LD_PRELOAD string */
        fprintf(stderr,
            "LW WARNING: LD_PRELOAD already set, not adding libwrapsim\n");
    } else if (outEnv == NULL || *outEnv == '\0') {
        fprintf(stderr, "LW WARNING: "
            "$ANDROID_PRODUCT_OUT not in env, not setting LD_PRELOAD\n");
    } else {
        preload = (char*) malloc(strlen(outEnv) + strlen(kWrapLib) +1);
        sprintf(preload, "%s%s", outEnv, kWrapLib);
        setenv("LD_PRELOAD", preload, 1);
        printf("LW: launching with LD_PRELOAD=%s\n", preload);
    }

    /* Let the process know that it's executing inside this LD_PRELOAD
     * wrapper.
     */
    setenv("ANDROID_WRAPSIM", "1", 1);

    return preload;
}

/*
 * Configure some environment variables that the runtime wants.
 *
 * Returns 0 if all goes well.
 */
static int configureEnvironment()
{
    const char* outEnv = getenv("ANDROID_PRODUCT_OUT");
    char pathBuf[PATH_MAX];
    int outLen;

    if (outEnv == NULL || *outEnv == '\0') {
        fprintf(stderr, "LW WARNING: "
            "$ANDROID_PRODUCT_OUT not in env, not configuring environment\n");
        return 1;
    }
    outLen = strlen(outEnv);
    assert(outLen + 64 < PATH_MAX);
    memcpy(pathBuf, outEnv, outLen);
    strcpy(pathBuf + outLen, "/system/lib");

    /*
     * Linux wants LD_LIBRARY_PATH
     * Mac OS X wants DYLD_LIBRARY_PATH
     * gdb under Mac OS X tramples on both of the above, so we added
     * ANDROID_LIBRARY_PATH as a workaround.
     *
     * We're only supporting Linux now, so just set LD_LIBRARY_PATH.  Note
     * this stomps the existing value, if any.
     *
     * If we only needed this for System.loadLibrary() we could do it later,
     * but we need it to get the runtime started.
     */
    printf("LW: setting LD_LIBRARY_PATH=%s\n", pathBuf);
    setenv("LD_LIBRARY_PATH", pathBuf, 1);

    /*
     * Trusted certificates are found, for some bizarre reason, through
     * the JAVA_HOME environment variable.  We don't need to set this
     * here, but it's convenient to do so.
     */
    strcpy(pathBuf /*+ outLen*/, "/system");
    printf("LW: setting JAVA_HOME=%s\n", pathBuf);
    setenv("JAVA_HOME", pathBuf, 1);

    return 0;
}

/*
 * Redirect stdout/stderr to the specified file.  If "fileName" is NULL,
 * this returns successfully without doing anything.
 *
 * Returns 0 on success.
 */
static int redirectStdio(const char* fileName)
{
    int fd;

    if (fileName == NULL)
        return 0;

    printf("Redirecting stdio to append to '%s'\n", fileName);
    fflush(stdout);
    fflush(stderr);

    fd = open(fileName, O_WRONLY | O_APPEND | O_CREAT, 0666);
    if (fd < 0) {
        fprintf(stderr, "ERROR: unable to open '%s' for writing\n",
            fileName);
        return 1;
    }
    dup2(fd, 1);
    dup2(fd, 2);
    close(fd);

    return 0;
}

/*
 * Launch the requested process directly.
 *
 * On success this does not return (ever).
 */
static int launch(char* argv[], const char* outputFile)
{
    (void) configurePreload();
    (void) redirectStdio(outputFile);
    execvp(argv[0], argv);
    fprintf(stderr, "execvp %s failed: %s\n", argv[0], strerror(errno));
    return 1;
}

/*
 * Launch in a sub-process and wait for it to finish.
 */
static int launchWithWait(char* argv[], const char* outputFile)
{
    pid_t child;

    child = fork();
    if (child < 0) {
        fprintf(stderr, "fork() failed: %s\n", strerror(errno));
        return 1;
    } else if (child == 0) {
        /*
         * This is the child, set up LD_PRELOAD if possible and launch.
         */
        (void) configurePreload();
        (void) redirectStdio(outputFile);
        execvp(argv[0], argv);
        fprintf(stderr, "execvp %s failed: %s\n", argv[0], strerror(errno));
        return 1;
    } else {
        pid_t result;
        int status;

        signal(SIGINT, SIG_IGN);
        signal(SIGQUIT, SIG_IGN);

        while (1) {
            printf("LW: in pid %d (grp=%d), waiting on pid %d\n",
                (int) getpid(), (int) getpgrp(), (int) child);
            result = waitpid(child, &status, 0);
            if (result < 0) {
                if (errno == EINTR) {
                    printf("Hiccup!\n");
                    continue;
                } else {
                    fprintf(stderr, "waitpid failed: %s\n", strerror(errno));
                    return 1;
                }
            } else if (result != child) {
                fprintf(stderr, "bizarre: waitpid returned %d (wanted %d)\n",
                    result, child);
                return 1;
            } else {
                break;
            }
        }

        printf("\n");
        if (WIFEXITED(status)) {
            printf("LW: process exited (status=%d)", WEXITSTATUS(status));
        } else if (WIFSIGNALED(status)) {
            printf("LW: process killed by signal %d", WTERMSIG(status));
        } else {
            printf("LW: process freaked out, status=0x%x\n", status);
        }
        if (WCOREDUMP(status)) {
            printf(" (core dumped)");
        }
        printf("\n");

        signal(SIGINT, SIG_DFL);
        signal(SIGQUIT, SIG_DFL);

        /*
         * The underlying process may have changed process groups and pulled
         * itself into the foreground.  Now that it's gone, pull ourselves
         * back into the foreground.
         */
        signal(SIGTTOU, SIG_IGN);
        if (tcsetpgrp(fileno(stdin), getpgrp()) != 0)
            fprintf(stderr, "WARNING: tcsetpgrp failed\n");

        printf("\nHit Ctrl-C or close window.\n");

        while (1) {
            sleep(10);
        }

        /* not reached */
        return 0;
    }
}


/*
 * All args are passed through.
 */
int main(int argc, char** argv)
{
    int waitForChild = 0;
    const char* outputFile = NULL;
    int result;

    /*
     * Skip past the reference to ourselves, and check for args.
     */
    argv++;
    argc--;
    while (argc > 0) {
        if (strcmp(argv[0], "-wait") == 0) {
            waitForChild = 1;
        } else if (strcmp(argv[0], "-output") == 0 && argc > 1) {
            argv++;
            argc--;
            outputFile = argv[0];
        } else {
            /* no more args for us */
            break;
        }

        argv++;
        argc--;
    }

    if (argc == 0) {
        fprintf(stderr,
            "Usage: launch-wrapper [-wait] [-output filename] <cmd> [args...]\n");
        result = 2;
        goto bail;
    }

    /*
     * Configure some environment variables.
     */
    if (configureEnvironment() != 0) {
        result = 1;
        goto bail;
    }

    /*
     * Launch.
     */
    if (waitForChild)
        result = launchWithWait(argv, outputFile);
    else
        result = launch(argv, outputFile);

bail:
    if (result != 0)
        sleep(2);
    return result;
}

