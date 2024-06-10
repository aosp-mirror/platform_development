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

import {assertDefined, assertTrue} from 'common/assert_utils';
import {FunctionUtils} from 'common/function_utils';
import {PersistentStoreProxy} from 'common/persistent_store_proxy';
import {
  TabbedViewSwitchRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {CustomQueryType} from 'trace/custom_query';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceEntryFinder} from 'trace/trace_entry_finder';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {IsModifiedCallbackType} from 'viewers/common/add_diffs';
import {AddDiffsHierarchyTree} from 'viewers/common/add_diffs_hierarchy_tree';
import {AddDiffsPropertiesTree} from 'viewers/common/add_diffs_properties_tree';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {VcCuratedProperties} from 'viewers/common/curated_properties';
import {DisplayIdentifier} from 'viewers/common/display_identifier';
import {AddChips} from 'viewers/common/operations/add_chips';
import {Filter} from 'viewers/common/operations/filter';
import {RectFilter} from 'viewers/common/rect_filter';
import {RectShowState} from 'viewers/common/rect_show_state';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {UI_RECT_FACTORY} from 'viewers/common/ui_rect_factory';
import {UiTreeFormatter} from 'viewers/common/ui_tree_formatter';
import {TreeNodeFilter, UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {UiRect} from 'viewers/components/rects/types2d';
import {SimplifyNamesVc} from './operations/simplify_names';
import {UiData} from './ui_data';

export class Presenter implements WinscopeEventEmitter {
  private static readonly DENYLIST_PROPERTY_NAMES = [
    'children',
    'isComputedVisible',
  ];

  private emitWinscopeEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private readonly traces: Traces;
  private readonly surfaceFlingerTrace: Trace<HierarchyTreeNode> | undefined;
  private readonly viewCaptureTraces: Array<Trace<HierarchyTreeNode>>;

  private viewCapturePackageNames: string[] = [];
  private previousFrameData:
    | Map<Trace<HierarchyTreeNode>, TraceEntry<HierarchyTreeNode>>
    | undefined;
  private selectedHierarchyTree:
    | [Trace<HierarchyTreeNode>, HierarchyTreeNode]
    | undefined;
  private currentHierarchyTrees:
    | Map<Trace<HierarchyTreeNode>, HierarchyTreeNode>
    | undefined;
  private previousHierarchyTrees:
    | Map<Trace<HierarchyTreeNode>, HierarchyTreeNode>
    | undefined;
  private uiData: UiData | undefined;
  private pinnedItems: UiHierarchyTreeNode[] = [];
  private pinnedIds: string[] = [];
  private highlightedItem: string = '';
  private windows: DisplayIdentifier[] = [];
  private allCurrentVcRects: UiRect[] = [];
  private rectFilter = new RectFilter();

  private hierarchyFilter: TreeNodeFilter = UiTreeUtils.makeIdFilter('');
  private propertiesFilter: TreeNodeFilter = UiTreeUtils.makePropertyFilter('');

  private rectsUserOptions: UserOptions = PersistentStoreProxy.new<UserOptions>(
    'SfRectsOptions',
    {
      ignoreNonHidden: {
        name: 'Ignore',
        icon: 'visibility',
        enabled: false,
      },
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
        enabled: false,
      },
    },
    this.storage,
  );
  private hierarchyUserOptions: UserOptions =
    PersistentStoreProxy.new<UserOptions>(
      'SfHierarchyOptions',
      {
        showDiff: {
          name: 'Show diff', // TODO: PersistentStoreObject.Ignored("Show diff") or something like that to instruct to not store this info
          enabled: false,
        },
        showOnlyVisible: {
          name: 'Show only',
          chip: VISIBLE_CHIP,
          enabled: false,
        },
        simplifyNames: {
          name: 'Simplify names',
          enabled: true,
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
    traces: Traces,
    private readonly storage: Storage,
    private readonly notifyUiDataCallback: (data: UiData) => void,
  ) {
    this.traces = traces;
    this.viewCaptureTraces = traces.getTraces(TraceType.VIEW_CAPTURE);
    this.surfaceFlingerTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
  }

  setEmitEvent(callback: EmitEvent) {
    this.emitWinscopeEvent = callback;
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        await this.initializeIfNeeded();

        const currHierarchyTrees = new Map<
          Trace<HierarchyTreeNode>,
          HierarchyTreeNode
        >();
        const prevEntries = new Map<
          Trace<HierarchyTreeNode>,
          TraceEntry<HierarchyTreeNode>
        >();

        for (const trace of this.viewCaptureTraces) {
          const entry = TraceEntryFinder.findCorrespondingEntry(
            trace,
            event.position,
          );
          const tree = await entry?.getValue();
          if (tree) currHierarchyTrees.set(trace, tree);

          if (entry && entry.getIndex() > 0) {
            prevEntries.set(trace, trace.getEntry(entry.getIndex() - 1));
          }
        }

        this.currentHierarchyTrees =
          currHierarchyTrees.size > 0 ? currHierarchyTrees : undefined;
        this.previousFrameData = prevEntries.size > 0 ? prevEntries : undefined;
        this.previousHierarchyTrees =
          prevEntries.size > 0
            ? new Map<Trace<HierarchyTreeNode>, HierarchyTreeNode>()
            : undefined;

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
    await this.initializePackageNamesIfNeeded();
    await this.initializeWindowsIfNeeded();
  }

  private async initializePackageNamesIfNeeded() {
    if (this.viewCapturePackageNames.length > 0) {
      return;
    }

    const promisesPackageName = this.viewCaptureTraces.map(async (trace) => {
      const packageAndWindow = await trace.customQuery(
        CustomQueryType.VIEW_CAPTURE_METADATA,
      );
      return packageAndWindow.packageName;
    });

    this.viewCapturePackageNames = await Promise.all(promisesPackageName);
  }

  private async initializeWindowsIfNeeded() {
    if (this.windows.length > 0) {
      return;
    }

    const shortenAndCapitalizeWindowName = (name: string) => {
      const lastDot = name.lastIndexOf('.');
      if (lastDot !== -1) {
        name = name.substring(lastDot + 1);
      }
      if (name.length > 0) {
        name = name[0].toUpperCase() + name.slice(1);
      }
      return name;
    };

    const promisesWindowName = this.viewCaptureTraces.map(async (trace) => {
      const packageAndWindow = await trace.customQuery(
        CustomQueryType.VIEW_CAPTURE_METADATA,
      );
      return shortenAndCapitalizeWindowName(packageAndWindow.windowName);
    });
    const windowNames = await Promise.all(promisesWindowName);

    this.windows = this.viewCaptureTraces
      .map((trace, i) => {
        const traceId = this.getIdFromViewCaptureTrace(trace);
        return {
          displayId: traceId,
          groupId: traceId,
          name: windowNames[i],
        };
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  private async refreshUI() {
    let trees: UiHierarchyTreeNode[] | undefined;
    this.allCurrentVcRects = [];
    let vcRectsToDraw: UiRect[] = [];
    let vcRectIdToShowState: Map<string, RectShowState> | undefined;

    if (this.currentHierarchyTrees) {
      for (const [
        trace,
        hierarchyTree,
      ] of this.currentHierarchyTrees.entries()) {
        const groupId = this.getIdFromViewCaptureTrace(trace);
        this.allCurrentVcRects.push(
          ...UI_RECT_FACTORY.makeVcUiRects(hierarchyTree, groupId),
        );
      }
      vcRectsToDraw = this.filterRects(this.allCurrentVcRects);
      vcRectIdToShowState = this.rectFilter.getRectIdToShowState(
        this.allCurrentVcRects,
        vcRectsToDraw,
      );

      this.pinnedItems = [];
      trees = assertDefined(
        await this.formatHierarchyTreesAndUpdatePinnedItems(
          this.currentHierarchyTrees,
          vcRectsToDraw,
        ),
      );

      if (!this.highlightedItem) {
        this.selectedHierarchyTree = [
          Array.from(this.currentHierarchyTrees.keys())[0],
          trees[0],
        ];
      } else {
        for (const [trace, tree] of this.currentHierarchyTrees) {
          const highlightedNode = tree.findDfs((node) =>
            UiTreeUtils.isHighlighted(node, this.highlightedItem),
          );
          if (highlightedNode) {
            this.selectedHierarchyTree = [trace, highlightedNode];
            break;
          }
        }
      }
    }

    let formattedPropertiesTree: UiPropertyTreeNode | undefined;
    let curatedProperties: VcCuratedProperties | undefined;

    if (this.selectedHierarchyTree) {
      const propertiesTree =
        await this.selectedHierarchyTree[1].getAllProperties();
      curatedProperties = this.getCuratedProperties(propertiesTree);
      formattedPropertiesTree = await this.formatPropertiesTree(propertiesTree);
    }

    this.uiData = new UiData(
      vcRectsToDraw,
      vcRectIdToShowState,
      this.windows,
      this.uiData?.sfRects,
      trees,
      this.rectsUserOptions,
      this.hierarchyUserOptions,
      this.propertiesUserOptions,
      this.pinnedItems,
      this.highlightedItem,
      formattedPropertiesTree,
      curatedProperties,
    );

    this.copyUiDataAndNotifyView();
  }

  private filterRects(rects: UiRect[]): UiRect[] {
    const isOnlyVisibleMode =
      this.rectsUserOptions['showOnlyVisible']?.enabled ?? false;
    const isIgnoreNonHiddenMode =
      this.rectsUserOptions['ignoreNonHidden']?.enabled ?? false;
    return this.rectFilter.filterRects(
      rects,
      isOnlyVisibleMode,
      isIgnoreNonHiddenMode,
    );
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

  private async formatHierarchyTreesAndUpdatePinnedItems(
    hierarchyTrees:
      | Map<Trace<HierarchyTreeNode>, HierarchyTreeNode>
      | undefined,
    rectsToDraw: UiRect[],
  ): Promise<UiHierarchyTreeNode[] | undefined> {
    if (!hierarchyTrees) return undefined;

    const formattedTrees = [];
    for (const [trace, hierarchyTree] of hierarchyTrees.entries()) {
      const uiTree = UiHierarchyTreeNode.from(hierarchyTree);
      uiTree.forEachNodeDfs((node) => node.setShowHeading(false));

      const formatter = new UiTreeFormatter<UiHierarchyTreeNode>().setUiTree(
        uiTree,
      );

      if (
        this.hierarchyUserOptions['showDiff']?.enabled &&
        !this.hierarchyUserOptions['showDiff']?.isUnavailable
      ) {
        let prevTree = this.previousHierarchyTrees?.get(trace);
        if (this.previousHierarchyTrees && !prevTree) {
          prevTree = await this.previousFrameData?.get(trace)?.getValue();
          if (prevTree) this.previousHierarchyTrees.set(trace, prevTree);
        }
        const prevEntryUiTree = prevTree
          ? UiHierarchyTreeNode.from(prevTree)
          : undefined;
        await new AddDiffsHierarchyTree(
          this.isHierarchyTreeModified,
        ).executeInPlace(uiTree, prevEntryUiTree);
      }

      const predicates = [this.hierarchyFilter];
      if (this.hierarchyUserOptions['showOnlyVisible']?.enabled) {
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
      formattedTrees.push(formattedTree);
    }
    return formattedTrees;
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

  async onHighlightedNodeChange(item: UiHierarchyTreeNode) {
    this.updateHighlightedItem(item.id);
    if (!this.currentHierarchyTrees) {
      return;
    }
    if (UiTreeUtils.shouldGetProperties(item)) {
      const idMatchFilter = UiTreeUtils.makeIdMatchFilter(item.id);
      for (const [type, trace] of this.currentHierarchyTrees) {
        const tree = trace.findDfs(idMatchFilter);
        if (tree) {
          this.selectedHierarchyTree = [type, item];
          break;
        }
      }
    }
    await this.updateSelectedTreeUiData();
  }

  async onHighlightedIdChange(newId: string) {
    this.updateHighlightedItem(newId);
    if (!this.currentHierarchyTrees) {
      return;
    }
    const idMatchFilter = UiTreeUtils.makeIdMatchFilter(newId);
    for (const [type, trace] of this.currentHierarchyTrees) {
      const tree = trace.findDfs(idMatchFilter);
      if (tree) {
        this.selectedHierarchyTree = [type, tree];
        break;
      }
    }
    await this.updateSelectedTreeUiData();
  }

  onRectsUserOptionsChange(userOptions: UserOptions) {
    const uiData = assertDefined(this.uiData);
    this.rectsUserOptions = userOptions;
    uiData.rectsUserOptions = this.rectsUserOptions;
    this.updateRectUiData();
    this.copyUiDataAndNotifyView();
  }

  async onHierarchyUserOptionsChange(userOptions: UserOptions) {
    const uiData = assertDefined(this.uiData);
    this.hierarchyUserOptions = userOptions;
    uiData.hierarchyUserOptions = this.hierarchyUserOptions;
    uiData.trees = await this.formatHierarchyTreesAndUpdatePinnedItems(
      this.currentHierarchyTrees,
      uiData.vcRectsToDraw,
    );
    this.copyUiDataAndNotifyView();
  }

  async onHierarchyFilterChange(filterString: string) {
    const uiData = assertDefined(this.uiData);
    this.hierarchyFilter = UiTreeUtils.makeIdFilter(filterString);
    uiData.trees = await this.formatHierarchyTreesAndUpdatePinnedItems(
      this.currentHierarchyTrees,
      uiData.vcRectsToDraw,
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

  async onMiniRectsDoubleClick() {
    if (!this.surfaceFlingerTrace) {
      return;
    }
    await this.emitWinscopeEvent(
      new TabbedViewSwitchRequest(this.surfaceFlingerTrace),
    );
  }

  getTraces(): Array<Trace<HierarchyTreeNode>> {
    return this.viewCaptureTraces;
  }

  getViewCaptureTraceFromId(id: number): Trace<HierarchyTreeNode> {
    return assertDefined(this.viewCaptureTraces[id]);
  }

  async onRectShowStateChange(id: string, newShowState: RectShowState) {
    this.rectFilter.updateRectShowState(id, newShowState);
    this.updateRectUiData();
    this.copyUiDataAndNotifyView();
  }

  private updateRectUiData() {
    const uiData = assertDefined(this.uiData);
    uiData.vcRectsToDraw = this.filterRects(this.allCurrentVcRects);
    uiData.vcRectIdToShowState = this.rectFilter.getRectIdToShowState(
      this.allCurrentVcRects,
      uiData.vcRectsToDraw,
    );
  }

  private getIdFromViewCaptureTrace(trace: Trace<HierarchyTreeNode>): number {
    const index = this.viewCaptureTraces.indexOf(trace);
    assertTrue(index !== -1);
    return index;
  }

  private updateHighlightedItem(id: string) {
    if (this.highlightedItem === id) {
      this.highlightedItem = '';
    } else {
      this.highlightedItem = id;
    }
    assertDefined(this.uiData).highlightedItem = this.highlightedItem;
  }

  private async updateSelectedTreeUiData() {
    if (this.selectedHierarchyTree) {
      const propertiesTree =
        await this.selectedHierarchyTree[1].getAllProperties();
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
      this.selectedHierarchyTree &&
      this.propertiesUserOptions['showDiff']?.enabled &&
      !this.propertiesUserOptions['showDiff']?.isUnavailable
    ) {
      const type = this.selectedHierarchyTree[0];
      let prevTree = this.previousHierarchyTrees?.get(type);
      if (this.previousHierarchyTrees && !prevTree) {
        prevTree = await this.previousFrameData?.get(type)?.getValue();
        if (prevTree) this.previousHierarchyTrees.set(type, prevTree);
      }
      const prevEntryNode = prevTree?.findDfs(
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
      UiTreeUtils.makeDenyListFilterByName(Presenter.DENYLIST_PROPERTY_NAMES),
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
