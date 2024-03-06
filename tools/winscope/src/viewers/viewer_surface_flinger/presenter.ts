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

import {assertDefined} from 'common/assert_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {FilterType, TreeUtils} from 'common/tree_utils';
import {Layer} from 'flickerlib/layers/Layer';
import {LayerTraceEntry} from 'flickerlib/layers/LayerTraceEntry';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType} from 'trace/trace_type';
import {SurfaceFlingerUtils} from 'viewers/common/surface_flinger_utils';
import {TreeGenerator} from 'viewers/common/tree_generator';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {ViewCaptureUtils} from 'viewers/common/view_capture_utils';
import {UiData} from './ui_data';

type NotifyViewCallbackType = (uiData: UiData) => void;

export class Presenter {
  private readonly notifyViewCallback: NotifyViewCallbackType;
  private readonly traces: Traces;
  private readonly trace: Trace<LayerTraceEntry>;
  private viewCapturePackageNames: string[] = [];
  private uiData: UiData;
  private hierarchyFilter: FilterType = TreeUtils.makeNodeFilter('');
  private propertiesFilter: FilterType = TreeUtils.makeNodeFilter('');
  private highlightedItem: string = '';
  private highlightedProperty: string = '';
  private pinnedItems: HierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];
  private selectedHierarchyTree: HierarchyTreeNode | null = null;
  private selectedLayer: LayerTraceEntry | Layer | null = null;
  private previousEntry: LayerTraceEntry | null = null;
  private entry: LayerTraceEntry | null = null;
  private hierarchyUserOptions: UserOptions = PersistentStoreProxy.new<UserOptions>(
    'SfHierarchyOptions',
    {
      showDiff: {
        name: 'Show diff', // TODO: PersistentStoreObject.Ignored("Show diff") or something like that to instruct to not store this info
        enabled: false,
        isUnavailable: false,
      },
      simplifyNames: {
        name: 'Simplify names',
        enabled: true,
      },
      onlyVisible: {
        name: 'Only visible',
        enabled: false,
      },
      flat: {
        name: 'Flat',
        enabled: false,
      },
    },
    this.storage
  );

  private propertiesUserOptions: UserOptions = PersistentStoreProxy.new<UserOptions>(
    'SfPropertyOptions',
    {
      showDiff: {
        name: 'Show diff',
        enabled: false,
        isUnavailable: false,
      },
      showDefaults: {
        name: 'Show defaults',
        enabled: false,
        tooltip: `
                If checked, shows the value of all properties.
                Otherwise, hides all properties whose value is
                the default for its data type.
              `,
      },
    },
    this.storage
  );

  constructor(
    traces: Traces,
    private readonly storage: Storage,
    notifyViewCallback: NotifyViewCallbackType
  ) {
    this.traces = traces;
    this.trace = assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER));
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new UiData([TraceType.SURFACE_FLINGER]);
    this.copyUiDataAndNotifyView();
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TRACE_POSITION_UPDATE, async (event) => {
      await this.initializeIfNeeded();

      const entry = TraceEntryFinder.findCorrespondingEntry(this.trace, event.position);
      const prevEntry =
        entry && entry.getIndex() > 0 ? this.trace.getEntry(entry.getIndex() - 1) : undefined;

      this.entry = (await entry?.getValue()) ?? null;
      this.previousEntry = (await prevEntry?.getValue()) ?? null;
      if (this.hierarchyUserOptions['showDiff'].isUnavailable !== undefined) {
        this.hierarchyUserOptions['showDiff'].isUnavailable = this.previousEntry == null;
      }
      if (this.propertiesUserOptions['showDiff'].isUnavailable !== undefined) {
        this.propertiesUserOptions['showDiff'].isUnavailable = this.previousEntry == null;
      }

      this.uiData = new UiData();
      this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
      this.uiData.propertiesUserOptions = this.propertiesUserOptions;

      if (this.entry) {
        this.uiData.highlightedItem = this.highlightedItem;
        this.uiData.highlightedProperty = this.highlightedProperty;
        this.uiData.rects = SurfaceFlingerUtils.makeRects(
          this.entry,
          this.viewCapturePackageNames,
          this.hierarchyUserOptions
        );
        this.uiData.displayIds = this.getDisplayIds(this.entry);
        this.uiData.tree = this.generateTree();
      }
      this.copyUiDataAndNotifyView();
    });
  }

  updatePinnedItems(pinnedItem: HierarchyTreeNode) {
    const pinnedId = `${pinnedItem.id}`;
    if (this.pinnedItems.map((item) => `${item.id}`).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter((pinned) => `${pinned.id}` !== pinnedId);
    } else {
      this.pinnedItems.push(pinnedItem);
    }
    this.updatePinnedIds(pinnedId);
    this.uiData.pinnedItems = this.pinnedItems;
    this.copyUiDataAndNotifyView();
  }

  updateHighlightedItem(id: string) {
    if (this.highlightedItem === id) {
      this.highlightedItem = '';
    } else {
      this.highlightedItem = id;
    }
    this.uiData.highlightedItem = this.highlightedItem;
    this.copyUiDataAndNotifyView();
  }

  updateHighlightedProperty(id: string) {
    if (this.highlightedProperty === id) {
      this.highlightedProperty = '';
    } else {
      this.highlightedProperty = id;
    }
    this.uiData.highlightedProperty = this.highlightedProperty;
    this.copyUiDataAndNotifyView();
  }

  updateHierarchyTree(userOptions: UserOptions) {
    this.hierarchyUserOptions = userOptions;
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.tree = this.generateTree();
    this.copyUiDataAndNotifyView();
  }

  filterHierarchyTree(filterString: string) {
    this.hierarchyFilter = TreeUtils.makeNodeFilter(filterString);
    this.uiData.tree = this.generateTree();
    this.copyUiDataAndNotifyView();
  }

  updatePropertiesTree(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;
    this.updateSelectedTreeUiData();
  }

  filterPropertiesTree(filterString: string) {
    this.propertiesFilter = TreeUtils.makeNodeFilter(filterString);
    this.updateSelectedTreeUiData();
  }

  newPropertiesTree(selectedItem: HierarchyTreeNode) {
    this.selectedHierarchyTree = selectedItem;
    this.updateSelectedTreeUiData();
  }

  private async initializeIfNeeded() {
    this.viewCapturePackageNames = await ViewCaptureUtils.getPackageNames(this.traces);
  }

  private getDisplayIds(entry: LayerTraceEntry): number[] {
    const ids = new Set<number>();
    entry.displays.forEach((display: any) => {
      ids.add(display.layerStackId);
    });
    entry.flattenedLayers.forEach((layer: Layer) => {
      ids.add(layer.stackId);
    });
    return Array.from(ids.values()).sort((a, b) => {
      return a - b;
    });
  }

  private updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree) {
      this.uiData.propertiesTree = this.getTreeWithTransformedProperties(
        this.selectedHierarchyTree
      );
      this.uiData.selectedLayer = this.selectedLayer;
      this.uiData.displayPropertyGroups = this.shouldDisplayPropertyGroups(this.selectedLayer);
    }
    this.copyUiDataAndNotifyView();
  }

  private generateTree() {
    if (!this.entry) {
      return null;
    }

    const generator = new TreeGenerator(this.entry, this.hierarchyFilter, this.pinnedIds)
      .setIsOnlyVisibleView(this.hierarchyUserOptions['onlyVisible']?.enabled)
      .setIsSimplifyNames(this.hierarchyUserOptions['simplifyNames']?.enabled)
      .setIsFlatView(this.hierarchyUserOptions['flat']?.enabled)
      .withUniqueNodeId();
    let tree: HierarchyTreeNode | null;
    if (
      !this.hierarchyUserOptions['showDiff']?.enabled ||
      this.hierarchyUserOptions['showDiff']?.isUnavailable
    ) {
      tree = generator.generateTree();
    } else {
      tree = generator
        .compareWith(this.previousEntry)
        .withModifiedCheck()
        .generateFinalTreeWithDiff();
    }
    this.pinnedItems = generator.getPinnedItems();
    this.uiData.pinnedItems = this.pinnedItems;
    return tree;
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  private getTreeWithTransformedProperties(selectedTree: HierarchyTreeNode): PropertiesTreeNode {
    const transformer = new TreeTransformer(selectedTree, this.propertiesFilter)
      .setOnlyProtoDump(true)
      .setIsShowDefaults(this.propertiesUserOptions['showDefaults']?.enabled)
      .setIsShowDiff(
        this.propertiesUserOptions['showDiff']?.enabled &&
          !this.propertiesUserOptions['showDiff']?.isUnavailable
      )
      .setTransformerOptions({skip: selectedTree.skip})
      .setProperties(this.entry)
      .setDiffProperties(this.previousEntry);
    this.selectedLayer = transformer.getOriginalFlickerItem(this.entry, selectedTree.stableId);
    const transformedTree = transformer.transform();
    return transformedTree;
  }

  private shouldDisplayPropertyGroups(selectedLayer: Layer): boolean {
    // Do not display property groups when the root layer is selected. The root layer doesn't
    // provide property groups info (visibility, geometry transforms, ...).
    const isRoot = selectedLayer === this.entry;
    return !isRoot;
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }
}
