#!/usr/bin/env python3
#
# Copyright 2020 - The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""This script is for test infrastructure to mix images in a super image."""

import argparse
import os
import shutil
import stat
import subprocess
import tempfile
import zipfile


# The file extension of the unpacked images.
IMG_FILE_EXT = ".img"

# The directory containing executable files in OTA tools zip.
BIN_DIR_NAME = "bin"


def existing_abs_path(path):
  """Validates that a path exists and returns the absolute path."""
  abs_path = os.path.abspath(path)
  if not os.path.exists(abs_path):
    raise ValueError(path + " does not exist.")
  return abs_path


def partition_image(part_img):
  """Splits a string into a pair of strings by "="."""
  part, sep, img = part_img.partition("=")
  if not part or not sep:
    raise ValueError(part_img + " is not in the format of "
                     "PARITITON_NAME=IMAGE_PATH.")
  return part, (existing_abs_path(img) if img else "")


def unzip_ota_tools(ota_tools_zip, output_dir):
  """Unzips OTA tools and sets the files in bin/ to be executable."""
  ota_tools_zip.extractall(output_dir)
  permissions = (stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH |
                 stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)
  for root_dir, dir_names, file_names in os.walk(
      os.path.join(output_dir, BIN_DIR_NAME)):
    for file_name in file_names:
      file_path = os.path.join(root_dir, file_name)
      file_stat = os.stat(file_path)
      os.chmod(file_path, file_stat.st_mode | permissions)


def is_sparse_image(image_path):
  """Checks whether a file is a sparse image."""
  with open(image_path, "rb") as image_file:
    return image_file.read(4) == b"\x3a\xff\x26\xed"


def rewrite_misc_info(args_part_imgs, unpacked_part_imgs, lpmake_path,
                      input_file, output_file):
  """Changes the lpmake path and image paths in a misc info file.

  Args:
    args_part_imgs: A dict of {partition_name: image_path} that the user
                    intends to substitute. The partition_names must not have
                    slot suffixes.
    unpacked_part_imgs: A dict of {partition_name: image_path} unpacked from
                        the input super image. The partition_names must have
                        slot suffixes if the misc info enables virtual_ab.
    lpmake_path: The path to the lpmake binary.
    input_file: The input misc info file object.
    output_file: The output misc info file object.

  Returns:
    The list of the partition names without slot suffixes.
  """
  virtual_ab = False
  partition_names = ()
  for line in input_file:
    split_line = line.strip().split("=", 1)
    if len(split_line) < 2:
      split_line = (split_line[0], "")
    if split_line[0] == "dynamic_partition_list":
      partition_names = split_line[1].split()
    elif split_line[0] == "lpmake":
      output_file.write("lpmake=%s\n" % lpmake_path)
      continue
    elif split_line[0].endswith("_image"):
      continue
    elif split_line[0] == "virtual_ab" and split_line[1] == "true":
      virtual_ab = True
    output_file.write(line)

  for partition_name in partition_names:
    img_path = args_part_imgs.get(partition_name)
    if img_path is None:
      # _a is the active slot for the super images built from source.
      img_path = unpacked_part_imgs.get(partition_name + "_a" if virtual_ab
                                        else partition_name)
    if img_path is None:
      raise KeyError("No image for " + partition_name + " partition.")
    if img_path != "":
      output_file.write("%s_image=%s\n" % (partition_name, img_path))

  return partition_names


def main():
  parser = argparse.ArgumentParser(
    description="This script is for test infrastructure to mix images in a "
                "super image.")

  parser.add_argument("--temp-dir",
                      default=tempfile.gettempdir(),
                      type=existing_abs_path,
                      help="The directory where this script creates "
                           "temporary files.")
  parser.add_argument("--ota-tools",
                      required=True,
                      type=existing_abs_path,
                      help="The path to the zip or directory containing OTA "
                           "tools.")
  parser.add_argument("--misc-info",
                      required=True,
                      type=existing_abs_path,
                      help="The path to the misc info file.")
  parser.add_argument("super_img",
                      metavar="SUPER_IMG",
                      type=existing_abs_path,
                      help="The path to the super image to be repacked.")
  parser.add_argument("part_imgs",
                      metavar="PART_IMG",
                      nargs="*",
                      type=partition_image,
                      help="The partition and the image that will be added "
                           "to the super image. The format is "
                           "PARITITON_NAME=IMAGE_PATH. PARTITION_NAME must "
                           "not have slot suffix. If IMAGE_PATH is empty, the "
                           "partition will be resized to 0.")
  args = parser.parse_args()

  # Convert the args.part_imgs to a dictionary.
  args_part_imgs = dict()
  for part, img in args.part_imgs:
    if part in args_part_imgs:
      raise ValueError(part + " partition is repeated.")
    args_part_imgs[part] = img

  if args.temp_dir:
    tempfile.tempdir = args.temp_dir

  temp_dirs = []
  temp_files = []
  try:
    if os.path.isdir(args.ota_tools):
      ota_tools_dir = args.ota_tools
    else:
      print("Unzip OTA tools.")
      temp_ota_tools_dir = tempfile.mkdtemp(prefix="ota_tools")
      temp_dirs.append(temp_ota_tools_dir)
      with zipfile.ZipFile(args.ota_tools) as ota_tools_zip:
        unzip_ota_tools(ota_tools_zip, temp_ota_tools_dir)
      ota_tools_dir = temp_ota_tools_dir

    if not is_sparse_image(args.super_img):
      super_img_path = args.super_img
    else:
      print("Convert to unsparsed super image.")
      simg2img_path = os.path.join(ota_tools_dir, BIN_DIR_NAME, "simg2img")
      with tempfile.NamedTemporaryFile(
          mode="wb", prefix="super", suffix=".img",
          delete=False) as raw_super_img:
        temp_files.append(raw_super_img.name)
        super_img_path = raw_super_img.name
        subprocess.check_call([simg2img_path, args.super_img, super_img_path])

    print("Unpack super image.")
    unpacked_part_imgs = dict()
    lpunpack_path = os.path.join(ota_tools_dir, BIN_DIR_NAME, "lpunpack")
    unpack_dir = tempfile.mkdtemp(prefix="lpunpack")
    temp_dirs.append(unpack_dir)
    subprocess.check_call([lpunpack_path, super_img_path, unpack_dir])
    for file_name in os.listdir(unpack_dir):
      if file_name.endswith(IMG_FILE_EXT):
        part = file_name[:-len(IMG_FILE_EXT)]
        unpacked_part_imgs[part] = os.path.join(unpack_dir, file_name)

    print("Create temporary misc info.")
    lpmake_path = os.path.join(ota_tools_dir, BIN_DIR_NAME, "lpmake")
    with tempfile.NamedTemporaryFile(
        mode="w", prefix="misc_info", suffix=".txt",
        delete=False) as misc_info_file:
      temp_files.append(misc_info_file.name)
      misc_info_file_path = misc_info_file.name
      with open(args.misc_info, "r") as misc_info:
        part_list = rewrite_misc_info(args_part_imgs, unpacked_part_imgs,
                                      lpmake_path, misc_info, misc_info_file)

    # Check that all input partitions are in the partition list.
    parts_not_found = args_part_imgs.keys() - set(part_list)
    if parts_not_found:
      raise ValueError("Cannot find partitions in misc info: " +
                       " ".join(parts_not_found))

    print("Build super image.")
    build_super_image_path = os.path.join(ota_tools_dir, BIN_DIR_NAME,
                                          "build_super_image")
    subprocess.check_call([build_super_image_path, misc_info_file_path,
                           args.super_img])
  finally:
    for temp_dir in temp_dirs:
      shutil.rmtree(temp_dir, ignore_errors=True)
    for temp_file in temp_files:
      os.remove(temp_file)


if __name__ == "__main__":
  main()
