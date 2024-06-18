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
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType, ViewCaptureTraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {IsModifiedCallbackType} from 'viewers/common/add_diffs';
import {AddDiffsHierarchyTree} from 'viewers/common/add_diffs_hierarchy_tree';
import {AddDiffsPropertiesTree} from 'viewers/common/add_diffs_properties_tree';
import {VcCuratedProperties} from 'viewers/common/curated_properties';
import {DiffType} from 'viewers/common/diff_type';
import {AddChips} from 'viewers/common/operations/add_chips';
import {Filter} from 'viewers/common/operations/filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UI_RECT_FACTORY} from 'viewers/common/ui_rect_factory';
import {UiTreeFormatter} from 'viewers/common/ui_tree_formatter';
import {TreeNodeFilter, UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {ViewCaptureUtils} from 'viewers/common/view_capture_utils';
import {UiRect} from 'viewers/components/rects/types2d';
import {SimplifyNamesVc} from './operations/simplify_names';
import {UiData} from './ui_data';

export class Presenter {
  private static readonly DENYLIST_PROPERTY_NAMES = [
    'children',
    'isComputedVisible',
  ];

  private readonly traces: Traces;
  private readonly surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  private readonly viewCaptureTrace: Trace<HierarchyTreeNode>;
  private viewCapturePackageNames: string[] = [];

  private previousFrameData: TraceEntry<HierarchyTreeNode> | undefined;
  private selectedHierarchyTree: UiHierarchyTreeNode | undefined;
  private currentHierarchyTree: HierarchyTreeNode | undefined;
  private previousHierarchyTree: HierarchyTreeNode | undefined;

  private uiData: UiData | undefined;

  private pinnedItems: UiHierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];

  private highlightedItem: string = '';

  private hierarchyFilter: TreeNodeFilter = UiTreeUtils.makeIdFilter('');
  private propertiesFilter: TreeNodeFilter = UiTreeUtils.makePropertyFilter('');

  private hierarchyUserOptions: UserOptions =
    PersistentStoreProxy.new<UserOptions>(
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
      this.storage,
    );

  private propertiesUserOptions: UserOptions =
    PersistentStoreProxy.new<UserOptions>(
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
      this.storage,
    );

  constructor(
    traceType: ViewCaptureTraceType,
    traces: Traces,
    private readonly storage: Storage,
    private readonly notifyUiDataCallback: (data: UiData) => void,
  ) {
    this.traces = traces;
    this.viewCaptureTrace = assertDefined(traces.getTrace(traceType));
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        await this.initializeIfNeeded();

        const vcEntry = TraceEntryFinder.findCorrespondingEntry(
          this.viewCaptureTrace,
          event.position,
        );
        this.currentHierarchyTree = await vcEntry?.getValue();

        this.previousFrameData = undefined;
        if (vcEntry && vcEntry.getIndex() > 0) {
          this.previousFrameData = this.viewCaptureTrace.getEntry(
            vcEntry.getIndex() - 1,
          );
        }
        this.previousHierarchyTree = undefined;

        if (this.uiData && this.surfaceFlingerTrace) {
          const surfaceFlingerEntry =
            (await TraceEntryFinder.findCorrespondingEntry(
              this.surfaceFlingerTrace,
              event.position,
            )?.getValue()) as HierarchyTreeNode;
          if (surfaceFlingerEntry) {
            this.uiData.sfRects = UI_RECT_FACTORY.makeUiRects(
              surfaceFlingerEntry,
              this.viewCapturePackageNames,
            );
          }
        }
        await this.refreshUI();
      },
    );
  }

  private async initializeIfNeeded() {
    this.viewCapturePackageNames = await ViewCaptureUtils.getPackageNames(
      this.traces,
    );
  }

  private async refreshUI() {
    let tree: UiHierarchyTreeNode | undefined;
    let vcRects: UiRect[] = [];
    if (this.currentHierarchyTree) {
      vcRects = UI_RECT_FACTORY.makeVcUiRects(this.currentHierarchyTree);
      this.pinnedItems = [];
      tree = await this.formatHierarchyTreeAndUpdatePinnedItems(
        this.currentHierarchyTree,
      );

      if (!this.selectedHierarchyTree) {
        this.selectedHierarchyTree = tree;
      }
    }

    let formattedPropertiesTree: UiPropertyTreeNode | undefined;
    let curatedProperties: VcCuratedProperties | undefined;
    if (this.selectedHierarchyTree) {
      const propertiesTree =
        await this.selectedHierarchyTree.getAllProperties();
      curatedProperties = this.getCuratedProperties(propertiesTree);
      formattedPropertiesTree = await this.formatPropertiesTree(propertiesTree);
    }

    this.uiData = new UiData(
      vcRects,
      this.uiData?.sfRects,
      tree,
      this.hierarchyUserOptions,
      this.propertiesUserOptions,
      this.pinnedItems,
      this.highlightedItem,
      formattedPropertiesTree,
      curatedProperties,
    );

    this.notifyUiDataCallback(this.uiData);
  }

  private getCuratedProperties(tree: PropertyTreeNode): VcCuratedProperties {
    const curated: VcCuratedProperties = {
      className: tree.name,
      hashcode: assertDefined(tree.getChildByName('hashcode')).formattedValue(),
      left: assertDefined(tree.getChildByName('left')).formattedValue(),
      top: assertDefined(tree.getChildByName('top')).formattedValue(),
      elevation: assertDefined(
        tree.getChildByName('elevation'),
      ).formattedValue(),
      height: assertDefined(tree.getChildByName('height')).formattedValue(),
      width: assertDefined(tree.getChildByName('width')).formattedValue(),
      translationX: assertDefined(
        tree.getChildByName('translationX'),
      ).formattedValue(),
      translationY: assertDefined(
        tree.getChildByName('translationY'),
      ).formattedValue(),
      scrollX: assertDefined(tree.getChildByName('scrollX')).formattedValue(),
      scrollY: assertDefined(tree.getChildByName('scrollY')).formattedValue(),
      scaleX: assertDefined(tree.getChildByName('scaleX')).formattedValue(),
      scaleY: assertDefined(tree.getChildByName('scaleY')).formattedValue(),
      visibility: assertDefined(
        tree.getChildByName('visibility'),
      ).formattedValue(),
      alpha: assertDefined(tree.getChildByName('alpha')).formattedValue(),
      willNotDraw: assertDefined(
        tree.getChildByName('willNotDraw'),
      ).formattedValue(),
      clipChildren: assertDefined(
        tree.getChildByName('clipChildren'),
      ).formattedValue(),
    };
    return curated;
  }

  private async formatHierarchyTreeAndUpdatePinnedItems(
    hierarchyTree: HierarchyTreeNode | undefined,
  ): Promise<UiHierarchyTreeNode | undefined> {
    if (!hierarchyTree) return undefined;

    const uiTree = UiHierarchyTreeNode.from(hierarchyTree);
    uiTree.forEachNodeDfs((node) => node.setShowHeading(false));

    const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(
      uiTree,
    );

    if (
      this.hierarchyUserOptions['showDiff']?.enabled &&
      !this.hierarchyUserOptions['showDiff']?.isUnavailable
    ) {
      if (this.previousFrameData && !this.previousHierarchyTree) {
        this.previousHierarchyTree = await this.previousFrameData.getValue();
      }
      const prevEntryUiTree = this.previousHierarchyTree
        ? UiHierarchyTreeNode.from(this.previousHierarchyTree)
        : undefined;
      await new AddDiffsHierarchyTree(
        this.isHierarchyTreeModified,
      ).executeInPlace(uiTree, prevEntryUiTree);
    }

    const predicates = [this.hierarchyFilter];
    if (this.hierarchyUserOptions['onlyVisible']?.enabled) {
      predicates.push(UiTreeUtils.isVisible);
    }

    formatter
      .addOperation(new Filter(predicates, true))
      .addOperation(new AddChips());

    if (this.hierarchyUserOptions['simplifyNames']?.enabled) {
      formatter.addOperation(new SimplifyNamesVc());
    }

    const formattedTree = formatter.format();
    this.pinnedItems.push(...this.getPinnedItems(formattedTree));
    return formattedTree;
  }

  private getPinnedItems(tree: UiHierarchyTreeNode): UiHierarchyTreeNode[] {
    const pinnedNodes = [];

    if (this.pinnedIds.includes(tree.id)) {
      pinnedNodes.push(tree);
    }

    for (const child of tree.getAllChildren()) {
      pinnedNodes.push(...this.getPinnedItems(child));
    }

    return pinnedNodes;
  }

  onPinnedItemChange(pinnedItem: UiHierarchyTreeNode) {
    const pinnedId = pinnedItem.id;
    if (this.pinnedItems.map((item) => item.id).includes(pinnedId)) {
      this.pinnedItems = this.pinnedItems.filter(
        (pinned) => pinned.id !== pinnedId,
      );
    } else {
      this.pinnedItems.push(pinnedItem);
    }
    this.updatePinnedIds(pinnedId);
    assertDefined(this.uiData).pinnedItems = this.pinnedItems;
    this.copyUiDataAndNotifyView();
  }

  private updatePinnedIds(newId: string) {
    if (this.pinnedIds.includes(newId)) {
      this.pinnedIds = this.pinnedIds.filter((pinned) => pinned !== newId);
    } else {
      this.pinnedIds.push(newId);
    }
  }

  onHighlightedItemChange(id: string) {
    if (this.highlightedItem === id) {
      this.highlightedItem = '';
    } else {
      this.highlightedItem = id;
    }
    assertDefined(this.uiData).highlightedItem = this.highlightedItem;
    this.copyUiDataAndNotifyView();
  }

  async onHierarchyUserOptionsChange(userOptions: UserOptions) {
    this.hierarchyUserOptions = userOptions;
    assertDefined(this.uiData).hierarchyUserOptions = this.hierarchyUserOptions;
    assertDefined(this.uiData).tree =
      await this.formatHierarchyTreeAndUpdatePinnedItems(
        this.currentHierarchyTree,
      );
    this.copyUiDataAndNotifyView();
  }

  async onHierarchyFilterChange(filterString: string) {
    this.hierarchyFilter = UiTreeUtils.makeIdFilter(filterString);
    assertDefined(this.uiData).tree =
      await this.formatHierarchyTreeAndUpdatePinnedItems(
        this.currentHierarchyTree,
      );
    this.copyUiDataAndNotifyView();
  }

  async onPropertiesUserOptionsChange(userOptions: UserOptions) {
    this.propertiesUserOptions = userOptions;
    assertDefined(this.uiData).propertiesUserOptions =
      this.propertiesUserOptions;
    await this.updateSelectedTreeUiData();
  }

  async onPropertiesFilterChange(filterString: string) {
    this.propertiesFilter = UiTreeUtils.makePropertyFilter(filterString);
    await this.updateSelectedTreeUiData();
  }

  async onSelectedHierarchyTreeChange(selectedTree: UiHierarchyTreeNode) {
    if (
      !selectedTree.isOldNode() ||
      selectedTree.getDiff() === DiffType.DELETED
    ) {
      this.selectedHierarchyTree = selectedTree;
      await this.updateSelectedTreeUiData();
    }
  }

  private async updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree) {
      const propertiesTree =
        await this.selectedHierarchyTree.getAllProperties();
      assertDefined(this.uiData).curatedProperties =
        this.getCuratedProperties(propertiesTree);
      assertDefined(this.uiData).propertiesTree =
        await this.formatPropertiesTree(propertiesTree);
    }
    this.copyUiDataAndNotifyView();
  }

  private async formatPropertiesTree(
    propertiesTree: PropertyTreeNode,
  ): Promise<UiPropertyTreeNode> {
    const uiTree = UiPropertyTreeNode.from(propertiesTree);

    if (
      this.propertiesUserOptions['showDiff']?.enabled &&
      !this.propertiesUserOptions['showDiff']?.isUnavailable
    ) {
      if (this.previousFrameData && !this.previousHierarchyTree) {
        this.previousHierarchyTree = await this.previousFrameData.getValue();
      }
      const prevEntryNode = this.previousHierarchyTree?.findDfs(
        UiTreeUtils.makeIdMatchFilter(propertiesTree.id),
      );
      const prevEntryUiTree = prevEntryNode
        ? UiPropertyTreeNode.from(await prevEntryNode.getAllProperties())
        : undefined;
      await new AddDiffsPropertiesTree(
        this.isPropertyNodeModified,
      ).executeInPlace(uiTree, prevEntryUiTree);
    }

    const predicatesKeepingChildren = [this.propertiesFilter];
    const predicatesDiscardingChildren = [
      UiTreeUtils.makeDenyListFilter(Presenter.DENYLIST_PROPERTY_NAMES),
    ];

    if (!this.propertiesUserOptions['showDefaults']?.enabled) {
      predicatesDiscardingChildren.push(UiTreeUtils.isNotDefault);
    }

    return new UiTreeFormatter<UiPropertyTreeNode>()
      .setUiTree(uiTree)
      .addOperation(new Filter(predicatesDiscardingChildren, false))
      .addOperation(new Filter(predicatesKeepingChildren, true))
      .format();
  }

  private isHierarchyTreeModified: IsModifiedCallbackType = async (
    newTree: TreeNode | undefined,
    oldTree: TreeNode | undefined,
  ) => {
    if (!newTree && !oldTree) return false;
    if (!newTree || !oldTree) return true;
    if ((newTree as UiHierarchyTreeNode).isRoot()) return false;
    const newProperties = await (
      newTree as UiHierarchyTreeNode
    ).getAllProperties();
    const oldProperties = await (
      oldTree as UiHierarchyTreeNode
    ).getAllProperties();

    return await this.isChildPropertyModified(newProperties, oldProperties);
  };

  private async isChildPropertyModified(
    newProperties: PropertyTreeNode,
    oldProperties: PropertyTreeNode,
  ): Promise<boolean> {
    for (const newProperty of newProperties.getAllChildren()) {
      if (Presenter.DENYLIST_PROPERTY_NAMES.includes(newProperty.name)) {
        continue;
      }

      const oldProperty = oldProperties.getChildByName(newProperty.name);
      if (!oldProperty) {
        return true;
      }

      if (newProperty.getAllChildren().length === 0) {
        if (await this.isPropertyNodeModified(newProperty, oldProperty)) {
          return true;
        }
      } else {
        const childrenModified = await this.isChildPropertyModified(
          newProperty,
          oldProperty,
        );
        if (childrenModified) return true;
      }
    }
    return false;
  }

  private isPropertyNodeModified: IsModifiedCallbackType = async (
    newTree: TreeNode | undefined,
    oldTree: TreeNode | undefined,
  ) => {
    if (!newTree && !oldTree) return false;
    if (!newTree || !oldTree) return true;

    const newValue = (newTree as UiPropertyTreeNode).formattedValue();
    const oldValue = (oldTree as UiPropertyTreeNode).formattedValue();
    return oldValue !== newValue;
  };

  private copyUiDataAndNotifyView() {
    // Create a shallow copy of the data, otherwise the Angular OnPush change detection strategy
    // won't detect the new input
    const copy = Object.assign({}, this.uiData);
    this.notifyUiDataCallback(copy);
  }
}
