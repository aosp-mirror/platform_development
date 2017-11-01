#if INCLUDE_UNUSED_STRUCTS
struct UnusedStruct {
  int mUnusedMember;
};
#endif

class Foo {
 public:
  Foo(int *a, int *b) : a_(a), b_(b) { }
 private:
  int *a_;
  int *b_;
};

Foo foo(int *a, int *b);

