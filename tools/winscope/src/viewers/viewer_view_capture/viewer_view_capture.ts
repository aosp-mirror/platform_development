/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {ActiveTraceChanged, WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

export class ViewerViewCapture implements Viewer {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.VIEW_CAPTURE];

  private readonly traces: Traces;
  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly view: View;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor(traces: Traces, storage: Storage) {
    this.traces = traces;
    this.htmlElement = document.createElement('viewer-view-capture');
    this.presenter = new Presenter(traces, storage, (data: UiData) => {
      (this.htmlElement as any).inputData = data;
    });

    this.htmlElement.addEventListener(
      ViewerEvents.HierarchyPinnedChange,
      (event) =>
        this.presenter.onPinnedItemChange(
          (event as CustomEvent).detail.pinnedItem,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.HighlightedIdChange,
      async (event) =>
        await this.presenter.onHighlightedIdChange(
          (event as CustomEvent).detail.id,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.HierarchyUserOptionsChange,
      async (event) =>
        await this.presenter.onHierarchyUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.HierarchyFilterChange,
      async (event) =>
        await this.presenter.onHierarchyFilterChange(
          (event as CustomEvent).detail.filterString,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.PropertiesUserOptionsChange,
      async (event) =>
        await this.presenter.onPropertiesUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.PropertiesFilterChange,
      async (event) =>
        await this.presenter.onPropertiesFilterChange(
          (event as CustomEvent).detail.filterString,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.HighlightedNodeChange,
      async (event) =>
        await this.presenter.onHighlightedNodeChange(
          (event as CustomEvent).detail.node,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.MiniRectsDblClick,
      async (event) => {
        await this.presenter.onMiniRectsDoubleClick();
      },
    );
    this.htmlElement.addEventListener(
      ViewerEvents.RectGroupIdChange,
      async (event) => {
        const traceId = (event as CustomEvent).detail.groupId;
        const trace = this.presenter.getViewCaptureTraceFromId(traceId);
        await this.emitAppEvent(new ActiveTraceChanged(trace));
      },
    );
    this.htmlElement.addEventListener(
      ViewerEvents.RectShowStateChange,
      async (event) => {
        await this.presenter.onRectShowStateChange(
          (event as CustomEvent).detail.rectId,
          (event as CustomEvent).detail.state,
        );
      },
    );
    this.htmlElement.addEventListener(
      ViewerEvents.RectsUserOptionsChange,
      (event) => {
        this.presenter.onRectsUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        );
      },
    );

    this.view = new View(
      ViewType.TAB,
      this.getTraces(),
      this.htmlElement,
      TRACE_INFO[TraceType.VIEW_CAPTURE].name,
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

  getTraces(): Array<Trace<HierarchyTreeNode>> {
    return this.presenter.getTraces();
  }
}
