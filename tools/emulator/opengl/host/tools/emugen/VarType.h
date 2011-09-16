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
#ifndef __VARTYPE__H__
#define __VARTYPE__H__

#include <string>

class VarConverter {
public:
    VarConverter(size_t bytes) : m_bytes(bytes) {}
    size_t bytes() const { return m_bytes; }
private:
    size_t m_bytes;
};

class Var8 : public VarConverter {
public:
    Var8() : VarConverter(1) {}
};

class Var16 : public VarConverter {
public:
    Var16() : VarConverter(2) {}
};

class Var32 : public VarConverter {
public:
    Var32() : VarConverter(4) {}
};

class Var0 : public VarConverter {
public:
    Var0() : VarConverter(0) {}
};


class VarType {
public:
    VarType() :
        m_id(0), m_name("default_constructed"), m_converter(NULL), m_printFomrat("0x%x"), m_isPointer(false)
    {
    }

    VarType(size_t id, const std::string & name, const VarConverter * converter, const std::string & printFormat , const bool isPointer) :
        m_id(id), m_name(name), m_converter(const_cast<VarConverter *>(converter)), m_printFomrat(printFormat), m_isPointer(isPointer)
    {
    }

    ~VarType()
    {
    }
    const std::string & name() const { return m_name; }
    const std::string & printFormat() const { return m_printFomrat; }
    size_t bytes() const { return m_converter->bytes(); }
    bool isPointer() const { return m_isPointer; }
    size_t id() const { return m_id; }
private:
    size_t m_id;
    std::string m_name;
    VarConverter * m_converter;
    std::string m_printFomrat;
    bool m_isPointer;
};

#endif
