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
import {
  Component,
  CUSTOM_ELEMENTS_SCHEMA,
  QueryList,
  ViewChildren,
} from '@angular/core';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';
import {TreeComponent} from './tree_component';
import {TreeNodeComponent} from './tree_node_component';

describe('TreeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [
        TreeComponent,
        TestHostComponent,
        TreeNodeComponent,
        HierarchyTreeNodeDataViewComponent,
        PropertyTreeNodeDataViewComponent,
      ],
      imports: [MatTooltipModule, MatIconModule],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
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
    const treeNode = htmlElement.querySelector('tree-node');
    expect(treeNode).toBeTruthy();
  });

  it('can identify if a parent node has a selected child', () => {
    expect(component.treeComponents.first.hasSelectedChild()).toBeFalse();
    component.highlightedItem = '3 Child3';
    fixture.detectChanges();
    expect(component.treeComponents.first.hasSelectedChild()).toBeTrue();
  });

  it('highlights node upon click', () => {
    const treeNode = assertDefined(htmlElement.querySelector('tree-node'));

    const spy = spyOn(component.treeComponents.first.highlightedChange, 'emit');
    (treeNode as HTMLButtonElement).dispatchEvent(
      new MouseEvent('click', {detail: 1}),
    );
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('toggles tree upon node double click', () => {
    const treeNode = assertDefined(htmlElement.querySelector('tree-node'));

    const currLocalExpandedState =
      component.treeComponents.first.localExpandedState;
    (treeNode as HTMLButtonElement).dispatchEvent(
      new MouseEvent('click', {detail: 2}),
    );
    fixture.detectChanges();
    expect(!currLocalExpandedState).toBe(
      component.treeComponents.first.localExpandedState,
    );
  });

  it('scrolls selected node only if not in view', () => {
    const tree = assertDefined(component.treeComponents.get(0));
    const treeNode = assertDefined(
      tree.elementRef.nativeElement.querySelector(`#nodeChild79`),
    );

    component.highlightedItem = 'Root node';
    fixture.detectChanges();

    const spy = spyOn(treeNode, 'scrollIntoView').and.callThrough();
    component.highlightedItem = '79 Child79';
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);

    component.highlightedItem = '78 Child78';
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('sets initial expanded state to true by default', () => {
    const tree = assertDefined(component.treeComponents.get(1));
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeTrue();
  });

  it('does not initially set expanded state to true if already exists in store', () => {
    // tree1 expanded by default
    const tree = assertDefined(component.treeComponents.get(1));
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeTrue();

    // tree1 collapsed
    tree.toggleTree();
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeFalse();

    // tree0 expanded by default
    component.itemWithStoredExpandedState = component.tree0;
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeTrue();

    // tree1 collapsed state retained
    component.itemWithStoredExpandedState = component.tree1;
    fixture.detectChanges();
    expect(tree.isExpanded()).toBeFalse();
  });

  @Component({
    selector: 'host-component',
    template: `
    <div class="tree-wrapper">
      <tree-view
        [node]="tree0"
        [store]="store"
        [isFlattened]="false"
        [isPinned]="false"
        [highlightedItem]="highlightedItem"
        [itemsClickable]="true"></tree-view>
    </div>

    <div class="tree-wrapper">
      <tree-view
        [node]="itemWithStoredExpandedState"
        [store]="store"
        [isFlattened]="false"
        [isPinned]="false"
        [highlightedItem]="highlightedItem"
        [useStoredExpandedState]="true"
        [itemsClickable]="true"></tree-view>
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
    tree0: UiHierarchyTreeNode;

    tree1 = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('RootNode2')
        .setName('Root node')
        .setChildren([{id: 0, name: 'Child0'}])
        .build(),
    );

    itemWithStoredExpandedState = this.tree1;

    store = new PersistentStore();
    highlightedItem = '';

    constructor() {
      localStorage.clear();
      const children = [];
      for (let i = 0; i < 80; i++) {
        children.push({id: i, name: `Child${i}`});
      }
      this.tree0 = UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('RootNode')
          .setName('Root node')
          .setChildren(children)
          .build(),
      );
    }

    @ViewChildren(TreeComponent)
    treeComponents!: QueryList<TreeComponent>;
  }
});
