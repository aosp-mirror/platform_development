#ifndef EXAMPLE2_H_
#define EXAMPLE2_H_
#include <memory>

namespace test2 {
struct HelloAgain {
  std::unique_ptr<HelloAgain> foo_again;
  int bar_again;
};
struct NowWeCrash;
} // namespace test2

enum Foo_s {
  foosball = 10,
  foosbat
};

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
  float bar_Again;
  float method_foo(int);
};

ByeAgain<double> double_bye;

template <typename T1, typename T2>
bool Begin(T1 arg1, T2 arg2);
template <>
bool Begin<int, float>(int a, float b);
bool End ( float arg = 2.0) {
  bool ret = Begin(arg, 2);
  return ret;
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


} // namespace test3

#endif  // EXAMPLE2_H_
