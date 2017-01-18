#ifndef EXAMPLE2_H_
#define EXAMPLE2_H_
namespace test2 {
struct HelloAgain {
  int foo_again;
  int bar_again;
};
} // namespace test2

namespace test3 {
template <typename T>
struct ByeAgain {
  T foo_again;
  int bar_again;
};
} // namespace test3

#endif  // EXAMPLE2_H_
