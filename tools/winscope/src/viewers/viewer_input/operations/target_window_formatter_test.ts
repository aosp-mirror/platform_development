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
import {TargetWindowFormatter} from './target_window_formatter';

describe('TargetWindowFormatter', () => {
  let operation: TargetWindowFormatter;
  const layerIdToName = new Map<number, string>([
    [1, 'one'],
    [2, 'two'],
  ]);

  beforeEach(() => {
    operation = new TargetWindowFormatter(layerIdToName);
  });

  it('formats windowId node', () => {
    const dispatchEntries = new PropertyTreeBuilder()
      .setRootId('entries')
      .setName('dispatchEntries');
    dispatchEntries.setChildren([
      {name: '0', children: [{name: 'windowId', value: 0}]},
      {name: '1', children: [{name: 'windowId', value: 1}]},
      {name: '2', children: [{name: 'windowId', value: 2}]},
    ]);

    const root = UiPropertyTreeNode.from(dispatchEntries.build());

    operation.apply(root);
    expect(
      root.getChildByName('0')?.getChildByName('windowId')?.getDisplayName(),
    ).toEqual('TargetWindow');
    expect(
      root.getChildByName('0')?.getChildByName('windowId')?.formattedValue(),
    ).toEqual('0 - <Unknown Name>');
    expect(
      root.getChildByName('1')?.getChildByName('windowId')?.getDisplayName(),
    ).toEqual('TargetWindow');
    expect(
      root.getChildByName('1')?.getChildByName('windowId')?.formattedValue(),
    ).toEqual('1 - one');
    expect(
      root.getChildByName('2')?.getChildByName('windowId')?.getDisplayName(),
    ).toEqual('TargetWindow');
    expect(
      root.getChildByName('2')?.getChildByName('windowId')?.formattedValue(),
    ).toEqual('2 - two');
  });
});
