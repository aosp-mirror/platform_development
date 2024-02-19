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
import {Component, ViewChild} from '@angular/core';
import {
  ComponentFixture,
  ComponentFixtureAutoDetect,
  TestBed,
} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatOptionModule} from '@angular/material/core';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {SelectWithFilterComponent} from './select_with_filter_component';

describe('SelectWithFilterComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{provide: ComponentFixtureAutoDetect, useValue: true}],
      declarations: [SelectWithFilterComponent, TestHostComponent],
      imports: [
        CommonModule,
        MatSelectModule,
        MatFormFieldModule,
        MatOptionModule,
        MatInputModule,
        BrowserAnimationsModule,
        FormsModule,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('applies filter correctly', () => {
    const selectComponent = assertDefined(component.selectWithFilterComponent);
    expect(selectComponent.filteredOptions).toEqual(['1', '2', '3']);
    selectComponent.filterString = '2';
    selectComponent.onOptionsFilterChange();
    fixture.detectChanges();
    expect(selectComponent.filteredOptions).toEqual(['2']);
  });

  @Component({
    selector: 'host-component',
    template: ` <select-with-filter [label]="label" [options]="allOptions"> </select-with-filter> `,
  })
  class TestHostComponent {
    label = 'TEST FILTER';
    allOptions = ['1', '2', '3'];

    @ViewChild(SelectWithFilterComponent)
    selectWithFilterComponent: SelectWithFilterComponent | undefined;
  }
});
