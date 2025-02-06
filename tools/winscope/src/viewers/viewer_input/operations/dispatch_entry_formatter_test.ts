/*
 * Copyright 2024 The Android Open Source Project
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
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {DispatchEntryFormatter} from './dispatch_entry_formatter';

describe('DispatchEntryFormatter', () => {
  let operation: DispatchEntryFormatter;
  const layerIdToName = new Map<number, string>([
    [1, 'one'],
    [2, 'two'],
  ]);

  beforeEach(() => {
    operation = new DispatchEntryFormatter(layerIdToName);
  });

  it('formats dispatch entry node', () => {
    const dispatchEntries = new PropertyTreeBuilder()
      .setRootId('entries')
      .setName('dispatchEntries');
    dispatchEntries.setChildren([
      {name: '0', children: [{name: 'windowId', value: 1}]},
      {name: '1', children: [{name: 'windowId', value: 0}]},
      {name: '2', children: [{name: 'windowId', value: 2}]},
    ]);

    const root = UiPropertyTreeNode.from(dispatchEntries.build());

    operation.apply(root);
    expect(root.getDisplayName()).toEqual('TargetWindows');
    expect(root.getChildByName('0')?.getDisplayName()).toEqual('one');
    expect(root.getChildByName('1')?.getDisplayName()).toEqual(
      'WindowId: 0 - <Unknown Name>',
    );
    expect(root.getChildByName('2')?.getDisplayName()).toEqual('two');
  });

  it('formats dispatched pointers', () => {
    const dispatchEntries = new PropertyTreeBuilder()
      .setRootId('entries')
      .setName('dispatchEntries');
    dispatchEntries.setChildren([
      {name: '0', children: [{name: 'dispatchedPointer', children: []}]},
      {
        name: '1',
        children: [
          {
            name: 'dispatchedPointer',
            children: [
              {
                name: '0',
                children: [
                  {
                    name: 'axisValueInWindow',
                    children: [
                      {
                        name: '0',
                        children: [
                          {name: 'axis', value: 0},
                          {
                            name: 'value',
                            value: 2,
                          },
                        ],
                      },
                      {
                        name: '1',
                        children: [
                          {name: 'axis', value: 1},
                          {
                            name: 'value',
                            value: 3,
                          },
                        ],
                      },
                    ],
                  },
                  {
                    name: 'xInDisplay',
                    value: 4.777777,
                  },
                ],
              },
            ],
          },
        ],
      },
      {
        name: '2',
        children: [
          {
            name: 'dispatchedPointer',
            children: [
              {
                name: '0',
                children: [
                  {
                    name: 'axisValueInWindow',
                    children: [
                      {
                        name: '0',
                        children: [
                          {name: 'axis', value: 0},
                          {
                            name: 'value',
                            value: 4,
                          },
                        ],
                      },
                      {
                        name: '1',
                        children: [
                          {name: 'axis', value: 1},
                          {
                            name: 'value',
                          },
                        ],
                      },
                    ],
                  },
                  {
                    name: 'xInDisplay',
                    value: 123,
                  },

                  {
                    name: 'yInDisplay',
                    value: 0.024,
                  },
                  {
                    name: 'pointerId',
                    value: 21,
                  },
                ],
              },
              {
                name: '2',
                children: [
                  {
                    name: 'axisValueInWindow',
                    children: [
                      {
                        name: '0',
                        children: [
                          {name: 'axis', value: 0},
                          {
                            name: 'value',
                            value: 5,
                          },
                        ],
                      },
                      {
                        name: '1',
                        children: [
                          {name: 'axis', value: 1},
                          {
                            name: 'value',
                            value: 6,
                          },
                        ],
                      },
                    ],
                  },
                ],
              },
            ],
          },
        ],
      },
    ]);

    const root = UiPropertyTreeNode.from(dispatchEntries.build());

    operation.apply(root);
    expect(
      root
        .getChildByName('0')
        ?.getChildByName('dispatchedPointer')
        ?.getDisplayName(),
    ).toEqual('DispatchedPointers');
    expect(
      root
        .getChildByName('0')
        ?.getChildByName('dispatchedPointer')
        ?.formattedValue(),
    ).toEqual('<none>');

    expect(
      root
        .getChildByName('1')
        ?.getChildByName('dispatchedPointer')
        ?.getDisplayName(),
    ).toEqual('DispatchedPointers');
    expect(
      root
        .getChildByName('1')
        ?.getChildByName('dispatchedPointer')
        ?.getChildByName('0')
        ?.getDisplayName(),
    ).toEqual('0 - Pointer');
    expect(
      root
        .getChildByName('1')
        ?.getChildByName('dispatchedPointer')
        ?.getChildByName('0')
        ?.formattedValue(),
    ).toEqual('ID: ?, XY: (2.00, 3.00), RawXY: (4.78, ?)');

    expect(
      root
        .getChildByName('2')
        ?.getChildByName('dispatchedPointer')
        ?.getDisplayName(),
    ).toEqual('DispatchedPointers');
    expect(
      root
        .getChildByName('2')
        ?.getChildByName('dispatchedPointer')
        ?.getChildByName('0')
        ?.getDisplayName(),
    ).toEqual('0 - Pointer');
    expect(
      root
        .getChildByName('2')
        ?.getChildByName('dispatchedPointer')
        ?.getChildByName('0')
        ?.formattedValue(),
    ).toEqual('ID: 21, XY: (4.00, ?), RawXY: (123.00, 0.02)');
    expect(
      root
        .getChildByName('2')
        ?.getChildByName('dispatchedPointer')
        ?.getChildByName('2')
        ?.getDisplayName(),
    ).toEqual('1 - Pointer');
    expect(
      root
        .getChildByName('2')
        ?.getChildByName('dispatchedPointer')
        ?.getChildByName('2')
        ?.formattedValue(),
    ).toEqual('ID: ?, XY: (5.00, 6.00), RawXY: (?, ?)');
  });
});
