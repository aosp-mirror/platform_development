#!/usr/bin/python

# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""summarize and compare the component sizes in installed-files.txt."""

import sys

bin_size1 = {}
bin_size2 = {}
bin_sizes = [bin_size1, bin_size2]

file_sizes = {}

def PrintUsage():
  print "usage: " + sys.argv[0] + " filename [filename2]"
  print ""
  print "  Input file is installed-files.txt from the build output directory."
  print "  When only one input file is given, it will generate module_0.csv."
  print "  When two input files are given, in addition it will generate"
  print "  module_1.csv and comparison.csv."
  print ""
  print "  The module_x.csv file shows the aggregated file size in each module"
  print "  (eg bin, lib, app, ...)"
  print "  The comparison.cvs file shows the individual file sizes side by side"
  print "  from two different builds"
  print ""
  print "  These files can be uploaded to Google Doc for further processing."
  sys.exit(1)

def ParseFile(install_file, idx):
  input_stream = open(install_file, 'r')
  for line in input_stream:
    # line = "25027208  /system/lib/libchromeview.so"
    line = line.strip()

    # size = "25027208", name = "/system/lib/libchromeview.so"
    size, name = line.split()

    # components = ["", "system", "lib", "libchromeview.so"]
    components = name.split('/')

    # module = "lib"
    module = components[2]

    # filename = libchromeview.so"
    filename = components[-1]

    # sum up the file sizes by module name
    if module not in bin_sizes[idx]:
      bin_sizes[idx][module] = int(size)
    else:
      bin_sizes[idx][module] += int(size)

    # sometimes a file only exists on one build but not the other - use 0 as the
    # default size.
    if idx == 0:
      file_sizes[name] = [module, size, 0]
    else:
      if name in file_sizes:
        file_sizes[name][-1] = size
      else:
        file_sizes[name] = [module, 0, size]

  input_stream.close()

  # output the module csv file
  output = open("module_%d.csv" % idx, 'w')
  total = 0
  for key in bin_sizes[idx]:
    output.write("%s, %d\n" % (key, bin_sizes[idx][key]))
  output.close()

def main():
  if len(sys.argv) < 2 or len(sys.argv) > 3:
    PrintUsage()
  # Parse the first installed-files.txt
  ParseFile(sys.argv[1], 0)

  # Parse the second installed-files.txt
  if len(sys.argv) == 3:
    ParseFile(sys.argv[2], 1)
    # comparison.csv has the following columns:
    # filename, module, size1, size2, size2-size1
    # eg: /system/lib/libchromeview.so, lib, 25027208, 33278460, 8251252
    output = open("comparison.csv", 'w')
    for key in file_sizes:
      output.write("%s, %s, %s, %s, %d\n" %
                   (key, file_sizes[key][0], file_sizes[key][1],
                    file_sizes[key][2],
                    int(file_sizes[key][2]) - int(file_sizes[key][1])))
    output.close()

if __name__ == '__main__':
  main()

# vi: ts=2 sw=2
