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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
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
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;
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
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
  });

  it('can be created', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('shows node', () => {
    fixture.detectChanges();
    expect(htmlElement.querySelector('tree-node')).toBeTruthy();
  });

  it('can identify if a parent node has a selected child', () => {
    fixture.detectChanges();
    const treeNode = assertDefined(
      htmlElement.querySelector<HTMLElement>('tree-node'),
    );
    expect(treeNode.className.includes('child-selected')).toBeFalse();
    component.highlightedItem = '3 Child3';
    fixture.detectChanges();
    expect(treeNode.className.includes('child-selected')).toBeTrue();
  });

  it('highlights node and inner node upon click', () => {
    fixture.detectChanges();
    const treeNodes = assertDefined(
      htmlElement.querySelectorAll<HTMLElement>('tree-node'),
    );

    const spy = spyOn(
      assertDefined(component.treeComponent).highlightedChange,
      'emit',
    );
    treeNodes.item(0).dispatchEvent(new MouseEvent('click', {detail: 1}));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);

    treeNodes.item(1).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('toggles tree upon node double click', () => {
    fixture.detectChanges();
    const toggleButton = assertDefined(
      htmlElement.querySelector('.toggle-tree-btn'),
    );
    expect(toggleButton.textContent?.trim()).toEqual('arrow_drop_down');
    checkIsExpanded(true);

    doubleClickFirstNode();
    expect(toggleButton.textContent?.trim()).toEqual('chevron_right');
    checkIsExpanded(false);
  });

  it('does not toggle tree in flat mode on double click', () => {
    fixture.detectChanges();
    component.isFlattened = true;
    fixture.detectChanges();
    doubleClickFirstNode();
    checkIsExpanded(true);
  });

  it('pins node on click', () => {
    fixture.detectChanges();
    const pinNodeButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.pin-node-btn'),
    );
    const spy = spyOn(
      assertDefined(component.treeComponent).pinnedItemChange,
      'emit',
    );
    pinNodeButton.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('expands tree on expand tree button click', () => {
    fixture.detectChanges();
    doubleClickFirstNode();
    checkIsExpanded(false);

    assertDefined(
      htmlElement.querySelector<HTMLElement>('.expand-tree-btn'),
    ).click();
    fixture.detectChanges();
    checkIsExpanded(true);
  });

  it('expands tree recursively on node selection', () => {
    fixture.detectChanges();
    doubleClickFirstNode();
    checkIsExpanded(false);
    component.highlightedItem = '79 Child79';
    fixture.detectChanges();
    checkIsExpanded(true);
  });

  it('scrolls selected node only if not in view', () => {
    fixture.detectChanges();
    checkNodeScrolling();
  });

  it('scrolls selected node if not in view even if pinned', () => {
    component.pinnedItems = [
      assertDefined(component.tree.getChildByName('Child78')),
      assertDefined(component.tree.getChildByName('Child79')),
    ];
    fixture.detectChanges();
    checkNodeScrolling();
  });

  it('sets initial expanded state to true by default for leaf', () => {
    fixture.detectChanges();
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
    fixture.detectChanges();
    checkIsExpanded(true);
  });

  it('sets initial expanded state to false if collapse state exists in store', () => {
    component.useStoredExpandedState = true;
    fixture.detectChanges();
    // tree expanded by default
    checkIsExpanded(true);

    // tree collapsed
    doubleClickFirstNode();
    checkIsExpanded(false);

    // tree collapsed state retained
    component.tree = makeTree();
    fixture.detectChanges();
    checkIsExpanded(false);
  });

  it('renders show state button if applicable', () => {
    fixture.detectChanges();
    expect(htmlElement.querySelector('.toggle-rect-show-state-btn')).toBeNull();
    expect(htmlElement.querySelector('.children.with-gutter')).toBeNull();

    component.rectIdToShowState = new Map([
      [component.tree.id, RectShowState.HIDE],
    ]);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.children.with-gutter')).toBeTruthy();
    expect(
      assertDefined(htmlElement.querySelector('.toggle-rect-show-state-btn'))
        .textContent,
    ).toContain('visibility_off');

    component.rectIdToShowState.set(component.tree.id, RectShowState.SHOW);
    fixture.detectChanges();
    expect(
      assertDefined(htmlElement.querySelector('.toggle-rect-show-state-btn'))
        .textContent,
    ).toContain('visibility');
  });

  it('handles show state button click', () => {
    component.rectIdToShowState = new Map([
      [component.tree.id, RectShowState.HIDE],
    ]);
    fixture.detectChanges();
    const button = assertDefined(
      htmlElement.querySelector<HTMLElement>('.toggle-rect-show-state-btn'),
    );
    expect(button.textContent).toContain('visibility_off');

    let id = '';
    htmlElement.addEventListener(ViewerEvents.RectShowStateChange, (event) => {
      const detail = (event as CustomEvent).detail;
      id = detail.rectId;
      component.rectIdToShowState?.set(detail.rectId, detail.state);
    });
    button.click();
    fixture.detectChanges();
    expect(component.rectIdToShowState.get(id)).toEqual(RectShowState.SHOW);

    button.click();
    fixture.detectChanges();
    expect(component.rectIdToShowState.get(id)).toEqual(RectShowState.HIDE);
  });

  it('shows node at full opacity when applicable', () => {
    fixture.detectChanges();
    expect(htmlElement.querySelector('.node.full-opacity')).toBeTruthy();

    component.rectIdToShowState = new Map([
      [component.tree.id, RectShowState.SHOW],
    ]);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.node.full-opacity')).toBeTruthy();

    component.tree = TreeNodeUtils.makeUiPropertyNode(
      component.tree.id,
      component.tree.name,
      0,
    );
    fixture.detectChanges();
    expect(htmlElement.querySelector('.node.full-opacity')).toBeTruthy();
  });

  it('shows node at non-full opacity when applicable', () => {
    component.rectIdToShowState = new Map([]);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.node.full-opacity')).toBeNull();

    component.rectIdToShowState = new Map([
      [component.tree.id, RectShowState.HIDE],
    ]);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.node.full-opacity')).toBeNull();
  });

  it('copies text via copy button without selecting node', () => {
    fixture.detectChanges();

    component.tree = TreeNodeUtils.makeUiPropertyNode(
      component.tree.id,
      component.tree.name,
      0,
    );
    fixture.detectChanges();

    const spy = spyOn(assertDefined(component.treeComponent), 'onNodeClick');
    const copyButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.icon-wrapper-copy button'),
    );
    copyButton.click();
    fixture.detectChanges();
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
    assertDefined(
      htmlElement.querySelector<HTMLElement>('tree-node'),
    ).dispatchEvent(new MouseEvent('click', {detail: 2}));
    fixture.detectChanges();
  }

  function checkIsExpanded(isExpanded: boolean) {
    expect(htmlElement.querySelector<HTMLElement>('.children')?.hidden).toEqual(
      !isExpanded,
    );
  }

  function checkNodeScrolling() {
    const treeNode = assertDefined(htmlElement.querySelector(`#nodeChild79`));
    const spy = spyOn(treeNode, 'scrollIntoView').and.callThrough();

    component.highlightedItem = 'Root node';
    fixture.detectChanges();

    component.highlightedItem = '79 Child79';
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);

    component.highlightedItem = '78 Child78';
    fixture.detectChanges();
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
