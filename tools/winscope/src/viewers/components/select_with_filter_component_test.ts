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
import {MatOptionModule, MatPseudoCheckboxModule} from '@angular/material/core';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {KeyboardEventCode, KeyboardEventKeyCode} from 'common/dom_utils';
import {SelectWithFilterComponent} from './select_with_filter_component';

describe('SelectWithFilterComponent', () => {
  const shiftAndClick = new MouseEvent('click', {shiftKey: true});
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;
  let selectChangeSpy: jasmine.Spy;

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
        MatPseudoCheckboxModule,
        MatDividerModule,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
    selectChangeSpy = spyOn(
      assertDefined(component.selectWithFilterComponent).selectChange,
      'emit',
    );
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
    openSelectPanel();

    const options = getOptions();
    checkHiddenOptions(options, []);

    options[0].click();
    fixture.detectChanges();
    checkSelectValue(['0']);

    const inputEl = getFilterInput();

    dispatchInput(inputEl, '2');
    checkHiddenOptions(options, [0, 1]);

    options[2].click();
    checkSelectValue(['0', '2']);

    dispatchInput(inputEl, '');
    checkHiddenOptions(options, []);

    options[1].click();
    checkSelectValue(['0', '1', '2']);
  });

  it('applies selection correctly', () => {
    openSelectPanel();
    const options = getOptions();

    options[0].click();
    checkSelectValue(['0']);

    options[0].click();
    checkSelectValue([]);
  });

  it('applies deselection from pinned selected options', () => {
    openSelectPanel();

    const options = getOptions();
    options[0].click();
    fixture.detectChanges();
    checkSelectValue(['0']);

    const pinnedOptions = getPinnedOptions();
    expect(pinnedOptions.length).toEqual(1);
    pinnedOptions[0].click();
    fixture.detectChanges();
    checkSelectValue([]);
    expect(getPinnedOptions().length).toEqual(0);
  });

  it('resets filter on close', async () => {
    openSelectPanel();

    const options = getOptions();
    checkHiddenOptions(options, []);

    const inputEl = getFilterInput();
    dispatchInput(inputEl, 'A');
    checkHiddenOptions(options, [0, 1, 2]);

    document.querySelector<HTMLElement>('.cdk-overlay-backdrop')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    openSelectPanel();
    checkHiddenOptions(getOptions(), []);
  });

  it('calls default select keydown handler', async () => {
    openSelectPanel();
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.whenRenderingDone();
    const panel = assertDefined(document.querySelector('.mat-select-panel'));
    panel.dispatchEvent(
      new KeyboardEvent('keydown', {keyCode: KeyboardEventKeyCode.SPACE}),
    );
    fixture.detectChanges();
    checkSelectValue(['0']);
  });

  it('calls custom select keydown handler for CTRL+A', async () => {
    openSelectPanel();
    fixture.detectChanges();
    await fixture.whenStable();
    await fixture.whenRenderingDone();
    const keydownCtrlA = new KeyboardEvent('keydown', {
      code: KeyboardEventCode.A,
      ctrlKey: true,
    });
    const panel = assertDefined(document.querySelector('.mat-select-panel'));

    panel.dispatchEvent(keydownCtrlA);
    fixture.detectChanges();
    checkSelectValue(['0', '1', '2']);

    panel.dispatchEvent(keydownCtrlA);
    fixture.detectChanges();
    checkSelectValue([]);

    panel.dispatchEvent(keydownCtrlA);
    fixture.detectChanges();

    const inputEl = getFilterInput(); // filters out '0' and '1' while all selected
    dispatchInput(inputEl, '2');

    panel.dispatchEvent(keydownCtrlA);
    fixture.detectChanges();
    checkSelectValue(['0', '1']);

    panel.dispatchEvent(keydownCtrlA);
    fixture.detectChanges();
    checkSelectValue(['0', '1', '2']);

    panel.dispatchEvent(keydownCtrlA);
    fixture.detectChanges();
    dispatchInput(inputEl, ''); // removes filter while '0' and '1' selected

    panel.dispatchEvent(keydownCtrlA);
    fixture.detectChanges();
    checkSelectValue(['0', '1', '2']);
  });

  it('does not emit second change after shift + click for adjacent options', () => {
    openSelectPanel();
    const options = getOptions();

    options[0].dispatchEvent(shiftAndClick);
    expect(selectChangeSpy).toHaveBeenCalledTimes(1);

    options[1].dispatchEvent(shiftAndClick);
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);

    options[0].dispatchEvent(shiftAndClick);
    expect(selectChangeSpy).toHaveBeenCalledTimes(3);

    options[1].click();
    expect(selectChangeSpy).toHaveBeenCalledTimes(4);
  });

  it('emits second change after shift + click to toggle options in-between', () => {
    openSelectPanel();
    const options = getOptions();

    options[0].click();
    selectChangeSpy.calls.reset();

    options[2].dispatchEvent(shiftAndClick);
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
    checkSelectValue(['0', '2', '1'], ['0', '1', '2']);
    selectChangeSpy.calls.reset();

    options[0].dispatchEvent(shiftAndClick);
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
    checkSelectValue([]);
  });

  it('sets in-between options to value of clicked option, regardless of current state', () => {
    component.allOptions.push('3');
    openSelectPanel();
    const options = getOptions();

    options[2].click();
    options[3].click();
    fixture.detectChanges();
    checkSelectValue(['2', '3']);
    selectChangeSpy.calls.reset();

    options[0].dispatchEvent(shiftAndClick);
    fixture.detectChanges();
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
    checkSelectValue(['0', '2', '3', '1'], ['0', '1', '2', '3']);

    options[2].click();
    options[3].click();
    fixture.detectChanges();
    checkSelectValue(['0', '1']);
    selectChangeSpy.calls.reset();

    options[0].dispatchEvent(shiftAndClick);
    fixture.detectChanges();
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
    checkSelectValue([]);
  });

  it('only toggles non-hidden options between last and current clicks', () => {
    component.allOptions.push('10');
    openSelectPanel();
    const options = getOptions();

    const inputEl = getFilterInput();
    dispatchInput(inputEl, '1');

    options[1].click();
    fixture.detectChanges();
    selectChangeSpy.calls.reset();
    options[3].dispatchEvent(shiftAndClick);
    fixture.detectChanges();
    checkSelectValue(['1', '10']);
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
  });

  function openSelectPanel() {
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.mat-select-trigger'),
    ).click();
  }

  function getOptions(): HTMLElement[] {
    return Array.from(
      document.querySelectorAll<HTMLElement>('.mat-select-panel .option'),
    );
  }

  function checkHiddenOptions(options: HTMLElement[], hidden: number[]) {
    expect(options.length).toEqual(3);
    options.forEach((option, index) => {
      expect(option.textContent).toContain(`${index}`);
      if (hidden.includes(index)) {
        expect(option.className).toContain('hidden-option');
      } else {
        expect(option.className).not.toContain('hidden-option');
      }
    });
  }

  function getFilterInput(): HTMLInputElement {
    return assertDefined(
      document.querySelector<HTMLInputElement>(
        '.mat-select-panel .select-filter input',
      ),
    );
  }

  function dispatchInput(inputEl: HTMLInputElement, input: string) {
    inputEl.value = input;
    inputEl.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function getPinnedOptions(): HTMLElement[] {
    return Array.from(
      document.querySelectorAll<HTMLElement>(
        '.mat-select-panel .selected-options .mat-option',
      ),
    ).slice(1);
  }

  function checkSelectValue(expValues: string[], expOpts = expValues) {
    expect(selectChangeSpy).toHaveBeenCalled();
    expect(
      assertDefined(selectChangeSpy.calls.mostRecent().args[0]).value,
    ).toEqual(expValues);
    if (!document.querySelector('.mat-select-panel')) {
      openSelectPanel();
    }
    expect(
      Array.from(getPinnedOptions()).map((o) => o.textContent?.trim()),
    ).toEqual(expOpts);
  }

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
});
