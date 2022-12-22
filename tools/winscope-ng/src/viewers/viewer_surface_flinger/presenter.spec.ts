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
import {Presenter} from "./presenter";
import {UiData} from "./ui_data";
import {UserOptions} from "viewers/common/user_options";
import {TraceType} from "common/trace/trace_type";
import {LayerTraceEntry} from "common/trace/flickerlib/common";
import {HierarchyTreeNode, PropertiesTreeNode} from "viewers/common/ui_tree_utils";
import {UnitTestUtils} from "test/unit/utils";
import {HierarchyTreeBuilder} from "test/unit/hierarchy_tree_builder";
import { MockStorage } from "test/unit/mock_storage";

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
    }, new MockStorage());
  });

  it("processes current trace entries", () => {
    presenter.notifyCurrentTraceEntries(entries);

    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.highlightedItems?.length).toEqual(0);
    expect(uiData.displayIds).toContain(0);
    const hierarchyOpts = uiData.hierarchyUserOptions ?
      Object.keys(uiData.hierarchyUserOptions) : null;
    expect(hierarchyOpts).toBeTruthy();
    const propertyOpts = uiData.propertiesUserOptions ?
      Object.keys(uiData.propertiesUserOptions) : null;
    expect(propertyOpts).toBeTruthy();
    expect(Object.keys(uiData.tree!).length > 0).toBeTrue();
  });

  it("handles unavailable trace entry", () => {
    presenter.notifyCurrentTraceEntries(entries);
    expect(uiData.tree).toBeDefined();
    expect(Object.keys(uiData.tree!).length > 0).toBeTrue();

    const emptyEntries = new Map<TraceType, any>();
    presenter.notifyCurrentTraceEntries(emptyEntries);
    expect(uiData.tree).toBeFalsy();
  });

  it("creates input data for rects view", () => {
    presenter.notifyCurrentTraceEntries(entries);
    expect(uiData.rects.length).toBeGreaterThan(0);
    expect(uiData.rects[0].topLeft).toEqual({x: 0, y: 0});
    expect(uiData.rects[0].bottomRight).toEqual({x: 1080, y: 118});
    expect(uiData.rects[0].width).toEqual(1080);
    expect(uiData.rects[0].height).toEqual(118);
  });

  it("updates pinned items", () => {
    expect(uiData.pinnedItems).toEqual([]);

    const pinnedItem = new HierarchyTreeBuilder().setName("FirstPinnedItem")
      .setStableId("TestItem 4").setLayerId(4).build();
    presenter.updatePinnedItems(pinnedItem);
    expect(uiData.pinnedItems).toContain(pinnedItem);
  });

  it("updates highlighted items", () => {
    expect(uiData.highlightedItems).toEqual([]);

    const id = "4";
    presenter.updateHighlightedItems(id);
    expect(uiData.highlightedItems).toContain(id);
  });

  it("updates hierarchy tree", () => {
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

  it("filters hierarchy tree", () => {
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


  it("sets properties tree and associated ui data", () => {
    presenter.notifyCurrentTraceEntries(entries);
    presenter.newPropertiesTree(selectedTree);
    // does not check specific tree values as tree transformation method may change
    expect(uiData.propertiesTree).toBeTruthy();
  });

  it("updates properties tree", () => {
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
    expect(uiData.propertiesTree?.diffType).toBeTruthy();
  });

  it("filters properties tree", () => {
    presenter.notifyCurrentTraceEntries(entries);
    presenter.newPropertiesTree(selectedTree);
    let nonTerminalChildren = uiData.propertiesTree?.children?.filter(
      (child: PropertiesTreeNode) => typeof child.propertyKey === "string"
    ) ?? [];

    expect(nonTerminalChildren.length).toEqual(22);
    presenter.filterPropertiesTree("bound");

    nonTerminalChildren = uiData.propertiesTree?.children?.filter(
      (child: PropertiesTreeNode) => typeof child.propertyKey === "string"
    ) ?? [];
    expect(nonTerminalChildren.length).toEqual(3);
  });
});
