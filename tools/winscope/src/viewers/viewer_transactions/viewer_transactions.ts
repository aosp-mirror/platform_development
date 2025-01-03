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

import {Store} from 'common/store/store';
import {WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ViewerComponent} from 'viewers/components/viewer_component';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class ViewerTransactions implements Viewer {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.TRANSACTIONS];

  private readonly trace: Trace<PropertyTreeNode>;
  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly view: View;

  constructor(trace: Trace<PropertyTreeNode>, traces: Traces, storage: Store) {
    this.trace = trace;
    this.htmlElement = document.createElement('viewer-transactions');
    const notifyViewCallback = (data: UiData) => {
      (this.htmlElement as unknown as ViewerComponent<UiData>).inputData = data;
    };
    this.presenter = new Presenter(trace, storage, notifyViewCallback);
    this.presenter.addEventListeners(this.htmlElement);

    this.view = new View(
      ViewType.TRACE_TAB,
      this.getTraces(),
      this.htmlElement,
      TRACE_INFO[TraceType.TRANSACTIONS].name,
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

  getTraces(): Array<Trace<PropertyTreeNode>> {
    return [this.trace];
  }
}

export {ViewerTransactions};
