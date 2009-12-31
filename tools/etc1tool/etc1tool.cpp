// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <png.h>
#include <ETC1/etc1.h>


int writePNGFile(const char* pOutput, png_uint_32 width, png_uint_32 height,
        const png_bytep pImageData, png_uint_32 imageStride);

const char* gpExeName;

static
void usage(char* message, ...) {
    if (message) {
        va_list ap;
        va_start(ap, message);
        vfprintf(stderr, message, ap);
        va_end(ap);
        fprintf(stderr, "\n\n");
        fprintf(stderr, "usage:\n");
    }
    fprintf(
            stderr,
            "%s infile [--help | --encode | --encodeNoHeader | --decode] [--showDifference difffile] [-o outfile]\n",
            gpExeName);
    fprintf(stderr, "\tDefault is --encode\n");
    fprintf(stderr, "\t\t--help           print this usage information.\n");
    fprintf(stderr,
            "\t\t--encode         create an ETC1 file from a PNG file.\n");
    fprintf(
            stderr,
            "\t\t--encodeNoHeader create a raw ETC1 data file (without a header) from a PNG file.\n");
    fprintf(stderr,
            "\t\t--decode         create a PNG file from an ETC1 file.\n");
    fprintf(stderr,
            "\t\t--showDifference difffile    Write difference between original and encoded\n");
    fprintf(stderr,
            "\t\t                             image to difffile. (Only valid when encoding).\n");
    fprintf(stderr,
            "\tIf outfile is not specified, an outfile path is constructed from infile,\n");
    fprintf(stderr, "\twith the apropriate suffix (.pkm or .png).\n");
    exit(1);
}

// Returns non-zero if an error occured

static
int changeExtension(char* pPath, size_t pathCapacity, const char* pExtension) {
    size_t pathLen = strlen(pPath);
    size_t extensionLen = strlen(pExtension);
    if (pathLen + extensionLen + 1 > pathCapacity) {
        return -1;
    }

    // Check for '.' and '..'
    if ((pathLen == 1 && pPath[0] == '.') || (pathLen == 2 && pPath[0] == '.'
            && pPath[1] == '.') || (pathLen >= 2 && pPath[pathLen - 2] == '/'
            && pPath[pathLen - 1] == '.') || (pathLen >= 3
            && pPath[pathLen - 3] == '/' && pPath[pathLen - 2] == '.'
            && pPath[pathLen - 1] == '.')) {
        return -2;
    }

    int index;
    for (index = pathLen - 1; index > 0; index--) {
        char c = pPath[index];
        if (c == '/') {
            // No extension found. Append our extension.
            strcpy(pPath + pathLen, pExtension);
            return 0;
        } else if (c == '.') {
            strcpy(pPath + index, pExtension);
            return 0;
        }
    }

    // No extension or directory found. Append our extension
    strcpy(pPath + pathLen, pExtension);
    return 0;
}

void PNGAPI user_error_fn(png_structp png_ptr, png_const_charp message) {
    fprintf(stderr, "PNG error: %s\n", message);
}

void PNGAPI user_warning_fn(png_structp png_ptr, png_const_charp message) {
    fprintf(stderr, "PNG warning: %s\n", message);
}

// Return non-zero on error
int fwrite_big_endian_uint16(png_uint_32 data, FILE* pOut) {
    if (fputc(0xff & (data >> 8), pOut) == EOF) {
        return -1;
    }
    if (fputc(0xff & data, pOut) == EOF) {
        return -1;
    }
    return 0;
}

// Return non-zero on error
int fread_big_endian_uint16(png_uint_32* data, FILE* pIn) {
    int a, b;
    if ((a = fgetc(pIn)) == EOF) {
        return -1;
    }
    if ((b = fgetc(pIn)) == EOF) {
        return -1;
    }
    *data = ((0xff & a) << 8) | (0xff & b);
    return 0;
}

// Read a PNG file into a contiguous buffer.
// Returns non-zero if an error occurred.
// caller has to delete[] *ppImageData when done with the image.

int read_PNG_File(const char* pInput, etc1_byte** ppImageData,
        etc1_uint32* pWidth, etc1_uint32* pHeight) {
    FILE* pIn = NULL;
    png_structp png_ptr = NULL;
    png_infop info_ptr = NULL;
    png_infop end_info = NULL;
    png_bytep* row_pointers = NULL; // Does not need to be deallocated.
    png_uint_32 width = 0;
    png_uint_32 height = 0;
    png_uint_32 stride = 0;
    int result = -1;
    etc1_byte* pSourceImage = 0;

    if ((pIn = fopen(pInput, "rb")) == NULL) {
        fprintf(stderr, "Could not open input file %s for reading: %d\n",
                pInput, errno);
        goto exit;
    }

    static const size_t PNG_HEADER_SIZE = 8;
    png_byte pngHeader[PNG_HEADER_SIZE];
    if (fread(pngHeader, 1, PNG_HEADER_SIZE, pIn) != PNG_HEADER_SIZE) {
        fprintf(stderr, "Could not read PNG header from %s: %d\n", pInput,
                errno);
        goto exit;
    }

    if (png_sig_cmp(pngHeader, 0, PNG_HEADER_SIZE)) {
        fprintf(stderr, "%s is not a PNG file.\n", pInput);
        goto exit;
    }

    if (!(png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING,
            (png_voidp) NULL, user_error_fn, user_warning_fn))) {
        fprintf(stderr, "Could not initialize png read struct.\n");
        goto exit;
    }

    if (!(info_ptr = png_create_info_struct(png_ptr))) {
        fprintf(stderr, "Could not create info struct.\n");
        goto exit;
    }
    if (!(end_info = png_create_info_struct(png_ptr))) {
        fprintf(stderr, "Could not create end_info struct.\n");
        goto exit;
    }

    if (setjmp(png_jmpbuf(png_ptr))) {
        goto exit;
    }

    png_init_io(png_ptr, pIn);
    png_set_sig_bytes(png_ptr, PNG_HEADER_SIZE);
    png_read_png(png_ptr, info_ptr, PNG_TRANSFORM_IDENTITY
            | PNG_TRANSFORM_STRIP_16 | PNG_TRANSFORM_STRIP_ALPHA
            | PNG_TRANSFORM_PACKING, NULL);

    row_pointers = png_get_rows(png_ptr, info_ptr);
    {
        int bit_depth, color_type;
        png_get_IHDR(png_ptr, info_ptr, &width, &height, &bit_depth,
                &color_type, NULL, NULL, NULL);
    }

    stride = 3 * width;

    pSourceImage = new etc1_byte[stride * height];
    if (! pSourceImage) {
        fprintf(stderr, "Out of memory.\n");
        goto exit;
    }

    for (etc1_uint32 y = 0; y < height; y++) {
        memcpy(pSourceImage + y * stride, row_pointers[y], stride);
    }

    *pWidth = width;
    *pHeight = height;
    *ppImageData = pSourceImage;

    result = 0;
    exit:
    if (result) {
        delete[] pSourceImage;
    }
    if (png_ptr) {
        png_destroy_read_struct(&png_ptr, &info_ptr, &end_info);
    }
    if (pIn) {
        fclose(pIn);
    }

    return result;
}

// Read a PNG file into a contiguous buffer.
// Returns non-zero if an error occurred.
// caller has to delete[] *ppImageData when done with the image.
int readPKMFile(const char* pInput, etc1_byte** ppImageData,
        etc1_uint32* pWidth, etc1_uint32* pHeight) {
    int result = -1;
    FILE* pIn = NULL;
    etc1_byte header[ETC_PKM_HEADER_SIZE];
    png_bytep pEncodedData = NULL;
    png_bytep pImageData = NULL;

    png_uint_32 width = 0;
    png_uint_32 height = 0;
    png_uint_32 stride = 0;
    png_uint_32 encodedSize = 0;

    if ((pIn = fopen(pInput, "rb")) == NULL) {
        fprintf(stderr, "Could not open input file %s for reading: %d\n",
                pInput, errno);
        goto exit;
    }

    if (fread(header, sizeof(header), 1, pIn) != 1) {
        fprintf(stderr, "Could not read header from input file %s: %d\n",
                pInput, errno);
        goto exit;
    }

    if (! etc1_pkm_is_valid(header)) {
        fprintf(stderr, "Bad header PKM header for input file %s\n", pInput);
        goto exit;
    }

    width = etc1_pkm_get_width(header);
    height = etc1_pkm_get_height(header);
    encodedSize = etc1_get_encoded_data_size(width, height);

    pEncodedData = new png_byte[encodedSize];
    if (!pEncodedData) {
        fprintf(stderr, "Out of memory.\n");
        goto exit;
    }

    if (fread(pEncodedData, encodedSize, 1, pIn) != 1) {
        fprintf(stderr, "Could not read encoded data from input file %s: %d\n",
                pInput, errno);
        goto exit;
    }

    fclose(pIn);
    pIn = NULL;

    stride = width * 3;
    pImageData = new png_byte[stride * height];
    if (!pImageData) {
        fprintf(stderr, "Out of memory.\n");
        goto exit;
    }

    etc1_decode_image(pEncodedData, pImageData, width, height, 3, stride);

    // Success
    result = 0;
    *ppImageData = pImageData;
    pImageData = 0;
    *pWidth = width;
    *pHeight = height;

    exit:
    delete[] pEncodedData;
    delete[] pImageData;
    if (pIn) {
        fclose(pIn);
    }

    return result;
}


// Encode the file.
// Returns non-zero if an error occurred.

int encode(const char* pInput, const char* pOutput, bool bEmitHeader, const char* pDiffFile) {
    FILE* pOut = NULL;
    etc1_uint32 width = 0;
    etc1_uint32 height = 0;
    etc1_uint32 encodedSize = 0;
    int result = -1;
    etc1_byte* pSourceImage = 0;
    etc1_byte* pEncodedData = 0;
    etc1_byte* pDiffImage = 0; // Used for differencing

    if (read_PNG_File(pInput, &pSourceImage, &width, &height)) {
        goto exit;
    }

    encodedSize = etc1_get_encoded_data_size(width, height);
    pEncodedData = new etc1_byte[encodedSize];
    if (!pEncodedData) {
        fprintf(stderr, "Out of memory.\n");
        goto exit;
    }

    etc1_encode_image(pSourceImage,
            width, height, 3, width * 3, pEncodedData);

    if ((pOut = fopen(pOutput, "wb")) == NULL) {
        fprintf(stderr, "Could not open output file %s: %d\n", pOutput, errno);
        goto exit;
    }

    if (bEmitHeader) {
        etc1_byte header[ETC_PKM_HEADER_SIZE];
        etc1_pkm_format_header(header, width, height);
        if (fwrite(header, sizeof(header), 1, pOut) != 1) {
            fprintf(stderr,
                    "Could not write header output file %s: %d\n",
                    pOutput, errno);
            goto exit;
        }
    }

    if (fwrite(pEncodedData, encodedSize, 1, pOut) != 1) {
        fprintf(stderr,
                "Could not write encoded data to output file %s: %d\n",
                pOutput, errno);
        goto exit;
    }

    fclose(pOut);
    pOut = NULL;

    if (pDiffFile) {
        etc1_uint32 outWidth;
        etc1_uint32 outHeight;
        if (readPKMFile(pOutput, &pDiffImage, &outWidth, &outHeight)) {
            goto exit;
        }
        if (outWidth != width || outHeight != height) {
            fprintf(stderr, "Output file has incorrect bounds: %u, %u != %u, %u\n",
                    outWidth, outHeight, width, height);
            goto exit;
        }
        const etc1_byte* pSrc = pSourceImage;
        etc1_byte* pDest = pDiffImage;
        etc1_uint32 size = width * height * 3;
        for (etc1_uint32 i = 0; i < size; i++) {
            int diff = *pSrc++ - *pDest;
            diff *= diff;
            diff <<= 3;
            if (diff < 0) {
                diff = 0;
            } else if (diff > 255) {
                diff = 255;
            }
            *pDest++ = (png_byte) diff;
        }
        writePNGFile(pDiffFile, outWidth, outHeight, pDiffImage, 3 * outWidth);
    }

    // Success
    result = 0;

    exit:
    delete[] pSourceImage;
    delete[] pEncodedData;
    delete[] pDiffImage;

    if (pOut) {
        fclose(pOut);
    }
    return result;
}

int writePNGFile(const char* pOutput, png_uint_32 width, png_uint_32 height,
        const png_bytep pImageData, png_uint_32 imageStride) {
    int result = -1;
    FILE* pOut = NULL;
    png_structp png_ptr = NULL;
    png_infop info_ptr = NULL;

    if (!(png_ptr = png_create_write_struct(PNG_LIBPNG_VER_STRING,
            (png_voidp) NULL, user_error_fn, user_warning_fn)) || !(info_ptr
            = png_create_info_struct(png_ptr))) {
        fprintf(stderr, "Could not initialize PNG library for writing.\n");
        goto exit;
    }

    if (setjmp(png_jmpbuf(png_ptr))) {
        goto exit;
    }

    if ((pOut = fopen(pOutput, "wb")) == NULL) {
        fprintf(stderr, "Could not open output file %s: %d\n", pOutput, errno);
        goto exit;
    }

    png_init_io(png_ptr, pOut);

    png_set_IHDR(png_ptr, info_ptr, width, height, 8, PNG_COLOR_TYPE_RGB,
            PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT,
            PNG_FILTER_TYPE_DEFAULT);

    png_write_info(png_ptr, info_ptr);

    for (png_uint_32 y = 0; y < height; y++) {
        png_write_row(png_ptr, pImageData + y * imageStride);
    }
    png_write_end(png_ptr, info_ptr);

    result = 0;

    exit: if (png_ptr) {
        png_destroy_write_struct(&png_ptr, &info_ptr);
    }

    if (pOut) {
        fclose(pOut);
    }
    return result;
}

int decode(const char* pInput, const char* pOutput) {
    int result = -1;
    png_bytep pImageData = NULL;
    etc1_uint32 width = 0;
    etc1_uint32 height = 0;

    if (readPKMFile(pInput, &pImageData, &width, &height)) {
        goto exit;
    }

    if (writePNGFile(pOutput, width, height, pImageData, width * 3)) {
        goto exit;
    }

    // Success
    result = 0;

    exit: delete[] pImageData;

    return result;
}

void multipleEncodeDecodeCheck(bool* pbEncodeDecodeSeen) {
    if (*pbEncodeDecodeSeen) {
        usage("At most one occurrence of --encode --encodeNoHeader or --decode is allowed.\n");
    }
    *pbEncodeDecodeSeen = true;
}

int main(int argc, char** argv) {
    gpExeName = argv[0];
    const char* pInput = NULL;
    const char* pOutput = NULL;
    const char* pDiffFile = NULL;
    char* pOutputFileBuff = NULL;

    bool bEncodeDecodeSeen = false;
    bool bEncode = false;
    bool bEncodeHeader = false;
    bool bDecode = false;
    bool bShowDifference = false;

    for (int i = 1; i < argc; i++) {
        const char* pArg = argv[i];
        if (pArg[0] == '-') {
            char c = pArg[1];
            switch (c) {
            case 'o':
                if (pOutput != NULL) {
                    usage("Only one -o flag allowed.");
                }
                if (i + 1 >= argc) {
                    usage("Expected outfile after -o");
                }
                pOutput = argv[++i];
                break;
            case '-':
                if (strcmp(pArg, "--encode") == 0) {
                    multipleEncodeDecodeCheck(&bEncodeDecodeSeen);
                    bEncode = true;
                    bEncodeHeader = true;
                } else if (strcmp(pArg, "--encodeNoHeader") == 0) {
                    multipleEncodeDecodeCheck(&bEncodeDecodeSeen);
                    bEncode = true;
                    bEncodeHeader = false;
                } else if (strcmp(pArg, "--decode") == 0) {
                    multipleEncodeDecodeCheck(&bEncodeDecodeSeen);
                    bDecode = true;
                } else if (strcmp(pArg, "--showDifference") == 0) {
                    if (bShowDifference) {
                        usage("Only one --showDifference option allowed.\n");
                    }
                    bShowDifference = true;
                    if (i + 1 >= argc) {
                        usage("Expected difffile after --showDifference");
                    }
                    pDiffFile = argv[++i];
                } else if (strcmp(pArg, "--help") == 0) {
                    usage( NULL);
                } else {
                    usage("Unknown flag %s", pArg);
                }

                break;
            default:
                usage("Unknown flag %s", pArg);
                break;
            }
        } else {
            if (pInput != NULL) {
                usage(
                        "Only one input file allowed. Already have %s, now see %s",
                        pInput, pArg);
            }
            pInput = pArg;
        }
    }

    if (!bEncodeDecodeSeen) {
        bEncode = true;
        bEncodeHeader = true;
    }
    if ((! bEncode) && bShowDifference) {
        usage("--showDifference is only valid when encoding.");
    }

    if (!pInput) {
        usage("Expected an input file.");
    }

    if (!pOutput) {
        const char* kDefaultExtension = bEncode ? ".pkm" : ".png";
        size_t buffSize = strlen(pInput) + strlen(kDefaultExtension) + 1;
        pOutputFileBuff = new char[buffSize];
        strcpy(pOutputFileBuff, pInput);
        if (changeExtension(pOutputFileBuff, buffSize, kDefaultExtension)) {
            usage("Could not change extension of input file name: %s\n", pInput);
        }
        pOutput = pOutputFileBuff;
    }

    if (bEncode) {
        encode(pInput, pOutput, bEncodeHeader, pDiffFile);
    } else {
        decode(pInput, pOutput);
    }

    delete[] pOutputFileBuff;

    return 0;
}
