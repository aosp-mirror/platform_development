/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {UpdateTransitionParticipants} from './update_transition_participants';

describe('UpdateTransitionParticipants', () => {
  let operation: UpdateTransitionParticipants;

  beforeEach(() => {
    const layerIdToName = new Map<number, string>([[2, 'testLayer']]);
    const windowTokenToTitle = new Map<string, string>([
      ['97b5518', 'testTitle'],
    ]);

    operation = new UpdateTransitionParticipants(
      layerIdToName,
      windowTokenToTitle,
    );
  });

  it('updates layerId and windowToken display names if in maps', () => {
    const transition = new HierarchyTreeBuilder()
      .setId('TransitionsTraceEntry')
      .setName('transition')
      .setProperties({
        layers: [2],
        windows: [159077656n],
      })
      .build();

    operation.apply(transition);
    expect(
      transition
        ?.getEagerPropertyByName('layers')
        ?.getChildByName('0')
        ?.formattedValue(),
    ).toEqual('2 (testLayer)');

    expect(
      transition
        ?.getEagerPropertyByName('windows')
        ?.getChildByName('0')
        ?.formattedValue(),
    ).toEqual('0x97b5518 (testTitle)');
  });

  it('updates only windowId display name if neither layer id nor token in maps', () => {
    const transition = new HierarchyTreeBuilder()
      .setId('TransitionsTraceEntry')
      .setName('transition')
      .setProperties({
        layers: [1],
        windows: [193491296n],
      })
      .build();

    operation.apply(transition);
    expect(
      transition
        ?.getEagerPropertyByName('layers')
        ?.getChildByName('0')
        ?.formattedValue(),
    ).toEqual('1');

    expect(
      transition
        ?.getEagerPropertyByName('windows')
        ?.getChildByName('0')
        ?.formattedValue(),
    ).toEqual('0xb887160');
  });
});
