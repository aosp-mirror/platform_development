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
import { TreeTransformer } from "viewers/common/tree_transformer";
import { DiffType, getFilter, Terminal } from "viewers/common/tree_utils";

describe("TreeTransformer", () => {
  it("creates ordinary properties tree without show diff enabled", () => {
    const selectedTree = {
      id: "3",
      name: "Child1",
      stackId: 0,
      isVisible: true,
      kind: "3",
      stableId: "3 Child1",
      shortName: undefined,
      simplifyNames: true,
      showInFilteredView: true,
      skip: null,
      proto: {
        barrierLayer: [],
        id: 3,
        parent: 1,
        type: "ContainerLayer",
      },
      chips: [],
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
        chips: [],
      }],
    };
    const expected = {
      kind: "",
      name: "Child1",
      stableId: "3 Child1",
      children: [
        {
          kind: "",
          name: "proto",
          stableId: "3 Child1.proto",
          children: [
            {
              kind: "",
              name: "id: empty",
              stableId: "3 Child1.proto.id",
              children: [],
              combined: true,
              propertyKey: "id",
              propertyValue: "empty"},
            {
              kind: "",
              name: "type: ContainerLayer",
              stableId: "3 Child1.proto.type",
              children: [],
              combined: true,
              propertyKey: "type",
              propertyValue: "ContainerLayer"
            }
          ],
          propertyKey: "proto",
          propertyValue: null,
        },
        {
          kind: "",
          name: new Terminal(),
          stableId: "3 Child1.null",
          children: []
        }
      ],
      propertyKey: "Child1",
      propertyValue: null
    };

    const filter = getFilter("");
    const transformer = new TreeTransformer(selectedTree, filter);

    const transformedTree = transformer.transform();

    expect(transformedTree).toEqual(expected);
  });

  it("creates properties tree with show diff enabled, comparing to a null previous entry", () => {
    const selectedTree = {
      id: "3",
      name: "Child1",
      stackId: 0,
      isVisible: true,
      kind: "3",
      stableId: "3 Child1",
      shortName: undefined,
      simplifyNames: true,
      showInFilteredView: true,
      skip: null,
      proto: {
        barrierLayer: [],
        id: 3,
        parent: 1,
        type: "ContainerLayer",
      },
      chips: [],
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
        chips: [],
      }],
    };
    const expected = {
      kind: "",
      name: "Child1",
      stableId: "3 Child1",
      children: [
        {
          kind: "",
          name: "proto",
          stableId: "3 Child1.proto",
          children: [
            {
              kind: "",
              name: "id: empty",
              stableId: "3 Child1.proto.id",
              children: [],
              combined: true,
              diffType: DiffType.ADDED,
              propertyKey: "id",
              propertyValue: "empty",
            },
            {
              kind: "",
              name: "type: ContainerLayer",
              stableId: "3 Child1.proto.type",
              children: [],
              combined: true,
              diffType: DiffType.ADDED,
              propertyKey: "type",
              propertyValue: "ContainerLayer",
            }
          ],
          diffType: DiffType.ADDED,
          propertyKey: "proto",
          propertyValue: null,
        }
      ],
      diffType: DiffType.NONE,
      propertyKey: "Child1",
      propertyValue: null,
    };

    const filter = getFilter("");
    const transformer = new TreeTransformer(selectedTree, filter)
      .setIsShowDiff(true)
      .setDiffProperties(null);

    const transformedTree = transformer.transform();
    expect(transformedTree).toEqual(expected);
  });
});
