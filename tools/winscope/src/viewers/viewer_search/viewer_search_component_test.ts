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

import {CdkMenuModule} from '@angular/cdk/menu';
import {ScrollingModule} from '@angular/cdk/scrolling';
import {Component, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTabsModule} from '@angular/material/tabs';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {
  DeleteSavedQueryClickDetail,
  QueryClickDetail,
  SaveQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {LogComponent} from 'viewers/components/log_component';
import {SearchListComponent} from './search_list_component';
import {Search, SearchResult, UiData} from './ui_data';
import {ViewerSearchComponent} from './viewer_search_component';

describe('ViewerSearchComponent', () => {
  const testQuery = 'select * from table';
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        TestHostComponent,
        ViewerSearchComponent,
        CollapsedSectionsComponent,
        CollapsibleSectionTitleComponent,
        SearchListComponent,
        LogComponent,
      ],
      imports: [
        MatFormFieldModule,
        MatInputModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatIconModule,
        MatTabsModule,
        CdkMenuModule,
        MatProgressSpinnerModule,
        ScrollingModule,
        MatTooltipModule,
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

  it('creates global search section with tabs', () => {
    const globalSearch = assertDefined(
      htmlElement.querySelector('.global-search'),
    );
    const searchTabs =
      globalSearch.querySelectorAll<HTMLElement>('.mat-tab-label');
    const [searchTab, savedTab, recentTab] = Array.from(searchTabs);
    expect(searchTab.textContent).toEqual('Search');
    expect(savedTab.textContent).toEqual('Saved');
    expect(recentTab.textContent).toEqual('Recent');
  });

  it('creates collapsed sections with no buttons', () => {
    UnitTestUtils.checkNoCollapsedSectionButtons(htmlElement);
  });

  it('handles search box section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.global-search',
      'GLOBAL SEARCH',
    );
  });

  it('handles tabulated results section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.search-results',
      'SEARCH RESULTS',
    );
  });

  it('handles documentation groups section collapse/expand', () => {
    UnitTestUtils.checkSectionCollapseAndExpand(
      htmlElement,
      fixture,
      '.how-to-search',
      'HOW TO SEARCH',
    );
  });

  it('handles search via search query click', () => {
    runSearchAndCheckHandled(runSearchByQueryButton);
  });

  it('handles search via run query from options click', () => {
    const runSearch = () => {
      component.searchComponent?.onRunQueryFromOptionsClick(
        new Search(testQuery),
      );
      fixture.detectChanges();
    };
    runSearchAndCheckHandled(runSearch);
  });

  it('handles running query complete', () => {
    runSearchByQueryButton();
    component.inputData.currentSearches.push(
      new SearchResult(testQuery, [], []),
    );
    component.inputData = Object.assign({}, component.inputData);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.running-query-message')).toBeNull();
    expect(htmlElement.querySelector('.query-execution-time')).toBeTruthy();
    expect(htmlElement.querySelector('log-view')).toBeTruthy();
    expect(getSearchQueryButton().disabled).toBeTrue();
  });

  it('handles running query failure', () => {
    runSearchByQueryButton();
    component.inputData.lastTraceFailed = true;
    component.inputData = Object.assign({}, component.inputData);
    fixture.detectChanges();
    expect(htmlElement.querySelector('.running-query-message')).toBeNull();
    expect(htmlElement.querySelector('log-view')).toBeNull();
    expect(getSearchQueryButton().disabled).toBeFalse();
  });

  it('emits event on reset query click', () => {
    let query: string | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.ResetQueryClick, (event) => {
        const detail: QueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });

    const resetButton = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.reset-button'),
    );
    expect(resetButton.disabled).toBeTrue();

    changeInput(getTextInput(), testQuery);
    component.inputData.currentSearches.push(
      new SearchResult(testQuery, [], []),
    );
    fixture.detectChanges();
    resetButton.click();
    fixture.detectChanges();
    expect(query).toEqual(testQuery);
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
        const detail: QueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });
    const textInput = getTextInput();
    changeInput(textInput, testQuery);
    pressEnter(textInput, true);
    expect(query).toBeUndefined();
  });

  it('emits event on save query click', () => {
    let detail: SaveQueryClickDetail | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.SaveQueryClick, (event) => {
        detail = (event as CustomEvent).detail;
      });
    const testName = 'Query 1';
    component.inputData.savedSearches.push(new Search(testQuery, testName));
    fixture.detectChanges();
    component.inputData.currentSearches.push(
      new SearchResult(testQuery, [], []),
    );
    fixture.detectChanges();

    const saveField = assertDefined(
      htmlElement.querySelector('.current-search .save-field'),
    );
    const saveQueryButton = assertDefined(
      saveField.querySelector<HTMLElement>('.query-button'),
    );
    const input = assertDefined(
      saveField.querySelector<HTMLInputElement>('input'),
    );
    changeInput(input, testName);
    pressEnter(input);
    saveQueryButton.click();
    fixture.detectChanges();
    expect(detail).toBeUndefined(); // name already exists

    const testName2 = 'Query 2';
    changeInput(input, testName2);
    pressEnter(input); // save by enter key
    expect(detail).toEqual(new SaveQueryClickDetail(testQuery, testName2));

    const testName3 = 'Query 3';
    changeInput(input, testName3);
    saveQueryButton.click();
    fixture.detectChanges(); // save by click
    expect(detail).toEqual(new SaveQueryClickDetail(testQuery, testName3));
  });

  it('emits event on delete query click', () => {
    let detail: DeleteSavedQueryClickDetail | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.DeleteSavedQueryClick, (event) => {
        detail = (event as CustomEvent).detail;
      });
    const search = new Search('');
    component.searchComponent?.onDeleteQueryClick(search);
    expect(detail).toEqual(new DeleteSavedQueryClickDetail(search));
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
        '.query-actions .query-button',
      ),
    );
  }

  function clickSearchQueryButton() {
    getSearchQueryButton().click();
    fixture.detectChanges();
  }

  function runSearchByQueryButton() {
    changeInput(getTextInput(), testQuery);
    clickSearchQueryButton();
  }

  function runSearchAndCheckHandled(runSearch: () => void) {
    let query: string | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.SearchQueryClick, (event) => {
        const detail: QueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });
    runSearch();
    expect(query).toEqual(testQuery);
    checkSearchQueryHandled();
  }

  function pressEnter(
    input: HTMLInputElement | HTMLTextAreaElement,
    shiftKey = false,
  ) {
    input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', shiftKey}));
    fixture.detectChanges();
  }

  function checkSearchQueryHandled() {
    expect(getSearchQueryButton().disabled).toBeTrue();
    const runningQueryMessage = assertDefined(
      htmlElement.querySelector('.running-query-message'),
    );
    expect(runningQueryMessage.textContent?.trim()).toEqual(
      'timer Calculating results',
    );
    expect(runningQueryMessage.querySelector('mat-spinner')).toBeTruthy();
    const resetButton = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.reset-button'),
    );
    expect(resetButton.disabled).toBeTrue();
  }

  @Component({
    selector: 'host-component',
    template: `
      <viewer-search [inputData]="inputData"></viewer-search>
    `,
  })
  class TestHostComponent {
    @ViewChild(ViewerSearchComponent) searchComponent:
      | ViewerSearchComponent
      | undefined;

    inputData = UiData.createEmpty();
  }
});
