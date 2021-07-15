extern char var;

struct Struct {
  Struct *member1;
};

struct Opaque;

void func(const struct Struct *, const struct Opaque *);
