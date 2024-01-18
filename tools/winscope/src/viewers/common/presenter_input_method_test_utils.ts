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
 * limitations under the License.d
 */

import {assertDefined} from 'common/assert_utils';
import {TracePositionUpdate} from 'messaging/winscope_event';
import {HierarchyTreeBuilderLegacy} from 'test/unit/hierarchy_tree_builder_legacy';
import {MockStorage} from 'test/unit/mock_storage';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {
  HierarchyTreeNodeLegacy,
  PropertiesTreeNodeLegacy,
} from 'viewers/common/ui_tree_utils_legacy';
import {UserOptions} from 'viewers/common/user_options';
import {PresenterInputMethodClients} from 'viewers/viewer_input_method_clients/presenter_input_method_clients';
import {PresenterInputMethodManagerService} from 'viewers/viewer_input_method_manager_service/presenter_input_method_manager_service';
import {PresenterInputMethodService} from 'viewers/viewer_input_method_service/presenter_input_method_service';
import {PresenterInputMethod} from './presenter_input_method';

export function executePresenterInputMethodTests(
  selected: HierarchyTreeNodeLegacy,
  propertiesTreeFilterString: string,
  expectedChildren: [number, number],
  expectHierarchyTreeWithSfSubtree: boolean,
  PresenterInputMethod:
    | typeof PresenterInputMethodClients
    | typeof PresenterInputMethodService
    | typeof PresenterInputMethodManagerService,
  imeTraceType: TraceType
) {
  describe('PresenterInputMethod', () => {
    let presenter: PresenterInputMethod;
    let uiData: ImeUiData;
    let positionUpdate: TracePositionUpdate;
    let selectedTree: HierarchyTreeNodeLegacy;

    beforeEach(async () => {
      selectedTree = selected;
      await setUpTestEnvironment([
        imeTraceType,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ]);
    });

    it('is robust to empty trace', async () => {
      const traces = new TracesBuilder().setEntries(imeTraceType, []).build();
      presenter = createPresenter(traces);

      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeFalsy();

      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeFalsy();
    });

    it('is robust to traces without SF', async () => {
      await setUpTestEnvironment([imeTraceType, TraceType.WINDOW_MANAGER]);
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('is robust to traces without WM', async () => {
      await setUpTestEnvironment([imeTraceType, TraceType.SURFACE_FLINGER]);
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('is robust to traces without WM and SF', async () => {
      await setUpTestEnvironment([imeTraceType]);
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('processes trace position updates', async () => {
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('can update pinned items', () => {
      expect(uiData.pinnedItems).toEqual([]);
      const pinnedItem = new HierarchyTreeBuilderLegacy()
        .setName('FirstPinnedItem')
        .setStableId('TestItem 4')
        .setLayerId(4)
        .build();
      presenter.onPinnedItemChange(pinnedItem);
      expect(uiData.pinnedItems).toContain(pinnedItem);
    });

    it('can update highlighted item', () => {
      expect(uiData.highlightedItem).toEqual('');
      const id = 'entry';
      presenter.onHighlightedItemChange(id);
      expect(uiData.highlightedItem).toBe(id);
    });

    it('can update hierarchy tree', async () => {
      //change flat view to true
      const userOptions: UserOptions = {
        onlyVisible: {
          name: 'Only visible',
          enabled: true,
        },
        simplifyNames: {
          name: 'Simplify names',
          enabled: true,
        },
        flat: {
          name: 'Flat',
          enabled: false,
        },
      };

      let expectedChildren = expectHierarchyTreeWithSfSubtree ? 2 : 1;
      await presenter.onAppEvent(positionUpdate);
      expect(assertDefined(uiData.tree).children.length + uiData.sfSubtrees.length).toEqual(
        expectedChildren
      );

      // Filter out non-visible child
      expectedChildren = expectHierarchyTreeWithSfSubtree ? 1 : 0;
      presenter.onHierarchyUserOptionsChange(userOptions);
      expect(uiData.hierarchyUserOptions).toEqual(userOptions);
      expect(assertDefined(uiData.tree).children.length + uiData.sfSubtrees.length).toEqual(
        expectedChildren
      );
    });

    it('can filter hierarchy tree', async () => {
      const userOptions: UserOptions = {
        onlyVisible: {
          name: 'Only visible',
          enabled: false,
        },
        simplifyNames: {
          name: 'Simplify names',
          enabled: true,
        },
        flat: {
          name: 'Flat',
          enabled: true,
        },
      };

      const expectedChildren = expectHierarchyTreeWithSfSubtree ? 11 : 1;
      await presenter.onAppEvent(positionUpdate);
      presenter.onHierarchyUserOptionsChange(userOptions);
      let subtreeChildren = 0;
      uiData.sfSubtrees.forEach((subtree) => (subtreeChildren += subtree.getAllChildren().length));
      expect(assertDefined(uiData.tree).children.length + subtreeChildren).toEqual(
        expectedChildren
      );

      // Filter out all children
      presenter.onHierarchyFilterChange('Reject all');
      subtreeChildren = 0;
      uiData.sfSubtrees.forEach((subtree) => (subtreeChildren += subtree.getAllChildren().length));
      expect(assertDefined(uiData.tree).children.length + subtreeChildren).toEqual(0);
    });

    it('can set new properties tree and associated ui data', async () => {
      await presenter.onAppEvent(positionUpdate);
      presenter.onSelectedHierarchyTreeChange(selectedTree);
      // does not check specific tree values as tree transformation method may change
      expect(uiData.propertiesTree).toBeTruthy();
    });

    it('can filter properties tree', async () => {
      await presenter.onAppEvent(positionUpdate);
      presenter.onSelectedHierarchyTreeChange(selectedTree);
      let nonTerminalChildren =
        (uiData.propertiesTree as PropertiesTreeNodeLegacy)?.children?.filter(
          (child: PropertiesTreeNodeLegacy) => typeof child.propertyKey === 'string'
        ) ?? [];

      expect(nonTerminalChildren.length).toEqual(expectedChildren[0]);
      presenter.onPropertiesFilterChange(propertiesTreeFilterString);

      nonTerminalChildren =
        (uiData.propertiesTree as PropertiesTreeNodeLegacy)?.children?.filter(
          (child: PropertiesTreeNodeLegacy) => typeof child.propertyKey === 'string'
        ) ?? [];
      expect(nonTerminalChildren.length).toEqual(expectedChildren[1]);
    });

    async function setUpTestEnvironment(traceTypes: TraceType[]) {
      const traces = new Traces();
      const entries = await UnitTestUtils.getImeTraceEntries();

      traceTypes.forEach((traceType) => {
        const trace = new TraceBuilder<object>()
          .setEntries([entries.get(traceType)])
          .setFrame(0, 0)
          .build();
        traces.setTrace(traceType, trace);
      });

      presenter = createPresenter(traces);

      const entry = assertDefined(traces.getTrace(imeTraceType)).getEntry(0);
      positionUpdate = TracePositionUpdate.fromTraceEntry(entry);
    }

    function createPresenter(traces: Traces): PresenterInputMethod {
      return new PresenterInputMethod(
        traces,
        new MockStorage(),
        [imeTraceType],
        (newData: ImeUiData) => {
          uiData = newData;
        }
      );
    }
  });
}
