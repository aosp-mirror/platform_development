#if defined(__cplusplus)
extern "C" {
#endif

struct Cinner {
  int c;
};

struct opaque_a;
struct opaque_b;

struct Cstruct {
  int a;
  struct Cinner *b;
#ifdef OPAQUE_STRUCT_A
  struct opaque_a *op;
#elif OPAQUE_STRUCT_B
  struct opaque_b *op;
#endif
};

void CFunction(struct Cstruct **cstruct);

#if defined(__cplusplus)
}
#endif
