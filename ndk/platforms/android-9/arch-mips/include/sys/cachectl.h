#ifndef _SYS_CACHECTL_H
#define _SYS_CACHECTL_H 1

#ifdef __mips__
#include <asm/cachectl.h>
extern int __cachectl (void *addr, __const int nbytes, __const int op);
extern int _flush_cache (char *addr, __const int nbytes, __const int op);
#endif

#endif /* sys/cachectl.h */
