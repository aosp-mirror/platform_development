#include <c_and_cpp.h>

Foo foo(int *a, int *b) {
  // This does not make sense
  return Foo(a, b);
}
