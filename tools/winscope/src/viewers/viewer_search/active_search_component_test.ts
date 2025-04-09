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
import {TestBed} from '@angular/core/testing';
import {FormControl, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {
  SearchQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {ActiveSearchComponent} from './active_search_component';

describe('ActiveSearchComponent', () => {
  const testQuery = 'select * from table';
  let component: ActiveSearchComponent;
  let dom: DOMTestHelper<ActiveSearchComponent>;

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
    const fixture = TestBed.createComponent(ActiveSearchComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.isSearchInitialized = true;
    component.lastTraceFailed = false;
    component.saveQueryNameControl = new FormControl();
    dom.detectChanges();
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
      textInput.dispatchInput(testQuery);
      textInput.keydownEnter();
    };
    runSearchAndCheckHandled(runSearch);
  });

  it('does not handle search on enter key + shift key', () => {
    let query: string | undefined;
    dom.addEventListener(ViewerEvents.SearchQueryClick, (event) => {
      const detail: SearchQueryClickDetail = (event as CustomEvent).detail;
      query = detail.query;
    });
    const textInput = getTextInput();
    textInput.dispatchInput(testQuery);
    textInput.keydownEnter(true);
    expect(query).toBeUndefined();
  });

  it('handles running query complete', () => {
    runSearchByQueryButton();
    component.canAdd = true;
    component.executedQuery = testQuery;
    dom.detectChanges();
    expect(dom.find('.running-query-message')).toBeUndefined();
    dom.get('.add-button').checkDisabled(false);
  });

  it('handles running query failure', () => {
    runSearchByQueryButton();
    component.canAdd = true;
    component.lastTraceFailed = true;
    dom.detectChanges();
    expect(dom.find('.running-query-message')).toBeUndefined();
    dom.get('.add-button').checkDisabled(true);
  });

  it('disables search query until initialized', () => {
    component.isSearchInitialized = false;
    dom.detectChanges();
    getTextInput().dispatchInput(testQuery);
    getSearchQueryButton().checkDisabled(true);

    component.isSearchInitialized = true;
    dom.detectChanges();
    getSearchQueryButton().checkDisabled(false);
  });

  it('clears query', () => {
    expect(dom.find('.clear-button')).toBeUndefined();
    component.canClear = true;
    dom.detectChanges();
    const clearButton = dom.get('.clear-button');
    spyOn(component.clearQueryClick, 'emit');
    clearButton.checkText('Clear');
    clearButton.click();
    expect(component.clearQueryClick.emit).toHaveBeenCalledTimes(1);
  });

  it('adds query', () => {
    expect(dom.find('.add-button')).toBeUndefined();
    component.canAdd = true;
    dom.detectChanges();
    const addButton = dom.get('.add-button');
    addButton.checkText('+ Add Query');
    addButton.checkDisabled(true);

    spyOn(component.addQueryClick, 'emit');
    component.executedQuery = testQuery;
    dom.detectChanges();
    addButton.click();
    expect(component.addQueryClick.emit).toHaveBeenCalledTimes(1);
  });

  it('labels section', () => {
    component.label = 'test label';
    dom.detectChanges();
    dom.get('.header').checkText('test label');
  });

  it('shows last query execution time', () => {
    expect(dom.find('.query-execution-time')).toBeUndefined();
    component.lastQueryExecutionTime = '10 ms';
    dom.detectChanges();
    dom.get('.query-execution-time').checkText('Executed in 10 ms');
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

  function getTextInput(): DOMTestHelper<ActiveSearchComponent> {
    return dom.get('.query-field textarea');
  }

  function getSearchQueryButton(): DOMTestHelper<ActiveSearchComponent> {
    return dom.get('.query-actions .search-button');
  }

  function runSearchByQueryButton() {
    getTextInput().dispatchInput(testQuery);
    getSearchQueryButton().click();
  }

  function runSearchAndCheckHandled(runSearch: () => void) {
    spyOn(component.searchQueryClick, 'emit');
    runSearch();
    component.runningQuery = true;
    dom.detectChanges();
    expect(component.searchQueryClick.emit).toHaveBeenCalledOnceWith(testQuery);
    getSearchQueryButton().checkDisabled(true);
    const runningQueryMessage = dom.get('.running-query-message');
    runningQueryMessage.checkTextExact('timer Calculating results');
    expect(runningQueryMessage.find('mat-spinner')).toBeDefined();
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
