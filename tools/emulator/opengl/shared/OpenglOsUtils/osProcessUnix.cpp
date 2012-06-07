/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "osProcess.h"
#include <stdio.h>
#include <stdlib.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <poll.h>
#include <pthread.h>
#include <string.h>
#include <pwd.h>
#include <paths.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <assert.h>

namespace osUtils {

//
// buildArgList converts a command line into null terminated argument list.
// to be used with execv or execvp.
// each argument is seperated by space or tab, to specify multiple words
// at the same argument place it inside single-quoted or double-quoted string.
//
static char **buildArgList(const char *command)
{
    char **argv = NULL;
    int argvSize = 0;
    int nArgs = 0;
    char *tmpcmd = strdup(command);
    char *t = tmpcmd;
    char *strStart = NULL;
    int i = 0;

    #define ADD_ARG \
        { \
            nArgs++; \
            if (!argv) { \
                argvSize = 12; \
                argv = (char **)malloc(argvSize * sizeof(char *)); \
            } \
            else if (nArgs > argvSize) { \
                argvSize += 12; \
                argv = (char **)realloc(argv, argvSize * sizeof(char *)); \
            } \
            argv[nArgs-1] = t; \
            t = NULL; \
        }

    while( tmpcmd[i] != '\0' ) {
        if (!strStart) {
            if (tmpcmd[i] == '"' || tmpcmd[i] == '\'') {
                strStart = &tmpcmd[i];
            }
            else if (tmpcmd[i] == ' ' || tmpcmd[i] == '\t') {
                tmpcmd[i] = '\0';
                if (t) ADD_ARG;
            }
            else if (!t) {
                t = &tmpcmd[i];
            }
        }
        else if (tmpcmd[i] == *strStart) {
            t = strStart;
            strStart = NULL;
        }

        i++;
    }
    if (t) {
        ADD_ARG;
    }
    if (nArgs > 0) {
        ADD_ARG; // for NULL terminating list
    }

    return argv;
}

static pid_t start_process(const char *command,const char *startDir)
{
    pid_t pid;

    pid = fork();

    if (pid < 0) {
        return pid;
    }
    else if (pid == 0) {
        //
        // Close all opened file descriptors
        //
        for (int i=3; i<256; i++) {
            close(i);
        }

        if (startDir) {
            chdir(startDir);
        }

        char **argv = buildArgList(command);
        if (!argv) {
            return -1;
        }
        execvp(argv[0], argv);

        perror("execl");
        exit(-101);
    }

    return pid;
}

childProcess *
childProcess::create(const char *p_cmdLine, const char *p_startdir)
{
    childProcess *child = new childProcess();
    if (!child) {
        return NULL;
    }

    child->m_pid = start_process(p_cmdLine, p_startdir);
    if (child->m_pid < 0) {
        delete child;
        return NULL;
    }

    return child;
}

childProcess::~childProcess()
{
}

bool
childProcess::wait(int *exitStatus)
{
    int ret=0;
    if (m_pid>0) {
        pid_t pid = waitpid(m_pid,&ret,0);
        if (pid != -1) {
            m_pid=-1;
            if (exitStatus) {
                *exitStatus = ret;
            }
            return true;
        }
    }
    return false;
}

int
childProcess::tryWait(bool &isAlive)
{
    int ret=0;
    isAlive = false;
    if (m_pid>0) {
        pid_t pid = waitpid(m_pid,&ret,WNOHANG);
        if (pid == 0) {
            isAlive = true;
        }
    }

    return ((char)WEXITSTATUS(ret));
}

int ProcessGetPID()
{
    return getpid();
}

int KillProcess(int pid, bool wait)
{
    if (pid<1) {
        return false;
    }

    if (0!=kill(pid,SIGTERM)) {
        return false;
    }

    if (wait) {
        if (waitpid(pid,NULL,0)<0) {
            return false;
        }
    }

    return true;
}

bool isProcessRunning(int pid)
{
    return (kill(pid,0) == 0);
}

} // of namespace osUtils
