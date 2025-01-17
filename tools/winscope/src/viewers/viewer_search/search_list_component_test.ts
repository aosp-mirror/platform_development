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
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {ListItemOption, SearchListComponent} from './search_list_component';
import {ListedSearch} from './ui_data';

describe('SearchListComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let htmlElement: HTMLElement;

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
    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('shows placeholder text if no searches', () => {
    expect(htmlElement.textContent?.trim()).toEqual('');
    const placeholderText = 'placeholder text';
    component.placeholderText = placeholderText;
    fixture.detectChanges();
    expect(htmlElement.textContent?.trim()).toEqual(placeholderText);
  });

  it('shows search names with tooltips', async () => {
    component.searches = [
      new ListedSearch('query1', 'name1'),
      new ListedSearch('query2', 'query2'),
    ];
    fixture.detectChanges();

    const listedSearches =
      htmlElement.querySelectorAll<HTMLElement>('.listed-search');
    expect(listedSearches.length).toEqual(2);

    const queryName1 = assertDefined(
      listedSearches[0].querySelector<HTMLElement>('.listed-search-name'),
    );
    const queryName2 = assertDefined(
      listedSearches[1].querySelector<HTMLElement>('.listed-search-name'),
    );
    expect(queryName1.textContent?.trim()).toEqual('name1');
    expect(queryName2.textContent?.trim()).toEqual('query2');

    // shows tooltip when name and query are different
    await UnitTestUtils.checkTooltips([queryName1], ['name1: query1'], fixture);

    // does not show tooltip when name and query are the same
    await UnitTestUtils.checkTooltips([queryName2], [undefined], fixture);

    // shows tooltip when element is overflowing
    queryName2.style.maxWidth = queryName2.offsetWidth / 2 + 'px';
    fixture.detectChanges();
    await UnitTestUtils.checkTooltips([queryName2], ['query2'], fixture);
  });

  it('formats search dates', () => {
    spyOn(Date, 'now').and.returnValue(1000);
    component.searches = [new ListedSearch('query1', 'name1')];
    fixture.detectChanges();
    const expectedDate = new Date(1000);
    expect(
      htmlElement
        .querySelector('.listed-search-date-options')
        ?.textContent?.trim(),
    ).toEqual(
      `${expectedDate
        .toTimeString()
        .slice(0, 5)}\n${expectedDate.toLocaleDateString()}`,
    );
  });

  it('shows options and triggers callback on interaction', () => {
    let optionClicked: ListedSearch | undefined;
    component.searches = [new ListedSearch('query1', 'name1')];
    fixture.detectChanges();
    // does not show menu button if no options
    expect(htmlElement.querySelector('.listed-search-options')).toBeNull();

    const onClickCallback = (search: ListedSearch) => (optionClicked = search);
    component.listItemOptions = [
      {name: 'option1', icon: 'test', onClickCallback},
    ];
    fixture.detectChanges();

    const option = assertDefined(
      htmlElement.querySelector<HTMLElement>('.listed-search-option'),
    );
    UnitTestUtils.checkTooltips([option], ['option1'], fixture);
    option.click();
    expect(optionClicked).toEqual(component.searches[0]);
  });

  it('shows menu', () => {
    component.listItemOptions = [
      {name: 'option1', icon: 'test', menu: component.testTemplate},
    ];
    component.searches = [new ListedSearch('query1', 'name1')];
    fixture.detectChanges();
    const option = assertDefined(
      htmlElement.querySelector<HTMLElement>('.listed-search-option'),
    );
    UnitTestUtils.checkTooltips([option], ['option1'], fixture);
    option.click();
    const menu = assertDefined(
      document.querySelector<HTMLElement>('.context-menu'),
    );
    expect(menu.querySelector('.test-menu-item')).toBeTruthy();
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
