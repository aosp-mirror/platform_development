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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANYf KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {Rect} from 'common/geometry/rect';
import {InMemoryStorage} from 'common/in_memory_storage';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {NotifyHierarchyViewCallbackType} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {AbstractHierarchyViewerPresenterTest} from 'viewers/common/abstract_hierarchy_viewer_presenter_test';
import {DiffType} from 'viewers/common/diff_type';
import {TextFilter} from 'viewers/common/text_filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

class PresenterWindowManagerTest extends AbstractHierarchyViewerPresenterTest<UiData> {
  private trace: Trace<HierarchyTreeNode> | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;
  private selectedTree: UiHierarchyTreeNode | undefined;
  private selectedTreeAfterPositionUpdate: UiHierarchyTreeNode | undefined;

  override readonly shouldExecuteFlatTreeTest = true;
  override readonly shouldExecuteRectTests = true;
  override readonly shouldExecuteShowDiffTests = true;
  override readonly shouldExecuteDumpTests = true;
  override readonly shouldExecuteSimplifyNamesTest = true;

  override readonly numberOfDefaultProperties = 29;
  override readonly numberOfNonDefaultProperties = 21;
  override readonly expectedFirstRect = new Rect(0, 0, 1080, 2400);
  override readonly propertiesFilter = new TextFilter('requested', []);
  override readonly expectedTotalRects = 12;
  override readonly expectedVisibleRects = 7;
  override readonly treeNodeLongName =
    'f7092ed com.google.android.apps.nexuslauncher/.NexusLauncherActivity';
  override readonly treeNodeShortName =
    'com.google.(...).NexusLauncherActivity';
  override readonly numberOfFilteredProperties = 2;
  override readonly hierarchyFilter = new TextFilter('ScreenDecor', []);
  override readonly expectedHierarchyChildrenAfterStringFilter = 2;
  override readonly propertyWithDiff = 'animator';
  override readonly expectedPropertyDiffType = DiffType.ADDED;

  private readonly numberOfFlattenedChildren = 68;
  private readonly numberOfVisibleChildren = 6;
  private readonly numberOfNestedChildren = 1;

  override async setUpTestEnvironment(): Promise<void> {
    this.trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.WINDOW_MANAGER)
      .setEntries([
        await UnitTestUtils.getWindowManagerState(0),
        await UnitTestUtils.getWindowManagerState(1),
      ])
      .build();

    const firstEntry = this.trace.getEntry(0);
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);
    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      this.trace.getEntry(1),
    );

    const firstEntryDataTree = await firstEntry.getValue();
    this.selectedTree = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs(UiTreeUtils.makeNodeFilter('93d3f3c')),
      ),
    );
    this.selectedTreeAfterPositionUpdate = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs(UiTreeUtils.makeNodeFilter('f7092ed')),
      ),
    );
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType<UiData>,
  ): Presenter {
    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.WINDOW_MANAGER)
      .setEntries([])
      .build();
    const traces = new Traces();
    traces.addTrace(trace);
    return new Presenter(trace, traces, new InMemoryStorage(), callback);
  }

  override createPresenter(
    callback: NotifyHierarchyViewCallbackType<UiData>,
  ): Presenter {
    const traces = new Traces();
    const trace = assertDefined(this.trace);
    traces.addTrace(trace);
    return new Presenter(trace, traces, new InMemoryStorage(), callback);
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
  }

  override getShowDiffPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getExpectedChildrenBeforeVisibilityFilter(): number {
    return this.numberOfFlattenedChildren;
  }

  override getExpectedChildrenAfterVisibilityFilter(): number {
    return this.numberOfVisibleChildren;
  }

  override getExpectedChildrenBeforeFlatFilter(): number {
    return this.numberOfNestedChildren;
  }

  override getExpectedChildrenAfterFlatFilter(): number {
    return this.numberOfFlattenedChildren;
  }

  override getExpectedHierarchyChildrenBeforeStringFilter(): number {
    return this.numberOfFlattenedChildren;
  }

  override getSelectedTree(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override getSelectedTreeAfterPositionUpdate(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTreeAfterPositionUpdate);
  }

  override executeChecksForPropertiesTreeAfterPositionUpdate(uiData: UiData) {
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(propertiesTree.getChildByName('state')).formattedValue(),
    ).toEqual('STOPPED');
    expect(uiData.displays).toEqual([
      {
        displayId: 'DisplayContent 1f3454e Built-in Screen',
        groupId: 0,
        name: 'Built-in Screen',
        isActive: true,
      },
    ]);
  }

  override executeChecksForPropertiesTreeAfterSecondPositionUpdate(
    uiData: UiData,
  ) {
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(propertiesTree.getChildByName('state')).formattedValue(),
    ).toEqual('RESUMED');
  }
}

describe('PresenterWindowManager', () => {
  new PresenterWindowManagerTest().execute();
});
