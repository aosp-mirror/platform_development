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
import {TransformMatrix} from 'common/geometry_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {FilterType, TreeUtils} from 'common/tree_utils';
import {DisplayContent} from 'flickerlib/windows/DisplayContent';
import {WindowManagerState} from 'flickerlib/windows/WindowManagerState';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceTreeNode} from 'trace/trace_tree_node';
import {TraceType} from 'trace/trace_type';
import {TreeGenerator} from 'viewers/common/tree_generator';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {UiRect} from 'viewers/components/rects/types2d';
import {UiData} from './ui_data';

type NotifyViewCallbackType = (uiData: UiData) => void;

export class Presenter {
  private readonly trace: Trace<WindowManagerState>;
  private readonly notifyViewCallback: NotifyViewCallbackType;
  private uiData: UiData;
  private hierarchyFilter: FilterType = TreeUtils.makeNodeFilter('');
  private propertiesFilter: FilterType = TreeUtils.makeNodeFilter('');
  private highlightedItem: string = '';
  private highlightedProperty: string = '';
  private pinnedItems: HierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];
  private selectedHierarchyTree: HierarchyTreeNode | null = null;
  private previousEntry: TraceTreeNode | null = null;
  private entry: TraceTreeNode | null = null;
  private hierarchyUserOptions: UserOptions = PersistentStoreProxy.new<UserOptions>(
    'WmHierarchyOptions',
    {
      showDiff: {
        name: 'Show diff',
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
    'WmPropertyOptions',
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
    private storage: Storage,
    notifyViewCallback: NotifyViewCallbackType
  ) {
    this.trace = assertDefined(traces.getTrace(TraceType.WINDOW_MANAGER));
    this.notifyViewCallback = notifyViewCallback;
    this.uiData = new UiData([TraceType.WINDOW_MANAGER]);
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

  newPropertiesTree(selectedTree: HierarchyTreeNode) {
    this.selectedHierarchyTree = selectedTree;
    this.updateSelectedTreeUiData();
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TRACE_POSITION_UPDATE, async (event) => {
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
        this.uiData.rects = this.generateRects(this.entry);
        this.uiData.displayIds = this.getDisplayIds(this.entry);
        this.uiData.tree = this.generateTree();
      }

      this.copyUiDataAndNotifyView();
    });
  }

  private generateRects(entry: TraceTreeNode): UiRect[] {
    const identityMatrix: TransformMatrix = {
      dsdx: 1,
      dsdy: 0,
      tx: 0,
      dtdx: 0,
      dtdy: 1,
      ty: 0,
    };
    const displayRects: UiRect[] =
      entry.displays?.map((display: DisplayContent) => {
        const rect: UiRect = {
          x: display.displayRect.left,
          y: display.displayRect.top,
          w: display.displayRect.right - display.displayRect.left,
          h: display.displayRect.bottom - display.displayRect.top,
          label: `Display - ${display.title}`,
          transform: identityMatrix,
          isVisible: false, //TODO: check if displayRect.ref.isVisible exists
          isDisplay: true,
          id: display.stableId,
          displayId: display.id,
          isVirtual: false,
          isClickable: false,
          cornerRadius: 0,
        };
        return rect;
      }) ?? [];

    const windowRects: UiRect[] =
      entry.windowStates
        ?.sort((a: any, b: any) => b.computedZ - a.computedZ)
        .map((it: any) => {
          const rect: UiRect = {
            x: it.rect.left,
            y: it.rect.top,
            w: it.rect.right - it.rect.left,
            h: it.rect.bottom - it.rect.top,
            label: it.rect.label,
            transform: identityMatrix,
            isVisible: it.isVisible,
            isDisplay: false,
            id: it.stableId,
            displayId: it.displayId,
            isVirtual: false, //TODO: is this correct?
            isClickable: true,
            cornerRadius: 0,
          };
          return rect;
        }) ?? [];

    return windowRects.concat(displayRects);
  }

  private getDisplayIds(entry: TraceTreeNode): number[] {
    const ids = new Set<number>();
    entry.windowStates?.map((it: any) => {
      ids.add(it.displayId);
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
    if (!this.entry) {
      return {};
    }
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
    const transformedTree = transformer.transform();
    return transformedTree;
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyViewCallback(copy);
  }
}
