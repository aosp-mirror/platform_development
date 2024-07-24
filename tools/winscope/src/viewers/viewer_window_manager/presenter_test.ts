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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {MockStorage} from 'test/unit/mock_storage';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
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

describe('PresenterWindowManager', () => {
  let trace: Trace<HierarchyTreeNode>;
  let positionUpdate: TracePositionUpdate;
  let presenter: Presenter;
  let uiData: UiData;
  let selectedTree: UiHierarchyTreeNode;

  beforeAll(async () => {
    trace = new TraceBuilder<HierarchyTreeNode>()
      .setEntries([await UnitTestUtils.getWindowManagerState()])
      .build();

    const firstEntry = trace.getEntry(0);
    positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    const firstEntryDataTree = await firstEntry.getValue();
    selectedTree = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs((node) => node.id.includes('2088ac1')),
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
      NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(0n),
    );
    await presenter.onAppEvent(positionUpdateWithoutTraceEntry);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.tree).toBeFalsy();
  });

  it('processes trace position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    const filteredUiDataRectLabels = uiData.rects
      ?.filter((rect) => rect.isVisible !== undefined)
      .map((rect) => rect.label);
    const hierarchyOpts = uiData.hierarchyUserOptions
      ? Object.keys(uiData.hierarchyUserOptions)
      : null;
    const propertyOpts = uiData.propertiesUserOptions
      ? Object.keys(uiData.propertiesUserOptions)
      : null;
    expect(uiData.highlightedItem?.length).toEqual(0);
    expect(filteredUiDataRectLabels?.length).toEqual(14);
    expect(uiData.displays.map((display) => display.groupId)).toContain(0);
    expect(hierarchyOpts).toBeTruthy();
    expect(propertyOpts).toBeTruthy();

    // does not check specific tree values as tree generation method may change
    expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
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
    expect(oldDataTree.getAllChildren().length).toEqual(1);

    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    const newDataTree = assertDefined(uiData.tree);
    expect(newDataTree.getAllChildren().length).toEqual(72);
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
      'com.google.android.apps.nexuslauncher/.NexusLauncherActivity';
    const id = `Activity 64953af ${longName}`;

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
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(72);

    await presenter.onHierarchyFilterChange('ScreenDecor');
    // All but four layers should be filtered out
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(2);
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
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
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
    await presenter.onSelectedHierarchyTreeChange(selectedTree);
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
    traces.setTrace(TraceType.WINDOW_MANAGER, trace);
    return new Presenter(traces, new MockStorage(), (newData: UiData) => {
      uiData = newData;
    });
  };
});
