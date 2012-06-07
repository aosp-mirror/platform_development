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
#include <windows.h>
#include <string>
#include <stdlib.h>
#include <psapi.h>

namespace osUtils {

childProcess *
childProcess::create(const char *p_cmdLine, const char *p_startdir)
{
    childProcess *child = new childProcess();
    if (!child) {
        return NULL;
    }

    STARTUPINFOA        si;
    ZeroMemory(&si, sizeof(si));

    ZeroMemory(&child->m_proc, sizeof(child->m_proc));
    BOOL ret = CreateProcessA(
                    NULL ,
                    (LPSTR)p_cmdLine,
                    NULL,
                    NULL,
                    FALSE,
                    CREATE_DEFAULT_ERROR_MODE,
                    NULL,
                    (p_startdir != NULL ? p_startdir : ".\\"),
                    &si,
                    &child->m_proc);
    if (ret == 0) {
        delete child;
        return NULL;
    }

    // close the thread handle we do not need it,
    // keep the process handle for wait/trywait operations, will
    // be closed on destruction
    CloseHandle(child->m_proc.hThread);

    return child;
}

childProcess::~childProcess()
{
    if (m_proc.hProcess) {
        CloseHandle(m_proc.hProcess);
    }
}

bool
childProcess::wait(int *exitStatus)
{
DWORD _exitStatus;

    if (WaitForSingleObject(m_proc.hProcess, INFINITE) == WAIT_FAILED) {
        return false;
    }

    if (!GetExitCodeProcess(m_proc.hProcess, &_exitStatus))
    {
        return false;
    }

    if (exitStatus) {
        *exitStatus = _exitStatus;
    }

    return true;
}

int
childProcess::tryWait(bool& isAlive)
{
    DWORD status = WaitForSingleObject(m_proc.hProcess, 0);

    if(status == WAIT_OBJECT_0)
    {
        // process has exited
        isAlive = false;
        GetExitCodeProcess(m_proc.hProcess, &status);
    }
    else if (status == WAIT_TIMEOUT)
    {
        isAlive = true;
        status = 0;
    }

    return status;

}

int ProcessGetPID()
{
    return GetCurrentProcessId();
}

int ProcessGetTID()
{
    return GetCurrentThreadId();
}

bool ProcessGetName(char *p_outName, int p_outNameLen)
{
    return 0 != GetModuleFileNameEx( GetCurrentProcess(), NULL, p_outName, p_outNameLen);
}

int KillProcess(int pid, bool wait)
{
    DWORD exitStatus = 1;
    HANDLE hProc = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pid);

    if (NULL == hProc) {
        return 0;
    }

    //
    // Terminate the process
    //
    TerminateProcess(hProc, 0x55);

    if (wait) {
        //
        // Wait for it to be terminated
        //
        if(WaitForSingleObject(hProc, INFINITE) == WAIT_FAILED) {
            CloseHandle(hProc);
            return 0;
        }

        if (!GetExitCodeProcess(hProc, &exitStatus)) {
            CloseHandle(hProc);
            return 0;
        }
    }

    CloseHandle(hProc);

    return exitStatus;
}

bool isProcessRunning(int pid)
{
    bool isRunning = false;

    HANDLE process = OpenProcess(SYNCHRONIZE, FALSE, pid);
    if (NULL != process) {
        DWORD ret = WaitForSingleObject(process, 0);
        CloseHandle(process);
        isRunning = (ret == WAIT_TIMEOUT);
    }
    return isRunning;
}

} // of namespace osUtils
