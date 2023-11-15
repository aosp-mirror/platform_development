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
import {CommonModule} from '@angular/common';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TreeComponentLegacy} from 'viewers/components/legacy/tree_component';
import {TreeNodeComponentLegacy} from 'viewers/components/legacy/tree_node_component';
import {TreeNodeDataViewComponentLegacy} from 'viewers/components/legacy/tree_node_data_view_component';
import {TreeComponent} from '../tree_component';
import {TreeNodeComponent} from '../tree_node_component';
import {TreeNodeDataViewComponent} from '../tree_node_data_view_component';
import {HierarchyComponentLegacy} from './hierarchy_component';

describe('HierarchyComponentLegacy', () => {
  let fixture: ComponentFixture<HierarchyComponentLegacy>;
  let component: HierarchyComponentLegacy;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [
        HierarchyComponentLegacy,
        TreeComponentLegacy,
        TreeNodeComponentLegacy,
        TreeNodeDataViewComponentLegacy,
        TreeComponent,
        TreeNodeComponent,
        TreeNodeDataViewComponent,
      ],
      imports: [
        CommonModule,
        MatCheckboxModule,
        MatDividerModule,
        MatInputModule,
        MatFormFieldModule,
        BrowserAnimationsModule,
        FormsModule,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(HierarchyComponentLegacy);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.tree = new HierarchyTreeBuilder()
      .setStableId('RootNode1')
      .setName('Root node')
      .setChildren([new HierarchyTreeBuilder().setName('Child node').build()])
      .build();

    component.store = new PersistentStore();
    component.userOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
        isUnavailable: false,
      },
    };

    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders title', () => {
    const title = htmlElement.querySelector('.hierarchy-title');
    expect(title).toBeTruthy();
  });

  it('renders view controls', () => {
    const viewControls = htmlElement.querySelector('.view-controls');
    expect(viewControls).toBeTruthy();
    const box = htmlElement.querySelector('.view-controls input');
    expect(box).toBeTruthy(); //renders at least one view control option
  });

  it('disables checkboxes if option unavailable', () => {
    let box = htmlElement.querySelector('.view-controls input');
    expect(box).toBeTruthy();
    expect((box as HTMLInputElement).disabled).toBeFalse();

    component.userOptions['showDiff'].isUnavailable = true;
    fixture.detectChanges();
    box = htmlElement.querySelector('.view-controls input');
    expect((box as HTMLInputElement).disabled).toBeTrue();
  });

  it('updates tree on user option checkbox change', () => {
    const box = htmlElement.querySelector('.view-controls input');
    expect(box).toBeTruthy();

    const spy = spyOn(component, 'updateTree');
    (box as HTMLInputElement).checked = true;
    (box as HTMLInputElement).dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('renders initial tree elements', () => {
    const treeView = htmlElement.querySelector('tree-view-legacy');
    expect(treeView).toBeTruthy();
    expect(assertDefined(treeView).innerHTML).toContain('Root node');
    expect(assertDefined(treeView).innerHTML).toContain('Child node');
  });

  it('renders pinned nodes', () => {
    const pinnedNodesDiv = htmlElement.querySelector('.pinned-items');
    expect(pinnedNodesDiv).toBeFalsy();

    component.pinnedItems = [assertDefined(component.tree)];
    fixture.detectChanges();
    const pinnedNodeEl = htmlElement.querySelector('.pinned-items tree-node-legacy');
    expect(pinnedNodeEl).toBeTruthy();
  });

  it('handles pinned node click', () => {
    component.pinnedItems = [assertDefined(component.tree)];
    fixture.detectChanges();
    const pinnedNodeEl = htmlElement.querySelector('.pinned-items tree-node-legacy');
    expect(pinnedNodeEl).toBeTruthy();

    const propertyTreeChangeSpy = spyOn(component, 'onSelectedTreeChange');
    const highlightedChangeSpy = spyOn(component, 'onHighlightedItemChange');
    (pinnedNodeEl as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(propertyTreeChangeSpy).toHaveBeenCalled();
    expect(highlightedChangeSpy).toHaveBeenCalled();
  });

  it('handles change in filter', () => {
    const inputEl = htmlElement.querySelector('.title-filter input');
    expect(inputEl).toBeTruthy();

    const spy = spyOn(component, 'filterTree');
    (inputEl as HTMLInputElement).value = 'Root';
    (inputEl as HTMLInputElement).dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
    expect(component.filterString).toBe('Root');
  });
});
