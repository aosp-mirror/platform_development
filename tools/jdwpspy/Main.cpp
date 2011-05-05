/*
 * Copyright 2006 The Android Open Source Project
 *
 * JDWP spy.
 */
#define _JDWP_MISC_INLINE
#include "Common.h"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <ctype.h>

static const char gHexDigit[] = "0123456789abcdef";

/*
 * Print a hex dump.  Just hands control off to the fancy version.
 */
void printHexDump(const void* vaddr, size_t length)
{
    printHexDumpEx(stdout, vaddr, length, kHexDumpLocal, "");
}
void printHexDump2(const void* vaddr, size_t length, const char* prefix)
{
    printHexDumpEx(stdout, vaddr, length, kHexDumpLocal, prefix);
}

/*
 * Print a hex dump in this format:
 *
01234567: 00 11 22 33 44 55 66 77 88 99 aa bb cc dd ee ff  0123456789abcdef\n
 */
void printHexDumpEx(FILE* fp, const void* vaddr, size_t length,
    HexDumpMode mode, const char* prefix)
{
    const unsigned char* addr = reinterpret_cast<const unsigned char*>(vaddr);
    char out[77];       /* exact fit */
    unsigned int offset;    /* offset to show while printing */
    char* hex;
    char* asc;
    int gap;

    if (mode == kHexDumpLocal)
        offset = 0;
    else
        offset = (int) addr;

    memset(out, ' ', sizeof(out)-1);
    out[8] = ':';
    out[sizeof(out)-2] = '\n';
    out[sizeof(out)-1] = '\0';

    gap = (int) offset & 0x0f;
    while (length) {
        unsigned int lineOffset = offset & ~0x0f;
        char* hex = out;
        char* asc = out + 59;

        for (int i = 0; i < 8; i++) {
            *hex++ = gHexDigit[lineOffset >> 28];
            lineOffset <<= 4;
        }
        hex++;
        hex++;

        int count = ((int)length > 16-gap) ? 16-gap : (int) length; /* cap length */
        assert(count != 0);
        assert(count+gap <= 16);

        if (gap) {
            /* only on first line */
            hex += gap * 3;
            asc += gap;
        }

        int i;
        for (i = gap ; i < count+gap; i++) {
            *hex++ = gHexDigit[*addr >> 4];
            *hex++ = gHexDigit[*addr & 0x0f];
            hex++;
            if (isprint(*addr))
                *asc++ = *addr;
            else
                *asc++ = '.';
            addr++;
        }
        for ( ; i < 16; i++) {
            /* erase extra stuff; only happens on last line */
            *hex++ = ' ';
            *hex++ = ' ';
            hex++;
            *asc++ = ' ';
        }

        fprintf(fp, "%s%s", prefix, out);

        gap = 0;
        length -= count;
        offset += count;
    }
}


/*
 * Explain it.
 */
static void usage(const char* progName)
{
    fprintf(stderr, "Usage: %s VM-port [debugger-listen-port]\n\n", progName);
    fprintf(stderr,
"When a debugger connects to the debugger-listen-port, jdwpspy will connect\n");
    fprintf(stderr, "to the VM on the VM-port.\n");
}

/*
 * Parse args.
 */
int main(int argc, char* argv[])
{
    if (argc < 2 || argc > 3) {
        usage("jdwpspy");
        return 2;
    }

    setvbuf(stdout, NULL, _IONBF, 0);

    /* may want this to be host:port */
    int connectPort = atoi(argv[1]);

    int listenPort;
    if (argc > 2)
        listenPort = atoi(argv[2]);
    else
        listenPort = connectPort + 1;

    int cc = run("localhost", connectPort, listenPort);

    return (cc != 0);
}
