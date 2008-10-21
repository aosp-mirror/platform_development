/*
 * Copyright 2007 The Android Open Source Project
 *
 * Simple bit vector.
 */
#ifndef _WRAPSIM_BITVECTOR_H
#define _WRAPSIM_BITVECTOR_H

#include <stdint.h>

/*
 * Expanding bitmap, used for tracking resources.  Bits are numbered starting
 * from zero.
 */
typedef struct BitVector {
    int         isExpandable;   /* expand bitmap if we run out? */
    int         storageSize;    /* current size, in 32-bit words */
    uint32_t*   storage;
} BitVector;

/* allocate a bit vector with enough space to hold "startBits" bits */
BitVector* wsAllocBitVector(int startBits, int isExpandable);
void wsFreeBitVector(BitVector* pBits);

/*
 * Set/clear a single bit; assumes external synchronization.
 *
 * We always allocate the first possible bit.  If we run out of space in
 * the bitmap, and it's not marked expandable, dvmAllocBit returns -1.
 */
int wsAllocBit(BitVector* pBits);
void wsFreeBit(BitVector* pBits, int num);

#endif /*_WRAPSIM_BITVECTOR_H*/
