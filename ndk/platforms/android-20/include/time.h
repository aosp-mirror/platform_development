/*
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

#ifndef _TIME_H_
#define _TIME_H_

#include <sys/cdefs.h>
#include <sys/time.h>

__BEGIN_DECLS

#define CLOCKS_PER_SEC 1000000

extern char* tzname[];
extern int daylight;
extern long int timezone;

struct sigevent;

struct tm {
  int tm_sec;
  int tm_min;
  int tm_hour;
  int tm_mday;
  int tm_mon;
  int tm_year;
  int tm_wday;
  int tm_yday;
  int tm_isdst;
  long int tm_gmtoff;
  const char* tm_zone;
};

#define TM_ZONE tm_zone

extern time_t time(time_t*);
extern int nanosleep(const struct timespec*, struct timespec*);

extern char* asctime(const struct tm*);
extern char* asctime_r(const struct tm*, char*);

extern double difftime(time_t, time_t);
extern time_t mktime(struct tm*);

extern struct tm* localtime(const time_t*);
extern struct tm* localtime_r(const time_t*, struct tm*);

extern struct tm* gmtime(const time_t*);
extern struct tm* gmtime_r(const time_t*, struct tm*);

extern char* strptime(const char*, const char*, struct tm*);
extern size_t strftime(char*, size_t, const char*, const struct tm*);

extern char* ctime(const time_t*);
extern char* ctime_r(const time_t*, char*);

extern void tzset(void);

extern clock_t clock(void);

extern int clock_getres(int, struct timespec*);
extern int clock_gettime(int, struct timespec*);

extern int timer_create(int, struct sigevent*, timer_t*);
extern int timer_delete(timer_t);
extern int timer_settime(timer_t, int, const struct itimerspec*, struct itimerspec*);
extern int timer_gettime(timer_t, struct itimerspec*);
extern int timer_getoverrun(timer_t);

extern time_t timelocal(struct tm*);
extern time_t timegm(struct tm*);
extern time_t time2posix(time_t);
extern time_t posix2time(time_t);

__END_DECLS

#endif /* _TIME_H_ */
