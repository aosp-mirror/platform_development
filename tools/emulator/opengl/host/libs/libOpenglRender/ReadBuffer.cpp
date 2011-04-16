/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "ReadBuffer.h"
#include <string.h>
#include <assert.h>

ReadBuffer::ReadBuffer(IOStream *stream, size_t bufsize)
{
    m_size = bufsize;
    m_stream = stream;
    m_buf = new unsigned char[m_size];
    m_validData = 0;
    m_readPtr = m_buf;
}

ReadBuffer::~ReadBuffer()
{
    delete m_buf;
}

int ReadBuffer::getData()
{
    if (m_validData > 0) {
        memcpy(m_buf, m_readPtr, m_validData);
    }
    m_readPtr = m_buf;
    // get fresh data into the buffer;
    size_t len = m_size - m_validData;
    if (NULL != m_stream->read(m_buf + m_validData, &len)) {
        m_validData += len;
        return len;
    }
    return -1;
}

void ReadBuffer::consume(size_t amount)
{
    assert(amount <= m_validData);
    m_validData -= amount;
    m_readPtr += amount;
}
