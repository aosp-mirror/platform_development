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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {MockStorage} from 'test/unit/mock_storage';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {DiffType} from 'viewers/common/diff_type';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

describe('PresenterSurfaceFlinger', () => {
  let trace: Trace<HierarchyTreeNode>;
  let positionUpdate: TracePositionUpdate;
  let positionUpdateMultiDisplayEntry: TracePositionUpdate;
  let presenter: Presenter;
  let uiData: UiData;
  let selectedTree: UiHierarchyTreeNode;

  beforeAll(async () => {
    trace = new TraceBuilder<HierarchyTreeNode>()
      .setEntries([
        await UnitTestUtils.getLayerTraceEntry(),
        await UnitTestUtils.getMultiDisplayLayerTraceEntry(),
      ])
      .build();

    const firstEntry = trace.getEntry(0);
    positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);
    positionUpdateMultiDisplayEntry = TracePositionUpdate.fromTraceEntry(
      trace.getEntry(1),
    );

    const firstEntryDataTree = await firstEntry.getValue();
    const layer = assertDefined(
      firstEntryDataTree.findDfs(
        UiTreeUtils.makeIdMatchFilter('53 Dim layer#53'),
      ),
    );
    const selectedTreeParent = UiHierarchyTreeNode.from(
      assertDefined(layer.getZParent()),
    );
    selectedTree = assertDefined(
      selectedTreeParent.getChildByName('Dim layer#53'),
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
    expect(
      Array.from(uiData.displays.map((display) => display.groupId)),
    ).toContain(0);
    const hierarchyOpts = uiData.hierarchyUserOptions
      ? Object.keys(uiData.hierarchyUserOptions)
      : null;
    expect(hierarchyOpts).toBeTruthy();
    const propertyOpts = uiData.propertiesUserOptions
      ? Object.keys(uiData.propertiesUserOptions)
      : null;
    expect(propertyOpts).toBeTruthy();
    expect(assertDefined(uiData.tree).getAllChildren().length > 0).toBeTrue();
  });

  it('disables show diff and generates non-diff tree if no prev entry available', async () => {
    await presenter.onAppEvent(positionUpdate);

    const hierarchyOpts = uiData.hierarchyUserOptions ?? null;
    expect(hierarchyOpts).toBeTruthy();
    expect(hierarchyOpts!['showDiff'].isUnavailable).toBeTrue();

    const propertyOpts = uiData.propertiesUserOptions ?? null;
    expect(propertyOpts).toBeTruthy();
    expect(propertyOpts!['showDiff'].isUnavailable).toBeTrue();

    expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
  });

  it('creates input data for rects view', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.rects[0].x).toEqual(0);
    expect(uiData.rects[0].y).toEqual(0);
    expect(uiData.rects[0].w).toEqual(1080);
    expect(uiData.rects[0].h).toEqual(2400);
  });

  it('updates pinned items', () => {
    expect(uiData.pinnedItems).toEqual([]);

    const pinnedItem = TreeNodeUtils.makeUiHierarchyNode({
      id: 'TestItem 4',
      name: 'FirstPinnedItem',
    });

    presenter.onPinnedItemChange(pinnedItem);
    expect(uiData.pinnedItems).toContain(pinnedItem);
  });

  it('updates highlighted item', () => {
    expect(uiData.highlightedItem).toEqual('');

    const id = '4';
    presenter.onHighlightedItemChange(id);
    expect(uiData.highlightedItem).toBe(id);
  });

  it('updates highlighted property', () => {
    expect(uiData.highlightedProperty).toEqual('');

    const id = '4';
    presenter.onHighlightedPropertyChange(id);
    expect(uiData.highlightedProperty).toBe(id);
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
      onlyVisible: {
        name: 'Only visible',
        enabled: false,
      },
      flat: {
        name: 'Flat',
        enabled: true,
      },
    };

    await presenter.onAppEvent(positionUpdate);
    const oldDataTree = assertDefined(uiData.tree);
    expect(oldDataTree.getAllChildren().length).toEqual(3);

    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    const newDataTree = assertDefined(uiData.tree);
    expect(newDataTree.getAllChildren().length).toEqual(94);
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
      onlyVisible: {
        name: 'Only visible',
        enabled: false,
      },
      flat: {
        name: 'Flat',
        enabled: false,
      },
    };

    await presenter.onAppEvent(positionUpdate);
    const longName =
      'ActivityRecord{64953af u0 com.google.android.apps.nexuslauncher/.NexusLauncherActivity#96';
    const id = `96 ${longName}`;
    const nodeWithLongName = assertDefined(
      assertDefined(uiData.tree).findDfs(UiTreeUtils.makeIdMatchFilter(id)),
    );
    expect(nodeWithLongName.getDisplayName()).toEqual(
      'ActivityRecord{64953af u0 com.google.(...).NexusLauncherActivity#96',
    );

    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    const nodeWithShortName = assertDefined(
      assertDefined(uiData.tree).findDfs(UiTreeUtils.makeIdMatchFilter(id)),
    );
    expect(nodeWithShortName.getDisplayName()).toEqual(longName);
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
      flat: {
        name: 'Flat',
        enabled: true,
      },
    };
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(94);

    await presenter.onHierarchyFilterChange('Wallpaper');
    // All but four layers should be filtered out
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(4);
  });

  it('sets properties tree and associated ui data', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
    // does not check specific tree values as tree transformation method may change
    expect(uiData.propertiesTree).toBeTruthy();
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
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
    expect(
      assertDefined(uiData.propertiesTree?.getChildByName('bounds')).getDiff(),
    ).toEqual(DiffType.NONE);

    await presenter.onPropertiesUserOptionsChange(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(
      assertDefined(uiData.propertiesTree?.getChildByName('bounds')).getDiff(),
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
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(22);

    await presenter.onPropertiesUserOptionsChange(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(56);
  });

  it('filters properties tree', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(22);

    await presenter.onPropertiesFilterChange('bound');
    expect(
      assertDefined(uiData.propertiesTree).getAllChildren().length,
    ).toEqual(3);
  });

  it('handles displays with no visible layers', async () => {
    await presenter.onAppEvent(positionUpdateMultiDisplayEntry);
    expect(uiData.displays.length).toEqual(5);
    // we want the displays to be sorted by name
    expect(uiData.displays).toEqual([
      {
        displayId: '11529215046312967684',
        groupId: 5,
        name: 'ClusterOsDouble-VD',
      },
      {displayId: '4619827259835644672', groupId: 0, name: 'EMU_display_0'},
      {displayId: '4619827551948147201', groupId: 2, name: 'EMU_display_1'},
      {displayId: '4619827124781842690', groupId: 3, name: 'EMU_display_2'},
      {displayId: '4619827540095559171', groupId: 4, name: 'EMU_display_3'},
    ]);
  });

  it('updates view capture package names', async () => {
    const vcTrace = new TraceBuilder<HierarchyTreeNode>()
      .setEntries([await UnitTestUtils.getViewCaptureEntry()])
      .setParserCustomQueryResult(
        CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME,
        'com.google.android.apps.nexuslauncher',
      )
      .build();
    const traces = new Traces();
    traces.setTrace(TraceType.SURFACE_FLINGER, trace);
    traces.setTrace(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY, vcTrace);
    const presenter = new Presenter(
      traces,
      new MockStorage(),
      (newData: UiData) => {
        uiData = newData;
      },
    );

    const firstEntry = trace.getEntry(0);
    const positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rects.filter((rect) => rect.hasContent).length).toEqual(1);
  });

  function createPresenter(trace: Trace<HierarchyTreeNode>): Presenter {
    const traces = new Traces();
    traces.setTrace(TraceType.SURFACE_FLINGER, trace);
    return new Presenter(traces, new MockStorage(), (newData: UiData) => {
      uiData = newData;
    });
  }
});
