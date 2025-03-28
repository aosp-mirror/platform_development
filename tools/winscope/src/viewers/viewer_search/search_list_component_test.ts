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
import {MenuOption, SearchListComponent} from './search_list_component';
import {Search} from './ui_data';

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

  it('shows search names with tooltips', () => {
    component.searches = [
      new Search('query1', 'name1'),
      new Search('query2', 'query2'),
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
    queryName1.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    expect(
      assertDefined(
        document.querySelector<HTMLElement>('.mat-tooltip-panel'),
      ).textContent?.trim(),
    ).toEqual('name1: query1');
    queryName1.dispatchEvent(new Event('mouseleave'));
    fixture.detectChanges();

    // does not show tooltip when name and query are the same
    queryName2.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    expect(
      document.querySelector<HTMLElement>('.mat-tooltip-panel'),
    ).toBeNull();

    // shows tooltip when element is overflowing
    queryName2.style.maxWidth = queryName2.offsetWidth / 2 + 'px';
    fixture.detectChanges();
    queryName2.dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    expect(
      assertDefined(
        document.querySelector<HTMLElement>('.mat-tooltip-panel'),
      ).textContent?.trim(),
    ).toEqual('query2');
  });

  it('formats search dates', () => {
    spyOn(Date, 'now').and.returnValue(0);
    component.searches = [new Search('query1', 'name1')];
    fixture.detectChanges();
    expect(
      htmlElement
        .querySelector('.listed-search-date-options')
        ?.textContent?.trim(),
    ).toEqual('01:00\n1/1/1970');
  });

  it('shows menu options and triggers callback on interaction', () => {
    let optionClicked: Search | undefined;
    component.searches = [new Search('query1', 'name1')];
    fixture.detectChanges();
    // does not show menu button if no options
    expect(htmlElement.querySelector('.listed-search-options')).toBeNull();

    const onClickCallback = (search: Search) => (optionClicked = search);
    component.menuOptions = [{name: 'option1', onClickCallback}];
    fixture.detectChanges();
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.listed-search-options'),
    ).click();
    const option = assertDefined(
      document.querySelector<HTMLElement>('.context-menu .context-menu-item'),
    );
    expect(option.textContent?.trim()).toEqual('option1');
    option.click();
    expect(optionClicked).toEqual(component.searches[0]);
  });

  it('shows inner menu', () => {
    let clickedSearch: Search | undefined;
    component.menuOptions = [
      {
        name: 'option1',
        onClickCallback: (search: Search) => (clickedSearch = search),
        innerMenu: component.testTemplate,
      },
    ];
    component.searches = [new Search('query1', 'name1')];
    fixture.detectChanges();
    assertDefined(
      htmlElement.querySelector<HTMLElement>('.listed-search-options'),
    ).click();

    const option = assertDefined(
      document.querySelector<HTMLElement>('.context-menu .context-menu-item'),
    );
    expect(option.textContent?.trim()).toEqual('option1');
    option.dispatchEvent(new MouseEvent('mouseenter'));

    const innerMenu = assertDefined(document.querySelector('.inner-menu'));
    expect(innerMenu.querySelector('.inner-menu-item')).toBeTruthy();
  });

  @Component({
    selector: 'host-component',
    template: `
      <search-list
        [searches]="searches"
        [placeholderText]="placeholderText"
        [menuOptions]="menuOptions"></search-list>

      <ng-template #testTemplate>
        <span class="inner-menu-item"></span>
      </ng-template>
    `,
  })
  class TestHostComponent {
    @ViewChild(SearchListComponent) searchListComponent:
      | SearchListComponent
      | undefined;
    @ViewChild('testTemplate') testTemplate: NgTemplateOutlet | undefined;

    searches: Search[] = [];
    placeholderText: string | undefined;
    menuOptions: MenuOption[] = [];
  }
});
