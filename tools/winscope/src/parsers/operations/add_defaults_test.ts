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
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/test/fake_proto/json';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AddDefaults} from './add_defaults';

describe('AddDefaults', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: AddDefaults;
  let protoType: TamperedMessageType;

  beforeEach(() => {
    const rootField = TamperedMessageType.tamper(root.lookupType('RootMessage')).fields['entry'];
    protoType = assertDefined(rootField.tamperedMessageType);
    propertyRoot = new PropertyTreeNode('test node', 'node', PropertySource.PROTO, undefined);
  });

  it('adds only defaults from allowlist', () => {
    operation = new AddDefaults(protoType, ['number_32bit']);
    operation.apply(propertyRoot);
    expect(propertyRoot.getAllChildren().length).toEqual(1);
    const defaultNode = assertDefined(propertyRoot.getChildByName('number_32bit'));
    expect(defaultNode.getValue()).toEqual(0);
    checkAllNodesAreDefault(propertyRoot);
  });

  it('adds all defaults from prototype definition in absence of allowlist', () => {
    operation = new AddDefaults(protoType);
    operation.apply(propertyRoot);
    expect(propertyRoot.getAllChildren().length).toEqual(11);
    checkAllNodesAreDefault(propertyRoot);
    expect(assertDefined(propertyRoot.getChildByName('array')).getValue()).toEqual([]);
    expect(assertDefined(propertyRoot.getChildByName('number_32bit')).getValue()).toEqual(0);
    expect(assertDefined(propertyRoot.getChildByName('number_64bit')).getValue()).toEqual(0n);
    expect(assertDefined(propertyRoot.getChildByName('boolValue')).getValue()).toBeFalse();
  });

  it('does not add defaults in denylist', () => {
    operation = new AddDefaults(protoType, undefined, ['number_32bit', 'number_64bit']);
    operation.apply(propertyRoot);

    expect(propertyRoot.getAllChildren().length).toEqual(9);
    checkAllNodesAreDefault(propertyRoot);
    expect(propertyRoot.getChildByName('number_32bit')).toBeUndefined();
    expect(propertyRoot.getChildByName('number_64bit')).toBeUndefined();
  });

  it('replaces undefined proto node with default node', () => {
    operation = new AddDefaults(protoType, ['number_32bit']);
    propertyRoot.addChild(
      TreeNodeUtils.makePropertyNode(propertyRoot.id, 'number_32bit', undefined)
    );
    operation.apply(propertyRoot);

    expect(propertyRoot.getAllChildren().length).toEqual(1);
    const defaultNode = assertDefined(propertyRoot.getChildByName('number_32bit'));
    expect(defaultNode.getValue()).toEqual(0);
    checkAllNodesAreDefault(propertyRoot);
  });

  function checkAllNodesAreDefault(root: PropertyTreeNode) {
    root.getAllChildren().forEach((child) => {
      expect(child.source).toEqual(PropertySource.DEFAULT);
    });
  }
});
