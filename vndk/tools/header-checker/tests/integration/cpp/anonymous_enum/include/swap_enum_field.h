enum : char {
  A=1,
};

enum : char {
  B=1,
};

struct {
  enum {
    C=1,
    B=0,
  } member;
} global_var;
