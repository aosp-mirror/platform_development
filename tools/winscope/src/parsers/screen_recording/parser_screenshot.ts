/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {Timestamp, TimestampType} from 'common/time';
import {AbstractParser} from 'parsers/abstract_parser';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';

class ParserScreenshot extends AbstractParser<ScreenRecordingTraceEntry> {
  private static readonly MAGIC_NUMBER = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]; // currently only support png files

  constructor(trace: TraceFile) {
    super(trace);
  }

  override getTraceType(): TraceType {
    return TraceType.SCREENSHOT;
  }

  override getMagicNumber(): number[] | undefined {
    return ParserScreenshot.MAGIC_NUMBER;
  }

  override getTimestamp(type: TimestampType, decodedEntry: number): Timestamp | undefined {
    if (type === TimestampType.ELAPSED) {
      return new Timestamp(TimestampType.ELAPSED, 0n);
    } else if (type === TimestampType.REAL) {
      return new Timestamp(TimestampType.REAL, 0n);
    }
    return undefined;
  }

  override decodeTrace(screenshotData: Uint8Array): number[] {
    return [0]; // require a non-empty array to be returned so trace can provide timestamps
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entry: number
  ): ScreenRecordingTraceEntry {
    const screenshotData = this.traceFile.file;
    return new ScreenRecordingTraceEntry(0, screenshotData, true);
  }
}

export {ParserScreenshot};
