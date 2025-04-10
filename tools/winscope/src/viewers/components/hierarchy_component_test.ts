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
import {ClipboardModule} from '@angular/cdk/clipboard';
import {CommonModule} from '@angular/common';
import {ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {FilterFlag} from 'common/filter_flag';
import {InMemoryStorage} from 'common/store/in_memory_storage';
import {PersistentStore} from 'common/store/persistent_store';
import {DuplicateLayerIds, MissingLayerIds} from 'messaging/user_warnings';
import {checkTooltips, DOMTestHelper} from 'test/unit/dom_test_utils';
import {HierarchyTreeBuilder} from 'test/unit/hierarchy_tree_builder';
import {TraceType} from 'trace/trace_type';
import {TextFilter} from 'viewers/common/text_filter';
import {UiHierarchyTreeNode} from 'viewers/common/ui_hierarchy_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {HierarchyTreeNodeDataViewComponent} from 'viewers/components/hierarchy_tree_node_data_view_component';
import {TreeComponent} from 'viewers/components/tree_component';
import {TreeNodeComponent} from 'viewers/components/tree_node_component';
import {CollapsibleSectionTitleComponent} from './collapsible_section_title_component';
import {HierarchyComponent} from './hierarchy_component';
import {SearchBoxComponent} from './search_box_component';
import {UserOptionsComponent} from './user_options_component';

describe('HierarchyComponent', () => {
  let component: HierarchyComponent;
  let dom: DOMTestHelper<HierarchyComponent>;

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
        SearchBoxComponent,
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
        ClipboardModule,
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(HierarchyComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);

    component.trees = [
      UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder()
          .setId('RootNode1')
          .setName('Root node')
          .setChildren([{id: 'Child1', name: 'Child node'}])
          .build(),
      ),
    ];

    component.store = new PersistentStore();
    component.userOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
        isUnavailable: false,
      },
    };
    component.textFilter = new TextFilter();
    component.dependencies = [TraceType.SURFACE_FLINGER];

    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('renders title', () => {
    expect(dom.find('.hierarchy-title')).toBeDefined();
  });

  it('renders view controls', () => {
    expect(dom.find('.view-controls')).toBeDefined();
    expect(dom.find('.view-controls .user-option')).toBeDefined(); //renders at least one view control option
  });

  it('renders initial tree elements', () => {
    const treeView = dom.get('tree-view');
    treeView.checkText('Root node');
    treeView.checkText('Child node');
  });

  it('renders multiple trees', () => {
    component.trees = [
      component.trees[0],
      UiHierarchyTreeNode.from(
        new HierarchyTreeBuilder().setId('subtree').setName('subtree').build(),
      ),
    ];
    dom.detectChanges();
    const trees = dom.findAll('.tree-wrapper .tree');
    expect(trees.length).toEqual(2);
    trees[1].checkText('subtree');
  });

  it('renders pinned nodes', () => {
    expect(dom.find('.pinned-items')).toBeUndefined();
    component.pinnedItems = assertDefined(component.trees);
    dom.detectChanges();
    expect(dom.find('.pinned-items tree-node')).toBeDefined();
  });

  it('renders placeholder text', () => {
    component.trees = [];
    component.placeholderText = 'Placeholder text.';
    dom.detectChanges();
    dom
      .get('.placeholder-text')
      .checkTextExact('Placeholder text. Try changing timeline position.');
  });

  it('handles pinned node click', () => {
    const node = assertDefined(component.trees[0]);
    component.pinnedItems = [node];
    dom.detectChanges();

    let highlightedItem: UiHierarchyTreeNode | undefined;
    dom.addEventListener(ViewerEvents.HighlightedNodeChange, (event) => {
      highlightedItem = (event as CustomEvent).detail.node;
    });

    dom.findAndClick('.pinned-items tree-node');
    expect(highlightedItem).toEqual(node);
  });

  it('handles pinned item change from tree', () => {
    let pinnedItem: UiHierarchyTreeNode | undefined;
    dom.addEventListener(ViewerEvents.HierarchyPinnedChange, (event) => {
      pinnedItem = (event as CustomEvent).detail.pinnedItem;
    });
    const child = assertDefined(
      component.trees[0].getChildByName('Child node'),
    );
    component.pinnedItems = [child];
    dom.detectChanges();

    dom.findAndClick('.pinned-items tree-node .pin-node-btn');
    expect(pinnedItem).toEqual(child);
  });

  it('handles change in filter', () => {
    let textFilter: TextFilter | undefined;
    dom.addEventListener(ViewerEvents.HierarchyFilterChange, (event) => {
      textFilter = (event as CustomEvent).detail;
    });
    dom.findAndClick('.search-box button');
    dom.findAndDispatchInput('.title-section', 'Root');
    expect(textFilter).toEqual(new TextFilter('Root', [FilterFlag.MATCH_CASE]));
  });

  it('handles collapse button click', () => {
    const spy = spyOn(component.collapseButtonClicked, 'emit');
    dom.findAndClick('collapsible-section-title button');
    expect(spy).toHaveBeenCalled();
  });

  it('shows warnings from all trees', () => {
    expect(dom.find('.warning')).toBeUndefined();

    component.trees = [
      component.trees[0],
      UiHierarchyTreeNode.from(component.trees[0]),
    ];
    dom.detectChanges();
    const warning1 = new DuplicateLayerIds([123]);
    component.trees[0].addWarning(warning1);
    const warning2 = new MissingLayerIds();
    component.trees[1].addWarning(warning2);
    dom.detectChanges();
    const warnings = dom.findAll('.warning');
    expect(warnings.length).toEqual(2);
    warnings[0].checkTextExact('warning ' + warning1.getMessage());
    warnings[1].checkTextExact('warning ' + warning2.getMessage());
  });

  it('shows warning tooltip if text overflowing', () => {
    const warning = new DuplicateLayerIds([123]);
    component.trees[0].addWarning(warning);
    dom.detectChanges();

    const warningEl = dom.get('.warning');
    const msgEl = dom.get('.warning-message').getHTMLElement();

    const spy = spyOnProperty(msgEl, 'scrollWidth').and.returnValue(
      msgEl.clientWidth,
    );
    checkTooltips([warningEl], [undefined]);

    spy.and.returnValue(msgEl.clientWidth + 1);
    dom.detectChanges();
    checkTooltips([warningEl], [warning.getMessage()]);
  });

  it('handles arrow down key press', () => {
    testArrowKeyPress(ViewerEvents.ArrowDownPress);
  });

  it('handles arrow up key press', () => {
    testArrowKeyPress(ViewerEvents.ArrowUpPress);
  });

  function testArrowKeyPress(viewerEvent: string) {
    let storage: InMemoryStorage | undefined;
    dom.addEventListener(viewerEvent, (event) => {
      storage = (event as CustomEvent).detail;
    });
    let keydown: () => void;
    if (viewerEvent === ViewerEvents.ArrowDownPress) {
      keydown = () => dom.keydownArrowDown(true);
    } else {
      keydown = () => dom.keydownArrowUp(true);
    }
    keydown();
    expect(storage).toEqual(component.treeStorage);

    storage = undefined;
    dom.getHTMLElement().style.height = '0px';
    dom.detectChanges();
    keydown();
    expect(storage).toBeUndefined();
  }
});
