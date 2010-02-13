#ifndef HELLONEON_INTRINSICS_H
#define HELLONEON_INTRINSICS_H

void fir_filter_neon_intrinsics(short *output, const short* input, const short* kernel, int width, int kernelSize);

#endif /* HELLONEON_INTRINSICS_H */
