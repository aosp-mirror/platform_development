#include <stdio.h>
#include <dlfcn.h>

typedef void (*test_func_t)(int *px);
int  x;

int main(void)
{
    void*  lib = dlopen("libtest1.so", RTLD_NOW);
    test_func_t test_func;

    if (lib == NULL) {
        fprintf(stderr, "Can't load library: %s\n", dlerror());
        return 1;
    }

    printf("Loaded !\n");

    test_func = dlsym(lib, "test1_set");
    if (test_func == NULL) {
        fprintf(stderr, "Can't find test function\n");
        return 2;
    }

    x = 0;
    test_func(&x);

    if (x == 1) {
        printf("Test function called !\n");
    } else {
        fprintf(stderr, "Test function failed to set variable !\n");
        return 3;
    }

    dlclose(lib);
    printf("Unloaded !\n");

    if (x == 2) {
        printf("Test destructor called !\n");
    } else if (x == 1) {
        fprintf(stderr, "Test destructor was *not* called !\n");
        return 4;
    } else {
        fprintf(stderr, "Test destructor called but returned invalid value (%d)\n", x);
        return 5;
    }
    return 0;
}
