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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {DEFAULT_PROPERTY_FORMATTER} from 'trace/tree_node/formatters';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';
import {TreeNodeComponent} from './tree_node_component';

describe('TreeNodeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;
  let mockCopyText: jasmine.Spy;

  const propertiesTree = UiPropertyTreeNode.from(
    new PropertyTreeBuilder()
      .setRootId('test')
      .setName('property tree')
      .setChildren([
        {name: 'key1', value: 'value1', formatter: DEFAULT_PROPERTY_FORMATTER},
        {name: 'key2', children: [{name: 'key3'}]},
      ])
      .build(),
  );
  propertiesTree.setIsRoot(true);

  beforeEach(async () => {
    mockCopyText = jasmine.createSpy();
    await TestBed.configureTestingModule({
      providers: [{provide: Clipboard, useValue: {copy: mockCopyText}}],
      declarations: [
        TreeNodeComponent,
        HierarchyTreeNodeDataViewComponent,
        PropertyTreeNodeDataViewComponent,
        TestHostComponent,
      ],
      imports: [MatIconModule, MatTooltipModule, ClipboardModule],
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
    assertDefined(component.treeNodeComponent).isPropertyTreeNode = jasmine
      .createSpy()
      .and.returnValue(false);
    fixture.detectChanges();
    const treeNodeDataView = htmlElement.querySelector(
      'hierarchy-tree-node-data-view',
    );
    expect(treeNodeDataView).toBeTruthy();
  });

  it('can trigger tree toggle on click of chevron', () => {
    const treeNodeComponent = assertDefined(component.treeNodeComponent);
    treeNodeComponent.showChevron = jasmine.createSpy().and.returnValue(true);
    fixture.detectChanges();

    const spy = spyOn(treeNodeComponent.toggleTreeChange, 'emit');
    const toggleButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.toggle-tree-btn'),
    );
    toggleButton.click();
    expect(spy).toHaveBeenCalled();
  });

  it('can trigger tree expansion on click of expand tree button', () => {
    const spy = spyOn(
      assertDefined(component.treeNodeComponent).expandTreeChange,
      'emit',
    );
    const expandButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.expand-tree-btn'),
    );
    expandButton.click();
    expect(spy).toHaveBeenCalled();
  });

  it('pins node on click', () => {
    const treeNodeComponent = assertDefined(component.treeNodeComponent);
    treeNodeComponent.showPinNodeIcon = jasmine
      .createSpy()
      .and.returnValue(true);
    fixture.detectChanges();

    const spy = spyOn(treeNodeComponent.pinNodeChange, 'emit');
    const pinNodeButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.pin-node-btn'),
    );
    pinNodeButton.click();
    expect(spy).toHaveBeenCalledWith(component.node as UiHierarchyTreeNode);
  });

  it('can trigger rect show state toggle on click of icon', () => {
    const treeNodeComponent = assertDefined(component.treeNodeComponent);
    treeNodeComponent.showStateIcon = 'visibility';
    fixture.detectChanges();

    const spy = spyOn(treeNodeComponent.rectShowStateChange, 'emit');
    const showStateButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.toggle-rect-show-state-btn'),
    );
    showStateButton.click();
    expect(spy).toHaveBeenCalled();
  });

  it('does not show copy button for hierarchy tree', () => {
    expect(htmlElement.querySelector('.icon-wrapper-copy')).toBeNull();
  });

  it('does not show copy button for property tree node that is not leaf or root', () => {
    component.node = assertDefined(propertiesTree.getChildByName('key2'));
    fixture.detectChanges();
    expect(htmlElement.querySelector('.icon-wrapper-copy')).toBeNull();
  });

  it('copies node name for root of property tree node', () => {
    component.node = propertiesTree;
    fixture.detectChanges();
    const copyButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.icon-wrapper-copy button'),
    );
    copyButton.click();
    fixture.detectChanges();
    expect(mockCopyText).toHaveBeenCalledWith(propertiesTree.name);
  });

  it('copies property name and value for leaf node', () => {
    component.node = assertDefined(propertiesTree.getChildByName('key1'));
    component.isLeaf = true;
    fixture.detectChanges();
    const copyButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.icon-wrapper-copy button'),
    );
    copyButton.click();
    fixture.detectChanges();
    expect(mockCopyText).toHaveBeenCalledWith('key1: value1');
  });

  @Component({
    selector: 'host-component',
    template: `
      <tree-node
        [node]="node"
        [isExpanded]="isExpanded"
        [isPinned]="false"
        [isInPinnedSection]="false"
        [isSelected]="isSelected"
        [isLeaf]="isLeaf"></tree-node>
    `,
  })
  class TestHostComponent {
    node: UiHierarchyTreeNode | UiPropertyTreeNode = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('LayerTraceEntry')
        .setName('4')
        .setChildren([{id: 1, name: 'Child 1'}])
        .build(),
    );

    isSelected = false;
    isLeaf = false;
    isExpanded = false;

    @ViewChild(TreeNodeComponent)
    treeNodeComponent: TreeNodeComponent | undefined;
  }
});
