/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#include "mosaic/Mosaic.h"
#include "mosaic/ImageUtils.h"

#define MAX_FRAMES 200
#define KERNEL_ITERATIONS 10

const int blendingType = Blend::BLEND_TYPE_HORZ;
const int stripType = Blend::STRIP_TYPE_WIDE;

ImageType yvuFrames[MAX_FRAMES];

int loadImages(const char* basename, int &width, int &height)
{
    char filename[512];
    struct stat filestat;
    int i;

    for (i = 0; i < MAX_FRAMES; i++) {
        sprintf(filename, "%s_%03d.ppm", basename, i + 1);
        if (stat(filename, &filestat) != 0) break;
        ImageType rgbFrame = ImageUtils::readBinaryPPM(filename, width, height);
        yvuFrames[i] = ImageUtils::allocateImage(width, height,
                                ImageUtils::IMAGE_TYPE_NUM_CHANNELS);
        ImageUtils::rgb2yvu(yvuFrames[i], rgbFrame, width, height);
        ImageUtils::freeImage(rgbFrame);
    }
    return i;
}

int main(int argc, char **argv)
{
    struct timespec t1, t2, t3;

    int width, height;
    float totalElapsedTime = 0;

    const char *basename;
    const char *filename;

    if (argc != 3) {
        printf("Usage: %s input_dir output_filename\n", argv[0]);
        return 0;
    } else {
        basename = argv[1];
        filename = argv[2];
    }

    // Load the images outside the computational kernel
    int totalFrames = loadImages(basename, width, height);

    if (totalFrames == 0) {
        printf("Image files not found. Make sure %s exists.\n",
               basename);
        return 1;
    }

    printf("%d frames loaded\n", totalFrames);


    // Interesting stuff is here
    for (int iteration = 0; iteration < KERNEL_ITERATIONS; iteration++)  {
        Mosaic mosaic;

        mosaic.initialize(blendingType, stripType, width, height, -1, false, 0);

        clock_gettime(CLOCK_MONOTONIC, &t1);
        for (int i = 0; i < totalFrames; i++) {
            mosaic.addFrame(yvuFrames[i]);
        }
        clock_gettime(CLOCK_MONOTONIC, &t2);

        float progress = 0.0;
        bool cancelComputation = false;

        mosaic.createMosaic(progress, cancelComputation);

        int mosaicWidth, mosaicHeight;
        ImageType resultYVU = mosaic.getMosaic(mosaicWidth, mosaicHeight);

        ImageType imageRGB = ImageUtils::allocateImage(
            mosaicWidth, mosaicHeight, ImageUtils::IMAGE_TYPE_NUM_CHANNELS);

        clock_gettime(CLOCK_MONOTONIC, &t3);

        float elapsedTime =
            (t3.tv_sec - t1.tv_sec) + (t3.tv_nsec - t1.tv_nsec)/1e9;
        float addImageTime =
            (t2.tv_sec - t1.tv_sec) + (t2.tv_nsec - t1.tv_nsec)/1e9;
        float stitchImageTime =
            (t3.tv_sec - t2.tv_sec) + (t3.tv_nsec - t2.tv_nsec)/1e9;

        totalElapsedTime += elapsedTime;

        printf("Iteration %d: %dx%d moasic created: "
               "%.2f seconds (%.2f + %.2f)\n",
               iteration, mosaicWidth, mosaicHeight,
               elapsedTime, addImageTime, stitchImageTime);

        // Write the output only once for correctness check
        if (iteration == 0) {
            ImageUtils::yvu2rgb(imageRGB, resultYVU, mosaicWidth,
                                mosaicHeight);
            ImageUtils::writeBinaryPPM(imageRGB, filename, mosaicWidth,
                                       mosaicHeight);
        }
    }
    printf("Total elapsed time: %.2f seconds\n", totalElapsedTime);

    return 0;
}
