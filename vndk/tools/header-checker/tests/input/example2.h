#ifndef EXAMPLE2_H_
#define EXAMPLE2_H_
#include <memory>
#include <vector>
#include <string>
#include <tuple>
#include "example3.h"

template <typename T, int size>
class fooray {
  T foor[size];
};

template <class _T1, class _T2>
struct __find_exactly_one_check {
    static constexpr bool __matches[] = {std::is_same<_T1, _T2>::value};
};

namespace test2 {
struct HelloAgain {
  std::vector<HelloAgain *> foo_again;
  int bar_again;
  static int hello_forever;
  virtual int again();
};
struct NowWeCrash;
} // namespace test2

enum Foo_s {
  foosball = 10,
  foosbat
};

static constexpr bool __test_var[] = {true, false};

namespace test3 {
template <typename T>
struct ByeAgain {
  T foo_again;
  int bar_again;
  T method_foo(T);
};

template<>
struct ByeAgain<float> {
  float foo_again;
  static int foo_forever;
  float bar_Again;
  float method_foo(int);
};

ByeAgain<double> double_bye;

template <typename T1, typename T2>
bool Begin( T1 arg1, T2 arg2, int c);

bool End ( float arg = 2.0) {
  bool ret = Begin(arg, 2, 2);
  return true;
}


enum Kind {
  kind1 = 24,
  kind2 = 2312
};

class Outer {
 public:
  int a;
 private:
  class Inner {
    int b;
  };
};

std::vector<int *> Dummy(int t);

} // namespace test3

#endif  // EXAMPLE2_H_
