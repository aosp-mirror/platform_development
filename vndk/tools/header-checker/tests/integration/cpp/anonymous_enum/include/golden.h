enum : char {
  A=1,
};

enum : char {
  B=1,
};

struct {
  enum {
    B,
    C,
  } member;
} global_var;
