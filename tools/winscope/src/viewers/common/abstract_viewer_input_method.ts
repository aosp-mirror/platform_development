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

import {Store} from 'common/store';
import {WinscopeEvent} from 'messaging/winscope_event';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {AbstractPresenterInputMethod} from 'viewers/common/abstract_presenter_input_method';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {View, Viewer, ViewType} from 'viewers/viewer';

export abstract class AbstractViewerInputMethod implements Viewer {
  private readonly trace: Trace<HierarchyTreeNode>;
  protected readonly htmlElement: HTMLElement;
  protected readonly presenter: AbstractPresenterInputMethod;
  protected readonly view: View;

  protected imeUiCallback = (uiData: ImeUiData) => {
    (this.htmlElement as any).inputData = uiData;
  };

  constructor(trace: Trace<HierarchyTreeNode>, traces: Traces, storage: Store) {
    this.trace = trace;
    this.htmlElement = document.createElement('viewer-input-method');
    this.presenter = this.initializePresenter(trace, traces, storage);
    this.addViewerEventListeners();
    this.view = new View(
      ViewType.TRACE_TAB,
      this.getTraces(),
      this.htmlElement,
      TRACE_INFO[trace.type].name,
    );
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await this.presenter.onAppEvent(event);
  }

  setEmitEvent() {
    // do nothing
  }

  getViews(): View[] {
    return [this.view];
  }

  getTraces(): Array<Trace<HierarchyTreeNode>> {
    return [this.trace];
  }

  protected addViewerEventListeners() {
    this.presenter.addEventListeners(this.htmlElement);
    this.htmlElement.addEventListener(
      ViewerEvents.AdditionalPropertySelected,
      async (event) =>
        await this.presenter.onAdditionalPropertySelected(
          (event as CustomEvent).detail.selectedItem,
        ),
    );
  }

  protected abstract initializePresenter(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    storage: Store,
  ): AbstractPresenterInputMethod;
}
