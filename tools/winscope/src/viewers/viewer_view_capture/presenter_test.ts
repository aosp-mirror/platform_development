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

import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {MockStorage} from 'test/unit/mock_storage';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Point} from 'trace/flickerlib/common';
import {Parser} from 'trace/parser';
import {RealTimestamp, TimestampType} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter} from 'viewers/viewer_view_capture/presenter';
import {UiData} from 'viewers/viewer_view_capture/ui_data';

describe('PresenterViewCapture', () => {
  let parser: Parser<object>;
  let trace: Trace<object>;
  let uiData: UiData;
  let presenter: Presenter;
  let position: TracePosition;
  let selectedTree: HierarchyTreeNode;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc'
    );
    trace = new TraceBuilder<object>()
      .setEntries([
        parser.getEntry(0, TimestampType.REAL),
        parser.getEntry(1, TimestampType.REAL),
        parser.getEntry(2, TimestampType.REAL),
      ])
      .build();
    position = TracePosition.fromTraceEntry(trace.getEntry(0));
    selectedTree = new HierarchyTreeBuilder()
      .setName('Name@Id')
      .setStableId('stableId')
      .setKind('kind')
      .setDiffType('diff type')
      .setId(53)
      .build();
  });

  beforeEach(async () => {
    presenter = createPresenter(trace);
  });

  it('is robust to empty trace', async () => {
    const emptyTrace = new TraceBuilder<object>().setEntries([]).build();
    const presenter = createPresenter(emptyTrace);

    const positionWithoutTraceEntry = TracePosition.fromTimestamp(new RealTimestamp(0n));
    await presenter.onTracePositionUpdate(positionWithoutTraceEntry);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.tree).toBeFalsy();
  });

  it('processes trace position updates', async () => {
    await presenter.onTracePositionUpdate(position);

    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.highlightedItems?.length).toEqual(0);
    const hierarchyOpts = Object.keys(uiData.hierarchyUserOptions);
    expect(hierarchyOpts).toBeTruthy();
    const propertyOpts = Object.keys(uiData.propertiesUserOptions);
    expect(propertyOpts).toBeTruthy();
    expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
  });

  it('creates input data for rects view', async () => {
    await presenter.onTracePositionUpdate(position);
    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.rects[0].topLeft).toEqual(new Point(0, 0));
    expect(uiData.rects[0].bottomRight).toEqual(new Point(1080, 2340));
  });

  it('updates pinned items', async () => {
    const pinnedItem = new HierarchyTreeBuilder().setName('FirstPinnedItem').setId('id').build();
    await presenter.onTracePositionUpdate(position);
    presenter.updatePinnedItems(pinnedItem);
    expect(uiData.pinnedItems).toContain(pinnedItem);
  });

  it('updates highlighted items', async () => {
    expect(uiData.highlightedItems).toEqual([]);

    const id = '4';
    await presenter.onTracePositionUpdate(position);
    presenter.updateHighlightedItems(id);
    expect(uiData.highlightedItems).toContain(id);
  });

  it('updates hierarchy tree', async () => {
    await presenter.onTracePositionUpdate(position);

    expect(
      // DecorView -> LinearLayout -> FrameLayout -> LauncherRootView -> DragLayer -> Workspace
      uiData.tree?.children[0].children[1].children[0].children[0].children[1].id
    ).toEqual('com.android.launcher3.Workspace@251960479');

    const userOptions: UserOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: true,
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
    presenter.updateHierarchyTree(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    expect(
      // DecorView -> LinearLayout -> FrameLayout (before, this was the 2nd child) -> LauncherRootView -> DragLayer -> Workspace if filter works as expected
      uiData.tree?.children[0].children[0].children[0].children[0].children[1].id
    ).toEqual('com.android.launcher3.Workspace@251960479');
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
    await presenter.onTracePositionUpdate(position);
    presenter.updateHierarchyTree(userOptions);
    presenter.filterHierarchyTree('Workspace');

    expect(
      // DecorView -> LinearLayout -> FrameLayout -> LauncherRootView -> DragLayer -> Workspace if filter works as expected
      uiData.tree?.children[0].children[0].children[0].children[0].children[0].id
    ).toEqual('com.android.launcher3.Workspace@251960479');
  });

  it('sets properties tree and associated ui data', async () => {
    await presenter.onTracePositionUpdate(position);
    presenter.newPropertiesTree(selectedTree);
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

    await presenter.onTracePositionUpdate(position);
    presenter.newPropertiesTree(selectedTree);
    expect(uiData.propertiesTree?.diffType).toBeFalsy();

    presenter.updatePropertiesTree(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(uiData.propertiesTree?.diffType).toBeTruthy();
  });

  it('filters properties tree', async () => {
    await presenter.onTracePositionUpdate(position);

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
    presenter.updatePropertiesTree(userOptions);
    let nonTerminalChildren = uiData.propertiesTree?.children?.filter(
      (it) => typeof it.propertyKey === 'string'
    );
    expect(nonTerminalChildren?.length).toEqual(24);
    presenter.filterPropertiesTree('alpha');

    nonTerminalChildren = uiData.propertiesTree?.children?.filter(
      (it) => typeof it.propertyKey === 'string'
    );
    expect(nonTerminalChildren?.length).toEqual(1);
  });

  const createPresenter = (trace: Trace<object>): Presenter => {
    const traces = new Traces();
    traces.setTrace(TraceType.VIEW_CAPTURE, trace);
    return new Presenter(traces, new MockStorage(), (newData: UiData) => {
      uiData = newData;
    });
  };
});
