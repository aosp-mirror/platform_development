/*
 *  Derived from Goldfish include/linux/signalfd.h
 *
 *  Copyright (C) 2007  Davide Libenzi <davidel@xmailserver.org>
 *
 */

#ifndef _LINUX_SIGNALFD_PORTABLE_H
#define _LINUX_SIGNALFD_PORTABLE_H

#include <linux/types.h>
#include <fcntl.h>

/* Flags for signalfd4.  */
#define SFD_CLOEXEC             O_CLOEXEC
#define SFD_NONBLOCK            O_NONBLOCK

/* For O_CLOEXEC_PORTABLE and O_NONBLOCK_PORTABLE */
#include "fcntl_portable.h"

#define SFD_CLOEXEC_PORTABLE    O_CLOEXEC_PORTABLE
#define SFD_NONBLOCK_PORTABLE   O_NONBLOCK_PORTABLE

/*
 * This structure is the same for Native and Portable.
 * However for MIPS ssi_signo and ssi_errno differ in their
 * values and need to be mapped.
 */
struct signalfd_siginfo {
        __u32 ssi_signo;
        __s32 ssi_errno;
        __s32 ssi_code;
        __u32 ssi_pid;
        __u32 ssi_uid;
        __s32 ssi_fd;
        __u32 ssi_tid;
        __u32 ssi_band;
        __u32 ssi_overrun;
        __u32 ssi_trapno;
        __s32 ssi_status;
        __s32 ssi_int;
        __u64 ssi_ptr;
        __u64 ssi_utime;
        __u64 ssi_stime;
        __u64 ssi_addr;

        /*
         * Pad structure to 128 bytes. Remember to update the
         * pad size when you add new members. We use a fixed
         * size structure to avoid compatibility problems with
         * future versions, and we leave extra space for additional
         * members. We use fixed size members because this structure
         * comes out of a read(2) and we really don't want to have
         * a compat (sp?) on read(2).
         */
        __u8 __pad[48];
};

#endif /* _LINUX_SIGNALFD_PORTABLE_H */

