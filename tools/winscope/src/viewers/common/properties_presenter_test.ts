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
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {TextFilter} from 'viewers/common/text_filter';
import {DiffType} from './diff_type';
import {PropertiesPresenter} from './properties_presenter';
import {UiPropertyTreeNode} from './ui_property_tree_node';

describe('PropertiesPresenter', () => {
  const pTree = new PropertyTreeBuilder()
    .setIsRoot(true)
    .setRootId('Test Trace')
    .setName('entry')
    .setChildren([
      {name: 'defProp', value: false, source: PropertySource.DEFAULT},
      {name: 'setProp', value: 1},
      {name: 'otherProp', value: 2},
      {name: 'calcProp', value: true, source: PropertySource.CALCULATED},
    ])
    .build();
  const hTree = new HierarchyTreeBuilder()
    .setId('Test Trace')
    .setName('entry')
    .setProperties({setProp: 2})
    .build();
  let presenter: PropertiesPresenter;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
  });

  beforeEach(() => {
    presenter = new PropertiesPresenter({}, new TextFilter(), []);
    presenter.setPropertiesTree(pTree);
  });

  it('exposes user options', () => {
    expect(presenter.getUserOptions()).toEqual({});
    const testOptions = {test: {name: 'Test opt', enabled: false}};
    presenter.applyPropertiesUserOptionsChange(testOptions);
    expect(presenter.getUserOptions()).toEqual(testOptions);
  });

  it('updates highlighted property', () => {
    expect(presenter.getHighlightedProperty()).toEqual('');
    const id = '4';
    presenter.applyHighlightedPropertyChange(id);
    expect(presenter.getHighlightedProperty()).toEqual(id);
    presenter.applyHighlightedPropertyChange(id);
    expect(presenter.getHighlightedProperty()).toEqual('');
  });

  it('updates properties tree to show diffs based on user option', async () => {
    let formattedTree = await getFormattedTree(hTree);
    expect(getDiff(formattedTree, 'setProp')).toEqual(DiffType.NONE);
    expect(getDiff(formattedTree, 'otherProp')).toEqual(DiffType.NONE);

    presenter.applyPropertiesUserOptionsChange({
      showDiff: {name: '', enabled: true},
    });
    formattedTree = await getFormattedTree();
    expect(getDiff(formattedTree, 'setProp')).toEqual(DiffType.ADDED);
    expect(getDiff(formattedTree, 'otherProp')).toEqual(DiffType.ADDED);

    formattedTree = await getFormattedTree(hTree);
    formattedTree = assertDefined(presenter.getFormattedTree());
    expect(getDiff(formattedTree, 'setProp')).toEqual(DiffType.MODIFIED);
    expect(getDiff(formattedTree, 'otherProp')).toEqual(DiffType.ADDED);
  });

  it('shows/hides defaults based on user option', async () => {
    expect((await getFormattedTree()).getAllChildren().length).toEqual(2);
    presenter.applyPropertiesUserOptionsChange({
      showDefaults: {name: '', enabled: true},
    });
    expect((await getFormattedTree()).getAllChildren().length).toEqual(3);
  });

  it('filters properties tree', async () => {
    expect((await getFormattedTree()).getAllChildren().length).toEqual(2);
    const filter = new TextFilter('setProp');
    presenter.applyPropertiesFilterChange(filter);
    expect(presenter.getTextFilter()).toEqual(filter);
    expect((await getFormattedTree()).getAllChildren().length).toEqual(1);
  });

  it('keeps calculated properties', async () => {
    const tree = await getFormattedTree(undefined, undefined, true);
    expect(tree.getAllChildren().length).toEqual(3);
  });

  it('overrides display name', async () => {
    const tree = await getFormattedTree(undefined, 'Override Name');
    expect(tree.getDisplayName()).toEqual('Override Name');
  });

  it('keeps allowlist default properties', async () => {
    presenter.updateDefaultAllowList(['defProp']);
    expect((await getFormattedTree()).getChildByName('defProp')).toBeDefined();
  });

  it('discards denylist properties', async () => {
    presenter = new PropertiesPresenter({}, new TextFilter(), ['setProp']);
    presenter.setPropertiesTree(pTree);
    const tree = await getFormattedTree();
    expect(tree.getChildByName('setProp')).toBeUndefined();
  });

  it('applies custom operations', async () => {
    const operation = jasmine.createSpyObj('operation', ['apply']);
    presenter = new PropertiesPresenter({}, new TextFilter(), [], [operation]);
    presenter.setPropertiesTree(pTree);
    await getFormattedTree();
    expect(operation.apply).toHaveBeenCalledTimes(1);
  });

  function getDiff(tree: UiPropertyTreeNode, propertyName: string): DiffType {
    return assertDefined(tree.getChildByName(propertyName)).getDiff();
  }

  async function getFormattedTree(
    previousHierarchyTree?: HierarchyTreeNode,
    displayName?: string,
    keepCalculated = false,
  ): Promise<UiPropertyTreeNode> {
    await presenter.formatPropertiesTree(
      previousHierarchyTree,
      displayName,
      keepCalculated,
    );
    return assertDefined(presenter.getFormattedTree());
  }
});
