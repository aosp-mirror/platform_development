#include <stdio.h>
#include <debug.h>
#include <cmdline.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>

#ifndef max
#define max(a,b) ({typeof(a) _a = (a); typeof(b) _b = (b); _a > _b ? _a : _b; })
#define min(a,b) ({typeof(a) _a = (a); typeof(b) _b = (b); _a < _b ? _a : _b; })
#endif

#define CONVERT_TYPE_PPM 0
#define CONVERT_TYPE_RGB 1
#define CONVERT_TYPE_ARGB 2

/*
   YUV 4:2:0 image with a plane of 8 bit Y samples followed by an interleaved
   U/V plane containing 8 bit 2x2 subsampled chroma samples.
   except the interleave order of U and V is reversed.

                        H V
   Y Sample Period      1 1
   U (Cb) Sample Period 2 2
   V (Cr) Sample Period 2 2
 */

typedef struct rgb_context {
    unsigned char *buffer;
    int width;
    int height;
    int rotate;
    int i;
    int j;
    int size; /* for debugging */
} rgb_context;

typedef void (*rgb_cb)(
    unsigned char r,
    unsigned char g,
    unsigned char b,
    rgb_context *ctx);

const int bytes_per_pixel = 2;

static void color_convert_common(
    unsigned char *pY, unsigned char *pUV,
    int width, int height,
    unsigned char *buffer,
    int size, /* buffer size in bytes */
    int gray,
    int rotate,
    rgb_cb cb) 
{
	int i, j;
	int nR, nG, nB;
	int nY, nU, nV;
    rgb_context ctx;

    ctx.buffer = buffer;
    ctx.size = size; /* debug */
    ctx.width = width;
    ctx.height = height;
    ctx.rotate = rotate;

    if (gray) {
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                nB = *(pY + i * width + j);
                ctx.i = i;
                ctx.j = j;
                cb(nB, nB, nB, &ctx);
            }
        }	
    } else {
        // YUV 4:2:0
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                nY = *(pY + i * width + j);
                nV = *(pUV + (i/2) * width + bytes_per_pixel * (j/2));
                nU = *(pUV + (i/2) * width + bytes_per_pixel * (j/2) + 1);
            
                // Yuv Convert
                nY -= 16;
                nU -= 128;
                nV -= 128;
            
                if (nY < 0)
                    nY = 0;
            
                // nR = (int)(1.164 * nY + 2.018 * nU);
                // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
                // nB = (int)(1.164 * nY + 1.596 * nV);
            
                nB = (int)(1192 * nY + 2066 * nU);
                nG = (int)(1192 * nY - 833 * nV - 400 * nU);
                nR = (int)(1192 * nY + 1634 * nV);
            
                nR = min(262143, max(0, nR));
                nG = min(262143, max(0, nG));
                nB = min(262143, max(0, nB));
            
                nR >>= 10; nR &= 0xff;
                nG >>= 10; nG &= 0xff;
                nB >>= 10; nB &= 0xff;

                ctx.i = i;
                ctx.j = j;
                cb(nR, nG, nB, &ctx);
            }
        }
    }
}   

static void rgb16_cb(
    unsigned char r,
    unsigned char g,
    unsigned char b,
    rgb_context *ctx)
{
    unsigned short *rgb16 = (unsigned short *)ctx->buffer;
    *(rgb16 + ctx->i * ctx->width + ctx->j) = b | (g << 5) | (r << 11);
}

static void common_rgb_cb(
    unsigned char r,
    unsigned char g,
    unsigned char b,
    rgb_context *ctx,
    int alpha)
{
    unsigned char *out = ctx->buffer;
    int offset = 0;
    int bpp;
    int i = 0;
    switch(ctx->rotate) {
    case 0: /* no rotation */
        offset = ctx->i * ctx->width + ctx->j;
        break;
    case 1: /* 90 degrees */
        offset = ctx->height * (ctx->j + 1) - ctx->i;
        break;
    case 2: /* 180 degrees */
        offset = (ctx->height - 1 - ctx->i) * ctx->width + ctx->j;
        break;
    case 3: /* 270 degrees */
        offset = (ctx->width - 1 - ctx->j) * ctx->height + ctx->i;
        break;
    default:
        FAILIF(1, "Unexpected roation value %d!\n", ctx->rotate);
    }

    bpp = 3 + !!alpha;
    offset *= bpp;
    FAILIF(offset < 0, "point (%d, %d) generates a negative offset.\n", ctx->i, ctx->j);
    FAILIF(offset + bpp > ctx->size, "point (%d, %d) at offset %d exceeds the size %d of the buffer.\n",
           ctx->i, ctx->j,
           offset,
           ctx->size);

    out += offset;

    if (alpha) out[i++] = 0xff;
    out[i++] = r;
    out[i++] = g;
    out[i] = b;
}

static void rgb24_cb(
    unsigned char r,
    unsigned char g,
    unsigned char b,
    rgb_context *ctx)
{
    return common_rgb_cb(r,g,b,ctx,0);
}

static void argb_cb(
    unsigned char r,
    unsigned char g,
    unsigned char b,
    rgb_context *ctx)
{
    return common_rgb_cb(r,g,b,ctx,1);
}

static void convert(const char *infile,
                    const char *outfile,
                    int height,
                    int width,
                    int gray,
                    int type,
                    int rotate)
{
    void *in, *out;
    int ifd, ofd, rc;
    int psz = getpagesize();
    static char header[1024];
    int header_size;
    size_t outsize;

    int bpp = 3;
    switch (type) {
    case CONVERT_TYPE_PPM:
        PRINT("encoding PPM\n");
        if (rotate & 1)
            header_size = snprintf(header, sizeof(header), "P6\n%d %d\n255\n", height, width);
        else
            header_size = snprintf(header, sizeof(header), "P6\n%d %d\n255\n", width, height);
	break;
    case CONVERT_TYPE_RGB:
        PRINT("encoding raw RGB24\n");
        header_size = 0;
        break;
    case CONVERT_TYPE_ARGB:
        PRINT("encoding raw ARGB\n");
        header_size = 0;
        bpp = 4;
        break;
    }
        
    outsize = header_size + width * height * bpp;
    outsize = (outsize + psz - 1) & ~(psz - 1);

    INFO("Opening input file %s\n", infile);
    ifd = open(infile, O_RDONLY);
    FAILIF(ifd < 0, "open(%s) failed: %s (%d)\n",
           infile, strerror(errno), errno);

    INFO("Opening output file %s\n", outfile);
    ofd = open(outfile, O_RDWR | O_CREAT, 0664);
    FAILIF(ofd < 0, "open(%s) failed: %s (%d)\n",
           outfile, strerror(errno), errno);
    
    INFO("Memory-mapping input file %s\n", infile);
    in = mmap(0, width * height * 3 / 2, PROT_READ, MAP_PRIVATE, ifd, 0);
    FAILIF(in == MAP_FAILED, "could not mmap input file: %s (%d)\n",
           strerror(errno), errno);

    INFO("Truncating output file %s to %d bytes\n", outfile, outsize);
    FAILIF(ftruncate(ofd, outsize) < 0,
           "Could not truncate output file to required size: %s (%d)\n",
           strerror(errno), errno);

    INFO("Memory mapping output file %s\n", outfile);
    out = mmap(0, outsize, PROT_WRITE, MAP_SHARED, ofd, 0);
    FAILIF(out == MAP_FAILED, "could not mmap output file: %s (%d)\n",
           strerror(errno), errno);

    INFO("PPM header (%d) bytes:\n%s\n", header_size, header);
    FAILIF(write(ofd, header, header_size) != header_size,
           "Error wrinting PPM header: %s (%d)\n",
           strerror(errno), errno);

    INFO("Converting %dx%d YUV 4:2:0 to RGB24...\n", width, height);
    color_convert_common(in, in + width * height,
                         width, height, 
                         out + header_size, outsize - header_size,
                         gray, rotate,
                         type == CONVERT_TYPE_ARGB ? argb_cb : rgb24_cb);
}

int verbose_flag;
int quiet_flag;

int main(int argc, char **argv) {

    char *infile, *outfile, *type;
    int height, width, gray, rotate;
    int cmdline_error = 0;

    /* Parse command-line arguments. */
    
    int first = get_options(argc, argv,
                            &outfile,
                            &height,
                            &width,
                            &gray,
                            &type,
                            &rotate,
                            &verbose_flag);

    if (first == argc) {
        ERROR("You must specify an input file!\n");
        cmdline_error++;
    }
    if (!outfile) {
        ERROR("You must specify an output file!\n");
        cmdline_error++;
    }
    if (height < 0 || width < 0) {
        ERROR("You must specify both image height and width!\n");
        cmdline_error++;
    }

    FAILIF(rotate % 90, "Rotation angle must be a multiple of 90 degrees!\n");

    rotate /= 90;
    rotate %= 4;
    if (rotate < 0) rotate += 4;

    if (cmdline_error) {
        print_help(argv[0]);
        exit(1);
    }

    infile = argv[first];

    INFO("input file: [%s]\n", infile);
    INFO("output file: [%s]\n", outfile);
    INFO("height: %d\n", height);
    INFO("width: %d\n", width);
    INFO("gray only: %d\n", gray);
    INFO("encode as: %s\n", type);
    INFO("rotation: %d\n", rotate);
    
    /* Convert the image */

    int conv_type;
    if (!type || !strcmp(type, "ppm"))
        conv_type = CONVERT_TYPE_PPM;
    else if (!strcmp(type, "rgb"))
        conv_type = CONVERT_TYPE_RGB;
    else if (!strcmp(type, "argb"))
        conv_type = CONVERT_TYPE_ARGB;
    else FAILIF(1, "Unknown encoding type %s.\n", type);
    
    convert(infile, outfile,
            height, width, gray,
            conv_type,
            rotate);
        
    free(outfile);
    return 0;
}
