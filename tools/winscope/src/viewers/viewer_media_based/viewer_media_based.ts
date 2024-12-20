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

import {WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {MediaBasedTraceEntry} from 'trace/media_based_trace_entry';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';
import {ViewerMediaBasedComponent} from './viewer_media_based_component';

export abstract class ViewerMediaBased implements Viewer {
  private readonly traces: Array<Trace<MediaBasedTraceEntry>>;
  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly view: View;

  constructor(
    traces: Traces,
    type: TraceType.SCREENSHOT | TraceType.SCREEN_RECORDING,
  ) {
    this.traces = traces.getTraces(type);
    this.htmlElement = document.createElement('viewer-media-based');

    const notifyViewCallback = (uiData: UiData) => {
      const component = this
        .htmlElement as unknown as ViewerMediaBasedComponent;
      component.titles = uiData.titles;
      component.currentTraceEntries = uiData.currentTraceEntries;
      component.forceMinimize = uiData.forceMinimize;
    };
    this.presenter = new Presenter(this.traces, notifyViewCallback);
    this.presenter.addEventListeners(this.htmlElement);

    this.view = new View(
      ViewType.OVERLAY,
      this.getTraces(),
      this.htmlElement,
      TRACE_INFO[type].name,
    );
  }

  setEmitEvent(callback: EmitEvent) {
    this.presenter.setEmitEvent(callback);
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await this.presenter.onAppEvent(event);
  }

  getViews(): View[] {
    return [this.view];
  }

  getTraces(): Array<Trace<MediaBasedTraceEntry>> {
    return this.traces;
  }
}
