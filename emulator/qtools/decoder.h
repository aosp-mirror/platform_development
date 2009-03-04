// Copyright 2006 The Android Open Source Project

#include <stdio.h>
#include <inttypes.h>

class Decoder {
 public:
  Decoder();
  ~Decoder();

  void          Open(char *filename);
  void          Close();
  int64_t       Decode(bool is_signed);
  void          Read(char *dest, int len);
  bool          IsEOF()          { return (end_ == next_) && feof(fstream_); }

 private:
  static const int kBufSize = 4096;
  static const int kDecodingSpace = 9;

  void          FillBuffer();

  char          *filename_;
  FILE          *fstream_;
  uint8_t       buf_[kBufSize];
  uint8_t       *next_;
  uint8_t       *end_;
};
