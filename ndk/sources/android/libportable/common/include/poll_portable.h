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

#ifndef _POLL_PORTABLE_H_
#define _POLL_PORTABLE_H_

/* Derived from development/ndk/platforms/android-3/arch-arm/include/asm/poll.h */

#define POLLIN_PORTABLE 0x0001
#define POLLPRI_PORTABLE 0x0002
#define POLLOUT_PORTABLE 0x0004
#define POLLERR_PORTABLE 0x0008
#define POLLHUP_PORTABLE 0x0010
#define POLLNVAL_PORTABLE 0x0020

#define POLLRDNORM_PORTABLE 0x0040
#define POLLRDBAND_PORTABLE 0x0080
#define POLLWRNORM_PORTABLE 0x0100
#define POLLWRBAND_PORTABLE 0x0200
#define POLLMSG_PORTABLE 0x0400
#define POLLREMOVE_PORTABLE 0x1000
#define POLLRDHUP_PORTABLE 0x2000

#endif /* _POLL_PORTABLE_H_ */
