#include <dlfcn.h>
#include <stdio.h>

int main(int argc, char **argv) {
  if (argc < 2) {
    puts("usage: main.out libtest.so");
    return 1;
  }

  void *handle = dlopen(argv[1], RTLD_NOW);
  if (!handle) {
    puts("failed to open lib");
    return 1;
  }

  void (*test)(void) = dlsym(handle, "test");
  if (!test) {
    puts("failed to find test() function");
  } else {
    test();
  }

  dlclose(handle);
  return 0;
}
