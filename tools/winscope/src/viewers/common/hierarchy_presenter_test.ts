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
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {TimestampConverterUtils} from 'common/time/test_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {TextFilter} from 'viewers/common/text_filter';
import {DiffType} from './diff_type';
import {HierarchyPresenter} from './hierarchy_presenter';
import {SimplifyNames} from './operations/simplify_names';
import {UserOptions} from './user_options';

describe('HierarchyPresenter', () => {
  const timestamp1 = TimestampConverterUtils.makeElapsedTimestamp(1n);
  const timestamp2 = TimestampConverterUtils.makeElapsedTimestamp(2n);
  const tree1 = new HierarchyTreeBuilder()
    .setId('Test Trace')
    .setName('entry')
    .addChildProperty({name: 'setProp', value: true})
    .addChildProperty({
      name: 'defProp',
      value: false,
      source: PropertySource.DEFAULT,
    })
    .setChildren([
      {
        id: '1',
        name: 'Parent1',
        properties: {isComputedVisible: true, testProp: true},
        children: [
          {id: '3', name: 'Child3', properties: {isComputedVisible: true}},
        ],
      },
      {
        id: '2',
        name: 'Parent2',
        properties: {isComputedVisible: false, nested: {innerProp: 1}},
      },
    ])
    .build();
  const tree2 = new HierarchyTreeBuilder()
    .setId('Test Trace')
    .setName('entry')
    .setChildren([
      {
        id: '1',
        name: 'Parent1',
        properties: {
          isComputedVisible: true,
          testProp: false,
          newProp: true,
        },
      },
      {
        id: '2',
        name: 'Parent2',
        properties: {isComputedVisible: false, nested: {innerProp: 2}},
      },
    ])
    .build();
  const trace = new TraceBuilder<HierarchyTreeNode>()
    .setType(TraceType.SURFACE_FLINGER)
    .setEntries([tree1, tree2])
    .setTimestamps([timestamp1, timestamp2])
    .build();

  const tree3 = new HierarchyTreeBuilder()
    .setId('Test Trace 2')
    .setName('entry')
    .setChildren([
      {
        id: '2',
        name: 'Parent2',
        properties: {isComputedVisible: false},
        children: [
          {
            id: '3',
            name: 'Child3',
            properties: {isComputedVisible: true},
            children: [
              {id: '4', name: 'Child4', properties: {isComputedVisible: false}},
            ],
          },
        ],
      },
    ])
    .build();
  const secondTrace = new TraceBuilder<HierarchyTreeNode>()
    .setType(TraceType.SURFACE_FLINGER)
    .setEntries([tree3])
    .setTimestamps([timestamp1])
    .build();

  let presenter: HierarchyPresenter;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(TreeNodeUtils.treeNodeEqualityTester);
  });

  beforeEach(() => {
    presenter = new HierarchyPresenter({}, new TextFilter(), [], false, false);
  });

  it('exposes user options', async () => {
    expect(presenter.getUserOptions()).toEqual({});
    const testOptions = {test: {name: '', enabled: false}};
    await presenter.applyHierarchyUserOptionsChange(testOptions);
    expect(presenter.getUserOptions()).toEqual(testOptions);
  });

  it('updates current and previous entries for trace', async () => {
    expect(presenter.getAllCurrentHierarchyTrees()?.length).toEqual(0);
    expect(presenter.getAllFormattedTrees()?.length).toEqual(0);

    await presenter.applyTracePositionUpdate(
      [trace.getEntry(1), secondTrace.getEntry(0)],
      '',
    );
    expect(presenter.getCurrentEntryForTrace(trace)).toEqual(trace.getEntry(1));
    expect(presenter.getCurrentEntryForTrace(secondTrace)).toEqual(
      secondTrace.getEntry(0),
    );
    expect(presenter.getCurrentHierarchyTreesForTrace(trace)?.length).toEqual(
      1,
    );
    expect(presenter.getAllCurrentHierarchyTrees()?.length).toEqual(2);
    expect(presenter.getAllFormattedTrees()?.length).toEqual(2);
    expect(presenter.getFormattedTreesByTrace(trace)?.length).toEqual(1);
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeUndefined();
    expect(
      presenter.getPreviousHierarchyTreeForTrace(secondTrace),
    ).toBeUndefined();

    await presenter.applyHierarchyUserOptionsChange({
      showDiff: {name: '', enabled: true, isUnavailable: false},
    });
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeDefined();
    expect(
      presenter.getPreviousHierarchyTreeForTrace(secondTrace),
    ).toBeUndefined();
  });

  it('explicitly sets selected tree', async () => {
    expect(presenter.getSelectedTree()).toBeUndefined();
    const selectedTree = {trace, tree: tree1, index: 0};
    presenter.setSelectedTree(selectedTree);
    expect(presenter.getSelectedTree()).toEqual(selectedTree);
    presenter.setSelectedTree(undefined);
    expect(presenter.getSelectedTree()).toBeUndefined();
  });

  it('adds current hierarchy trees', async () => {
    await presenter.addCurrentHierarchyTrees(
      {trace, trees: [tree1]},
      undefined,
    );
    expect(presenter.getCurrentEntryForTrace(trace)).toBeUndefined();
    expect(presenter.getCurrentHierarchyTreesForTrace(trace)?.length).toEqual(
      1,
    );
    expect(presenter.getAllCurrentHierarchyTrees()?.length).toEqual(1);
    expect(presenter.getAllFormattedTrees()?.length).toEqual(1);
    expect(presenter.getFormattedTreesByTrace(trace)?.length).toEqual(1);

    const entry = secondTrace.getEntry(0);
    await presenter.addCurrentHierarchyTrees(
      {trace: secondTrace, trees: [tree3], entry},
      tree3.id,
    );
    expect(presenter.getSelectedTree()).toEqual({
      trace: secondTrace,
      tree: tree3,
      index: 1,
    });

    await presenter.addCurrentHierarchyTrees(
      {trace, trees: [tree2]},
      undefined,
    );
    expect(presenter.getCurrentHierarchyTreesForTrace(trace)?.length).toEqual(
      2,
    );
    const formattedTrees = assertDefined(presenter.getAllFormattedTrees());
    expect(presenter.getAllCurrentHierarchyTrees()).toEqual([
      {
        trace,
        trees: [tree1, tree2],
        formattedTrees: formattedTrees.slice(0, 2),
        entry: undefined,
      },
      {
        trace: secondTrace,
        trees: [tree3],
        formattedTrees: formattedTrees.slice(2),
        entry,
      },
    ]);
    expect(formattedTrees.length).toEqual(3);
    expect(presenter.getFormattedTreesByTrace(trace)?.length).toEqual(2);
  });

  it('updates previous hierarchy trees', async () => {
    await applyTracePositionUpdate(0);
    await presenter.updatePreviousHierarchyTrees();
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeUndefined();

    await applyTracePositionUpdate(1);
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeUndefined();
    await presenter.updatePreviousHierarchyTrees();
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeDefined();
  });

  it('robust to empty trace position update', async () => {
    await applyTracePositionUpdate();
    expect(presenter.getCurrentEntryForTrace(trace)).toEqual(trace.getEntry(0));
    expect(presenter.getCurrentHierarchyTreesForTrace(trace)).toEqual([tree1]);

    await presenter.applyTracePositionUpdate([], '');
    expect(presenter.getCurrentEntryForTrace(trace)).toBeUndefined();
    expect(presenter.getCurrentHierarchyTreesForTrace(trace)).toBeUndefined();
  });

  it('adds diffs to hierarchy tree based on user option', async () => {
    await applyTracePositionUpdate(1);
    expect(
      getFormattedTree().findDfs((node) => node.name === 'Child3'),
    ).toBeUndefined();
    await presenter.applyHierarchyUserOptionsChange({
      showDiff: {name: '', enabled: true, isUnavailable: false},
    });
    expect(
      getFormattedTree()
        .findDfs((node) => node.name === 'Child3')
        ?.getDiff(),
    ).toEqual(DiffType.DELETED);
  });

  it('adds diffs based on modified properties', async () => {
    presenter = new HierarchyPresenter(
      {showDiff: {name: '', enabled: true, isUnavailable: false}},
      new TextFilter(),
      [],
      false,
      false,
    );
    await applyTracePositionUpdate(1);
    const tree = getFormattedTree();
    expect(tree.findDfs((node) => node.name === 'Parent1')?.getDiff()).toEqual(
      DiffType.MODIFIED,
    );
    expect(tree.findDfs((node) => node.name === 'Parent2')?.getDiff()).toEqual(
      DiffType.MODIFIED,
    );
  });

  it('applies deny list in add diffs operation', async () => {
    presenter = new HierarchyPresenter(
      {showDiff: {name: '', enabled: true, isUnavailable: false}},
      new TextFilter(),
      ['newProp', 'testProp'],
      false,
      false,
    );
    await applyTracePositionUpdate(1);
    expect(
      getFormattedTree()
        .findDfs((node) => node.name === 'Parent1')
        ?.getDiff(),
    ).toEqual(DiffType.NONE);
  });

  it('disables show diff and generates non-diff tree if no prev entry available', async () => {
    const opts = {showDiff: {name: '', enabled: false, isUnavailable: false}};
    presenter.applyHierarchyUserOptionsChange(opts);
    await applyTracePositionUpdate();
    expect(opts['showDiff'].isUnavailable).toBeTrue();
    const trees = assertDefined(presenter.getAllFormattedTrees());
    expect(trees.length).toBeGreaterThan(0);
    trees.forEach((tree) => {
      expect(tree.getAllChildren().length > 0).toBeTrue();
      tree.forEachNodeDfs((node) =>
        expect(node.getDiff()).toEqual(DiffType.NONE),
      );
    });
  });

  it('makes node display name by strategy', async () => {
    const testName = 'Test Name';
    presenter = new HierarchyPresenter(
      {},
      new TextFilter(),
      [],
      false,
      false,
      () => testName,
    );
    expect(presenter.getCurrentHierarchyTreeNames(trace)).toBeUndefined();
    await applyTracePositionUpdate();
    const node = assertDefined(presenter.getAllFormattedTrees()?.at(0));
    expect(node.name).toEqual('entry');
    expect(node.getDisplayName()).toEqual(testName);
    expect(presenter.getCurrentHierarchyTreeNames(trace)).toEqual([testName]);
  });

  it('disables headings based on showHeading', async () => {
    await applyTracePositionUpdate();
    let node = assertDefined(presenter.getAllFormattedTrees()?.at(0));
    expect(node.name).toEqual('entry');
    expect(node.heading()).toBeUndefined();

    presenter = new HierarchyPresenter({}, new TextFilter(), [], true, false);
    await applyTracePositionUpdate();
    node = assertDefined(presenter.getAllFormattedTrees()?.at(0));
    expect(node.name).toEqual('entry');
    expect(node.heading()).toEqual('Test');
  });

  it('selects first node based on forceSelectFirstNode', async () => {
    await applyTracePositionUpdate();
    expect(presenter.getSelectedTree()).toBeUndefined();

    presenter = new HierarchyPresenter({}, new TextFilter(), [], false, true);
    await applyTracePositionUpdate();
    expect(presenter.getSelectedTree()).toBeDefined();
  });

  it('applies custom operations', async () => {
    const operation = jasmine.createSpyObj('operation', ['apply']);
    presenter = new HierarchyPresenter(
      {},
      new TextFilter(),
      [],
      false,
      false,
      undefined,
      [[TraceType.SURFACE_FLINGER, [operation]]],
    );
    await applyTracePositionUpdate();
    expect(operation.apply).toHaveBeenCalledTimes(1);
  });

  it('propagates item selection to new entry', async () => {
    await applyTracePositionUpdate(0, '1 Parent1');
    expect(presenter.getSelectedTree()).toEqual({
      trace,
      tree: assertDefined(tree1.getChildByName('Parent1')),
      index: 0,
    });
  });

  it('handles pinned item change', () => {
    expect(presenter.getPinnedItems()).toEqual([]);
    const item = TreeNodeUtils.makeUiHierarchyNode({id: '', name: ''});
    presenter.applyPinnedItemChange(item);
    expect(presenter.getPinnedItems()).toEqual([item]);
    presenter.applyPinnedItemChange(item);
    expect(presenter.getPinnedItems()).toEqual([]);
  });

  it('flattens hierarchy tree based on user option', async () => {
    await applyTracePositionUpdate();
    expect(getTotalHierarchyChildren()).toEqual(2);
    await presenter.applyHierarchyUserOptionsChange({
      flat: {name: '', enabled: true},
    });
    expect(getTotalHierarchyChildren()).toEqual(3);
  });

  it('filters hierarchy tree by visibility based on user option', async () => {
    const userOptions: UserOptions = {
      showOnlyVisible: {name: '', enabled: false},
      flat: {name: '', enabled: true},
    };
    await presenter.applyHierarchyUserOptionsChange(userOptions);
    await applyTracePositionUpdate();
    expect(getTotalHierarchyChildren()).toEqual(3);

    const nonVisibleNode = assertDefined(
      getFormattedTree()?.findDfs(
        (node) =>
          !node.isRoot() &&
          !node.getEagerPropertyByName('isComputedVisible')?.getValue(),
      ),
    );
    presenter.applyPinnedItemChange(nonVisibleNode);

    userOptions['showOnlyVisible'].enabled = true;
    await presenter.applyHierarchyUserOptionsChange(userOptions);
    expect(getTotalHierarchyChildren()).toEqual(2);
    expect(presenter.getPinnedItems()).toEqual([nonVisibleNode]); // keeps pinned node

    presenter.clear(); // robust to no current entries
    await presenter.applyHierarchyUserOptionsChange(userOptions);
  });

  it('filters hierarchy tree by search string', async () => {
    const userOptions: UserOptions = {flat: {name: '', enabled: true}};
    await presenter.applyHierarchyUserOptionsChange(userOptions);
    await applyTracePositionUpdate();
    expect(getTotalHierarchyChildren()).toEqual(3);

    const filterString = 'Parent';
    const nonMatchNode = assertDefined(
      getFormattedTree()?.findDfs(
        (node) => !node.isRoot() && !node.id.includes(filterString),
      ),
    );
    presenter.applyPinnedItemChange(nonMatchNode);

    const filter = new TextFilter(filterString);
    await presenter.applyHierarchyFilterChange(filter);
    expect(presenter.getTextFilter()).toEqual(filter);
    expect(getTotalHierarchyChildren()).toEqual(2);
    expect(presenter.getPinnedItems()).toEqual([nonMatchNode]); // keeps pinned node

    presenter.clear(); // robust to no current entries
    await presenter.applyHierarchyFilterChange(filter);
  });

  it('simplifies names in hierarchy tree based on user option', async () => {
    const spy = spyOn(SimplifyNames.prototype, 'apply').and.callThrough();
    await applyTracePositionUpdate();
    expect(spy).not.toHaveBeenCalled();

    await presenter.applyHierarchyUserOptionsChange({
      simplifyNames: {name: '', enabled: true},
    });
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('applies highlighted id change', async () => {
    await applyTracePositionUpdate();
    presenter.applyHighlightedIdChange('fake node');
    expect(presenter.getSelectedTree()).toBeUndefined();
    presenter.applyHighlightedIdChange('1 Parent1');
    expect(presenter.getSelectedTree()).toBeDefined();
    presenter.clear(); // robust to no current entries
    presenter.applyHighlightedIdChange('1 Parent1');
  });

  it('applies highlighted node change', async () => {
    await applyTracePositionUpdate();
    const tree = getFormattedTree();
    tree.setIsOldNode(true);
    presenter.applyHighlightedNodeChange(tree);
    expect(presenter.getSelectedTree()).toBeUndefined();

    tree.setDiff(DiffType.DELETED);
    presenter.applyHighlightedNodeChange(tree);
    expect(presenter.getSelectedTree()).toEqual({trace, tree, index: 0});

    const newNode = assertDefined(tree.getChildByName('Parent1'));
    presenter.applyHighlightedNodeChange(newNode);
    expect(presenter.getSelectedTree()).toEqual({
      trace,
      tree: newNode,
      index: 0,
    });

    presenter.clear(); // robust to no current entries
    presenter.applyHighlightedNodeChange(tree);
  });

  it('can be cleared and re-populated', async () => {
    const testName = 'Test Name';
    presenter = new HierarchyPresenter(
      {showDiff: {name: 'Show diff', enabled: true}},
      new TextFilter(),
      [],
      false,
      false,
      () => testName,
    );
    await applyTracePositionUpdate(1, '1 Parent1');
    expect(presenter.getAllCurrentHierarchyTrees()).toBeDefined();
    expect(presenter.getAllFormattedTrees()).toBeDefined();
    expect(presenter.getSelectedTree()).toBeDefined();
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeDefined();
    expect(presenter.getCurrentEntryForTrace(trace)).toBeDefined();
    expect(presenter.getCurrentHierarchyTreeNames(trace)).toBeDefined();

    presenter.clear();
    expect(presenter.getAllCurrentHierarchyTrees()).toBeUndefined();
    expect(presenter.getAllFormattedTrees()).toBeUndefined();
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeUndefined();
    expect(presenter.getCurrentEntryForTrace(trace)).toBeUndefined();
    expect(presenter.getSelectedTree()).toBeUndefined();
    expect(presenter.getCurrentHierarchyTreeNames(trace)).toBeUndefined();

    await presenter.addCurrentHierarchyTrees(
      {trace, trees: [tree1]},
      '1 Parent1',
    );
    expect(presenter.getAllCurrentHierarchyTrees()).toBeDefined();
    expect(presenter.getAllFormattedTrees()).toBeDefined();
    expect(presenter.getSelectedTree()).toBeDefined();
    expect(presenter.getPreviousHierarchyTreeForTrace(trace)).toBeUndefined();
    expect(presenter.getCurrentEntryForTrace(trace)).toBeUndefined();
    expect(presenter.getCurrentHierarchyTreeNames(trace)).toBeUndefined();
  });

  it('get adjacent node robust to no current trees', () => {
    const storage = new InMemoryStorage();
    expect(presenter.getAdjacentVisibleNode(storage, false)).toBeUndefined();
    expect(presenter.getAdjacentVisibleNode(storage, true)).toBeUndefined();
  });

  it('gets next visible node via DFS', async () => {
    await applyTracePositionUpdate();
    const p1 = assertDefined(getFormattedTree().getChildByName('Parent1'));
    presenter.applyHighlightedNodeChange(p1);

    const storage = new InMemoryStorage();
    const adj = presenter.getAdjacentVisibleNode(storage, false);
    expect(adj?.id).toEqual('3 Child3');

    // next node is hidden so recursively finds next visible node
    storage.add(p1.id + '.collapsedState', 'true');
    expect(presenter.getAdjacentVisibleNode(storage, false)?.id).toEqual(
      '2 Parent2',
    );
  });

  it('gets next visible node as first root if no node selected', async () => {
    await applyTracePositionUpdate();
    const adj = presenter.getAdjacentVisibleNode(new InMemoryStorage(), false);
    expect(adj?.id).toEqual('Test Trace entry');
  });

  it('gets next visible node if selected node is final node of tree', async () => {
    await applyTracePositionUpdate(0, undefined, secondTrace);
    const p2 = assertDefined(
      getFormattedTree(secondTrace).getChildByName('Parent2'),
    );
    presenter.applyHighlightedNodeChange(
      assertDefined(p2.getChildByName('Child3')?.getChildByName('Child4')),
    );
    const storage = new InMemoryStorage();
    // already at final node of tree - returns undefined
    expect(presenter.getAdjacentVisibleNode(storage, false)).toBeUndefined();

    // already at final node of tree - node is hidden so returns non-hidden parent
    const storeKey = p2.id + '.collapsedState';
    storage.add(storeKey, 'true');
    expect(presenter.getAdjacentVisibleNode(storage, false)?.id).toEqual(p2.id);
    storage.clear(storeKey);

    // already at final node of first tree - returns next tree root
    await presenter.applyTracePositionUpdate(
      [trace.getEntry(0), secondTrace.getEntry(0)],
      '',
    );
    presenter.applyHighlightedNodeChange(
      assertDefined(getFormattedTree().getChildByName('Parent2')),
    );
    expect(presenter.getAdjacentVisibleNode(storage, false)?.id).toEqual(
      'Test Trace 2 entry',
    );
  });

  it('gets next visible node if selected node is not formatted', async () => {
    await applyTracePositionUpdate();
    const storage = new InMemoryStorage();
    // selected tree id present in current trees so returns next non hidden node
    presenter.applyHighlightedIdChange('1 Parent1');
    expect(presenter.getAdjacentVisibleNode(storage, false)?.id).toEqual(
      '3 Child3',
    );
    // selected tree id not present in current trees so returns first tree root
    presenter.setSelectedTree({trace: secondTrace, tree: tree3, index: 0});
    expect(presenter.getAdjacentVisibleNode(storage, false)?.id).toEqual(
      'Test Trace entry',
    );
  });

  it('gets prev visible node via DFS', async () => {
    await applyTracePositionUpdate(0, undefined, secondTrace);
    const p2 = getFormattedTree(secondTrace).getChildByName('Parent2');
    const c4 = assertDefined(
      p2?.getChildByName('Child3')?.getChildByName('Child4'),
    );
    presenter.applyHighlightedNodeChange(c4);

    const storage = new InMemoryStorage();
    const adj = presenter.getAdjacentVisibleNode(storage, true);
    expect(adj?.id).toEqual('3 Child3');

    // prev node is hidden so recursively finds prev visible node
    storage.add(p2?.id + '.collapsedState', 'true');
    expect(presenter.getAdjacentVisibleNode(storage, true)?.id).toEqual(
      '2 Parent2',
    );
  });

  it('gets prev visible node as first root if no node selected', async () => {
    await applyTracePositionUpdate();
    const adj = presenter.getAdjacentVisibleNode(new InMemoryStorage(), true);
    expect(adj?.id).toEqual('Test Trace entry');
  });

  it('gets prev visible node if selected node is first node of tree', async () => {
    await presenter.applyTracePositionUpdate(
      [trace.getEntry(0), secondTrace.getEntry(0)],
      '',
    );
    const formattedTree = getFormattedTree();
    presenter.applyHighlightedNodeChange(formattedTree);
    const storage = new InMemoryStorage();

    // already at first node of first tree - returns undefined
    expect(presenter.getAdjacentVisibleNode(storage, true)).toBeUndefined();

    // already at first node of second tree - returns final child of previous tree
    presenter.applyHighlightedNodeChange(getFormattedTree(secondTrace));
    expect(presenter.getAdjacentVisibleNode(storage, true)?.id).toEqual(
      '2 Parent2',
    );

    // already at first node of second tree - returns final non-collapsed child of previous tree
    storage.add(formattedTree.id + '.collapsedState', 'true');
    expect(presenter.getAdjacentVisibleNode(storage, true)?.id).toEqual(
      'Test Trace entry',
    );
  });

  it('gets prev visible node if selected node is not formatted', async () => {
    await applyTracePositionUpdate();
    const selectedTree = assertDefined(
      getFormattedTree().getChildByName('Parent1')?.getChildByName('Child3'),
    );
    presenter.applyHighlightedIdChange(selectedTree.id);
    const storage = new InMemoryStorage();

    // selected tree id present in current trees so returns prev non hidden node
    expect(presenter.getAdjacentVisibleNode(storage, true)?.id).toEqual(
      '1 Parent1',
    );

    // selected tree id not present in current trees so returns first tree root
    presenter.setSelectedTree({trace: secondTrace, tree: tree3, index: 0});
    expect(presenter.getAdjacentVisibleNode(storage, true)?.id).toEqual(
      'Test Trace entry',
    );
  });

  async function applyTracePositionUpdate(index = 0, item = '', t = trace) {
    await presenter.applyTracePositionUpdate([t.getEntry(index)], item);
  }

  function getFormattedTree(t = trace) {
    return assertDefined(presenter.getFormattedTreesByTrace(t))[0];
  }

  function getTotalHierarchyChildren() {
    return assertDefined(presenter.getAllFormattedTrees()).reduce(
      (tot, tree) => (tot += tree.getAllChildren().length),
      0,
    );
  }
});
