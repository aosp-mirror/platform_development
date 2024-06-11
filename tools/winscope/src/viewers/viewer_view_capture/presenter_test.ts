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
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {DiffType} from 'viewers/common/diff_type';
import {RectShowState} from 'viewers/common/rect_show_state';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter} from 'viewers/viewer_view_capture/presenter';
import {UiData} from 'viewers/viewer_view_capture/ui_data';

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
      traces.addTrace(Trace.fromParser(parser));
    }

    traceTaskbar = assertDefined(traces.getTraces(TraceType.VIEW_CAPTURE)[0]);
    const firstEntry = traceTaskbar.getEntry(0);
    positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    const traceLauncherActivity = assertDefined(
      traces.getTraces(TraceType.VIEW_CAPTURE)[1],
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

  it('is robust to empty trace', async () => {
    const emptyTrace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE)
      .setEntries([])
      .setParserCustomQueryResult(CustomQueryType.VIEW_CAPTURE_METADATA, {
        packageName: 'the_package_name',
        windowName: 'the_window_name',
      })
      .build();
    const emptyTraces = new Traces();
    emptyTraces.addTrace(emptyTrace);
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
    expect(uiData.vcRectIdToShowState?.size).toEqual(13);
    expect(uiData.highlightedItem?.length).toEqual(0);

    const hierarchyUserOptions = Object.keys(uiData.hierarchyUserOptions);
    expect(hierarchyUserOptions).toBeTruthy();

    const propertiesUserOptions = Object.keys(uiData.propertiesUserOptions);
    expect(propertiesUserOptions).toBeTruthy();

    expect(assertDefined(uiData.trees).length === 1).toBeTrue();
    expect(
      assertDefined(uiData.trees)[0].getAllChildren().length > 0,
    ).toBeTrue();
    expect(uiData.windows).toEqual([
      {displayId: 1, groupId: 1, name: 'PhoneWindow@25063d9'},
      {displayId: 0, groupId: 0, name: 'Taskbar'},
    ]);

    await presenter.onAppEvent(secondPositionUpdate);
    expect(uiData.vcRectsToDraw.length).toEqual(145);
    expect(assertDefined(uiData.trees).length === 2).toBeTrue();
    expect(
      assertDefined(uiData.trees).every(
        (tree) => tree.getAllChildren().length > 0,
      ),
    ).toBeTrue();
    expect(uiData.windows).toEqual([
      {displayId: 1, groupId: 1, name: 'PhoneWindow@25063d9'},
      {displayId: 0, groupId: 0, name: 'Taskbar'},
    ]);
  });

  it('creates input data for rects view', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.vcRectsToDraw[0].x).toEqual(0);
    expect(uiData.vcRectsToDraw[0].y).toEqual(0);
    expect(uiData.vcRectsToDraw[0].w).toEqual(1080);
    expect(uiData.vcRectsToDraw[0].h).toEqual(249);
    checkRectUiData(13, 13, 13);
  });

  it('filters rects by visibility', async () => {
    const userOptions: UserOptions = {
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
        enabled: false,
      },
    };

    await presenter.onAppEvent(positionUpdate);
    presenter.onRectsUserOptionsChange(userOptions);
    expect(uiData.rectsUserOptions).toEqual(userOptions);
    checkRectUiData(13, 13, 13);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(5, 13, 5);
  });

  it('filters rects by show/hide state', async () => {
    const userOptions: UserOptions = {
      ignoreNonHidden: {
        name: 'Ignore',
        icon: 'visibility',
        enabled: true,
      },
    };
    await presenter.onAppEvent(positionUpdate);
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(13, 13, 13);

    await presenter.onRectShowStateChange(
      'ViewNode com.android.launcher3.views.IconButtonView@78121542',
      RectShowState.HIDE,
    );
    checkRectUiData(13, 13, 12);

    userOptions['ignoreNonHidden'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(12, 13, 12);
  });

  it('handles both visibility and show/hide state in rects', async () => {
    const userOptions: UserOptions = {
      ignoreNonHidden: {
        name: 'Apply',
        icon: 'visibility',
        enabled: true,
      },
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
        enabled: false,
      },
    };
    await presenter.onAppEvent(positionUpdate);
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(13, 13, 13);

    await presenter.onRectShowStateChange(
      'ViewNode com.android.launcher3.taskbar.TaskbarScrimView@114418695',
      RectShowState.HIDE,
    );
    checkRectUiData(13, 13, 12);

    userOptions['ignoreNonHidden'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(12, 13, 12);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(4, 13, 4);

    userOptions['ignoreNonHidden'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(5, 13, 4);
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
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
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
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
        enabled: false,
      },
    };
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    expect(uiData.trees?.at(0)?.getAllChildren()[0].getDisplayName()).toEqual(
      'com.android.launcher3.taskbar.TaskbarView@80213537',
    );
  });

  it('filters hierarchy tree by search string', async () => {
    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
      },
      simplifyNames: {
        name: 'Simplify names',
        enabled: true,
      },
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
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
    const rect = assertDefined(uiData.vcRectsToDraw.at(5));
    await presenter.onHighlightedIdChange(rect.id);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toEqual(
      'ViewNode com.android.launcher3.views.DoubleShadowBubbleTextView@124683434',
    );
    expect(propertiesTree.getAllChildren().length).toEqual(15);
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
    return new Presenter(traces, new InMemoryStorage(), (newData: UiData) => {
      uiData = newData;
    });
  }

  function checkRectUiData(
    rectsToDraw: number,
    allRects: number,
    shownRects: number,
  ) {
    expect(uiData.vcRectsToDraw.length).toEqual(rectsToDraw);
    const showStates = Array.from(
      assertDefined(uiData.vcRectIdToShowState).values(),
    );
    expect(showStates.length).toEqual(allRects);
    expect(showStates.filter((s) => s === RectShowState.SHOW).length).toEqual(
      shownRects,
    );
  }
});
