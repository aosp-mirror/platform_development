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
import {Store} from 'common/store/store';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractViewer} from 'viewers/abstract_viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';
import {ViewerInputComponent} from './viewer_input_component';

export class ViewerInput extends AbstractViewer<PropertyTreeNode> {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.INPUT_EVENT_MERGED];

  constructor(traces: Traces, store: Store) {
    const trace = assertDefined(traces.getTrace(TraceType.INPUT_EVENT_MERGED));
    super(trace, traces, 'viewer-input', store);
  }

  protected override initializePresenter(
    trace: Trace<PropertyTreeNode>,
    traces: Traces,
    store: Store,
  ): Presenter {
    const notifyViewCallback = (uiData: UiData) => {
      (this.htmlElement as unknown as ViewerInputComponent).inputData = uiData;
    };
    return new Presenter(traces, trace, store, notifyViewCallback);
  }
}
