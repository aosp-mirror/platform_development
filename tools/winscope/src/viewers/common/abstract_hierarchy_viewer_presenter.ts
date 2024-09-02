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

import {assertDefined} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {Store} from 'common/store';
import {TracePositionUpdate, WinscopeEvent} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {RectsPresenter} from 'viewers/common/rects_presenter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UserOptions} from 'viewers/common/user_options';
import {HierarchyPresenter} from './hierarchy_presenter';
import {RectShowState} from './rect_show_state';
import {UiDataHierarchy} from './ui_data_hierarchy';
import {ViewerEvents} from './viewer_events';

export type NotifyHierarchyViewCallbackType<UiData> = (uiData: UiData) => void;

export abstract class AbstractHierarchyViewerPresenter<
  UiData extends UiDataHierarchy,
> implements WinscopeEventEmitter
{
  protected emitWinscopeEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  protected overridePropertiesTree: PropertyTreeNode | undefined;
  protected overridePropertiesTreeName: string | undefined;
  protected rectsPresenter?: RectsPresenter;
  protected abstract hierarchyPresenter: HierarchyPresenter;
  protected abstract propertiesPresenter: PropertiesPresenter;
  protected abstract readonly multiTraceType?: TraceType;
  private highlightedItem = '';

  constructor(
    private readonly trace: Trace<HierarchyTreeNode> | undefined,
    protected readonly traces: Traces,
    protected readonly storage: Readonly<Store>,
    private readonly notifyViewCallback: NotifyHierarchyViewCallbackType<UiData>,
    protected readonly uiData: UiData,
  ) {
    this.copyUiDataAndNotifyView();
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitWinscopeEvent = callback;
  }

  addEventListeners(htmlElement: HTMLElement) {
    htmlElement.addEventListener(ViewerEvents.HierarchyPinnedChange, (event) =>
      this.onPinnedItemChange((event as CustomEvent).detail.pinnedItem),
    );
    htmlElement.addEventListener(
      ViewerEvents.HighlightedIdChange,
      async (event) =>
        await this.onHighlightedIdChange((event as CustomEvent).detail.id),
    );
    htmlElement.addEventListener(
      ViewerEvents.HighlightedPropertyChange,
      (event) =>
        this.onHighlightedPropertyChange((event as CustomEvent).detail.id),
    );
    htmlElement.addEventListener(
      ViewerEvents.HierarchyUserOptionsChange,
      async (event) =>
        await this.onHierarchyUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        ),
    );
    htmlElement.addEventListener(
      ViewerEvents.HierarchyFilterChange,
      async (event) =>
        await this.onHierarchyFilterChange(
          (event as CustomEvent).detail.filterString,
        ),
    );
    htmlElement.addEventListener(
      ViewerEvents.PropertiesUserOptionsChange,
      async (event) =>
        await this.onPropertiesUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        ),
    );
    htmlElement.addEventListener(
      ViewerEvents.PropertiesFilterChange,
      async (event) =>
        await this.onPropertiesFilterChange(
          (event as CustomEvent).detail.filterString,
        ),
    );
    htmlElement.addEventListener(
      ViewerEvents.HighlightedNodeChange,
      async (event) =>
        await this.onHighlightedNodeChange((event as CustomEvent).detail.node),
    );
    htmlElement.addEventListener(
      ViewerEvents.RectShowStateChange,
      async (event) => {
        await this.onRectShowStateChange(
          (event as CustomEvent).detail.rectId,
          (event as CustomEvent).detail.state,
        );
      },
    );
    htmlElement.addEventListener(
      ViewerEvents.RectsUserOptionsChange,
      (event) => {
        this.onRectsUserOptionsChange(
          (event as CustomEvent).detail.userOptions,
        );
      },
    );
  }

  onPinnedItemChange(pinnedItem: UiHierarchyTreeNode) {
    this.hierarchyPresenter.applyPinnedItemChange(pinnedItem);
    this.uiData.pinnedItems = this.hierarchyPresenter.getPinnedItems();
    this.copyUiDataAndNotifyView();
  }

  onHighlightedPropertyChange(id: string) {
    this.propertiesPresenter.applyHighlightedPropertyChange(id);
    this.uiData.highlightedProperty =
      this.propertiesPresenter.getHighlightedProperty();
    this.copyUiDataAndNotifyView();
  }

  onRectsUserOptionsChange(userOptions: UserOptions) {
    if (!this.rectsPresenter) {
      return;
    }
    this.rectsPresenter.applyRectsUserOptionsChange(userOptions);

    this.uiData.rectsUserOptions = this.rectsPresenter.getUserOptions();
    this.uiData.rectsToDraw = this.rectsPresenter.getRectsToDraw();
    this.uiData.rectIdToShowState = this.rectsPresenter.getRectIdToShowState();

    this.copyUiDataAndNotifyView();
  }

  async onHierarchyUserOptionsChange(userOptions: UserOptions) {
    await this.hierarchyPresenter.applyHierarchyUserOptionsChange(userOptions);
    this.uiData.hierarchyUserOptions = this.hierarchyPresenter.getUserOptions();
    this.uiData.hierarchyTrees = this.hierarchyPresenter.getAllFormattedTrees();
    this.uiData.pinnedItems = this.hierarchyPresenter.getPinnedItems();
    this.copyUiDataAndNotifyView();
  }

  async onHierarchyFilterChange(filterString: string) {
    await this.hierarchyPresenter.applyHierarchyFilterChange(filterString);
    this.uiData.hierarchyTrees = this.hierarchyPresenter.getAllFormattedTrees();
    this.uiData.pinnedItems = this.hierarchyPresenter.getPinnedItems();
    this.copyUiDataAndNotifyView();
  }

  async onPropertiesUserOptionsChange(userOptions: UserOptions) {
    if (!this.propertiesPresenter) {
      return;
    }
    this.propertiesPresenter.applyPropertiesUserOptionsChange(userOptions);
    await this.updatePropertiesTree();
    this.uiData.propertiesUserOptions =
      this.propertiesPresenter.getUserOptions();
    this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    this.copyUiDataAndNotifyView();
  }

  async onPropertiesFilterChange(filterString: string) {
    if (!this.propertiesPresenter) {
      return;
    }
    this.propertiesPresenter.applyPropertiesFilterChange(filterString);
    await this.updatePropertiesTree();
    this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    this.copyUiDataAndNotifyView();
  }

  async onRectShowStateChange(id: string, newShowState: RectShowState) {
    if (!this.rectsPresenter) {
      return;
    }
    this.rectsPresenter.applyRectShowStateChange(id, newShowState);

    this.uiData.rectsToDraw = this.rectsPresenter.getRectsToDraw();
    this.uiData.rectIdToShowState = this.rectsPresenter.getRectIdToShowState();
    this.copyUiDataAndNotifyView();
  }

  protected async applyTracePositionUpdate(event: TracePositionUpdate) {
    let entries: Array<TraceEntry<HierarchyTreeNode>> = [];
    if (this.multiTraceType !== undefined) {
      entries = this.traces
        .getTraces(this.multiTraceType)
        .map((trace) => {
          return TraceEntryFinder.findCorrespondingEntry(
            trace,
            event.position,
          ) as TraceEntry<HierarchyTreeNode> | undefined;
        })
        .filter((entry) => entry !== undefined) as Array<
        TraceEntry<HierarchyTreeNode>
      >;
    } else {
      const entry = TraceEntryFinder.findCorrespondingEntry(
        assertDefined(this.trace),
        event.position,
      );
      if (entry) entries.push(entry);
    }

    try {
      await this.hierarchyPresenter.applyTracePositionUpdate(
        entries,
        this.highlightedItem,
      );
    } catch (e) {
      this.hierarchyPresenter.clear();
      this.rectsPresenter?.clear();
      this.propertiesPresenter.clear();
      this.refreshHierarchyViewerUiData();
      throw e;
    }

    const propertiesOpts = this.propertiesPresenter.getUserOptions();
    const hasPreviousEntry = entries.some((e) => e.getIndex() > 0);
    if (propertiesOpts['showDiff']?.isUnavailable !== undefined) {
      propertiesOpts['showDiff'].isUnavailable = !hasPreviousEntry;
    }

    const currentHierarchyTrees =
      this.hierarchyPresenter.getAllCurrentHierarchyTrees();
    if (currentHierarchyTrees) {
      this.rectsPresenter?.applyHierarchyTreesChange(currentHierarchyTrees);
      await this.updatePropertiesTree();
    }
  }

  protected async applyHighlightedNodeChange(node: UiHierarchyTreeNode) {
    this.updateHighlightedItem(node.id);
    this.hierarchyPresenter.applyHighlightedNodeChange(node);
    await this.updatePropertiesTree();
  }

  protected async applyHighlightedIdChange(newId: string) {
    this.updateHighlightedItem(newId);
    this.hierarchyPresenter.applyHighlightedIdChange(newId);
    await this.updatePropertiesTree();
  }

  protected async updatePropertiesTree() {
    if (this.overridePropertiesTree) {
      this.propertiesPresenter.setPropertiesTree(this.overridePropertiesTree);
      await this.propertiesPresenter.formatPropertiesTree(
        undefined,
        this.overridePropertiesTreeName,
        false,
      );
      return;
    }
    const selected = this.hierarchyPresenter.getSelectedTree();
    if (selected) {
      const [trace, selectedTree] = selected;
      const propertiesTree = await selectedTree.getAllProperties();
      if (
        this.propertiesPresenter.getUserOptions()['showDiff']?.enabled &&
        !this.hierarchyPresenter.getPreviousHierarchyTreeForTrace(trace)
      ) {
        await this.hierarchyPresenter.updatePreviousHierarchyTrees();
      }
      const previousTree =
        this.hierarchyPresenter.getPreviousHierarchyTreeForTrace(trace);
      this.propertiesPresenter.setPropertiesTree(propertiesTree);
      await this.propertiesPresenter.formatPropertiesTree(
        previousTree,
        this.getOverrideDisplayName(selected),
        this.keepCalculated(selectedTree),
      );
    } else {
      this.propertiesPresenter.clear();
    }
  }

  protected updateHighlightedItem(id: string) {
    if (this.highlightedItem === id) {
      this.highlightedItem = '';
    } else {
      this.highlightedItem = id;
    }
  }

  protected refreshHierarchyViewerUiData() {
    this.uiData.highlightedItem = this.highlightedItem;
    this.uiData.pinnedItems = this.hierarchyPresenter.getPinnedItems();
    this.uiData.hierarchyUserOptions = this.hierarchyPresenter.getUserOptions();
    this.uiData.hierarchyTrees = this.hierarchyPresenter.getAllFormattedTrees();

    this.uiData.propertiesUserOptions =
      this.propertiesPresenter.getUserOptions();
    this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    this.uiData.highlightedProperty =
      this.propertiesPresenter.getHighlightedProperty();

    if (this.rectsPresenter) {
      this.uiData.rectsToDraw = this.rectsPresenter?.getRectsToDraw();
      this.uiData.rectIdToShowState =
        this.rectsPresenter.getRectIdToShowState();
      this.uiData.displays = this.rectsPresenter.getDisplays();
      this.uiData.rectsUserOptions = this.rectsPresenter.getUserOptions();
    }

    this.copyUiDataAndNotifyView();
  }

  protected getHighlightedItem(): string | undefined {
    return this.highlightedItem;
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }

  protected getEntryFormattedTimestamp(
    entry: TraceEntry<HierarchyTreeNode>,
  ): string {
    if (entry.getFullTrace().isDumpWithoutTimestamp()) {
      return 'Dump';
    }
    return entry.getTimestamp().format();
  }

  abstract onAppEvent(event: WinscopeEvent): Promise<void>;
  abstract onHighlightedNodeChange(node: UiHierarchyTreeNode): Promise<void>;
  abstract onHighlightedIdChange(id: string): Promise<void>;
  protected abstract keepCalculated(tree: HierarchyTreeNode): boolean;
  protected abstract getOverrideDisplayName(
    selected: [Trace<HierarchyTreeNode>, HierarchyTreeNode],
  ): string | undefined;
}
