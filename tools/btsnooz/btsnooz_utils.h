/******************************************************************************
 *
 *  Copyright (C) 2015 Google, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#pragma once

#include <iostream>
#include <vector>

size_t writeBtSnoop(std::ostream &out, std::vector<uint8_t> &in);
int readLog(std::istream &in, std::vector<char> &buffer);
int base64Decode(std::vector<char> &buffer);
int inflate(std::vector<char> &in, std::vector<uint8_t> &out);
