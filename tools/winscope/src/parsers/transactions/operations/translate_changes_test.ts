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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TranslateChanges} from './translate_changes';

class Long {
  constructor(public low: number, public high: number) {}
}

describe('TranslateChanges', () => {
  let operation: TranslateChanges;

  beforeEach(() => {
    operation = new TranslateChanges();
  });

  it("decodes 'what' field in LayerState from layerChanges", async () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('TransactionsTraceEntry')
      .setName('entry')
      .setIsRoot(true)
      .setChildren([
        {
          name: 'transactions',
          children: [
            {
              name: '0',
              children: [
                {
                  name: 'layerChanges',
                  children: [
                    {name: '0', children: [{name: 'what', value: 2}]},
                    {
                      name: '1',
                      children: [{name: 'what', value: new Long(2, 0)}],
                    },
                    {name: '2', children: [{name: 'what', value: 4294967360}]},
                    {
                      name: '3',
                      children: [{name: 'what', value: new Long(64, 1)}],
                    },
                  ],
                },
              ],
            },
          ],
        },
      ])
      .build();

    operation.apply(propertyRoot);

    const layerChanges = assertDefined(
      propertyRoot
        .getChildByName('transactions')
        ?.getChildByName('0')
        ?.getChildByName('layerChanges'),
    );

    expect(
      layerChanges
        ?.getChildByName('0')
        ?.getChildByName('what')
        ?.formattedValue(),
    ).toEqual('eLayerChanged');

    expect(
      layerChanges
        ?.getChildByName('1')
        ?.getChildByName('what')
        ?.formattedValue(),
    ).toEqual('eLayerChanged');

    expect(
      layerChanges
        ?.getChildByName('2')
        ?.getChildByName('what')
        ?.formattedValue(),
    ).toEqual('eFlagsChanged | eDestinationFrameChanged');

    expect(
      layerChanges
        ?.getChildByName('3')
        ?.getChildByName('what')
        ?.formattedValue(),
    ).toEqual('eFlagsChanged | eDestinationFrameChanged');
  });

  it("decodes 'what' field in DisplayState from displayChanges", async () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('TransactionsTraceEntry')
      .setName('entry')
      .setIsRoot(true)
      .setChildren([
        {
          name: 'transactions',
          children: [
            {
              name: '0',
              children: [
                {
                  name: 'displayChanges',
                  children: [
                    {name: '0', children: [{name: 'what', value: 22}]},
                  ],
                },
              ],
            },
          ],
        },
      ])
      .build();

    operation.apply(propertyRoot);

    const displayChanges = assertDefined(
      propertyRoot
        .getChildByName('transactions')
        ?.getChildByName('0')
        ?.getChildByName('displayChanges'),
    );

    expect(
      displayChanges
        ?.getChildByName('0')
        ?.getChildByName('what')
        ?.formattedValue(),
    ).toEqual('eLayerStackChanged | eDisplayProjectionChanged | eFlagsChanged');
  });

  it("decodes 'what' field in DisplayState from addedDisplays", async () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('TransactionsTraceEntry')
      .setName('entry')
      .setIsRoot(true)
      .setChildren([
        {
          name: 'addedDisplays',
          children: [{name: '0', children: [{name: 'what', value: 22}]}],
        },
      ])
      .build();

    operation.apply(propertyRoot);

    expect(
      propertyRoot
        .getChildByName('addedDisplays')
        ?.getChildByName('0')
        ?.getChildByName('what')
        ?.formattedValue(),
    ).toEqual('eLayerStackChanged | eDisplayProjectionChanged | eFlagsChanged');
  });
});
