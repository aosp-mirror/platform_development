/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The "SDK Launcher" is for Windows only.
 * This simple .exe will sit at the root of the Windows SDK
 * and currently simply executes tools\android.bat.
 * Eventually it should simply replace the batch file.
 *
 * TODO:
 * - detect that java is installed; error dialog if not, explaning where to get it.
 * - create temp dir, always copy *.jar there, exec android.jar
 * - get jars to copy from some file
 * - use a version number to copy jars only if needed (tools.revision?)
 */

#ifdef _WIN32

#include <stdio.h>
#include <windows.h>

int sdk_launcher() {
    STARTUPINFO           startup;
    PROCESS_INFORMATION   pinfo;
    char                  program_path[MAX_PATH];
    int                   ret;

    ZeroMemory(&startup, sizeof(startup));
    startup.cb = sizeof(startup);

    ZeroMemory(&pinfo, sizeof(pinfo));

    /* get path of current program */
    GetModuleFileName(NULL, program_path, sizeof(program_path));

    ret = CreateProcess(
            NULL,                                  /* program path */
            "tools\\android.bat update sdk",         /* command-line */
            NULL,                  /* process handle is not inheritable */
            NULL,                   /* thread handle is not inheritable */
            TRUE,                          /* yes, inherit some handles */
            CREATE_NO_WINDOW,                /* we don't want a console */
            NULL,                     /* use parent's environment block */
            NULL,                    /* use parent's starting directory */
            &startup,                 /* startup info, i.e. std handles */
            &pinfo);

    if (!ret) {
        DWORD err = GetLastError();
        fprintf(stderr, "CreateProcess failure, error %ld\n", err);

        LPSTR s;
        if (FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | /* dwFlags */
                          FORMAT_MESSAGE_FROM_SYSTEM,
                          NULL,                             /* lpSource */
                          err,                              /* dwMessageId */
                          0,                                /* dwLanguageId */
                          (LPSTR)&s,                        /* lpBuffer */
                          0,                                /* nSize */
                          NULL) != 0) {                     /* va_list args */
            fprintf(stderr, "%s", s);
            LocalFree(s);
        }

        return -1;
    }

    return 0;
}

int main(int argc, char **argv) {
    return sdk_launcher();
}

#endif /* _WIN32 */
