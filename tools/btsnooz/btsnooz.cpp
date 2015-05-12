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

#include <iostream>
#include <iomanip>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>

#include "btsnooz_utils.h"

int main(int argc, char *argv[]) {
  if (argc > 3) {
    std::cerr << "Usage: " << argv[0] << " [input_file] [output_file]\n";
    return 1;
  }

  std::vector<char> buffer;

  int read = 0;
  if (argc < 3) {
    std::cerr << "<Reading from stdin>\n";
    read = readLog(std::cin, buffer);

  } else {
    std::cerr << "<Reading " << argv[1] << ">\n";
    std::ifstream ff(argv[1]);
    read = readLog(ff, buffer);
    ff.close();
  }

  if (read == 0) {
    std::cerr << "File not found or not BTSNOOP data block....\n";
    return 2;
  }

  std::cerr << std::setw(8) << read << " bytes of base64 data read\n";

  read = base64Decode(buffer);
  if (read <= 0) {
    std::cerr << "Decoding base64 data failed...\n";
    return 3;
  }

  std::cerr << std::setw(8) << read << " bytes of compressed data decoded\n";

  std::vector<uint8_t> uncompressed;
  read = inflate(buffer, uncompressed);
  if (read <= 0) {
    std::cerr << "Error inflating data...\n";
    return 4;
  }

  std::cerr << std::setw(8) << read << " bytes of data inflated\n";

  if (argc < 2) {
    std::cerr << "<Writing to stdout>\n";
    read = writeBtSnoop(std::cout, uncompressed);

  } else {
    const int arg = argc > 2 ? 2 : 1;
    std::cerr << "<Writing " << argv[arg] << ">\n";
    std::ofstream ff(argv[arg]);
    read = writeBtSnoop(ff, uncompressed);
    ff.close();
  }

  std::cerr << std::setw(8) << read << " btsnoop packets written\n";

  return 0;
}
