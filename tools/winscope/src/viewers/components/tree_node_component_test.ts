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
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {DEFAULT_PROPERTY_FORMATTER} from 'trace/tree_node/formatters';
import {DiffType} from 'viewers/common/diff_type';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';
import {TreeNodeComponent} from './tree_node_component';

describe('TreeNodeComponent', () => {
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;
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
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('can generate hierarchy data view component', () => {
    expect(dom.find('hierarchy-tree-node-data-view')).toBeDefined();
    expect(dom.find('property-tree-node-data-view')).toBeUndefined();
  });

  it('can generate property data view component', () => {
    component.node = propertiesTree;
    dom.detectChanges();
    expect(dom.find('property-tree-node-data-view')).toBeDefined();
    expect(dom.find('hierarchy-tree-node-data-view')).toBeUndefined();
  });

  it('can trigger tree toggle on click of chevron', () => {
    const treeNodeComponent = assertDefined(component.treeNodeComponent);
    treeNodeComponent.showChevron = jasmine.createSpy().and.returnValue(true);
    dom.detectChanges();

    const spy = spyOn(treeNodeComponent.toggleTreeChange, 'emit');
    dom.findAndClick('.toggle-tree-btn');
    expect(spy).toHaveBeenCalled();
  });

  it('can trigger tree expansion on click of expand tree button', () => {
    const spy = spyOn(
      assertDefined(component.treeNodeComponent).expandTreeChange,
      'emit',
    );
    dom.findAndClick('.expand-tree-btn');
    expect(spy).toHaveBeenCalled();
  });

  it('can trigger tree expansion if node is selected and not in pinned section', () => {
    const spy = spyOn(
      assertDefined(component.treeNodeComponent).expandTreeChange,
      'emit',
    );
    component.isInPinnedSection = true;
    component.isSelected = true;
    dom.detectChanges();
    expect(spy).not.toHaveBeenCalled();

    component.isSelected = false;
    component.isInPinnedSection = false;
    dom.detectChanges();
    component.isSelected = true;
    dom.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('assigns diff css classes to expand tree button', () => {
    const expandButton = dom.get('.expand-tree-btn');
    expandButton.checkClassNameExact('icon-button expand-tree-btn');
    component.node = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('LayerTraceEntry')
        .setName('Added Diff')
        .setChildren([
          {id: 1, name: 'Child 1', children: [{id: 2, name: 'Child 2'}]},
        ])
        .build(),
    );
    component.node.getChildByName('Child 1')?.setDiff(DiffType.ADDED);
    dom.detectChanges();
    expandButton.checkClassNameExact('icon-button expand-tree-btn added');

    component.node = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('LayerTraceEntry')
        .setName('Added Diff')
        .setChildren([
          {id: 1, name: 'Child 1', children: [{id: 2, name: 'Child 2'}]},
        ])
        .build(),
    );
    const child1 = assertDefined(component.node.getChildByName('Child 1'));
    child1.setDiff(DiffType.ADDED);
    child1.getChildByName('Child 2')?.setDiff(DiffType.DELETED);
    dom.detectChanges();
    expandButton.checkClassNameExact('icon-button expand-tree-btn modified');
  });

  it('pins node on click', () => {
    const treeNodeComponent = assertDefined(component.treeNodeComponent);
    treeNodeComponent.showPinNodeIcon = jasmine
      .createSpy()
      .and.returnValue(true);
    dom.detectChanges();

    const spy = spyOn(treeNodeComponent.pinNodeChange, 'emit');
    dom.findAndClick('.pin-node-btn');
    expect(spy).toHaveBeenCalledWith(component.node as UiHierarchyTreeNode);
  });

  it('can trigger rect show state toggle on click of icon', () => {
    const treeNodeComponent = assertDefined(component.treeNodeComponent);
    treeNodeComponent.showStateIcon = 'visibility';
    dom.detectChanges();

    const spy = spyOn(treeNodeComponent.rectShowStateChange, 'emit');
    dom.findAndClick('.toggle-rect-show-state-btn');
    expect(spy).toHaveBeenCalled();
  });

  it('does not show copy button for hierarchy tree', () => {
    expect(dom.find('.icon-wrapper-copy')).toBeUndefined();
  });

  it('does not show copy button for property tree node that is not leaf or root', () => {
    component.node = assertDefined(propertiesTree.getChildByName('key2'));
    dom.detectChanges();
    expect(dom.find('.icon-wrapper-copy')).toBeUndefined();
  });

  it('copies node name for root of property tree node', () => {
    component.node = propertiesTree;
    dom.detectChanges();
    dom.findAndClick('.icon-wrapper-copy button');
    expect(mockCopyText).toHaveBeenCalledWith(propertiesTree.name);
  });

  it('copies property name and value for leaf node', () => {
    component.node = assertDefined(propertiesTree.getChildByName('key1'));
    component.isLeaf = true;
    dom.detectChanges();
    dom.findAndClick('.icon-wrapper-copy button');
    expect(mockCopyText).toHaveBeenCalledWith('key1: value1');
  });

  @Component({
    selector: 'host-component',
    template: `
      <tree-node
        [node]="node"
        [isExpanded]="isExpanded"
        [isPinned]="false"
        [isInPinnedSection]="isInPinnedSection"
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
    isInPinnedSection = false;

    @ViewChild(TreeNodeComponent)
    treeNodeComponent: TreeNodeComponent | undefined;
  }
});
