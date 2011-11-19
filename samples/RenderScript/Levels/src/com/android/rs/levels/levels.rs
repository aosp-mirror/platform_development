#pragma version(1)
#pragma rs java_package_name(com.android.rs.levels)

float inBlack;
float outBlack;
float inWMinInB;
float outWMinOutB;
float overInWMinInB;
float gamma;
rs_matrix3x3 colorMat;

void root(const uchar4 *in, uchar4 *out, uint32_t x, uint32_t y) {
    float3 pixel = convert_float4(in[0]).rgb;
    pixel = rsMatrixMultiply(&colorMat, pixel);
    pixel = clamp(pixel, 0.f, 255.f);
    pixel = (pixel - inBlack) * overInWMinInB;
    if (gamma != 1.0f)
        pixel = pow(pixel, (float3)gamma);
    pixel = pixel * outWMinOutB + outBlack;
    pixel = clamp(pixel, 0.f, 255.f);
    out->xyz = convert_uchar3(pixel);
}

