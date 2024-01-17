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
import {TamperedMessageType, TamperedProtoField} from 'parsers/tampered_message_type';
import root from 'protos/test/fake_proto/json';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {EMPTY_OBJ_STRING, LAYER_ID_FORMATTER} from 'trace/tree_node/formatters';
import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TransformType} from '../surface_flinger/transform_utils';
import {SetFormatters} from './set_formatters';

describe('SetFormatters', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: SetFormatters;
  let field: TamperedProtoField;

  beforeEach(() => {
    field = TamperedMessageType.tamper(root.lookupType('RootMessage')).fields['entry'];
    propertyRoot = new PropertyTreeNode('test node', 'node', PropertySource.PROTO, undefined);
  });

  it('adds correct formatter for enum node', () => {
    propertyRoot.addChild(
      new PropertyTreeNode('test node.enum0', 'enum0', PropertySource.PROTO, 0)
    );
    operation = new SetFormatters(field);
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(rootWithFormatters.getChildByName('enum0')?.formattedValue()).toEqual(
      'ENUM0_VALUE_ZERO'
    );
  });

  it('adds correct formatter for color node', () => {
    propertyRoot.addChild(TreeNodeUtils.makeColorNode(0, 0, 0, 1));
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('color')).formattedValue()).toEqual(
      `${EMPTY_OBJ_STRING}, alpha: 1`
    );
  });

  it('adds correct formatter for rect node', () => {
    propertyRoot.addChild(TreeNodeUtils.makeRectNode(0, 0, 1, 1));
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('rect')).formattedValue()).toEqual(
      '(0, 0) - (1, 1)'
    );
  });

  it('adds correct formatter for buffer node', () => {
    propertyRoot.addChild(TreeNodeUtils.makeBufferNode());
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('buffer')).formattedValue()).toEqual(
      'w: 1, h: 0, stride: 0, format: 1'
    );
  });

  it('adds correct formatter for size node', () => {
    propertyRoot.addChild(TreeNodeUtils.makeSizeNode(1, 2));
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('size')).formattedValue()).toEqual(
      '1 x 2'
    );
  });

  it('adds correct formatter for region node', () => {
    const region = new PropertyTreeNode(
      'test node.region',
      'region',
      PropertySource.PROTO,
      undefined
    );
    const rect = new PropertyTreeNode('test node.region.rect', 'rect', PropertySource.PROTO, []);
    rect.addChild(TreeNodeUtils.makeRectNode(0, 0, 1, 1, 'test node.region.rect.0'));
    region.addChild(rect);
    propertyRoot.addChild(region);

    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('region')).formattedValue()).toEqual(
      'SkRegion((0, 0, 1, 1))'
    );
  });

  it('adds correct formatter for position node', () => {
    propertyRoot.addChild(TreeNodeUtils.makePositionNode(1, 2));
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('pos')).formattedValue()).toEqual(
      'x: 1, y: 2'
    );
  });

  it('adds correct formatter for transform node', () => {
    propertyRoot.addChild(TreeNodeUtils.makeTransformNode(TransformType.EMPTY));
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('transform')).formattedValue()).toEqual(
      'IDENTITY'
    );
  });

  it('does not add formatter to unrecognised nested property', () => {
    const property = new PropertyTreeNode(
      'test node.nestedProperty',
      'nestedProperty',
      PropertySource.PROTO,
      undefined
    );
    property.addChild(
      new PropertyTreeNode('test node.nestedProperty.val', 'val', PropertySource.PROTO, 1)
    );
    propertyRoot.addChild(property);
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    const propertyWithFormatters = assertDefined(
      rootWithFormatters.getChildByName('nestedProperty')
    );
    expect(propertyWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(propertyWithFormatters.getChildByName('val')).formattedValue()).toEqual(
      '1'
    );
  });

  it('adds correct formatter for simple leaf property', () => {
    propertyRoot.addChild(new PropertyTreeNode('test node.val', 'val', PropertySource.PROTO, 1));
    operation = new SetFormatters();
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('val')).formattedValue()).toEqual('1');
  });

  it('adds custom formatter', () => {
    propertyRoot.addChild(
      new PropertyTreeNode('test node.layerId', 'layerId', PropertySource.PROTO, -1)
    );
    operation = new SetFormatters(undefined, new Map([['layerId', LAYER_ID_FORMATTER]]));
    const rootWithFormatters = operation.apply(propertyRoot);

    expect(rootWithFormatters.formattedValue()).toEqual('');
    expect(assertDefined(rootWithFormatters.getChildByName('layerId')).formattedValue()).toEqual(
      'none'
    );
  });
});
