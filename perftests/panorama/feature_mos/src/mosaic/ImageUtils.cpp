/*
 * Copyright (C) 2011 The Android Open Source Project
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

///////////////////////////////////////////////////
// ImageUtils.cpp
// $Id: ImageUtils.cpp,v 1.12 2011/06/17 13:35:48 mbansal Exp $


#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>

#include "ImageUtils.h"

void ImageUtils::rgba2yvu(ImageType out, ImageType in, int width, int height)
{
  int r,g,b, a;
  ImageType yimg = out;
  ImageType vimg = yimg + width*height;
  ImageType uimg = vimg + width*height;
  ImageType image = in;

  for (int ii = 0; ii < height; ii++) {
    for (int ij = 0; ij < width; ij++) {
      r = (*image++);
      g = (*image++);
      b = (*image++);
      a = (*image++);

      if (r < 0) r = 0;
      if (r > 255) r = 255;
      if (g < 0) g = 0;
      if (g > 255) g = 255;
      if (b < 0) b = 0;
      if (b > 255) b = 255;

      int val = (int) (REDY * r + GREENY * g + BLUEY * b) / 1000 + 16;
      if (val < 0) val = 0;
      if (val > 255) val = 255;
      *(yimg) = val;

      val = (int) (REDV * r - GREENV * g - BLUEV * b) / 1000 + 128;
      if (val < 0) val = 0;
      if (val > 255) val = 255;
      *(vimg) = val;

      val = (int) (-REDU * r - GREENU * g + BLUEU * b) / 1000 + 128;
      if (val < 0) val = 0;
      if (val > 255) val = 255;
      *(uimg) = val;

      yimg++;
      uimg++;
      vimg++;
    }
  }
}


void ImageUtils::rgb2yvu(ImageType out, ImageType in, int width, int height)
{
  int r,g,b;
  ImageType yimg = out;
  ImageType vimg = yimg + width*height;
  ImageType uimg = vimg + width*height;
  ImageType image = in;

  for (int ii = 0; ii < height; ii++) {
    for (int ij = 0; ij < width; ij++) {
      r = (*image++);
      g = (*image++);
      b = (*image++);

      if (r < 0) r = 0;
      if (r > 255) r = 255;
      if (g < 0) g = 0;
      if (g > 255) g = 255;
      if (b < 0) b = 0;
      if (b > 255) b = 255;

      int val = (int) (REDY * r + GREENY * g + BLUEY * b) / 1000 + 16;
      if (val < 0) val = 0;
      if (val > 255) val = 255;
      *(yimg) = val;

      val = (int) (REDV * r - GREENV * g - BLUEV * b) / 1000 + 128;
      if (val < 0) val = 0;
      if (val > 255) val = 255;
      *(vimg) = val;

      val = (int) (-REDU * r - GREENU * g + BLUEU * b) / 1000 + 128;
      if (val < 0) val = 0;
      if (val > 255) val = 255;
      *(uimg) = val;

      yimg++;
      uimg++;
      vimg++;
    }
  }
}

ImageType ImageUtils::rgb2gray(ImageType in, int width, int height)
{
  int r,g,b, nr, ng, nb, val;
  ImageType gray = NULL;
  ImageType image = in;
  ImageType out = ImageUtils::allocateImage(width, height, 1);
  ImageType outCopy = out;

  for (int ii = 0; ii < height; ii++) {
    for (int ij = 0; ij < width; ij++) {
      r = (*image++);
      g = (*image++);
      b = (*image++);

      if (r < 0) r = 0;
      if (r > 255) r = 255;
      if (g < 0) g = 0;
      if (g > 255) g = 255;
      if (b < 0) b = 0;
      if (b > 255) b = 255;

      (*outCopy) = ( 0.3*r + 0.59*g + 0.11*b);

      outCopy++;
    }
  }

  return out;
}

ImageType ImageUtils::rgb2gray(ImageType out, ImageType in, int width, int height)
{
  int r,g,b, nr, ng, nb, val;
  ImageType gray = out;
  ImageType image = in;
  ImageType outCopy = out;

  for (int ii = 0; ii < height; ii++) {
    for (int ij = 0; ij < width; ij++) {
      r = (*image++);
      g = (*image++);
      b = (*image++);

      if (r < 0) r = 0;
      if (r > 255) r = 255;
      if (g < 0) g = 0;
      if (g > 255) g = 255;
      if (b < 0) b = 0;
      if (b > 255) b = 255;

      (*outCopy) = ( 0.3*r + 0.59*g + 0.11*b);

      outCopy++;
    }
  }

  return out;

}

ImageType *ImageUtils::imageTypeToRowPointers(ImageType in, int width, int height)
{
  int i;
  int m_h = height;
  int m_w = width;

  ImageType *m_rows = new ImageType[m_h];

  for (i=0;i<m_h;i++) {
    m_rows[i] = &in[(m_w)*i];
  }
  return m_rows;
}

void ImageUtils::yvu2rgb(ImageType out, ImageType in, int width, int height)
{
  int y,v,u, r, g, b;
  unsigned char *yimg = in;
  unsigned char *vimg = yimg + width*height;
  unsigned char *uimg = vimg + width*height;
  unsigned char *image = out;

  for (int i = 0; i < height; i++) {
    for (int j = 0; j < width; j++) {

      y = (*yimg);
      v = (*vimg);
      u = (*uimg);

      if (y < 0) y = 0;
      if (y > 255) y = 255;
      if (u < 0) u = 0;
      if (u > 255) u = 255;
      if (v < 0) v = 0;
      if (v > 255) v = 255;

      b = (int) ( 1.164*(y - 16) + 2.018*(u-128));
      g = (int) ( 1.164*(y - 16) - 0.813*(v-128) - 0.391*(u-128));
      r = (int) ( 1.164*(y - 16) + 1.596*(v-128));

      if (r < 0) r = 0;
      if (r > 255) r = 255;
      if (g < 0) g = 0;
      if (g > 255) g = 255;
      if (b < 0) b = 0;
      if (b > 255) b = 255;

      *(image++) = r;
      *(image++) = g;
      *(image++) = b;

      yimg++;
      uimg++;
      vimg++;

    }
  }
}

void ImageUtils::yvu2bgr(ImageType out, ImageType in, int width, int height)
{
  int y,v,u, r, g, b;
  unsigned char *yimg = in;
  unsigned char *vimg = yimg + width*height;
  unsigned char *uimg = vimg + width*height;
  unsigned char *image = out;

  for (int i = 0; i < height; i++) {
    for (int j = 0; j < width; j++) {

      y = (*yimg);
      v = (*vimg);
      u = (*uimg);

      if (y < 0) y = 0;
      if (y > 255) y = 255;
      if (u < 0) u = 0;
      if (u > 255) u = 255;
      if (v < 0) v = 0;
      if (v > 255) v = 255;

      b = (int) ( 1.164*(y - 16) + 2.018*(u-128));
      g = (int) ( 1.164*(y - 16) - 0.813*(v-128) - 0.391*(u-128));
      r = (int) ( 1.164*(y - 16) + 1.596*(v-128));

      if (r < 0) r = 0;
      if (r > 255) r = 255;
      if (g < 0) g = 0;
      if (g > 255) g = 255;
      if (b < 0) b = 0;
      if (b > 255) b = 255;

      *(image++) = b;
      *(image++) = g;
      *(image++) = r;

      yimg++;
      uimg++;
      vimg++;

    }
  }
}


ImageType ImageUtils::readBinaryPPM(const char *filename, int &width, int &height)
{

  FILE *imgin = NULL;
  int mval=0, format=0, eret;
  ImageType ret = IMAGE_TYPE_NOIMAGE;

  imgin = fopen(filename, "r");
  if (imgin == NULL) {
    fprintf(stderr, "Error: Filename %s not found\n", filename);
    return ret;
  }

  eret = fscanf(imgin, "P%d\n", &format);
  if (format != 6) {
    fprintf(stderr, "Error: readBinaryPPM only supports PPM format (P6)\n");
    return ret;
  }

  eret = fscanf(imgin, "%d %d\n", &width, &height);
  eret = fscanf(imgin, "%d\n", &mval);
  ret  = allocateImage(width, height, IMAGE_TYPE_NUM_CHANNELS);
  eret = fread(ret, sizeof(ImageTypeBase), IMAGE_TYPE_NUM_CHANNELS*width*height, imgin);

  fclose(imgin);

  return ret;

}

void ImageUtils::writeBinaryPPM(ImageType image, const char *filename, int width, int height, int numChannels)
{
  FILE *imgout = fopen(filename, "w");

  if (imgout == NULL) {
    fprintf(stderr, "Error: Filename %s could not be opened for writing\n", filename);
    return;
  }

  if (numChannels == 3) {
    fprintf(imgout, "P6\n%d %d\n255\n", width, height);
  } else if (numChannels == 1) {
    fprintf(imgout, "P5\n%d %d\n255\n", width, height);
  } else {
    fprintf(stderr, "Error: writeBinaryPPM: Unsupported number of channels\n");
  }
  fwrite(image, sizeof(ImageTypeBase), numChannels*width*height, imgout);

  fclose(imgout);

}

ImageType ImageUtils::allocateImage(int width, int height, int numChannels, short int border)
{
  int overallocation = 256;
 return (ImageType) calloc(width*height*numChannels+overallocation, sizeof(ImageTypeBase));
}


void ImageUtils::freeImage(ImageType image)
{
  free(image);
}


// allocation of one color image used for tmp buffers, etc.
// format of contiguous memory block:
//    YUVInfo struct (type + BimageInfo for Y,U, and V),
//    Y row pointers
//    U row pointers
//    V row pointers
//    Y image pixels
//    U image pixels
//    V image pixels
YUVinfo *YUVinfo::allocateImage(unsigned short width, unsigned short height)
{
    unsigned short heightUV, widthUV;

    widthUV = width;
    heightUV = height;

    // figure out how much space to hold all pixels...
    int size = ((width * height * 3) + 8);
    unsigned char *position = 0;

    // VC 8 does not like calling free on yuv->Y.ptr since it is in
    // the middle of a block.  So rearrange the memory layout so after
    // calling mapYUVInforToImage yuv->Y.ptr points to the begginning
    // of the calloc'ed block.
    YUVinfo *yuv = (YUVinfo *) calloc(sizeof(YUVinfo), 1);
    if (yuv) {
        yuv->Y.width  = yuv->Y.pitch = width;
        yuv->Y.height = height;
        yuv->Y.border = yuv->U.border = yuv->V.border = (unsigned short) 0;
        yuv->U.width  = yuv->U.pitch = yuv->V.width = yuv->V.pitch = widthUV;
        yuv->U.height = yuv->V.height = heightUV;

        unsigned char* block = (unsigned char*) calloc(
                sizeof(unsigned char *) * (height + heightUV + heightUV) +
                sizeof(unsigned char) * size, 1);

        position = block;
        unsigned char **y = (unsigned char **) (block + size);

        /* Initialize and assign row pointers */
        yuv->Y.ptr = y;
        yuv->V.ptr = &y[height];
        yuv->U.ptr = &y[height + heightUV];
    }
    if (size)
        mapYUVInfoToImage(yuv, position);
    return yuv;
}

// wrap YUVInfo row pointers around 3 contiguous image (color component) planes.
// position = starting pixel in image.
void YUVinfo::mapYUVInfoToImage(YUVinfo *img, unsigned char *position)
{
    int i;
    for (i = 0; i < img->Y.height; i++, position += img->Y.width)
        img->Y.ptr[i] = position;
    for (i = 0; i < img->V.height; i++, position += img->V.width)
        img->V.ptr[i] = position;
    for (i = 0; i < img->U.height; i++, position += img->U.width)
        img->U.ptr[i] = position;
}


