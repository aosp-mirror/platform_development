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
import {Parser} from "parsers/parser";
import {ParserFactory} from "parsers/parser_factory";
import {Viewer} from "viewers/viewer";
import {ViewerFactory} from "viewers/viewer_factory";

class Core {
  private parsers: Parser[];
  private viewers: Viewer[];

  constructor() {
    this.parsers = [];
    this.viewers = [];
  }

  async bootstrap(traces: Blob[]) {
    this.parsers = await new ParserFactory().createParsers(traces);
    console.log("created parsers: ", this.parsers);

    const activeTraceTypes = this.parsers.map(parser => parser.getTraceTypeId());
    console.log("active trace types: ", activeTraceTypes);

    this.viewers = new ViewerFactory().createViewers(new Set<TraceTypeId>(activeTraceTypes));
    console.log("created viewers: ", this.viewers);
  }

  getViews(): HTMLElement[] {
    return this.viewers.map(viewer => viewer.getView());
  }

  getTimestamps(): number[] {
    const mergedTimestamps: number[] = [];

    this.parsers
      .map(parser => parser.getTimestamps())
      .forEach(timestamps => {
        mergedTimestamps.push(...timestamps);
      });

    const uniqueTimestamps = [... new Set<number>(mergedTimestamps)];

    return uniqueTimestamps;
  }

  notifyCurrentTimestamp(timestamp: number) {
    const traceEntries: Map<TraceTypeId, any> = new Map<TraceTypeId, any>();

    this.parsers.forEach(parser => {
      const entry = parser.getTraceEntry(timestamp);
      if (entry != undefined) {
        traceEntries.set(parser.getTraceTypeId(), entry);
      }
    });

    this.viewers.forEach(viewer => {
      viewer.notifyCurrentTraceEntries(traceEntries);
    });
  }
}

export { Core };
