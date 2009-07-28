#if !defined(BANANA)
#  error LOCAL_CFLAGS does not work for C++ source file
#endif
#if BANANA != 100
#  error LOCAL_CFLAGS does not work correctly for C++ source file
#endif

void  __banana_foo2(void)
{
}
