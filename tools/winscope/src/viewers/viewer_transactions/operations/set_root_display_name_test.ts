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
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {SetRootDisplayNames} from './set_root_display_name';

describe('SetRootDisplayNames', () => {
  let operation: SetRootDisplayNames;

  beforeEach(() => {
    operation = new SetRootDisplayNames();
  });

  it('sets display name LayerState', () => {
    const propertyRoot = UiPropertyTreeNode.from(
      new PropertyTreeBuilder().setRootId('layerChanges').setName('0').build(),
    );

    operation.apply(propertyRoot);
    expect(propertyRoot.getDisplayName()).toEqual('LayerState');
  });

  it('sets display name DisplayState for change in display', () => {
    const propertyRoot = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('displayChanges')
        .setName('0')
        .build(),
    );

    operation.apply(propertyRoot);
    expect(propertyRoot.getDisplayName()).toEqual('DisplayState');
  });

  it('sets display name DisplayState for added display', () => {
    const propertyRoot = UiPropertyTreeNode.from(
      new PropertyTreeBuilder().setRootId('addedDisplays').setName('0').build(),
    );

    operation.apply(propertyRoot);
    expect(propertyRoot.getDisplayName()).toEqual('DisplayState');
  });

  it('sets display name LayerCreationArgs', () => {
    const propertyRoot = UiPropertyTreeNode.from(
      new PropertyTreeBuilder().setRootId('addedLayers').setName('0').build(),
    );

    operation.apply(propertyRoot);
    expect(propertyRoot.getDisplayName()).toEqual('LayerCreationArgs');
  });

  it('sets display name destroyedLayerId', () => {
    const propertyRoot = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('destroyedLayers')
        .setName('0')
        .build(),
    );

    operation.apply(propertyRoot);
    expect(propertyRoot.getDisplayName()).toEqual('destroyedLayerId');
  });

  it('sets display name removedDisplayId', () => {
    const propertyRoot = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('removedDisplays')
        .setName('0')
        .build(),
    );

    operation.apply(propertyRoot);
    expect(propertyRoot.getDisplayName()).toEqual('removedDisplayId');
  });

  it('sets display name destroyedLayerHandleId', () => {
    const propertyRoot = UiPropertyTreeNode.from(
      new PropertyTreeBuilder()
        .setRootId('destroyedLayerHandles')
        .setName('0')
        .build(),
    );

    operation.apply(propertyRoot);
    expect(propertyRoot.getDisplayName()).toEqual('destroyedLayerHandleId');
  });
});
