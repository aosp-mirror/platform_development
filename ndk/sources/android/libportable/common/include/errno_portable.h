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

#ifndef _ERRNO_PORTABLE_H_
#define _ERRNO_PORTABLE_H_

#include <portability.h>

/*
 * Derived from development/ndk/platforms/android-3/include/asm-generic/errno.h
 * NOTE:
 *   Base errno #defines from 1...35 are ARCH independent and not defined;
 *   they are defined in ./asm-generic/errno-base.h
 */
#define EDEADLK_PORTABLE 35
#define ENAMETOOLONG_PORTABLE 36
#define ENOLCK_PORTABLE 37
#define ENOSYS_PORTABLE 38
#define ENOTEMPTY_PORTABLE 39
#define ELOOP_PORTABLE 40
#define EWOULDBLOCK_PORTABLE 11 /* EAGAIN */
#define ENOMSG_PORTABLE 42
#define EIDRM_PORTABLE 43
#define ECHRNG_PORTABLE 44
#define EL2NSYNC_PORTABLE 45
#define EL3HLT_PORTABLE 46
#define EL3RST_PORTABLE 47
#define ELNRNG_PORTABLE 48
#define EUNATCH_PORTABLE 49
#define ENOCSI_PORTABLE 50
#define EL2HLT_PORTABLE 51
#define EBADE_PORTABLE 52
#define EBADR_PORTABLE 53
#define EXFULL_PORTABLE 54
#define ENOANO_PORTABLE 55
#define EBADRQC_PORTABLE 56
#define EBADSLT_PORTABLE 57

#define EDEADLOCK_PORTABLE EDEADLK_PORTABLE

#define EBFONT_PORTABLE 59
#define ENOSTR_PORTABLE 60
#define ENODATA_PORTABLE 61
#define ETIME_PORTABLE 62
#define ENOSR_PORTABLE 63
#define ENONET_PORTABLE 64
#define ENOPKG_PORTABLE 65
#define EREMOTE_PORTABLE 66
#define ENOLINK_PORTABLE 67
#define EADV_PORTABLE 68
#define ESRMNT_PORTABLE 69
#define ECOMM_PORTABLE 70
#define EPROTO_PORTABLE 71
#define EMULTIHOP_PORTABLE 72
#define EDOTDOT_PORTABLE 73
#define EBADMSG_PORTABLE 74
#define EOVERFLOW_PORTABLE 75
#define ENOTUNIQ_PORTABLE 76
#define EBADFD_PORTABLE 77
#define EREMCHG_PORTABLE 78
#define ELIBACC_PORTABLE 79
#define ELIBBAD_PORTABLE 80
#define ELIBSCN_PORTABLE 81
#define ELIBMAX_PORTABLE 82
#define ELIBEXEC_PORTABLE 83
#define EILSEQ_PORTABLE 84
#define ERESTART_PORTABLE 85
#define ESTRPIPE_PORTABLE 86
#define EUSERS_PORTABLE 87
#define ENOTSOCK_PORTABLE 88
#define EDESTADDRREQ_PORTABLE 89
#define EMSGSIZE_PORTABLE 90
#define EPROTOTYPE_PORTABLE 91
#define ENOPROTOOPT_PORTABLE 92
#define EPROTONOSUPPORT_PORTABLE 93
#define ESOCKTNOSUPPORT_PORTABLE 94
#define EOPNOTSUPP_PORTABLE 95
#define EPFNOSUPPORT_PORTABLE 96
#define EAFNOSUPPORT_PORTABLE 97
#define EADDRINUSE_PORTABLE 98
#define EADDRNOTAVAIL_PORTABLE 99
#define ENETDOWN_PORTABLE 100
#define ENETUNREACH_PORTABLE 101
#define ENETRESET_PORTABLE 102
#define ECONNABORTED_PORTABLE 103
#define ECONNRESET_PORTABLE 104
#define ENOBUFS_PORTABLE 105
#define EISCONN_PORTABLE 106
#define ENOTCONN_PORTABLE 107
#define ESHUTDOWN_PORTABLE 108
#define ETOOMANYREFS_PORTABLE 109
#define ETIMEDOUT_PORTABLE 110
#define ECONNREFUSED_PORTABLE 111
#define EHOSTDOWN_PORTABLE 112
#define EHOSTUNREACH_PORTABLE 113
#define EALREADY_PORTABLE 114
#define EINPROGRESS_PORTABLE 115
#define ESTALE_PORTABLE 116
#define EUCLEAN_PORTABLE 117
#define ENOTNAM_PORTABLE 118
#define ENAVAIL_PORTABLE 119
#define EISNAM_PORTABLE 120
#define EREMOTEIO_PORTABLE 121
#define EDQUOT_PORTABLE 122

#define ENOMEDIUM_PORTABLE 123
#define EMEDIUMTYPE_PORTABLE 124
#define ECANCELED_PORTABLE 125
#define ENOKEY_PORTABLE 126
#define EKEYEXPIRED_PORTABLE 127
#define EKEYREVOKED_PORTABLE 128
#define EKEYREJECTED_PORTABLE 129

#define EOWNERDEAD_PORTABLE 130
#define ENOTRECOVERABLE_PORTABLE 131

extern __hidden int errno_ntop(int native_errno);
extern __hidden int errno_pton(int native_errno);

extern volatile int*   REAL(__errno)(void);

#endif /* _ERRNO_PORTABLE_H */
