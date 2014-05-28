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

// pyramid.cpp

#include <stdio.h>
#include <string.h>

#include "Pyramid.h"

// We allocate the entire pyramid into one contiguous storage. This makes
// cleanup easier than fragmented stuff. In addition, we added a "pitch"
// field, so pointer manipulation is much simpler when it would be faster.
PyramidShort *PyramidShort::allocatePyramidPacked(real levels,
        real width, real height, real border)
{
    real border2 = (real) (border << 1);
    int lines, size = calcStorage(width, height, border2, levels, &lines);

    PyramidShort *img = (PyramidShort *) calloc(sizeof(PyramidShort) * levels
            + sizeof(short *) * lines +
            + sizeof(short) * size, 1);

    if (img) {
        PyramidShort *curr, *last;
        ImageTypeShort *y = (ImageTypeShort *) &img[levels];
        ImageTypeShort position = (ImageTypeShort) &y[lines];
        for (last = (curr = img) + levels; curr < last; curr++) {
            curr->width = width;
            curr->height = height;
            curr->border = border;
            curr->pitch = (real) (width + border2);
            curr->ptr = y + border;

            // Assign row pointers
            for (int j = height + border2; j--; y++, position += curr->pitch) {
                *y = position + border;
            }

            width >>= 1;
            height >>= 1;
        }
    }

    return img;
}

// Allocate an image of type short
PyramidShort *PyramidShort::allocateImage(real width, real height, real border)
{
    real border2 = (real) (border << 1);
    PyramidShort *img = (PyramidShort *)
        calloc(sizeof(PyramidShort) + sizeof(short *) * (height + border2) +
                sizeof(short) * (width + border2) * (height + border2), 1);

    if (img) {
        short **y = (short **) &img[1];
        short *position = (short *) &y[height + border2];
        img->width = width;
        img->height = height;
        img->border = border;
        img->pitch = (real) (width + border2);
        img->ptr = y + border;
        position += border; // Move position down to origin of real image

        // Assign row pointers
        for (int j = height + border2; j--; y++, position += img->pitch) {
            *y = position;
        }
    }

    return img;
}

// Free the images
void PyramidShort::freeImage(PyramidShort *image)
{
    if (image != NULL)
        free(image);
}

// Calculate amount of storage needed taking into account the borders, etc.
unsigned int PyramidShort::calcStorage(real width, real height, real border2,   int levels, int *lines)
{
    int size;

    *lines = size = 0;

    while(levels--) {
        size += (width + border2) * (height + border2);
        *lines += height + border2;
        width >>= 1;
        height >>= 1;
    }

    return size;
}

void PyramidShort::BorderSpread(PyramidShort *pyr, int left, int right,
        int top, int bot)
{
    int off, off2, height, h, w;
    ImageTypeShort base;

    if (left || right) {
        off = pyr->border - left;
        off2 = pyr->width + off + pyr->border - right - 1;
        h = pyr->border - top;
        height = pyr->height + (h << 1);
        base = pyr->ptr[-h] - off;

        // spread in X
        for (h = height; h--; base += pyr->pitch) {
            for (w = left; w--;)
                base[-1 - w] = base[0];
            for (w = right; w--;)
                base[off2 + w + 1] = base[off2];
        }
    }

    if (top || bot) {
        // spread in Y
        base = pyr->ptr[top - pyr->border] - pyr->border;
        for (h = top; h--; base -= pyr->pitch) {
            memcpy(base - pyr->pitch, base, pyr->pitch * sizeof(short));
        }

        base = pyr->ptr[pyr->height + pyr->border - bot] - pyr->border;
        for (h = bot; h--; base += pyr->pitch) {
            memcpy(base, base - pyr->pitch, pyr->pitch * sizeof(short));
        }
    }
}

void PyramidShort::BorderExpandOdd(PyramidShort *in, PyramidShort *out, PyramidShort *scr,
        int mode)
{
    int i,j;
    int off = in->border / 2;

    // Vertical Filter
    for (j = -off; j < in->height + off; j++) {
        int j2 = j * 2;
        int limit = scr->width + scr->border;
        for (i = -scr->border; i < limit; i++) {
            int t1 = in->ptr[j][i];
            int t2 = in->ptr[j+1][i];
            scr->ptr[j2][i] = (short)
                ((6 * t1 + (in->ptr[j-1][i] + t2) + 4) >> 3);
            scr->ptr[j2+1][i] = (short)((t1 + t2 + 1) >> 1);
        }
    }

    BorderSpread(scr, 0, 0, 3, 3);

    // Horizontal Filter
    int limit = out->height + out->border;
    for (j = -out->border; j < limit; j++) {
        for (i = -off; i < scr->width + off; i++) {
            int i2 = i * 2;
            int t1 = scr->ptr[j][i];
            int t2 = scr->ptr[j][i+1];
            out->ptr[j][i2] = (short) (out->ptr[j][i2] +
                    (mode * ((6 * t1 +
                              scr->ptr[j][i-1] + t2 + 4) >> 3)));
            out->ptr[j][i2+1] = (short) (out->ptr[j][i2+1] +
                    (mode * ((t1 + t2 + 1) >> 1)));
        }
    }

}

int PyramidShort::BorderExpand(PyramidShort *pyr, int nlev, int mode)
{
    PyramidShort *tpyr = pyr + nlev - 1;
    PyramidShort *scr = allocateImage(pyr[1].width, pyr[0].height, pyr->border);
    if (scr == NULL) return 0;

    if (mode > 0) {
        // Expand and add (reconstruct from Laplacian)
        for (; tpyr > pyr; tpyr--) {
            scr->width = tpyr[0].width;
            scr->height = tpyr[-1].height;
            BorderExpandOdd(tpyr, tpyr - 1, scr, 1);
        }
    }
    else if (mode < 0) {
        // Expand and subtract (build Laplacian)
        while ((pyr++) < tpyr) {
            scr->width = pyr[0].width;
            scr->height = pyr[-1].height;
            BorderExpandOdd(pyr, pyr - 1, scr, -1);
        }
    }

    freeImage(scr);
    return 1;
}

void PyramidShort::BorderReduceOdd(PyramidShort *in, PyramidShort *out, PyramidShort *scr)
{
    ImageTypeShortBase *s, *ns, *ls, *p, *np;

    int off = scr->border - 2;
    s = scr->ptr[-scr->border] - (off >> 1);
    ns = s + scr->pitch;
    ls = scr->ptr[scr->height + scr->border - 1] + scr->pitch - (off >> 1);
    int width = scr->width + scr->border;
    p = in->ptr[-scr->border] - off;
    np = p + in->pitch;

    // treat it as if the whole thing were the image
    for (; s < ls; s = ns, ns += scr->pitch, p = np, np += in->pitch) {
        for (int w = width; w--; s++, p += 2) {
            *s = (short)((((int) p[-2]) + ((int) p[2]) + 8 +    // 1
                        ((((int) p[-1]) + ((int) p[1])) << 2) + // 4
                        ((int) *p) * 6) >> 4);          // 6
        }
    }

    BorderSpread(scr, 5, 4 + ((in->width ^ 1) & 1), 0, 0); //

    s = out->ptr[-(off >> 1)] - out->border;
    ns = s + out->pitch;
    ls = s + out->pitch * (out->height + off);
    p = scr->ptr[-off] - out->border;
    int pitch = scr->pitch;
    int pitch2 = pitch << 1;
    np = p + pitch2;
    for (; s < ls; s = ns, ns += out->pitch, p = np, np += pitch2) {
        for (int w = out->pitch; w--; s++, p++) {
            *s = (short)((((int) p[-pitch2]) + ((int) p[pitch2]) + 8 + // 1
                        ((((int) p[-pitch]) + ((int) p[pitch])) << 2) + // 4
                        ((int) *p) * 6) >> 4);              // 6
        }
    }
    BorderSpread(out, 0, 0, 5, 5);

}

int PyramidShort::BorderReduce(PyramidShort *pyr, int nlev)
{
    PyramidShort *scr = allocateImage(pyr[1].width, pyr[0].height, pyr->border);
    if (scr == NULL)
        return 0;

    BorderSpread(pyr, pyr->border, pyr->border, pyr->border, pyr->border);
    while (--nlev) {
        BorderReduceOdd(pyr, pyr + 1, scr);
        pyr++;
        scr->width = pyr[1].width;
        scr->height = pyr[0].height;
    }

    freeImage(scr);
    return 1;
}
