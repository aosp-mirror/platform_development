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

#ifndef _IOCTLS_PORTABLE_H_
#define _IOCTLS_PORTABLE_H_

/*
 * Derived from development/ndk/platforms/android-3/include/asm-generic/ioctl.h
 */
#define _IOC_NRBITS_PORTABLE 8
#define _IOC_TYPEBITS_PORTABLE 8
#define _IOC_SIZEBITS_PORTABLE 14
#define _IOC_DIRBITS_PORTABLE 2

#define _IOC_NRMASK_PORTABLE ((1 << _IOC_NRBITS_PORTABLE)-1)
#define _IOC_TYPEMASK_PORTABLE ((1 << _IOC_TYPEBITS_PORTABLE)-1)
#define _IOC_SIZEMASK_PORTABLE ((1 << _IOC_SIZEBITS_PORTABLE)-1)
#define _IOC_DIRMASK_PORTABLE ((1 << _IOC_DIRBITS_PORTABLE)-1)

#define _IOC_NRSHIFT_PORTABLE 0
#define _IOC_TYPESHIFT_PORTABLE (_IOC_NRSHIFT_PORTABLE+_IOC_NRBITS_PORTABLE)
#define _IOC_SIZESHIFT_PORTABLE (_IOC_TYPESHIFT_PORTABLE+_IOC_TYPEBITS_PORTABLE)
#define _IOC_DIRSHIFT_PORTABLE (_IOC_SIZESHIFT_PORTABLE+_IOC_SIZEBITS_PORTABLE)

#define _IOC_NONE_PORTABLE 0U
#define _IOC_WRITE_PORTABLE 1U
#define _IOC_READ_PORTABLE 2U

#define _IOC_PORTABLE(dir, type, nr, size) (                                            \
    ((dir) << _IOC_DIRSHIFT_PORTABLE)    |                                              \
    ((type) << _IOC_TYPESHIFT_PORTABLE)  |                                              \
    ((nr) << _IOC_NRSHIFT_PORTABLE)      |                                              \
    ((size) << _IOC_SIZESHIFT_PORTABLE)                                                 \
)

extern unsigned int __invalid_size_argument_for_IOC;

#define _IOC_TYPECHECK_PORTABLE(t) (                                                    \
    (sizeof(t) == sizeof(t[1]) && sizeof(t) < (1 << _IOC_SIZEBITS_PORTABLE)) ?          \
    sizeof(t) :                                                                         \
    __invalid_size_argument_for_IOC                                                     \
)

#define _IO_PORTABLE(type, nr) _IOC_PORTABLE(_IOC_NONE_PORTABLE, (type), (nr), 0)

#define _IOR_PORTABLE(type, nr, size)                                                    \
    _IOC_PORTABLE(_IOC_READ_PORTABLE, (type), (nr), (_IOC_TYPECHECK_PORTABLE(size)))

#define _IOW_PORTABLE(type, nr, size)                                                    \
    _IOC_PORTABLE(_IOC_WRITE_PORTABLE, (type), (nr), (_IOC_TYPECHECK_PORTABLE(size)))

#define _IOWR_PORTABLE(type, nr, size)                                                   \
    IOC_PORTABLE(_IOC_READ_PORTABLE |                                                    \
                 _IOC_WRITE_PORTABLE, (type), (nr), (IOC_TYPECHECK_PORTABLE(size)) )


/*
 * Derived from development/ndk/platforms/android-3/arch-arm/include/asm/ioctls.h
 */
#define TCGETS_PORTABLE     0x5401
#define TCSETS_PORTABLE     0x5402
#define TCSETSW_PORTABLE    0x5403
#define TCSETSF_PORTABLE    0x5404
#define TCGETA_PORTABLE     0x5405
#define TCSETA_PORTABLE     0x5406
#define TCSETAW_PORTABLE    0x5407
#define TCSETAF_PORTABLE    0x5408
#define TCSBRK_PORTABLE     0x5409
#define TCXONC_PORTABLE     0x540A
#define TCFLSH_PORTABLE     0x540B
#define TIOCEXCL_PORTABLE   0x540C
#define TIOCNXCL_PORTABLE   0x540D
#define TIOCSCTTY_PORTABLE  0x540E
#define TIOCGPGRP_PORTABLE  0x540F
#define TIOCSPGRP_PORTABLE  0x5410
#define TIOCOUTQ_PORTABLE   0x5411
#define TIOCSTI_PORTABLE    0x5412
#define TIOCGWINSZ_PORTABLE 0x5413
#define TIOCSWINSZ_PORTABLE 0x5414
#define TIOCMGET_PORTABLE   0x5415
#define TIOCMBIS_PORTABLE   0x5416
#define TIOCMBIC_PORTABLE   0x5417
#define TIOCMSET_PORTABLE   0x5418
#define TIOCGSOFTCAR_PORTABLE   0x5419
#define TIOCSSOFTCAR_PORTABLE   0x541A
#define FIONREAD_PORTABLE   0x541B
#define TIOCINQ_PORTABLE    FIONREAD_PORTABLE
#define TIOCLINUX_PORTABLE  0x541C
#define TIOCCONS_PORTABLE   0x541D
#define TIOCGSERIAL_PORTABLE    0x541E
#define TIOCSSERIAL_PORTABLE    0x541F
#define TIOCPKT_PORTABLE    0x5420
#define FIONBIO_PORTABLE    0x5421
#define TIOCNOTTY_PORTABLE  0x5422
#define TIOCSETD_PORTABLE   0x5423
#define TIOCGETD_PORTABLE   0x5424
#define TCSBRKP_PORTABLE    0x5425
#define TIOCSBRK_PORTABLE   0x5427
#define TIOCCBRK_PORTABLE   0x5428
#define TIOCGSID_PORTABLE   0x5429
#define TIOCGPTN_PORTABLE _IOR_PORTABLE('T',0x30, unsigned int)
#define TIOCSPTLCK_PORTABLE _IOW_PORTABLE('T',0x31, int)

#define FIONCLEX_PORTABLE   0x5450
#define FIOCLEX_PORTABLE    0x5451
#define FIOASYNC_PORTABLE   0x5452
#define TIOCSERCONFIG_PORTABLE  0x5453
#define TIOCSERGWILD_PORTABLE   0x5454
#define TIOCSERSWILD_PORTABLE   0x5455
#define TIOCGLCKTRMIOS_PORTABLE 0x5456
#define TIOCSLCKTRMIOS_PORTABLE 0x5457
#define TIOCSERGSTRUCT_PORTABLE 0x5458
#define TIOCSERGETLSR_PORTABLE  0x5459
#define TIOCSERGETMULTI_PORTABLE 0x545A
#define TIOCSERSETMULTI_PORTABLE 0x545B

#define TIOCMIWAIT_PORTABLE     0x545C
#define TIOCGICOUNT_PORTABLE    0x545D
#define FIOQSIZE_PORTABLE       0x545E /* x86 differs here */

#define TIOCPKT_DATA_PORTABLE       0
#define TIOCPKT_FLUSHREAD_PORTABLE  1
#define TIOCPKT_FLUSHWRITE_PORTABLE 2
#define TIOCPKT_STOP_PORTABLE       4
#define TIOCPKT_START_PORTABLE      8
#define TIOCPKT_NOSTOP_PORTABLE     16
#define TIOCPKT_DOSTOP_PORTABLE     32

#define TIOCSER_TEMT_PORTABLE   0x01

/*
 * Derived from development/ndk/platforms/android-3/include/sys/ioctl_compat.h
 */
struct tchars_portable {
        char    t_intrc;        /* interrupt */
        char    t_quitc;        /* quit */
        char    t_startc;       /* start output */
        char    t_stopc;        /* stop output */
        char    t_eofc;         /* end-of-file */
        char    t_brkc;         /* input delimiter (like nl) */
};

struct ltchars_portable {
        char    t_suspc;        /* stop process signal */
        char    t_dsuspc;       /* delayed stop process signal */
        char    t_rprntc;       /* reprint line */
        char    t_flushc;       /* flush output (toggles) */
        char    t_werasc;       /* word erase */
        char    t_lnextc;       /* literal next character */
};

struct sgttyb_portable {
        char    sg_ispeed;      /* input speed */
        char    sg_ospeed;      /* output speed */
        char    sg_erase;       /* erase character */
        char    sg_kill;        /* kill character */
        short   sg_flags;       /* mode flags */
};

#ifdef USE_OLD_TTY
# define TIOCGETD_PORTABLE   _IOR_PORTABLE('t', 0, int)       /* get line discipline */
# define TIOCSETD_PORTABLE   _IOW_PORTABLE('t', 1, int)       /* set line discipline */
#else
# define OTIOCGETD_PORTABLE  _IOR_PORTABLE('t', 0, int)       /* get line discipline */
# define OTIOCSETD_PORTABLE  _IOW_PORTABLE('t', 1, int)       /* set line discipline */
#endif

/* hang up on last close */
#define TIOCHPCL_PORTABLE    _IO_PORTABLE('t', 2)

/* get parameters -- gtty */
#define TIOCGETP_PORTABLE    _IOR_PORTABLE('t', 8,struct sgttyb_portable)

/* set parameters -- stty */
#define TIOCSETP_PORTABLE    _IOW_PORTABLE('t', 9,struct sgttyb_portable)

/* as above, but no flushtty*/
#define TIOCSETN_PORTABLE    _IOW_PORTABLE('t',10,struct sgttyb_portable)

/* set special characters */
#define TIOCSETC_PORTABLE    _IOW_PORTABLE('t',17,struct tchars_portable)

/* get special characters */
#define TIOCGETC_PORTABLE    _IOR_PORTABLE('t',18,struct tchars_portable)

/* bis local mode bits */
#define TIOCLBIS_PORTABLE    _IOW_PORTABLE('t', 127, int)

/* bic local mode bits */
#define TIOCLBIC_PORTABLE    _IOW_PORTABLE('t', 126, int)

/* set entire local mode word */
#define TIOCLSET_PORTABLE    _IOW_PORTABLE('t', 125, int)

/* get local modes */
#define TIOCLGET_PORTABLE    _IOR_PORTABLE('t', 124, int)

/* set local special chars*/
#define TIOCSLTC_PORTABLE    _IOW_PORTABLE('t',117,struct ltchars_portable)

/* get local special chars*/
#define TIOCGLTC_PORTABLE    _IOR_PORTABLE('t',116,struct ltchars_portable)

 /* for hp300 -- sans int arg */
#define OTIOCCONS_PORTABLE   _IO_PORTABLE('t', 98)

/*
 * Derived from development/ndk/platforms/android-3/arch-arm/include/asm/sockios.h
 */
#define FIOSETOWN_PORTABLE 0x8901
#define SIOCSPGRP_PORTABLE 0x8902
#define FIOGETOWN_PORTABLE 0x8903
#define SIOCGPGRP_PORTABLE 0x8904
#define SIOCATMARK_PORTABLE 0x8905
#define SIOCGSTAMP_PORTABLE 0x8906

#endif /* _IOCTLS_PORTABLE_H */
