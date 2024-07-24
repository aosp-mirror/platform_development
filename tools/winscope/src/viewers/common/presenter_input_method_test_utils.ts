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
import {MockStorage} from 'test/unit/mock_storage';
import {TracesBuilder} from 'test/unit/traces_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {Traces} from 'trace/traces';
import {ImeTraceType, TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {UserOptions} from 'viewers/common/user_options';
import {PresenterInputMethodClients} from 'viewers/viewer_input_method_clients/presenter_input_method_clients';
import {PresenterInputMethodManagerService} from 'viewers/viewer_input_method_manager_service/presenter_input_method_manager_service';
import {PresenterInputMethodService} from 'viewers/viewer_input_method_service/presenter_input_method_service';
import {PresenterInputMethod} from './presenter_input_method';
import {UiHierarchyTreeNode} from './ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from './ui_property_tree_node';

export function executePresenterInputMethodTests(
  selected: HierarchyTreeNode,
  propertiesTreeFilterString: string,
  expectedChildren: number[],
  expectHierarchyTreeWithSfSubtree: boolean,
  PresenterInputMethod:
    | typeof PresenterInputMethodClients
    | typeof PresenterInputMethodService
    | typeof PresenterInputMethodManagerService,
  imeTraceType: ImeTraceType,
  selectedPropertyTree?: PropertyTreeNode,
) {
  describe('PresenterInputMethod', () => {
    let presenter: PresenterInputMethod;
    let uiData: ImeUiData;
    let positionUpdate: TracePositionUpdate;
    let selectedTree: UiHierarchyTreeNode;
    let traces: Traces;
    let entries: Map<TraceType, HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
      await loadTraces();
    });

    it('is robust to empty trace', async () => {
      const traceWithEntries = new TraceBuilder<HierarchyTreeNode>()
        .setEntries([assertDefined(entries.get(imeTraceType))])
        .setFrame(0, 0)
        .build();
      const traces = new TracesBuilder().setEntries(imeTraceType, []).build();
      presenter = createPresenter(traces);

      const entry = traceWithEntries.getEntry(0);
      positionUpdate = TracePositionUpdate.fromTraceEntry(entry);
      selectedTree = UiHierarchyTreeNode.from(selected);

      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeUndefined();

      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeUndefined();
    });

    it('is robust to traces without SF', async () => {
      setUpPresenter([imeTraceType, TraceType.WINDOW_MANAGER]);
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeDefined();
    });

    it('is robust to traces without WM', async () => {
      setUpPresenter([imeTraceType, TraceType.SURFACE_FLINGER]);
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeDefined();
    });

    it('is robust to traces without WM and SF', async () => {
      setUpPresenter([imeTraceType]);
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeDefined();
    });

    it('processes trace position updates', async () => {
      setUpPresenter([imeTraceType]);
      await presenter.onAppEvent(positionUpdate);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(uiData.tree).toBeDefined();
    });

    it('can update pinned items', () => {
      setUpPresenter([imeTraceType]);
      expect(uiData.pinnedItems).toEqual([]);
      const pinnedItem = TreeNodeUtils.makeUiHierarchyNode({
        id: 'TestItem 4',
        name: 'FirstPinnedItem',
      });
      presenter.onPinnedItemChange(pinnedItem);
      expect(uiData.pinnedItems).toContain(pinnedItem);
    });

    it('can update highlighted item', () => {
      setUpPresenter([imeTraceType]);
      expect(uiData.highlightedItem).toEqual('');
      const id = 'entry';
      presenter.onHighlightedItemChange(id);
      expect(uiData.highlightedItem).toEqual(id);
    });

    it('flattens hierarchy tree', async () => {
      setUpPresenter([imeTraceType, TraceType.SURFACE_FLINGER]);
      //change flat view to true
      const userOptions: UserOptions = {
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
      uiData.sfSubtrees?.forEach((tree) =>
        expect(tree.getAllChildren().length).toEqual(1),
      );

      presenter.onHierarchyUserOptionsChange(userOptions);
      expect(uiData.hierarchyUserOptions).toEqual(userOptions);
      uiData.sfSubtrees?.forEach((tree) =>
        expect(tree.getAllChildren().length).toEqual(10),
      );
    });

    it('can filter hierarchy tree', async () => {
      setUpPresenter([
        imeTraceType,
        TraceType.SURFACE_FLINGER,
        TraceType.WINDOW_MANAGER,
      ]);
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
      uiData.sfSubtrees.forEach(
        (subtree) => (subtreeChildren += subtree.getAllChildren().length),
      );
      expect(
        assertDefined(uiData.tree).getAllChildren().length + subtreeChildren,
      ).toEqual(expectedChildren);

      // Filter out all children
      presenter.onHierarchyFilterChange('Reject all');
      subtreeChildren = 0;
      uiData.sfSubtrees.forEach(
        (subtree) => (subtreeChildren += subtree.getAllChildren().length),
      );
      expect(
        assertDefined(uiData.tree).getAllChildren().length + subtreeChildren,
      ).toEqual(0);
    });

    it('can set new properties tree and associated ui data', async () => {
      setUpPresenter([imeTraceType]);
      expect(uiData.propertiesTree).toBeUndefined();
      await presenter.onAppEvent(positionUpdate);
      await presenter.onSelectedHierarchyTreeChange(selectedTree);
      const propertiesTree = assertDefined(uiData.propertiesTree);
      expect(propertiesTree.name).toEqual(selectedTree.name);
    });

    it('can show/hide defaults in properties tree', async () => {
      setUpPresenter([imeTraceType]);
      await presenter.onAppEvent(positionUpdate);
      await presenter.onSelectedHierarchyTreeChange(selectedTree);
      expect(
        assertDefined(uiData.propertiesTree).getAllChildren().length,
      ).toEqual(expectedChildren[0]);
      await presenter.onPropertiesUserOptionsChange({
        showDefaults: {
          name: 'Show defaults',
          enabled: true,
        },
      });
      expect(
        assertDefined(uiData.propertiesTree).getAllChildren().length,
      ).toEqual(expectedChildren[2]);
    });

    it('can filter properties tree', async () => {
      setUpPresenter([imeTraceType]);
      await presenter.onAppEvent(positionUpdate);
      await presenter.onSelectedHierarchyTreeChange(selectedTree);
      expect(
        assertDefined(uiData.propertiesTree).getAllChildren().length,
      ).toEqual(expectedChildren[0]);
      await presenter.onPropertiesFilterChange(propertiesTreeFilterString);

      expect(
        assertDefined(uiData.propertiesTree).getAllChildren().length,
      ).toEqual(expectedChildren[1]);
    });

    it('can set new additional properties tree and associated ui data from hierarchy tree node', async () => {
      setUpPresenter([imeTraceType]);
      expect(uiData.propertiesTree).toBeUndefined();
      await presenter.onAppEvent(positionUpdate);
      await presenter.onAdditionalPropertySelected({
        name: 'Test Tree',
        treeNode: selectedTree,
      });
      const propertiesTree = assertDefined(uiData.propertiesTree);
      expect(propertiesTree.getDisplayName()).toEqual('Test Tree');
      expect(uiData.highlightedItem).toEqual(selectedTree.id);
    });

    it('can set new additional properties tree and associated ui data from property tree node', async () => {
      if (!selectedPropertyTree) {
        return;
      }
      setUpPresenter([imeTraceType]);
      expect(uiData.propertiesTree).toBeUndefined();
      await presenter.onAppEvent(positionUpdate);
      await presenter.onAdditionalPropertySelected({
        name: 'Additional Properties Tree',
        treeNode: selectedPropertyTree,
      });
      const propertiesTree = assertDefined(uiData.propertiesTree);
      expect(propertiesTree.getDisplayName()).toEqual(
        'Additional Properties Tree',
      );
      expect(propertiesTree).toEqual(
        UiPropertyTreeNode.from(selectedPropertyTree),
      );
      expect(uiData.highlightedItem).toEqual(selectedPropertyTree.id);
    });

    async function loadTraces() {
      traces = new Traces();
      entries = await UnitTestUtils.getImeTraceEntries();
    }

    function setUpPresenter(traceTypes: TraceType[]) {
      traceTypes.forEach((traceType) => {
        const trace = new TraceBuilder<HierarchyTreeNode>()
          .setEntries([assertDefined(entries.get(traceType))])
          .setFrame(0, 0)
          .build();

        traces.setTrace(traceType, trace);
      });
      presenter = createPresenter(traces);

      const entry = assertDefined(traces.getTrace(imeTraceType)).getEntry(0);
      positionUpdate = TracePositionUpdate.fromTraceEntry(entry);
      selectedTree = UiHierarchyTreeNode.from(selected);
    }

    function createPresenter(traces: Traces): PresenterInputMethod {
      return new PresenterInputMethod(
        traces,
        new MockStorage(),
        [imeTraceType],
        (newData: ImeUiData) => {
          uiData = newData;
        },
      );
    }
  });
}
