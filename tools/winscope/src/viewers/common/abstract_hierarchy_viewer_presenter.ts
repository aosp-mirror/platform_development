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
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {parseMap, stringifyMap} from 'common/store/persistent_store_proxy';
import {Store} from 'common/store/store';
import {Analytics} from 'logging/analytics';
import {
  TracePositionUpdate,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {EmitEvent} from 'messaging/winscope_event_emitter';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TRACE_INFO} from 'trace/trace_info';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertiesPresenter} from 'viewers/common/properties_presenter';
import {RectsPresenter} from 'viewers/common/rects_presenter';
import {TextFilter} from 'viewers/common/text_filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UserOption, UserOptions} from 'viewers/common/user_options';
import {HierarchyPresenter, SelectedTree} from './hierarchy_presenter';
import {PresetHierarchy, TextFilterValues} from './preset_hierarchy';
import {RectShowState} from './rect_show_state';
import {UiDataHierarchy} from './ui_data_hierarchy';
import {ViewerEvents} from './viewer_events';

export type NotifyHierarchyViewCallbackType<UiData> = (uiData: UiData) => void;

export abstract class AbstractHierarchyViewerPresenter<
  UiData extends UiDataHierarchy,
> {
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
    uiData.isDarkMode = storage.get('dark-mode') === 'true';
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
      ViewerEvents.ArrowDownPress,
      async (event) =>
        await this.onArrowPress((event as CustomEvent).detail, false),
    );
    htmlElement.addEventListener(
      ViewerEvents.ArrowUpPress,
      async (event) =>
        await this.onArrowPress((event as CustomEvent).detail, true),
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
      async (event) => {
        const detail: TextFilter = (event as CustomEvent).detail;
        await this.onHierarchyFilterChange(detail);
      },
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
      async (event) => {
        const detail: TextFilter = (event as CustomEvent).detail;
        await this.onPropertiesFilterChange(detail);
      },
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
    this.addViewerSpecificListeners(htmlElement);
  }

  onPinnedItemChange(pinnedItem: UiHierarchyTreeNode) {
    this.hierarchyPresenter.applyPinnedItemChange(pinnedItem);
    this.uiData.pinnedItems = this.hierarchyPresenter.getPinnedItems();
    this.copyUiDataAndNotifyView();
  }

  async onArrowPress(storage: InMemoryStorage, getPrevious: boolean) {
    const newNode = this.hierarchyPresenter.getAdjacentVisibleNode(
      storage,
      getPrevious,
    );
    if (newNode) {
      await this.onHighlightedNodeChange(newNode);
    }
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

  async onHierarchyFilterChange(textFilter: TextFilter) {
    await this.hierarchyPresenter.applyHierarchyFilterChange(textFilter);
    this.uiData.hierarchyTrees = this.hierarchyPresenter.getAllFormattedTrees();
    this.uiData.pinnedItems = this.hierarchyPresenter.getPinnedItems();
    this.copyUiDataAndNotifyView();
  }

  async onPropertiesUserOptionsChange(userOptions: UserOptions) {
    this.propertiesPresenter.applyPropertiesUserOptionsChange(userOptions);
    await this.updatePropertiesTree();
    this.uiData.propertiesUserOptions =
      this.propertiesPresenter.getUserOptions();
    this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    this.copyUiDataAndNotifyView();
  }

  async onPropertiesFilterChange(textFilter: TextFilter) {
    this.propertiesPresenter.applyPropertiesFilterChange(textFilter);
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

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        if (this.initializeIfNeeded) await this.initializeIfNeeded(event);
        await this.applyTracePositionUpdate(event);
        if (this.processDataAfterPositionUpdate) {
          await this.processDataAfterPositionUpdate(event);
        }
        this.refreshUIData();
      },
    );
    await event.visit(
      WinscopeEventType.FILTER_PRESET_SAVE_REQUEST,
      async (event) => {
        this.saveConfigAsPreset(event.name);
      },
    );
    await event.visit(WinscopeEventType.DARK_MODE_TOGGLED, async (event) => {
      this.uiData.isDarkMode = event.isDarkMode;
      this.copyUiDataAndNotifyView();
    });
    await event.visit(
      WinscopeEventType.FILTER_PRESET_APPLY_REQUEST,
      async (event) => {
        const filterPresetName = event.name;
        await this.applyPresetConfig(filterPresetName);
        this.refreshUIData();
      },
    );
    await this.onViewerSpecificWinscopeEvent(event);
  }

  protected async onViewerSpecificWinscopeEvent(event: WinscopeEvent) {
    // do nothing
  }

  protected addViewerSpecificListeners(htmlElement: HTMLElement) {
    // do nothing;
  }

  protected saveConfigAsPreset(storeKey: string) {
    const preset: PresetHierarchy = {
      hierarchyUserOptions: this.uiData.hierarchyUserOptions,
      hierarchyFilter: TextFilterValues.fromTextFilter(
        this.uiData.hierarchyFilter,
      ),
      propertiesUserOptions: this.uiData.propertiesUserOptions,
      propertiesFilter: TextFilterValues.fromTextFilter(
        this.uiData.propertiesFilter,
      ),
      rectsUserOptions: this.uiData.rectsUserOptions,
      rectIdToShowState: this.uiData.rectIdToShowState,
    };
    this.storage.add(storeKey, JSON.stringify(preset, stringifyMap));
  }

  protected async applyPresetConfig(storeKey: string) {
    const preset = this.storage.get(storeKey);
    if (preset) {
      const parsedPreset: PresetHierarchy = JSON.parse(preset, parseMap);
      await this.hierarchyPresenter.applyHierarchyUserOptionsChange(
        parsedPreset.hierarchyUserOptions,
      );
      await this.hierarchyPresenter.applyHierarchyFilterChange(
        new TextFilter(
          parsedPreset.hierarchyFilter.filterString,
          parsedPreset.hierarchyFilter.flags,
        ),
      );

      this.propertiesPresenter.applyPropertiesUserOptionsChange(
        parsedPreset.propertiesUserOptions,
      );
      this.propertiesPresenter.applyPropertiesFilterChange(
        new TextFilter(
          parsedPreset.propertiesFilter.filterString,
          parsedPreset.propertiesFilter.flags,
        ),
      );
      await this.updatePropertiesTree();

      if (this.rectsPresenter) {
        this.rectsPresenter?.applyRectsUserOptionsChange(
          assertDefined(parsedPreset.rectsUserOptions),
        );
        this.rectsPresenter?.updateRectShowStates(
          parsedPreset.rectIdToShowState,
        );
      }
      this.refreshHierarchyViewerUiData();
    }
  }

  protected async applyTracePositionUpdate(event: TracePositionUpdate) {
    const hierarchyStartTime = Date.now();

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
      const showDiff = this.hierarchyPresenter.getUserOptions()['showDiff'];
      this.logFetchComponentData(hierarchyStartTime, 'hierarchy', showDiff);
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
      const rectStartTime = Date.now();
      this.rectsPresenter?.applyHierarchyTreesChange(currentHierarchyTrees);
      this.logFetchComponentData(rectStartTime, 'rects');

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
    const showDiff = this.propertiesPresenter.getUserOptions()['showDiff'];
    const propertiesStartTime = Date.now();

    if (this.overridePropertiesTree) {
      this.propertiesPresenter.setPropertiesTree(this.overridePropertiesTree);
      await this.propertiesPresenter.formatPropertiesTree(
        undefined,
        this.overridePropertiesTreeName,
        false,
      );
      this.logFetchComponentData(propertiesStartTime, 'properties', showDiff);
      return;
    }
    const selected = this.hierarchyPresenter.getSelectedTree();
    if (selected) {
      const {trace, tree: selectedTree} = selected;
      const propertiesTree = await selectedTree.getAllProperties();
      if (
        showDiff?.enabled &&
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
        trace.type,
      );
      this.logFetchComponentData(propertiesStartTime, 'properties', showDiff);
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
    this.uiData.hierarchyFilter = this.hierarchyPresenter.getTextFilter();

    this.uiData.propertiesUserOptions =
      this.propertiesPresenter.getUserOptions();
    this.uiData.propertiesTree = this.propertiesPresenter.getFormattedTree();
    this.uiData.highlightedProperty =
      this.propertiesPresenter.getHighlightedProperty();
    this.uiData.propertiesFilter = assertDefined(
      this.propertiesPresenter.getTextFilter(),
    );

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

  protected getEntryFormattedTimestamp(
    entry: TraceEntry<HierarchyTreeNode>,
  ): string {
    if (entry.getFullTrace().isDumpWithoutTimestamp()) {
      return 'Dump';
    }
    return entry.getTimestamp().format();
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }

  private logFetchComponentData(
    startTimeMs: number,
    component: 'hierarchy' | 'properties' | 'rects',
    showDiffs?: UserOption,
  ) {
    const traceName =
      TRACE_INFO[this.trace?.type ?? assertDefined(this.multiTraceType)].name;
    Analytics.Navigation.logFetchComponentDataTime(
      component,
      traceName,
      showDiffs !== undefined && showDiffs.enabled && !showDiffs.isUnavailable,
      Date.now() - startTimeMs,
    );
  }

  abstract onHighlightedNodeChange(node: UiHierarchyTreeNode): Promise<void>;
  abstract onHighlightedIdChange(id: string): Promise<void>;
  protected abstract keepCalculated(tree: HierarchyTreeNode): boolean;
  protected abstract getOverrideDisplayName(
    selected: SelectedTree,
  ): string | undefined;
  protected abstract refreshUIData(): void;
  protected initializeIfNeeded?(event: TracePositionUpdate): Promise<void>;
  protected processDataAfterPositionUpdate?(
    event: TracePositionUpdate,
  ): Promise<void>;
}
