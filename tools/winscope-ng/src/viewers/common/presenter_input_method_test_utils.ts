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

import {TraceType} from "common/trace/trace_type";
import {PresenterInputMethod} from "./presenter_input_method";
import {HierarchyTreeBuilder} from "test/unit/hierarchy_tree_builder";
import {UnitTestUtils} from "test/unit/utils";
import {ImeUiData} from "viewers/common/ime_ui_data";
import {HierarchyTreeNode, PropertiesTreeNode} from "viewers/common/ui_tree_utils";
import {UserOptions} from "viewers/common/user_options";
import {PresenterInputMethodClients} from "viewers/viewer_input_method_clients/presenter_input_method_clients";
import {PresenterInputMethodService} from "viewers/viewer_input_method_service/presenter_input_method_service";
import {PresenterInputMethodManagerService} from "viewers/viewer_input_method_manager_service/presenter_input_method_manager_service";

export function executePresenterInputMethodTests(
  selected: HierarchyTreeNode,
  propertiesTreeFilterString: string,
  expectedChildren: [number, number],
  expectHierarchyTreeWithSfSubtree: boolean,
  PresenterInputMethod: typeof PresenterInputMethodClients | typeof PresenterInputMethodService | typeof PresenterInputMethodManagerService,
  traceType: TraceType,
) {
  describe("PresenterInputMethod", () => {
    let presenter: PresenterInputMethod;
    let uiData: ImeUiData;
    let entries: Map<TraceType, any>;
    let selectedTree: HierarchyTreeNode;

    beforeEach(async () => {
      entries = await UnitTestUtils.getImeTraceEntries();
      selectedTree = selected;
      presenter = new PresenterInputMethod((newData: ImeUiData) => {
        uiData = newData;
      }, [traceType]);
    });

    it("can notify current trace entries", () => {
      presenter.notifyCurrentTraceEntries(entries);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it("is robust to trace entry without SF", () => {
      entries.delete(TraceType.SURFACE_FLINGER);
      presenter.notifyCurrentTraceEntries(entries);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it("is robust to trace entry without WM", () => {
      entries.delete(TraceType.WINDOW_MANAGER);
      presenter.notifyCurrentTraceEntries(entries);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it("is robust to trace entry without WM and SF", () => {
      entries.delete(TraceType.SURFACE_FLINGER);
      entries.delete(TraceType.WINDOW_MANAGER);
      presenter.notifyCurrentTraceEntries(entries);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
    });

    it("can handle unavailable trace entry", () => {
      presenter.notifyCurrentTraceEntries(entries);
      expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
      const emptyEntries = new Map<TraceType, any>();
      presenter.notifyCurrentTraceEntries(emptyEntries);
      expect(uiData.tree).toBeFalsy();
    });

    it("can update pinned items", () => {
      expect(uiData.pinnedItems).toEqual([]);
      const pinnedItem = new HierarchyTreeBuilder().setName("FirstPinnedItem")
        .setStableId("TestItem 4").setLayerId(4).build();
      presenter.updatePinnedItems(pinnedItem);
      expect(uiData.pinnedItems).toContain(pinnedItem);
    });

    it("can update highlighted items", () => {
      expect(uiData.highlightedItems).toEqual([]);
      const id = "entry";
      presenter.updateHighlightedItems(id);
      expect(uiData.highlightedItems).toContain(id);
    });

    it("can update hierarchy tree", () => {
      //change flat view to true
      const userOptions: UserOptions = {
        onlyVisible: {
          name: "Only visible",
          enabled: true
        },
        simplifyNames: {
          name: "Simplify names",
          enabled: true
        },
        flat: {
          name: "Flat",
          enabled: false
        }
      };

      let expectedChildren = expectHierarchyTreeWithSfSubtree ? 2 : 1;
      presenter.notifyCurrentTraceEntries(entries);
      expect(uiData.tree?.children.length).toEqual(expectedChildren);

      // Filter out non-visible child
      expectedChildren = expectHierarchyTreeWithSfSubtree ? 1 : 0;
      presenter.updateHierarchyTree(userOptions);
      expect(uiData.hierarchyUserOptions).toEqual(userOptions);
      expect(uiData.tree?.children.length).toEqual(expectedChildren);
    });

    it("can filter hierarchy tree", () => {
      const userOptions: UserOptions = {
        onlyVisible: {
          name: "Only visible",
          enabled: false
        },
        simplifyNames: {
          name: "Simplify names",
          enabled: true
        },
        flat: {
          name: "Flat",
          enabled: true
        }
      };

      const expectedChildren = expectHierarchyTreeWithSfSubtree ? 12 : 1;
      presenter.notifyCurrentTraceEntries(entries);
      presenter.updateHierarchyTree(userOptions);
      expect(uiData.tree?.children.length).toEqual(expectedChildren);

      // Filter out all children
      presenter.filterHierarchyTree("Reject all");
      expect(uiData.tree?.children.length).toEqual(0);
    });

    it("can set new properties tree and associated ui data", () => {
      presenter.notifyCurrentTraceEntries(entries);
      presenter.newPropertiesTree(selectedTree);
      // does not check specific tree values as tree transformation method may change
      expect(uiData.propertiesTree).toBeTruthy();
    });

    it("can filter properties tree", () => {
      presenter.notifyCurrentTraceEntries(entries);
      presenter.newPropertiesTree(selectedTree);
      let nonTerminalChildren = uiData.propertiesTree?.children?.filter(
        (child: PropertiesTreeNode) => typeof child.propertyKey === "string"
      ) ?? [];

      expect(nonTerminalChildren.length).toEqual(expectedChildren[0]);
      presenter.filterPropertiesTree(propertiesTreeFilterString);

      nonTerminalChildren = uiData.propertiesTree?.children?.filter(
        (child: PropertiesTreeNode) => typeof child.propertyKey === "string"
      ) ?? [];
      expect(nonTerminalChildren.length).toEqual(expectedChildren[1]);
    });
  });
}
