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

import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {MockStorage} from 'test/unit/mock_storage';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {LayerTraceEntry} from 'trace/flickerlib/layers/LayerTraceEntry';
import {RealTimestamp} from 'trace/timestamp';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {Presenter} from './presenter';
import {UiData} from './ui_data';

describe('PresenterSurfaceFlinger', () => {
  let trace: Trace<LayerTraceEntry>;
  let position: TracePosition;
  let positionMultiDisplayEntry: TracePosition;
  let presenter: Presenter;
  let uiData: UiData;
  let selectedTree: HierarchyTreeNode;

  beforeAll(async () => {
    trace = new TraceBuilder<LayerTraceEntry>()
      .setEntries([
        await UnitTestUtils.getLayerTraceEntry(),
        await UnitTestUtils.getMultiDisplayLayerTraceEntry(),
      ])
      .build();

    position = TracePosition.fromTraceEntry(trace.getEntry(0));
    positionMultiDisplayEntry = TracePosition.fromTraceEntry(trace.getEntry(1));

    selectedTree = new HierarchyTreeBuilder()
      .setName('Dim layer#53')
      .setStableId('EffectLayer 53 Dim layer#53')
      .setFilteredView(true)
      .setKind('53')
      .setDiffType('EffectLayer')
      .setId(53)
      .build();
  });

  beforeEach(async () => {
    presenter = createPresenter(trace);
  });

  it('is robust to empty trace', () => {
    const emptyTrace = new TraceBuilder<LayerTraceEntry>().setEntries([]).build();
    const presenter = createPresenter(emptyTrace);

    const positionWithoutTraceEntry = TracePosition.fromTimestamp(new RealTimestamp(0n));
    presenter.onTracePositionUpdate(positionWithoutTraceEntry);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.tree).toBeFalsy();
  });

  it('processes trace position updates', () => {
    presenter.onTracePositionUpdate(position);

    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.highlightedItems?.length).toEqual(0);
    expect(uiData.displayIds).toContain(0);
    const hierarchyOpts = uiData.hierarchyUserOptions
      ? Object.keys(uiData.hierarchyUserOptions)
      : null;
    expect(hierarchyOpts).toBeTruthy();
    const propertyOpts = uiData.propertiesUserOptions
      ? Object.keys(uiData.propertiesUserOptions)
      : null;
    expect(propertyOpts).toBeTruthy();
    expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
  });

  it('creates input data for rects view', () => {
    presenter.onTracePositionUpdate(position);
    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.rects[0].topLeft).toEqual({x: 0, y: 0});
    expect(uiData.rects[0].bottomRight).toEqual({x: 1080, y: 118});
  });

  it('updates pinned items', () => {
    expect(uiData.pinnedItems).toEqual([]);

    const pinnedItem = new HierarchyTreeBuilder()
      .setName('FirstPinnedItem')
      .setStableId('TestItem 4')
      .setLayerId(4)
      .build();
    presenter.updatePinnedItems(pinnedItem);
    expect(uiData.pinnedItems).toContain(pinnedItem);
  });

  it('updates highlighted items', () => {
    expect(uiData.highlightedItems).toEqual([]);

    const id = '4';
    presenter.updateHighlightedItems(id);
    expect(uiData.highlightedItems).toContain(id);
  });

  it('updates hierarchy tree', () => {
    //change flat view to true
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

    presenter.onTracePositionUpdate(position);
    expect(uiData.tree?.children.length).toEqual(3);

    presenter.updateHierarchyTree(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    // nested children should now be on same level as initial parents
    expect(uiData.tree?.children.length).toEqual(94);
  });

  it('filters hierarchy tree', () => {
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
    presenter.onTracePositionUpdate(position);
    presenter.updateHierarchyTree(userOptions);
    expect(uiData.tree?.children.length).toEqual(94);
    presenter.filterHierarchyTree('Wallpaper');
    // All but four layers should be filtered out
    expect(uiData.tree?.children.length).toEqual(4);
  });

  it('sets properties tree and associated ui data', () => {
    presenter.onTracePositionUpdate(position);
    presenter.newPropertiesTree(selectedTree);
    // does not check specific tree values as tree transformation method may change
    expect(uiData.propertiesTree).toBeTruthy();
  });

  it('updates properties tree', () => {
    //change flat view to true
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

    presenter.onTracePositionUpdate(position);
    presenter.newPropertiesTree(selectedTree);
    expect(uiData.propertiesTree?.diffType).toBeFalsy();

    presenter.updatePropertiesTree(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(uiData.propertiesTree?.diffType).toBeTruthy();
  });

  it('filters properties tree', () => {
    presenter.onTracePositionUpdate(position);
    presenter.newPropertiesTree(selectedTree);
    let nonTerminalChildren =
      uiData.propertiesTree?.children?.filter(
        (child: PropertiesTreeNode) => typeof child.propertyKey === 'string'
      ) ?? [];

    expect(nonTerminalChildren.length).toEqual(22);
    presenter.filterPropertiesTree('bound');

    nonTerminalChildren =
      uiData.propertiesTree?.children?.filter(
        (child: PropertiesTreeNode) => typeof child.propertyKey === 'string'
      ) ?? [];
    expect(nonTerminalChildren.length).toEqual(3);
  });

  it('handles displays with no visible layers', async () => {
    presenter.onTracePositionUpdate(positionMultiDisplayEntry);
    expect(uiData.displayIds.length).toEqual(5);
    // we want the ids to be sorted
    expect(uiData.displayIds).toEqual([0, 2, 3, 4, 5]);
  });

  const createPresenter = (trace: Trace<LayerTraceEntry>): Presenter => {
    const traces = new Traces();
    traces.setTrace(TraceType.SURFACE_FLINGER, trace);
    return new Presenter(traces, new MockStorage(), (newData: UiData) => {
      uiData = newData;
    });
  };
});
