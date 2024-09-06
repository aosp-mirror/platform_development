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
import {Component, ViewChild} from '@angular/core';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';
import {TreeNodeComponent} from './tree_node_component';

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [
        TreeNodeComponent,
        HierarchyTreeNodeDataViewComponent,
        PropertyTreeNodeDataViewComponent,
        TestHostComponent,
      ],
      imports: [MatIconModule, MatTooltipModule],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can generate data view component', () => {
    component.treeNodeComponent.isPropertyTreeNode = jasmine
      .createSpy()
      .and.returnValue(false);
    fixture.detectChanges();
    const treeNodeDataView = htmlElement.querySelector(
      'hierarchy-tree-node-data-view',
    );
    expect(treeNodeDataView).toBeTruthy();
  });

  it('can trigger tree toggle on click of chevron', () => {
    component.treeNodeComponent.showChevron = jasmine
      .createSpy()
      .and.returnValue(true);
    fixture.detectChanges();

    const spy = spyOn(component.treeNodeComponent.toggleTreeChange, 'emit');
    const toggleButton = assertDefined(
      htmlElement.querySelector('.toggle-tree-btn'),
    );
    (toggleButton as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalled();
  });

  it('can trigger tree expansion on click of expand tree button', () => {
    const spy = spyOn(component.treeNodeComponent.expandTreeChange, 'emit');
    const expandButton = assertDefined(
      htmlElement.querySelector('.expand-tree-btn'),
    );
    (expandButton as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalled();
  });

  it('can trigger node pin on click of star', () => {
    component.treeNodeComponent.showPinNodeIcon = jasmine
      .createSpy()
      .and.returnValue(true);
    fixture.detectChanges();

    const spy = spyOn(component.treeNodeComponent.pinNodeChange, 'emit');
    const pinNodeButton = assertDefined(
      htmlElement.querySelector('.pin-node-btn'),
    );
    (pinNodeButton as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalledWith(component.node);
  });

  it('can trigger rect show state toggle on click of icon', () => {
    component.treeNodeComponent.showStateIcon = 'visibility';
    fixture.detectChanges();

    const spy = spyOn(component.treeNodeComponent.rectShowStateChange, 'emit');
    const pinNodeButton = assertDefined(
      htmlElement.querySelector('.toggle-rect-show-state-btn'),
    );
    (pinNodeButton as HTMLButtonElement).click();
    expect(spy).toHaveBeenCalled();
  });

  @Component({
    selector: 'host-component',
    template: `
      <tree-node
        [node]="node"
        [isExpanded]="false"
        [isPinned]="false"
        [isInPinnedSection]="false"
        [isSelected]="isSelected"></tree-node>
    `,
  })
  class TestHostComponent {
    node = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('LayerTraceEntry')
        .setName('4')
        .setChildren([{id: 1, name: 'Child 1'}])
        .build(),
    );

    isSelected = false;

    @ViewChild(TreeNodeComponent)
    treeNodeComponent!: TreeNodeComponent;
  }
});
