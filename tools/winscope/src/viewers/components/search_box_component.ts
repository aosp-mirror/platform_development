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

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {assertDefined} from 'common/assert_utils';
import {FilterFlag} from 'common/filter_flag';
import {TextFilter} from 'viewers/common/text_filter';

@Component({
  selector: 'search-box',
  template: `
    <mat-form-field
      *ngIf="textFilter"
      [class]="getFormFieldClasses()"
      [appearance]="appearance"
      [style.height]="height"
      (keydown.esc)="$event.target.blur()"
      (keydown.enter)="$event.target.blur()">
      <mat-label>{{ label }}</mat-label>
      <input
        matInput
        [(ngModel)]="textFilter.filterString"
        (ngModelChange)="onFilterChange()"
        [name]="filterName" />
      <div class="field-suffix" matSuffix>
        <button
          mat-icon-button
          matTooltip="Match case"
          [color]="hasFlag(FilterFlag.MATCH_CASE) ? 'primary' : undefined"
          (click)="onFilterFlagClick($event, FilterFlag.MATCH_CASE)">
          <mat-icon class="material-symbols-outlined">match_case</mat-icon>
        </button>
        <button
          mat-icon-button
          matTooltip="Match whole word"
          [color]="hasFlag(FilterFlag.MATCH_WORD) ? 'primary' : undefined"
          (click)="onFilterFlagClick($event, FilterFlag.MATCH_WORD)">
          <mat-icon class="material-symbols-outlined">match_word</mat-icon>
        </button>
        <button
          mat-icon-button
          matTooltip="Use regex"
          [color]="hasFlag(FilterFlag.USE_REGEX) ? 'primary' : undefined"
          (click)="onFilterFlagClick($event, FilterFlag.USE_REGEX)">
          <mat-icon class="material-symbols-outlined">regular_expression</mat-icon>
        </button>
      </div>
    </mat-form-field>
  `,
  styles: [
    `
    .search-box {
      font-size: 14px;
      margin-top: 4px;
    }
    .search-box .mat-icon {
      font-size: 18px;
    }
    .wide-field {
      width: 100%;
    }
  `,
  ],
})
export class SearchBoxComponent {
  FilterFlag = FilterFlag;

  @Input() textFilter: TextFilter | undefined = new TextFilter();
  @Input() label = 'Search';
  @Input() filterName = 'filter';
  @Input() appearance = '';
  @Input() formFieldClass = '';
  @Input() height = '48px';

  @Output() readonly filterChange = new EventEmitter<TextFilter>();

  hasFlag(flag: FilterFlag): boolean {
    return assertDefined(this.textFilter).flags.includes(flag) ?? false;
  }

  onFilterFlagClick(event: MouseEvent, flag: FilterFlag) {
    event.stopPropagation();
    const filter = assertDefined(this.textFilter);
    if (this.hasFlag(flag)) {
      filter.flags = filter.flags.filter((f) => f !== flag);
    } else {
      filter.flags = filter.flags.concat(flag);
    }
    this.onFilterChange();
  }

  onFilterChange() {
    this.filterChange.emit(this.textFilter);
  }

  getFormFieldClasses(): string {
    return (
      'search-box ' +
      ((this.textFilter?.filterString.length ?? 0) > 0 ? 'highlighted ' : '') +
      this.formFieldClass
    );
  }
}
