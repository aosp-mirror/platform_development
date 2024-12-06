/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {
  AbstractHierarchyViewerPresenter,
  NotifyHierarchyViewCallbackType,
} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {HierarchyPresenter} from 'viewers/common/hierarchy_presenter';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {RectsPresenter} from 'viewers/common/rects_presenter';
import {RectShowState} from 'viewers/common/rect_show_state';
import {TextFilter} from 'viewers/common/text_filter';
import {UiDataHierarchy} from 'viewers/common/ui_data_hierarchy';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UserOptions} from 'viewers/common/user_options';
import {UiRect} from 'viewers/components/rects/ui_rect';

export class MockPresenter extends AbstractHierarchyViewerPresenter<UiDataHierarchy> {
  protected override hierarchyPresenter = new HierarchyPresenter(
    {opt: {name: '', enabled: false}},
    new TextFilter(),
    [],
    true,
    false,
    this.getEntryFormattedTimestamp,
  );
  protected override propertiesPresenter = new PropertiesPresenter(
    {opt: {name: '', enabled: false}},
    new TextFilter(),
    [],
  );
  uiRects: UiRect[] = [];
  displays: DisplayIdentifier[] = [];

  constructor(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    storage: Store,
    notifyViewCallback: NotifyHierarchyViewCallbackType<UiDataHierarchy>,
    protected readonly multiTraceType: TraceType | undefined,
  ) {
    super(trace, traces, storage, notifyViewCallback, new MockData());
  }

  initializeRectsPresenter() {
    this.rectsPresenter = new RectsPresenter(
      {opt: {name: 'Test opt', enabled: false}},
      () => this.uiRects,
      () => this.displays,
    );
  }

  override async onHighlightedNodeChange(node: UiHierarchyTreeNode) {
    await this.applyHighlightedNodeChange(node);
    this.refreshHierarchyViewerUiData();
  }

  override async onHighlightedIdChange(id: string) {
    await this.applyHighlightedIdChange(id);
    this.refreshHierarchyViewerUiData();
  }

  protected override keepCalculated(): boolean {
    return false;
  }

  protected override getOverrideDisplayName(): string | undefined {
    return undefined;
  }

  protected override refreshUIData(): void {
    this.refreshHierarchyViewerUiData();
  }
}

export class MockData implements UiDataHierarchy {
  highlightedItem = '';
  pinnedItems: UiHierarchyTreeNode[] = [];
  hierarchyUserOptions: UserOptions = {};
  hierarchyTrees: UiHierarchyTreeNode[] | undefined;
  propertiesUserOptions: UserOptions = {};
  propertiesTree: UiPropertyTreeNode | undefined;
  highlightedProperty = '';
  hierarchyFilter = new TextFilter();
  propertiesFilter = new TextFilter();
  isDarkMode?: boolean;
  rectsToDraw: UiRect[] = [];
  rectIdToShowState = new Map<string, RectShowState>();
  displays: DisplayIdentifier[] = [];
  rectsUserOptions: UserOptions = {};
}
