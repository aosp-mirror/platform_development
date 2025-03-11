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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatTooltipModule} from '@angular/material/tooltip';
import {assertDefined} from 'common/assert_utils';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {VISIBLE_CHIP} from 'viewers/common/chip';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';

describe('HierarchyTreeNodeDataViewComponent', () => {
  let testNode: UiHierarchyTreeNode;
  let fixture: ComponentFixture<HierarchyTreeNodeDataViewComponent>;
  let component: HierarchyTreeNodeDataViewComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HierarchyTreeNodeDataViewComponent],
      imports: [MatTooltipModule],
    }).compileComponents();
    fixture = TestBed.createComponent(HierarchyTreeNodeDataViewComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();

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
    fixture.detectChanges();
    expect(htmlElement.textContent).toEqual('1 - test node');
    testNode.setShowHeading(false);
    fixture.detectChanges();
    expect(htmlElement.textContent).toEqual('test node');
  });

  it('shows display name if set, with full name on hover', () => {
    testNode.setDisplayName('display name');
    component.node = testNode;
    fixture.detectChanges();
    expect(htmlElement.textContent).toEqual('1 - display name');
    const displayName = assertDefined(
      htmlElement.querySelector<HTMLElement>('.display-name'),
    );
    checkTooltip(displayName, 'test node');
  });

  it('shows chips with tooltip on hover', () => {
    testNode.addChip(VISIBLE_CHIP);
    component.node = testNode;
    fixture.detectChanges();
    expect(htmlElement.textContent?.trim()).toEqual(
      `1 - test node ${VISIBLE_CHIP.short}`,
    );
    const chip = assertDefined(
      htmlElement.querySelector<HTMLElement>('.tree-view-chip'),
    );
    checkTooltip(chip, VISIBLE_CHIP.long);
  });

  function checkTooltip(triggerElement: HTMLElement, expectedText: string) {
    triggerElement.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    const tooltipPanel = assertDefined(
      document.querySelector<HTMLElement>('.mat-tooltip-panel'),
    );
    expect(tooltipPanel.textContent?.trim()).toEqual(expectedText);
  }
});
