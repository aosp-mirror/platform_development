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
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {AbstractViewer} from 'viewers/abstract_viewer';
import {AbstractPresenterInputMethod} from 'viewers/common/abstract_presenter_input_method';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {ViewerComponent} from 'viewers/components/viewer_component';
import {NotifyHierarchyViewCallbackType} from './abstract_hierarchy_viewer_presenter';

export abstract class AbstractViewerInputMethod extends AbstractViewer<HierarchyTreeNode> {
  constructor(trace: Trace<HierarchyTreeNode>, traces: Traces, store: Store) {
    super(trace, traces, 'viewer-input-method', store);
  }

  protected override initializePresenter(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    store: Store,
  ): AbstractPresenterInputMethod {
    const imeUiCallback = (uiData: ImeUiData) => {
      (this.htmlElement as unknown as ViewerComponent<ImeUiData>).inputData =
        uiData;
    };
    return this.createPresenter(trace, traces, store, imeUiCallback);
  }

  protected abstract createPresenter(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    store: Store,
    imeUiCallback: NotifyHierarchyViewCallbackType<ImeUiData>,
  ): AbstractPresenterInputMethod;
}
