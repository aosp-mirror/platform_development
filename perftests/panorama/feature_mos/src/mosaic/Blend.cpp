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
// Blend.cpp
// $Id: Blend.cpp,v 1.22 2011/06/24 04:22:14 mbansal Exp $

#include <string.h>

#include "Interp.h"
#include "Blend.h"

#include "Geometry.h"
#include "trsMatrix.h"

Blend::Blend()
{
  m_wb.blendingType = BLEND_TYPE_NONE;
}

Blend::~Blend()
{
    if (m_pFrameVPyr) free(m_pFrameVPyr);
    if (m_pFrameUPyr) free(m_pFrameUPyr);
    if (m_pFrameYPyr) free(m_pFrameYPyr);
}

int Blend::initialize(int blendingType, int stripType, int frame_width, int frame_height)
{
    this->width = frame_width;
    this->height = frame_height;
    this->m_wb.blendingType = blendingType;
    this->m_wb.stripType = stripType;

    m_wb.blendRange = m_wb.blendRangeUV = BLEND_RANGE_DEFAULT;
    m_wb.nlevs = m_wb.blendRange;
    m_wb.nlevsC = m_wb.blendRangeUV;

    if (m_wb.nlevs <= 0) m_wb.nlevs = 1; // Need levels for YUV processing
    if (m_wb.nlevsC > m_wb.nlevs) m_wb.nlevsC = m_wb.nlevs;

    m_wb.roundoffOverlap = 1.5;

    m_pFrameYPyr = NULL;
    m_pFrameUPyr = NULL;
    m_pFrameVPyr = NULL;

    m_pFrameYPyr = PyramidShort::allocatePyramidPacked(m_wb.nlevs, (unsigned short) width, (unsigned short) height, BORDER);
    m_pFrameUPyr = PyramidShort::allocatePyramidPacked(m_wb.nlevsC, (unsigned short) (width), (unsigned short) (height), BORDER);
    m_pFrameVPyr = PyramidShort::allocatePyramidPacked(m_wb.nlevsC, (unsigned short) (width), (unsigned short) (height), BORDER);

    if (!m_pFrameYPyr || !m_pFrameUPyr || !m_pFrameVPyr)
    {
        return BLEND_RET_ERROR_MEMORY;
    }

    return BLEND_RET_OK;
}

inline double max(double a, double b) { return a > b ? a : b; }
inline double min(double a, double b) { return a < b ? a : b; }

void Blend::AlignToMiddleFrame(MosaicFrame **frames, int frames_size)
{
    // Unwarp this frame and Warp the others to match
    MosaicFrame *mb = NULL;
    MosaicFrame *ref = frames[int(frames_size/2)];    // Middle frame

    double invtrs[3][3];
    inv33d(ref->trs, invtrs);

    for(int mfit = 0; mfit < frames_size; mfit++)
    {
        mb = frames[mfit];
        double temp[3][3];
        mult33d(temp, invtrs, mb->trs);
        memcpy(mb->trs, temp, sizeof(temp));
        normProjMat33d(mb->trs);
    }
}

int Blend::runBlend(MosaicFrame **oframes, MosaicFrame **rframes,
        int frames_size,
        ImageType &imageMosaicYVU, int &mosaicWidth, int &mosaicHeight,
        float &progress, bool &cancelComputation)
{
    int ret;
    int numCenters;

    MosaicFrame **frames;

    // For THIN strip mode, accept all frames for blending
    if (m_wb.stripType == STRIP_TYPE_THIN)
    {
        frames = oframes;
    }
    else // For WIDE strip mode, first select the relevant frames to blend.
    {
        SelectRelevantFrames(oframes, frames_size, rframes, frames_size);
        frames = rframes;
    }

    ComputeBlendParameters(frames, frames_size, true);
    numCenters = frames_size;

    if (numCenters == 0)
    {
        return BLEND_RET_ERROR;
    }

    if (!(m_AllSites = m_Triangulator.allocMemory(numCenters)))
    {
        return BLEND_RET_ERROR_MEMORY;
    }

    // Bounding rectangle (real numbers) of the final mosaic computed by projecting
    // each input frame into the mosaic coordinate system.
    BlendRect global_rect;

    global_rect.lft = global_rect.bot = 2e30; // min values
    global_rect.rgt = global_rect.top = -2e30; // max values
    MosaicFrame *mb = NULL;
    double halfwidth = width / 2.0;
    double halfheight = height / 2.0;

    double z, x0, y0, x1, y1, x2, y2, x3, y3;

    // Corners of the left-most and right-most frames respectively in the
    // mosaic coordinate system.
    double xLeftCorners[2] = {2e30, 2e30};
    double xRightCorners[2] = {-2e30, -2e30};

    // Corners of the top-most and bottom-most frames respectively in the
    // mosaic coordinate system.
    double yTopCorners[2] = {2e30, 2e30};
    double yBottomCorners[2] = {-2e30, -2e30};


    // Determine the extents of the final mosaic
    CSite *csite = m_AllSites ;
    for(int mfit = 0; mfit < frames_size; mfit++)
    {
        mb = frames[mfit];

        // Compute clipping for this frame's rect
        FrameToMosaicRect(mb->width, mb->height, mb->trs, mb->brect);
        // Clip global rect using this frame's rect
        ClipRect(mb->brect, global_rect);

        // Calculate the corner points
        FrameToMosaic(mb->trs, 0.0,             0.0,            x0, y0);
        FrameToMosaic(mb->trs, 0.0,             mb->height-1.0, x1, y1);
        FrameToMosaic(mb->trs, mb->width-1.0,   mb->height-1.0, x2, y2);
        FrameToMosaic(mb->trs, mb->width-1.0,   0.0,            x3, y3);

        if(x0 < xLeftCorners[0] || x1 < xLeftCorners[1])    // If either of the left corners is lower
        {
            xLeftCorners[0] = x0;
            xLeftCorners[1] = x1;
        }

        if(x3 > xRightCorners[0] || x2 > xRightCorners[1])    // If either of the right corners is higher
        {
            xRightCorners[0] = x3;
            xRightCorners[1] = x2;
        }

        if(y0 < yTopCorners[0] || y3 < yTopCorners[1])    // If either of the top corners is lower
        {
            yTopCorners[0] = y0;
            yTopCorners[1] = y3;
        }

        if(y1 > yBottomCorners[0] || y2 > yBottomCorners[1])    // If either of the bottom corners is higher
        {
            yBottomCorners[0] = y1;
            yBottomCorners[1] = y2;
        }


        // Compute the centroid of the warped region
        FindQuadCentroid(x0, y0, x1, y1, x2, y2, x3, y3, csite->getVCenter().x, csite->getVCenter().y);

        csite->setMb(mb);
        csite++;
    }

    // Get origin and sizes

    // Bounding rectangle (int numbers) of the final mosaic computed by projecting
    // each input frame into the mosaic coordinate system.
    MosaicRect fullRect;

    fullRect.left = (int) floor(global_rect.lft); // min-x
    fullRect.top = (int) floor(global_rect.bot);  // min-y
    fullRect.right = (int) ceil(global_rect.rgt); // max-x
    fullRect.bottom = (int) ceil(global_rect.top);// max-y
    Mwidth = (unsigned short) (fullRect.right - fullRect.left + 1);
    Mheight = (unsigned short) (fullRect.bottom - fullRect.top + 1);

    int xLeftMost, xRightMost;
    int yTopMost, yBottomMost;

    // Rounding up, so that we don't include the gray border.
    xLeftMost = max(0, max(xLeftCorners[0], xLeftCorners[1]) - fullRect.left + 1);
    xRightMost = min(Mwidth - 1, min(xRightCorners[0], xRightCorners[1]) - fullRect.left - 1);

    yTopMost = max(0, max(yTopCorners[0], yTopCorners[1]) - fullRect.top + 1);
    yBottomMost = min(Mheight - 1, min(yBottomCorners[0], yBottomCorners[1]) - fullRect.top - 1);

    if (xRightMost <= xLeftMost || yBottomMost <= yTopMost)
    {
        return BLEND_RET_ERROR;
    }

    // Make sure image width is multiple of 4
    Mwidth = (unsigned short) ((Mwidth + 3) & ~3);
    Mheight = (unsigned short) ((Mheight + 3) & ~3);    // Round up.

    ret = MosaicSizeCheck(LIMIT_SIZE_MULTIPLIER, LIMIT_HEIGHT_MULTIPLIER);
    if (ret != BLEND_RET_OK)
    {
       return ret;
    }

    YUVinfo *imgMos = YUVinfo::allocateImage(Mwidth, Mheight);
    if (imgMos == NULL)
    {
        return BLEND_RET_ERROR_MEMORY;
    }

    // Set the Y image to 255 so we can distinguish when frame idx are written to it
    memset(imgMos->Y.ptr[0], 255, (imgMos->Y.width * imgMos->Y.height));
    // Set the v and u images to black
    memset(imgMos->V.ptr[0], 128, (imgMos->V.width * imgMos->V.height) << 1);

    // Do the triangulation.  It returns a sorted list of edges
    SEdgeVector *edge;
    int n = m_Triangulator.triangulate(&edge, numCenters, width, height);
    m_Triangulator.linkNeighbors(edge, n, numCenters);

    // Bounding rectangle that determines the positioning of the rectangle that is
    // cropped out of the computed mosaic to get rid of the gray borders.
    MosaicRect cropping_rect;

    if (m_wb.horizontal)
    {
        cropping_rect.left = xLeftMost;
        cropping_rect.right = xRightMost;
    }
    else
    {
        cropping_rect.top = yTopMost;
        cropping_rect.bottom = yBottomMost;
    }

    // Do merging and blending :
    ret = DoMergeAndBlend(frames, numCenters, width, height, *imgMos, fullRect,
            cropping_rect, progress, cancelComputation);

    if (m_wb.blendingType == BLEND_TYPE_HORZ)
        CropFinalMosaic(*imgMos, cropping_rect);


    m_Triangulator.freeMemory();    // note: can be called even if delaunay_alloc() wasn't successful

    imageMosaicYVU = imgMos->Y.ptr[0];


    if (m_wb.blendingType == BLEND_TYPE_HORZ)
    {
        mosaicWidth = cropping_rect.right - cropping_rect.left + 1;
        mosaicHeight = cropping_rect.bottom - cropping_rect.top + 1;
    }
    else
    {
        mosaicWidth = Mwidth;
        mosaicHeight = Mheight;
    }

    return ret;
}

int Blend::MosaicSizeCheck(float sizeMultiplier, float heightMultiplier) {
   if (Mwidth < width || Mheight < height) {
        return BLEND_RET_ERROR;
    }

   if ((Mwidth * Mheight) > (width * height * sizeMultiplier)) {
         return BLEND_RET_ERROR;
   }

   // We won't do blending for the cases where users swing the device too much
   // in the secondary direction. We use a short side to determine the
   // secondary direction because users may hold the device in landsape
   // or portrait.
   int shortSide = min(Mwidth, Mheight);
   if (shortSide > height * heightMultiplier) {
       return BLEND_RET_ERROR;
   }

   return BLEND_RET_OK;
}

int Blend::FillFramePyramid(MosaicFrame *mb)
{
    ImageType mbY, mbU, mbV;
    // Lay this image, centered into the temporary buffer
    mbY = mb->image;
    mbU = mb->getU();
    mbV = mb->getV();

    int h, w;

    for(h=0; h<height; h++)
    {
        ImageTypeShort yptr = m_pFrameYPyr->ptr[h];
        ImageTypeShort uptr = m_pFrameUPyr->ptr[h];
        ImageTypeShort vptr = m_pFrameVPyr->ptr[h];

        for(w=0; w<width; w++)
        {
            yptr[w] = (short) ((*(mbY++)) << 3);
            uptr[w] = (short) ((*(mbU++)) << 3);
            vptr[w] = (short) ((*(mbV++)) << 3);
        }
    }

    // Spread the image through the border
    PyramidShort::BorderSpread(m_pFrameYPyr, BORDER, BORDER, BORDER, BORDER);
    PyramidShort::BorderSpread(m_pFrameUPyr, BORDER, BORDER, BORDER, BORDER);
    PyramidShort::BorderSpread(m_pFrameVPyr, BORDER, BORDER, BORDER, BORDER);

    // Generate Laplacian pyramids
    if (!PyramidShort::BorderReduce(m_pFrameYPyr, m_wb.nlevs) || !PyramidShort::BorderExpand(m_pFrameYPyr, m_wb.nlevs, -1) ||
            !PyramidShort::BorderReduce(m_pFrameUPyr, m_wb.nlevsC) || !PyramidShort::BorderExpand(m_pFrameUPyr, m_wb.nlevsC, -1) ||
            !PyramidShort::BorderReduce(m_pFrameVPyr, m_wb.nlevsC) || !PyramidShort::BorderExpand(m_pFrameVPyr, m_wb.nlevsC, -1))
    {
        return BLEND_RET_ERROR;
    }
    else
    {
        return BLEND_RET_OK;
    }
}

int Blend::DoMergeAndBlend(MosaicFrame **frames, int nsite,
             int width, int height, YUVinfo &imgMos, MosaicRect &rect,
             MosaicRect &cropping_rect, float &progress, bool &cancelComputation)
{
    m_pMosaicYPyr = NULL;
    m_pMosaicUPyr = NULL;
    m_pMosaicVPyr = NULL;

    m_pMosaicYPyr = PyramidShort::allocatePyramidPacked(m_wb.nlevs,(unsigned short)rect.Width(),(unsigned short)rect.Height(),BORDER);
    m_pMosaicUPyr = PyramidShort::allocatePyramidPacked(m_wb.nlevsC,(unsigned short)rect.Width(),(unsigned short)rect.Height(),BORDER);
    m_pMosaicVPyr = PyramidShort::allocatePyramidPacked(m_wb.nlevsC,(unsigned short)rect.Width(),(unsigned short)rect.Height(),BORDER);
    if (!m_pMosaicYPyr || !m_pMosaicUPyr || !m_pMosaicVPyr)
    {
      return BLEND_RET_ERROR_MEMORY;
    }

    MosaicFrame *mb;

    CSite *esite = m_AllSites + nsite;
    int site_idx;

    // First go through each frame and for each mosaic pixel determine which frame it should come from
    site_idx = 0;
    for(CSite *csite = m_AllSites; csite < esite; csite++)
    {
        if(cancelComputation)
        {
            if (m_pMosaicVPyr) free(m_pMosaicVPyr);
            if (m_pMosaicUPyr) free(m_pMosaicUPyr);
            if (m_pMosaicYPyr) free(m_pMosaicYPyr);
            return BLEND_RET_CANCELLED;
        }

        mb = csite->getMb();

        mb->vcrect = mb->brect;
        ClipBlendRect(csite, mb->vcrect);

        ComputeMask(csite, mb->vcrect, mb->brect, rect, imgMos, site_idx);

        site_idx++;
    }

    ////////// imgMos.Y, imgMos.V, imgMos.U are used as follows //////////////
    ////////////////////// THIN STRIP MODE ///////////////////////////////////

    // imgMos.Y is used to store the index of the image from which each pixel
    // in the output mosaic can be read out for the thin-strip mode. Thus,
    // there is no special handling for pixels around the seam. Also, imgMos.Y
    // is set to 255 wherever we can't get its value from any input image e.g.
    // in the gray border areas. imgMos.V and imgMos.U are set to 128 for the
    // thin-strip mode.

    ////////////////////// WIDE STRIP MODE ///////////////////////////////////

    // imgMos.Y is used the same way as the thin-strip mode.
    // imgMos.V is used to store the index of the neighboring image which
    // should contribute to the color of an output pixel in a band around
    // the seam. Thus, in this band, we will crossfade between the color values
    // from the image index imgMos.Y and image index imgMos.V. imgMos.U is
    // used to store the weight (multiplied by 100) that each image will
    // contribute to the blending process. Thus, we start at 99% contribution
    // from the first image, then go to 50% contribution from each image at
    // the seam. Then, the contribution from the second image goes up to 99%.

    // For WIDE mode, set the pixel masks to guide the blender to cross-fade
    // between the images on either side of each seam:
    if (m_wb.stripType == STRIP_TYPE_WIDE)
    {
        if(m_wb.horizontal)
        {
            // Set the number of pixels around the seam to cross-fade between
            // the two component images,
            int tw = STRIP_CROSS_FADE_WIDTH_PXLS;

            // Proceed with the image index calculation for cross-fading
            // only if the cross-fading width is larger than 0
            if (tw > 0)
            {
                for(int y = 0; y < imgMos.Y.height; y++)
                {
                    // Since we compare two adjecant pixels to determine
                    // whether there is a seam, the termination condition of x
                    // is set to imgMos.Y.width - tw, so that x+1 below
                    // won't exceed the imgMos' boundary.
                    for(int x = tw; x < imgMos.Y.width - tw; )
                    {
                        // Determine where the seam is...
                        if (imgMos.Y.ptr[y][x] != imgMos.Y.ptr[y][x+1] &&
                                imgMos.Y.ptr[y][x] != 255 &&
                                imgMos.Y.ptr[y][x+1] != 255)
                        {
                            // Find the image indices on both sides of the seam
                            unsigned char idx1 = imgMos.Y.ptr[y][x];
                            unsigned char idx2 = imgMos.Y.ptr[y][x+1];

                            for (int o = tw; o >= 0; o--)
                            {
                                // Set the image index to use for cross-fading
                                imgMos.V.ptr[y][x - o] = idx2;
                                // Set the intensity weights to use for cross-fading
                                imgMos.U.ptr[y][x - o] = 50 + (99 - 50) * o / tw;
                            }

                            for (int o = 1; o <= tw; o++)
                            {
                                // Set the image index to use for cross-fading
                                imgMos.V.ptr[y][x + o] = idx1;
                                // Set the intensity weights to use for cross-fading
                                imgMos.U.ptr[y][x + o] = imgMos.U.ptr[y][x - o];
                            }

                            x += (tw + 1);
                        }
                        else
                        {
                            x++;
                        }
                    }
                }
            }
        }
        else
        {
            // Set the number of pixels around the seam to cross-fade between
            // the two component images,
            int tw = STRIP_CROSS_FADE_WIDTH_PXLS;

            // Proceed with the image index calculation for cross-fading
            // only if the cross-fading width is larger than 0
            if (tw > 0)
            {
                for(int x = 0; x < imgMos.Y.width; x++)
                {
                    // Since we compare two adjecant pixels to determine
                    // whether there is a seam, the termination condition of y
                    // is set to imgMos.Y.height - tw, so that y+1 below
                    // won't exceed the imgMos' boundary.
                    for(int y = tw; y < imgMos.Y.height - tw; )
                    {
                        // Determine where the seam is...
                        if (imgMos.Y.ptr[y][x] != imgMos.Y.ptr[y+1][x] &&
                                imgMos.Y.ptr[y][x] != 255 &&
                                imgMos.Y.ptr[y+1][x] != 255)
                        {
                            // Find the image indices on both sides of the seam
                            unsigned char idx1 = imgMos.Y.ptr[y][x];
                            unsigned char idx2 = imgMos.Y.ptr[y+1][x];

                            for (int o = tw; o >= 0; o--)
                            {
                                // Set the image index to use for cross-fading
                                imgMos.V.ptr[y - o][x] = idx2;
                                // Set the intensity weights to use for cross-fading
                                imgMos.U.ptr[y - o][x] = 50 + (99 - 50) * o / tw;
                            }

                            for (int o = 1; o <= tw; o++)
                            {
                                // Set the image index to use for cross-fading
                                imgMos.V.ptr[y + o][x] = idx1;
                                // Set the intensity weights to use for cross-fading
                                imgMos.U.ptr[y + o][x] = imgMos.U.ptr[y - o][x];
                            }

                            y += (tw + 1);
                        }
                        else
                        {
                            y++;
                        }
                    }
                }
            }
        }

    }

    // Now perform the actual blending using the frame assignment determined above
    site_idx = 0;
    for(CSite *csite = m_AllSites; csite < esite; csite++)
    {
        if(cancelComputation)
        {
            if (m_pMosaicVPyr) free(m_pMosaicVPyr);
            if (m_pMosaicUPyr) free(m_pMosaicUPyr);
            if (m_pMosaicYPyr) free(m_pMosaicYPyr);
            return BLEND_RET_CANCELLED;
        }

        mb = csite->getMb();


        if(FillFramePyramid(mb)!=BLEND_RET_OK)
            return BLEND_RET_ERROR;

        ProcessPyramidForThisFrame(csite, mb->vcrect, mb->brect, rect, imgMos, mb->trs, site_idx);

        progress += TIME_PERCENT_BLEND/nsite;

        site_idx++;
    }


    // Blend
    PerformFinalBlending(imgMos, cropping_rect);

    if (cropping_rect.Width() <= 0 || cropping_rect.Height() <= 0)
    {
        return BLEND_RET_ERROR;
    }

    if (m_pMosaicVPyr) free(m_pMosaicVPyr);
    if (m_pMosaicUPyr) free(m_pMosaicUPyr);
    if (m_pMosaicYPyr) free(m_pMosaicYPyr);

    progress += TIME_PERCENT_FINAL;

    return BLEND_RET_OK;
}

void Blend::CropFinalMosaic(YUVinfo &imgMos, MosaicRect &cropping_rect)
{
    int i, j, k;
    ImageType yimg;
    ImageType uimg;
    ImageType vimg;


    yimg = imgMos.Y.ptr[0];
    uimg = imgMos.U.ptr[0];
    vimg = imgMos.V.ptr[0];

    k = 0;
    for (j = cropping_rect.top; j <= cropping_rect.bottom; j++)
    {
        for (i = cropping_rect.left; i <= cropping_rect.right; i++)
        {
            yimg[k] = yimg[j*imgMos.Y.width+i];
            k++;
        }
    }
    for (j = cropping_rect.top; j <= cropping_rect.bottom; j++)
    {
       for (i = cropping_rect.left; i <= cropping_rect.right; i++)
        {
            yimg[k] = vimg[j*imgMos.Y.width+i];
            k++;
        }
    }
    for (j = cropping_rect.top; j <= cropping_rect.bottom; j++)
    {
       for (i = cropping_rect.left; i <= cropping_rect.right; i++)
        {
            yimg[k] = uimg[j*imgMos.Y.width+i];
            k++;
        }
    }
}

int Blend::PerformFinalBlending(YUVinfo &imgMos, MosaicRect &cropping_rect)
{
    if (!PyramidShort::BorderExpand(m_pMosaicYPyr, m_wb.nlevs, 1) || !PyramidShort::BorderExpand(m_pMosaicUPyr, m_wb.nlevsC, 1) ||
        !PyramidShort::BorderExpand(m_pMosaicVPyr, m_wb.nlevsC, 1))
    {
      return BLEND_RET_ERROR;
    }

    ImageTypeShort myimg;
    ImageTypeShort muimg;
    ImageTypeShort mvimg;
    ImageType yimg;
    ImageType uimg;
    ImageType vimg;

    int cx = (int)imgMos.Y.width/2;
    int cy = (int)imgMos.Y.height/2;

    // 2D boolean array that contains true wherever the mosaic image data is
    // invalid (i.e. in the gray border).
    bool **b = new bool*[imgMos.Y.height];

    for(int j=0; j<imgMos.Y.height; j++)
    {
        b[j] = new bool[imgMos.Y.width];
    }

    // Copy the resulting image into the full image using the mask
    int i, j;

    yimg = imgMos.Y.ptr[0];
    uimg = imgMos.U.ptr[0];
    vimg = imgMos.V.ptr[0];

    for (j = 0; j < imgMos.Y.height; j++)
    {
        myimg = m_pMosaicYPyr->ptr[j];
        muimg = m_pMosaicUPyr->ptr[j];
        mvimg = m_pMosaicVPyr->ptr[j];

        for (i = 0; i<imgMos.Y.width; i++)
        {
            // A final mask was set up previously,
            // if the value is zero skip it, otherwise replace it.
            if (*yimg <255)
            {
                short value = (short) ((*myimg) >> 3);
                if (value < 0) value = 0;
                else if (value > 255) value = 255;
                *yimg = (unsigned char) value;

                value = (short) ((*muimg) >> 3);
                if (value < 0) value = 0;
                else if (value > 255) value = 255;
                *uimg = (unsigned char) value;

                value = (short) ((*mvimg) >> 3);
                if (value < 0) value = 0;
                else if (value > 255) value = 255;
                *vimg = (unsigned char) value;

                b[j][i] = false;

            }
            else
            {   // set border color in here
                *yimg = (unsigned char) 96;
                *uimg = (unsigned char) 128;
                *vimg = (unsigned char) 128;

                b[j][i] = true;
            }

            yimg++;
            uimg++;
            vimg++;
            myimg++;
            muimg++;
            mvimg++;
        }
    }

    if(m_wb.horizontal)
    {
        //Scan through each row and increment top if the row contains any gray
        for (j = 0; j < imgMos.Y.height; j++)
        {
            for (i = cropping_rect.left; i < cropping_rect.right; i++)
            {
                if (b[j][i])
                {
                    break; // to next row
                }
            }

            if (i == cropping_rect.right)   //no gray pixel in this row!
            {
                cropping_rect.top = j;
                break;
            }
        }

        //Scan through each row and decrement bottom if the row contains any gray
        for (j = imgMos.Y.height-1; j >= 0; j--)
        {
            for (i = cropping_rect.left; i < cropping_rect.right; i++)
            {
                if (b[j][i])
                {
                    break; // to next row
                }
            }

            if (i == cropping_rect.right)   //no gray pixel in this row!
            {
                cropping_rect.bottom = j;
                break;
            }
        }
    }
    else // Vertical Mosaic
    {
        //Scan through each column and increment left if the column contains any gray
        for (i = 0; i < imgMos.Y.width; i++)
        {
            for (j = cropping_rect.top; j < cropping_rect.bottom; j++)
            {
                if (b[j][i])
                {
                    break; // to next column
                }
            }

            if (j == cropping_rect.bottom)   //no gray pixel in this column!
            {
                cropping_rect.left = i;
                break;
            }
        }

        //Scan through each column and decrement right if the column contains any gray
        for (i = imgMos.Y.width-1; i >= 0; i--)
        {
            for (j = cropping_rect.top; j < cropping_rect.bottom; j++)
            {
                if (b[j][i])
                {
                    break; // to next column
                }
            }

            if (j == cropping_rect.bottom)   //no gray pixel in this column!
            {
                cropping_rect.right = i;
                break;
            }
        }

    }

    RoundingCroppingSizeToMultipleOf8(cropping_rect);

    for(int j=0; j<imgMos.Y.height; j++)
    {
        delete b[j];
    }

    delete b;

    return BLEND_RET_OK;
}

void Blend::RoundingCroppingSizeToMultipleOf8(MosaicRect &rect) {
    int height = rect.bottom - rect.top + 1;
    int residue = height & 7;
    rect.bottom -= residue;

    int width = rect.right - rect.left + 1;
    residue = width & 7;
    rect.right -= residue;
}

void Blend::ComputeMask(CSite *csite, BlendRect &vcrect, BlendRect &brect, MosaicRect &rect, YUVinfo &imgMos, int site_idx)
{
    PyramidShort *dptr = m_pMosaicYPyr;

    int nC = m_wb.nlevsC;
    int l = (int) ((vcrect.lft - rect.left));
    int b = (int) ((vcrect.bot - rect.top));
    int r = (int) ((vcrect.rgt - rect.left));
    int t = (int) ((vcrect.top - rect.top));

    if (vcrect.lft == brect.lft)
        l = (l <= 0) ? -BORDER : l - BORDER;
    else if (l < -BORDER)
        l = -BORDER;

    if (vcrect.bot == brect.bot)
        b = (b <= 0) ? -BORDER : b - BORDER;
    else if (b < -BORDER)
        b = -BORDER;

    if (vcrect.rgt == brect.rgt)
        r = (r >= dptr->width) ? dptr->width + BORDER - 1 : r + BORDER;
    else if (r >= dptr->width + BORDER)
        r = dptr->width + BORDER - 1;

    if (vcrect.top == brect.top)
        t = (t >= dptr->height) ? dptr->height + BORDER - 1 : t + BORDER;
    else if (t >= dptr->height + BORDER)
        t = dptr->height + BORDER - 1;

    // Walk the Region of interest and populate the pyramid
    for (int j = b; j <= t; j++)
    {
        int jj = j;
        double sj = jj + rect.top;

        for (int i = l; i <= r; i++)
        {
            int ii = i;
            // project point and then triangulate to neighbors
            double si = ii + rect.left;

            double dself = hypotSq(csite->getVCenter().x - si, csite->getVCenter().y - sj);
            int inMask = ((unsigned) ii < imgMos.Y.width &&
                    (unsigned) jj < imgMos.Y.height) ? 1 : 0;

            if(!inMask)
                continue;

            // scan the neighbors to see if this is a valid position
            unsigned char mask = (unsigned char) 255;
            SEdgeVector *ce;
            int ecnt;
            for (ce = csite->getNeighbor(), ecnt = csite->getNumNeighbors(); ecnt--; ce++)
            {
                double d1 = hypotSq(m_AllSites[ce->second].getVCenter().x - si,
                        m_AllSites[ce->second].getVCenter().y - sj);
                if (d1 < dself)
                {
                    break;
                }
            }

            if (ecnt >= 0) continue;

            imgMos.Y.ptr[jj][ii] = (unsigned char)site_idx;
        }
    }
}

void Blend::ProcessPyramidForThisFrame(CSite *csite, BlendRect &vcrect, BlendRect &brect, MosaicRect &rect, YUVinfo &imgMos, double trs[3][3], int site_idx)
{
    // Put the Region of interest (for all levels) into m_pMosaicYPyr
    double inv_trs[3][3];
    inv33d(trs, inv_trs);

    // Process each pyramid level
    PyramidShort *sptr = m_pFrameYPyr;
    PyramidShort *suptr = m_pFrameUPyr;
    PyramidShort *svptr = m_pFrameVPyr;

    PyramidShort *dptr = m_pMosaicYPyr;
    PyramidShort *duptr = m_pMosaicUPyr;
    PyramidShort *dvptr = m_pMosaicVPyr;

    int dscale = 0; // distance scale for the current level
    int nC = m_wb.nlevsC;
    for (int n = m_wb.nlevs; n--; dscale++, dptr++, sptr++, dvptr++, duptr++, svptr++, suptr++, nC--)
    {
        int l = (int) ((vcrect.lft - rect.left) / (1 << dscale));
        int b = (int) ((vcrect.bot - rect.top) / (1 << dscale));
        int r = (int) ((vcrect.rgt - rect.left) / (1 << dscale) + .5);
        int t = (int) ((vcrect.top - rect.top) / (1 << dscale) + .5);

        if (vcrect.lft == brect.lft)
            l = (l <= 0) ? -BORDER : l - BORDER;
        else if (l < -BORDER)
            l = -BORDER;

        if (vcrect.bot == brect.bot)
            b = (b <= 0) ? -BORDER : b - BORDER;
        else if (b < -BORDER)
            b = -BORDER;

        if (vcrect.rgt == brect.rgt)
            r = (r >= dptr->width) ? dptr->width + BORDER - 1 : r + BORDER;
        else if (r >= dptr->width + BORDER)
            r = dptr->width + BORDER - 1;

        if (vcrect.top == brect.top)
            t = (t >= dptr->height) ? dptr->height + BORDER - 1 : t + BORDER;
        else if (t >= dptr->height + BORDER)
            t = dptr->height + BORDER - 1;

        // Walk the Region of interest and populate the pyramid
        for (int j = b; j <= t; j++)
        {
            int jj = (j << dscale);
            double sj = jj + rect.top;

            for (int i = l; i <= r; i++)
            {
                int ii = (i << dscale);
                // project point and then triangulate to neighbors
                double si = ii + rect.left;

                int inMask = ((unsigned) ii < imgMos.Y.width &&
                        (unsigned) jj < imgMos.Y.height) ? 1 : 0;

                if(inMask && imgMos.Y.ptr[jj][ii] != site_idx &&
                        imgMos.V.ptr[jj][ii] != site_idx &&
                        imgMos.Y.ptr[jj][ii] != 255)
                    continue;

                // Setup weights for cross-fading
                // Weight of the intensity already in the output pixel
                double wt0 = 0.0;
                // Weight of the intensity from the input pixel (current frame)
                double wt1 = 1.0;

                if (m_wb.stripType == STRIP_TYPE_WIDE)
                {
                    if(inMask && imgMos.Y.ptr[jj][ii] != 255)
                    {
                        // If not on a seam OR pyramid level exceeds
                        // maximum level for cross-fading.
                        if((imgMos.V.ptr[jj][ii] == 128) ||
                            (dscale > STRIP_CROSS_FADE_MAX_PYR_LEVEL))
                        {
                            wt0 = 0.0;
                            wt1 = 1.0;
                        }
                        else
                        {
                            wt0 = 1.0;
                            wt1 = ((imgMos.Y.ptr[jj][ii] == site_idx) ?
                                    (double)imgMos.U.ptr[jj][ii] / 100.0 :
                                    1.0 - (double)imgMos.U.ptr[jj][ii] / 100.0);
                        }
                    }
                }

                // Project this mosaic point into the original frame coordinate space
                double xx, yy;

                MosaicToFrame(inv_trs, si, sj, xx, yy);

                if (xx < 0.0 || yy < 0.0 || xx > width - 1.0 || yy > height - 1.0)
                {
                    if(inMask)
                    {
                        imgMos.Y.ptr[jj][ii] = 255;
                        wt0 = 0.0f;
                        wt1 = 1.0f;
                    }
                }

                xx /= (1 << dscale);
                yy /= (1 << dscale);


                int x1 = (xx >= 0.0) ? (int) xx : (int) floor(xx);
                int y1 = (yy >= 0.0) ? (int) yy : (int) floor(yy);

                // Final destination in extended pyramid
#ifndef LINEAR_INTERP
                if(inSegment(x1, sptr->width, BORDER-1) &&
                        inSegment(y1, sptr->height, BORDER-1))
                {
                    double xfrac = xx - x1;
                    double yfrac = yy - y1;
                    dptr->ptr[j][i] = (short) (wt0 * dptr->ptr[j][i] + .5 +
                            wt1 * ciCalc(sptr, x1, y1, xfrac, yfrac));
                    if (dvptr >= m_pMosaicVPyr && nC > 0)
                    {
                        duptr->ptr[j][i] = (short) (wt0 * duptr->ptr[j][i] + .5 +
                                wt1 * ciCalc(suptr, x1, y1, xfrac, yfrac));
                        dvptr->ptr[j][i] = (short) (wt0 * dvptr->ptr[j][i] + .5 +
                                wt1 * ciCalc(svptr, x1, y1, xfrac, yfrac));
                    }
                }
#else
                if(inSegment(x1, sptr->width, BORDER) && inSegment(y1, sptr->height, BORDER))
                {
                    int x2 = x1 + 1;
                    int y2 = y1 + 1;
                    double xfrac = xx - x1;
                    double yfrac = yy - y1;
                    double y1val = sptr->ptr[y1][x1] +
                        (sptr->ptr[y1][x2] - sptr->ptr[y1][x1]) * xfrac;
                    double y2val = sptr->ptr[y2][x1] +
                        (sptr->ptr[y2][x2] - sptr->ptr[y2][x1]) * xfrac;
                    dptr->ptr[j][i] = (short) (y1val + yfrac * (y2val - y1val));

                    if (dvptr >= m_pMosaicVPyr && nC > 0)
                    {
                        y1val = suptr->ptr[y1][x1] +
                            (suptr->ptr[y1][x2] - suptr->ptr[y1][x1]) * xfrac;
                        y2val = suptr->ptr[y2][x1] +
                            (suptr->ptr[y2][x2] - suptr->ptr[y2][x1]) * xfrac;

                        duptr->ptr[j][i] = (short) (y1val + yfrac * (y2val - y1val));

                        y1val = svptr->ptr[y1][x1] +
                            (svptr->ptr[y1][x2] - svptr->ptr[y1][x1]) * xfrac;
                        y2val = svptr->ptr[y2][x1] +
                            (svptr->ptr[y2][x2] - svptr->ptr[y2][x1]) * xfrac;

                        dvptr->ptr[j][i] = (short) (y1val + yfrac * (y2val - y1val));
                    }
                }
#endif
                else
                {
                    clipToSegment(x1, sptr->width, BORDER);
                    clipToSegment(y1, sptr->height, BORDER);

                    dptr->ptr[j][i] = (short) (wt0 * dptr->ptr[j][i] + 0.5 +
                            wt1 * sptr->ptr[y1][x1] );
                    if (dvptr >= m_pMosaicVPyr && nC > 0)
                    {
                        dvptr->ptr[j][i] = (short) (wt0 * dvptr->ptr[j][i] +
                                0.5 + wt1 * svptr->ptr[y1][x1] );
                        duptr->ptr[j][i] = (short) (wt0 * duptr->ptr[j][i] +
                                0.5 + wt1 * suptr->ptr[y1][x1] );
                    }
                }
            }
        }
    }
}

void Blend::MosaicToFrame(double trs[3][3], double x, double y, double &wx, double &wy)
{
    double X, Y, z;
    if (m_wb.theta == 0.0)
    {
        X = x;
        Y = y;
    }
    else if (m_wb.horizontal)
    {
        double alpha = x * m_wb.direction / m_wb.width;
        double length = (y - alpha * m_wb.correction) * m_wb.direction + m_wb.radius;
        double deltaTheta = m_wb.theta * alpha;
        double sinTheta = sin(deltaTheta);
        double cosTheta = sqrt(1.0 - sinTheta * sinTheta) * m_wb.direction;
        X = length * sinTheta + m_wb.x;
        Y = length * cosTheta + m_wb.y;
    }
    else
    {
        double alpha = y * m_wb.direction / m_wb.width;
        double length = (x - alpha * m_wb.correction) * m_wb.direction + m_wb.radius;
        double deltaTheta = m_wb.theta * alpha;
        double sinTheta = sin(deltaTheta);
        double cosTheta = sqrt(1.0 - sinTheta * sinTheta) * m_wb.direction;
        Y = length * sinTheta + m_wb.y;
        X = length * cosTheta + m_wb.x;
    }
    z = ProjZ(trs, X, Y, 1.0);
    wx = ProjX(trs, X, Y, z, 1.0);
    wy = ProjY(trs, X, Y, z, 1.0);
}

void Blend::FrameToMosaic(double trs[3][3], double x, double y, double &wx, double &wy)
{
    // Project into the intermediate Mosaic coordinate system
    double z = ProjZ(trs, x, y, 1.0);
    double X = ProjX(trs, x, y, z, 1.0);
    double Y = ProjY(trs, x, y, z, 1.0);

    if (m_wb.theta == 0.0)
    {
        // No rotation, then this is all we need to do.
        wx = X;
        wy = Y;
    }
    else if (m_wb.horizontal)
    {
        double deltaX = X - m_wb.x;
        double deltaY = Y - m_wb.y;
        double length = sqrt(deltaX * deltaX + deltaY * deltaY);
        double deltaTheta = asin(deltaX / length);
        double alpha = deltaTheta / m_wb.theta;
        wx = alpha * m_wb.width * m_wb.direction;
        wy = (length - m_wb.radius) * m_wb.direction + alpha * m_wb.correction;
    }
    else
    {
        double deltaX = X - m_wb.x;
        double deltaY = Y - m_wb.y;
        double length = sqrt(deltaX * deltaX + deltaY * deltaY);
        double deltaTheta = asin(deltaY / length);
        double alpha = deltaTheta / m_wb.theta;
        wy = alpha * m_wb.width * m_wb.direction;
        wx = (length - m_wb.radius) * m_wb.direction + alpha * m_wb.correction;
    }
}



// Clip the region of interest as small as possible by using the Voronoi edges of
// the neighbors
void Blend::ClipBlendRect(CSite *csite, BlendRect &brect)
{
      SEdgeVector *ce;
      int ecnt;
      for (ce = csite->getNeighbor(), ecnt = csite->getNumNeighbors(); ecnt--; ce++)
      {
        // calculate the Voronoi bisector intersection
        const double epsilon = 1e-5;
        double dx = (m_AllSites[ce->second].getVCenter().x - m_AllSites[ce->first].getVCenter().x);
        double dy = (m_AllSites[ce->second].getVCenter().y - m_AllSites[ce->first].getVCenter().y);
        double xmid = m_AllSites[ce->first].getVCenter().x + dx/2.0;
        double ymid = m_AllSites[ce->first].getVCenter().y + dy/2.0;
        double inter;

        if (dx > epsilon)
        {
          // neighbor is on right
          if ((inter = m_wb.roundoffOverlap + xmid - dy * (((dy >= 0.0) ? brect.bot : brect.top) - ymid) / dx) < brect.rgt)
            brect.rgt = inter;
        }
        else if (dx < -epsilon)
        {
          // neighbor is on left
          if ((inter = -m_wb.roundoffOverlap + xmid - dy * (((dy >= 0.0) ? brect.bot : brect.top) - ymid) / dx) > brect.lft)
            brect.lft = inter;
        }
        if (dy > epsilon)
        {
          // neighbor is above
          if ((inter = m_wb.roundoffOverlap + ymid - dx * (((dx >= 0.0) ? brect.lft : brect.rgt) - xmid) / dy) < brect.top)
            brect.top = inter;
        }
        else if (dy < -epsilon)
        {
          // neighbor is below
          if ((inter = -m_wb.roundoffOverlap + ymid - dx * (((dx >= 0.0) ? brect.lft : brect.rgt) - xmid) / dy) > brect.bot)
            brect.bot = inter;
        }
      }
}

void Blend::FrameToMosaicRect(int width, int height, double trs[3][3], BlendRect &brect)
{
    // We need to walk the perimeter since the borders can be bent.
    brect.lft = brect.bot = 2e30;
    brect.rgt = brect.top = -2e30;
    double xpos, ypos;
    double lasty = height - 1.0;
    double lastx = width - 1.0;
    int i;

    for (i = width; i--;)
    {

        FrameToMosaic(trs, (double) i, 0.0, xpos, ypos);
        ClipRect(xpos, ypos, brect);
        FrameToMosaic(trs, (double) i, lasty, xpos, ypos);
        ClipRect(xpos, ypos, brect);
    }
    for (i = height; i--;)
    {
        FrameToMosaic(trs, 0.0, (double) i, xpos, ypos);
        ClipRect(xpos, ypos, brect);
        FrameToMosaic(trs, lastx, (double) i, xpos, ypos);
        ClipRect(xpos, ypos, brect);
    }
}

void Blend::SelectRelevantFrames(MosaicFrame **frames, int frames_size,
        MosaicFrame **relevant_frames, int &relevant_frames_size)
{
    MosaicFrame *first = frames[0];
    MosaicFrame *last = frames[frames_size-1];
    MosaicFrame *mb;

    double fxpos = first->trs[0][2], fypos = first->trs[1][2];

    double midX = last->width / 2.0;
    double midY = last->height / 2.0;
    double z = ProjZ(first->trs, midX, midY, 1.0);
    double firstX, firstY;
    double prevX = firstX = ProjX(first->trs, midX, midY, z, 1.0);
    double prevY = firstY = ProjY(first->trs, midX, midY, z, 1.0);

    relevant_frames[0] = first; // Add first frame by default
    relevant_frames_size = 1;

    for (int i = 0; i < frames_size - 1; i++)
    {
        mb = frames[i];
        double currX, currY;
        z = ProjZ(mb->trs, midX, midY, 1.0);
        currX = ProjX(mb->trs, midX, midY, z, 1.0);
        currY = ProjY(mb->trs, midX, midY, z, 1.0);
        double deltaX = currX - prevX;
        double deltaY = currY - prevY;
        double center2centerDist = sqrt(deltaY * deltaY + deltaX * deltaX);

        if (fabs(deltaX) > STRIP_SEPARATION_THRESHOLD_PXLS ||
                fabs(deltaY) > STRIP_SEPARATION_THRESHOLD_PXLS)
        {
            relevant_frames[relevant_frames_size] = mb;
            relevant_frames_size++;

            prevX = currX;
            prevY = currY;
        }
    }

    // Add last frame by default
    relevant_frames[relevant_frames_size] = last;
    relevant_frames_size++;
}

void Blend::ComputeBlendParameters(MosaicFrame **frames, int frames_size, int is360)
{
    // For FULL and PAN modes, we do not unwarp the mosaic into a rectangular coordinate system
    // and so we set the theta to 0 and return.
    if (m_wb.blendingType != BLEND_TYPE_CYLPAN && m_wb.blendingType != BLEND_TYPE_HORZ)
    {
        m_wb.theta = 0.0;
        return;
    }

    MosaicFrame *first = frames[0];
    MosaicFrame *last = frames[frames_size-1];
    MosaicFrame *mb;

    double lxpos = last->trs[0][2], lypos = last->trs[1][2];
    double fxpos = first->trs[0][2], fypos = first->trs[1][2];

    // Calculate warp to produce proper stitching.
    // get x, y displacement
    double midX = last->width / 2.0;
    double midY = last->height / 2.0;
    double z = ProjZ(first->trs, midX, midY, 1.0);
    double firstX, firstY;
    double prevX = firstX = ProjX(first->trs, midX, midY, z, 1.0);
    double prevY = firstY = ProjY(first->trs, midX, midY, z, 1.0);

    double arcLength, lastTheta;
    m_wb.theta = lastTheta = arcLength = 0.0;

    // Step through all the frames to compute the total arc-length of the cone
    // swept while capturing the mosaic (in the original conical coordinate system).
    for (int i = 0; i < frames_size; i++)
    {
        mb = frames[i];
        double currX, currY;
        z = ProjZ(mb->trs, midX, midY, 1.0);
        currX = ProjX(mb->trs, midX, midY, z, 1.0);
        currY = ProjY(mb->trs, midX, midY, z, 1.0);
        double deltaX = currX - prevX;
        double deltaY = currY - prevY;

        // The arcLength is computed by summing the lengths of the chords
        // connecting the pairwise projected image centers of the input image frames.
        arcLength += sqrt(deltaY * deltaY + deltaX * deltaX);

        if (!is360)
        {
            double thisTheta = asin(mb->trs[1][0]);
            m_wb.theta += thisTheta - lastTheta;
            lastTheta = thisTheta;
        }

        prevX = currX;
        prevY = currY;
    }

    // Stretch this to end at the proper alignment i.e. the width of the
    // rectangle is determined by the arcLength computed above and the cone
    // sector angle is determined using the rotation of the last frame.
    m_wb.width = arcLength;
    if (is360) m_wb.theta = asin(last->trs[1][0]);

    // If there is no rotation, we're done.
    if (m_wb.theta != 0.0)
    {
        double dx = prevX - firstX;
        double dy = prevY - firstY;

        // If the mosaic was captured by sweeping horizontally
        if (abs(lxpos - fxpos) > abs(lypos - fypos))
        {
            m_wb.horizontal = 1;
            // Calculate radius position to make ends exactly the same Y offset
            double radiusTheta = dx / cos(3.14159 / 2.0 - m_wb.theta);
            m_wb.radius = dy + radiusTheta * cos(m_wb.theta);
            if (m_wb.radius < 0.0) m_wb.radius = -m_wb.radius;
        }
        else
        {
            m_wb.horizontal = 0;
            // Calculate radius position to make ends exactly the same Y offset
            double radiusTheta = dy / cos(3.14159 / 2.0 - m_wb.theta);
            m_wb.radius = dx + radiusTheta * cos(m_wb.theta);
            if (m_wb.radius < 0.0) m_wb.radius = -m_wb.radius;
        }

        // Determine major direction
        if (m_wb.horizontal)
        {
            // Horizontal strip
            // m_wb.x,y record the origin of the rectangle coordinate system.
            if (is360) m_wb.x = firstX;
            else
            {
                if (lxpos - fxpos < 0)
                {
                    m_wb.x = firstX + midX;
                    z = ProjZ(last->trs, 0.0, midY, 1.0);
                    prevX = ProjX(last->trs, 0.0, midY, z, 1.0);
                    prevY = ProjY(last->trs, 0.0, midY, z, 1.0);
                }
                else
                {
                    m_wb.x = firstX - midX;
                    z = ProjZ(last->trs, last->width - 1.0, midY, 1.0);
                    prevX = ProjX(last->trs, last->width - 1.0, midY, z, 1.0);
                    prevY = ProjY(last->trs, last->width - 1.0, midY, z, 1.0);
                }
            }
            dy = prevY - firstY;
            if (dy < 0.0) m_wb.direction = 1.0;
            else m_wb.direction = -1.0;
            m_wb.y = firstY - m_wb.radius * m_wb.direction;
            if (dy * m_wb.theta > 0.0) m_wb.width = -m_wb.width;
        }
        else
        {
            // Vertical strip
            if (is360) m_wb.y = firstY;
            else
            {
                if (lypos - fypos < 0)
                {
                    m_wb.x = firstY + midY;
                    z = ProjZ(last->trs, midX, 0.0, 1.0);
                    prevX = ProjX(last->trs, midX, 0.0, z, 1.0);
                    prevY = ProjY(last->trs, midX, 0.0, z, 1.0);
                }
                else
                {
                    m_wb.x = firstX - midX;
                    z = ProjZ(last->trs, midX, last->height - 1.0, 1.0);
                    prevX = ProjX(last->trs, midX, last->height - 1.0, z, 1.0);
                    prevY = ProjY(last->trs, midX, last->height - 1.0, z, 1.0);
                }
            }
            dx = prevX - firstX;
            if (dx < 0.0) m_wb.direction = 1.0;
            else m_wb.direction = -1.0;
            m_wb.x = firstX - m_wb.radius * m_wb.direction;
            if (dx * m_wb.theta > 0.0) m_wb.width = -m_wb.width;
        }

        // Calculate the correct correction factor
        double deltaX = prevX - m_wb.x;
        double deltaY = prevY - m_wb.y;
        double length = sqrt(deltaX * deltaX + deltaY * deltaY);
        double deltaTheta = (m_wb.horizontal) ? deltaX : deltaY;
        deltaTheta = asin(deltaTheta / length);
        m_wb.correction = ((m_wb.radius - length) * m_wb.direction) /
            (deltaTheta / m_wb.theta);
    }
}
