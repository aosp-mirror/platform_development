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
#ifndef _OSUTILS_PROCESS_H
#define _OSUTILS_PROCESS_H

#ifdef _WIN32
#include <windows.h>
#endif

namespace osUtils {

class childProcess
{
public:
    static childProcess *create(const char *p_cmdLine, const char *p_startdir);
    ~childProcess();

    int getPID()
    {
#ifdef _WIN32
        return m_proc.dwProcessId;
#else
        return(m_pid);
#endif
    }

    int tryWait(bool& isAlive);
    bool wait(int *exitStatus);

private:
    childProcess() {};

private:
#ifdef _WIN32
    PROCESS_INFORMATION m_proc;
#else
    int m_pid;
#endif
};

int ProcessGetPID();
int ProcessGetTID();
bool ProcessGetName(char *p_outName, int p_outNameLen);
int KillProcess(int pid, bool wait);
bool isProcessRunning(int pid);

} // of namespace osUtils

#endif
