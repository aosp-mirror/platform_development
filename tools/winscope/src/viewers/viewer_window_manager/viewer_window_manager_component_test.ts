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
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from '@angular/core';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {HierarchyComponent} from 'viewers/components/hierarchy_component';
import {PropertiesComponent} from 'viewers/components/properties_component';
import {RectsComponent} from 'viewers/components/rects/rects_component';
import {ViewerWindowManagerComponent} from './viewer_window_manager_component';

describe('ViewerWindowManagerComponent', () => {
  let fixture: ComponentFixture<ViewerWindowManagerComponent>;
  let component: ViewerWindowManagerComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      imports: [MatIconModule, MatDividerModule],
      declarations: [
        ViewerWindowManagerComponent,
        HierarchyComponent,
        PropertiesComponent,
        RectsComponent,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ViewerWindowManagerComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
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
});
