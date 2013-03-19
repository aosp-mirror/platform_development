/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _FILEFD_PORTABLE_H_
#define _FILEFD_PORTABLE_H_

/*
 * Maintaining a list of special file descriptors in lib-portable
 * which are maintained across a execve() via environment variables.
 * See portable/arch-mips/filefd.c for details.
 */
enum filefd_type {
    UNUSED_FD_TYPE = 0,
    EVENT_FD_TYPE,
    INOTIFY_FD_TYPE,
    SIGNAL_FD_TYPE,
    TIMER_FD_TYPE,
    MAX_FD_TYPE
};

extern __hidden void filefd_opened(int fd, enum filefd_type fd_type);
extern __hidden void filefd_closed(int fd);
extern __hidden void filefd_CLOEXEC_enabled(int fd);
extern __hidden void filefd_CLOEXEC_disabled(int fd);
extern __hidden void filefd_disable_mapping(void);

extern int WRAP(close)(int fd);
extern int WRAP(read)(int fd, void *buf, size_t count);
extern int WRAP(pipe2)(int pipefd[2], int portable_flags);

#endif /* _FILEFD_PORTABLE_H_ */
