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
import {Timestamp} from 'common/time';
import {UserNotifier} from 'common/user_notifier';
import {MonotonicScreenRecording} from 'messaging/user_warnings';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {CoarseVersion} from 'trace/coarse_version';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {ScreenRecordingUtils} from 'trace/screen_recording_utils';
import {TraceType} from 'trace/trace_type';

class ParserScreenRecording extends AbstractParser {
  private realToBootTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.SCREEN_RECORDING;
  }

  override getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LATEST;
  }

  override getMagicNumber(): number[] {
    return ParserScreenRecording.MPEG4_MAGIC_NUMBER;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override decodeTrace(videoData: Uint8Array): Array<bigint> {
    const posVersion = this.searchMagicString(videoData);
    const [posTimeOffset, metadataVersion] = this.parseMetadataVersion(
      videoData,
      posVersion,
    );

    if (metadataVersion !== 1 && metadataVersion !== 2) {
      throw new TypeError(
        `Metadata version "${metadataVersion}" not supported`,
      );
    }

    if (metadataVersion === 1) {
      // UI traces contain "elapsed" timestamps (SYSTEM_TIME_BOOTTIME), whereas
      // metadata Version 1 contains SYSTEM_TIME_MONOTONIC timestamps.
      //
      // Here we are pretending that metadata Version 1 contains "elapsed"
      // timestamps as well, in order to synchronize with the other traces.
      //
      // If no device suspensions are involved, SYSTEM_TIME_MONOTONIC should
      // indeed correspond to SYSTEM_TIME_BOOTTIME and things will work as
      // expected.
      UserNotifier.add(new MonotonicScreenRecording());
    }

    const [posCount, timeOffsetNs] = this.parserealToBootTimeOffsetNs(
      videoData,
      posTimeOffset,
    );
    this.realToBootTimeOffsetNs = timeOffsetNs;
    const [posTimestamps, count] = this.parseFramesCount(videoData, posCount);
    const timestampsElapsedNs = this.parseTimestampsElapsedNs(
      videoData,
      posTimestamps,
      count,
    );

    return timestampsElapsedNs;
  }

  protected override getTimestamp(decodedEntry: bigint): Timestamp {
    return this.timestampConverter.makeTimestampFromBootTimeNs(decodedEntry);
  }

  override processDecodedEntry(
    index: number,
    entry: bigint,
  ): MediaBasedTraceEntry {
    const videoTimeSeconds = ScreenRecordingUtils.timestampToVideoTimeSeconds(
      this.decodedEntries[0],
      entry,
    );
    const videoData = this.traceFile.file;
    return new MediaBasedTraceEntry(videoTimeSeconds, videoData);
  }

  private searchMagicString(videoData: Uint8Array): number {
    let pos = ArrayUtils.searchSubarray(
      videoData,
      ParserScreenRecording.WINSCOPE_META_MAGIC_STRING,
    );
    if (pos === undefined) {
      throw new TypeError("video data doesn't contain winscope magic string");
    }
    pos += ParserScreenRecording.WINSCOPE_META_MAGIC_STRING.length;
    return pos;
  }

  private parseMetadataVersion(
    videoData: Uint8Array,
    pos: number,
  ): [number, number] {
    if (pos + 4 > videoData.length) {
      throw new TypeError(
        'Failed to parse metadata version. Video data is too short.',
      );
    }
    const version = Number(
      ArrayUtils.toUintLittleEndian(videoData, pos, pos + 4),
    );
    pos += 4;
    return [pos, version];
  }

  private parserealToBootTimeOffsetNs(
    videoData: Uint8Array,
    pos: number,
  ): [number, bigint] {
    if (pos + 8 > videoData.length) {
      throw new TypeError(
        'Failed to parse realtime-to-elapsed time offset. Video data is too short.',
      );
    }
    const offset = ArrayUtils.toIntLittleEndian(videoData, pos, pos + 8);
    pos += 8;
    return [pos, offset];
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
    const count = Number(
      ArrayUtils.toUintLittleEndian(videoData, pos, pos + 4),
    );
    pos += 4;
    return [pos, count];
  }

  private parseTimestampsElapsedNs(
    videoData: Uint8Array,
    pos: number,
    count: number,
  ): Array<bigint> {
    if (pos + count * 8 > videoData.length) {
      throw new TypeError(
        'Failed to parse timestamps. Video data is too short.',
      );
    }
    const timestamps: Array<bigint> = [];
    for (let i = 0; i < count; ++i) {
      const timestamp = ArrayUtils.toUintLittleEndian(videoData, pos, pos + 8);
      pos += 8;
      timestamps.push(timestamp);
    }
    return timestamps;
  }

  private static readonly MPEG4_MAGIC_NUMBER = [
    0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x6d, 0x70, 0x34, 0x32,
  ]; // ....ftypmp42
  private static readonly WINSCOPE_META_MAGIC_STRING = [
    0x23, 0x56, 0x56, 0x31, 0x4e, 0x53, 0x43, 0x30, 0x50, 0x45, 0x54, 0x31,
    0x4d, 0x45, 0x32, 0x23,
  ]; // #VV1NSC0PET1ME2#
}

export {ParserScreenRecording};
