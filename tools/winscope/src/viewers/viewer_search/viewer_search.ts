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

import {Store} from 'common/store';
import {WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';
import {ViewerSearchComponent} from './viewer_search_component';

export class ViewerSearch implements Viewer {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.SEARCH];

  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly traces: Traces;
  private readonly view: View;

  constructor(traces: Traces, storage: Store) {
    this.htmlElement = document.createElement('viewer-search');
    const notifyViewCallback = (data: UiData) => {
      (this.htmlElement as unknown as ViewerSearchComponent).inputData = data;
    };
    this.presenter = new Presenter(traces, storage, notifyViewCallback);
    this.presenter.addEventListeners(this.htmlElement);
    this.traces = traces;
    this.view = new View(
      ViewType.GLOBAL_SEARCH,
      this.getTraces(),
      this.htmlElement,
      TRACE_INFO[TraceType.SEARCH].name,
    );
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await this.presenter.onAppEvent(event);
  }

  setEmitEvent(callback: EmitEvent) {
    this.presenter.setEmitEvent(callback);
  }

  getViews(): View[] {
    return [this.view];
  }

  getTraces(): Array<Trace<QueryResult>> {
    return this.traces.getTraces(TraceType.SEARCH);
  }
}
