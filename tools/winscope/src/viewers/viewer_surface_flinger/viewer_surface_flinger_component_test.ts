/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {HttpClientModule} from '@angular/common/http';
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {UnitTestUtils} from 'test/unit/utils';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {HierarchyComponent} from 'viewers/components/hierarchy_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {SurfaceFlingerPropertyGroupsComponent} from 'viewers/components/surface_flinger_property_groups_component';
import {ViewerSurfaceFlingerComponent} from './viewer_surface_flinger_component';

describe('ViewerSurfaceFlingerComponent', () => {
  let fixture: ComponentFixture<ViewerSurfaceFlingerComponent>;
  let component: ViewerSurfaceFlingerComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [
        ViewerSurfaceFlingerComponent,
        HierarchyComponent,
        PropertiesComponent,
        RectsComponent,
        SurfaceFlingerPropertyGroupsComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
      ],
      imports: [
        CommonModule,
        MatIconModule,
        MatDividerModule,
        MatCheckboxModule,
        MatSliderModule,
        MatFormFieldModule,
        MatInputModule,
        BrowserAnimationsModule,
        FormsModule,
        MatTooltipModule,
        MatButtonModule,
        MatSelectModule,
        HttpClientModule,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(ViewerSurfaceFlingerComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates rects view', () => {
    const rectsView = htmlElement.querySelector('.rects-view');
    expect(rectsView).toBeTruthy();
  });

  it('creates hierarchy view', () => {
    const hierarchyView = htmlElement.querySelector('.hierarchy-view');
    expect(hierarchyView).toBeTruthy();
  });

  it('creates property groups view', () => {
    const propertyGroups = htmlElement.querySelector('.property-groups');
    expect(propertyGroups).toBeTruthy();
  });

  it('creates properties view', () => {
    const propertiesView = htmlElement.querySelector('.properties-view');
    expect(propertiesView).toBeTruthy();
  });

  it('creates collapsed sections with no buttons', () => {
    UnitTestUtils.checkNoCollapsedSectionButtons(htmlElement);
  });

  it('handles rects section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.rects-view',
      'LAYERS',
    );
  });

  it('handles hierarchy section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.hierarchy-view',
      'HIERARCHY',
    );
  });

  it('handles property groups section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.property-groups',
      'PROPERTIES',
    );
  });

  it('handles properties section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.properties-view',
      'PROTO DUMP',
    );
  });
});
