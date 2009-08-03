#if !defined(BANANA)
#  error LOCAL_CPPFLAGS does not work for C++ source file
#endif
#if BANANA != 300
#  error LOCAL_CPPFLAGS does not work correctly for C++ source file
#endif

void  __banana_foo2(void)
{
}
