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
import {InMemoryStorage} from 'common/in_memory_storage';
import {Rect} from 'common/rect';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {NotifyHierarchyViewCallbackType} from 'viewers/common/abstract_hierarchy_viewer_presenter';
import {AbstractHierarchyViewerPresenterTest} from 'viewers/common/abstract_hierarchy_viewer_presenter_test';
import {DiffType} from 'viewers/common/diff_type';
import {UiDataHierarchy} from 'viewers/common/ui_data_hierarchy';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {Presenter} from 'viewers/viewer_view_capture/presenter';
import {UiData} from 'viewers/viewer_view_capture/ui_data';

class PresenterViewCaptureTest extends AbstractHierarchyViewerPresenterTest {
  private traces: Traces | undefined;
  private positionUpdate: TracePositionUpdate | undefined;
  private secondPositionUpdate: TracePositionUpdate | undefined;
  private diffPositionUpdate: TracePositionUpdate | undefined;
  private selectedTree: UiHierarchyTreeNode | undefined;

  override readonly shouldExecuteFlatTreeTest = false;
  override readonly shouldExecuteRectTests = true;
  override readonly shouldExecuteShowDiffTests = true;
  override readonly shouldExecuteDumpTests = false;
  override readonly shouldExecuteSimplifyNamesTest = true;

  override readonly numberOfDefaultProperties = 3;
  override readonly numberOfNonDefaultProperties = 15;
  override readonly expectedFirstRect = new Rect(0, 0, 1080, 249);
  override readonly propertiesFilterString = 'alpha';
  override readonly expectedTotalRects = 13;
  override readonly expectedVisibleRects = 5;
  override readonly treeNodeLongName =
    'com.android.launcher3.taskbar.TaskbarView@80213537';
  override readonly treeNodeShortName = 'TaskbarView@80213537';
  override readonly numberOfFilteredProperties = 1;
  override readonly hierarchyFilterString = 'BubbleBarView';
  override readonly expectedHierarchyChildrenAfterStringFilter = 1;
  override readonly propertyWithDiff = 'translationY';
  override readonly expectedPropertyDiffType = DiffType.MODIFIED;

  override async setUpTestEnvironment(): Promise<void> {
    const parsers = (await UnitTestUtils.getParsers(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
    )) as Array<Parser<HierarchyTreeNode>>;

    this.traces = new Traces();
    for (const parser of parsers) {
      this.traces.addTrace(Trace.fromParser(parser));
    }

    const traceTaskbar = assertDefined(
      this.traces.getTraces(TraceType.VIEW_CAPTURE)[0],
    );
    const firstEntry = traceTaskbar.getEntry(0);
    this.positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    const traceLauncherActivity = assertDefined(
      this.traces.getTraces(TraceType.VIEW_CAPTURE)[1],
    );
    const firstEntryLauncherActivity = traceLauncherActivity.getEntry(0);
    this.secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      firstEntryLauncherActivity,
    );

    const firstEntryDataTree = await firstEntry.getValue();
    this.selectedTree = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs(
          UiTreeUtils.makeIdMatchFilter(
            'ViewNode com.android.launcher3.taskbar.TaskbarView@80213537',
          ),
        ),
      ),
    );

    this.diffPositionUpdate = TracePositionUpdate.fromTraceEntry(
      traceTaskbar.getEntry(21),
    );
  }

  override createPresenterWithEmptyTrace(
    callback: NotifyHierarchyViewCallbackType,
  ): Presenter {
    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE)
      .setEntries([])
      .setParserCustomQueryResult(CustomQueryType.VIEW_CAPTURE_METADATA, {
        packageName: 'the_package_name',
        windowName: 'the_window_name',
      })
      .build();
    const traces = new Traces();
    traces.addTrace(trace);
    return new Presenter(traces, new InMemoryStorage(), callback);
  }

  override createPresenter(
    callback: NotifyHierarchyViewCallbackType,
  ): Presenter {
    return new Presenter(
      assertDefined(this.traces),
      new InMemoryStorage(),
      callback,
    );
  }

  override getPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.positionUpdate);
  }

  override getSecondPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.secondPositionUpdate);
  }

  override getShowDiffPositionUpdate(): TracePositionUpdate {
    return assertDefined(this.diffPositionUpdate);
  }

  override getExpectedChildrenBeforeVisibilityFilter(): number {
    return this.numberOfNestedChildren;
  }

  override getExpectedChildrenAfterVisibilityFilter(): number {
    return this.numberOfVisibleChildren;
  }

  override getExpectedHierarchyChildrenBeforeStringFilter(): number {
    return this.numberOfNestedChildren;
  }

  override executeSpecializedChecksForPropertiesFromNode(
    uiData: UiDataHierarchy,
  ) {
    expect(
      assertDefined((uiData as UiData).curatedProperties).translationY,
    ).toEqual('-0.633');
  }

  override getSelectedTree(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override getSelectedTreeAfterPositionUpdate(): UiHierarchyTreeNode {
    return assertDefined(this.selectedTree);
  }

  override executeChecksForPropertiesTreeAfterPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('-0.633');
  }

  override executeChecksForPropertiesTreeAfterSecondPositionUpdate(
    uiData: UiDataHierarchy,
  ) {
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('0');
  }

  override executeSpecializedChecksForPropertiesFromRect(
    uiData: UiDataHierarchy,
  ) {
    const curatedProperties = assertDefined(
      (uiData as UiData).curatedProperties,
    );
    expect(curatedProperties.translationX).toEqual('233.075');
    expect(curatedProperties.translationY).toEqual('81');
    expect(curatedProperties.alpha).toEqual('1');
    expect(curatedProperties.willNotDraw).toEqual('false');
  }

  private readonly numberOfVisibleChildren = 6;
  private readonly numberOfNestedChildren = 16;
}

describe('PresenterViewCapture', () => {
  new PresenterViewCaptureTest().execute();
});
