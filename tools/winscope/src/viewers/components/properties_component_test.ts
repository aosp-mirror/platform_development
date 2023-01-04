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
import {CommonModule} from '@angular/common';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {ComponentFixture, ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {PropertiesComponent} from './properties_component';
import {PropertyGroupsComponent} from './property_groups_component';
import {TreeComponent} from './tree_component';

describe('PropertiesComponent', () => {
  let fixture: ComponentFixture<PropertiesComponent>;
  let component: PropertiesComponent;
  let htmlElement: HTMLElement;

  beforeAll(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [PropertiesComponent, PropertyGroupsComponent, TreeComponent],
      imports: [
        CommonModule,
        MatInputModule,
        MatFormFieldModule,
        MatCheckboxModule,
        MatDividerModule,
        BrowserAnimationsModule,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PropertiesComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.propertiesTree = {};
    component.selectedFlickerItem = null;
    component.userOptions = {
      showDefaults: {
        name: 'Show defaults',
        enabled: false,
      },
    };
  });

  it('can be created', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('creates title', () => {
    fixture.detectChanges();
    const title = htmlElement.querySelector('.properties-title');
    expect(title).toBeTruthy();
  });

  it('creates view controls', () => {
    fixture.detectChanges();
    const viewControls = htmlElement.querySelector('.view-controls');
    expect(viewControls).toBeTruthy();
  });

  it('creates initial tree elements', () => {
    fixture.detectChanges();
    const tree = htmlElement.querySelector('.tree-wrapper');
    expect(tree).toBeTruthy();
  });
});
