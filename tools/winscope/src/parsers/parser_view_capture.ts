/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {FrameData, TraceType, WindowData} from 'trace/trace_type';
import {ParserViewCaptureWindow} from './parser_view_capture_window';
import {ParsingUtils} from './parsing_utils';
import {ExportedData} from './proto_types';

export class ParserViewCapture {
  private readonly windowParsers: Array<Parser<FrameData>> = [];

  constructor(private readonly traceFile: TraceFile) {}

  async parse() {
    const traceBuffer = new Uint8Array(await this.traceFile.file.arrayBuffer());
    ParsingUtils.throwIfMagicNumberDoesntMatch(traceBuffer, ParserViewCapture.MAGIC_NUMBER);

    const exportedData = ExportedData.decode(traceBuffer) as any;

    exportedData.windowData.forEach((windowData: WindowData) =>
      this.windowParsers.push(
        new ParserViewCaptureWindow(
          [this.traceFile.getDescriptor()],
          windowData.frameData,
          ParserViewCapture.toTraceType(windowData),
          BigInt(exportedData.realToElapsedTimeOffsetNanos),
          exportedData.package,
          exportedData.classname
        )
      )
    );
  }

  getTraceType(): TraceType {
    return TraceType.VIEW_CAPTURE;
  }

  getWindowParsers(): Array<Parser<FrameData>> {
    return this.windowParsers;
  }

  private static toTraceType(windowData: WindowData): TraceType {
    switch (windowData.title) {
      case '.Taskbar':
        return TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER;
      case '.TaskbarOverlay':
        return TraceType.VIEW_CAPTURE_TASKBAR_OVERLAY_DRAG_LAYER;
      default:
        return TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY;
    }
  }

  private static readonly MAGIC_NUMBER = [0x9, 0x78, 0x65, 0x90, 0x65, 0x73, 0x82, 0x65, 0x68];
}
