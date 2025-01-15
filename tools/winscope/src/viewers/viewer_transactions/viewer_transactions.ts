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
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AbstractViewer} from 'viewers/abstract_viewer';
import {ViewerComponent} from 'viewers/components/viewer_component';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

export class ViewerTransactions extends AbstractViewer<PropertyTreeNode> {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.TRANSACTIONS];

  constructor(trace: Trace<PropertyTreeNode>, traces: Traces, store: Store) {
    super(trace, traces, 'viewer-transactions', store);
  }

  protected override initializePresenter(
    trace: Trace<PropertyTreeNode>,
    traces: Traces,
    store: Store,
  ): Presenter {
    const notifyViewCallback = (data: UiData) => {
      (this.htmlElement as unknown as ViewerComponent<UiData>).inputData = data;
    };
    return new Presenter(trace, store, notifyViewCallback);
  }
}
