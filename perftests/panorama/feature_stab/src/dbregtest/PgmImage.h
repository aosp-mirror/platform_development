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

#pragma once

#include <vector>
#include <iostream>
#include <fstream>
#include <sstream>
#include <memory.h>

/*!
 * Simple class to manipulate PGM/PPM images. Not suitable for heavy lifting.
 */
class PgmImage
{
    friend std::ostream& operator<< (std::ostream& o, const PgmImage& im);
public:
    enum {PGM_BINARY_GRAYMAP,PGM_BINARY_PIXMAP,PGM_FORMAT_INVALID};
    /*!
    * Constructor from a PGM file name.
    */
    PgmImage(std::string filename);
    /*!
    * Constructor to allocate an image of given size and type.
    */
    PgmImage(int w, int h, int format = PGM_BINARY_GRAYMAP);
    /*!
    * Constructor to allocate an image of given size and copy the data in.
    */
    PgmImage(unsigned char *data, int w, int h);
    /*!
    * Constructor to allocate an image of given size and copy the data in.
    */
    PgmImage(std::vector<unsigned char> &data, int w, int h);

    PgmImage(const PgmImage &im);

    PgmImage& operator= (const PgmImage &im);
    ~PgmImage();

    int GetHeight() const { return m_h; }
    int GetWidth() const { return m_w; }

    //! Copy pixels from data pointer
    void SetData(const unsigned char * data);

    //! Get a data pointer to unaligned memory area
    unsigned char * GetDataPointer() { if ( m_data.size() > 0 ) return &m_data[0]; else return NULL; }
    unsigned char ** GetRowPointers() { if ( m_rows.size() == m_h ) return &m_rows[0]; else return NULL; }

    //! Read a PGM file from disk
    bool ReadPGM(const std::string filename);
    //! Write a PGM file to disk
    bool WritePGM(const std::string filename, const std::string comment="");

    //! Get image format (returns PGM_BINARY_GRAYMAP, PGM_BINARY_PIXMAP or PGM_FORMAT_INVALID)
    int GetFormat() const { return m_format; }

    //! Set image format (returns PGM_BINARY_GRAYMAP, PGM_BINARY_PIXMAP). Image data becomes invalid.
    void SetFormat(int format);

    //! If the image is PGM_BINARY_PIXMAP, convert it to PGM_BINARY_GRAYMAP via Y = 0.3*R + 0.59*G + 0.11*B.
    void ConvertToGray();
protected:
    // Generic functions:
    void DeepCopy(const PgmImage& src, PgmImage& dst);
    void SetupRowPointers();

    // PGM data
    int m_w;
    int m_h;
    int m_format;
    int m_colors;
    int m_over_allocation;
    std::vector<unsigned char> m_data;
    std::string m_comment;

    std::vector<unsigned char *> m_rows;
};

std::ostream& operator<< (std::ostream& o, const PgmImage& im);
