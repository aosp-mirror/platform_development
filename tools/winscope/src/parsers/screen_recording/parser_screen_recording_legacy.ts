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

import {ArrayUtils} from 'common/array_utils';
import {assertDefined} from 'common/assert_utils';
import {Timestamp, TimestampType} from 'common/time';
import {AbstractParser} from 'parsers/abstract_parser';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
import {TraceType} from 'trace/trace_type';

class ParserScreenRecordingLegacy extends AbstractParser {
  override getTraceType(): TraceType {
    return TraceType.SCREEN_RECORDING;
  }

  override getMagicNumber(): number[] {
    return ParserScreenRecordingLegacy.MPEG4_MAGIC_NMBER;
  }

  override decodeTrace(videoData: Uint8Array): Timestamp[] {
    const posCount = this.searchMagicString(videoData);
    const [posTimestamps, count] = this.parseFramesCount(videoData, posCount);
    return this.parseTimestamps(videoData, posTimestamps, count);
  }

  override getTimestamp(
    type: TimestampType,
    decodedEntry: Timestamp,
  ): undefined | Timestamp {
    if (type !== TimestampType.ELAPSED) {
      return undefined;
    }
    return decodedEntry;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: Timestamp,
  ): ScreenRecordingTraceEntry {
    const currentTimestamp = entry;
    const initialTimestamp = assertDefined(
      this.getTimestamps(TimestampType.ELAPSED),
    )[0];
    const videoTimeSeconds =
      Number(currentTimestamp.getValueNs() - initialTimestamp.getValueNs()) /
        1000000000 +
      ParserScreenRecordingLegacy.EPSILON;
    const videoData = this.traceFile.file;
    return new ScreenRecordingTraceEntry(videoTimeSeconds, videoData);
  }

  private searchMagicString(videoData: Uint8Array): number {
    let pos = ArrayUtils.searchSubarray(
      videoData,
      ParserScreenRecordingLegacy.WINSCOPE_META_MAGIC_STRING,
    );
    if (pos === undefined) {
      throw new TypeError("video data doesn't contain winscope magic string");
    }
    pos += ParserScreenRecordingLegacy.WINSCOPE_META_MAGIC_STRING.length;
    return pos;
  }

  private parseFramesCount(
    videoData: Uint8Array,
    pos: number,
  ): [number, number] {
    if (pos + 4 > videoData.length) {
      throw new TypeError(
        'Failed to parse frames count. Video data is too short.',
      );
    }
    const framesCount = Number(
      ArrayUtils.toUintLittleEndian(videoData, pos, pos + 4),
    );
    pos += 4;
    return [pos, framesCount];
  }

  private parseTimestamps(
    videoData: Uint8Array,
    pos: number,
    count: number,
  ): Timestamp[] {
    if (pos + count * 8 > videoData.length) {
      throw new TypeError(
        'Failed to parse timestamps. Video data is too short.',
      );
    }
    const timestamps: Timestamp[] = [];
    for (let i = 0; i < count; ++i) {
      const value =
        ArrayUtils.toUintLittleEndian(videoData, pos, pos + 8) * 1000n;
      pos += 8;
      timestamps.push(this.timestampFactory.makeElapsedTimestamp(value));
    }
    return timestamps;
  }

  private static readonly MPEG4_MAGIC_NMBER = [
    0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x6d, 0x70, 0x34, 0x32,
  ]; // ....ftypmp42
  private static readonly WINSCOPE_META_MAGIC_STRING = [
    0x23, 0x56, 0x56, 0x31, 0x4e, 0x53, 0x43, 0x30, 0x50, 0x45, 0x54, 0x31,
    0x4d, 0x45, 0x21, 0x23,
  ]; // #VV1NSC0PET1ME!#
  private static readonly EPSILON = 0.00001;
}

export {ParserScreenRecordingLegacy};
