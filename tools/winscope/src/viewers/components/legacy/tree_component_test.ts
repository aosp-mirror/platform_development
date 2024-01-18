/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {Component, NO_ERRORS_SCHEMA, QueryList, ViewChildren} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {UiTreeNode} from 'viewers/common/ui_tree_utils_legacy';
import {TreeComponentLegacy} from './tree_component';
import {TreeNodeComponentLegacy} from './tree_node_component';

describe('TreeComponentLegacy', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;
  const children = makeTreeNodeChildren();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [TreeComponentLegacy, TestHostComponent, TreeNodeComponentLegacy],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('shows node', () => {
    const treeNode = htmlElement.querySelector('tree-node-legacy');
    expect(treeNode).toBeTruthy();
  });

  it('can identify if a parent node has a selected child', () => {
    expect(component.treeComponents.first.hasSelectedChild()).toBeFalse();
    component.highlightedItem = '1child3';
    fixture.detectChanges();
    expect(component.treeComponents.first.hasSelectedChild()).toBeTrue();
  });

  it('highlights item upon node click', () => {
    const treeNode = htmlElement.querySelector('tree-node-legacy');
    expect(treeNode).toBeTruthy();

    const spy = spyOn(component.treeComponents.first.highlightedChange, 'emit');
    (treeNode as HTMLButtonElement).dispatchEvent(new MouseEvent('click', {detail: 1}));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('toggles tree upon node double click', () => {
    const treeNode = htmlElement.querySelector('tree-node-legacy');
    expect(treeNode).toBeTruthy();

    const currCollapseValue = component.treeComponents.first.localExpandedState;
    (treeNode as HTMLButtonElement).dispatchEvent(new MouseEvent('click', {detail: 2}));
    fixture.detectChanges();
    expect(!currCollapseValue).toBe(component.treeComponents.first.localExpandedState);
  });

  it('scrolls selected node into view if out of view', () => {
    const treeNode = assertDefined(htmlElement.querySelector('#node1child50'));
    const spy = spyOn(treeNode, 'scrollIntoView');
    component.highlightedItem = '1child50';
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('does not scroll selected element if already in view', () => {
    const treeNode = assertDefined(htmlElement.querySelector('#node1child2'));
    const spy = spyOn(treeNode, 'scrollIntoView');
    component.highlightedItem = '1child2';
    fixture.detectChanges();
    expect(spy).not.toHaveBeenCalled();
  });

  it('sets initial expanded state to true by default', () => {
    const tree = assertDefined(component.treeComponents.get(1));
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeTrue();
  });

  it('does not initially set expanded state to true if already exists in store', () => {
    // item1 expanded by default
    const tree = assertDefined(component.treeComponents.get(1));
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeTrue();

    // item1 collapsed
    tree.toggleTree();
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeFalse();

    // item0 expanded by default
    component.itemWithStoredExpandedState = component.item0;
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeTrue();

    // item0 collapsed state retained
    component.itemWithStoredExpandedState = component.item1;
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeFalse();
  });

  function makeTreeNodeChildren(): UiTreeNode[] {
    const children = [];
    for (let i = 0; i < 60; i++) {
      const child: UiTreeNode = {
        kind: `${i}`,
        stableId: `1child${i}`,
        name: `Child${i}`,
        children: [
          {kind: `${i}`, stableId: `1innerChild${i}`, name: `InnerChild${i}`, children: []},
        ],
      };
      children.push(child);
    }
    return children;
  }

  @Component({
    selector: 'host-component',
    template: `
      <tree-view-legacy
        [item]="item0"
        [store]="store"
        [isFlattened]="false"
        [isPinned]="false"
        [highlightedItem]="highlightedItem"
        [itemsClickable]="true"></tree-view-legacy>

      <tree-view-legacy
        [item]="itemWithStoredExpandedState"
        [store]="store"
        [isFlattened]="false"
        [isPinned]="false"
        [highlightedItem]="highlightedItem"
        [useStoredExpandedState]="true"
        [itemsClickable]="true"></tree-view-legacy>
    `,
  })
  class TestHostComponent {
    item0: UiTreeNode = {
      simplifyNames: false,
      kind: 'entry',
      name: 'LayerTraceEntry',
      stableId: 'LayerTraceEntry 1',
      children,
    };

    item1: UiTreeNode = {
      simplifyNames: false,
      kind: 'entry',
      name: 'LayerTraceEntry',
      stableId: 'LayerTraceEntry 2',
      children: [
        {
          kind: '1',
          stableId: '2child1',
          name: 'Child1',
          children: [],
        },
      ],
    };

    itemWithStoredExpandedState = this.item1;

    store = new PersistentStore();
    highlightedItem = '';

    constructor() {
      localStorage.clear();
    }

    @ViewChildren(TreeComponentLegacy)
    treeComponents!: QueryList<TreeComponentLegacy>;
  }
});
