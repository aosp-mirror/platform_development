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
import {InMemoryStorage} from 'common/in_memory_storage';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {TimestampConverterUtils} from 'test/unit/timestamp_converter_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
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

describe('PresenterSurfaceFlinger', () => {
  let traceSf: Trace<HierarchyTreeNode>;
  let positionUpdate: TracePositionUpdate;
  let secondPositionUpdate: TracePositionUpdate;
  let positionUpdateMultiDisplayEntry: TracePositionUpdate;
  let presenter: Presenter;
  let uiData: UiData;
  let selectedTree: UiHierarchyTreeNode;
  let nodeWithRect: UiHierarchyTreeNode;

  beforeAll(async () => {
    traceSf = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([
        await UnitTestUtils.getLayerTraceEntry(0),
        await UnitTestUtils.getMultiDisplayLayerTraceEntry(),
        await UnitTestUtils.getLayerTraceEntry(1),
      ])
      .build();

    const firstEntry = traceSf.getEntry(0);
    positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);
    positionUpdateMultiDisplayEntry = TracePositionUpdate.fromTraceEntry(
      traceSf.getEntry(1),
    );
    secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      traceSf.getEntry(2),
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
    nodeWithRect = UiHierarchyTreeNode.from(
      assertDefined(
        firstEntryDataTree.findDfs(
          UiTreeUtils.makeIdMatchFilter('79 Wallpaper BBQ wrapper#79'),
        ),
      ),
    );
    const rect = assertDefined(nodeWithRect.getRects()?.at(0));
    Object.assign(rect, {isVisible: false});
  });

  beforeEach(() => {
    presenter = createPresenter(traceSf);
  });

  it('is robust to empty trace', async () => {
    const emptyTrace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([])
      .build();
    const presenter = createPresenter(emptyTrace);

    const positionUpdateWithoutTraceEntry = TracePositionUpdate.fromTimestamp(
      TimestampConverterUtils.makeRealTimestamp(0n),
    );
    await presenter.onAppEvent(positionUpdateWithoutTraceEntry);
    expect(uiData.hierarchyUserOptions).toBeTruthy();
    expect(uiData.tree).toBeFalsy();
  });

  it('processes trace position updates', async () => {
    await presenter.onAppEvent(positionUpdate);

    expect(uiData.rectsToDraw.length).toBeGreaterThan(0);
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
    checkRectUiData(7, 7, 7);
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
    checkRectUiData(7, 7, 7);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(6, 7, 6);
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
    checkRectUiData(7, 7, 7);

    await presenter.onRectShowStateChange(nodeWithRect.id, RectShowState.HIDE);
    checkRectUiData(7, 7, 6);

    userOptions['ignoreNonHidden'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(6, 7, 6);
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
    checkRectUiData(7, 7, 7);

    await presenter.onRectShowStateChange(
      '89 StatusBar#89',
      RectShowState.HIDE,
    );
    checkRectUiData(7, 7, 6);

    userOptions['ignoreNonHidden'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(6, 7, 6);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(5, 7, 5);

    userOptions['ignoreNonHidden'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(6, 7, 5);
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
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(94);

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
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(94);

    await presenter.onHierarchyFilterChange('Wallpaper');
    // All but four layers should be filtered out
    expect(assertDefined(uiData.tree).getAllChildren().length).toEqual(4);
  });

  it('sets properties tree and associated ui data from tree node', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.propertiesTree).toBeUndefined();
    await presenter.onHighlightedNodeChange(selectedTree);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toEqual('53 Dim layer#53');
    expect(propertiesTree.getAllChildren().length).toEqual(22);
    expect(assertDefined(uiData.curatedProperties).flags).toEqual(
      'HIDDEN (0x1)',
    );
  });

  it('sets properties tree and associated ui data from rect', async () => {
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.propertiesTree).toBeUndefined();
    const rect = assertDefined(uiData.rectsToDraw.at(4));
    await presenter.onHighlightedIdChange(rect.id);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toEqual('85 NavigationBar0#85');
    expect(propertiesTree.getAllChildren().length).toEqual(28);

    const curatedProperties = assertDefined(uiData.curatedProperties);
    expect(curatedProperties.flags).toEqual('ENABLE_BACKPRESSURE (0x100)');
    expect(curatedProperties.summary).toEqual([
      {
        key: 'Covered by',
        layerValues: [
          {
            layerId: '65',
            nodeId: '65 ScreenDecorOverlayBottom#65',
            name: 'ScreenDecorOverlayBottom#65',
          },
        ],
      },
    ]);
  });

  it('after highlighting a node, updates properties tree on position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    const selectedTree = assertDefined(
      assertDefined(uiData.tree).findDfs(
        UiTreeUtils.makeIdMatchFilter('79 Wallpaper BBQ wrapper#79'),
      ),
    );
    await presenter.onHighlightedNodeChange(selectedTree);
    let propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree
          .getChildByName('metadata')
          ?.getChildByName('2')
          ?.getChildByName('byteOffset'),
      ).formattedValue(),
    ).toEqual('2919');

    await presenter.onAppEvent(secondPositionUpdate);
    propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree
          .getChildByName('metadata')
          ?.getChildByName('2')
          ?.getChildByName('byteOffset'),
      ).formattedValue(),
    ).toEqual('44517');
  });

  it('after highlighting a rect, updates properties tree on position update', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedIdChange('79 Wallpaper BBQ wrapper#79');
    let propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree
          .getChildByName('metadata')
          ?.getChildByName('2')
          ?.getChildByName('byteOffset'),
      ).formattedValue(),
    ).toEqual('2919');

    await presenter.onAppEvent(secondPositionUpdate);
    propertiesTree = assertDefined(uiData.propertiesTree);
    expect(
      assertDefined(
        propertiesTree
          .getChildByName('metadata')
          ?.getChildByName('2')
          ?.getChildByName('byteOffset'),
      ).formattedValue(),
    ).toEqual('44517');
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
    await presenter.onHighlightedNodeChange(selectedTree);
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
    await presenter.onHighlightedNodeChange(selectedTree);
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
    const traceVc = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.VIEW_CAPTURE)
      .setEntries([await UnitTestUtils.getViewCaptureEntry()])
      .setParserCustomQueryResult(CustomQueryType.VIEW_CAPTURE_METADATA, {
        packageName: 'com.google.android.apps.nexuslauncher',
        windowName: 'not_used',
      })
      .build();
    const traces = new Traces();
    traces.addTrace(traceSf);
    traces.addTrace(traceVc);
    const presenter = new Presenter(
      traceSf,
      traces,
      new InMemoryStorage(),
      (newData: UiData) => {
        uiData = newData;
      },
    );

    const firstEntry = traceSf.getEntry(0);
    const positionUpdate = TracePositionUpdate.fromTraceEntry(firstEntry);

    await presenter.onAppEvent(positionUpdate);
    expect(uiData.rectsToDraw.filter((rect) => rect.hasContent).length).toEqual(
      1,
    );
  });

  function createPresenter(trace: Trace<HierarchyTreeNode>): Presenter {
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
  }

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
