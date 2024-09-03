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
import {Store} from 'common/store';
import {Timestamp} from 'common/time';
import {WinscopeEvent, WinscopeEventType} from 'messaging/winscope_event';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {ImeTraceType, TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TreeNode} from 'trace/tree_node/tree_node';
import {ImeAdditionalProperties} from 'viewers/common/ime_additional_properties';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {
  ImeLayers,
  ImeUtils,
  ProcessedWindowManagerState,
} from 'viewers/common/ime_utils';
import {TableProperties} from 'viewers/common/table_properties';
import {UserOptions} from 'viewers/common/user_options';
import {
  AbstractHierarchyViewerPresenter,
  NotifyHierarchyViewCallbackType,
} from './abstract_hierarchy_viewer_presenter';
import {VISIBLE_CHIP} from './chip';
import {HierarchyPresenter} from './hierarchy_presenter';
import {UpdateSfSubtreeDisplayNames} from './operations/update_sf_subtree_display_names';
import {PropertiesPresenter} from './properties_presenter';
import {TextFilter} from './text_filter';
import {UiHierarchyTreeNode} from './ui_hierarchy_tree_node';
import {UiTreeUtils} from './ui_tree_utils';

export abstract class AbstractPresenterInputMethod extends AbstractHierarchyViewerPresenter<ImeUiData> {
  protected getHierarchyTreeNameStrategy = (
    entry: TraceEntry<HierarchyTreeNode>,
    tree: HierarchyTreeNode,
  ) => {
    const where = tree.getEagerPropertyByName('where')?.formattedValue();
    return this.getEntryFormattedTimestamp(entry) + ' - ' + where;
  };
  protected override hierarchyPresenter = new HierarchyPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'ImeHierarchyOptions',
      {
        simplifyNames: {
          name: 'Simplify names',
          enabled: true,
        },
        showOnlyVisible: {
          name: 'Show only',
          chip: VISIBLE_CHIP,
          enabled: false,
        },
        flat: {
          name: 'Flat',
          enabled: false,
        },
      },
      this.storage,
    ),
    PersistentStoreProxy.new<TextFilter>(
      'ImeHierarchyFilter',
      new TextFilter('', []),
      this.storage,
    ),
    [],
    true,
    false,
    this.getHierarchyTreeNameStrategy,
    [[TraceType.SURFACE_FLINGER, [new UpdateSfSubtreeDisplayNames()]]],
  );
  protected override propertiesPresenter = new PropertiesPresenter(
    PersistentStoreProxy.new<UserOptions>(
      'ImePropertiesOptions',
      {
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
    ),
    PersistentStoreProxy.new<TextFilter>(
      'ImePropertiesFilter',
      new TextFilter('', []),
      this.storage,
    ),
    [],
  );
  protected override multiTraceType = undefined;

  protected readonly imeTrace: Trace<HierarchyTreeNode>;
  private readonly wmTrace?: Trace<HierarchyTreeNode>;
  private readonly sfTrace?: Trace<HierarchyTreeNode>;

  private hierarchyTableProperties: TableProperties | undefined;
  private additionalProperties: ImeAdditionalProperties | undefined;

  constructor(
    trace: Trace<HierarchyTreeNode>,
    traces: Traces,
    storage: Store,
    notifyViewCallback: NotifyHierarchyViewCallbackType<ImeUiData>,
  ) {
    super(
      trace,
      traces,
      storage,
      notifyViewCallback,
      new ImeUiData(trace.type as ImeTraceType),
    );
    this.imeTrace = trace;
    this.sfTrace = traces.getTrace(TraceType.SURFACE_FLINGER);
    this.wmTrace = traces.getTrace(TraceType.WINDOW_MANAGER);
  }

  async onAppEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.TRACE_POSITION_UPDATE,
      async (event) => {
        this.clearOverridePropertiesTreeSelection();
        await this.applyTracePositionUpdate(event);

        const imeEntry = this.hierarchyPresenter.getCurrentEntryForTrace(
          this.imeTrace,
        );
        const [sfEntry, wmEntry] = this.findSfWmTraceEntries(imeEntry);

        if (imeEntry) {
          this.additionalProperties = await this.getAdditionalProperties(
            await wmEntry?.getValue(),
            await sfEntry?.getValue(),
            wmEntry?.getTimestamp(),
            sfEntry?.getTimestamp(),
          );
          this.hierarchyTableProperties = this.getHierarchyTableProperties();

          await this.updateOverridePropertiesTree(this.additionalProperties);

          const highlightedItem = this.getHighlightedItem();
          const selected = this.hierarchyPresenter.getSelectedTree();

          if (!selected && highlightedItem !== undefined) {
            const isHighlightedFilter = (node: HierarchyTreeNode) =>
              UiTreeUtils.isHighlighted(node, highlightedItem);
            let selectedTree =
              this.additionalProperties?.sf?.taskLayerOfImeContainer?.findDfs(
                isHighlightedFilter,
              );
            if (!selectedTree) {
              selectedTree =
                this.additionalProperties?.sf?.taskLayerOfImeSnapshot?.findDfs(
                  isHighlightedFilter,
                );
            }

            if (selectedTree) {
              this.hierarchyPresenter.setSelectedTree([
                assertDefined(this.sfTrace),
                selectedTree,
              ]);
              await this.updatePropertiesTree();
            }
          }
        }
        this.refreshUIData();
      },
    );
  }

  async onHighlightedNodeChange(node: UiHierarchyTreeNode) {
    this.clearOverridePropertiesTreeSelection();
    await this.applyHighlightedNodeChange(node);
    this.refreshUIData();
  }

  async onHighlightedIdChange(newId: string) {
    const selectedHierarchyTree = this.hierarchyPresenter.getSelectedTree();
    if (!selectedHierarchyTree || selectedHierarchyTree[1].id !== newId) {
      this.clearOverridePropertiesTreeSelection();
    }
    await this.applyHighlightedIdChange(newId);
    this.refreshUIData();
  }

  async onAdditionalPropertySelected(selectedItem: {
    name: string;
    treeNode: TreeNode;
  }) {
    this.updateHighlightedItem(selectedItem.treeNode.id);
    if (selectedItem.treeNode instanceof HierarchyTreeNode) {
      this.clearOverridePropertiesTreeSelection();
      this.hierarchyPresenter.setSelectedTree([
        assertDefined(this.wmTrace),
        selectedItem.treeNode,
      ]);
    } else if (selectedItem.treeNode instanceof PropertyTreeNode) {
      this.hierarchyPresenter.setSelectedTree(undefined);
      this.overridePropertiesTree = selectedItem.treeNode;
    }

    this.overridePropertiesTreeName = selectedItem.name;
    await this.updatePropertiesTree();
    this.refreshUIData();
  }

  protected async getAdditionalProperties(
    wmEntry: HierarchyTreeNode | undefined,
    sfEntry: HierarchyTreeNode | undefined,
    wmEntryTimestamp: Timestamp | undefined,
    sfEntryTimestamp: Timestamp | undefined,
  ): Promise<ImeAdditionalProperties> {
    let wmProperties: ProcessedWindowManagerState | undefined;
    let sfProperties: ImeLayers | undefined;

    if (wmEntry) {
      wmProperties = ImeUtils.processWindowManagerTraceEntry(
        wmEntry,
        wmEntryTimestamp,
      );

      if (sfEntry) {
        sfProperties = ImeUtils.getImeLayers(
          sfEntry,
          wmProperties,
          sfEntryTimestamp,
        );

        if (sfProperties) {
          await this.makeSfSubtrees(sfProperties);
        }
      }
    }

    return new ImeAdditionalProperties(wmProperties, sfProperties);
  }

  protected override keepCalculated(tree: HierarchyTreeNode): boolean {
    return false;
  }

  protected override getOverrideDisplayName(
    selected: [Trace<HierarchyTreeNode>, HierarchyTreeNode],
  ): string | undefined {
    return this.overridePropertiesTreeName;
  }

  private async makeSfSubtrees(
    sfProperties: ImeLayers,
  ): Promise<UiHierarchyTreeNode[]> {
    const sfHierarchyTrees = [];
    const sfTrace = assertDefined(this.sfTrace);
    if (sfProperties.taskLayerOfImeContainer) {
      sfHierarchyTrees.push(sfProperties.taskLayerOfImeContainer);
    }
    if (sfProperties.taskLayerOfImeSnapshot) {
      sfHierarchyTrees.push(sfProperties.taskLayerOfImeSnapshot);
    }
    if (sfHierarchyTrees.length > 0) {
      await this.hierarchyPresenter.addCurrentHierarchyTrees(
        [sfTrace, sfHierarchyTrees],
        this.getHighlightedItem(),
      );
    }
    const sfSubtrees = assertDefined(
      this.hierarchyPresenter.getFormattedTreesByTrace(sfTrace),
    );
    sfSubtrees.forEach((subtree) =>
      subtree.setDisplayName('SfSubtree - ' + subtree.name),
    );
    return sfSubtrees;
  }

  private clearOverridePropertiesTreeSelection() {
    this.overridePropertiesTree = undefined;
    this.overridePropertiesTreeName = undefined;
  }

  private findSfWmTraceEntries(
    imeEntry: TraceEntry<HierarchyTreeNode> | undefined,
  ): [
    TraceEntry<HierarchyTreeNode> | undefined,
    TraceEntry<HierarchyTreeNode> | undefined,
  ] {
    if (!imeEntry || !this.imeTrace.hasFrameInfo()) {
      return [undefined, undefined];
    }

    const frames = imeEntry.getFramesRange();
    if (!frames || frames.start === frames.end) {
      return [undefined, undefined];
    }

    const frame = frames.start;
    const sfEntry = this.sfTrace
      ?.getFrame(frame)
      ?.findClosestEntry(imeEntry.getTimestamp());
    const wmEntry = this.wmTrace
      ?.getFrame(frame)
      ?.findClosestEntry(imeEntry.getTimestamp());

    return [sfEntry, wmEntry];
  }

  private async updateOverridePropertiesTree(
    additionalProperties: ImeAdditionalProperties,
  ) {
    const highlightedItem = this.getHighlightedItem();
    if (!highlightedItem) {
      this.clearOverridePropertiesTreeSelection();
      return;
    }
    if (highlightedItem.includes('WindowManagerState')) {
      this.overridePropertiesTree = undefined;
      const wmHierarchyTree = additionalProperties.wm?.hierarchyTree;
      this.hierarchyPresenter.setSelectedTree(
        wmHierarchyTree
          ? [assertDefined(this.wmTrace), wmHierarchyTree]
          : undefined,
      );
      this.overridePropertiesTreeName = wmHierarchyTree
        ? 'Window Manager State'
        : undefined;
    } else if (highlightedItem.includes('imeInsetsSourceProvider')) {
      this.hierarchyPresenter.setSelectedTree(undefined);
      const imeInsetsSourceProvider =
        additionalProperties.wm?.wmStateProperties?.imeInsetsSourceProvider;
      this.overridePropertiesTree = imeInsetsSourceProvider;
      this.overridePropertiesTreeName = imeInsetsSourceProvider
        ? 'Ime Insets Source Provider'
        : undefined;
    } else if (highlightedItem.includes('inputMethodControlTarget')) {
      this.hierarchyPresenter.setSelectedTree(undefined);
      const imeControlTarget =
        additionalProperties.wm?.wmStateProperties?.imeControlTarget;
      this.overridePropertiesTree = imeControlTarget;
      this.overridePropertiesTreeName = imeControlTarget
        ? 'Ime Control Target'
        : undefined;
    } else if (highlightedItem.includes('inputMethodInputTarget')) {
      this.hierarchyPresenter.setSelectedTree(undefined);
      const imeInputTarget =
        additionalProperties.wm?.wmStateProperties?.imeInputTarget;
      this.overridePropertiesTree = imeInputTarget;
      this.overridePropertiesTreeName = imeInputTarget
        ? 'Ime Input Target'
        : undefined;
    } else if (highlightedItem.includes('inputMethodTarget')) {
      this.hierarchyPresenter.setSelectedTree(undefined);
      const imeLayeringTarget =
        additionalProperties.wm?.wmStateProperties?.imeLayeringTarget;
      this.overridePropertiesTree = imeLayeringTarget;
      this.overridePropertiesTreeName = imeLayeringTarget
        ? 'Ime Layering Target'
        : undefined;
    }

    await this.updatePropertiesTree();
  }

  private refreshUIData() {
    this.uiData.hierarchyTableProperties = this.hierarchyTableProperties;
    this.uiData.additionalProperties = this.additionalProperties;
    this.refreshHierarchyViewerUiData();
  }

  protected abstract getHierarchyTableProperties(): TableProperties;
}
