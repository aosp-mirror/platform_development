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
import {TestBed} from '@angular/core/testing';
import {MatTooltipModule} from '@angular/material/tooltip';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';

describe('HierarchyTreeNodeDataViewComponent', () => {
  let testNode: UiHierarchyTreeNode;
  let component: HierarchyTreeNodeDataViewComponent;
  let dom: DOMTestHelper<HierarchyTreeNodeDataViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HierarchyTreeNodeDataViewComponent],
      imports: [MatTooltipModule],
    }).compileComponents();
    const fixture = TestBed.createComponent(HierarchyTreeNodeDataViewComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
    testNode = TreeNodeUtils.makeUiHierarchyNode({
      id: 1,
      name: 'test node',
    });
  });

  it('is robust to no node', () => {
    expect(component).toBeTruthy();
  });

  it('shows node heading if set', () => {
    component.node = testNode;
    dom.detectChanges();
    dom.checkTextExact('1 - test node');
    testNode.setShowHeading(false);
    dom.detectChanges();
    dom.checkTextExact('test node');
  });

  it('shows display name if set, with full name on hover', async () => {
    testNode.setDisplayName('display name');
    component.node = testNode;
    dom.detectChanges();
    dom.checkTextExact('1 - display name');
    await dom.get('.display-name').checkTooltip('test node');
  });

  it('shows chips with tooltip on hover', async () => {
    testNode.addChip(VISIBLE_CHIP);
    component.node = testNode;
    dom.detectChanges();
    dom.checkTextExact(`1 - test node ${VISIBLE_CHIP.short}`);
    await dom.get('.tree-view-chip').checkTooltip(VISIBLE_CHIP.long);
  });
});
