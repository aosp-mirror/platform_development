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

#include <poll.h>
#include <poll_portable.h>

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
    /* MIPS POLLWRNORM equals POLLOUT that is the same as POLLOUT_PORTABLE, so we just update POLLWRBNAD_PORTABLE. */
    if (mips_events & POLLWRBAND) {
        mips_events &= ~POLLWRBAND;
        mips_events |= POLLWRBAND_PORTABLE;
    }

    return mips_events;
}

extern int poll(struct pollfd *, nfds_t, long);

int poll_portable(struct pollfd *fds, nfds_t nfds, long timeout)
{
  nfds_t i;
  int ret;

  for (i = 0; i < nfds; i++)
      fds->events = mips_change_portable_events(fds->events);

  ret = poll(fds, nfds, timeout);

  for (i = 0; i < nfds; i++) {
      fds->events = change_mips_events(fds->events);
      fds->revents = change_mips_events(fds->revents);
  }

  return ret;
}
