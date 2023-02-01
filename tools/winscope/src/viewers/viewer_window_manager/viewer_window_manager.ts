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
import {MockStorage} from 'test/unit/mock_storage';
import {TraceType} from 'trace/trace_type';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {View, Viewer, ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class ViewerWindowManager implements Viewer {
  constructor() {
    this.htmlElement = document.createElement('viewer-window-manager');
    this.presenter = new Presenter((uiData: UiData) => {
      // Angular does not deep watch @Input properties. Clearing inputData to null before repopulating
      // automatically ensures that the UI will change via the Angular change detection cycle. Without
      // resetting, Angular does not auto-detect that inputData has changed.
      (this.htmlElement as any).inputData = null;
      (this.htmlElement as any).inputData = uiData;
    }, new MockStorage());
    this.htmlElement.addEventListener(ViewerEvents.HierarchyPinnedChange, (event) =>
      this.presenter.updatePinnedItems((event as CustomEvent).detail.pinnedItem)
    );
    this.htmlElement.addEventListener(ViewerEvents.HighlightedChange, (event) =>
      this.presenter.updateHighlightedItems(`${(event as CustomEvent).detail.id}`)
    );
    this.htmlElement.addEventListener(ViewerEvents.HierarchyUserOptionsChange, (event) =>
      this.presenter.updateHierarchyTree((event as CustomEvent).detail.userOptions)
    );
    this.htmlElement.addEventListener(ViewerEvents.HierarchyFilterChange, (event) =>
      this.presenter.filterHierarchyTree((event as CustomEvent).detail.filterString)
    );
    this.htmlElement.addEventListener(ViewerEvents.PropertiesUserOptionsChange, (event) =>
      this.presenter.updatePropertiesTree((event as CustomEvent).detail.userOptions)
    );
    this.htmlElement.addEventListener(ViewerEvents.PropertiesFilterChange, (event) =>
      this.presenter.filterPropertiesTree((event as CustomEvent).detail.filterString)
    );
    this.htmlElement.addEventListener(ViewerEvents.SelectedTreeChange, (event) =>
      this.presenter.newPropertiesTree((event as CustomEvent).detail.selectedItem)
    );
  }

  notifyCurrentTraceEntries(entries: Map<TraceType, any>): void {
    this.presenter.notifyCurrentTraceEntries(entries);
  }

  getViews(): View[] {
    return [new View(ViewType.TAB, this.getDependencies(), this.htmlElement, 'Window Manager')];
  }

  getDependencies(): TraceType[] {
    return ViewerWindowManager.DEPENDENCIES;
  }

  static readonly DEPENDENCIES: TraceType[] = [TraceType.WINDOW_MANAGER];
  private htmlElement: HTMLElement;
  private presenter: Presenter;
}

export {ViewerWindowManager};
