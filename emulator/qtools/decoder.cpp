// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "decoder.h"
#include "trace_common.h"

// This array provides a fast conversion from the initial byte in
// a varint-encoded object to the length (in bytes) of that object.
int prefix_to_len[] = {
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
    3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
    4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
    5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 9, 9, 17, 17
};

// This array provides a fast conversion from the initial byte in
// a varint-encoded object to the initial data bits for that object.
int prefix_to_data[] = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
    64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
    80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95,
    96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
    112, 113, 114, 115, 116, 117, 118, 119,
    120, 121, 122, 123, 124, 125, 126, 127,
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
    0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 0, 0, 0, 0
};

signed char prefix_to_signed_data[] = {
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
    0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
    0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
    0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
    0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
    0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,
    0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
    0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f,
    0xc0, 0xc1, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7,
    0xc8, 0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xce, 0xcf,
    0xd0, 0xd1, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7,
    0xd8, 0xd9, 0xda, 0xdb, 0xdc, 0xdd, 0xde, 0xdf,
    0xe0, 0xe1, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7,
    0xe8, 0xe9, 0xea, 0xeb, 0xec, 0xed, 0xee, 0xef,
    0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7,
    0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff,
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
    0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
    0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
    0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
    0xe0, 0xe1, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7,
    0xe8, 0xe9, 0xea, 0xeb, 0xec, 0xed, 0xee, 0xef,
    0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7,
    0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff,
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
    0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
    0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7,
    0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff,
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
    0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xff,
    0x00, 0x01, 0x02, 0x03, 0xfc, 0xfd, 0xfe, 0xff,
    0x00, 0x01, 0xfe, 0xff, 0x00, 0xff, 0x00, 0xff,
};

Decoder::Decoder()
{
    filename_ = NULL;
    fstream_ = NULL;
    next_ = NULL;
    end_ = NULL;
}

Decoder::~Decoder()
{
    Close();
    delete[] filename_;
}

void Decoder::Close()
{
    if (fstream_) {
        fclose(fstream_);
        fstream_ = NULL;
    }
}

void Decoder::Open(char *filename)
{
    if (filename_ != NULL) {
        delete[] filename_;
    }
    filename_ = new char[strlen(filename) + 1];
    strcpy(filename_, filename);
    fstream_ = fopen(filename_, "r");
    if (fstream_ == NULL) {
        perror(filename_);
        exit(1);
    }

    int rval = fread(buf_, 1, kBufSize, fstream_);
    if (rval != kBufSize) {
        if (ferror(fstream_)) {
            perror(filename_);
            exit(1);
        }
        if (!feof(fstream_)) {
            fprintf(stderr, "Unexpected short fread() before eof\n");
            exit(1);
        }
    }
    next_ = buf_;
    end_ = buf_ + rval;
}

void Decoder::FillBuffer()
{
    assert(next_ <= end_);
  
    if (end_ - next_ < kDecodingSpace && end_ == &buf_[kBufSize]) {
        // Copy the unused bytes left at the end to the beginning of the
        // buffer.
        int len = end_ - next_;
        if (len > 0)
            memcpy(buf_, next_, len);

        // Read enough bytes to fill up the buffer, if possible.
        int nbytes = kBufSize - len;
        int rval = fread(buf_ + len, 1, nbytes, fstream_);
        if (rval < nbytes) {
            if (ferror(fstream_)) {
                perror(filename_);
                exit(1);
            }
            if (!feof(fstream_)) {
                fprintf(stderr, "Unexpected short fread() before eof\n");
                exit(1);
            }
        }
        end_ = &buf_[len + rval];
        next_ = buf_;
    }
}

void Decoder::Read(char *dest, int len)
{
    while (len > 0) {
        int nbytes = end_ - next_;
        if (nbytes == 0) {
            FillBuffer();
            nbytes = end_ - next_;
            if (nbytes == 0)
                break;
        }
        if (nbytes > len)
            nbytes = len;
        memcpy(dest, next_, nbytes);
        dest += nbytes;
        len -= nbytes;
        next_ += nbytes;
    }
}

// Decode a varint-encoded object starting at the current position in
// the array "buf_" and return the decoded value as a 64-bit integer.
// A varint-encoded object has an initial prefix that specifies how many
// data bits follow.  If the first bit is zero, for example, then there
// are 7 data bits that follow.  The table below shows the prefix values
// and corresponding data bits.
//
// Prefix     Bytes  Data bits
// 0          1      7
// 10         2      14
// 110        3      21
// 1110       4      28
// 11110      5      35
// 111110     6      42
// 11111100   9      64
// 11111101   reserved
// 11111110   reserved
// 11111111   reserved
int64_t Decoder::Decode(bool is_signed)
{
    int64_t val64;

    if (end_ - next_ < kDecodingSpace)
        FillBuffer();

#if BYTE_ORDER == BIG_ENDIAN
    uint8_t byte0 = *next_;

    // Get the number of bytes to decode based on the first byte.
    int len = prefix_to_len[byte0];

    if (next_ + len > end_) {
        fprintf(stderr, "%s: decoding past end of file.\n", filename_);
        exit(1);
    }

    // Get the first data byte.
    if (is_signed)
        val64 = prefix_to_signed_data[byte0];
    else
        val64 = prefix_to_data[byte0];

    next_ += 1;
    for (int ii = 1; ii < len; ++ii) {
        val64 = (val64 << 8) | *next_++;
    }
#else
    // If we are on a little-endian machine, then use large, unaligned loads.
    uint64_t data = *(reinterpret_cast<uint64_t*>(next_));
    uint8_t byte0 = data;
    data = bswap64(data);

    // Get the number of bytes to decode based on the first byte.
    int len = prefix_to_len[byte0];

    if (next_ + len > end_) {
        fprintf(stderr, "%s: decoding past end of file.\n", filename_);
        exit(1);
    }

    // Get the first data byte.
    if (is_signed)
        val64 = prefix_to_signed_data[byte0];
    else
        val64 = prefix_to_data[byte0];

    switch (len) {
        case 1:
            break;
        case 2:
            val64 = (val64 << 8) | ((data >> 48) & 0xffull);
            break;
        case 3:
            val64 = (val64 << 16) | ((data >> 40) & 0xffffull);
            break;
        case 4:
            val64 = (val64 << 24) | ((data >> 32) & 0xffffffull);
            break;
        case 5:
            val64 = (val64 << 32) | ((data >> 24) & 0xffffffffull);
            break;
        case 6:
            val64 = (val64 << 40) | ((data >> 16) & 0xffffffffffull);
            break;
        case 9:
            data = *(reinterpret_cast<uint64_t*>(&next_[1]));
            val64 = bswap64(data);
            break;
    }
    next_ += len;
#endif
    return val64;
}
