#ifndef LIBTEST1_H
#define LIBTEST1_H

#ifdef __cplusplus
extern "C" {
#endif

/* define in libtest1, will be called dynamically through dlsym()
 * by main.c. This function receives the address of an integer
 * and sets its value to 1.
 *
 * when the library is unloaded, the value is set to 2 automatically
 * by the destructor there.
 */
extern void test1_set(int *px);

#ifdef __cplusplus
}
#endif

#endif /* LIBTEST1_H */
