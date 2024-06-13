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
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AddDisplayProperties} from './add_display_properties';

describe('AddDisplayProperties', () => {
  let propertyRoot: PropertyTreeNode;
  let operation: AddDisplayProperties;

  beforeEach(() => {
    operation = new AddDisplayProperties();
    propertyRoot = TreeNodeUtils.makePropertyNode(
      'LayerTraceEntry',
      'root',
      null,
    );
  });

  it('adds isLargeScreen true', () => {
    const displays = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'displays',
      [
        {
          dpiX: 0,
          dpiY: 0,
          size: {w: 1080, h: 2340},
          layerStack: 0,
        },
      ],
    );
    propertyRoot.addOrReplaceChild(displays);

    operation.apply(propertyRoot);
    const displayWithProperties = assertDefined(
      propertyRoot.getChildByName('displays'),
    ).getAllChildren()[0];
    expect(
      displayWithProperties.getChildByName('isLargeScreen')?.getValue(),
    ).toEqual(true);
  });

  it('adds isLargeScreen false', () => {
    const displays = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'displays',
      [
        {
          dpiX: 0,
          dpiY: 0,
          size: {w: 0, h: 0},
          layerStack: 0,
        },
      ],
    );
    propertyRoot.addOrReplaceChild(displays);

    operation.apply(propertyRoot);
    const displayWithProperties = assertDefined(
      propertyRoot.getChildByName('displays'),
    ).getAllChildren()[0];
    expect(
      displayWithProperties.getChildByName('isLargeScreen')?.getValue(),
    ).toEqual(false);
  });

  it('adds isOn true', () => {
    const displays = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'displays',
      [
        {
          dpiX: 0,
          dpiY: 0,
          size: {w: 1080, h: 2340},
          layerStack: 0,
        },
      ],
    );
    propertyRoot.addOrReplaceChild(displays);

    operation.apply(propertyRoot);
    const displayWithProperties = assertDefined(
      propertyRoot.getChildByName('displays'),
    ).getAllChildren()[0];
    expect(displayWithProperties.getChildByName('isOn')?.getValue()).toEqual(
      true,
    );
  });

  it('adds isOn false', () => {
    const displays = TreeNodeUtils.makePropertyNode(
      propertyRoot.id,
      'displays',
      [
        {
          dpiX: 0,
          dpiY: 0,
          size: {w: 1080, h: 2340},
          layerStack: 4294967295,
        },
      ],
    );
    propertyRoot.addOrReplaceChild(displays);

    operation.apply(propertyRoot);
    const displayWithProperties = assertDefined(
      propertyRoot.getChildByName('displays'),
    ).getAllChildren()[0];
    expect(displayWithProperties.getChildByName('isOn')?.getValue()).toEqual(
      false,
    );
  });
});
