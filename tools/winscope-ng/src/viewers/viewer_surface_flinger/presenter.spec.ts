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
import { RELATIVE_Z_CHIP, VISIBLE_CHIP } from "viewers/common/chip";
import { Transform, LayerTraceEntry } from "common/trace/flickerlib/common";
import { DiffType, PropertiesTree, Terminal, Tree } from "viewers/common/tree_utils";

describe("PresenterSurfaceFlinger", () => {
  let presenter: Presenter;
  let uiData: UiData;
  let entries: Map<TraceType, any>;
  let transform: Transform;
  let visibleLayerRect: any;
  let selectedItem: Tree;

  beforeAll(async () => {
    entries = new Map<TraceType, any>();
    transform = {matrix: {
      dsdx: 1,
      dsdy: 0,
      dtdx: 0,
      dtdy: 1,
      tx: 1,
      ty: 1,
    }};
    const displayRect = {
      topLeft: {x: 0, y: 0},
      bottomRight: {x: 1, y: -1},
      transform: transform,
      height: 1,
      width: 1,
      ref: null,
      id: 12345,
    };
    visibleLayerRect = {
      topLeft: {x: 0, y: 0},
      bottomRight: {x: 1, y: -1},
      transform: transform,
      height: 1,
      width: 1,
      label: "Layer1Rect",
      ref: null,
      id: 54321,
    };
    const entry: LayerTraceEntry = {
      kind: "entry",
      name: "BaseLayerTraceEntry",
      stableId: "BaseLayerTraceEntry",
      shortName: "BLTE",
      id: 0,
      chips: [],
      displays: [{
        layerStackSpace: displayRect,
        name: "DisplayStack0",
        id: 44,
        layerStackId: 0,
      }],
      visibleLayers: [
        {
          name: "Layer1",
          id: 1,
          stableId: "1 Layer1",
          rect: visibleLayerRect,
          stackId: 0,
          absoluteZ: 0,
          isVisible: true,
        }
      ],
      children: [
        {
          name: "Layer1",
          id: 1,
          stableId: "1 Layer1",
          rect: visibleLayerRect,
          stackId: 0,
          absoluteZ: 0,
          isVisible: true,
        },
        {
          kind: "3",
          id: "3",
          name: "Child1",
          stableId: "3 Child1",
          stackId: 0,
          absoluteZ: 0,
          isVisible: true,
          children: [
            {
              kind: "2",
              id: "2",
              name: "Child2",
              stableId: "2 Child2",
              children: [],
              stackId: 0,
              absoluteZ: 0,
              isVisible: true,
            }
          ]
        }
      ]
    };
    selectedItem = {
      id: "3",
      name: "Child1",
      stackId: 0,
      isVisible: true,
      kind: "3",
      stableId: "3 Child1",
      shortName: undefined,
      simplifyNames: true,
      showInFilteredView: true,
      proto: {
        barrierLayer: [],
        id: 3,
        parent: 1,
        type: "ContainerLayer",
      },
      chips: [ VISIBLE_CHIP, RELATIVE_Z_CHIP ],
      children: [{
        id: "2",
        name: "Child2",
        stackId: 0,
        children: [],
        kind: "2",
        stableId: "2 Child2",
        shortName: undefined,
        simplifyNames: true,
        proto: {
          barrierLayer: [],
          id: 2,
          parent: 3,
          type: "ContainerLayer",
        },
        isVisible: true,
        showInFilteredView: true,
        chips: [ VISIBLE_CHIP, RELATIVE_Z_CHIP ],
      }],
    };
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
    expect(filteredUiDataRectLabels).toContain("Layer1Rect");
    expect(filteredUiDataRectLabels).toContain("DisplayStack0");
    expect(uiData.displayIds).toContain(0);
    expect(hierarchyOpts).toBeTruthy();
    expect(propertyOpts).toBeTruthy();

    // does not check specific tree values as tree generation method may change
    expect(Object.keys(uiData.tree).length > 0).toBeTrue();
  });

  it("can update pinned items", () => {
    expect(uiData.pinnedItems).toEqual([]);
    const pinnedItem = {
      name: "FirstPinnedItem",
      kind: "4",
      id: 4,
      type: "TestItem",
      stableId: "TestItem 4 FirstPinnedItem"
    };
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
    presenter.updateHierarchyTree(userOptions);
    expect(uiData.hierarchyUserOptions).toEqual(userOptions);

    // nested child should now be on same level its parent
    expect(uiData.tree.children.length).toEqual(3);
  });

  it("can filter hierarchy tree", () => {
    presenter.notifyCurrentTraceEntries(entries);
    presenter.filterHierarchyTree("Child");
    // Layer1 should be filtered out
    expect(uiData.tree.children.length).toEqual(1);
  });


  it("can set new properties tree and associated ui data", () => {
    presenter.notifyCurrentTraceEntries(entries);
    presenter.newPropertiesTree(selectedItem);
    // does not check specific tree values as tree transformation method may change
    expect(Object.keys(uiData.selectedTree).length > 0).toBeTrue();
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
    presenter.newPropertiesTree(selectedItem);
    presenter.updatePropertiesTree(userOptions);
    expect(uiData.propertiesUserOptions).toEqual(userOptions);
    //check that diff type added
    expect(uiData.selectedTree.diffType).toEqual(DiffType.NONE);
  });

  it("can filter properties tree", () => {
    presenter.notifyCurrentTraceEntries(entries);
    presenter.newPropertiesTree(selectedItem);

    let nonTerminalChildren = uiData.selectedTree
      .children[0]
      .children.filter(
        (child: PropertiesTree) => !(child.propertyKey instanceof Terminal)
      );
    expect(nonTerminalChildren.length).toEqual(2);

    presenter.filterPropertiesTree("ContainerLayer");

    // ContainerLayer should be filtered out
    nonTerminalChildren = uiData.selectedTree
      .children[0]
      .children.filter(
        (child: PropertiesTree) => !(child.propertyKey instanceof Terminal)
      );
    expect(nonTerminalChildren.length).toEqual(1);
  });
});
