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
import {TestBed} from '@angular/core/testing';
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
import {DOMTestHelper} from 'test/unit/dom_test_utils';
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
  const accordionItemSelector = '.accordion-item-header';
  const searchQuerySelector = '.query-actions .search-button';
  const listedSearchSelector = '.listed-search-option';
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

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
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.inputData.initialized = true;
    component.inputData.currentSearches = [new CurrentSearch(1)];
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('creates global search section with tabs', () => {
    const globalSearch = dom.get('.global-search');
    const [searchTab, savedTab, recentTab] =
      globalSearch.findAll('.mat-tab-label');
    searchTab.checkTextExact('Search');
    savedTab.checkTextExact('Saved');
    recentTab.checkTextExact('Recent');
  });

  it('creates collapsed sections with no buttons', () => {
    dom.checkNoCollapsedSectionButtons();
  });

  it('handles search box section collapse/expand', () => {
    dom.checkSectionCollapseAndExpand('.global-search', 'GLOBAL SEARCH');
  });

  it('handles tabulated results section collapse/expand', () => {
    dom.checkSectionCollapseAndExpand('.search-results', 'SEARCH RESULTS');
  });

  it('handles documentation groups section collapse/expand', () => {
    dom.checkSectionCollapseAndExpand('.how-to-search', 'HOW TO SEARCH');
  });

  it('handles search via search query click', () => {
    runSearchAndCheckHandled(runSearchByQueryButton);
  });

  it('handles search via run query from saved without creating new active search', async () => {
    component.inputData.savedSearches = [new ListedSearch(testQuery, 'saved1')];
    dom.detectChanges();
    await changeTab(1);
    runSearchAndCheckHandled(() => dom.findAndClick(listedSearchSelector));
  });

  it('handles search via run query from recents without creating new active search', async () => {
    component.inputData.recentSearches = [new ListedSearch(testQuery)];
    dom.detectChanges();
    await changeTab(2);
    runSearchAndCheckHandled(() => dom.findAndClick(listedSearchSelector));
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
    expect(dom.find(placeholderCss)).toBeDefined();

    dom.get(searchQuerySelector).click();
    runSearchByQueryButton();
    expect(dom.find(placeholderCss)).toBeUndefined();

    addCurrentSearchWithResult();
    expect(dom.find('.query-execution-time')).toBeDefined();
    expect(dom.find('log-view')).toBeDefined();
    expect(dom.find(placeholderCss)).toBeUndefined();
  });

  it('adds search sections', () => {
    const spy = jasmine.createSpy();
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.AddQueryClick, (event) => {
        const detail: AddQueryClickDetail = (event as CustomEvent).detail;
        expect(detail).toBeFalsy();
        spy();
      });

    const addButton = dom.get('.add-button');
    expect(dom.find('.clear-button')).toBeUndefined();
    addButton.checkDisabled(true);

    const data = structuredClone(component.inputData);
    data.currentSearches[0].query = testQuery;
    updateInputDataAndDetectChanges(data);

    addButton.click();
    expect(spy).toHaveBeenCalledTimes(1);

    const newData = structuredClone(component.inputData);
    newData.currentSearches.push(new CurrentSearch(2));
    updateInputDataAndDetectChanges(newData);

    const activeSections = dom.findAll('active-search');
    expect(activeSections.length).toEqual(2);
    expect(activeSections[0].find('.clear-button')).toBeDefined();
    expect(activeSections[1].find('.clear-button')).toBeDefined();

    expect(activeSections[0].find('.add-button')).toBeUndefined();
    activeSections[1].get('.add-button').checkDisabled(true);
  });

  it('handles multiple results', async () => {
    let uid: number | undefined;
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.ClearQueryClick, (event) => {
        const detail: ClearQueryClickDetail = (event as CustomEvent).detail;
        uid = detail.uid;
      });

    const data = structuredClone(component.inputData);
    data.currentSearches[0].result = new SearchResult([], []);
    updateInputDataAndDetectChanges(data);
    addCurrentSearchWithResult(testQuery, 2);
    let resultTabs = dom.findAll('.result-tabs .mat-tab-label');
    let activeSections = dom.findAll('active-search');
    expect(activeSections.length).toEqual(2);
    expect(resultTabs.length).toEqual(2);
    resultTabs[0].checkTextExact('Query 1');
    resultTabs[1].checkTextExact('Query 2');

    dom.findAndClick('.clear-button');
    expect(uid).toEqual(1);

    const spy = spyOn(activeSections[1].getHTMLElement(), 'scrollIntoView');

    const newData = structuredClone(component.inputData);
    newData.currentSearches.shift();
    updateInputDataAndDetectChanges(newData);
    await dom.whenStable();

    resultTabs = dom.findAll('.result-tabs .mat-tab-label');
    activeSections = dom.findAll('active-search');
    expect(resultTabs.length).toEqual(1);
    resultTabs[0].checkTextExact('Query 2');
    expect(activeSections.length).toEqual(1);
    expect(spy).toHaveBeenCalled();
  });

  it('handles running query failure', () => {
    runSearchByQueryButton();
    const data = structuredClone(component.inputData);
    data.lastTraceFailed = true;
    updateInputDataAndDetectChanges(data);
    expect(dom.find('.query-execution-time')).toBeDefined();
    expect(dom.find('.running-query-message')).toBeUndefined();
    expect(dom.find('log-view')).toBeUndefined();
    dom.get(searchQuerySelector).checkDisabled(false);
  });

  it('emits event on save query click', () => {
    let detail: SaveQueryClickDetail | undefined;
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.SaveQueryClick, (event) => {
        detail = (event as CustomEvent).detail;
      });
    const testName = 'Query 1';
    component.inputData.savedSearches.push(
      new ListedSearch(testQuery, testName),
    );
    dom.detectChanges();
    addCurrentSearchWithResult();
    const saveField = dom.get('.current-search .save-field');
    const saveQueryButton = saveField.get('.query-button');
    const input = saveField.get('input');
    input.dispatchInput(testName);
    saveQueryButton.click();
    expect(detail).toBeUndefined(); // name already exists

    const testName2 = 'Query 2';
    input.dispatchInput(testName2);
    input.keydownEnter(); // save by enter key
    expect(detail).toEqual(new SaveQueryClickDetail(testQuery, testName2));

    const testName3 = 'Query 3';
    input.dispatchInput(testName3);
    saveQueryButton.click(); // save by click
    expect(detail).toEqual(new SaveQueryClickDetail(testQuery, testName3));
  });

  it('emits event on delete saved query click', async () => {
    let detail: DeleteSavedQueryClickDetail | undefined;
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.DeleteSavedQueryClick, (event) => {
        detail = (event as CustomEvent).detail;
      });
    const search = new ListedSearch(testQuery);
    component.inputData.savedSearches = [search];
    dom.detectChanges();

    await changeTab(1);
    dom.findAndClickByIndex(listedSearchSelector, 2);
    expect(detail).toEqual(new DeleteSavedQueryClickDetail(search));
  });

  it('handles trace search initialization', () => {
    component.inputData.initialized = false;
    dom.detectChanges();
    const spy = jasmine.createSpy();
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.GlobalSearchSectionClick, (event) =>
        spy(),
      );
    const globalSearch = dom.get('.global-search');
    expect(globalSearch.find('.message-with-spinner')).toBeUndefined();

    clickGlobalSearchAndCheckMessage(globalSearch);
    clickGlobalSearchAndCheckMessage(globalSearch);
    expect(spy).toHaveBeenCalledTimes(1);

    getTextInput().dispatchInput(testQuery);
    dom.get(searchQuerySelector).checkDisabled(true);

    const data = structuredClone(component.inputData);
    data.initialized = true;
    updateInputDataAndDetectChanges(data);
    expect(globalSearch.find('.message-with-spinner')).toBeUndefined();
    dom.get(searchQuerySelector).checkDisabled(false);
  });

  it('can open SQL view descriptors in how to section', () => {
    const accordionItems = dom.findAll('.how-to-search .accordion-item');
    expect(accordionItems.length).toEqual(6);
    accordionItems.forEach((item) => checkAccordionItemCollapsed(item));

    accordionItems[0].get(accordionItemSelector).click();
    checkAccordionItemExpanded(accordionItems[0]);
    checkAccordionItemCollapsed(accordionItems[1]);

    accordionItems[1].get(accordionItemSelector).click();
    checkAccordionItemExpanded(accordionItems[0]);
    checkAccordionItemExpanded(accordionItems[1]);

    accordionItems[0].get(accordionItemSelector).click();
    checkAccordionItemCollapsed(accordionItems[0]);
    checkAccordionItemExpanded(accordionItems[1]);
  });

  function clickGlobalSearchAndCheckMessage(
    globalSearch: DOMTestHelper<TestHostComponent>,
  ) {
    globalSearch.click();
    expect(dom.find('.message-with-spinner')).toBeDefined();
    dom.get(searchQuerySelector).checkDisabled(true);
  }

  function getTextInput(i = 0): DOMTestHelper<TestHostComponent> {
    return dom.findAll('.query-field textarea')[i];
  }

  function runSearchByQueryButton(i = 0) {
    getTextInput(i).dispatchInput(testQuery);
    dom.findAndClickByIndex(searchQuerySelector, i);
  }

  async function changeTab(index: number) {
    const matTabGroups = assertDefined(component.searchComponent?.matTabGroups);
    matTabGroups.first.selectedIndex = index;
    await dom.detectChangesAndWaitStable();
  }

  async function checkRunQueryFromOptionsWhenResultPresent(tabIndex: number) {
    const data = structuredClone(component.inputData);
    data.currentSearches[0].query = testQuery;
    data.currentSearches[0].result = new SearchResult([], []);
    let query: string | undefined;
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.AddQueryClick, (event) => {
        const detail: AddQueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });
    updateInputDataAndDetectChanges(data);

    await changeTab(tabIndex);
    dom.findAndClick(listedSearchSelector);
    expect(query).toEqual(testQuery);
    await changeTab(0);
    runSearchAndCheckHandled(addCurrentSearchWithResult);
    expect(dom.findAll('active-search').length).toEqual(2);
  }

  function runSearchAndCheckHandled(runSearch: () => void) {
    let query: string | undefined;
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.SearchQueryClick, (event) => {
        const detail: SearchQueryClickDetail = (event as CustomEvent).detail;
        query = detail.query;
      });
    runSearch();
    expect(query).toEqual(testQuery);
    dom.get(searchQuerySelector).checkDisabled(true);
    const runningQueryMessage = dom.get('.running-query-message');
    runningQueryMessage.checkTextExact('timer Calculating results');
    expect(runningQueryMessage.find('mat-spinner')).toBeDefined();
  }

  async function checkEditQueryFromOptionsWhenResultPresent(tabIndex: number) {
    component.inputData.currentSearches[0].result = new SearchResult([], []);
    dom.detectChanges();

    let query: string | undefined;
    dom
      .get('viewer-search')
      .addEventListener(ViewerEvents.AddQueryClick, (event) => {
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
    await dom.detectChangesAndWaitStable();
    expect(
      component.searchComponent?.matTabGroups?.first.selectedIndex,
    ).toEqual(0);
    getTextInput(0).checkValue('');
    getTextInput(1).checkValue(testQuery);
  }

  async function checkEditQueryFromOptions(tabIndex: number) {
    dom.detectChanges();
    const input = getTextInput();
    expect(input.checkValue(''));
    await changeTabAndClickEdit(tabIndex);
    expect(
      component.searchComponent?.matTabGroups?.first.selectedIndex,
    ).toEqual(0);
    expect(input.checkValue(testQuery));
  }

  async function changeTabAndClickEdit(tabIndex: number) {
    await changeTab(tabIndex);
    const listedSearchButton = dom.findAll('.listed-search-option');
    listedSearchButton[1].click();
    await dom.whenStable();
  }

  function addCurrentSearchWithResult(q = testQuery, uid = 2) {
    const data = structuredClone(component.inputData);
    const currentSearch = new CurrentSearch(uid, q, new SearchResult([], []));
    data.currentSearches.push(currentSearch);
    updateInputDataAndDetectChanges(data);
  }

  function checkAccordionItemCollapsed(item: DOMTestHelper<TestHostComponent>) {
    item.get(accordionItemSelector).checkText('chevron_right');
    expect(item.find('.accordion-item-body')).toBeUndefined();
  }

  function checkAccordionItemExpanded(item: DOMTestHelper<TestHostComponent>) {
    item.get(accordionItemSelector).checkText('arrow_drop_down');
    expect(item.find('.accordion-item-body')).toBeDefined();
  }

  function updateInputDataAndDetectChanges(data: UiData) {
    component.inputData = data;
    dom.detectChanges();
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
