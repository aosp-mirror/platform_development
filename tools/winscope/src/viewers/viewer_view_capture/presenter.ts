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
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {FrameData, TraceType, ViewNode} from 'trace/trace_type';
import {SurfaceFlingerUtils} from 'viewers/common/surface_flinger_utils';
import {TreeGenerator} from 'viewers/common/tree_generator';
import {TreeTransformer} from 'viewers/common/tree_transformer';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {ViewCaptureUtils} from 'viewers/common/view_capture_utils';
import {UiRect} from 'viewers/components/rects/types2d';
import {UiData} from './ui_data';

export class Presenter {
  private readonly traces: Traces;
  private readonly surfaceFlingerTrace: Trace<object> | undefined;
  private readonly viewCaptureTrace: Trace<object>;
  private viewCapturePackageNames: string[] = [];

  private selectedFrameData: FrameData | undefined;
  private previousFrameData: FrameData | undefined;
  private selectedHierarchyTree: HierarchyTreeNode | undefined;

  private uiData: UiData | undefined;

  private pinnedItems: HierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];

  private highlightedItem: string = '';

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
    traceType: TraceType,
    traces: Traces,
    private readonly storage: Storage,
    private readonly notifyUiDataCallback: (data: UiData) => void
  ) {
    this.traces = traces;
    this.viewCaptureTrace = assertDefined(traces.getTrace(traceType));
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(WinscopeEventType.TRACE_POSITION_UPDATE, async (event) => {
      await this.initializeIfNeeded();

      const vcEntry = TraceEntryFinder.findCorrespondingEntry(
        this.viewCaptureTrace,
        event.position
      );
      let prevVcEntry: typeof vcEntry;
      if (vcEntry && vcEntry.getIndex() > 0) {
        prevVcEntry = this.viewCaptureTrace.getEntry(vcEntry.getIndex() - 1);
      }

      this.selectedFrameData = await vcEntry?.getValue();
      this.previousFrameData = await prevVcEntry?.getValue();

      if (this.uiData && this.surfaceFlingerTrace) {
        const surfaceFlingerEntry = await TraceEntryFinder.findCorrespondingEntry(
          this.surfaceFlingerTrace,
          event.position
        )?.getValue();
        if (surfaceFlingerEntry) {
          this.uiData.sfRects = SurfaceFlingerUtils.makeRects(
            surfaceFlingerEntry,
            this.viewCapturePackageNames,
            this.hierarchyUserOptions
          );
        }
      }
      this.refreshUI();
    });
  }

  private async initializeIfNeeded() {
    this.viewCapturePackageNames = await ViewCaptureUtils.getPackageNames(this.traces);
  }

  private refreshUI() {
    // this.pinnedItems is updated in generateTree, so don't inline
    const tree = this.generateTree();
    if (!this.selectedHierarchyTree && tree) {
      this.selectedHierarchyTree = tree;
    }
    let selectedViewNode: any | null = null;
    if (this.selectedHierarchyTree?.name && this.selectedFrameData?.node) {
      selectedViewNode = this.findViewNode(
        this.selectedHierarchyTree.name,
        this.selectedFrameData.node
      );
    }

    this.uiData = new UiData(
      this.generateViewCaptureUiRects(),
      this.uiData?.sfRects,
      tree,
      this.hierarchyUserOptions,
      this.propertiesUserOptions,
      this.pinnedItems,
      this.highlightedItem,
      this.getTreeWithTransformedProperties(this.selectedHierarchyTree),
      selectedViewNode
    );

    this.notifyUiDataCallback(this.uiData);
  }

  private generateViewCaptureUiRects(): UiRect[] {
    const rectangles: UiRect[] = [];

    function inner(node: any /* ViewNode */) {
      const aUiRect: UiRect = {
        x: node.boxPos.left,
        y: node.boxPos.top,
        w: node.boxPos.width,
        h: node.boxPos.height,
        label: '',
        transform: undefined,
        isVisible: node.isVisible,
        isDisplay: false,
        id: node.id,
        displayId: 0,
        isVirtual: false,
        isClickable: true,
        cornerRadius: 0,
        depth: node.depth,
        hasContent: node.isVisible,
      };
      rectangles.push(aUiRect);
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

  updateHighlightedItem(id: string) {
    if (this.highlightedItem === id) {
      this.highlightedItem = '';
    } else {
      this.highlightedItem = id;
    }
    this.uiData!!.highlightedItem = this.highlightedItem;
    this.copyUiDataAndNotifyView();
  }

  updateHierarchyTree(userOptions: UserOptions) {
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
    this.uiData!!.selectedViewNode = this.findViewNode(
      selectedItem.name,
      this.selectedFrameData.node
    );
    this.updateSelectedTreeUiData();
  }

  private updateSelectedTreeUiData() {
    this.uiData!!.propertiesTree = this.getTreeWithTransformedProperties(
      this.selectedHierarchyTree
    );
    this.copyUiDataAndNotifyView();
  }

  private findViewNode(name: string, root: ViewNode): ViewNode | null {
    if (name === root.name) {
      return root;
    } else {
      for (let i = 0; i < root.children.length; i++) {
        const subTreeSearch = this.findViewNode(name, root.children[i]);
        if (subTreeSearch != null) {
          return subTreeSearch;
        }
      }
    }
    return null;
  }

  private getTreeWithTransformedProperties(
    selectedTree: HierarchyTreeNode | undefined
  ): PropertiesTreeNode | null {
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
