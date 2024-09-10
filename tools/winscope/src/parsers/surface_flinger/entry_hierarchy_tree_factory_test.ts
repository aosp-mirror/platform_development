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

import {MissingLayerIds} from 'messaging/user_warnings';
import {UserNotifierChecker} from 'test/unit/user_notifier_checker';
import {EntryHierarchyTreeFactory} from './entry_hierarchy_tree_factory';
import {ParserSurfaceFlinger} from './legacy/parser_surface_flinger';

describe('EntryHierarchyTreeFactory', () => {
  const factory = new EntryHierarchyTreeFactory();
  let userNotifierChecker: UserNotifierChecker;

  beforeEach(() => {
    userNotifierChecker = new UserNotifierChecker();
  });

  it('handles missing layer ids', () => {
    const entryProto = {};
    const layerProtos = [
      {
        id: 0,
        name: 'Test layer',
      },
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
    expect(tree.getChildByName('Test layer')).toBeDefined();
    userNotifierChecker.expectNotified([new MissingLayerIds()]);
  });
});
