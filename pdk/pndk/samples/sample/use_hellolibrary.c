/* use_hellolibrary.c -- used to show how to link to the hellolibrary */

int hellolibrary(char *msg);

int main()
{
  hellolibrary("Hello from the NDK.\n");
  return 0;
}
