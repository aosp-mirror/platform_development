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
import {Timestamp} from 'common/time/time';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {TIME_UNIT_TO_NANO} from 'common/time/time_units';
import {UserNotifier} from 'common/user_notifier';
import {MonotonicScreenRecording} from 'messaging/user_warnings';
import * as MP4Box from 'mp4box';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {CoarseVersion} from 'trace/coarse_version';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {ScreenRecordingUtils} from 'trace/screen_recording_utils';
import {TraceFile} from 'trace/trace_file';
import {ScreenRecordingOffsets, TraceMetadata} from 'trace/trace_metadata';
import {TraceType} from 'trace/trace_type';

class ParserScreenRecording extends AbstractParser<
  MediaBasedTraceEntry,
  bigint
> {
  private realToBootTimeOffsetNs: bigint | undefined;

  constructor(
    trace: TraceFile,
    timestampConverter: ParserTimestampConverter,
    metadata: TraceMetadata,
  ) {
    super(trace, timestampConverter, metadata);
  }

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

  override async decodeTrace(videoData: Uint8Array): Promise<Array<bigint>> {
    const posVersion = this.searchMagicString(videoData);
    if (posVersion !== undefined) {
      return this.parseTimestampsUsingEmbeddedMetadata(videoData, posVersion);
    } else if (this.metadata?.screenRecordingOffsets !== undefined) {
      return await this.parseTimestampsUsingExternalMetadata(
        videoData,
        this.metadata.screenRecordingOffsets,
      );
    }
    throw new TypeError(
      "video data doesn't contain winscope magic string and metadata json not provided",
    );
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

  private searchMagicString(videoData: Uint8Array): number | undefined {
    let pos = ArrayUtils.searchSubarray(
      videoData,
      ParserScreenRecording.WINSCOPE_META_MAGIC_STRING,
    );
    if (pos === undefined) {
      return undefined;
    }
    pos += ParserScreenRecording.WINSCOPE_META_MAGIC_STRING.length;
    return pos;
  }

  private parseTimestampsUsingEmbeddedMetadata(
    videoData: Uint8Array,
    posVersion: number,
  ): Array<bigint> {
    const [posCount, timeOffsetNs] = this.getOffsetAndCountFromPosVersion(
      videoData,
      posVersion,
    );
    const [posTimestamps, count] = this.parseFramesCount(videoData, posCount);
    this.realToBootTimeOffsetNs = timeOffsetNs;
    const timestampsElapsedNs = this.parseTimestampsElapsedNs(
      videoData,
      posTimestamps,
      count,
    );
    return timestampsElapsedNs;
  }

  private getOffsetAndCountFromPosVersion(
    videoData: Uint8Array,
    posVersion: number,
  ): [number, bigint] {
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

    return this.parseRealToBootTimeOffsetNs(videoData, posTimeOffset);
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

  private parseRealToBootTimeOffsetNs(
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

  private async parseTimestampsUsingExternalMetadata(
    videoData: Uint8Array,
    metadata: ScreenRecordingOffsets,
  ): Promise<Array<bigint>> {
    this.realToBootTimeOffsetNs = metadata.realToElapsedTimeOffsetNanos;
    const timestampsElapsedNs = await this.parseTimestampsFromMp4(
      videoData.buffer.slice(
        videoData.byteOffset,
        videoData.byteLength + videoData.byteOffset,
      ),
      metadata.elapsedRealTimeNanos,
    );
    return timestampsElapsedNs;
  }

  private async parseTimestampsFromMp4(
    arrayBuffer: ArrayBuffer,
    elapsedRealTimeNanos: bigint,
  ): Promise<Array<bigint>> {
    const timestamps: Array<bigint> = [];
    const mp4File: MP4Box.MP4File = MP4Box.createFile();
    await new Promise<void>((resolve) => {
      mp4File.onReady = (info) => {
        mp4File.onSamples = (id, user, samples) => {
          let curr = elapsedRealTimeNanos;
          samples.forEach((sample) => {
            const timeSeconds = sample.duration / sample.timescale;
            const timeNs = BigInt(
              Math.floor(TIME_UNIT_TO_NANO.s * timeSeconds),
            );
            curr += timeNs;
            timestamps.push(curr);
          });
          resolve();
        };
        mp4File.setExtractionOptions(info.tracks[0].id);
      };
      const buffer = arrayBuffer as MP4Box.MP4ArrayBuffer;
      buffer.fileStart = 0;
      mp4File.appendBuffer(buffer);
      mp4File.start();
    });
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
