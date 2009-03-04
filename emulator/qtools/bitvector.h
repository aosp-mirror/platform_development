// Copyright 2006 The Android Open Source Project

#ifndef BITVECTOR_H
#define BITVECTOR_H

#include <inttypes.h>
#include <assert.h>

class Bitvector {
 public:
  explicit Bitvector(int num_bits) {
    num_bits_ = num_bits;

    // Round up to a multiple of 32
    num_bits = (num_bits + 31) & ~31;
    vector_ = new uint32_t[num_bits >> 5];
  }
  ~Bitvector() {
    delete[] vector_;
  }

  void        SetBit(int bitnum) {
    assert(bitnum < num_bits_);
    vector_[bitnum >> 5] |= 1 << (bitnum & 31);
  }
  void        ClearBit(int bitnum) {
    assert(bitnum < num_bits_);
    vector_[bitnum >> 5] &= ~(1 << (bitnum & 31));
  }
  bool        GetBit(int bitnum) {
    assert(bitnum < num_bits_);
    return (vector_[bitnum >> 5] >> (bitnum & 31)) & 1;
  }

 private:
  int         num_bits_;
  uint32_t    *vector_;
};

#endif  // BITVECTOR_H
