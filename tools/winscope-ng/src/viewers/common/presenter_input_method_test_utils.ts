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
import { ImeUiData } from "viewers/common/ime_ui_data";
import { UserOptions } from "viewers/common/user_options";
import { TraceType } from "common/trace/trace_type";
import { HierarchyTreeNode, PropertiesTreeNode } from "viewers/common/ui_tree_utils";
import { HierarchyTreeBuilder } from "test/unit/hierarchy_tree_builder";
import { PresenterInputMethod } from "./presenter_input_method";
import { PresenterInputMethodClients } from "viewers/viewer_input_method_clients/presenter_input_method_clients";
import { PresenterInputMethodService } from "viewers/viewer_input_method_service/presenter_input_method_service";
import { PresenterInputMethodManagerService } from "viewers/viewer_input_method_manager_service/presenter_input_method_manager_service";

export function executePresenterInputMethodTests(
  getEntry: () => any,
  selected: HierarchyTreeNode,
  propertiesTreeFilterString: string,
  expectedChildren: [number, number],
  PresenterInputMethod: typeof PresenterInputMethodClients | typeof PresenterInputMethodService | typeof PresenterInputMethodManagerService,
  traceType: TraceType,
) {
  describe("PresenterInputMethod", () => {
    let presenter: PresenterInputMethod;
    let uiData: ImeUiData;
    let entries: Map<TraceType, any>;
    let selectedTree: HierarchyTreeNode;

    beforeAll(async () => {
      const entry = await getEntry();
      entries = new Map<TraceType, any>();
      entries.set(traceType, [entry, null]);
      selectedTree = selected;
    });

    beforeEach(async () => {
      presenter = new PresenterInputMethod((newData: ImeUiData) => {
        uiData = newData;
      }, [traceType]);
    });

    it("can notify current trace entries", () => {
      presenter.notifyCurrentTraceEntries(entries);
      expect(uiData.hierarchyUserOptions).toBeTruthy();
      expect(uiData.propertiesUserOptions).toBeTruthy();

      // does not check specific tree values as tree generation method may change
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

      presenter.notifyCurrentTraceEntries(entries);
      expect(uiData.tree?.children.length).toEqual(1);

      presenter.updateHierarchyTree(userOptions);
      expect(uiData.hierarchyUserOptions).toEqual(userOptions);
      // non visible child filtered out
      expect(uiData.tree?.children.length).toEqual(0);
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
      presenter.notifyCurrentTraceEntries(entries);
      presenter.updateHierarchyTree(userOptions);
      expect(uiData.tree?.children.length).toEqual(1);
      presenter.filterHierarchyTree("Reject all");
      // All children should be filtered out
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
