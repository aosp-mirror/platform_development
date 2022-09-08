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
import { RELATIVE_Z_CHIP } from "viewers/common/chip";
import { DiffType, getFilter } from "viewers/common/tree_utils";
import { TreeGenerator } from "viewers/common/tree_generator";

describe("TreeGenerator", () => {
  it("generates tree", () => {
    const tree = {
      kind: "entry",
      name: "BaseLayerTraceEntry",
      shortName: "BLTE",
      id: 0,
      chips: [],
      children: [{
        kind: "3",
        id: "3",
        name: "Child1",
        children: [
          {
            kind: "2",
            id: "2",
            name: "Child2",
            children: []
          }
        ]}]
    };
    const expected = {
      simplifyNames: false,
      name: "BaseLayerTraceEntry",
      id: 0,
      children: [
        {
          id: "3",
          name: "Child1",
          children: [{
            kind: "2",
            id: "2",
            name: "Child2",
            children: [],
            simplifyNames: false,
            showInFilteredView: true,
            stableId: undefined,
            shortName: undefined,
            chips: [ RELATIVE_Z_CHIP ]
          }],
          kind: "3",
          simplifyNames: false,
          showInFilteredView: true,
          stableId: undefined,
          shortName: undefined,
          chips: [ RELATIVE_Z_CHIP ],
        }
      ],
      kind: "entry",
      stableId: undefined,
      shortName: "BLTE",
      chips: [],
      showInFilteredView: true,
    };

    const filter = getFilter("");
    const generator = new TreeGenerator(tree, filter);
    expect(generator.generateTree()).toEqual(expected);
  });

  it("generates diff tree with no diff", () => {
    const tree = {
      kind: "entry",
      name: "BaseLayerTraceEntry",
      shortName: "BLTE",
      stableId: "0",
      chips: [],
      id: 0,
      children: [{
        kind: "3",
        id: "3",
        stableId: "3",
        name: "Child1",
        children: [
          {
            kind: "2",
            id: "2",
            stableId: "2",
            name: "Child2",
          }
        ]}]
    };
    const newTree = tree;
    const expected = {
      simplifyNames: false,
      name: "BaseLayerTraceEntry",
      id: 0,
      stableId: "0",
      children: [
        {
          id: "3",
          stableId: "3",
          name: "Child1",
          children: [{
            kind: "2",
            id: "2",
            name: "Child2",
            children: [],
            simplifyNames: false,
            showInFilteredView: true,
            stableId: "2",
            shortName: undefined,
            diffType: DiffType.NONE,
            chips: [ RELATIVE_Z_CHIP ]
          }],
          kind: "3",
          shortName: undefined,
          simplifyNames: false,
          showInFilteredView: true,
          chips: [ RELATIVE_Z_CHIP ],
          diffType: DiffType.NONE
        }
      ],
      kind: "entry",
      shortName: "BLTE",
      chips: [],
      diffType: DiffType.NONE,
      showInFilteredView: true,
    };

    const filter = getFilter("");
    const generator = new TreeGenerator(tree, filter);
    expect(generator.withUniqueNodeId((node: any) => {
      if (node) return node.stableId;
      else return null;
    }).compareWith(newTree).generateFinalDiffTree()).toEqual(expected);
  });

  it("generates diff tree with moved node", () => {
    const tree = {
      kind: "entry",
      name: "BaseLayerTraceEntry",
      shortName: "BLTE",
      stableId: "0",
      chips: [],
      id: 0,
      children: [{
        kind: "3",
        id: "3",
        stableId: "3",
        name: "Child1",
        children: [
          {
            kind: "2",
            id: "2",
            stableId: "2",
            name: "Child2",
          }
        ]}]
    };
    const newTree =  {
      kind: "entry",
      name: "BaseLayerTraceEntry",
      shortName: "BLTE",
      stableId: "0",
      chips: [],
      id: 0,
      children: [
        {
          kind: "3",
          id: "3",
          stableId: "3",
          name: "Child1",
          children: []
        },
        {
          kind: "2",
          id: "2",
          stableId: "2",
          name: "Child2",
        }
      ]
    };
    const expected = {
      simplifyNames: false,
      name: "BaseLayerTraceEntry",
      id: 0,
      stableId: "0",
      children: [
        {
          id: "3",
          stableId: "3",
          name: "Child1",
          children: [ {
            kind: "2",
            id: "2",
            name: "Child2",
            children: [],
            simplifyNames: false,
            showInFilteredView: true,
            stableId: "2",
            shortName: undefined,
            diffType: DiffType.ADDED_MOVE,
            chips: [ RELATIVE_Z_CHIP ]
          }],
          kind: "3",
          shortName: undefined,
          simplifyNames: false,
          showInFilteredView: true,
          chips: [ RELATIVE_Z_CHIP ],
          diffType: DiffType.NONE
        },
        {
          kind: "2",
          id: "2",
          name: "Child2",
          children: [],
          simplifyNames: false,
          showInFilteredView: true,
          stableId: "2",
          shortName: undefined,
          chips: [ RELATIVE_Z_CHIP ],
          diffType: DiffType.DELETED_MOVE
        }
      ],
      kind: "entry",
      shortName: "BLTE",
      chips: [],
      diffType: DiffType.NONE,
      showInFilteredView: true
    };

    const filter = getFilter("");
    const generator = new TreeGenerator(tree, filter);
    const newDiffTree = generator.withUniqueNodeId((node: any) => {
      if (node) return node.stableId;
      else return null;
    }).compareWith(newTree).generateFinalDiffTree();
    expect(newDiffTree).toEqual(expected);
  });
});
