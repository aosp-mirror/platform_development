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

import {TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTooltipModule} from '@angular/material/tooltip';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {FilterFlag} from 'common/filter_flag';
import {DOMTestHelper} from 'test/unit/dom_test_utils';
import {TextFilter} from 'viewers/common/text_filter';
import {SearchBoxComponent} from './search_box_component';

describe('SearchBoxComponent', () => {
  let component: SearchBoxComponent;
  let dom: DOMTestHelper<SearchBoxComponent>;

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
    const fixture = TestBed.createComponent(SearchBoxComponent);
    component = fixture.componentInstance;
    dom = new DOMTestHelper(fixture, fixture.nativeElement);
    component.textFilter = new TextFilter();
    dom.detectChanges();
  });

  it('can be created', () => {
    expect(component).toBeTruthy();
  });

  it('shows custom label', () => {
    const label = dom.get('.search-box mat-label');
    label.checkTextExact('Search');
    component.label = 'custom label';
    dom.detectChanges();
    label.checkTextExact('custom label');
  });

  it('handles change in filter', () => {
    const spy = spyOn(component.filterChange, 'emit');
    expect(component.textFilter?.filterString).toEqual('');
    expect(dom.find('.highlighted')).toBeUndefined();
    dom.findAndDispatchInput('.search-box', 'Test');
    expect(component.textFilter?.filterString).toEqual('Test');
    expect(spy).toHaveBeenCalledWith(new TextFilter('Test'));
    expect(dom.find('.highlighted')).toBeDefined();
  });

  it('handles change in flags', () => {
    const spy = spyOn(component.filterChange, 'emit');
    const buttons = dom.findAll('.search-box button');
    expect(buttons.length).toEqual(3);

    buttons[0].click();
    expect(spy).toHaveBeenCalledWith(
      new TextFilter('', [FilterFlag.MATCH_CASE]),
    );

    buttons[0].click();
    expect(spy).toHaveBeenCalledWith(new TextFilter());

    buttons[2].click();
    expect(spy).toHaveBeenCalledWith(
      new TextFilter('', [FilterFlag.USE_REGEX]),
    );

    buttons[1].click();
    expect(spy).toHaveBeenCalledWith(
      new TextFilter('', [FilterFlag.USE_REGEX, FilterFlag.MATCH_WORD]),
    );
  });
});
