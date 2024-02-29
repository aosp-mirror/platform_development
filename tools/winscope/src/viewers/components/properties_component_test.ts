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
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {PersistentStore} from 'common/persistent_store';
import {TreeNodeUtils} from 'test/unit/tree_node_utils';
import {HierarchyTreeNodeDataViewComponent} from './hierarchy_tree_node_data_view_component';
import {PropertiesComponent} from './properties_component';
import {PropertyTreeNodeDataViewComponent} from './property_tree_node_data_view_component';
import {SurfaceFlingerPropertyGroupsComponent} from './surface_flinger_property_groups_component';
import {TreeComponent} from './tree_component';
import {TreeNodeComponent} from './tree_node_component';

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
        HierarchyTreeNodeDataViewComponent,
        PropertyTreeNodeDataViewComponent,
      ],
      imports: [
        CommonModule,
        MatInputModule,
        MatFormFieldModule,
        MatCheckboxModule,
        MatDividerModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        MatIconModule,
        MatTooltipModule,
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

  it('renders tree in proto dump upon selected item', () => {
    component.propertiesTree = TreeNodeUtils.makeUiPropertyNode(
      'selectedItem',
      'property',
      null,
    );
    fixture.detectChanges();
    const treeEl = htmlElement.querySelector('tree-view');
    expect(treeEl).toBeTruthy();
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
