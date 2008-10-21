/*
 * Copyright 2007 The Android Open Source Project
 *
 * Simple bit vector.
 */
#include "Common.h"

#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <assert.h>

#define kBitVectorGrowth    4   /* increase by 4 uint32_t when limit hit */


/*
 * Allocate a bit vector with enough space to hold at least the specified
 * number of bits.
 */
BitVector* wsAllocBitVector(int startBits, int isExpandable)
{
    BitVector* bv;
    int count;

    assert(sizeof(bv->storage[0]) == 4);        /* assuming 32-bit units */
    assert(startBits > 0);

    bv = (BitVector*) malloc(sizeof(BitVector));

    count = (startBits + 31) >> 5;

    bv->storageSize = count;
    bv->isExpandable = isExpandable;
    bv->storage = (uint32_t*) malloc(count * sizeof(uint32_t));
    memset(bv->storage, 0xff, count * sizeof(uint32_t));
    return bv;
}

/*
 * Free a BitVector.
 */
void wsFreeBitVector(BitVector* pBits)
{
    if (pBits == NULL)
        return;

    free(pBits->storage);
    free(pBits);
}

/*
 * "Allocate" the first-available bit in the bitmap.
 *
 * This is not synchronized.  The caller is expected to hold some sort of
 * lock that prevents multiple threads from executing simultaneously in
 * dvmAllocBit/dvmFreeBit.
 *
 * The bitmap indicates which resources are free, so we use '1' to indicate
 * available and '0' to indicate allocated.
 */
int wsAllocBit(BitVector* pBits)
{
    int word, bit;

retry:
    for (word = 0; word < pBits->storageSize; word++) {
        if (pBits->storage[word] != 0) {
            /*
             * There are unallocated bits in this word.  Return the first.
             */
            bit = ffs(pBits->storage[word]) -1;
            assert(bit >= 0 && bit < 32);
            pBits->storage[word] &= ~(1 << bit);
            return (word << 5) | bit;
        }
    }

    /*
     * Ran out of space, allocate more if we're allowed to.
     */
    if (!pBits->isExpandable)
        return -1;

    pBits->storage = realloc(pBits->storage,
                    (pBits->storageSize + kBitVectorGrowth) * sizeof(uint32_t));
    memset(&pBits->storage[pBits->storageSize], 0xff,
        kBitVectorGrowth * sizeof(uint32_t));
    pBits->storageSize += kBitVectorGrowth;
    goto retry;
}

/*
 * Mark the specified bit as "free".
 */
void wsFreeBit(BitVector* pBits, int num)
{
    assert(num >= 0 &&
           num < (int) pBits->storageSize * (int)sizeof(uint32_t) * 8);

    pBits->storage[num >> 5] |= 1 << (num & 0x1f);
}

