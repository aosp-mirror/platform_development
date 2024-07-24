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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {MockStorage} from 'test/unit/mock_storage';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType, ViewCaptureTraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {DiffType} from 'viewers/common/diff_type';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter} from 'viewers/viewer_view_capture/presenter';
import {UiData} from 'viewers/viewer_view_capture/ui_data';

describe('PresenterViewCapture', () => {
  let parser: Parser<HierarchyTreeNode>;
  let trace: Trace<HierarchyTreeNode>;
  let uiData: UiData;
  let presenter: Presenter;
  let positionUpdate: TracePositionUpdate;
  let selectedTree: UiHierarchyTreeNode;

  beforeAll(async () => {
    parser = (await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
    )) as Parser<HierarchyTreeNode>;

    trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY)
      .setParser(parser)
      .build();

    const firstEntry = trace.getEntry(0);
    positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    const firstEntryDataTree = await firstEntry.getValue();

    selectedTree = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs(
          UiTreeUtils.makeIdMatchFilter(
            'ViewNode com.android.launcher3.taskbar.TaskbarView@80213537',
          ),
        ),
      ),
    );
  });

  beforeEach(() => {
    presenter = createPresenter(trace);
  });

  it('is robust to empty trace', async () => {
    const emptyTrace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY)
      .setEntries([])
      .setParserCustomQueryResult(
        CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME,
        'the_package_name',
      )
      .build();
    const presenter = createPresenter(emptyTrace);

    const positionUpdateWithoutTraceEntry = TracePositionUpdate.fromTimestamp(
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n),
    );
    await presenter.onAppEvent(positionUpdateWithoutTraceEntry);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.tree).toBeFalsy();
  });

  it('processes trace position updates', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.highlightedItem?.length).toEqual(0);

    const hierarchyOpts = Object.keys(uiData.hierarchyUserOptions);
    expect(hierarchyOpts).toBeTruthy();
    const propertyOpts = Object.keys(uiData.propertiesUserOptions);
    expect(propertyOpts).toBeTruthy();
    expect(assertDefined(uiData.tree).getAllChildren().length > 0).toBeTrue();
  });

  it('creates input data for rects view', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.rects[0].x).toEqual(0);
    expect(uiData.rects[0].y).toEqual(0);
    expect(uiData.rects[0].w).toEqual(1080);
    expect(uiData.rects[0].h).toEqual(249);
  });

  it('updates pinned items', async () => {
    const pinnedItem = TreeNodeUtils.makeUiHierarchyNode({
      id: 'id',
      name: 'FirstPinnedItem',
    });
    await presenter.onAppEvent(positionUpdate);
    presenter.onPinnedItemChange(pinnedItem);
    expect(uiData.pinnedItems).toContain(pinnedItem);
  });

  it('updates highlighted item', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.highlightedItem).toEqual('');

    const id = '4';
    presenter.onHighlightedItemChange(id);
    expect(uiData.highlightedItem).toBe(id);
  });

  it('shows only visible in hierarchy tree', async () => {
    await presenter.onAppEvent(positionUpdate);

    expect(
      // TaskbarDragLayer -> TaskbarView
      uiData.tree?.getAllChildren()[0].name,
    ).toEqual('com.android.launcher3.taskbar.TaskbarView@80213537');

    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
      },
      simplifyNames: {
        name: 'Simplify names',
        enabled: false,
      },
      onlyVisible: {
        name: 'Only visible',
        enabled: true,
      },
    };
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    expect(
      // TaskbarDragLayer -> TaskbarScrimView
      uiData.tree?.getAllChildren()[0].name,
    ).toEqual('com.android.launcher3.taskbar.TaskbarScrimView@114418695');
  });

  it('simplifies names in hierarchy tree', async () => {
    await presenter.onAppEvent(positionUpdate);

    expect(
      // TaskbarDragLayer -> TaskbarView
      uiData.tree?.getAllChildren()[0].getDisplayName(),
    ).toEqual('TaskbarView@80213537');

    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
      },
      simplifyNames: {
        name: 'Simplify names',
        enabled: false,
      },
      onlyVisible: {
        name: 'Only visible',
        enabled: false,
      },
    };
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    expect(
      // TaskbarDragLayer -> TaskbarScrimView
      uiData.tree?.getAllChildren()[0].getDisplayName(),
    ).toEqual('com.android.launcher3.taskbar.TaskbarView@80213537');
  });

  it('filters hierarchy tree', async () => {
    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
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
    };
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHierarchyUserOptionsChange(userOptions);
    await presenter.onHierarchyFilterChange('BubbleBarView');

    expect(
      // TaskbarDragLayer -> BubbleBarView if filter works as expected
      uiData.tree?.getAllChildren()[0].name,
    ).toEqual('com.android.launcher3.taskbar.bubbles.BubbleBarView@256010548');
  });

  it('sets properties tree and associated ui data', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
    expect(uiData.propertiesTree).toBeTruthy();
  });

  it('updates properties tree', async () => {
    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: true,
      },
      showDefaults: {
        name: 'Show defaults',
        enabled: true,
        tooltip: `
                      If checked, shows the value of all properties.
                      Otherwise, hides all properties whose value is
                      the default for its data type.
                    `,
      },
    };

    const entry = trace.getEntry(21);
    const update = TracePositionUpdate.fromTraceEntry(entry);

    await presenter.onAppEvent(update);
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
    expect(
      assertDefined(
        uiData.propertiesTree?.getChildByName('translationY'),
      ).getDiff(),
    ).toEqual(DiffType.NONE);

    await presenter.onPropertiesUserOptionsChange(userOptions);

    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(
      assertDefined(
        uiData.propertiesTree?.getChildByName('translationY'),
      ).getDiff(),
    ).toEqual(DiffType.MODIFIED);
  });

  it('filters properties tree', async () => {
    await presenter.onAppEvent(positionUpdate);

    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: true,
      },
      showDefaults: {
        name: 'Show defaults',
        enabled: true,
        tooltip: `
                        If checked, shows the value of all properties.
                        Otherwise, hides all properties whose value is
                        the default for its data type.
                      `,
      },
    };
    await presenter.onPropertiesUserOptionsChange(userOptions);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(18);

    await presenter.onPropertiesFilterChange('alpha');
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(1);
  });

  function createPresenter(trace: Trace<HierarchyTreeNode>): Presenter {
    const traces = new Traces();
    const traceType = parser.getTraceType();
    traces.setTrace(traceType, trace);
    return new Presenter(
      traceType as ViewCaptureTraceType,
      traces,
      new MockStorage(),
      (newData: UiData) => {
        uiData = newData;
      },
    );
  }
});
