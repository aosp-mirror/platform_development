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
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {FilterFlag} from 'common/filter_flag';
import {PersistentStore} from 'common/persistent_store';
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
  let fixture: ComponentFixture<PropertiesComponent>;
  let component: PropertiesComponent;
  let htmlElement: HTMLElement;

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

    fixture = TestBed.createComponent(PropertiesComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.store = new PersistentStore();
    component.userOptions = {
      showDiff: {
        name: 'Show diff',
        enabled: false,
        isUnavailable: false,
      },
    };
    component.textFilter = new TextFilter('', []);
    component.traceType = TraceType.SURFACE_FLINGER;

    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates title', () => {
    const title = htmlElement.querySelector('.properties-title');
    expect(title).toBeTruthy();
  });

  it('renders view controls', () => {
    const viewControls = htmlElement.querySelector('.view-controls');
    expect(viewControls).toBeTruthy();
    const box = htmlElement.querySelector('.view-controls .user-option');
    expect(box).toBeTruthy(); //renders at least one view control option
  });

  it('renders tree in proto dump upon selected item', () => {
    const tree = new PropertyTreeBuilder()
      .setRootId('selectedItem')
      .setName('property')
      .setValue(null)
      .build();
    tree.setIsRoot(true);
    component.propertiesTree = UiPropertyTreeNode.from(tree);
    fixture.detectChanges();
    const treeEl = htmlElement.querySelector('tree-view');
    expect(treeEl).toBeTruthy();
  });

  it('renders placeholder text', () => {
    component.propertiesTree = undefined;
    component.placeholderText = 'Placeholder text';
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.placeholder-text')?.textContent,
    ).toContain('Placeholder text');
  });

  it('handles node click', () => {
    const tree = new PropertyTreeBuilder()
      .setRootId('selectedItem')
      .setName('property')
      .setValue(null)
      .build();
    tree.setIsRoot(true);
    component.propertiesTree = UiPropertyTreeNode.from(tree);
    fixture.detectChanges();

    let highlightedItem: string | undefined;
    htmlElement.addEventListener(
      ViewerEvents.HighlightedPropertyChange,
      (event) => {
        highlightedItem = (event as CustomEvent).detail.id;
      },
    );

    const node = assertDefined(
      htmlElement.querySelector('tree-node'),
    ) as HTMLElement;
    node.click();
    fixture.detectChanges();
    expect(highlightedItem).toEqual(tree.id);
  });

  it('handles change in filter', () => {
    let textFilter: TextFilter | undefined;
    htmlElement.addEventListener(
      ViewerEvents.PropertiesFilterChange,
      (event) => {
        textFilter = (event as CustomEvent).detail;
      },
    );
    const inputEl = assertDefined(
      htmlElement.querySelector<HTMLInputElement>('.title-section input'),
    );
    const flagButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.search-box button'),
    );
    flagButton.click();
    fixture.detectChanges();

    inputEl.value = 'Root';
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(textFilter).toEqual(new TextFilter('Root', [FilterFlag.MATCH_CASE]));
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
