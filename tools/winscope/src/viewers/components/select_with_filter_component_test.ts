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
import {ComponentFixtureAutoDetect, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatOptionModule, MatPseudoCheckboxModule} from '@angular/material/core';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {KeyboardEventCode} from 'common/dom_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {SelectWithFilterComponent} from './select_with_filter_component';

describe('SelectWithFilterComponent', () => {
  const filterInputField = '.select-filter';
  const shiftAndClick = new MouseEvent('click', {shiftKey: true});
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;
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
        MatTooltipModule,
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
    selectChangeSpy = spyOn(
      assertDefined(component.selectWithFilterComponent).selectChange,
      'emit',
    );
  });

  afterAll(() => {
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('applies filter correctly', () => {
    dom.openMatSelect();

    const options = getOptions();
    checkHiddenOptions(options, []);

    const panel = dom.getMatSelectPanel();
    const input = panel.findAndDispatchInput(filterInputField, '2');
    checkHiddenOptions(options, [0, 1]);

    input.dispatchInput('');
    checkHiddenOptions(options, []);
  });

  it('maintains selection even if filtered out', () => {
    dom.openMatSelect();

    const options = getOptions();
    checkHiddenOptions(options, []);

    options[0].click();
    checkSelectValue(['0']);

    const panel = dom.getMatSelectPanel();
    const input = panel.findAndDispatchInput(filterInputField, '2');
    checkHiddenOptions(options, [0, 1]);

    options[2].click();
    checkSelectValue(['0', '2']);

    input.dispatchInput('');
    checkHiddenOptions(options, []);

    options[1].click();
    checkSelectValue(['0', '1', '2']);
  });

  it('applies selection correctly', () => {
    dom.openMatSelect();
    const options = getOptions();

    options[0].click();
    checkSelectValue(['0']);

    options[0].click();
    checkSelectValue([]);
  });

  it('applies deselection from pinned selected options', () => {
    dom.openMatSelect();

    const options = getOptions();
    options[0].click();
    checkSelectValue(['0']);

    const pinnedOptions = getPinnedOptions();
    expect(pinnedOptions.length).toEqual(1);
    pinnedOptions[0].click();
    checkSelectValue([]);
    expect(getPinnedOptions().length).toEqual(0);
  });

  it('resets filter on close', async () => {
    dom.openMatSelect();

    const options = getOptions();
    checkHiddenOptions(options, []);

    dom.getMatSelectPanel().findAndDispatchInput(filterInputField, 'A');
    checkHiddenOptions(options, [0, 1, 2]);

    dom.getInDocument('.cdk-overlay-backdrop').click();
    await dom.whenStable();
    await dom.whenRenderingDone();

    dom.openMatSelect();
    checkHiddenOptions(getOptions(), []);
  });

  it('calls default select keydown handler', async () => {
    dom.openMatSelect();
    await dom.detectChangesAndWaitStable();
    await dom.whenRenderingDone();
    dom.getMatSelectPanel().keydownSpace();
    checkSelectValue(['0']);
  });

  it('calls custom select keydown handler for CTRL+A', async () => {
    dom.openMatSelect();
    await dom.detectChangesAndWaitStable();
    await dom.whenRenderingDone();
    const keydownCtrlA = new KeyboardEvent('keydown', {
      code: KeyboardEventCode.A,
      ctrlKey: true,
    });
    const panel = dom.getMatSelectPanel();

    panel.dispatchEvent(keydownCtrlA);
    checkSelectValue(['0', '1', '2']);

    panel.dispatchEvent(keydownCtrlA);
    checkSelectValue([]);

    panel.dispatchEvent(keydownCtrlA);
    const inputEl = panel.findAndDispatchInput(filterInputField, '2'); // filters out '0' and '1' while all selected

    panel.dispatchEvent(keydownCtrlA);
    checkSelectValue(['0', '1']);

    panel.dispatchEvent(keydownCtrlA);
    checkSelectValue(['0', '1', '2']);

    panel.dispatchEvent(keydownCtrlA);
    inputEl.dispatchInput(''); // removes filter while '0' and '1' selected

    panel.dispatchEvent(keydownCtrlA);
    checkSelectValue(['0', '1', '2']);
  });

  it('does not emit second change after shift + click for adjacent options', () => {
    dom.openMatSelect();
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
    dom.openMatSelect();
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
    dom.openMatSelect();
    const options = getOptions();

    options[2].click();
    options[3].click();
    checkSelectValue(['2', '3']);
    selectChangeSpy.calls.reset();

    options[0].dispatchEvent(shiftAndClick);
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
    checkSelectValue(['0', '2', '3', '1'], ['0', '1', '2', '3']);

    options[2].click();
    options[3].click();
    checkSelectValue(['0', '1']);
    selectChangeSpy.calls.reset();

    options[0].dispatchEvent(shiftAndClick);
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
    checkSelectValue([]);
  });

  it('only toggles non-hidden options between last and current clicks', () => {
    component.allOptions.push('10');
    dom.openMatSelect();
    const options = getOptions();
    dom.getMatSelectPanel().findAndDispatchInput(filterInputField, '1');

    options[1].click();
    selectChangeSpy.calls.reset();
    options[3].dispatchEvent(shiftAndClick);
    checkSelectValue(['1', '10']);
    expect(selectChangeSpy).toHaveBeenCalledTimes(2);
  });

  function getOptions(): Array<DOMTestHelper<TestHostComponent>> {
    return Array.from(dom.getMatSelectPanel().findAll('.option'));
  }

  function checkHiddenOptions(
    options: Array<DOMTestHelper<TestHostComponent>>,
    hidden: number[],
  ) {
    expect(options.length).toEqual(3);
    options.forEach((option, index) => {
      option.checkText(`${index}`);
      option.checkClassName('hidden-option', hidden.includes(index));
    });
  }

  function getPinnedOptions(): Array<DOMTestHelper<TestHostComponent>> {
    return Array.from(
      dom.getMatSelectPanel().findAll('.selected-options .mat-option'),
    ).slice(1);
  }

  function checkSelectValue(expValues: string[], expOpts = expValues) {
    expect(selectChangeSpy).toHaveBeenCalled();
    expect(
      assertDefined(selectChangeSpy.calls.mostRecent().args[0]).value,
    ).toEqual(expValues);
    if (!dom.isMatSelectOpen()) {
      dom.openMatSelect();
    }
    const pinnedOptions = getPinnedOptions();
    expect(pinnedOptions.length).toEqual(expOpts.length);
    pinnedOptions.forEach((option, index) => {
      option.checkTextExact(expOpts[index]);
    });
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
