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

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {assertDefined} from 'common/assert_utils';
import {FilterFlag} from 'common/filter_flag';
import {TextFilter, TextFilterValues} from 'viewers/common/text_filter';
import {SearchBoxComponent} from './search_box_component';

describe('SearchBoxComponent', () => {
  let fixture: ComponentFixture<SearchBoxComponent>;
  let component: SearchBoxComponent;
  let htmlElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatFormFieldModule,
        MatInputModule,
        FormsModule,
        MatButtonModule,
        BrowserAnimationsModule,
        MatIconModule,
        MatTooltipModule,
      ],
      declarations: [SearchBoxComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(SearchBoxComponent);
    component = fixture.componentInstance;
    htmlElement = fixture.nativeElement;
    component.textFilter = new TextFilter();
    fixture.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('shows custom label', () => {
    const label = htmlElement.querySelector('.search-box mat-label');
    expect(label?.textContent).toEqual('Search');

    component.label = 'custom label';
    fixture.detectChanges();
    expect(label?.textContent).toEqual('custom label');
  });

  it('handles change in filter', () => {
    const spy = spyOn(component.filterChange, 'emit');
    expect(component.textFilter?.values.filterString).toEqual('');
    changeFilterString('Test');
    expect(component.textFilter?.values.filterString).toEqual('Test');
    expect(spy).toHaveBeenCalledWith(
      new TextFilter(new TextFilterValues('Test', [])),
    );
  });

  it('handles change in flags', () => {
    const spy = spyOn(component.filterChange, 'emit');
    const buttons =
      htmlElement.querySelectorAll<HTMLElement>('.search-box button');
    expect(buttons.length).toEqual(3);

    buttons.item(0).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(
      new TextFilter(new TextFilterValues('', [FilterFlag.MATCH_CASE])),
    );

    buttons.item(0).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(new TextFilter());

    buttons.item(2).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(
      new TextFilter(new TextFilterValues('', [FilterFlag.USE_REGEX])),
    );

    buttons.item(1).click();
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(
      new TextFilter(
        new TextFilterValues('', [FilterFlag.USE_REGEX, FilterFlag.MATCH_WORD]),
      ),
    );
  });

  function changeFilterString(
    newString: string,
    el = htmlElement,
    f = fixture,
  ) {
    const inputEl = assertDefined(
      el.querySelector<HTMLInputElement>('.search-box input'),
    );
    inputEl.value = newString;
    inputEl.dispatchEvent(new Event('input'));
    f.detectChanges();
  }
});
