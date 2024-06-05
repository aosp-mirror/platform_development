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
import {InMemoryStorage} from 'common/in_memory_storage';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
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
import {Presenter} from './presenter';
import {UiData} from './ui_data';

describe('PresenterWindowManager', () => {
  let trace: Trace<HierarchyTreeNode>;
  let positionUpdate: TracePositionUpdate;
  let secondPositionUpdate: TracePositionUpdate;
  let presenter: Presenter;
  let uiData: UiData;
  let selectedTree: UiHierarchyTreeNode;

  beforeAll(async () => {
    trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.WINDOW_MANAGER)
      .setEntries([
        await UnitTestUtils.getWindowManagerState(0),
        await UnitTestUtils.getWindowManagerState(1),
      ])
      .build();

    const firstEntry = trace.getEntry(0);
    positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);
    secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      trace.getEntry(1),
    );

    const firstEntryDataTree = await firstEntry.getValue();
    selectedTree = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs(UiTreeUtils.makeIdFilter('93d3f3c')),
      ),
    );
  });

  beforeEach(() => {
    presenter = createPresenter(trace);
  });

  it('is robust to empty trace', async () => {
    const emptyTrace = new TraceBuilder<HierarchyTreeNode>()
      .setEntries([])
      .build();
    const presenter = createPresenter(emptyTrace);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.tree).toBeFalsy();

    const positionUpdateWithoutTraceEntry = TracePositionUpdate.fromTimestamp(
      TimestampConverterUtils.makeRealTimestamp(0n),
    );
    await presenter.onAppEvent(positionUpdateWithoutTraceEntry);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.tree).toBeFalsy();
  });

  it('processes trace position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    const filteredUiDataRectLabels = uiData.rectsToDraw
      ?.filter((rect) => rect.isVisible !== undefined)
      .map((rect) => rect.label);
    const hierarchyOpts = uiData.hierarchyUserOptions
      ? Object.keys(uiData.hierarchyUserOptions)
      : null;
    const propertyOpts = uiData.propertiesUserOptions
      ? Object.keys(uiData.propertiesUserOptions)
      : null;
    expect(uiData.highlightedItem?.length).toEqual(0);
    expect(filteredUiDataRectLabels?.length).toEqual(12);
    expect(uiData.displays.map((display) => display.groupId)).toContain(0);
    expect(hierarchyOpts).toBeTruthy();
    expect(propertyOpts).toBeTruthy();

    // does not check specific tree values as tree generation method may change
    expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
  });

  it('disables show diff and generates non-diff tree if no prev entry available', async () => {
    await presenter.onAppEvent(positionUpdate);

    const hierarchyOpts = assertDefined(uiData.hierarchyUserOptions);
    expect(hierarchyOpts['showDiff'].isUnavailable).toBeTrue();

    const propertyOpts = assertDefined(uiData.propertiesUserOptions);
    expect(propertyOpts['showDiff'].isUnavailable).toBeTrue();

    expect(assertDefined(uiData.tree).getAllChildren().length > 0).toBeTrue();
  });

  it('creates input data for rects view', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rectsToDraw[0].x).toEqual(0);
    expect(uiData.rectsToDraw[0].y).toEqual(0);
    expect(uiData.rectsToDraw[0].w).toEqual(1080);
    expect(uiData.rectsToDraw[0].h).toEqual(2400);
    checkRectUiData(12, 12, 12);
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
    checkRectUiData(12, 12, 12);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(7, 12, 7);
  });

  it('filters rects by show/hide state', async () => {
    const userOptions: UserOptions = {
      ignoreNonHidden: {
        name: 'Ignore',
        icon: 'visibility',
        enabled: true,
      },
    };
    presenter.onRectsUserOptionsChange(userOptions);
    await presenter.onAppEvent(positionUpdate);
    checkRectUiData(12, 12, 12);

    await presenter.onRectShowStateChange(
      'WindowState 93d3f3c ScreenDecorOverlayBottom',
      RectShowState.HIDE,
    );
    checkRectUiData(12, 12, 11);

    userOptions['ignoreNonHidden'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(11, 12, 11);
  });

  it('handles both visibility and show/hide state in rects', async () => {
    const userOptions: UserOptions = {
      ignoreNonHidden: {
        name: 'Ignore',
        icon: 'visibility',
        enabled: true,
      },
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
        enabled: false,
      },
    };
    presenter.onRectsUserOptionsChange(userOptions);
    await presenter.onAppEvent(positionUpdate);
    checkRectUiData(12, 12, 12);

    await presenter.onRectShowStateChange(
      'WindowState 93d3f3c ScreenDecorOverlayBottom',
      RectShowState.HIDE,
    );
    checkRectUiData(12, 12, 11);

    userOptions['ignoreNonHidden'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(11, 12, 11);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(6, 12, 6);

    userOptions['ignoreNonHidden'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(7, 12, 6);
  });

  it('updates pinned items', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.pinnedItems).toEqual([]);

    const pinnedItem = TreeNodeUtils.makeUiHierarchyNode({
      id: 'TestItem 4',
      name: 'FirstPinnedItem',
    });

    presenter.onPinnedItemChange(pinnedItem);
    expect(uiData.pinnedItems).toContain(pinnedItem);
  });

  it('updates highlighted property', () => {
    expect(uiData.highlightedProperty).toEqual('');
    const id = '4';
    presenter.onHighlightedPropertyChange(id);
    expect(uiData.highlightedProperty).toBe(id);
  });

  it('filters hierarchy tree by visibility', async () => {
    const userOptions: UserOptions = {
      showOnlyVisible: {
        name: 'Show only',
        chip: VISIBLE_CHIP,
        enabled: false,
      },
      flat: {
        name: 'Flat',
        enabled: true,
      },
    };

    await presenter.onAppEvent(positionUpdate);
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(68);

    userOptions['showOnlyVisible'].enabled = true;
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(6);
  });

  it('flattens hierarchy tree', async () => {
    //change flat view to true
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
      flat: {
        name: 'Flat',
        enabled: true,
      },
    };

    await presenter.onAppEvent(positionUpdate);
    const oldDataTree = assertDefined(uiData.tree);
    expect(oldDataTree.getAllChildren().length).toEqual(1);

    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    const newDataTree = assertDefined(uiData.tree);
    expect(newDataTree.getAllChildren().length).toEqual(68);
    newDataTree.getAllChildren().forEach((child) => {
      expect(child.getAllChildren().length).toEqual(0);
    });
  });

  it('simplifies names in hierarchy tree', async () => {
    //change flat view to true
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
      flat: {
        name: 'Flat',
        enabled: false,
      },
    };

    await presenter.onAppEvent(positionUpdate);

    const longName =
      'com.google.android.apps.nexuslauncher/.NexusLauncherActivity';
    const id = `Activity f7092ed ${longName}`;

    const nodeWithLongName = assertDefined(
      assertDefined(uiData.tree).findDfs(UiTreeUtils.makeIdMatchFilter(id)),
    );
    expect(nodeWithLongName.getDisplayName()).toEqual(
      'com.google.(...).NexusLauncherActivity',
    );

    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    const nodeWithShortName = assertDefined(
      assertDefined(uiData.tree).findDfs(UiTreeUtils.makeIdMatchFilter(id)),
    );
    expect(nodeWithShortName.getDisplayName()).toEqual(longName);
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
      flat: {
        name: 'Flat',
        enabled: true,
      },
    };
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(68);

    await presenter.onHierarchyFilterChange('ScreenDecor');
    // All but four layers should be filtered out
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(2);
  });

  it('sets properties tree and associated ui data from tree node', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.propertiesTree).toBeUndefined();
    await presenter.onHighlightedNodeChange(selectedTree);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toEqual(
      'WindowState 93d3f3c ScreenDecorOverlayBottom',
    );
    expect(propertiesTree.getAllChildren().length).toEqual(21);
  });

  it('sets properties tree and associated ui data from rect', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.propertiesTree).toBeUndefined();
    const rect = assertDefined(uiData.rectsToDraw.at(5));
    await presenter.onHighlightedIdChange(rect.id);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toEqual('WindowState e3666ec NotificationShade');
    expect(propertiesTree.getAllChildren().length).toEqual(16);
  });

  it('after highlighting a node, updates properties tree on position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    const selectedTree = assertDefined(
      assertDefined(uiData.tree).findDfs(
        UiTreeUtils.makeIdMatchFilter(
          'Activity f7092ed com.google.android.apps.nexuslauncher/.NexusLauncherActivity',
        ),
      ),
    );
    await presenter.onHighlightedNodeChange(selectedTree);
    let propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(propertiesTree.getChildByName('state')).formattedValue(),
    ).toEqual('STOPPED');

    await presenter.onAppEvent(secondPositionUpdate);
    propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(propertiesTree.getChildByName('state')).formattedValue(),
    ).toEqual('RESUMED');
  });

  it('after highlighting a rect, updates properties tree on position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedIdChange(
      'Activity f7092ed com.google.android.apps.nexuslauncher/.NexusLauncherActivity',
    );
    let propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(propertiesTree.getChildByName('state')).formattedValue(),
    ).toEqual('STOPPED');

    await presenter.onAppEvent(secondPositionUpdate);
    propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(propertiesTree.getChildByName('state')).formattedValue(),
    ).toEqual('RESUMED');
  });

  it('updates properties tree to show diffs', async () => {
    //change flat view to true
    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: true,
      },
    };

    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedNodeChange(selectedTree);
    expect(
      assertDefined(
        uiData.propertiesTree?.getChildByName('animator'),
      ).getDiff(),
    ).toEqual(DiffType.NONE);

    await presenter.onPropertiesUserOptionsChange(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);

    expect(
      assertDefined(
        uiData.propertiesTree?.getChildByName('animator'),
      ).getDiff(),
    ).toEqual(DiffType.ADDED);
  });

  it('shows/hides defaults', async () => {
    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: true,
      },
      showDefaults: {
        name: 'Show defaults',
        enabled: true,
      },
    };

    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedNodeChange(selectedTree);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(21);

    await presenter.onPropertiesUserOptionsChange(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(50);
  });

  it('filters properties tree', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedNodeChange(selectedTree);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(21);

    await presenter.onPropertiesFilterChange('requested');
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(2);
  });

  const createPresenter = (trace: Trace<HierarchyTreeNode>): Presenter => {
    const traces = new Traces();
    traces.addTrace(trace);
    return new Presenter(
      trace,
      traces,
      new InMemoryStorage(),
      (newData: UiData) => {
        uiData = newData;
      },
    );
  };

  function checkRectUiData(
    rectsToDraw: number,
    allRects: number,
    shownRects: number,
  ) {
    expect(uiData.rectsToDraw.length).toEqual(rectsToDraw);
    const showStates = Array.from(
      assertDefined(uiData.rectIdToShowState).values(),
    );
    expect(showStates.length).toEqual(allRects);
    expect(showStates.filter((s) => s === RectShowState.SHOW).length).toEqual(
      shownRects,
    );
  }
});
