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

import {HttpClientModule} from '@angular/common/http';
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
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
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {UnitTestUtils} from 'test/unit/utils';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {HierarchyComponent} from 'viewers/components/hierarchy_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {ViewerViewCaptureComponent} from './viewer_view_capture_component';

describe('ViewerViewCaptureComponent', () => {
  let fixture: ComponentFixture<ViewerViewCaptureComponent>;
  let component: ViewerViewCaptureComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        MatIconModule,
        MatDividerModule,
        MatTooltipModule,
        MatButtonModule,
        MatInputModule,
        MatFormFieldModule,
        FormsModule,
        BrowserAnimationsModule,
        MatSelectModule,
        HttpClientModule,
      ],
      declarations: [
        ViewerViewCaptureComponent,
        HierarchyComponent,
        PropertiesComponent,
        RectsComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(ViewerViewCaptureComponent);
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
      'SKETCH',
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

  it('handles properties section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.properties-view',
      'PROPERTIES',
    );
  });
});
