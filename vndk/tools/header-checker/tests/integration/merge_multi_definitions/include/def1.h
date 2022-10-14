extern char var;

struct Struct {
  Struct *member1;
};

struct Opaque;

struct DefinedInOneHeader;

extern "C" {
void func(Struct *, Opaque *, DefinedInOneHeader *);
}
