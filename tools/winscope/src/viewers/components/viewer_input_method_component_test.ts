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

import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';
import {ImeUiData} from 'viewers/common/ime_ui_data';
import {CollapsedSectionsComponent} from './collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from './collapsible_section_title_component';
import {HierarchyComponent} from './hierarchy_component';
import {ImeAdditionalPropertiesComponent} from './ime_additional_properties_component';
import {PropertiesComponent} from './properties_component';
import {ViewerInputMethodComponent} from './viewer_input_method_component';

describe('ViewerInputMethodComponent', () => {
  let fixture: ComponentFixture<ViewerInputMethodComponent>;
  let component: ViewerInputMethodComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [
        MatIconModule,
        MatDividerModule,
        MatButtonModule,
        MatTooltipModule,
        FormsModule,
      ],
      declarations: [
        ViewerInputMethodComponent,
        HierarchyComponent,
        PropertiesComponent,
        ImeAdditionalPropertiesComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(ViewerInputMethodComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.inputData = new ImeUiData(TraceType.INPUT_METHOD_CLIENTS);
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates hierarchy view', () => {
    const hierarchyView = htmlElement.querySelector('.hierarchy-view');
    expect(hierarchyView).toBeTruthy();
  });

  it('creates additional properties view', () => {
    const additionalProperties = htmlElement.querySelector(
      '.ime-additional-properties',
    );
    expect(additionalProperties).toBeTruthy();
  });

  it('creates properties view', () => {
    const propertiesView = htmlElement.querySelector('.properties-view');
    expect(propertiesView).toBeTruthy();
  });

  it('creates collapsed sections with no buttons', () => {
    UnitTestUtils.checkNoCollapsedSectionButtons(htmlElement);
  });

  it('handles hierarchy section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.hierarchy-view',
      'HIERARCHY',
    );
  });

  it('handles ime additional properties section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.ime-additional-properties',
      'WM & SF PROPERTIES',
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
