/*
 * Copyright (C) 2024 The Android Open Source Project
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
import {IDENTITY_MATRIX} from 'common/geometry/transform_matrix';
import {InMemoryStorage} from 'common/in_memory_storage';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {
  DarkModeToggled,
  FilterPresetApplyRequest,
  FilterPresetSaveRequest,
  TracePositionUpdate,
} from 'messaging/winscope_event';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {MockPresenter} from 'test/unit/mock_hierarchy_viewer_presenter';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {TextFilter} from 'viewers/common/text_filter';
import {UiRectBuilder} from 'viewers/components/rects/ui_rect_builder';
import {DiffType} from './diff_type';
import {RectShowState} from './rect_show_state';
import {UiDataHierarchy} from './ui_data_hierarchy';
import {UiHierarchyTreeNode} from './ui_hierarchy_tree_node';
import {UserOptions} from './user_options';
import {ViewerEvents} from './viewer_events';

describe('AbstractHierarchyViewerPresenter', () => {
  const timestamp1 = TimestampConverterUtils.makeElapsedTimestamp(1n);
  const timestamp2 = TimestampConverterUtils.makeElapsedTimestamp(2n);
  let uiData: UiDataHierarchy;
  let presenter: MockPresenter;
  let trace: Trace<HierarchyTreeNode>;
  let traces: Traces;
  let positionUpdate: TracePositionUpdate;
  let secondPositionUpdate: TracePositionUpdate;
  let selectedTree: UiHierarchyTreeNode;
  let storage: InMemoryStorage;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
    trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([
        new HierarchyTreeBuilder()
          .setId('Test Trace')
          .setName('entry')
          .setChildren([
            {
              id: '1',
              name: 'p1',
              properties: {isComputedVisible: true, testProp: true},
              children: [
                {id: '3', name: 'c3', properties: {isComputedVisible: true}},
              ],
            },
            {id: '2', name: 'p2', properties: {isComputedVisible: false}},
          ])
          .build(),
        new HierarchyTreeBuilder()
          .setId('Test Trace')
          .setName('entry')
          .setChildren([
            {
              id: '1',
              name: 'p1',
              properties: {isComputedVisible: true, testProp: false},
            },
            {id: '2', name: 'p2'},
          ])
          .build(),
      ])
      .setTimestamps([timestamp1, timestamp2])
      .build();
    selectedTree = UiHierarchyTreeNode.from(
      assertDefined((await trace.getEntry(0).getValue()).getChildByName('p1')),
    );
    positionUpdate = TracePositionUpdate.fromTraceEntry(trace.getEntry(0));
    secondPositionUpdate = TracePositionUpdate.fromTraceEntry(
      trace.getEntry(1),
    );
    traces = new Traces();
    traces.addTrace(trace);
  });

  beforeEach(() => {
    storage = new InMemoryStorage();
    presenter = new MockPresenter(
      trace,
      traces,
      storage,
      (newData) => {
        uiData = newData;
      },
      undefined,
    );
  });

  it('clears ui data before throwing error on corrupted trace', async () => {
    const notifyViewCallback = (newData: UiDataHierarchy) => {
      uiData = newData;
    };
    const trace = new TraceBuilder<HierarchyTreeNode>()
      .setType(TraceType.SURFACE_FLINGER)
      .setEntries([selectedTree])
      .setTimestamps([timestamp1])
      .setIsCorrupted(true)
      .build();
    const traces = new Traces();
    traces.addTrace(trace);
    const presenter = new MockPresenter(
      trace,
      traces,
      new InMemoryStorage(),
      notifyViewCallback,
      undefined,
    );
    initializeRectsPresenter(presenter);

    try {
      await presenter.onAppEvent(
        TracePositionUpdate.fromTraceEntry(trace.getEntry(0)),
      );
      fail('error should be thrown for corrupted trace');
    } catch (e) {
      expect(Object.keys(uiData.hierarchyUserOptions).length).toBeGreaterThan(
        0,
      );
      expect(Object.keys(uiData.propertiesUserOptions).length).toBeGreaterThan(
        0,
      );
      expect(uiData.hierarchyTrees).toBeUndefined();
      expect(uiData.propertiesTree).toBeUndefined();
      expect(uiData.highlightedItem).toEqual('');
      expect(uiData.highlightedProperty).toEqual('');
      expect(uiData.pinnedItems.length).toEqual(0);
      expect(
        Object.keys(assertDefined(uiData?.rectsUserOptions)).length,
      ).toBeGreaterThan(0);
      expect(uiData.rectsToDraw).toEqual([]);
    }
  });

  it('processes trace position updates', async () => {
    initializeRectsPresenter();
    pinNode(selectedTree);
    await presenter.onAppEvent(positionUpdate);

    expect(uiData.highlightedItem?.length).toEqual(0);
    expect(Object.keys(uiData.hierarchyUserOptions).length).toBeGreaterThan(0);
    expect(Object.keys(uiData.propertiesUserOptions).length).toBeGreaterThan(0);
    assertDefined(uiData.hierarchyTrees).forEach((tree) => {
      expect(tree.getAllChildren().length > 0).toBeTrue();
    });
    expect(uiData.pinnedItems.length).toBeGreaterThan(0);
    expect(
      Object.keys(assertDefined(uiData.rectsUserOptions)).length,
    ).toBeGreaterThan(0);
    expect(uiData.rectsToDraw?.length).toBeGreaterThan(0);
    expect(uiData.displays?.length).toBeGreaterThan(0);
  });

  it('adds events listeners', () => {
    const element = document.createElement('div');
    presenter.addEventListeners(element);

    let spy: jasmine.Spy = spyOn(presenter, 'onPinnedItemChange');
    const node = TreeNodeUtils.makeUiHierarchyNode({name: 'test'});
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.HierarchyPinnedChange, {
        detail: {pinnedItem: node},
      }),
    );
    expect(spy).toHaveBeenCalledWith(node);

    spy = spyOn(presenter, 'onHighlightedIdChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.HighlightedIdChange, {
        detail: {id: 'test'},
      }),
    );
    expect(spy).toHaveBeenCalledWith('test');

    spy = spyOn(presenter, 'onHighlightedPropertyChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.HighlightedPropertyChange, {
        detail: {id: 'test'},
      }),
    );
    expect(spy).toHaveBeenCalledWith('test');

    spy = spyOn(presenter, 'onHierarchyUserOptionsChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.HierarchyUserOptionsChange, {
        detail: {userOptions: {}},
      }),
    );
    expect(spy).toHaveBeenCalledWith({});

    spy = spyOn(presenter, 'onHierarchyFilterChange');
    const filter = new TextFilter();
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.HierarchyFilterChange, {detail: filter}),
    );
    expect(spy).toHaveBeenCalledWith(filter);

    spy = spyOn(presenter, 'onPropertiesUserOptionsChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.PropertiesUserOptionsChange, {
        detail: {userOptions: {}},
      }),
    );
    expect(spy).toHaveBeenCalledWith({});

    spy = spyOn(presenter, 'onPropertiesFilterChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.PropertiesFilterChange, {
        detail: filter,
      }),
    );
    expect(spy).toHaveBeenCalledWith(filter);

    spy = spyOn(presenter, 'onHighlightedNodeChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.HighlightedNodeChange, {detail: {node}}),
    );
    expect(spy).toHaveBeenCalledWith(node);

    spy = spyOn(presenter, 'onRectShowStateChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.RectShowStateChange, {
        detail: {rectId: 'test', state: RectShowState.HIDE},
      }),
    );
    expect(spy).toHaveBeenCalledWith('test', RectShowState.HIDE);

    spy = spyOn(presenter, 'onRectsUserOptionsChange');
    element.dispatchEvent(
      new CustomEvent(ViewerEvents.RectsUserOptionsChange, {
        detail: {userOptions: {}},
      }),
    );
    expect(spy).toHaveBeenCalledWith({});
  });

  it('is robust to empty trace', async () => {
    const callback = (newData: UiDataHierarchy) => {
      uiData = newData;
    };
    const trace = UnitTestUtils.makeEmptyTrace(TraceType.WINDOW_MANAGER);
    const traces = new Traces();
    traces.addTrace(trace);
    const presenter = new MockPresenter(
      trace,
      traces,
      new InMemoryStorage(),
      callback,
      undefined,
    );
    presenter.initializeRectsPresenter();

    const positionUpdateWithoutTraceEntry = TracePositionUpdate.fromTimestamp(
      TimestampConverterUtils.makeRealTimestamp(0n),
    );
    await presenter.onAppEvent(positionUpdateWithoutTraceEntry);

    expect(Object.keys(uiData.hierarchyUserOptions).length).toBeGreaterThan(0);
    expect(Object.keys(uiData.propertiesUserOptions).length).toBeGreaterThan(0);
    expect(uiData.hierarchyTrees).toBeUndefined();
    expect(
      Object.keys(assertDefined(uiData?.rectsUserOptions)).length,
    ).toBeGreaterThan(0);
  });

  it('handles filter preset requests', async () => {
    initializeRectsPresenter();
    await presenter.onAppEvent(positionUpdate);
    const saveEvent = new FilterPresetSaveRequest(
      'TestPreset',
      TraceType.TEST_TRACE_STRING,
    );
    expect(storage.get(saveEvent.name)).toBeUndefined();
    await presenter.onAppEvent(saveEvent);
    expect(storage.get(saveEvent.name)).toBeDefined();

    await presenter.onHierarchyFilterChange(new TextFilter('Test Filter'));
    await presenter.onHierarchyUserOptionsChange({});
    await presenter.onPropertiesUserOptionsChange({});
    await presenter.onPropertiesFilterChange(new TextFilter('Test Filter'));
    presenter.onRectsUserOptionsChange({});
    await presenter.onRectShowStateChange(
      assertDefined(uiData.rectsToDraw)[0].id,
      RectShowState.HIDE,
    );
    const currentUiData = uiData;

    const applyEvent = new FilterPresetApplyRequest(
      saveEvent.name,
      TraceType.TEST_TRACE_STRING,
    );
    await presenter.onAppEvent(applyEvent);
    expect(uiData).not.toEqual(currentUiData);
  });

  it('updates dark mode', async () => {
    expect(uiData.isDarkMode).toBeFalse();
    await presenter.onAppEvent(new DarkModeToggled(true));
    expect(uiData.isDarkMode).toBeTrue();
  });

  it('disables show diff if no prev entry available', async () => {
    const userOptions: UserOptions = {
      showDiff: {name: '', enabled: false, isUnavailable: false},
    };
    await presenter.onHierarchyUserOptionsChange(userOptions);
    await presenter.onPropertiesUserOptionsChange(userOptions);
    await presenter.onAppEvent(positionUpdate);
    expect(uiData.hierarchyUserOptions['showDiff'].isUnavailable).toBeTrue();
    expect(uiData.propertiesUserOptions['showDiff'].isUnavailable).toBeTrue();
  });

  it('shows correct hierarchy tree name for entry', async () => {
    const spy = spyOn(
      assertDefined(positionUpdate.position.entry?.getFullTrace()),
      'isDumpWithoutTimestamp',
    );
    spy.and.returnValue(false);
    await presenter.onAppEvent(positionUpdate);
    const entryNode = assertDefined(uiData.hierarchyTrees?.at(0));
    expect(entryNode.getDisplayName()).toContain(
      positionUpdate.position.timestamp.format(),
    );

    pinNode(entryNode);
    spy.and.returnValue(true);
    await presenter.onAppEvent(positionUpdate);
    const newEntryNode = assertDefined(uiData.hierarchyTrees?.at(0));
    expect(newEntryNode.getDisplayName()).toContain('Dump');
    expect(uiData.pinnedItems).toEqual([newEntryNode]);
  });

  it('handles pinned item change', () => {
    expect(uiData.pinnedItems).toEqual([]);
    const item = TreeNodeUtils.makeUiHierarchyNode({id: '', name: ''});
    presenter.onPinnedItemChange(item);
    expect(uiData.pinnedItems).toEqual([item]);
    presenter.onPinnedItemChange(item);
    expect(uiData.pinnedItems).toEqual([]);
  });

  it('updates and applies hierarchy user options', async () => {
    await presenter.onAppEvent(positionUpdate);
    const userOptions: UserOptions = {flat: {name: '', enabled: true}};
    await presenter.onHierarchyUserOptionsChange(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    expect(uiData.hierarchyTrees?.at(0)?.getAllChildren().length).toEqual(3);
  });

  it('updates highlighted property', () => {
    const id = '4';
    presenter.onHighlightedPropertyChange(id);
    expect(uiData.highlightedProperty).toEqual(id);
    presenter.onHighlightedPropertyChange(id);
    expect(uiData.highlightedProperty).toEqual('');
  });

  it('sets properties tree and associated ui data from tree node', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedNodeChange(selectedTree);
    const propertiesTree = assertDefined(uiData.propertiesTree);
    expect(propertiesTree.id).toContain(selectedTree.id);
    expect(propertiesTree.getAllChildren().length).toEqual(2);
  });

  it('updates and applies properties user options, calculating diffs from prev hierarchy tree', async () => {
    await presenter.onAppEvent(positionUpdate);
    await presenter.onHighlightedIdChange(selectedTree.id);
    await presenter.onAppEvent(secondPositionUpdate);
    expect(
      uiData.propertiesTree?.getChildByName('testProp')?.getDiff(),
    ).toEqual(DiffType.NONE);

    const userOptions: UserOptions = {showDiff: {name: '', enabled: true}};
    await presenter.onPropertiesUserOptionsChange(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    expect(
      uiData.propertiesTree?.getChildByName('testProp')?.getDiff(),
    ).toEqual(DiffType.MODIFIED);
  });

  it('is robust to attempts to change rect user data if no rects presenter', async () => {
    expect(() => presenter.onRectsUserOptionsChange({})).not.toThrowError();
    await expectAsync(
      presenter.onRectShowStateChange('', RectShowState.SHOW),
    ).not.toBeRejected();
  });

  it('creates input data for rects view', async () => {
    initializeRectsPresenter();
    await presenter.onAppEvent(positionUpdate);
    const rectsToDraw = assertDefined(uiData.rectsToDraw);
    const expectedFirstRect = presenter.uiRects[0];
    expect(rectsToDraw[0].x).toEqual(expectedFirstRect.x);
    expect(rectsToDraw[0].y).toEqual(expectedFirstRect.y);
    expect(rectsToDraw[0].w).toEqual(expectedFirstRect.w);
    expect(rectsToDraw[0].h).toEqual(expectedFirstRect.h);
    checkRectUiData(uiData, 3, 3, 3);
  });

  it('filters rects by visibility', async () => {
    initializeRectsPresenter();
    const userOptions: UserOptions = {
      showOnlyVisible: {name: '', enabled: false},
    };
    await presenter.onAppEvent(positionUpdate);
    presenter.onRectsUserOptionsChange(userOptions);
    expect(uiData.rectsUserOptions).toEqual(userOptions);
    checkRectUiData(uiData, 3, 3, 3);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(uiData, 2, 3, 2);
  });

  it('filters rects by show/hide state', async () => {
    initializeRectsPresenter();
    const userOptions: UserOptions = {
      ignoreRectShowState: {
        name: 'Ignore',
        icon: 'visibility',
        enabled: true,
      },
    };
    await presenter.onAppEvent(positionUpdate);
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(uiData, 3, 3, 3);

    await presenter.onRectShowStateChange(
      assertDefined(uiData.rectsToDraw)[0].id,
      RectShowState.HIDE,
    );
    checkRectUiData(uiData, 3, 3, 2);

    userOptions['ignoreRectShowState'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(uiData, 2, 3, 2);
  });

  it('handles both visibility and show/hide state in rects', async () => {
    initializeRectsPresenter();
    const userOptions: UserOptions = {
      ignoreRectShowState: {name: '', enabled: true},
      showOnlyVisible: {name: '', enabled: false},
    };
    presenter.onRectsUserOptionsChange(userOptions);
    await presenter.onAppEvent(positionUpdate);
    checkRectUiData(uiData, 3, 3, 3);

    await presenter.onRectShowStateChange(
      assertDefined(uiData.rectsToDraw)[0].id,
      RectShowState.HIDE,
    );
    checkRectUiData(uiData, 3, 3, 2);

    userOptions['ignoreRectShowState'].enabled = false;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(uiData, 2, 3, 2);

    userOptions['showOnlyVisible'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(uiData, 1, 3, 1);

    userOptions['ignoreRectShowState'].enabled = true;
    presenter.onRectsUserOptionsChange(userOptions);
    checkRectUiData(uiData, 2, 3, 1);
  });

  function pinNode(node: UiHierarchyTreeNode) {
    presenter.onPinnedItemChange(node);
    expect(uiData.pinnedItems).toEqual([node]);
  }

  function initializeRectsPresenter(p = presenter) {
    p.initializeRectsPresenter();
    p.uiRects = [
      new UiRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setLabel('test rect')
        .setTransform(IDENTITY_MATRIX)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsActiveDisplay(true)
        .setId('1 p1')
        .setGroupId(0)
        .setIsClickable(true)
        .setCornerRadius(0)
        .setDepth(0)
        .build(),
      new UiRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setLabel('test rect 2')
        .setTransform(IDENTITY_MATRIX)
        .setIsVisible(true)
        .setIsDisplay(false)
        .setIsActiveDisplay(true)
        .setId('3 c3')
        .setGroupId(0)
        .setIsClickable(true)
        .setCornerRadius(0)
        .setDepth(1)
        .build(),
      new UiRectBuilder()
        .setX(0)
        .setY(0)
        .setWidth(1)
        .setHeight(1)
        .setLabel('test rect 3')
        .setTransform(IDENTITY_MATRIX)
        .setIsVisible(false)
        .setIsDisplay(false)
        .setIsActiveDisplay(true)
        .setId('2 p2')
        .setGroupId(0)
        .setIsClickable(true)
        .setCornerRadius(0)
        .setDepth(2)
        .build(),
    ];
    p.displays = [{displayId: 0, groupId: 0, name: 'Display', isActive: true}];
  }

  function checkRectUiData(
    uiData: UiDataHierarchy,
    rectsToDraw: number,
    allRects: number,
    shownRects: number,
  ) {
    expect(assertDefined(uiData.rectsToDraw).length).toEqual(rectsToDraw);
    const showStates = Array.from(
      assertDefined(uiData.rectIdToShowState).values(),
    );
    expect(showStates.length).toEqual(allRects);
    expect(showStates.filter((s) => s === RectShowState.SHOW).length).toEqual(
      shownRects,
    );
  }
});
