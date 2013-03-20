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

#include <portability.h>
#include <pthread.h>
#include <string.h>
#include <errno.h>
#include <errno_portable.h>

#define PORTABLE_TAG "errno_portable"
#include <log_portable.h>

#if ENAMETOOLONG==ENAMETOOLONG_PORTABLE
#error Bad build environment
#endif

__hidden int errno_ntop(int native_errno)
{
    switch (native_errno) {
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
    return native_errno;
}

__hidden int errno_pton(int portable_errno)
{
    switch (portable_errno) {
      case ENAMETOOLONG_PORTABLE: return ENAMETOOLONG;
      case ENOLCK_PORTABLE: return ENOLCK;
      case ENOSYS_PORTABLE: return ENOSYS;
      case ENOTEMPTY_PORTABLE: return ENOTEMPTY;
      case ELOOP_PORTABLE: return ELOOP;
      case EWOULDBLOCK_PORTABLE: return EWOULDBLOCK;
      case ENOMSG_PORTABLE: return ENOMSG;
      case EIDRM_PORTABLE: return EIDRM;
      case ECHRNG_PORTABLE: return ECHRNG;
      case EL2NSYNC_PORTABLE: return EL2NSYNC;
      case EL3HLT_PORTABLE: return EL3HLT;
      case EL3RST_PORTABLE: return EL3RST;
      case ELNRNG_PORTABLE: return ELNRNG;
      case EUNATCH_PORTABLE: return EUNATCH;
      case ENOCSI_PORTABLE: return ENOCSI;
      case EL2HLT_PORTABLE: return EL2HLT;
      case EBADE_PORTABLE: return EBADE;
      case EBADR_PORTABLE: return EBADR;
      case EXFULL_PORTABLE: return EXFULL;
      case ENOANO_PORTABLE: return ENOANO;
      case EBADRQC_PORTABLE: return EBADRQC;
      case EBADSLT_PORTABLE: return EBADSLT;
      case EDEADLOCK_PORTABLE: return EDEADLOCK;
      case EBFONT_PORTABLE: return EBFONT;
      case ENOSTR_PORTABLE: return ENOSTR;
      case ENODATA_PORTABLE: return ENODATA;
      case ETIME_PORTABLE: return ETIME;
      case ENOSR_PORTABLE: return ENOSR;
      case ENONET_PORTABLE: return ENONET;
      case ENOPKG_PORTABLE: return ENOPKG;
      case EREMOTE_PORTABLE: return EREMOTE;
      case ENOLINK_PORTABLE: return ENOLINK;
      case EADV_PORTABLE: return EADV;
      case ESRMNT_PORTABLE: return ESRMNT;
      case ECOMM_PORTABLE: return ECOMM;
      case EPROTO_PORTABLE: return EPROTO;
      case EMULTIHOP_PORTABLE: return EMULTIHOP;
      case EDOTDOT_PORTABLE: return EDOTDOT;
      case EBADMSG_PORTABLE: return EBADMSG;
      case EOVERFLOW_PORTABLE: return EOVERFLOW;
      case ENOTUNIQ_PORTABLE: return ENOTUNIQ;
      case EBADFD_PORTABLE: return EBADFD;
      case EREMCHG_PORTABLE: return EREMCHG;
      case ELIBACC_PORTABLE: return ELIBACC;
      case ELIBBAD_PORTABLE: return ELIBBAD;
      case ELIBSCN_PORTABLE: return ELIBSCN;
      case ELIBMAX_PORTABLE: return ELIBMAX;
      case ELIBEXEC_PORTABLE: return ELIBEXEC;
      case EILSEQ_PORTABLE: return EILSEQ;
      case ERESTART_PORTABLE: return ERESTART;
      case ESTRPIPE_PORTABLE: return ESTRPIPE;
      case EUSERS_PORTABLE: return EUSERS;
      case ENOTSOCK_PORTABLE: return ENOTSOCK;
      case EDESTADDRREQ_PORTABLE: return EDESTADDRREQ;
      case EMSGSIZE_PORTABLE: return EMSGSIZE;
      case EPROTOTYPE_PORTABLE: return EPROTOTYPE;
      case ENOPROTOOPT_PORTABLE: return ENOPROTOOPT;
      case EPROTONOSUPPORT_PORTABLE: return EPROTONOSUPPORT;
      case ESOCKTNOSUPPORT_PORTABLE: return ESOCKTNOSUPPORT;
      case EOPNOTSUPP_PORTABLE: return EOPNOTSUPP;
      case EPFNOSUPPORT_PORTABLE: return EPFNOSUPPORT;
      case EAFNOSUPPORT_PORTABLE: return EAFNOSUPPORT;
      case EADDRINUSE_PORTABLE: return EADDRINUSE;
      case EADDRNOTAVAIL_PORTABLE: return EADDRNOTAVAIL;
      case ENETDOWN_PORTABLE: return ENETDOWN;
      case ENETUNREACH_PORTABLE: return ENETUNREACH;
      case ENETRESET_PORTABLE: return ENETRESET;
      case ECONNABORTED_PORTABLE: return ECONNABORTED;
      case ECONNRESET_PORTABLE: return ECONNRESET;
      case ENOBUFS_PORTABLE: return ENOBUFS;
      case EISCONN_PORTABLE: return EISCONN;
      case ENOTCONN_PORTABLE: return ENOTCONN;
      case ESHUTDOWN_PORTABLE: return ESHUTDOWN;
      case ETOOMANYREFS_PORTABLE: return ETOOMANYREFS;
      case ETIMEDOUT_PORTABLE: return ETIMEDOUT;
      case ECONNREFUSED_PORTABLE: return ECONNREFUSED;
      case EHOSTDOWN_PORTABLE: return EHOSTDOWN;
      case EHOSTUNREACH_PORTABLE: return EHOSTUNREACH;
      case EALREADY_PORTABLE: return EALREADY;
      case EINPROGRESS_PORTABLE: return EINPROGRESS;
      case ESTALE_PORTABLE: return ESTALE;
      case EUCLEAN_PORTABLE: return EUCLEAN;
      case ENOTNAM_PORTABLE: return ENOTNAM;
      case ENAVAIL_PORTABLE: return ENAVAIL;
      case EISNAM_PORTABLE: return EISNAM;
      case EREMOTEIO_PORTABLE: return EREMOTEIO;
      case EDQUOT_PORTABLE: return EDQUOT;
      case ENOMEDIUM_PORTABLE: return ENOMEDIUM;
      case EMEDIUMTYPE_PORTABLE: return EMEDIUMTYPE;
      case ECANCELED_PORTABLE: return ECANCELED;
      case ENOKEY_PORTABLE: return ENOKEY;
      case EKEYEXPIRED_PORTABLE: return EKEYEXPIRED;
      case EKEYREVOKED_PORTABLE: return EKEYREVOKED;
      case EKEYREJECTED_PORTABLE: return EKEYREJECTED;
      case EOWNERDEAD_PORTABLE: return EOWNERDEAD;
      case ENOTRECOVERABLE_PORTABLE: return ENOTRECOVERABLE;
    }
    return portable_errno;
}

/* Key for the thread-specific portable errno */
static pthread_key_t errno_key;

/* Once-only initialisation of the key */
static pthread_once_t errno_key_once = PTHREAD_ONCE_INIT;

/* Free the thread-specific portable errno */
static void errno_key_destroy(void *buf)
{
    if (buf)
        free(buf);
}

/* Allocate the key */
static void errno_key_create(void)
{
    pthread_key_create(&errno_key, errno_key_destroy);
}

struct errno_state {
    int pshadow;                /* copy of last portable errno */
    int perrno;                 /* portable errno that may be modified by app */
};

/* Return the thread-specific portable errno */
static struct errno_state *errno_key_data(void)
{
    struct errno_state *data;
    static struct errno_state errno_state;

    pthread_once(&errno_key_once, errno_key_create);
    data = (struct errno_state *)pthread_getspecific(errno_key);
    if (data == NULL) {
        data = malloc(sizeof(struct errno_state));
        pthread_setspecific(errno_key, data);
    }
    if (data == NULL)
        data = &errno_state;
    return data;
}

/*
 * Attempt to return a thread specific location containnig the portable errno.
 * This can be assigned to without affecting the native errno. If the key
 * allocation fails fall back to using the native errno location.
 */
volatile int* WRAP(__errno)()
{
    struct errno_state *p;
    int save_errno;

    /* pthread_* calls may modify errno so use a copy */
    save_errno = *REAL(__errno)();

    p = errno_key_data();

    ALOGV(" ");
    ALOGV("%s(): { save_errno = errno:%d, (p:%p)->{pshadow:%d, perrno:%d}", __func__,
                   save_errno,             p,   p->pshadow, p->perrno);

    if (save_errno == 0 && p->pshadow != p->perrno) {
        /*
         * portable errno has changed but native hasn't
         * - copy portable error back to native
         */
        p->pshadow = p->perrno;
        save_errno = errno_pton(p->perrno);
    }
    else if (save_errno != 0 && p->pshadow == p->perrno) {
        /*
         * Native errno has changed but portable hasn't
         * - copy native error to portable.
         */
        p->pshadow = p->perrno = errno_ntop(save_errno);
        save_errno = 0;
    }
    else if (save_errno != 0 && p->pshadow != p->perrno) {
        /*
         * Both native and portable errno values have changed
         * so give priority to native errno
         * - copy native error to portable
         */
        p->pshadow = p->perrno = errno_ntop(save_errno);
        save_errno = 0;
    }

    ALOGV("%s: new save_errno:%d p:%p->{pshadow:%d, perrno:%d}", __func__,
                   save_errno,   p,  p->pshadow, p->perrno);

    *REAL(__errno)() = save_errno;

    ALOGV("%s: return (&p->perrno):%p; }", __func__, &p->perrno);

    /* return pointer to the modifiable portable errno value */
    return &p->perrno;
}


/* set portable errno */
void WRAP(__set_errno)(int portable_errno)
{
    struct errno_state *p;
    int save_errno;

    /* pthread_* calls may modify errno so use a copy */
    save_errno = *REAL(__errno)();

    p = errno_key_data();

    ALOGV("%s(): { save_errno = errno:%d, p:%p->{pshadow:%d, perrno:%d}", __func__,
                   save_errno,            p,  p->pshadow, p->perrno);

    p->pshadow = p->perrno = portable_errno;

    save_errno = errno_pton(portable_errno);

    ALOGV("%s: new save_errno:%d, p:%p->{pshadow:%d, perrno:%d}", __func__,
                   save_errno,    p,  p->pshadow, p->perrno);

    *REAL(__errno)() = save_errno;

    ALOGV("%s: return; }", __func__);
}

extern char* REAL(strerror)(int);
char *WRAP(strerror)(int errnum)
{
    return REAL(strerror)(errno_pton(errnum));
}

/* BSD style strerror_r */
int WRAP(strerror_r)(int errnum, char *buf, size_t buflen)
{
    return REAL(strerror_r)(errno_pton(errnum), buf, buflen);
}
