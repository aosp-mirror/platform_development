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
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {FilterFlag} from 'common/filter_flag';
import {PersistentStore} from 'common/store/persistent_store';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {TraceType} from 'trace/trace_type';
import {TextFilter} from 'viewers/common/text_filter';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {CollapsibleSectionTitleComponent} from './collapsible_section_title_component';
import {PropertiesComponent} from './properties_component';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';
import {SearchBoxComponent} from './search_box_component';
import {SurfaceFlingerPropertyGroupsComponent} from './surface_flinger_property_groups_component';
import {TreeComponent} from './tree_component';
import {TreeNodeComponent} from './tree_node_component';
import {UserOptionsComponent} from './user_options_component';

describe('PropertiesComponent', () => {
  let component: PropertiesComponent;
  let dom: DOMTestHelper<PropertiesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [
        PropertiesComponent,
        SurfaceFlingerPropertyGroupsComponent,
        TreeComponent,
        TreeNodeComponent,
        PropertyTreeNodeDataViewComponent,
        CollapsibleSectionTitleComponent,
        UserOptionsComponent,
        SearchBoxComponent,
      ],
      imports: [
        CommonModule,
        MatInputModule,
        MatFormFieldModule,
        MatButtonModule,
        MatDividerModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        MatIconModule,
        MatTooltipModule,
        ClipboardModule,
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(PropertiesComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);

    component.store = new PersistentStore();
    component.userOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
        isUnavailable: false,
      },
    };
    component.textFilter = new TextFilter();
    component.traceType = TraceType.SURFACE_FLINGER;

    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates title', () => {
    expect(dom.find('.properties-title')).toBeDefined();
  });

  it('renders view controls', () => {
    expect(dom.find('.view-controls')).toBeDefined();
    expect(dom.find('.view-controls .user-option')).toBeDefined(); //renders at least one view control option
  });

  it('renders tree in proto dump upon selected item', () => {
    const tree = new PropertyTreeBuilder()
      .setRootId('selectedItem')
      .setName('property')
      .setValue(null)
      .build();
    tree.setIsRoot(true);
    component.propertiesTree = UiPropertyTreeNode.from(tree);
    dom.detectChanges();
    expect(dom.find('tree-view')).toBeDefined();
  });

  it('renders placeholder text', () => {
    component.propertiesTree = undefined;
    component.placeholderText = 'Placeholder text';
    dom.detectChanges();
    dom.get('.placeholder-text').checkTextExact('Placeholder text');
  });

  it('handles node click', () => {
    const tree = new PropertyTreeBuilder()
      .setRootId('selectedItem')
      .setName('property')
      .setValue(null)
      .build();
    tree.setIsRoot(true);
    component.propertiesTree = UiPropertyTreeNode.from(tree);
    dom.detectChanges();

    let highlightedItem: string | undefined;
    dom.addEventListener(ViewerEvents.HighlightedPropertyChange, (event) => {
      highlightedItem = (event as CustomEvent).detail.id;
    });

    dom.findAndClick('tree-node');
    expect(highlightedItem).toEqual(tree.id);
  });

  it('handles change in filter', () => {
    let textFilter: TextFilter | undefined;
    dom.addEventListener(ViewerEvents.PropertiesFilterChange, (event) => {
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
});
