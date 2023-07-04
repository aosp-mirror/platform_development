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

import {assertDefined} from 'common/assert_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {FilterType, TreeUtils} from 'common/tree_utils';
import {Point} from 'trace/flickerlib/common';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {Rectangle} from 'viewers/common/rectangle';
import {TreeGenerator} from 'viewers/common/tree_generator';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {UiData} from './ui_data';

export class Presenter {
  private viewCaptureTrace: Trace<object>;

  private selectedFrameData: any | undefined;
  private previousFrameData: any | undefined;
  private selectedHierarchyTree: HierarchyTreeNode | undefined;

  private uiData: UiData | undefined;

  private pinnedItems: HierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];

  private highlightedItems: string[] = [];

  private hierarchyFilter: FilterType = TreeUtils.makeNodeFilter('');
  private propertiesFilter: FilterType = TreeUtils.makeNodeFilter('');

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
    private readonly notifyUiDataCallback: (data: UiData) => void
  ) {
    this.viewCaptureTrace = assertDefined(traces.getTrace(TraceType.VIEW_CAPTURE));
  }

  async onTracePositionUpdate(position: TracePosition) {
    const entry = TraceEntryFinder.findCorrespondingEntry(this.viewCaptureTrace, position);

    let prevEntry: typeof entry;
    if (entry && entry.getIndex() > 0) {
      prevEntry = this.viewCaptureTrace.getEntry(entry.getIndex() - 1);
    }

    this.selectedFrameData = await entry?.getValue();
    this.previousFrameData = await prevEntry?.getValue();

    this.refreshUI();
  }

  private refreshUI() {
    // this.pinnedItems is updated in generateTree, so don't inline
    const tree = this.generateTree();
    if (!this.selectedHierarchyTree && tree) {
      this.selectedHierarchyTree = tree;
    }

    this.uiData = new UiData(
      this.generateRectangles(),
      tree,
      this.hierarchyUserOptions,
      this.propertiesUserOptions,
      this.pinnedItems,
      this.highlightedItems,
      this.getTreeWithTransformedProperties(this.selectedHierarchyTree)
    );
    this.notifyUiDataCallback(this.uiData);
  }

  private generateRectangles(): Rectangle[] {
    const rectangles: Rectangle[] = [];

    function inner(node: any /* ViewNode */) {
      const aRectangle: Rectangle = {
        topLeft: new Point(node.boxPos.left, node.boxPos.top),
        bottomRight: new Point(
          node.boxPos.left + node.boxPos.width,
          node.boxPos.top + node.boxPos.height
        ),
        label: '',
        transform: null,
        isVisible: node.isVisible,
        isDisplay: false,
        ref: {},
        id: node.id,
        displayId: 0,
        isVirtual: false,
        isClickable: true,
        cornerRadius: 0,
        depth: node.depth,
      };
      rectangles.push(aRectangle);
      node.children.forEach((it: any) /* ViewNode */ => inner(it));
    }
    if (this.selectedFrameData?.node) {
      inner(this.selectedFrameData.node);
    }

    return rectangles;
  }

  private generateTree(): HierarchyTreeNode | null {
    if (!this.selectedFrameData?.node) {
      return null;
    }
    const generator = new TreeGenerator(
      this.selectedFrameData.node,
      this.hierarchyFilter,
      this.pinnedIds
    )
      .setIsOnlyVisibleView(this.hierarchyUserOptions['onlyVisible']?.enabled)
      .setIsSimplifyNames(this.hierarchyUserOptions['simplifyNames']?.enabled)
      .withUniqueNodeId();

    this.pinnedItems = generator.getPinnedItems();

    if (this.hierarchyUserOptions['showDiff'].enabled && this.previousFrameData?.node) {
      return generator
        .compareWith(this.previousFrameData.node)
        .withModifiedCheck()
        .generateFinalTreeWithDiff();
    } else {
      return generator.generateTree();
    }
  }

  updatePinnedItems(pinnedItem: HierarchyTreeNode) {
    const pinnedId = `${pinnedItem.id}`;
    if (this.pinnedItems.map((item) => `${item.id}`).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter((pinned) => `${pinned.id}` !== pinnedId);
    } else {
      this.pinnedItems.push(pinnedItem);
    }
    this.updatePinnedIds(pinnedId);
    this.uiData!!.pinnedItems = this.pinnedItems;
    this.copyUiDataAndNotifyView();
  }

  updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  updateHighlightedItems(id: string) {
    if (this.highlightedItems.includes(id)) {
      this.highlightedItems = this.highlightedItems.filter((hl) => hl !== id);
    } else {
      this.highlightedItems = [];
      this.highlightedItems.push(id);
    }
    this.uiData!!.highlightedItems = this.highlightedItems;
    this.copyUiDataAndNotifyView();
  }

  updateHierarchyTree(userOptions: any) {
    this.hierarchyUserOptions = userOptions;
    this.uiData!!.hierarchyUserOptions = this.hierarchyUserOptions;
    this.uiData!!.tree = this.generateTree();
    this.copyUiDataAndNotifyView();
  }

  filterHierarchyTree(filterString: string) {
    this.hierarchyFilter = TreeUtils.makeNodeFilter(filterString);
    this.uiData!!.tree = this.generateTree();
    this.copyUiDataAndNotifyView();
  }

  updatePropertiesTree(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    this.uiData!!.propertiesUserOptions = this.propertiesUserOptions;
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

  private updateSelectedTreeUiData() {
    this.uiData!!.propertiesTree = this.getTreeWithTransformedProperties(
      this.selectedHierarchyTree
    );
    this.copyUiDataAndNotifyView();
  }

  private getTreeWithTransformedProperties(selectedTree: any): PropertiesTreeNode | null {
    if (!selectedTree) {
      return null;
    }

    return new TreeTransformer(selectedTree, this.propertiesFilter)
      .setOnlyProtoDump(false)
      .setIsShowDiff(this.propertiesUserOptions['showDiff']?.enabled)
      .setIsShowDefaults(this.propertiesUserOptions['showDefaults']?.enabled)
      .setProperties(this.selectedFrameData?.node)
      .setDiffProperties(this.previousFrameData?.node)
      .transform();
  }

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyUiDataCallback(copy);
  }
}
