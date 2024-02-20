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

import {FunctionUtils} from 'common/function_utils';
import {TabbedViewSwitchRequest, WinscopeEvent} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {ViewCaptureUtils} from 'viewers/common/view_capture_utils';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class ViewerSurfaceFlinger implements Viewer {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.SURFACE_FLINGER];

  private readonly htmlElement: HTMLElement;
  private readonly presenter: Presenter;
  private readonly view: View;
  private emitAppEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;

  constructor(traces: Traces, storage: Storage) {
    this.htmlElement = document.createElement('viewer-surface-flinger');

    this.presenter = new Presenter(traces, storage, (uiData: UiData) => {
      (this.htmlElement as any).inputData = uiData;
    });

    this.htmlElement.addEventListener(
      ViewerEvents.HierarchyPinnedChange,
      (event) =>
        this.presenter.onPinnedItemChange(
          (event as CustomEvent).detail.pinnedItem,
        ),
    );
    this.htmlElement.addEventListener(ViewerEvents.HighlightedChange, (event) =>
      this.presenter.onHighlightedItemChange(
        `${(event as CustomEvent).detail.id}`,
      ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.HighlightedPropertyChange,
      (event) =>
        this.presenter.onHighlightedPropertyChange(
          `${(event as CustomEvent).detail.id}`,
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
      ViewerEvents.SelectedTreeChange,
      async (event) =>
        await this.presenter.onSelectedHierarchyTreeChange(
          (event as CustomEvent).detail.selectedItem,
        ),
    );
    this.htmlElement.addEventListener(ViewerEvents.RectsDblClick, (event) => {
      if (
        (event as CustomEvent).detail.clickedRectId.includes(
          ViewCaptureUtils.NEXUS_LAUNCHER_PACKAGE_NAME,
        )
      ) {
        this.switchToNexusLauncherViewer();
      }
    });

    this.view = new View(
      ViewType.TAB,
      this.getDependencies(),
      this.htmlElement,
      'Surface Flinger',
      TraceType.SURFACE_FLINGER,
    );
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await this.presenter.onAppEvent(event);
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitAppEvent = callback;
  }

  // TODO: Make this generic by package name once TraceType is not explicitly defined
  async switchToNexusLauncherViewer() {
    await this.emitAppEvent(
      new TabbedViewSwitchRequest(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY),
    );
  }

  getViews(): View[] {
    return [this.view];
  }

  getDependencies(): TraceType[] {
    return ViewerSurfaceFlinger.DEPENDENCIES;
  }
}

export {ViewerSurfaceFlinger};
