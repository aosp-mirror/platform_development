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

import {Timestamp} from 'common/time';
import {FrameMap} from 'trace/frame_map';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {TraceBuilder} from './trace_builder';

export class TracesBuilder {
  private readonly traceBuilders = new Map<TraceType, TraceBuilder<{}>>();

  setEntries(type: TraceType, entries: Array<{}>): TracesBuilder {
    const builder = this.getOrCreateTraceBuilder(type);
    builder.setEntries(entries);
    return this;
  }

  setTimestamps(type: TraceType, timestamps: Timestamp[]): TracesBuilder {
    const builder = this.getOrCreateTraceBuilder(type);
    builder.setTimestamps(timestamps);
    return this;
  }

  setFrameMap(type: TraceType, frameMap: FrameMap | undefined): TracesBuilder {
    const builder = this.getOrCreateTraceBuilder(type);
    builder.setFrameMap(frameMap);
    return this;
  }

  build(): Traces {
    const traces = new Traces();
    this.traceBuilders.forEach((builder) => {
      traces.addTrace(builder.build());
    });
    return traces;
  }

  private getOrCreateTraceBuilder(type: TraceType): TraceBuilder<{}> {
    let builder = this.traceBuilders.get(type);
    if (!builder) {
      builder = new TraceBuilder<{}>();
      builder.setType(type);
      this.traceBuilders.set(type, builder);
    }
    return builder;
  }
}
