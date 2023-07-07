extern int var;

struct Struct {
  Struct *member2;
};

struct Opaque;

struct DefinedInOneHeader {};

extern "C" {
void func(Opaque *, Struct *, DefinedInOneHeader *);
void func2(DefinedInOneHeader *);
}
