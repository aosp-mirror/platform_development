__attribute__((used)) static void *ifunc_resolver() {
  return 0;
}

void ifunc() __attribute__((ifunc("ifunc_resolver")));
