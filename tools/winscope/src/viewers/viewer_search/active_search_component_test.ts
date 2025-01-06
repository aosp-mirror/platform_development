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

import {CommonModule, NgTemplateOutlet} from '@angular/common';
import {Component} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormControl, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {
  SearchQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {ActiveSearchComponent} from './active_search_component';

describe('ActiveSearchComponent', () => {
  const testQuery = 'select * from table';
  let fixture: ComponentFixture<ActiveSearchComponent>;
  let component: ActiveSearchComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ActiveSearchComponent, TestHostComponent],
      imports: [
        MatFormFieldModule,
        MatInputModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatTooltipModule,
        CommonModule,
        NgTemplateOutlet,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ActiveSearchComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;

    component.isSearchInitialized = true;
    component.lastTraceFailed = false;
    component.saveQueryNameControl = new FormControl();
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('handles search via button', () => {
    runSearchAndCheckHandled(runSearchByQueryButton);
  });

  it('handles search via enter key', () => {
    const runSearch = () => {
      const textInput = getTextInput();
      changeInput(textInput, testQuery);
      pressEnter(textInput);
    };
    runSearchAndCheckHandled(runSearch);
  });

  it('does not handle search on enter key + shift key', () => {
    let query: string | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.SearchQueryClick, (event) => {
        const detail: SearchQueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });
    const textInput = getTextInput();
    changeInput(textInput, testQuery);
    pressEnter(textInput, true);
    expect(query).toBeUndefined();
  });

  it('handles running query complete', () => {
    runSearchByQueryButton();
    component.canAdd = true;
    component.executedQuery = testQuery;
    fixture.detectChanges();
    expect(htmlElement.querySelector('.running-query-message')).toBeNull();
    expect(
      htmlElement.querySelector<HTMLButtonElement>('.add-button')?.disabled,
    ).toBeFalse();
  });

  it('handles running query failure', () => {
    runSearchByQueryButton();
    component.canAdd = true;
    component.lastTraceFailed = true;
    fixture.detectChanges();
    expect(htmlElement.querySelector('.running-query-message')).toBeNull();
    expect(
      htmlElement.querySelector<HTMLButtonElement>('.add-button')?.disabled,
    ).toBeTrue();
  });

  it('disables search query until initialized', () => {
    component.isSearchInitialized = false;
    fixture.detectChanges();
    changeInput(getTextInput(), testQuery);
    expect(getSearchQueryButton().disabled).toBeTrue();

    component.isSearchInitialized = true;
    fixture.detectChanges();
    expect(getSearchQueryButton().disabled).toBeFalse();
  });

  it('clears query', () => {
    expect(htmlElement.querySelector('.clear-button')).toBeNull();
    component.canClear = true;
    fixture.detectChanges();
    const clearButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.clear-button'),
    );
    spyOn(component.clearQueryClick, 'emit');
    expect(clearButton.textContent?.trim()).toContain('Clear');
    clearButton.click();
    fixture.detectChanges();
    expect(component.clearQueryClick.emit).toHaveBeenCalledTimes(1);
  });

  it('adds query', () => {
    expect(htmlElement.querySelector('.add-button')).toBeNull();
    component.canAdd = true;
    fixture.detectChanges();
    const addButton = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.add-button'),
    );
    expect(addButton.textContent?.trim()).toContain('+ Add Query');
    expect(addButton.disabled).toBeTrue();

    spyOn(component.addQueryClick, 'emit');
    component.executedQuery = testQuery;
    fixture.detectChanges();
    addButton.click();
    fixture.detectChanges();
    expect(component.addQueryClick.emit).toHaveBeenCalledTimes(1);
  });

  it('labels section', () => {
    component.label = 'test label';
    fixture.detectChanges();
    expect(htmlElement.querySelector('.header')?.textContent?.trim()).toEqual(
      'test label',
    );
  });

  it('shows last query execution time', () => {
    expect(htmlElement.querySelector('.query-execution-time')).toBeNull();

    component.lastQueryExecutionTime = '10 ms';
    fixture.detectChanges();
    expect(
      htmlElement.querySelector('.query-execution-time')?.textContent?.trim(),
    ).toEqual('Executed in 10 ms');
  });

  it('shows current search information and save query field', () => {
    const hostFixture = TestBed.createComponent(TestHostComponent);
    const hostComponent = hostFixture.componentInstance;
    const hostElement = hostFixture.nativeElement;
    hostFixture.detectChanges();

    expect(hostElement.querySelector('.current-search')).toBeNull();
    expect(hostElement.querySelector('.test-query')).toBeNull();
    expect(hostElement.querySelector('.test-control-value')).toBeNull();
    hostComponent.control.setValue('test name');
    hostComponent.executedQuery = 'test query';
    hostFixture.detectChanges();

    const currentSearch = assertDefined(
      hostElement.querySelector('.current-search'),
    );
    expect(currentSearch.querySelector('.query')?.textContent?.trim()).toEqual(
      'Last executed:  test query',
    );
    expect(
      currentSearch.querySelector('.test-query')?.textContent?.trim(),
    ).toEqual('test query');
    expect(
      currentSearch.querySelector('.test-control-value')?.textContent?.trim(),
    ).toEqual('test name');
  });

  function getTextInput(): HTMLTextAreaElement {
    return assertDefined(
      htmlElement.querySelector<HTMLTextAreaElement>('.query-field textarea'),
    );
  }

  function changeInput(
    input: HTMLInputElement | HTMLTextAreaElement,
    query: string,
  ) {
    input.value = query;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function getSearchQueryButton(): HTMLButtonElement {
    return assertDefined(
      htmlElement.querySelector<HTMLButtonElement>(
        '.query-actions .search-button',
      ),
    );
  }

  function runSearchByQueryButton() {
    changeInput(getTextInput(), testQuery);
    getSearchQueryButton().click();
    fixture.detectChanges();
  }

  function runSearchAndCheckHandled(runSearch: () => void) {
    spyOn(component.searchQueryClick, 'emit');
    runSearch();
    component.runningQuery = true;
    fixture.detectChanges();
    expect(component.searchQueryClick.emit).toHaveBeenCalledOnceWith(testQuery);
    expect(getSearchQueryButton().disabled).toBeTrue();
    const runningQueryMessage = assertDefined(
      htmlElement.querySelector('.running-query-message'),
    );
    expect(runningQueryMessage.textContent?.trim()).toEqual(
      'timer Calculating results',
    );
    expect(runningQueryMessage.querySelector('mat-spinner')).toBeTruthy();
  }

  function pressEnter(
    input: HTMLInputElement | HTMLTextAreaElement,
    shiftKey = false,
  ) {
    input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', shiftKey}));
    fixture.detectChanges();
  }

  @Component({
    selector: 'test-component',
    template: `
      <active-search [saveQueryField]="testTemplate" [executedQuery]=executedQuery [saveQueryNameControl]="control"></active-search>
      <ng-template #testTemplate let-search="search" let-query="query">
        <span class="test-query"> {{query}} </span>
        <span class="test-control-value"> {{control?.value}} </span>
      </ng-template>
    `,
  })
  class TestHostComponent {
    control = new FormControl();
    executedQuery: string | undefined;
  }
});
