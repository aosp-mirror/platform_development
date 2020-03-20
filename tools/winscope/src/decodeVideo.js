/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const WINSCOPE_META_MAGIC_STRING = [0x23, 0x56, 0x56, 0x31, 0x4e, 0x53, 0x43, 0x30, 0x50, 0x45, 0x54, 0x31, 0x4d, 0x45, 0x21, 0x23]; // #VV1NSC0PET1ME!#

// Suitable only for short patterns
function findFirstInArray(array, pattern) {
  for (var i = 0; i < array.length; i++) {
    var match = true;
    for (var j = 0; j < pattern.length; j++) {
      if (array[i + j] != pattern[j]) {
        match = false;
        break;
      }
    }
    if (match) {
      return i;
    }
  }
  return -1;
}

function parseUintNLE(buffer, position, bytes) {
  var num = 0;
  for (var i = bytes - 1; i >= 0; i--) {
    num = num * 256
    num += buffer[position + i];
  }
  return num;
}

function parseUint32LE(buffer, position) {
  return parseUintNLE(buffer, position, 4)
}

function parseUint64LE(buffer, position) {
  return parseUintNLE(buffer, position, 8)
}

function mp4Decoder(buffer) {
  var dataStart = findFirstInArray(buffer, WINSCOPE_META_MAGIC_STRING);
  if (dataStart < 0) {
    throw new Error('Unable to find sync metadata in the file. Are you using the latest Android ScreenRecorder version?');
  }
  dataStart += WINSCOPE_META_MAGIC_STRING.length;
  var frameNum = parseUint32LE(buffer, dataStart);
  dataStart += 4;
  var timeline = [];
  for (var i = 0; i < frameNum; i++) {
    timeline.push(parseUint64LE(buffer, dataStart) * 1000);
    dataStart += 8;
  }
  return [buffer, timeline]
}

export { mp4Decoder };
