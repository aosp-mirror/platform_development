#include<c_include.h>

void CFunction(struct Cstruct **cstruct) {
    if (cstruct) {
      struct Cstruct *next = *cstruct++;
      next--;
    }
}
