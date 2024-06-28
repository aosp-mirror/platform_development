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
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {PersistentStore} from 'common/persistent_store';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceType} from 'trace/trace_type';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {HierarchyTreeNodeDataViewComponent} from 'viewers/components/hierarchy_tree_node_data_view_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {CollapsibleSectionTitleComponent} from './collapsible_section_title_component';
import {HierarchyComponent} from './hierarchy_component';
import {UserOptionsComponent} from './user_options_component';

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
        CollapsibleSectionTitleComponent,
        UserOptionsComponent,
      ],
      imports: [
        CommonModule,
        MatButtonModule,
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

    component.tree = UiHierarchyTreeNode.from(
      new HierarchyTreeBuilder()
        .setId('RootNode1')
        .setName('Root node')
        .setChildren([{id: 'Child1', name: 'Child node'}])
        .build(),
    );

    component.store = new PersistentStore();
    component.userOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
        isUnavailable: false,
      },
    };
    component.dependencies = [TraceType.SURFACE_FLINGER];

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
    const button = htmlElement.querySelector('.view-controls .user-option');
    expect(button).toBeTruthy(); //renders at least one view control option
  });

  it('renders initial tree elements', () => {
    const treeView = htmlElement.querySelector('tree-view');
    expect(treeView).toBeTruthy();
    expect(assertDefined(treeView).innerHTML).toContain('Root node');
    expect(assertDefined(treeView).innerHTML).toContain('Child node');
  });

  it('renders subtrees', () => {
    component.subtrees = [
      UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('subtree').setName('subtree').build(),
      ),
    ];
    fixture.detectChanges();
    const subtree = assertDefined(
      htmlElement.querySelector('.tree-wrapper .subtrees tree-view'),
    );
    expect(assertDefined(subtree).innerHTML).toContain('subtree');
  });

  it('renders pinned nodes', () => {
    const pinnedNodesDiv = htmlElement.querySelector('.pinned-items');
    expect(pinnedNodesDiv).toBeFalsy();

    component.pinnedItems = [assertDefined(component.tree)];
    fixture.detectChanges();
    const pinnedNodeEl = htmlElement.querySelector('.pinned-items tree-node');
    expect(pinnedNodeEl).toBeTruthy();
  });

  it('renders placeholder text', () => {
    component.tree = undefined;
    component.placeholderText = 'Placeholder text';
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.placeholder-text')?.textContent,
    ).toContain('Placeholder text');
  });

  it('handles pinned node click', () => {
    const node = assertDefined(component.tree);
    component.pinnedItems = [node];
    fixture.detectChanges();

    let highlightedItem: UiHierarchyTreeNode | undefined;
    htmlElement.addEventListener(
      ViewerEvents.HighlightedNodeChange,
      (event) => {
        highlightedItem = (event as CustomEvent).detail.node;
      },
    );

    const pinnedNodeEl = assertDefined(
      htmlElement.querySelector('.pinned-items tree-node'),
    );

    (pinnedNodeEl as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(highlightedItem).toEqual(node);
  });

  it('handles pinned item change from tree', () => {
    let pinnedItem: UiHierarchyTreeNode | undefined;
    htmlElement.addEventListener(
      ViewerEvents.HierarchyPinnedChange,
      (event) => {
        pinnedItem = (event as CustomEvent).detail.pinnedItem;
      },
    );
    const child = assertDefined(component.tree?.getChildByName('Child node'));
    component.pinnedItems = [child];
    fixture.detectChanges();

    const pinButton = assertDefined(
      htmlElement.querySelector('.pinned-items tree-node .pin-node-btn'),
    );
    (pinButton as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(pinnedItem).toEqual(child);
  });

  it('handles change in filter', () => {
    let filterString: string | undefined;
    htmlElement.addEventListener(
      ViewerEvents.HierarchyFilterChange,
      (event) => {
        filterString = (event as CustomEvent).detail.filterString;
      },
    );
    const inputEl = assertDefined(
      htmlElement.querySelector('.title-section input'),
    ) as HTMLInputElement;

    inputEl.value = 'Root';
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(filterString).toBe('Root');
  });

  it('handles collapse button click', () => {
    const spy = spyOn(component.collapseButtonClicked, 'emit');
    const collapseButton = assertDefined(
      htmlElement.querySelector('collapsible-section-title button'),
    ) as HTMLButtonElement;
    collapseButton.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });
});
