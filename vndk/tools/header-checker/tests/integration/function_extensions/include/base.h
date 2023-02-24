struct Struct;

extern "C" {
void ConstParameter(char (&)[2]);
void VolatileParameter(Struct &&);
void Restrict(char *__restrict__);
const char *const *MultipleConst(char **);
}

const char &ConstReturn();
volatile Struct &&VolatileReturn();
