/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {VisibilityComputation} from './visibility_computation';

describe('VisibilityComputation', () => {
  let computation: VisibilityComputation;

  beforeEach(() => {
    computation = new VisibilityComputation();
  });

  it('adds isComputedVisible true if root node is visible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('ViewNode')
      .setName('test.package.name@123456789')
      .setProperties({visibility: 0})
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    expect(
      assertDefined(
        hierarchyRoot.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeTrue();
  });

  it('adds isComputedVisible true if node is visible and parent is visible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('ViewNode')
      .setName('test.package.name@123456789')
      .setProperties({visibility: 0})
      .setChildren([
        {
          id: 'ViewNode',
          name: 'test.package.name@987654321',
          properties: {visibility: 0},
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    hierarchyRoot.forEachNodeDfs((node) =>
      expect(
        assertDefined(
          node.getEagerPropertyByName('isComputedVisible'),
        ).getValue(),
      ).toBeTrue(),
    );
  });

  it('adds isComputedVisible false if root is not visible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('ViewNode')
      .setName('test.package.name@123456789')
      .setProperties({visibility: 4})
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    expect(
      assertDefined(
        hierarchyRoot.getEagerPropertyByName('isComputedVisible'),
      ).getValue(),
    ).toBeFalse();
  });

  it('adds isComputedVisible false if node is visible but parent is not visible', () => {
    const hierarchyRoot = new HierarchyTreeBuilder()
      .setId('ViewNode')
      .setName('test.package.name@123456789')
      .setProperties({visibility: 4})
      .setChildren([
        {
          id: 'ViewNode',
          name: 'test.package.name@987654321',
          properties: {visibility: 0},
        },
      ])
      .build();

    computation.setRoot(hierarchyRoot).executeInPlace();
    hierarchyRoot.forEachNodeDfs((node) =>
      expect(
        assertDefined(
          node.getEagerPropertyByName('isComputedVisible'),
        ).getValue(),
      ).toBeFalse(),
    );
  });
});
