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

import {Clipboard, ClipboardModule} from '@angular/cdk/clipboard';
import {Component, ViewChild} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {RectShowState} from 'viewers/common/rect_show_state';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';
import {TreeComponent} from './tree_component';
import {TreeNodeComponent} from './tree_node_component';

describe('TreeComponent', () => {
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;
  let mockCopyText: jasmine.Spy;

  beforeEach(async () => {
    mockCopyText = jasmine.createSpy();
    await TestBed.configureTestingModule({
      providers: [{provide: Clipboard, useValue: {copy: mockCopyText}}],
      declarations: [
        TreeComponent,
        TestHostComponent,
        TreeNodeComponent,
        HierarchyTreeNodeDataViewComponent,
        PropertyTreeNodeDataViewComponent,
      ],
      imports: [MatTooltipModule, MatIconModule, ClipboardModule],
    }).compileComponents();
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
  });

  it('can be created', () => {
    dom.detectChanges();
    expect(component).toBeTruthy();
  });

  it('shows node', () => {
    dom.detectChanges();
    expect(dom.find('tree-node')).toBeDefined();
  });

  it('can identify if a parent node has a selected child', () => {
    dom.detectChanges();
    const treeNode = dom.get('tree-node');
    treeNode.checkClassName('child-selected', false);
    component.highlightedItem = '3 Child3';
    dom.detectChanges();
    treeNode.checkClassName('child-selected', true);
  });

  it('highlights node and inner node upon click', () => {
    dom.detectChanges();
    const spy = spyOn(
      assertDefined(component.treeComponent).highlightedChange,
      'emit',
    );
    const treeNodes = dom.findAll('tree-node');
    treeNodes[0].dispatchEvent(new MouseEvent('click', {detail: 1}));
    expect(spy).toHaveBeenCalledTimes(1);
    treeNodes[1].click();
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('toggles tree upon node double click', () => {
    dom.detectChanges();
    const toggleButton = dom.get('.toggle-tree-btn');
    toggleButton.checkTextExact('arrow_drop_down');
    checkIsExpanded(true);
    doubleClickFirstNode();
    toggleButton.checkTextExact('chevron_right');
    checkIsExpanded(false);
  });

  it('does not toggle tree in flat mode on double click', () => {
    dom.detectChanges();
    component.isFlattened = true;
    dom.detectChanges();
    doubleClickFirstNode();
    checkIsExpanded(true);
  });

  it('pins node on click', () => {
    dom.detectChanges();
    const spy = spyOn(
      assertDefined(component.treeComponent).pinnedItemChange,
      'emit',
    );
    dom.findAndClick('.pin-node-btn');
    expect(spy).toHaveBeenCalled();
  });

  it('expands tree on expand tree button click', () => {
    dom.detectChanges();
    doubleClickFirstNode();
    checkIsExpanded(false);
    dom.findAndClick('.expand-tree-btn');
    checkIsExpanded(true);
  });

  it('expands tree recursively on node selection', () => {
    dom.detectChanges();
    doubleClickFirstNode();
    checkIsExpanded(false);
    component.highlightedItem = '79 Child79';
    dom.detectChanges();
    checkIsExpanded(true);
  });

  it('scrolls selected node only if not in view', () => {
    dom.detectChanges();
    checkNodeScrolling();
  });

  it('scrolls selected node if not in view even if pinned', () => {
    component.pinnedItems = [
      assertDefined(component.tree.getChildByName('Child78')),
      assertDefined(component.tree.getChildByName('Child79')),
    ];
    dom.detectChanges();
    checkNodeScrolling();
  });

  it('sets initial expanded state to true by default for leaf', () => {
    dom.detectChanges();
    checkIsExpanded(true);
  });

  it('sets initial expanded state to true by default for non root', () => {
    const child = component.tree.getAllChildren()[0] as UiHierarchyTreeNode;
    const innerChild = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('InnerChild')
        .setName('child')
        .setChildren([])
        .build(),
    );
    child.addOrReplaceChild(innerChild);
    component.tree = child;
    dom.detectChanges();
    checkIsExpanded(true);
  });

  it('sets initial expanded state to false if collapse state exists in store', () => {
    component.useStoredExpandedState = true;
    dom.detectChanges();
    // tree expanded by default
    checkIsExpanded(true);

    // tree collapsed
    doubleClickFirstNode();
    checkIsExpanded(false);

    // tree collapsed state retained
    component.tree = makeTree();
    dom.detectChanges();
    checkIsExpanded(false);
  });

  it('renders show state button if applicable', () => {
    dom.detectChanges();
    expect(dom.find('.toggle-rect-show-state-btn')).toBeUndefined();
    expect(dom.find('.children.with-gutter')).toBeUndefined();

    const id = component.tree.id;
    component.rectIdToShowState = new Map([[id, RectShowState.HIDE]]);
    dom.detectChanges();
    expect(dom.find('.children.with-gutter')).toBeDefined();
    dom.get('.toggle-rect-show-state-btn').checkTextExact('visibility_off');

    component.rectIdToShowState = new Map([[id, RectShowState.SHOW]]);
    dom.detectChanges();
    dom.get('.toggle-rect-show-state-btn').checkTextExact('visibility');
  });

  it('handles show state button click', () => {
    component.rectIdToShowState = new Map([
      [component.tree.id, RectShowState.HIDE],
    ]);
    dom.detectChanges();
    const button = dom.get('.toggle-rect-show-state-btn');
    button.checkTextExact('visibility_off');

    let id = '';
    dom.addEventListener(ViewerEvents.RectShowStateChange, (event) => {
      const detail = (event as CustomEvent).detail;
      id = detail.rectId;
      component.rectIdToShowState?.set(detail.rectId, detail.state);
    });
    button.click();
    expect(component.rectIdToShowState.get(id)).toEqual(RectShowState.SHOW);
    button.click();
    expect(component.rectIdToShowState.get(id)).toEqual(RectShowState.HIDE);
  });

  it('shows node at full opacity when applicable', () => {
    dom.detectChanges();
    expect(dom.find('.node.full-opacity')).toBeDefined();

    component.rectIdToShowState = new Map([
      [component.tree.id, RectShowState.SHOW],
    ]);
    dom.detectChanges();
    expect(dom.find('.node.full-opacity')).toBeDefined();

    component.tree = TreeNodeUtils.makeUiPropertyNode(
      component.tree.id,
      component.tree.name,
      0,
    );
    dom.detectChanges();
    expect(dom.find('.node.full-opacity')).toBeDefined();
  });

  it('shows node at non-full opacity when applicable', () => {
    component.rectIdToShowState = new Map([]);
    dom.detectChanges();
    expect(dom.find('.node.full-opacity')).toBeUndefined();

    component.rectIdToShowState = new Map([
      [component.tree.id, RectShowState.HIDE],
    ]);
    dom.detectChanges();
    expect(dom.find('.node.full-opacity')).toBeUndefined();
  });

  it('copies text via copy button without selecting node', () => {
    dom.detectChanges();
    component.tree = TreeNodeUtils.makeUiPropertyNode(
      component.tree.id,
      component.tree.name,
      0,
    );
    dom.detectChanges();

    const spy = spyOn(assertDefined(component.treeComponent), 'onNodeClick');
    dom.findAndClick('.icon-wrapper-copy button');
    expect(mockCopyText).toHaveBeenCalled();
    expect(spy).not.toHaveBeenCalled();
  });

  function makeTree() {
    const children = [];
    for (let i = 0; i < 80; i++) {
      children.push({id: i, name: `Child${i}`});
    }
    return UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('RootNode')
        .setName('Root node')
        .setChildren(children)
        .build(),
    );
  }

  function doubleClickFirstNode() {
    dom.get('tree-node').dispatchEvent(new MouseEvent('click', {detail: 2}));
  }

  function checkIsExpanded(isExpanded: boolean) {
    expect(dom.get('.children').getHTMLElement().hidden).toEqual(!isExpanded);
  }

  function checkNodeScrolling() {
    const treeNode = dom.get(`#nodeChild79`).getHTMLElement();
    const spy = spyOn(treeNode, 'scrollIntoView').and.callThrough();

    component.highlightedItem = 'Root node';
    dom.detectChanges();

    component.highlightedItem = '79 Child79';
    dom.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);

    component.highlightedItem = '78 Child78';
    dom.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);
  }

  @Component({
    selector: 'host-component',
    template: `
    <div class="tree-wrapper">
      <tree-view
        [node]="tree"
        [isFlattened]="isFlattened"
        [pinnedItems]="pinnedItems"
        [highlightedItem]="highlightedItem"
        [useStoredExpandedState]="useStoredExpandedState"
        [itemsClickable]="true"
        [rectIdToShowState]="rectIdToShowState"></tree-view>
    </div>
    `,
    styles: [
      `
      .tree-wrapper {
        height: 500px;
        overflow: auto;
      }
    `,
    ],
  })
  class TestHostComponent {
    tree: UiHierarchyTreeNode | UiPropertyTreeNode;
    highlightedItem = '';
    isFlattened = false;
    useStoredExpandedState = false;
    rectIdToShowState: Map<string, RectShowState> | undefined;
    pinnedItems: Array<UiHierarchyTreeNode | UiPropertyTreeNode> = [];

    constructor() {
      this.tree = makeTree();
    }

    @ViewChild(TreeComponent)
    treeComponent: TreeComponent | undefined;
  }
});
