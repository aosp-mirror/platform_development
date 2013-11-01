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
#include <poll.h>
#include <poll_portable.h>

/*
 *_XOPEN_SOURCE added the ability to not only poll for data coming in or out
 * but now also the ability to poll for high priority input and output. Though
 * the normal priority is equivalent to the original I/O it was assigned new bits:
 *       POLLIN  Equivalent to POLLRDNORM
 *       POLLOUT Equivalent to POLLWRNORM
 *
 * The Linux kernel sets both POLLIN and POLLRDNORM when data is available and sets
 * both POLLOUT and POLLWRNORM when data can be written; so the new priority BAND bits
 * just supplement the meaning of the prior POLLIN and POLLOUT bits as well as the
 * new POLLRDNORM and POLLWRNORM bits.
 *
 * The DECNet Protocol can set the poll in  priority flag, POLLRDBAND.
 * ATM as well as a whole bunch of other protocols can set the poll out priority flag,
 * POLLWRBAND.
 *
 * MIPS and SPARC likely assigned the new XOPEN poll out event flags in UNIX well before
 * UNIX was ported to X86.  It appears that Intel chose different bits and that was
 * established by Linus as the the generic case and later also chosen by ARM.
 *
 *     POLLWRNORM:0x100 -  MIPS used POLLOUT:0x0004, which is equivalent in meaning.
 *
 *     POLLWRBAND:0x200 -  MIPS used 0x0100. which is POLLWRNORM:0x100.
 *
 * Summary:
 * ========
 *    Both Normal and Priority flags can be mapped to MIPS flags (left to right below).
 *    Only the Priority poll out flag can be mapped back to portable because MIPS
 *    is using the same number as POLLOUT for POLLWRNORM (right to left below).
 *
 *                    ARM/GENERIC/PORTABLE           MIPS
 *                    ====================          ======
 *      POLLIN          0x0001                      0x0001
 *      POLLPRI         0x0002                      0x0002
 *      POLLOUT         0x0004 <-----+              0x0004
 *      POLLERR         0x0008        \             0x0008
 *      POLLHUP         0x0010         \            0x0010
 *      POLLNVAL        0x0020          \           0x0020
 *      POLLRDNORM      0x0040           \          0x0040
 *      POLLRDBAND      0x0080            \         0x0080
 *      POLLWRNORM      0x0100  -----------+<---->  0x0004
 *      POLLWRBAND      0x0200 <----------------->  0x0100
 *      POLLMSG         0x0400                      0x0400
 *      POLLREMOVE      0x1000                      0x1000
 *      POLLRDHUP       0x2000                      0x2000
 *
 *  The loss of the high priority notice for the polling
 *  of output data is likely minor as it was only being used
 *  in DECNet. Also, the poll system call and device poll
 *  implementations processes POLLOUT and POLLWRNORM event
 *  flags the same.
 */

#if POLLWRNORM_PORTABLE==POLLWRNORM
#error Bad build environment
#endif

static inline short mips_change_portable_events(short portable_events)
{
    /* MIPS has different POLLWRNORM and POLLWRBAND. */
    if (portable_events & POLLWRNORM_PORTABLE) {
        portable_events &= ~POLLWRNORM_PORTABLE;
        portable_events |= POLLWRNORM;
    }
    if (portable_events & POLLWRBAND_PORTABLE) {
        portable_events &= ~POLLWRBAND_PORTABLE;
        portable_events |= POLLWRBAND;
    }

    return portable_events;
}

static inline short change_mips_events(short mips_events)
{
    /*
     * MIPS POLLWRNORM equals MIPS POLLOUT, which is the same as POLLOUT_PORTABLE;
     * so we just map POLLWRBAND to POLLWRBAND_PORTABLE.
     */
    if (mips_events & POLLWRBAND) {
        mips_events &= ~POLLWRBAND;
        mips_events |= POLLWRBAND_PORTABLE;
    }

    return mips_events;
}

extern int poll(struct pollfd *, nfds_t, int);

int WRAP(poll)(struct pollfd *fds, nfds_t nfds, int timeout)
{
  nfds_t i;
  int ret;

  for (i = 0; i < nfds; i++)
      fds->events = mips_change_portable_events(fds->events);

  ret = REAL(poll)(fds, nfds, timeout);

  for (i = 0; i < nfds; i++) {
      fds->events = change_mips_events(fds->events);
      fds->revents = change_mips_events(fds->revents);
  }

  return ret;
}
