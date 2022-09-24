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
import { Presenter } from "./presenter";
import { UiData } from "./ui_data";
import { UserOptions } from "viewers/common/user_options";
import { TraceType } from "common/trace/trace_type";
import { LayerTraceEntry } from "common/trace/flickerlib/common";
import { HierarchyTreeNode, PropertiesTreeNode } from "viewers/common/ui_tree_utils";
import { UnitTestUtils } from "test/unit/utils";
import { HierarchyTreeBuilder } from "test/unit/hierarchy_tree_builder";

describe("PresenterSurfaceFlinger", () => {
  let presenter: Presenter;
  let uiData: UiData;
  let entries: Map<TraceType, any>;
  let selectedTree: HierarchyTreeNode;

  beforeAll(async () => {
    entries = new Map<TraceType, any>();
    const entry: LayerTraceEntry = await UnitTestUtils.getLayerTraceEntry();

    selectedTree = new HierarchyTreeBuilder().setName("Dim layer#53").setStableId("EffectLayer 53 Dim layer#53")
      .setFilteredView(true).setKind("53").setDiffType("EffectLayer").setId(53).build();

    entries.set(TraceType.SURFACE_FLINGER, [entry, null]);
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
    expect(filteredUiDataRectLabels?.length).toEqual(7);
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
    expect(uiData.tree?.children.length).toEqual(3);

    presenter.updateHierarchyTree(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);
    // nested children should now be on same level as initial parents
    expect(uiData.tree?.children.length).toEqual(94);
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
    expect(uiData.tree?.children.length).toEqual(94);
    presenter.filterHierarchyTree("Wallpaper");
    // All but four layers should be filtered out
    expect(uiData.tree?.children.length).toEqual(4);
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

    expect(nonTerminalChildren.length).toEqual(55);
    presenter.filterPropertiesTree("bound");

    nonTerminalChildren = uiData.propertiesTree?.children?.filter(
      (child: PropertiesTreeNode) => typeof child.propertyKey === "string"
    ) ?? [];
    expect(nonTerminalChildren.length).toEqual(3);
  });
});
