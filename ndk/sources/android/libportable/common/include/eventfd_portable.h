/*
 * Derived from bionic/libc/include/sys/eventfd.h
 *
 * Copyright (C) 2008 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
#ifndef _SYS_EVENTFD_PORTABLE_H
#define _SYS_EVENTFD_PORTABLE_H

#include <portability.h>
#include <sys/cdefs.h>
#include <fcntl_portable.h>

__BEGIN_DECLS

/*
 * EFD_SEMAPHORE is defined in recent linux kernels;
 * but isn't mentioned elsewhere. See linux 3.4
 * include/linux/eventfd.h for example.
 */
#define EFD_SEMAPHORE           (1 << 0)

#define EFD_SEMAPHORE_PORTABLE  EFD_SEMAPHORE
#define EFD_CLOEXEC_PORTABLE    O_CLOEXEC_PORTABLE
#define EFD_NONBLOCK_PORTABLE   O_NONBLOCK_PORTABLE

/* type of event counter */
typedef uint64_t  eventfd_portable_t;

extern int WRAP(eventfd)(unsigned int initval, int flags);

#if 0
/* Compatibility with GLibc; libportable versions don't appear to be necessary */
extern int eventfd_read(int fd, eventfd_t *counter);
extern int eventfd_write(int fd, const eventfd_t counter);
#endif

__END_DECLS

#endif /* _SYS_EVENTFD_H */
