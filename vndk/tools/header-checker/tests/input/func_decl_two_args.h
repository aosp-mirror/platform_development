#ifndef FUNC_DECL_TWO_ARGS_H_
#define FUNC_DECL_TWO_ARGS_H_

#if defined(__cplusplus)
extern "C" {
#endif

extern void test_char(int, char);

extern void test_short(int, short);

extern void test_int(int, int);

extern void test_long(int, long);

extern void test_long_long(int, long long);

extern void test_unsigned_char(int, unsigned char);

extern void test_unsigned_short(int, unsigned short);

extern void test_unsigned_int(int, unsigned int);

extern void test_unsigned_long(int, unsigned long);

extern void test_unsigned_long_long(int, unsigned long long);

extern void test_unsigned_float(int, float);

extern void test_unsigned_double(int, double);

extern void test_unsigned_long_double(int, long double);

#if defined(__cplusplus)
}  // extern "C"
#endif

#endif  // FUNC_DECL_TWO_ARGS_H_
