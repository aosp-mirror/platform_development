/* hellolibrary.c - demonstrate library use with the NDK. 
 * This will be the library that gets called as wither a static or shared lib.*/

#include <stdio.h>

int hellolibrary(char *msg)
{
  printf("Library printing message: %s", msg);
  return 0;
}
