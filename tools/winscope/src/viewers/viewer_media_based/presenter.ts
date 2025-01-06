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

import {FunctionUtils} from 'common/function_utils';
import {
  ActiveTraceChanged,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {Trace, TraceEntry} from 'trace/trace';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {UiData} from './ui_data';

export type NotifyHierarchyViewCallbackType<UiData> = (uiData: UiData) => void;

export class Presenter {
  private readonly uiData: UiData;
  private emitWinscopeEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor(
    private readonly traces: Array<Trace<MediaBasedTraceEntry>>,
    private readonly notifyViewCallback: NotifyHierarchyViewCallbackType<UiData>,
  ) {
    this.uiData = new UiData(
      this.traces.map((trace) => trace.getDescriptors().join(', ')),
    );
    this.notifyViewCallback(this.uiData);
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitWinscopeEvent = callback;
  }

  addEventListeners(htmlElement: HTMLElement) {
    htmlElement.addEventListener(
      ViewerEvents.OverlayDblClick,
      async (event) => {
        this.onOverlayDblClick((event as CustomEvent).detail);
      },
    );
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        const traceEntries = this.traces
          .map((trace) =>
            TraceEntryFinder.findCorrespondingEntry(trace, event.position),
          )
          .filter((entry) => entry !== undefined) as Array<
          TraceEntry<MediaBasedTraceEntry>
        >;
        const entries: MediaBasedTraceEntry[] = await Promise.all(
          traceEntries.map((entry) => {
            return entry.getValue();
          }),
        );
        this.uiData.currentTraceEntries = entries;
        this.notifyViewCallback(this.uiData);
      },
    );
    await event.visit(
      WinscopeEventType.EXPANDED_TIMELINE_TOGGLED,
      async (event) => {
        this.uiData.forceMinimize = event.isTimelineExpanded;
        this.notifyViewCallback(this.uiData);
      },
    );
  }

  async onOverlayDblClick(index: number) {
    const currTrace = this.traces.at(index);
    if (currTrace) {
      this.emitWinscopeEvent(new ActiveTraceChanged(currTrace));
    }
  }
}
