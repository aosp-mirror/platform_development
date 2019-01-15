#if INCLUDE_UNUSED_STRUCTS
#if MAKE_UNUSED_STRUCT_C
extern "C" {
#endif
struct UnusedStruct {
  int mUnusedMember;
};
#if MAKE_UNUSED_STRUCT_C
}
#endif
#endif

class Foo {
 public:
  Foo(int *a, int *b) : a_(a), b_(b) {}
 private:
  int *a_;
  int *b_;
};

Foo foo(int *a, int *b);

