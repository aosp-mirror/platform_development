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

#include <errno.h>
#include <errno_portable.h>

#if ENAMETOOLONG==ENAMETOOLONG_PORTABLE
#error Bad build environment
#endif

static inline int mips_change_errno(int mips_errno)
{
    switch (mips_errno) {
      case ENAMETOOLONG: return ENAMETOOLONG_PORTABLE;
      case ENOLCK: return ENOLCK_PORTABLE;
      case ENOSYS: return ENOSYS_PORTABLE;
      case ENOTEMPTY: return ENOTEMPTY_PORTABLE;
      case ELOOP: return ELOOP_PORTABLE;
      case EWOULDBLOCK: return EWOULDBLOCK_PORTABLE;
      case ENOMSG: return ENOMSG_PORTABLE;
      case EIDRM: return EIDRM_PORTABLE;
      case ECHRNG: return ECHRNG_PORTABLE;
      case EL2NSYNC: return EL2NSYNC_PORTABLE;
      case EL3HLT: return EL3HLT_PORTABLE;
      case EL3RST: return EL3RST_PORTABLE;
      case ELNRNG: return ELNRNG_PORTABLE;
      case EUNATCH: return EUNATCH_PORTABLE;
      case ENOCSI: return ENOCSI_PORTABLE;
      case EL2HLT: return EL2HLT_PORTABLE;
      case EBADE: return EBADE_PORTABLE;
      case EBADR: return EBADR_PORTABLE;
      case EXFULL: return EXFULL_PORTABLE;
      case ENOANO: return ENOANO_PORTABLE;
      case EBADRQC: return EBADRQC_PORTABLE;
      case EBADSLT: return EBADSLT_PORTABLE;
      case EDEADLOCK: return EDEADLOCK_PORTABLE;
      case EBFONT: return EBFONT_PORTABLE;
      case ENOSTR: return ENOSTR_PORTABLE;
      case ENODATA: return ENODATA_PORTABLE;
      case ETIME: return ETIME_PORTABLE;
      case ENOSR: return ENOSR_PORTABLE;
      case ENONET: return ENONET_PORTABLE;
      case ENOPKG: return ENOPKG_PORTABLE;
      case EREMOTE: return EREMOTE_PORTABLE;
      case ENOLINK: return ENOLINK_PORTABLE;
      case EADV: return EADV_PORTABLE;
      case ESRMNT: return ESRMNT_PORTABLE;
      case ECOMM: return ECOMM_PORTABLE;
      case EPROTO: return EPROTO_PORTABLE;
      case EMULTIHOP: return EMULTIHOP_PORTABLE;
      case EDOTDOT: return EDOTDOT_PORTABLE;
      case EBADMSG: return EBADMSG_PORTABLE;
      case EOVERFLOW: return EOVERFLOW_PORTABLE;
      case ENOTUNIQ: return ENOTUNIQ_PORTABLE;
      case EBADFD: return EBADFD_PORTABLE;
      case EREMCHG: return EREMCHG_PORTABLE;
      case ELIBACC: return ELIBACC_PORTABLE;
      case ELIBBAD: return ELIBBAD_PORTABLE;
      case ELIBSCN: return ELIBSCN_PORTABLE;
      case ELIBMAX: return ELIBMAX_PORTABLE;
      case ELIBEXEC: return ELIBEXEC_PORTABLE;
      case EILSEQ: return EILSEQ_PORTABLE;
      case ERESTART: return ERESTART_PORTABLE;
      case ESTRPIPE: return ESTRPIPE_PORTABLE;
      case EUSERS: return EUSERS_PORTABLE;
      case ENOTSOCK: return ENOTSOCK_PORTABLE;
      case EDESTADDRREQ: return EDESTADDRREQ_PORTABLE;
      case EMSGSIZE: return EMSGSIZE_PORTABLE;
      case EPROTOTYPE: return EPROTOTYPE_PORTABLE;
      case ENOPROTOOPT: return ENOPROTOOPT_PORTABLE;
      case EPROTONOSUPPORT: return EPROTONOSUPPORT_PORTABLE;
      case ESOCKTNOSUPPORT: return ESOCKTNOSUPPORT_PORTABLE;
      case EOPNOTSUPP: return EOPNOTSUPP_PORTABLE;
      case EPFNOSUPPORT: return EPFNOSUPPORT_PORTABLE;
      case EAFNOSUPPORT: return EAFNOSUPPORT_PORTABLE;
      case EADDRINUSE: return EADDRINUSE_PORTABLE;
      case EADDRNOTAVAIL: return EADDRNOTAVAIL_PORTABLE;
      case ENETDOWN: return ENETDOWN_PORTABLE;
      case ENETUNREACH: return ENETUNREACH_PORTABLE;
      case ENETRESET: return ENETRESET_PORTABLE;
      case ECONNABORTED: return ECONNABORTED_PORTABLE;
      case ECONNRESET: return ECONNRESET_PORTABLE;
      case ENOBUFS: return ENOBUFS_PORTABLE;
      case EISCONN: return EISCONN_PORTABLE;
      case ENOTCONN: return ENOTCONN_PORTABLE;
      case ESHUTDOWN: return ESHUTDOWN_PORTABLE;
      case ETOOMANYREFS: return ETOOMANYREFS_PORTABLE;
      case ETIMEDOUT: return ETIMEDOUT_PORTABLE;
      case ECONNREFUSED: return ECONNREFUSED_PORTABLE;
      case EHOSTDOWN: return EHOSTDOWN_PORTABLE;
      case EHOSTUNREACH: return EHOSTUNREACH_PORTABLE;
      case EALREADY: return EALREADY_PORTABLE;
      case EINPROGRESS: return EINPROGRESS_PORTABLE;
      case ESTALE: return ESTALE_PORTABLE;
      case EUCLEAN: return EUCLEAN_PORTABLE;
      case ENOTNAM: return ENOTNAM_PORTABLE;
      case ENAVAIL: return ENAVAIL_PORTABLE;
      case EISNAM: return EISNAM_PORTABLE;
      case EREMOTEIO: return EREMOTEIO_PORTABLE;
      case EDQUOT: return EDQUOT_PORTABLE;
      case ENOMEDIUM: return ENOMEDIUM_PORTABLE;
      case EMEDIUMTYPE: return EMEDIUMTYPE_PORTABLE;
      case ECANCELED: return ECANCELED_PORTABLE;
      case ENOKEY: return ENOKEY_PORTABLE;
      case EKEYEXPIRED: return EKEYEXPIRED_PORTABLE;
      case EKEYREVOKED: return EKEYREVOKED_PORTABLE;
      case EKEYREJECTED: return EKEYREJECTED_PORTABLE;
      case EOWNERDEAD: return EOWNERDEAD_PORTABLE;
      case ENOTRECOVERABLE: return ENOTRECOVERABLE_PORTABLE;
    }
    return mips_errno;
}

extern volatile int*   __errno(void);
volatile int* __errno_portable()
{
  /* Note that writing to static_errno will not affect the underlying system. */
  static int static_errno;
  static_errno = mips_change_errno(*__errno());
  return &static_errno;
}
