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
import {PropertySource, PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TranslateIntDef} from './translate_intdef';

describe('TranslateIntDef', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: TranslateIntDef;
  let field: TamperedProtoField;

  beforeEach(() => {
    field = TamperedMessageType.tamper(root.lookupType('RootMessage')).fields['intdefMappingEntry'];
    propertyRoot = new PropertyTreeNode('test node', 'node', PropertySource.PROTO, undefined);
  });

  it('translates intdef from stored mapping', () => {
    propertyRoot.addChild(
      new PropertyTreeNode(
        'test node.layoutParamsFlags',
        'layoutParamsFlags',
        PropertySource.PROTO,
        1
      )
    );
    operation = new TranslateIntDef(field);
    expect(
      assertDefined(
        operation.apply(propertyRoot).getChildByName('layoutParamsFlags')
      ).formattedValue()
    ).toEqual('FLAG_ALLOW_LOCK_WHILE_SCREEN_ON');
  });
});
