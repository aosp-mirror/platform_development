/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {Store} from 'common/store/store';
import {TimestampConverter} from 'common/time/timestamp_converter';
import {WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {ViewerComponent} from './components/viewer_component';
import {View, Viewer, ViewType} from './viewer';

interface Presenter {
  onAppEvent(event: WinscopeEvent): Promise<void>;
  setEmitEvent(callback: EmitEvent): void;
  addEventListeners(htmlElement: HTMLElement): void;
}

export abstract class AbstractViewer<T extends object> implements Viewer {
  protected readonly trace: Trace<T> | undefined;
  protected readonly htmlElement: HTMLElement;
  protected readonly presenter: Presenter;
  private readonly view: View;

  constructor(
    trace: Trace<T> | undefined,
    traces: Traces,
    componentSelector: string,
    store: Store,
    timestampConverter?: TimestampConverter,
  ) {
    this.trace = trace;
    this.htmlElement = document.createElement(componentSelector);
    (this.htmlElement as unknown as ViewerComponent<T>).store = store;
    this.presenter = this.initializePresenter(
      trace,
      traces,
      store,
      timestampConverter,
    );
    this.presenter.addEventListeners(this.htmlElement);
    this.view = new View(
      this.getViewType(),
      this.getTraces(),
      this.htmlElement,
      TRACE_INFO[this.getTraceTypeForViewTitle()].name,
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

  getTraces(): Array<Trace<object>> {
    return [assertDefined(this.trace)];
  }

  protected getTraceTypeForViewTitle(): TraceType {
    return assertDefined(this.trace).type;
  }

  protected getViewType(): ViewType {
    return ViewType.TRACE_TAB;
  }

  protected abstract initializePresenter(
    trace: Trace<T> | undefined,
    traces: Traces,
    store: Store,
    timestampConverter?: TimestampConverter,
  ): Presenter;
}
