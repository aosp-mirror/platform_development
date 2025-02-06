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

import {CdkAccordionModule} from '@angular/cdk/accordion';
import {CdkMenuModule} from '@angular/cdk/menu';
import {ScrollingModule} from '@angular/cdk/scrolling';
import {Component, ViewChild} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
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
  AddQueryClickDetail,
  ClearQueryClickDetail,
  DeleteSavedQueryClickDetail,
  SaveQueryClickDetail,
  SearchQueryClickDetail,
  ViewerEvents,
} from 'viewers/common/viewer_events';
import {CollapsedSectionsComponent} from 'viewers/components/collapsed_sections_component';
import {CollapsibleSectionTitleComponent} from 'viewers/components/collapsible_section_title_component';
import {LogComponent} from 'viewers/components/log_component';
import {ActiveSearchComponent} from './active_search_component';
import {SearchListComponent} from './search_list_component';
import {CurrentSearch, ListedSearch, SearchResult, UiData} from './ui_data';
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
        ActiveSearchComponent,
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
        CdkAccordionModule,
        MatDividerModule,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.inputData.initialized = true;
    component.inputData.currentSearches = [new CurrentSearch(1)];
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

  it('handles search via run query from saved without creating new active search', async () => {
    component.inputData.savedSearches = [new ListedSearch(testQuery, 'saved1')];
    fixture.detectChanges();
    await changeTab(1);
    runSearchAndCheckHandled(runSearchFromListedSearchOption);
  });

  it('handles search via run query from recents without creating new active search', async () => {
    component.inputData.recentSearches = [new ListedSearch(testQuery)];
    fixture.detectChanges();
    await changeTab(2);
    runSearchAndCheckHandled(runSearchFromListedSearchOption);
  });

  it('handles search via run query from saved creating new active search', async () => {
    component.inputData.savedSearches = [new ListedSearch(testQuery, 'saved1')];
    await checkRunQueryFromOptionsWhenResultPresent(1);
  });

  it('handles search via run query from recents creating new active search', async () => {
    component.inputData.recentSearches = [new ListedSearch(testQuery)];
    await checkRunQueryFromOptionsWhenResultPresent(2);
  });

  it('handles edit saved search without creating new section', async () => {
    component.inputData.savedSearches = [new ListedSearch(testQuery, 'saved1')];
    await checkEditQueryFromOptions(1);
  });

  it('handles edit recent search without creating new section', async () => {
    component.inputData.recentSearches = [new ListedSearch(testQuery)];
    await checkEditQueryFromOptions(2);
  });

  it('handles edit saved search creating new section', async () => {
    component.inputData.savedSearches = [new ListedSearch(testQuery, 'saved1')];
    await checkEditQueryFromOptionsWhenResultPresent(1);
  });

  it('handles edit recent search creating new section', async () => {
    component.inputData.recentSearches = [new ListedSearch(testQuery)];
    await checkEditQueryFromOptionsWhenResultPresent(2);
  });

  it('handles running query complete', () => {
    const placeholderCss = '.results-placeholder.placeholder-text';
    expect(htmlElement.querySelector(placeholderCss)).toBeTruthy();

    clickSearchQueryButton();
    runSearchByQueryButton();
    expect(htmlElement.querySelector(placeholderCss)).toBeNull();

    addCurrentSearchWithResult();
    expect(htmlElement.querySelector('.query-execution-time')).toBeTruthy();
    expect(htmlElement.querySelector('log-view')).toBeTruthy();
    expect(htmlElement.querySelector(placeholderCss)).toBeNull();
  });

  it('adds search sections', () => {
    const spy = jasmine.createSpy();
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.AddQueryClick, (event) => {
        const detail: AddQueryClickDetail = (event as CustomEvent).detail;
        expect(detail).toBeFalsy();
        spy();
      });

    let addButton = assertDefined(
      htmlElement.querySelector<HTMLButtonElement>('.add-button'),
    );
    expect(htmlElement.querySelector('.clear-button')).toBeNull();
    expect(addButton.disabled).toBeTrue();

    const data = structuredClone(component.inputData);
    data.currentSearches[0].query = testQuery;
    updateInputDataAndDetectChanges(data);

    addButton.click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledTimes(1);

    const newData = structuredClone(component.inputData);
    newData.currentSearches.push(new CurrentSearch(2));
    updateInputDataAndDetectChanges(newData);

    const activeSections = htmlElement.querySelectorAll('active-search');
    expect(activeSections.length).toEqual(2);
    expect(activeSections.item(0).querySelector('.clear-button')).toBeTruthy();
    expect(activeSections.item(1).querySelector('.clear-button')).toBeTruthy();

    expect(activeSections.item(0).querySelector('.add-button')).toBeNull();
    addButton = assertDefined(
      activeSections.item(1).querySelector<HTMLButtonElement>('.add-button'),
    );
    expect(addButton.disabled).toBeTrue();
  });

  it('handles multiple results', async () => {
    let uid: number | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.ClearQueryClick, (event) => {
        const detail: ClearQueryClickDetail = (event as CustomEvent).detail;
        uid = detail.uid;
      });

    const data = structuredClone(component.inputData);
    data.currentSearches[0].result = new SearchResult([], []);
    updateInputDataAndDetectChanges(data);
    addCurrentSearchWithResult(testQuery, 2);
    let resultTabs = htmlElement.querySelectorAll(
      '.result-tabs .mat-tab-label',
    );
    let activeSections =
      htmlElement.querySelectorAll<HTMLElement>('active-search');
    expect(activeSections.length).toEqual(2);
    expect(resultTabs.length).toEqual(2);
    expect(resultTabs.item(0).textContent).toEqual('Query 1');
    expect(resultTabs.item(1).textContent).toEqual('Query 2');

    const clearButton = assertDefined(
      htmlElement.querySelector<HTMLElement>('.clear-button'),
    );
    clearButton.click();
    fixture.detectChanges();
    expect(uid).toEqual(1);

    const finalActiveSection = activeSections.item(1);
    const spy = spyOn(finalActiveSection, 'scrollIntoView');

    const newData = structuredClone(component.inputData);
    newData.currentSearches.shift();
    updateInputDataAndDetectChanges(newData);
    await fixture.whenStable();

    resultTabs = htmlElement.querySelectorAll('.result-tabs .mat-tab-label');
    activeSections = htmlElement.querySelectorAll('active-search');
    expect(resultTabs.length).toEqual(1);
    expect(resultTabs.item(0).textContent).toEqual('Query 2');
    expect(activeSections.length).toEqual(1);
    expect(spy).toHaveBeenCalled();
  });

  it('handles running query failure', () => {
    runSearchByQueryButton();
    const data = structuredClone(component.inputData);
    data.lastTraceFailed = true;
    updateInputDataAndDetectChanges(data);
    expect(htmlElement.querySelector('.query-execution-time')).toBeTruthy();
    expect(htmlElement.querySelector('.running-query-message')).toBeNull();
    expect(htmlElement.querySelector('log-view')).toBeNull();
    expect(getSearchQueryButton().disabled).toBeFalse();
  });

  it('emits event on save query click', () => {
    let detail: SaveQueryClickDetail | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.SaveQueryClick, (event) => {
        detail = (event as CustomEvent).detail;
      });
    const testName = 'Query 1';
    component.inputData.savedSearches.push(
      new ListedSearch(testQuery, testName),
    );
    fixture.detectChanges();
    addCurrentSearchWithResult();
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

  it('emits event on delete saved query click', async () => {
    let detail: DeleteSavedQueryClickDetail | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.DeleteSavedQueryClick, (event) => {
        detail = (event as CustomEvent).detail;
      });
    const search = new ListedSearch(testQuery);
    component.inputData.savedSearches = [search];
    fixture.detectChanges();

    await changeTab(1);
    const listedSearchButton = assertDefined(
      htmlElement.querySelectorAll<HTMLElement>('.listed-search-option'),
    );
    listedSearchButton.item(2).click();
    expect(detail).toEqual(new DeleteSavedQueryClickDetail(search));
  });

  it('handles trace search initialization', () => {
    component.inputData.initialized = false;
    fixture.detectChanges();
    const spy = jasmine.createSpy();
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.GlobalSearchSectionClick, (event) =>
        spy(),
      );
    const globalSearch = assertDefined(
      htmlElement.querySelector<HTMLElement>('.global-search'),
    );
    expect(globalSearch.querySelector('.message-with-spinner')).toBeNull();

    clickGlobalSearchAndCheckMessage(globalSearch);
    clickGlobalSearchAndCheckMessage(globalSearch);
    expect(spy).toHaveBeenCalledTimes(1);

    changeInput(getTextInput(), testQuery);
    expect(getSearchQueryButton().disabled).toBeTrue();

    const data = structuredClone(component.inputData);
    data.initialized = true;
    updateInputDataAndDetectChanges(data);
    expect(globalSearch.querySelector('.message-with-spinner')).toBeNull();
    expect(getSearchQueryButton().disabled).toBeFalse();
  });

  it('can open SQL view descriptors in how to section', () => {
    const accordionItems = htmlElement.querySelectorAll<HTMLElement>(
      '.how-to-search .accordion-item',
    );
    expect(accordionItems.length).toEqual(6);
    accordionItems.forEach((item) => checkAccordionItemCollapsed(item));

    clickAccordionItemHeader(accordionItems.item(0));
    checkAccordionItemExpanded(accordionItems.item(0));
    checkAccordionItemCollapsed(accordionItems.item(1));

    clickAccordionItemHeader(accordionItems.item(1));
    checkAccordionItemExpanded(accordionItems.item(0));
    checkAccordionItemExpanded(accordionItems.item(1));

    clickAccordionItemHeader(accordionItems.item(0));
    checkAccordionItemCollapsed(accordionItems.item(0));
    checkAccordionItemExpanded(accordionItems.item(1));
  });

  function clickGlobalSearchAndCheckMessage(globalSearch: HTMLElement) {
    globalSearch.click();
    fixture.detectChanges();
    expect(globalSearch.querySelector('.message-with-spinner')).toBeTruthy();
    expect(getSearchQueryButton().disabled).toBeTrue();
  }

  function getTextInput(i = 0): HTMLTextAreaElement {
    return htmlElement
      .querySelectorAll<HTMLTextAreaElement>('.query-field textarea')
      .item(i);
  }

  function changeInput(
    input: HTMLInputElement | HTMLTextAreaElement,
    query: string,
  ) {
    input.value = query;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function getSearchQueryButton(i = 0): HTMLButtonElement {
    return htmlElement
      .querySelectorAll<HTMLButtonElement>('.query-actions .search-button')
      .item(i);
  }

  function clickSearchQueryButton(i = 0) {
    getSearchQueryButton(i).click();
    fixture.detectChanges();
  }

  function runSearchByQueryButton(i = 0) {
    changeInput(getTextInput(i), testQuery);
    clickSearchQueryButton(i);
  }

  async function changeTab(index: number) {
    const matTabGroups = assertDefined(component.searchComponent?.matTabGroups);
    matTabGroups.first.selectedIndex = index;
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function checkRunQueryFromOptionsWhenResultPresent(tabIndex: number) {
    const data = structuredClone(component.inputData);
    data.currentSearches[0].query = testQuery;
    data.currentSearches[0].result = new SearchResult([], []);
    let query: string | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.AddQueryClick, (event) => {
        const detail: AddQueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });
    updateInputDataAndDetectChanges(data);

    await changeTab(tabIndex);
    runSearchFromListedSearchOption();
    expect(query).toEqual(testQuery);
    await changeTab(0);
    runSearchAndCheckHandled(addCurrentSearchWithResult);
    const activeSections = htmlElement.querySelectorAll('active-search');
    expect(activeSections.length).toEqual(2);
  }

  function runSearchFromListedSearchOption() {
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.listed-search-option'),
    ).click();
    fixture.detectChanges();
  }

  function runSearchAndCheckHandled(runSearch: () => void) {
    let query: string | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.SearchQueryClick, (event) => {
        const detail: SearchQueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });
    runSearch();
    expect(query).toEqual(testQuery);
    expect(getSearchQueryButton().disabled).toBeTrue();
    const runningQueryMessage = assertDefined(
      htmlElement.querySelector('.running-query-message'),
    );
    expect(runningQueryMessage.textContent?.trim()).toEqual(
      'timer Calculating results',
    );
    expect(runningQueryMessage.querySelector('mat-spinner')).toBeTruthy();
  }

  function pressEnter(input: HTMLInputElement, shiftKey = false) {
    input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', shiftKey}));
    fixture.detectChanges();
  }

  async function checkEditQueryFromOptionsWhenResultPresent(tabIndex: number) {
    component.inputData.currentSearches[0].result = new SearchResult([], []);
    fixture.detectChanges();

    let query: string | undefined;
    htmlElement
      .querySelector('viewer-search')
      ?.addEventListener(ViewerEvents.AddQueryClick, (event) => {
        const detail: AddQueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });

    await changeTabAndClickEdit(tabIndex);
    expect(
      component.searchComponent?.matTabGroups?.first.selectedIndex,
    ).toEqual(tabIndex);
    expect(query).toEqual(testQuery);

    const data = structuredClone(component.inputData);
    data.currentSearches.push(new CurrentSearch(2, testQuery));
    updateInputDataAndDetectChanges(data);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(
      component.searchComponent?.matTabGroups?.first.selectedIndex,
    ).toEqual(0);
    expect(getTextInput(0).value).toEqual('');
    expect(getTextInput(1).value).toEqual(testQuery);
  }

  async function checkEditQueryFromOptions(tabIndex: number) {
    fixture.detectChanges();
    const input = getTextInput();
    expect(input.value).toEqual('');
    await changeTabAndClickEdit(tabIndex);
    expect(
      component.searchComponent?.matTabGroups?.first.selectedIndex,
    ).toEqual(0);
    expect(input.value).toEqual(testQuery);
  }

  async function changeTabAndClickEdit(tabIndex: number) {
    await changeTab(tabIndex);
    const listedSearchButton = assertDefined(
      htmlElement.querySelectorAll<HTMLElement>('.listed-search-option'),
    );
    listedSearchButton.item(1).click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function addCurrentSearchWithResult(q = testQuery, uid = 2) {
    const data = structuredClone(component.inputData);
    const currentSearch = new CurrentSearch(uid, q, new SearchResult([], []));
    data.currentSearches.push(currentSearch);
    updateInputDataAndDetectChanges(data);
  }

  function getAccordionItemHeader(item: HTMLElement) {
    return assertDefined(
      item.querySelector<HTMLElement>('.accordion-item-header'),
    );
  }

  function clickAccordionItemHeader(item: HTMLElement) {
    const header = getAccordionItemHeader(item);
    header.click();
    fixture.detectChanges();
  }

  function checkAccordionItemCollapsed(item: HTMLElement) {
    const header = getAccordionItemHeader(item);
    expect(header.textContent).toContain('chevron_right');
    expect(item.querySelector('.accordion-item-body')).toBeNull();
  }

  function checkAccordionItemExpanded(item: HTMLElement) {
    const header = getAccordionItemHeader(item);
    expect(header.textContent).toContain('arrow_drop_down');
    expect(item.querySelector('.accordion-item-body')).toBeTruthy();
  }

  function updateInputDataAndDetectChanges(data: UiData) {
    component.inputData = data;
    fixture.detectChanges();
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
