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
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {DiffType} from 'viewers/common/diff_type';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter} from 'viewers/viewer_view_capture/presenter';
import {UiData} from 'viewers/viewer_view_capture/ui_data';
import {ViewerViewCaptureLauncher} from './viewer_view_capture';

describe('PresenterViewCapture', () => {
  let parsers: Array<Parser<HierarchyTreeNode>>;
  let traces: Traces;
  let traceTaskbar: Trace<HierarchyTreeNode>;
  let uiData: UiData;
  let presenter: Presenter;
  let positionUpdate: TracePositionUpdate;
  let secondPositionUpdate: TracePositionUpdate;
  let selectedTree: UiHierarchyTreeNode;

  beforeAll(async () => {
    parsers = (await UnitTestUtils.getParsers(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc',
    )) as Array<Parser<HierarchyTreeNode>>;

    traces = new Traces();
    for (const parser of parsers) {
      traces.setTrace(
        parser.getTraceType(),
        new TraceBuilder<HierarchyTreeNode>()
          .setType(parser.getTraceType())
          .setParser(parser)
          .build(),
      );
    }
    traceTaskbar = assertDefined(
      traces.getTrace(TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER),
    );
    const firstEntry = traceTaskbar.getEntry(0);
    positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    const traceLauncherActivity = assertDefined(
      traces.getTrace(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY),
    );
    const firstEntryLauncherActivity = traceLauncherActivity.getEntry(0);
    secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      firstEntryLauncherActivity,
    );

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
    presenter = createPresenter(traces);
  });

  it('initializes active trace type', () => {
    expect(presenter.getActiveTraceType()).toEqual(
      TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY,
    );
  });

  it('is robust to empty trace', async () => {
    const emptyTrace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER)
      .setEntries([])
      .setParserCustomQueryResult(
        CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME,
        'the_package_name',
      )
      .build();
    const emptyTraces = new Traces();
    emptyTraces.setTrace(emptyTrace.type, emptyTrace);
    const presenter = createPresenter(emptyTraces);

    const positionUpdateWithoutTraceEntry = TracePositionUpdate.fromTimestamp(
      TimestampConverterUtils.makeRealTimestamp(0n),
    );
    await presenter.onAppEvent(positionUpdateWithoutTraceEntry);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.trees).toBeFalsy();
  });

  it('processes trace position updates', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rects.length).toEqual(17);
    expect(uiData.highlightedItem?.length).toEqual(0);

    const hierarchyOpts = Object.keys(uiData.hierarchyUserOptions);
    expect(hierarchyOpts).toBeTruthy();
    const propertyOpts = Object.keys(uiData.propertiesUserOptions);
    expect(propertyOpts).toBeTruthy();
    expect(assertDefined(uiData.trees).length === 1).toBeTrue();
    expect(
      assertDefined(uiData.trees)[0].getAllChildren().length > 0,
    ).toBeTrue();
    expect(uiData.windows).toEqual([
      {displayId: 21, groupId: 21, name: 'Nexuslauncher'},
      {displayId: 22, groupId: 22, name: 'Taskbar'},
    ]);

    await presenter.onAppEvent(secondPositionUpdate);
    expect(uiData.rects.length).toEqual(168);
    expect(assertDefined(uiData.trees).length === 2).toBeTrue();
    expect(
      assertDefined(uiData.trees).every(
        (tree) => tree.getAllChildren().length > 0,
      ),
    ).toBeTrue();
    expect(uiData.windows).toEqual([
      {displayId: 21, groupId: 21, name: 'Nexuslauncher'},
      {displayId: 22, groupId: 22, name: 'Taskbar'},
    ]);
  });

  it('creates input data for rects view', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rects.length).toEqual(17);
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

  it('shows only visible in hierarchy tree', async () => {
    await presenter.onAppEvent(positionUpdate);

    expect(
      // TaskbarDragLayer -> TaskbarView
      uiData.trees?.at(0)?.getAllChildren()[0].name,
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
      uiData.trees?.at(0)?.getAllChildren()[0].name,
    ).toEqual('com.android.launcher3.taskbar.TaskbarScrimView@114418695');
  });

  it('simplifies names in hierarchy tree', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.trees?.at(0)?.getAllChildren()[0].getDisplayName()).toEqual(
      'TaskbarView@80213537',
    );

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
    expect(uiData.trees?.at(0)?.getAllChildren()[0].getDisplayName()).toEqual(
      'com.android.launcher3.taskbar.TaskbarView@80213537',
    );
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
      uiData.trees?.at(0)?.getAllChildren()[0].name,
    ).toEqual('com.android.launcher3.taskbar.bubbles.BubbleBarView@256010548');
  });

  it('sets properties tree and associated ui data from tree node', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedNodeChange(selectedTree);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toEqual(
      'ViewNode com.android.launcher3.taskbar.TaskbarView@80213537',
    );
    expect(propertiesTree.getAllChildren().length).toEqual(15);
    expect(assertDefined(uiData.curatedProperties).translationY).toEqual(
      '-0.633',
    );
  });

  it('sets properties tree and associated ui data from rect', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(assertDefined(uiData.propertiesTree).id).toEqual(
      'ViewNode com.android.launcher3.taskbar.TaskbarDragLayer@265160962',
    );
    const rect = assertDefined(uiData.rects.at(5));
    await presenter.onHighlightedIdChange(rect.id);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toEqual(
      'ViewNode com.android.launcher3.views.DoubleShadowBubbleTextView@124683434',
    );
    expect(propertiesTree.getAllChildren().length).toEqual(14);
    expect(assertDefined(uiData.curatedProperties).translationX).toEqual(
      '19.143',
    );
  });

  it('after highlighting a node, updates properties tree on position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    const selectedTree = assertDefined(
      assertDefined(uiData.trees?.at(0)).findDfs(
        UiTreeUtils.makeIdMatchFilter(
          'ViewNode com.android.launcher3.taskbar.TaskbarView@80213537',
        ),
      ),
    );
    await presenter.onHighlightedNodeChange(selectedTree);
    let propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('-0.633');

    await presenter.onAppEvent(secondPositionUpdate);
    propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('0');
  });

  it('after highlighting a rect, updates properties tree on position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedIdChange(
      'ViewNode com.android.launcher3.taskbar.TaskbarView@80213537',
    );
    let propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('-0.633');

    await presenter.onAppEvent(secondPositionUpdate);
    propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree.getChildByName('translationY'),
      ).formattedValue(),
    ).toEqual('0');
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

    const entry = traceTaskbar.getEntry(21);
    const update = TracePositionUpdate.fromTraceEntry(entry);

    await presenter.onAppEvent(update);
    await presenter.onHighlightedNodeChange(selectedTree);
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

  function createPresenter(traces: Traces): Presenter {
    return new Presenter(
      ViewerViewCaptureLauncher.DEPENDENCIES,
      traces,
      new InMemoryStorage(),
      (newData: UiData) => {
        uiData = newData;
      },
    );
  }
});
