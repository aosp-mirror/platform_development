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
import {Component, NO_ERRORS_SCHEMA, ViewChild} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {UiTreeNode} from 'viewers/common/ui_tree_utils';
import {TreeComponent} from './tree_component';
import {TreeNodeComponent} from './tree_node_component';

describe('TreeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [TreeComponent, TestHostComponent, TreeNodeComponent],
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
    const treeNode = htmlElement.querySelector('tree-node');
    expect(treeNode).toBeTruthy();
  });

  it('can identify if a parent node has a selected child', () => {
    expect(component.treeComponent.hasSelectedChild()).toBeFalse();
    component.highlightedItem = 'child3';
    fixture.detectChanges();
    expect(component.treeComponent.hasSelectedChild()).toBeTrue();
  });

  it('highlights item upon node click', () => {
    const treeNode = htmlElement.querySelector('tree-node');
    expect(treeNode).toBeTruthy();

    const spy = spyOn(component.treeComponent.highlightedChange, 'emit');
    (treeNode as HTMLButtonElement).dispatchEvent(new MouseEvent('click', {detail: 1}));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('toggles tree upon node double click', () => {
    const treeNode = htmlElement.querySelector('tree-node');
    expect(treeNode).toBeTruthy();

    const currCollapseValue = component.treeComponent.localCollapsedState;
    (treeNode as HTMLButtonElement).dispatchEvent(new MouseEvent('click', {detail: 2}));
    fixture.detectChanges();
    expect(!currCollapseValue).toBe(component.treeComponent.localCollapsedState);
  });

  it('scrolls selected node into view if out of view', async () => {
    const treeNode = assertDefined(htmlElement.querySelector(`#nodechild50`));
    const spy = spyOn(treeNode, 'scrollIntoView');
    component.highlightedItem = 'child50';
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('does not scroll selected element if already in view', () => {
    const treeNode = assertDefined(htmlElement.querySelector(`#nodechild2`));
    const spy = spyOn(treeNode, 'scrollIntoView');
    component.highlightedItem = 'child2';
    fixture.detectChanges();
    expect(spy).not.toHaveBeenCalled();
  });

  function makeTreeNodeChildren(): UiTreeNode[] {
    const children = [];
    for (let i = 0; i < 60; i++) {
      children.push({kind: `${i}`, stableId: `child${i}`, name: `Child${i}`});
    }
    return children;
  }

  @Component({
    selector: 'host-component',
    template: `
      <tree-view
        [item]="item"
        [store]="store"
        [isFlattened]="false"
        [isPinned]="false"
        [highlightedItem]="highlightedItem"
        [itemsClickable]="true"></tree-view>
    `,
  })
  class TestHostComponent {
    item: UiTreeNode = {
      simplifyNames: false,
      kind: 'entry',
      name: 'LayerTraceEntry',
      stableId: 'LayerTraceEntry 2',
      children: makeTreeNodeChildren(),
    };
    store = new PersistentStore();
    highlightedItem = '';

    @ViewChild(TreeComponent)
    treeComponent!: TreeComponent;
  }
});
