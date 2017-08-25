#if defined(__cplusplus)
extern "C" {
#endif

struct Cinner {
  int c;
};

struct Cstruct {
  int a;
  struct Cinner *b;
};

void CFunction(struct Cstruct **cstruct);

#if defined(__cplusplus)
}
#endif
