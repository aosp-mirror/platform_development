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
#include <stdarg.h>
#include <sys/ioctl.h>
#include <ioctls_portable.h>
#include <termios.h>
#include <linux/sockios.h>

#if FIONREAD_PORTABLE==FIONREAD
#error Bad build environment
#endif

static inline int mips_change_request(int request)
{
    switch(request) {
    case TCGETS_PORTABLE:
        return TCGETS;
    case TCSETS_PORTABLE:
        return TCSETS;
    case TCSETSW_PORTABLE:
        return TCSETSW;
    case TCSETSF_PORTABLE:
        return TCSETSF;
    case TCGETA_PORTABLE:
        return TCGETA;
    case TCSETA_PORTABLE:
        return TCSETA;
    case TCSETAW_PORTABLE:
        return TCSETAW;
    case TCSETAF_PORTABLE:
        return TCSETAF;
    case TCSBRK_PORTABLE:
        return TCSBRK;
    case TCXONC_PORTABLE:
        return TCXONC;
    case TCFLSH_PORTABLE:
        return TCFLSH;
    case TIOCEXCL_PORTABLE:
        return TIOCEXCL;
    case TIOCNXCL_PORTABLE:
        return TIOCNXCL;
    case TIOCSCTTY_PORTABLE:
        return TIOCSCTTY;
    case TIOCGPGRP_PORTABLE:
        return TIOCGPGRP;
    case TIOCSPGRP_PORTABLE:
        return TIOCSPGRP;
    case TIOCOUTQ_PORTABLE:
        return TIOCOUTQ;
    case TIOCSTI_PORTABLE:
        return TIOCSTI;
    case TIOCGWINSZ_PORTABLE:
        return TIOCGWINSZ;
    case TIOCSWINSZ_PORTABLE:
        return TIOCSWINSZ;
    case TIOCMGET_PORTABLE:
        return TIOCMGET;
    case TIOCMBIS_PORTABLE:
        return TIOCMBIS;
    case TIOCMBIC_PORTABLE:
        return TIOCMBIC;
    case TIOCMSET_PORTABLE:
        return TIOCMSET;
    case TIOCGSOFTCAR_PORTABLE:
        return TIOCGSOFTCAR;
    case TIOCSSOFTCAR_PORTABLE:
        return TIOCSSOFTCAR;
    case FIONREAD_PORTABLE:
        return FIONREAD;
    /* case TIOCINQ_PORTABLE: // same as FIONREAD_PORTABLE
        return TIOCINQ; */
    case TIOCLINUX_PORTABLE:
        return TIOCLINUX;
    case TIOCCONS_PORTABLE:
        return TIOCCONS;
    case TIOCGSERIAL_PORTABLE:
        return TIOCGSERIAL;
    case TIOCSSERIAL_PORTABLE:
        return TIOCSSERIAL;
    case TIOCPKT_PORTABLE:
        return TIOCPKT;
    case FIONBIO_PORTABLE:
        return FIONBIO;
    case TIOCNOTTY_PORTABLE:
        return TIOCNOTTY;
    case TIOCSETD_PORTABLE:
        return TIOCSETD;
    case TIOCGETD_PORTABLE:
        return TIOCGETD;
    case TCSBRKP_PORTABLE:
        return TCSBRKP;
    case TIOCSBRK_PORTABLE:
        return TIOCSBRK;
    case TIOCCBRK_PORTABLE:
        return TIOCCBRK;
    case TIOCGSID_PORTABLE:
        return TIOCGSID;
    case FIONCLEX_PORTABLE:
        return FIONCLEX;
    case FIOCLEX_PORTABLE:
        return FIOCLEX;
    case FIOASYNC_PORTABLE:
        return FIOASYNC;
    case TIOCSERCONFIG_PORTABLE:
        return TIOCSERCONFIG;
    case TIOCSERGWILD_PORTABLE:
        return TIOCSERGWILD;
    case TIOCSERSWILD_PORTABLE:
        return TIOCSERSWILD;
    case TIOCGLCKTRMIOS_PORTABLE:
        return TIOCGLCKTRMIOS;
    case TIOCSLCKTRMIOS_PORTABLE:
        return TIOCSLCKTRMIOS;
    case TIOCSERGSTRUCT_PORTABLE:
        return TIOCSERGSTRUCT;
    case TIOCSERGETLSR_PORTABLE:
        return TIOCSERGETLSR;
    case TIOCSERGETMULTI_PORTABLE:
        return TIOCSERGETMULTI;
    case TIOCSERSETMULTI_PORTABLE:
        return TIOCSERSETMULTI;
    case TIOCMIWAIT_PORTABLE:
        return TIOCMIWAIT;
    case TIOCGICOUNT_PORTABLE:
        return TIOCGICOUNT;
    case FIOQSIZE_PORTABLE:
        return FIOQSIZE;
    case TIOCPKT_DATA_PORTABLE:
        return TIOCPKT_DATA;
    case TIOCPKT_FLUSHREAD_PORTABLE:
        return TIOCPKT_FLUSHREAD;
    case TIOCPKT_FLUSHWRITE_PORTABLE:
        return TIOCPKT_FLUSHWRITE;
    case TIOCPKT_STOP_PORTABLE:
        return TIOCPKT_STOP;
    case TIOCPKT_START_PORTABLE:
        return TIOCPKT_START;
    case TIOCPKT_NOSTOP_PORTABLE:
        return TIOCPKT_NOSTOP;
    case TIOCPKT_DOSTOP_PORTABLE:
        return TIOCPKT_DOSTOP;
    /* case TIOCSER_TEMT_PORTABLE: // = 1 same as TIOCPKT_FLUSHREAD_PORTABLE
        return TIOCSER_TEMT; */
    case TIOCGPTN_PORTABLE:
        return TIOCGPTN;
    case TIOCSPTLCK_PORTABLE:
        return TIOCSPTLCK;
#ifdef USE_OLD_TTY
    case TIOCGETD_PORTABLE:
        return TIOCGETD;
    case TIOCSETD_PORTABLE:
        return TIOCSETD;
#else
    case OTIOCGETD_PORTABLE:
        return OTIOCGETD;
    case OTIOCSETD_PORTABLE:
        return OTIOCSETD;
#endif
    case TIOCHPCL_PORTABLE:
        return TIOCHPCL;
    case TIOCGETP_PORTABLE:
        return TIOCGETP;
    case TIOCSETP_PORTABLE:
        return TIOCSETP;
    case TIOCSETN_PORTABLE:
        return TIOCSETN;
    case TIOCSETC_PORTABLE:
        return TIOCSETC;
    case TIOCGETC_PORTABLE:
        return TIOCGETC;
    case TIOCLBIS_PORTABLE:
        return TIOCLBIS;
    case TIOCLBIC_PORTABLE:
        return TIOCLBIC;
    case TIOCLSET_PORTABLE:
        return TIOCLSET;
    case TIOCLGET_PORTABLE:
        return TIOCLGET;
    case TIOCSLTC_PORTABLE:
        return TIOCSLTC;
    case TIOCGLTC_PORTABLE:
        return TIOCGLTC;
    case OTIOCCONS_PORTABLE:
        return OTIOCCONS;
    case FIOSETOWN_PORTABLE:
        return FIOSETOWN;
    case SIOCSPGRP_PORTABLE:
        return SIOCSPGRP;
    case FIOGETOWN_PORTABLE:
        return FIOGETOWN;
    case SIOCGPGRP_PORTABLE:
        return SIOCGPGRP;
    case SIOCATMARK_PORTABLE:
        return SIOCATMARK;
    case SIOCGSTAMP_PORTABLE:
        return SIOCGSTAMP;
    }
    return request;
}

extern int __ioctl(int, int, void *);
int WRAP(ioctl)(int fd, int request, ...)
{
    va_list ap;
    void * arg;

    va_start(ap, request);
    arg = va_arg(ap, void *);
    va_end(ap);

    return __ioctl(fd, mips_change_request(request), arg);
}
