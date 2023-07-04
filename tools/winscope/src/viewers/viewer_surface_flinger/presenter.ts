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
import {Layer} from 'trace/flickerlib/layers/Layer';
import {LayerTraceEntry} from 'trace/flickerlib/layers/LayerTraceEntry';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {Rectangle, RectMatrix, RectTransform} from 'viewers/common/rectangle';
import {TreeGenerator} from 'viewers/common/tree_generator';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {UiData} from './ui_data';

type NotifyViewCallbackType = (uiData: UiData) => void;

export class Presenter {
  private readonly notifyViewCallback: NotifyViewCallbackType;
  private readonly trace: Trace<LayerTraceEntry>;
  private uiData: UiData;
  private hierarchyFilter: FilterType = TreeUtils.makeNodeFilter('');
  private propertiesFilter: FilterType = TreeUtils.makeNodeFilter('');
  private highlightedItems: string[] = [];
  private displayIds: number[] = [];
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
    this.trace = assertDefined(traces.getTrace(TraceType.SURFACE_FLINGER));
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new UiData([TraceType.SURFACE_FLINGER]);
    this.copyUiDataAndNotifyView();
  }

  async onTracePositionUpdate(position: TracePosition) {
    this.uiData = new UiData();
    this.uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData.propertiesUserOptions = this.propertiesUserOptions;

    const entry = TraceEntryFinder.findCorrespondingEntry(this.trace, position);
    const prevEntry =
      entry && entry.getIndex() > 0 ? this.trace.getEntry(entry.getIndex() - 1) : undefined;

    this.entry = (await entry?.getValue()) ?? null;
    this.previousEntry = (await prevEntry?.getValue()) ?? null;
    if (this.entry) {
      this.uiData.highlightedItems = this.highlightedItems;
      this.uiData.rects = this.generateRects();
      this.uiData.displayIds = this.displayIds;
      this.uiData.tree = this.generateTree();
    }

    this.copyUiDataAndNotifyView();
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

  updateHighlightedItems(id: string) {
    if (this.highlightedItems.includes(id)) {
      this.highlightedItems = this.highlightedItems.filter((hl) => hl !== id);
    } else {
      this.highlightedItems = []; //if multi-select surfaces implemented, remove this line
      this.highlightedItems.push(id);
    }
    this.uiData.highlightedItems = this.highlightedItems;
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

  private generateRects(): Rectangle[] {
    const displayRects =
      this.entry.displays.map((display: any) => {
        const rect = display.layerStackSpace;
        rect.label = 'Display';
        if (display.name) {
          rect.label += ` - ${display.name}`;
        }
        rect.stableId = `Display - ${display.id}`;
        rect.displayId = display.layerStackId;
        rect.isDisplay = true;
        rect.cornerRadius = 0;
        rect.isVirtual = display.isVirtual ?? false;
        rect.transform = {
          matrix: display.transform.matrix,
        };
        return rect;
      }) ?? [];
    this.displayIds = this.entry.displays.map((it: any) => it.layerStackId);
    this.displayIds.sort();
    const rects = this.getLayersForRectsView()
      .sort(this.compareLayerZ)
      .map((it: any) => {
        const rect = it.rect;
        rect.displayId = it.stackId;
        rect.cornerRadius = it.cornerRadius;
        if (!this.displayIds.includes(it.stackId)) {
          this.displayIds.push(it.stackId);
        }
        rect.transform = {
          matrix: rect.transform.matrix,
        };
        return rect;
      });

    return this.rectsToUiData(rects.concat(displayRects));
  }

  private getLayersForRectsView(): Layer[] {
    const onlyVisible = this.hierarchyUserOptions['onlyVisible']?.enabled ?? false;
    // Show only visible layers or Visible + Occluded layers. Don't show all layers
    // (flattenedLayers) because container layers are never meant to be displayed
    return this.entry.flattenedLayers.filter(
      (it: any) => it.isVisible || (!onlyVisible && it.occludedBy.length > 0)
    );
  }

  private compareLayerZ(a: Layer, b: Layer): number {
    const zipLength = Math.min(a.zOrderPath.length, b.zOrderPath.length);
    for (let i = 0; i < zipLength; ++i) {
      const zOrderA = a.zOrderPath[i];
      const zOrderB = b.zOrderPath[i];
      if (zOrderA > zOrderB) return -1;
      if (zOrderA < zOrderB) return 1;
    }
    return b.zOrderPath.length - a.zOrderPath.length;
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
    if (!this.hierarchyUserOptions['showDiff']?.enabled) {
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

  private rectsToUiData(rects: any[]): Rectangle[] {
    const uiRects: Rectangle[] = [];
    rects.forEach((rect: any) => {
      let t = null;
      if (rect.transform && rect.transform.matrix) {
        t = rect.transform.matrix;
      } else if (rect.transform) {
        t = rect.transform;
      }
      let transform: RectTransform | null = null;
      if (t !== null) {
        const matrix: RectMatrix = {
          dsdx: t.dsdx,
          dsdy: t.dsdy,
          dtdx: t.dtdx,
          dtdy: t.dtdy,
          tx: t.tx,
          ty: t.ty,
        };
        transform = {
          matrix,
        };
      }

      const newRect: Rectangle = {
        topLeft: {x: rect.left, y: rect.top},
        bottomRight: {x: rect.right, y: rect.bottom},
        label: rect.label,
        transform,
        isVisible: rect.ref?.isVisible ?? false,
        isDisplay: rect.isDisplay ?? false,
        ref: rect.ref,
        id: rect.stableId ?? rect.ref.stableId,
        displayId: rect.displayId ?? rect.ref.stackId,
        isVirtual: rect.isVirtual ?? false,
        isClickable: !(rect.isDisplay ?? false),
        cornerRadius: rect.cornerRadius,
      };
      uiRects.push(newRect);
    });
    return uiRects;
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
      .setIsShowDiff(this.propertiesUserOptions['showDiff']?.enabled)
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
