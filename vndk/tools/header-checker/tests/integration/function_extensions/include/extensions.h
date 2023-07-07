struct Struct;

extern "C" {
void ConstParameter(const char (&)[2]);
void VolatileParameter(volatile Struct &&);
void Restrict(char *);
char **MultipleConst(char const *const *const);
}

char &ConstReturn();
Struct &&VolatileReturn();
