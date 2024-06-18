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
import {CommonModule} from '@angular/common';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {HierarchyTreeNodeDataViewComponent} from 'viewers/components/hierarchy_tree_node_data_view_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {HierarchyComponent} from './hierarchy_component';

describe('HierarchyComponent', () => {
  let fixture: ComponentFixture<HierarchyComponent>;
  let component: HierarchyComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [
        HierarchyComponent,
        TreeComponent,
        TreeNodeComponent,
        HierarchyTreeNodeDataViewComponent,
      ],
      imports: [
        CommonModule,
        MatCheckboxModule,
        MatDividerModule,
        MatInputModule,
        MatFormFieldModule,
        BrowserAnimationsModule,
        FormsModule,
        MatIconModule,
        MatTooltipModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HierarchyComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    const tree = TreeNodeUtils.makeUiHierarchyNode({
      id: 'RootNode1',
      name: 'Root node',
    });
    tree.addOrReplaceChild(
      TreeNodeUtils.makeUiHierarchyNode({id: 'Child1', name: 'Child node'}),
    );
    component.tree = tree;

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

    const spy = spyOn(component, 'onUserOptionChange');
    (box as HTMLInputElement).checked = true;
    (box as HTMLInputElement).dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('renders initial tree elements', () => {
    const treeView = htmlElement.querySelector('tree-view');
    expect(treeView).toBeTruthy();
    expect(assertDefined(treeView).innerHTML).toContain('Root node');
    expect(assertDefined(treeView).innerHTML).toContain('Child node');
  });

  it('renders pinned nodes', () => {
    const pinnedNodesDiv = htmlElement.querySelector('.pinned-items');
    expect(pinnedNodesDiv).toBeFalsy();

    component.pinnedItems = [assertDefined(component.tree)];
    fixture.detectChanges();
    const pinnedNodeEl = htmlElement.querySelector('.pinned-items tree-node');
    expect(pinnedNodeEl).toBeTruthy();
  });

  it('handles pinned node click', () => {
    component.pinnedItems = [assertDefined(component.tree)];
    fixture.detectChanges();
    const pinnedNodeEl = htmlElement.querySelector('.pinned-items tree-node');
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

    const spy = spyOn(component, 'onFilterChange');
    (inputEl as HTMLInputElement).value = 'Root';
    (inputEl as HTMLInputElement).dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
    expect(component.filterString).toBe('Root');
  });
});
