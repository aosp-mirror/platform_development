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
import {TransformType} from 'parsers/surface_flinger/transform_utils';
import {
  TamperedMessageType,
  TamperedProtoField,
} from 'parsers/tampered_message_type';
import root from 'protos/test/fake_proto/json';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {EMPTY_OBJ_STRING, LAYER_ID_FORMATTER} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {SetFormatters} from './set_formatters';

describe('SetFormatters', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: SetFormatters;
  let field: TamperedProtoField;

  beforeEach(() => {
    field = TamperedMessageType.tamper(root.lookupType('RootMessage')).fields[
      'entry'
    ];
  });

  it('adds correct formatter for enum node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'enum0', value: 0}])
      .build();
    operation = new SetFormatters(field);
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(propertyRoot.getChildByName('enum0')?.formattedValue()).toEqual(
      'ENUM0_VALUE_ZERO',
    );
  });

  it('adds correct formatter for color node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .build();
    propertyRoot.addOrReplaceChild(TreeNodeUtils.makeColorNode(0, 0, 0, 1));
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('color')).formattedValue(),
    ).toEqual(`${EMPTY_OBJ_STRING}, alpha: 1`);
  });

  it('adds correct formatter for color3 node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .build();
    propertyRoot.addOrReplaceChild(
      TreeNodeUtils.makeColorNode(0, 0, 0, undefined),
    );
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('color')).formattedValue(),
    ).toEqual(`(0, 0, 0)`);
  });

  it('adds correct formatter for rect node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .build();
    propertyRoot.addOrReplaceChild(TreeNodeUtils.makeRectNode(0, 0, 1, 1));
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('rect')).formattedValue(),
    ).toEqual('(0, 0) - (1, 1)');
  });

  it('adds correct formatter for buffer node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .build();
    propertyRoot.addOrReplaceChild(TreeNodeUtils.makeBufferNode());
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('buffer')).formattedValue(),
    ).toEqual('w: 1, h: 0, stride: 0, format: 1');
  });

  it('adds correct formatter for size node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .build();
    propertyRoot.addOrReplaceChild(TreeNodeUtils.makeSizeNode(1, 2));
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('size')).formattedValue(),
    ).toEqual('1 x 2');
  });

  it('adds correct formatter for region node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([
        {
          name: 'region',
          value: undefined,
          children: [
            {
              name: 'rect',
              value: undefined,
              children: [
                {
                  name: '0',
                  value: undefined,
                  children: [
                    {name: 'left', value: 0},
                    {name: 'top', value: 0},
                    {name: 'right', value: 1},
                    {name: 'bottom', value: 1},
                  ],
                },
              ],
            },
          ],
        },
      ])
      .build();
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('region')).formattedValue(),
    ).toEqual('SkRegion((0, 0, 1, 1))');
  });

  it('adds correct formatter for position node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .build();
    propertyRoot.addOrReplaceChild(TreeNodeUtils.makePositionNode(1, 2));
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('pos')).formattedValue(),
    ).toEqual('x: 1, y: 2');
  });

  it('adds correct formatter for transform node', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .build();
    propertyRoot.addOrReplaceChild(
      TreeNodeUtils.makeTransformNode(TransformType.EMPTY),
    );
    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('transform')).formattedValue(),
    ).toEqual('IDENTITY');
  });

  it('does not add formatter to unrecognised nested property', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([
        {name: 'nestedProperty', children: [{name: 'val', value: 1}]},
      ])
      .build();

    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    const propertyWithFormatters = assertDefined(
      propertyRoot.getChildByName('nestedProperty'),
    );
    expect(propertyWithFormatters.formattedValue()).toEqual('');
    expect(
      assertDefined(
        propertyWithFormatters.getChildByName('val'),
      ).formattedValue(),
    ).toEqual('1');
  });

  it('adds correct formatter for simple leaf property', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'val', value: 1}])
      .build();

    operation = new SetFormatters();
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('val')).formattedValue(),
    ).toEqual('1');
  });

  it('adds custom formatter', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'layerId', value: -1}])
      .build();
    operation = new SetFormatters(
      undefined,
      new Map([['layerId', LAYER_ID_FORMATTER]]),
    );
    operation.apply(propertyRoot);

    expect(propertyRoot.formattedValue()).toEqual('');
    expect(
      assertDefined(propertyRoot.getChildByName('layerId')).formattedValue(),
    ).toEqual('none');
  });
});
