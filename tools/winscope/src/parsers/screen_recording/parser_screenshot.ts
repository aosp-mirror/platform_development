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

import {Timestamp} from 'common/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {CoarseVersion} from 'trace/coarse_version';
import {ScreenRecordingTraceEntry} from 'trace/screen_recording';
import {TraceType} from 'trace/trace_type';

class ParserScreenshot extends AbstractParser<ScreenRecordingTraceEntry> {
  private static readonly MAGIC_NUMBER = [
    0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
  ]; // currently only support png files

  override getTraceType(): TraceType {
    return TraceType.SCREENSHOT;
  }

  override getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LATEST;
  }

  override getMagicNumber(): number[] | undefined {
    return ParserScreenshot.MAGIC_NUMBER;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  protected override getTimestamp(decodedEntry: number): Timestamp {
    return this.timestampConverter.makeZeroTimestamp();
  }

  override decodeTrace(screenshotData: Uint8Array): number[] {
    return [0]; // require a non-empty array to be returned so trace can provide timestamps
  }

  override processDecodedEntry(
    index: number,
    entry: number,
  ): ScreenRecordingTraceEntry {
    const screenshotData = this.traceFile.file;
    return new ScreenRecordingTraceEntry(0, screenshotData, true);
  }
}

export {ParserScreenshot};
