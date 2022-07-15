/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {TraceTypeId} from "common/trace/type_id";
import {ArrayUtils} from "common/utils/array_utils";
import {Parser} from "./parser";
import {ScreenRecordingTraceEntry} from "common/trace/screen_recording";

class ParserScreenRecording extends Parser {
  constructor(trace: Blob) {
    super(trace);
  }

  override getTraceTypeId(): TraceTypeId {
    return TraceTypeId.SCREEN_RECORDING;
  }

  override getMagicNumber(): number[] {
    return ParserScreenRecording.MPEG4_MAGIC_NMBER;
  }

  override decodeTrace(videoData: Uint8Array): number[] {
    const posCount = this.searchMagicString(videoData);
    const [posTimestamps, count] = this.parseTimestampsCount(videoData, posCount);
    return this.parseTimestamps(videoData, posTimestamps, count);
  }

  override getTimestamp(decodedEntry: number): number {
    return decodedEntry;
  }

  override processDecodedEntry(timestamp: number): ScreenRecordingTraceEntry {
    const videoTimeSeconds = (timestamp - this.timestamps[0]) / 1000000000 + ParserScreenRecording.EPSILON;
    const videoData = this.trace;
    return new ScreenRecordingTraceEntry(timestamp, videoTimeSeconds, videoData);
  }

  private searchMagicString(videoData: Uint8Array): number {
    let pos = ArrayUtils.searchSubarray(videoData, ParserScreenRecording.WINSCOPE_META_MAGIC_STRING);
    if (pos === undefined) {
      throw new TypeError("video data doesn't contain winscope magic string");
    }
    pos += ParserScreenRecording.WINSCOPE_META_MAGIC_STRING.length;
    return pos;
  }

  private parseTimestampsCount(videoData: Uint8Array, pos: number) : [number, number] {
    if (pos + 4 >= videoData.length) {
      throw new TypeError("video data is too short. Expected timestamps count doesn't fit");
    }
    const timestampsCount = ArrayUtils.toUintLittleEndian(videoData, pos, pos+4);
    pos += 4;
    return [pos, timestampsCount];
  }

  private parseTimestamps(videoData: Uint8Array, pos: number, count: number): number[] {
    if (pos + count * 8 >= videoData.length) {
      throw new TypeError("video data is too short. Expected timestamps do not fit");
    }
    const timestamps: number[] = [];
    for (let i = 0; i < count; ++i) {
      const timestamp = ArrayUtils.toUintLittleEndian(videoData, pos, pos+8) * 1000;
      pos += 8;
      timestamps.push(timestamp);
    }
    return timestamps;
  }

  private static readonly MPEG4_MAGIC_NMBER = [0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x6d, 0x70, 0x34, 0x32]; // ....ftypmp42
  private static readonly WINSCOPE_META_MAGIC_STRING = [0x23, 0x56, 0x56, 0x31, 0x4e, 0x53, 0x43, 0x30, 0x50, 0x45, 0x54, 0x31, 0x4d, 0x45, 0x21, 0x23]; // #VV1NSC0PET1ME!#
  private static readonly EPSILON = 0.00001;
}

export {ParserScreenRecording};
