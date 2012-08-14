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

/* Derived from development/ndk/platforms/android-3/arch-arm/include/asm/ioctls.h */

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
//#define TIOCGPTN _IOR('T',0x30, unsigned int)
//#define TIOCSPTLCK _IOW('T',0x31, int)

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

#endif /* _IOCTLS_PORTABLE_H */
