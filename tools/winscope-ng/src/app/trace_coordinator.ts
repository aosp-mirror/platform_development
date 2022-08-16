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
import {Timestamp, TimestampType} from "common/trace/timestamp";
import {TraceType} from "common/trace/trace_type";
import {Parser} from "parsers/parser";
import {ParserFactory} from "parsers/parser_factory";
import { setTraces } from "trace_collection/set_traces";
import { Viewer } from "viewers/viewer";
import { ViewerFactory } from "viewers/viewer_factory";
import { LoadedTrace } from "app/loaded_trace";
import { TRACE_INFO } from "./trace_info";

class TraceCoordinator {
  private parsers: Parser[];
  private viewers: Viewer[];

  constructor() {
    this.parsers = [];
    this.viewers = [];
  }

  async addTraces(traces: Blob[]) {
    traces = this.parsers.map(parser => parser.getTrace()).concat(traces);
    this.parsers = await new ParserFactory().createParsers(traces);
    console.log("created parsers: ", this.parsers);
  }

  removeTrace(type: TraceType) {
    this.parsers = this.parsers.filter(parser => parser.getTraceType() !== type);
  }

  createViewers() {
    const activeTraceTypes = this.parsers.map(parser => parser.getTraceType());
    console.log("active trace types: ", activeTraceTypes);

    this.viewers = new ViewerFactory().createViewers(new Set<TraceType>(activeTraceTypes));
    console.log("created viewers: ", this.viewers);
  }

  getLoadedTraces(): LoadedTrace[] {
    return this.parsers.map((parser: Parser) => {
      const name = (<File>parser.getTrace()).name;
      const type = parser.getTraceType();
      return {name: name, type: type};
    });
  }

  getViews(): HTMLElement[] {
    return this.viewers.map(viewer => viewer.getView());
  }

  getViewers(): Viewer[] {
    return this.viewers;
  }

  loadedTraceTypes(): TraceType[] {
    return this.parsers.map(parser => parser.getTraceType());
  }

  findParser(fileType: TraceType): Parser | null {
    const parser = this.parsers.find(parser => parser.getTraceType() === fileType);
    return parser ?? null;
  }

  getTimestamps(): Timestamp[] {
    for (const type of [TimestampType.REAL, TimestampType.ELAPSED]) {
      const mergedTimestamps: Timestamp[] = [];

      let isTypeProvidedByAllParsers = true;

      for(const timestamps of this.parsers.map(parser => parser.getTimestamps(type))) {
        if (timestamps === undefined) {
          isTypeProvidedByAllParsers = false;
          break;
        }
        mergedTimestamps.push(...timestamps!);
      }

      if (isTypeProvidedByAllParsers) {
        const uniqueTimestamps = [... new Set<Timestamp>(mergedTimestamps)];
        uniqueTimestamps.sort();
        return uniqueTimestamps;
      }
    }

    throw new Error("Failed to create aggregated timestamps (any type)");
  }

  notifyCurrentTimestamp(timestamp: Timestamp) {
    const traceEntries: Map<TraceType, any> = new Map<TraceType, any>();

    this.parsers.forEach(parser => {
      const targetTimestamp = timestamp;
      const entry = parser.getTraceEntry(targetTimestamp);
      if (entry !== undefined) {
        traceEntries.set(parser.getTraceType(), entry);
      }
    });

    this.viewers.forEach(viewer => {
      viewer.notifyCurrentTraceEntries(traceEntries);
    });
  }

  clearData() {
    this.getViews().forEach(view => view.remove());
    this.parsers = [];
    this.viewers = [];
    setTraces.dataReady = false;
  }

  saveTraces(traceTypes: TraceType[]) {
    const blobs: Blob[] = [];
    traceTypes.forEach(type => {
      const trace = this.findParser(type)?.getTrace();
      if (trace) {
        blobs.push(trace);
      }
    });
    blobs.forEach((blob, idx) => {
      const a = document.createElement("a");
      document.body.appendChild(a);
      const url = window.URL.createObjectURL(blob);
      a.href = url;
      a.download = (blob as any).name ?? `${TRACE_INFO[traceTypes[idx]].name}.pb`;
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    });
  }
}

export { TraceCoordinator };