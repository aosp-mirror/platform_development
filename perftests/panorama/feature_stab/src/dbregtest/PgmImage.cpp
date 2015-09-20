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

#include "PgmImage.h"
#include <cassert>

using namespace std;

PgmImage::PgmImage(std::string filename) :
m_w(0),m_h(0),m_colors(255),m_format(PGM_BINARY_GRAYMAP),m_over_allocation(256)
{
    if ( !ReadPGM(filename) )
        return;
}

PgmImage::PgmImage(int w, int h, int format) :
m_colors(255),m_w(w),m_h(h),m_format(format),m_over_allocation(256)
{
    SetFormat(format);
}

PgmImage::PgmImage(unsigned char *data, int w, int h) :
m_colors(255),m_w(w),m_h(h),m_format(PGM_BINARY_GRAYMAP),m_over_allocation(256)
{
    SetData(data);
}

PgmImage::PgmImage(std::vector<unsigned char> &data, int w, int h) :
m_colors(255),m_w(w),m_h(h),m_format(PGM_BINARY_GRAYMAP),m_over_allocation(256)
{
    if ( data.size() == w*h )
        SetData(&data[0]);
    else
        //throw (std::exception("Size of data is not w*h."));
        throw (std::exception());
}

PgmImage::PgmImage(const PgmImage &im) :
m_colors(255),m_w(0),m_h(0),m_format(PGM_BINARY_GRAYMAP),m_over_allocation(256)
{
    DeepCopy(im, *this);
}

PgmImage& PgmImage::operator= (const PgmImage &im)
{
    if (this == &im) return *this;
    DeepCopy(im, *this);
    return *this;
}

void PgmImage::DeepCopy(const PgmImage& src, PgmImage& dst)
{
    dst.m_data = src.m_data;

    // PGM data
    dst.m_w = src.m_w;
    dst.m_h = src.m_h;
    dst.m_format = src.m_format;
    dst.m_colors = src.m_colors;

    dst.m_comment = src.m_comment;
    SetupRowPointers();
}

PgmImage::~PgmImage()
{

}

void PgmImage::SetFormat(int format)
{
    m_format = format;

    switch (format)
    {
    case PGM_BINARY_GRAYMAP:
        m_data.resize(m_w*m_h+m_over_allocation);
        break;
    case PGM_BINARY_PIXMAP:
        m_data.resize(m_w*m_h*3+m_over_allocation);
        break;
    default:
        return;
        break;
    }
    SetupRowPointers();
}

void PgmImage::SetData(const unsigned char * data)
{
    m_data.resize(m_w*m_h+m_over_allocation);
    memcpy(&m_data[0],data,m_w*m_h);
    SetupRowPointers();
}

bool PgmImage::ReadPGM(const std::string filename)
{
    ifstream in(filename.c_str(),std::ios::in | std::ios::binary);
    if ( !in.is_open() )
        return false;

    // read the header:
    string format_header,size_header,colors_header;

    getline(in,format_header);
    stringstream s;
    s << format_header;

    s >> format_header >> m_w >> m_h >> m_colors;
    s.clear();

    if ( m_w == 0 )
    {
        while ( in.peek() == '#' )
            getline(in,m_comment);

        getline(in,size_header);

        while ( in.peek() == '#' )
            getline(in,m_comment);

            m_colors = 0;

        // parse header
        s << size_header;
        s >> m_w >> m_h >> m_colors;
        s.clear();

        if ( m_colors == 0 )
        {
            getline(in,colors_header);
            s << colors_header;
            s >> m_colors;
        }
    }

    if ( format_header == "P5" )
        m_format = PGM_BINARY_GRAYMAP;
    else if (format_header == "P6" )
        m_format = PGM_BINARY_PIXMAP;
    else
        m_format = PGM_FORMAT_INVALID;

    switch(m_format)
    {
    case(PGM_BINARY_GRAYMAP):
        m_data.resize(m_w*m_h+m_over_allocation);
        in.read((char *)(&m_data[0]),m_data.size());
        break;
    case(PGM_BINARY_PIXMAP):
        m_data.resize(m_w*m_h*3+m_over_allocation);
        in.read((char *)(&m_data[0]),m_data.size());
        break;
    default:
        return false;
        break;
    }
    in.close();

    SetupRowPointers();

    return true;
}

bool PgmImage::WritePGM(const std::string filename, const std::string comment)
{
    string format_header;

    switch(m_format)
    {
    case PGM_BINARY_GRAYMAP:
        format_header = "P5\n";
        break;
    case PGM_BINARY_PIXMAP:
        format_header = "P6\n";
        break;
    default:
        return false;
        break;
    }

    ofstream out(filename.c_str(),std::ios::out |ios::binary);
    out << format_header << "# " << comment << '\n' << m_w << " " << m_h << '\n' << m_colors << '\n';

    out.write((char *)(&m_data[0]), m_data.size());

    out.close();

    return true;
}

void PgmImage::SetupRowPointers()
{
    int i;
    m_rows.resize(m_h);

    switch (m_format)
    {
    case PGM_BINARY_GRAYMAP:
        for(i=0;i<m_h;i++)
        {
            m_rows[i]=&m_data[m_w*i];
        }
        break;
    case PGM_BINARY_PIXMAP:
        for(i=0;i<m_h;i++)
        {
            m_rows[i]=&m_data[(m_w*3)*i];
        }
        break;
    }
}

void PgmImage::ConvertToGray()
{
    if ( m_format != PGM_BINARY_PIXMAP ) return;

    // Y = 0.3*R + 0.59*G + 0.11*B;
    for ( int i = 0; i < m_w*m_h; ++i )
        m_data[i] = (unsigned char)(0.3*m_data[3*i]+0.59*m_data[3*i+1]+0.11*m_data[3*i+2]);

    m_data.resize(m_w*m_h+m_over_allocation);
    m_format = PGM_BINARY_GRAYMAP;

    SetupRowPointers();
}

std::ostream& operator<< (std::ostream& o, const PgmImage& im)
{
    o << "PGM Image Info:\n";
    o << "Size: " << im.m_w << " x " << im.m_h << "\n";
    o << "Comment: " << im.m_comment << "\n";
    switch (im.m_format)
    {
    case PgmImage::PGM_BINARY_PIXMAP:
        o << "Format: RGB binary pixmap";
        break;
    case PgmImage::PGM_BINARY_GRAYMAP:
        o << "Format: PPM binary graymap";
        break;
    default:
        o << "Format: Invalid";
        break;
    }
    o << endl;
    return o;
}
