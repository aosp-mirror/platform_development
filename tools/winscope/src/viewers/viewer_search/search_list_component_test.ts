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
import {NgTemplateOutlet} from '@angular/common';
import {Component, ViewChild} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {ListItemOption, SearchListComponent} from './search_list_component';
import {ListedSearch} from './ui_data';

describe('SearchListComponent', () => {
  let component: TestHostComponent;
  let dom: DOMTestHelper<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TestHostComponent, SearchListComponent],
      imports: [
        CdkMenuModule,
        BrowserAnimationsModule,
        MatTooltipModule,
        MatIconModule,
        MatButtonModule,
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('shows placeholder text if no searches', () => {
    dom.checkTextExact('');
    const placeholderText = 'placeholder text';
    component.placeholderText = placeholderText;
    dom.detectChanges();
    dom.checkTextExact(placeholderText);
  });

  it('shows search names with tooltips', async () => {
    component.searches = [
      new ListedSearch('query1', 'name1'),
      new ListedSearch('query2', 'query2'),
    ];
    dom.detectChanges();

    const listedSearches = dom.findAll('.listed-search');
    expect(listedSearches.length).toEqual(2);

    const queryName1 = listedSearches[0].get('.listed-search-name');
    const queryName2 = listedSearches[1].get('.listed-search-name');
    queryName1.checkTextExact('name1');
    queryName2.checkTextExact('query2');

    // shows tooltip when name and query are different
    await queryName1.checkTooltip('name1: query1');

    // does not show tooltip when name and query are the same
    await queryName2.checkTooltip(undefined);

    // shows tooltip when element is overflowing
    const query2El = queryName2.getHTMLElement();
    query2El.style.maxWidth = query2El.offsetWidth / 2 + 'px';
    dom.detectChanges();
    await queryName2.checkTooltip('query2');
  });

  it('formats search dates', () => {
    spyOn(Date, 'now').and.returnValue(1000);
    component.searches = [new ListedSearch('query1', 'name1')];
    dom.detectChanges();
    const expectedDate = new Date(1000);
    dom
      .get('.listed-search-date-options')
      .checkTextExact(
        `${expectedDate
          .toTimeString()
          .slice(0, 5)}\n${expectedDate.toLocaleDateString()}`,
      );
  });

  it('shows options and triggers callback on interaction', async () => {
    let optionClicked: ListedSearch | undefined;
    component.searches = [new ListedSearch('query1', 'name1')];
    dom.detectChanges();
    // does not show menu button if no options
    expect(dom.find('.listed-search-options')).toBeUndefined();

    const onClickCallback = (search: ListedSearch) => (optionClicked = search);
    component.listItemOptions = [
      {name: 'option1', icon: 'test', onClickCallback},
    ];
    dom.detectChanges();

    const option = dom.get('.listed-search-option');
    await option.checkTooltip('option1');
    option.click();
    expect(optionClicked).toEqual(component.searches[0]);
  });

  it('shows menu', async () => {
    component.listItemOptions = [
      {name: 'option1', icon: 'test', menu: component.testTemplate},
    ];
    component.searches = [new ListedSearch('query1', 'name1')];
    dom.detectChanges();
    const option = dom.get('.listed-search-option');
    await option.checkTooltip('option1');
    option.click();
    const menu = dom.getInDocument('.context-menu');
    expect(menu.find('.test-menu-item')).toBeDefined();
  });

  @Component({
    selector: 'host-component',
    template: `
      <search-list
        [searches]="searches"
        [placeholderText]="placeholderText"
        [listItemOptions]="listItemOptions"></search-list>

      <ng-template #testTemplate>
        <span class="test-menu-item"></span>
      </ng-template>
    `,
  })
  class TestHostComponent {
    @ViewChild(SearchListComponent) searchListComponent:
      | SearchListComponent
      | undefined;
    @ViewChild('testTemplate') testTemplate: NgTemplateOutlet | undefined;

    searches: ListedSearch[] = [];
    placeholderText: string | undefined;
    listItemOptions: ListItemOption[] = [];
  }
});
