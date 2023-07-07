#include <cstdint>

enum Int8 : int8_t {
  ZERO = 0,
  MINUS_1 = (int8_t)-1,
};

enum Uint8 : uint8_t {
  UNSIGNED_255 = UINT8_MAX,
};

enum Int64 : int64_t {
  SIGNED_MAX = INT64_MAX,
  SIGNED_MIN = INT64_MIN,
};

enum Uint64 : uint64_t {
  UNSIGNED_MAX = UINT64_MAX,
};

extern "C" {
void function(Int8, Uint8, Int64, Uint64);
}
