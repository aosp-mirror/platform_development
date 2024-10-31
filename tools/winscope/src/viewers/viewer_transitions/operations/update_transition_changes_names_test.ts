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

import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {UpdateTransitionChangesNames} from './update_transition_changes_names';

describe('UpdateTransitionChangesNames', () => {
  let operation: UpdateTransitionChangesNames;

  beforeEach(() => {
    const layerIdToName = new Map<number, string>([[2, 'testLayer']]);
    const windowTokenToTitle = new Map<string, string>([
      ['97b5518', 'testTitle'],
    ]);

    operation = new UpdateTransitionChangesNames(
      layerIdToName,
      windowTokenToTitle,
    );
  });

  it('updates layerId and windowToken display names if in maps', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [
            {
              name: 'targets',
              children: [
                {
                  name: '0',
                  children: [
                    {name: 'layerId', value: 2},
                    {name: 'windowId', value: 159077656n},
                  ],
                },
              ],
            },
          ],
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(
      propertyRoot
        .getChildByName('wmData')
        ?.getChildByName('targets')
        ?.getChildByName('0')
        ?.getChildByName('layerId')
        ?.formattedValue(),
    ).toEqual('2 (testLayer)');

    expect(
      propertyRoot
        .getChildByName('wmData')
        ?.getChildByName('targets')
        ?.getChildByName('0')
        ?.getChildByName('windowId')
        ?.formattedValue(),
    ).toEqual('0x97b5518 (testTitle)');
  });

  it('updates only windowId display name if neither layer id nor token in maps', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('TransitionsTraceEntry')
      .setName('transition')
      .setChildren([
        {
          name: 'wmData',
          children: [
            {
              name: 'targets',
              children: [
                {
                  name: '0',
                  children: [
                    {name: 'layerId', value: 1},
                    {name: 'windowId', value: 193491296n},
                  ],
                },
              ],
            },
          ],
        },
      ])
      .build();

    operation.apply(propertyRoot);
    expect(
      propertyRoot
        .getChildByName('wmData')
        ?.getChildByName('targets')
        ?.getChildByName('0')
        ?.getChildByName('layerId')
        ?.formattedValue(),
    ).toEqual('');

    expect(
      propertyRoot
        .getChildByName('wmData')
        ?.getChildByName('targets')
        ?.getChildByName('0')
        ?.getChildByName('windowId')
        ?.formattedValue(),
    ).toEqual('0xb887160');
  });
});
