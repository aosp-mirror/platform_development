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

import {DuplicateLayerIds, MissingLayerIds} from 'messaging/user_warnings';
import {EntryHierarchyTreeFactory} from './entry_hierarchy_tree_factory';
import {ParserSurfaceFlinger} from './legacy/parser_surface_flinger';

describe('EntryHierarchyTreeFactory', () => {
  const factory = new EntryHierarchyTreeFactory();
  const testLayer = {
    id: 0,
    name: 'Test layer',
  };

  it('handles missing layer ids', () => {
    const entryProto = {};
    const layerProtos = [
      testLayer,
      {
        id: undefined,
        name: 'Second test layer',
      },
    ];
    const tree = factory.makeEntryHierarchyTree(
      entryProto,
      layerProtos,
      ParserSurfaceFlinger,
    );
    expect(tree.getAllChildren().length).toEqual(1);
    expect(tree.getChildByName(testLayer.name)).toBeDefined();
    expect(tree.getWarnings()).toEqual([new MissingLayerIds()]);
  });

  it('handles duplicate layer ids', () => {
    const entryProto = {};
    const layerProtos = [testLayer, testLayer];
    const tree = factory.makeEntryHierarchyTree(
      entryProto,
      layerProtos,
      ParserSurfaceFlinger,
    );
    expect(tree.getAllChildren().length).toEqual(2);
    expect(tree.getChildByName(testLayer.name)).toBeDefined();
    expect(tree.getChildByName(testLayer.name + ' duplicate(1)')).toBeDefined();
    expect(tree.getWarnings()).toEqual([new DuplicateLayerIds([0])]);
  });

  it('defaults excludesCompositionState to true', () => {
    checkExcludesCompositionState({excludesCompositionState: false}, false);
    checkExcludesCompositionState({excludesCompositionState: null}, true);
    checkExcludesCompositionState({}, true);
  });

  function checkExcludesCompositionState(entry: object, expected: boolean) {
    const tree = factory.makeEntryHierarchyTree(
      entry,
      [testLayer],
      ParserSurfaceFlinger,
    );
    const excludesCompositionState = tree
      .getChildByName(testLayer.name)
      ?.getEagerPropertyByName('excludesCompositionState')
      ?.getValue();
    expect(excludesCompositionState).toEqual(expected);
  }
});
