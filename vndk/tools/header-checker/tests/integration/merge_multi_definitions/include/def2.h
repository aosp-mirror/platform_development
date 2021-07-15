extern int var;

struct Struct {
  Struct *member2;
};

struct Opaque;

void func(const struct Opaque *, const struct Struct *);
