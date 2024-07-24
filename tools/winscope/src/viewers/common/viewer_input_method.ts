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

import {WinscopeEvent} from 'messaging/winscope_event';
import {Traces} from 'trace/traces';
import {ImeTraceType} from 'trace/trace_type';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {PresenterInputMethod} from 'viewers/common/presenter_input_method';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {View, Viewer} from 'viewers/viewer';

abstract class ViewerInputMethod implements Viewer {
  protected readonly htmlElement: HTMLElement;
  protected readonly presenter: PresenterInputMethod;
  protected abstract readonly view: View;

  constructor(traces: Traces, storage: Storage) {
    this.htmlElement = document.createElement('viewer-input-method');
    this.presenter = this.initialisePresenter(traces, storage);
    this.addViewerEventListeners();
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

  abstract getDependencies(): ImeTraceType[];

  protected imeUiCallback = (uiData: ImeUiData) => {
    (this.htmlElement as any).inputData = uiData;
  };

  protected addViewerEventListeners() {
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
      ViewerEvents.HierarchyUserOptionsChange,
      (event) =>
        this.presenter.onHierarchyUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        ),
    );
    this.htmlElement.addEventListener(
      ViewerEvents.HierarchyFilterChange,
      (event) =>
        this.presenter.onHierarchyFilterChange(
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
    this.htmlElement.addEventListener(
      ViewerEvents.AdditionalPropertySelected,
      async (event) =>
        await this.presenter.onAdditionalPropertySelected(
          (event as CustomEvent).detail.selectedItem,
        ),
    );
  }

  protected abstract initialisePresenter(
    traces: Traces,
    storage: Storage,
  ): PresenterInputMethod;
}

export {ViewerInputMethod};
