/*
 * Copyright 2024 The Android Open Source Project
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
import {Store} from 'common/store';
import {WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';
import {ViewerInputComponent} from './viewer_input_component';

export class ViewerInput implements Viewer {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.INPUT_EVENT_MERGED];

  private readonly traces: Traces;
  private readonly mergedInputEventTrace: Trace<PropertyTreeNode>;
  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly view: View;

  constructor(traces: Traces, storage: Store) {
    this.traces = traces;
    this.mergedInputEventTrace = assertDefined(
      traces.getTrace(TraceType.INPUT_EVENT_MERGED),
    );
    this.htmlElement = document.createElement('viewer-input');

    this.presenter = new Presenter(
      traces,
      this.mergedInputEventTrace,
      storage,
      (uiData: UiData) => {
        (this.htmlElement as unknown as ViewerInputComponent).inputData =
          uiData;
      },
    );

    this.presenter.addEventListeners(this.htmlElement);

    this.view = new View(
      ViewType.TAB,
      this.getTraces(),
      this.htmlElement,
      'Input',
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

  getTraces(): Array<Trace<PropertyTreeNode>> {
    return [this.mergedInputEventTrace];
  }
}
