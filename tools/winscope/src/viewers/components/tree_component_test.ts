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

  it('can identify if a parent node has a selected child', () => {
    expect(component.treeComponent.hasSelectedChild()).toBeFalse();
    component.highlightedItems.push('3');
    fixture.detectChanges();
    expect(component.treeComponent.hasSelectedChild()).toBeTrue();
  });

  @Component({
    selector: 'host-component',
    template: `
      <tree-view
        [item]="item"
        [store]="store"
        [isFlattened]="false"
        [isPinned]="false"
        [highlightedItems]="highlightedItems"
        [itemsClickable]="true"></tree-view>
    `,
  })
  class TestHostComponent {
    item: UiTreeNode = {
      simplifyNames: false,
      kind: 'entry',
      name: 'LayerTraceEntry',
      stableId: '2',
      children: [{kind: '3', stableId: '3', name: 'Child1'}],
    };
    store = new PersistentStore();
    highlightedItems: string[] = [];

    @ViewChild(TreeComponent)
    treeComponent!: TreeComponent;
  }
});
