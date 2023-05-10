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
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {MockStorage} from 'test/unit/mock_storage';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {Traces} from 'trace/traces';
import {TracePosition} from 'trace/trace_position';
import {TraceType} from 'trace/trace_type';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {HierarchyTreeNode, PropertiesTreeNode} from 'viewers/common/ui_tree_utils';
import {UserOptions} from 'viewers/common/user_options';
import {PresenterInputMethodClients} from 'viewers/viewer_input_method_clients/presenter_input_method_clients';
import {PresenterInputMethodManagerService} from 'viewers/viewer_input_method_manager_service/presenter_input_method_manager_service';
import {PresenterInputMethodService} from 'viewers/viewer_input_method_service/presenter_input_method_service';
import {PresenterInputMethod} from './presenter_input_method';

export function executePresenterInputMethodTests(
  selected: HierarchyTreeNode,
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
    let position: TracePosition;
    let selectedTree: HierarchyTreeNode;

    beforeEach(async () => {
      selectedTree = selected;
      await setUpTestEnvironment([
        imeTraceType,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ]);
    });

    it('is robust to empty trace', () => {
      const traces = new TracesBuilder().setEntries(imeTraceType, []).build();
      presenter = createPresenter(traces);

      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeFalsy();

      presenter.onTracePositionUpdate(position);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeFalsy();
    });

    it('is robust to traces without SF', async () => {
      await setUpTestEnvironment([imeTraceType, TraceType.WINDOW_MANAGER]);
      presenter.onTracePositionUpdate(position);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('is robust to traces without WM', async () => {
      await setUpTestEnvironment([imeTraceType, TraceType.SURFACE_FLINGER]);
      presenter.onTracePositionUpdate(position);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('is robust to traces without WM and SF', async () => {
      await setUpTestEnvironment([imeTraceType]);
      presenter.onTracePositionUpdate(position);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('processes trace position updates', () => {
      presenter.onTracePositionUpdate(position);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it('can update pinned items', () => {
      expect(uiData.pinnedItems).toEqual([]);
      const pinnedItem = new HierarchyTreeBuilder()
        .setName('FirstPinnedItem')
        .setStableId('TestItem 4')
        .setLayerId(4)
        .build();
      presenter.updatePinnedItems(pinnedItem);
      expect(uiData.pinnedItems).toContain(pinnedItem);
    });

    it('can update highlighted items', () => {
      expect(uiData.highlightedItems).toEqual([]);
      const id = 'entry';
      presenter.updateHighlightedItems(id);
      expect(uiData.highlightedItems).toContain(id);
    });

    it('can update hierarchy tree', () => {
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
      presenter.onTracePositionUpdate(position);
      expect(uiData.tree?.children.length).toEqual(expectedChildren);

      // Filter out non-visible child
      expectedChildren = expectHierarchyTreeWithSfSubtree ? 1 : 0;
      presenter.updateHierarchyTree(userOptions);
      expect(uiData.hierarchyUserOptions).toEqual(userOptions);
      expect(uiData.tree?.children.length).toEqual(expectedChildren);
    });

    it('can filter hierarchy tree', () => {
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

      const expectedChildren = expectHierarchyTreeWithSfSubtree ? 12 : 1;
      presenter.onTracePositionUpdate(position);
      presenter.updateHierarchyTree(userOptions);
      expect(uiData.tree?.children.length).toEqual(expectedChildren);

      // Filter out all children
      presenter.filterHierarchyTree('Reject all');
      expect(uiData.tree?.children.length).toEqual(0);
    });

    it('can set new properties tree and associated ui data', () => {
      presenter.onTracePositionUpdate(position);
      presenter.newPropertiesTree(selectedTree);
      // does not check specific tree values as tree transformation method may change
      expect(uiData.propertiesTree).toBeTruthy();
    });

    it('can filter properties tree', () => {
      presenter.onTracePositionUpdate(position);
      presenter.newPropertiesTree(selectedTree);
      let nonTerminalChildren =
        uiData.propertiesTree?.children?.filter(
          (child: PropertiesTreeNode) => typeof child.propertyKey === 'string'
        ) ?? [];

      expect(nonTerminalChildren.length).toEqual(expectedChildren[0]);
      presenter.filterPropertiesTree(propertiesTreeFilterString);

      nonTerminalChildren =
        uiData.propertiesTree?.children?.filter(
          (child: PropertiesTreeNode) => typeof child.propertyKey === 'string'
        ) ?? [];
      expect(nonTerminalChildren.length).toEqual(expectedChildren[1]);
    });

    const setUpTestEnvironment = async (traceTypes: TraceType[]) => {
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

      position = TracePosition.fromTraceEntry(
        assertDefined(traces.getTrace(imeTraceType)).getEntry(0)
      );
    };

    const createPresenter = (traces: Traces): PresenterInputMethod => {
      return new PresenterInputMethod(
        traces,
        new MockStorage(),
        [imeTraceType],
        (newData: ImeUiData) => {
          uiData = newData;
        }
      );
    };
  });
}
