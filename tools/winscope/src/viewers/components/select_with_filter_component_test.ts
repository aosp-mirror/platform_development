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
    openSelectPanel();

    const options = getOptions();
    checkHiddenOptions(options, []);

    const inputEl = getFilterInput();
    dispatchInput(inputEl, '2');
    checkHiddenOptions(options, [0, 1]);

    dispatchInput(inputEl, '');
    checkHiddenOptions(options, []);
  });

  it('maintains selection even if filtered out', () => {
    const spy = spyOn(
      assertDefined(component.selectWithFilterComponent).selectChange,
      'emit',
    );
    openSelectPanel();

    const options = getOptions();
    checkHiddenOptions(options, []);

    (options.item(0) as HTMLElement).click();
    fixture.detectChanges();
    checkSelectValue(spy, ['0']);

    const inputEl = getFilterInput();

    dispatchInput(inputEl, '2');
    checkHiddenOptions(options, [0, 1]);

    (options.item(2) as HTMLElement).click();
    checkSelectValue(spy, ['0', '2']);

    dispatchInput(inputEl, '');
    checkHiddenOptions(options, []);

    (options.item(1) as HTMLElement).click();
    checkSelectValue(spy, ['0', '1', '2']);
  });

  it('applies selection correctly', () => {
    const spy = spyOn(
      assertDefined(component.selectWithFilterComponent).selectChange,
      'emit',
    );
    openSelectPanel();

    const options = getOptions();

    (options.item(0) as HTMLElement).click();
    checkSelectValue(spy, ['0']);

    (options.item(0) as HTMLElement).click();
    checkSelectValue(spy, []);
  });

  it('resets filter on close', async () => {
    openSelectPanel();

    const options = getOptions();
    checkHiddenOptions(options, []);

    const inputEl = getFilterInput();
    dispatchInput(inputEl, 'A');
    checkHiddenOptions(options, [0, 1, 2]);

    (document.querySelector('.cdk-overlay-backdrop') as HTMLElement).click();
    fixture.detectChanges();
    await fixture.whenStable();

    openSelectPanel();
    checkHiddenOptions(getOptions(), []);
  });

  @Component({
    selector: 'host-component',
    template: `
      <select-with-filter
        [label]="label"
        [options]="allOptions"></select-with-filter>
    `,
  })
  class TestHostComponent {
    label = 'TEST FILTER';
    allOptions = ['0', '1', '2'];

    @ViewChild(SelectWithFilterComponent)
    selectWithFilterComponent: SelectWithFilterComponent | undefined;
  }

  function openSelectPanel() {
    const trigger = assertDefined(
      htmlElement.querySelector('.mat-select-trigger'),
    ) as HTMLElement;
    trigger.click();
  }

  function getOptions(): NodeList {
    return document.querySelectorAll('.mat-select-panel .mat-option');
  }

  function checkHiddenOptions(options: NodeList, hidden: number[]) {
    expect(options.length).toEqual(3);
    options.forEach((option, index) => {
      expect(option.textContent).toContain(`${index}`);
      if (hidden.includes(index)) {
        expect((option as HTMLElement).className).toContain('hidden-option');
      } else {
        expect((option as HTMLElement).className).not.toContain(
          'hidden-option',
        );
      }
    });
  }

  function getFilterInput(): HTMLInputElement {
    return assertDefined(
      document.querySelector('.mat-select-panel .select-filter input'),
    ) as HTMLInputElement;
  }

  function dispatchInput(inputEl: HTMLInputElement, input: string) {
    inputEl.value = input;
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function checkSelectValue(spy: jasmine.Spy, expected: string[]) {
    expect(spy).toHaveBeenCalled();
    expect(assertDefined(spy.calls.mostRecent().args[0]).value).toEqual(
      expected,
    );
  }
});
