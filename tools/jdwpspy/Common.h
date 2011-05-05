/*
 * Copyright 2006 The Android Open Source Project
 *
 * jdwpspy common stuff.
 */
#ifndef _JDWPSPY_COMMON
#define _JDWPSPY_COMMON

#include <stdio.h>
#include <sys/types.h>

typedef unsigned char u1;
typedef unsigned short u2;
typedef unsigned int u4;
typedef unsigned long long u8;

#define NELEM(x) (sizeof(x) / sizeof((x)[0]))

#ifndef _JDWP_MISC_INLINE
# define INLINE extern inline
#else
# define INLINE
#endif

/*
 * Get 1 byte.  (Included to make the code more legible.)
 */
INLINE u1 get1(unsigned const char* pSrc)
{
    return *pSrc;
}

/*
 * Get 2 big-endian bytes.
 */
INLINE u2 get2BE(unsigned char const* pSrc)
{
    u2 result;

    result = *pSrc++ << 8;
    result |= *pSrc++;

    return result;
}

/*
 * Get 4 big-endian bytes.
 */
INLINE u4 get4BE(unsigned char const* pSrc)
{
    u4 result;

    result = *pSrc++ << 24;
    result |= *pSrc++ << 16;
    result |= *pSrc++ << 8;
    result |= *pSrc++;

    return result;
}

/*
 * Get 8 big-endian bytes.
 */
INLINE u8 get8BE(unsigned char const* pSrc)
{
    u8 result;

    result = (u8) *pSrc++ << 56;
    result |= (u8) *pSrc++ << 48;
    result |= (u8) *pSrc++ << 40;
    result |= (u8) *pSrc++ << 32;
    result |= (u8) *pSrc++ << 24;
    result |= (u8) *pSrc++ << 16;
    result |= (u8) *pSrc++ << 8;
    result |= (u8) *pSrc++;

    return result;
}


/*
 * Start here.
 */
int run(const char* connectHost, int connectPort, int listenPort);

/*
 * Print a hex dump to the specified file pointer.
 *
 * "local" mode prints a hex dump starting from offset 0 (roughly equivalent
 * to "xxd -g1").
 *
 * "mem" mode shows the actual memory address, and will offset the start
 * so that the low nibble of the address is always zero.
 */
typedef enum { kHexDumpLocal, kHexDumpMem } HexDumpMode;
void printHexDump(const void* vaddr, size_t length);
void printHexDump2(const void* vaddr, size_t length, const char* prefix);
void printHexDumpEx(FILE* fp, const void* vaddr, size_t length,
    HexDumpMode mode, const char* prefix);

#endif /*_JDWPSPY_COMMON*/
