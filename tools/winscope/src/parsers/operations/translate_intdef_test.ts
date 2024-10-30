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
import root from 'protos/test/intdef_translation/json';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TranslateIntDef} from './translate_intdef';

describe('TranslateIntDef', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: TranslateIntDef;
  let rootType: TamperedMessageType;

  beforeEach(() => {
    rootType = TamperedMessageType.tamper(root.lookupType('RootMessage'));
  });

  it('translates intdef from stored mapping', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'layoutParamsFlags', value: 1}])
      .build();

    const field = rootType.fields['intdefMappingEntry'];
    operation = new TranslateIntDef(field);
    operation.apply(propertyRoot);
    expect(
      assertDefined(
        propertyRoot.getChildByName('layoutParamsFlags'),
      ).formattedValue(),
    ).toEqual('FLAG_ALLOW_LOCK_WHILE_SCREEN_ON');
  });

  it('translates intdef from field mapping', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([
        {
          name: 'windowLayoutParams',
          value: undefined,
          children: [
            {name: 'type', value: 1},
            {name: 'gravity', value: 1},
            {name: 'softInputMode', value: 1},
            {name: 'inputFeatureFlags', value: 1},
            {name: 'flags', value: 1},
            {name: 'systemUiVisibilityFlags', value: 1},
            {name: 'subtreeSystemUiVisibilityFlags', value: 1},
            {name: 'appearance', value: 1},
            {name: 'behavior', value: 1},
          ],
        },
      ])
      .build();

    const field = rootType.fields['windowLayoutParams'];
    operation = new TranslateIntDef(field);
    operation.apply(propertyRoot);

    const params = assertDefined(
      propertyRoot.getChildByName('windowLayoutParams'),
    );

    expect(
      assertDefined(params.getChildByName('type')).formattedValue(),
    ).toEqual('TYPE_BASE_APPLICATION');
    expect(
      assertDefined(params.getChildByName('gravity')).formattedValue(),
    ).toEqual('CENTER_HORIZONTAL');
    expect(
      assertDefined(params.getChildByName('softInputMode')).formattedValue(),
    ).toEqual('SOFT_INPUT_STATE_UNCHANGED');
    expect(
      assertDefined(
        params.getChildByName('inputFeatureFlags'),
      ).formattedValue(),
    ).toEqual('INPUT_FEATURE_NO_INPUT_CHANNEL');
    expect(
      assertDefined(params.getChildByName('flags')).formattedValue(),
    ).toEqual('FLAG_ALLOW_LOCK_WHILE_SCREEN_ON');
    expect(
      assertDefined(
        params.getChildByName('systemUiVisibilityFlags'),
      ).formattedValue(),
    ).toEqual('SYSTEM_UI_FLAG_LOW_PROFILE');
    expect(
      assertDefined(
        params.getChildByName('subtreeSystemUiVisibilityFlags'),
      ).formattedValue(),
    ).toEqual('SYSTEM_UI_FLAG_LOW_PROFILE');
    expect(
      assertDefined(params.getChildByName('appearance')).formattedValue(),
    ).toEqual('APPEARANCE_OPAQUE_STATUS_BARS');
    expect(
      assertDefined(params.getChildByName('behavior')).formattedValue(),
    ).toEqual('BEHAVIOR_DEFAULT');
  });

  it('translates BigInt', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'layoutParamsFlags', value: 1n}])
      .build();

    const field = rootType.fields['intdefMappingEntry'];
    operation = new TranslateIntDef(field);
    operation.apply(propertyRoot);
    expect(
      assertDefined(
        propertyRoot.getChildByName('layoutParamsFlags'),
      ).formattedValue(),
    ).toEqual('FLAG_ALLOW_LOCK_WHILE_SCREEN_ON');
  });

  it('formats leftover', () => {
    propertyRoot = new PropertyTreeBuilder()
      .setIsRoot(true)
      .setRootId('test')
      .setName('node')
      .setChildren([{name: 'inputConfig', value: 3}])
      .build();

    const field = rootType.fields['intdefMappingEntry'];
    operation = new TranslateIntDef(field);
    operation.apply(propertyRoot);
    expect(
      assertDefined(
        propertyRoot.getChildByName('inputConfig'),
      ).formattedValue(),
    ).toEqual('NO_INPUT_CHANNEL | 2');
  });
});
