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
import { Presenter } from "./presenter";
import { UiData } from "./ui_data";
import { UserOptions } from "viewers/common/user_options";
import { TraceType } from "common/trace/trace_type";
import { WindowManagerState } from "common/trace/flickerlib/common";
import { PropertiesTreeNode, HierarchyTreeNode } from "viewers/common/ui_tree_utils";
import { UnitTestUtils } from "test/unit/utils";
import { HierarchyTreeBuilder } from "test/unit/hierarchy_tree_builder";
import { VISIBLE_CHIP } from "viewers/common/chip";

describe("PresenterWindowManager", () => {
  let presenter: Presenter;
  let uiData: UiData;
  let entries: Map<TraceType, any>;
  let selectedTree: HierarchyTreeNode;

  beforeAll(async () => {
    entries = new Map<TraceType, any>();
    const entry: WindowManagerState = await UnitTestUtils.getWindowManagerState();

    selectedTree = new HierarchyTreeBuilder().setName("ScreenDecorOverlayBottom")
      .setStableId("WindowState 2088ac1 ScreenDecorOverlayBottom").setKind("WindowState")
      .setSimplifyNames(true).setShortName("ScreenDecorOverlayBottom").setLayerId(61)
      .setFilteredView(true).setIsVisible(true).setChips([VISIBLE_CHIP]).build();

    entries.set(TraceType.WINDOW_MANAGER, [entry, null]);
  });

  beforeEach(async () => {
    presenter = new Presenter((newData: UiData) => {
      uiData = newData;
    });
  });

  it("can notify current trace entries", () => {
    presenter.notifyCurrentTraceEntries(entries);
    const filteredUiDataRectLabels = uiData.rects?.filter(rect => rect.isVisible != undefined)
      .map(rect => rect.label);
    const hierarchyOpts = uiData.hierarchyUserOptions ?
      Object.keys(uiData.hierarchyUserOptions) : null;
    const propertyOpts = uiData.propertiesUserOptions ?
      Object.keys(uiData.propertiesUserOptions) : null;
    expect(uiData.highlightedItems?.length).toEqual(0);
    expect(filteredUiDataRectLabels?.length).toEqual(14);
    expect(uiData.displayIds).toContain(0);
    expect(hierarchyOpts).toBeTruthy();
    expect(propertyOpts).toBeTruthy();

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
    presenter.notifyCurrentTraceEntries(entries);
    expect(uiData.pinnedItems).toEqual([]);

    const pinnedItem = new HierarchyTreeBuilder().setName("FirstPinnedItem")
      .setStableId("TestItem 4").setLayerId(4).build();

    presenter.updatePinnedItems(pinnedItem);
    expect(uiData.pinnedItems).toContain(pinnedItem);
  });

  it("can update highlighted items", () => {
    expect(uiData.highlightedItems).toEqual([]);
    const id = "4";
    presenter.updateHighlightedItems(id);
    expect(uiData.highlightedItems).toContain(id);
  });

  it("can update hierarchy tree", () => {
    //change flat view to true
    const userOptions: UserOptions = {
      showDiff: {
        name: "Show diff",
        enabled: false
      },
      simplifyNames: {
        name: "Simplify names",
        enabled: true
      },
      onlyVisible: {
        name: "Only visible",
        enabled: false
      },
      flat: {
        name: "Flat",
        enabled: true
      }
    };

    presenter.notifyCurrentTraceEntries(entries);
    expect(uiData.tree?.children.length).toEqual(1);
    presenter.updateHierarchyTree(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    // nested children should now be on same level initial parent
    expect(uiData.tree?.children.length).toEqual(72);
  });

  it("can filter hierarchy tree", () => {
    const userOptions: UserOptions = {
      showDiff: {
        name: "Show diff",
        enabled: false
      },
      simplifyNames: {
        name: "Simplify names",
        enabled: true
      },
      onlyVisible: {
        name: "Only visible",
        enabled: false
      },
      flat: {
        name: "Flat",
        enabled: true
      }
    };
    presenter.notifyCurrentTraceEntries(entries);
    presenter.updateHierarchyTree(userOptions);
    expect(uiData.tree?.children.length).toEqual(72);
    presenter.filterHierarchyTree("ScreenDecor");
    // All but two window states should be filtered out
    expect(uiData.tree?.children.length).toEqual(2);
  });


  it("can set new properties tree and associated ui data", () => {
    presenter.notifyCurrentTraceEntries(entries);
    presenter.newPropertiesTree(selectedTree);
    // does not check specific tree values as tree transformation method may change
    expect(uiData.propertiesTree).toBeTruthy();
  });

  it("can update properties tree", () => {
    //change flat view to true
    const userOptions: UserOptions = {
      showDiff: {
        name: "Show diff",
        enabled: true
      },
      showDefaults: {
        name: "Show defaults",
        enabled: true,
        tooltip: `
                  If checked, shows the value of all properties.
                  Otherwise, hides all properties whose value is
                  the default for its data type.
                `
      },
    };

    presenter.notifyCurrentTraceEntries(entries);
    presenter.newPropertiesTree(selectedTree);
    expect(uiData.propertiesTree?.diffType).toBeFalsy();
    presenter.updatePropertiesTree(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    //check that diff type added
    expect(uiData.propertiesTree?.diffType).toBeTruthy();
  });

  it("can filter properties tree", () => {
    presenter.notifyCurrentTraceEntries(entries);
    presenter.newPropertiesTree(selectedTree);

    let nonTerminalChildren = uiData.propertiesTree?.children?.filter(
      (child: PropertiesTreeNode) => typeof child.propertyKey === "string"
    ) ?? [];

    expect(nonTerminalChildren.length).toEqual(45);
    presenter.filterPropertiesTree("visible");

    nonTerminalChildren = uiData.propertiesTree?.children?.filter(
      (child: PropertiesTreeNode) => typeof child.propertyKey === "string"
    ) ?? [];
    expect(nonTerminalChildren.length).toEqual(4);
  });
});
