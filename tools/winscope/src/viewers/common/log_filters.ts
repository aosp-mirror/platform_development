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

import {StringFilterPredicate} from 'viewers/common/string_filter_predicate';
import {TextFilter} from 'viewers/common/text_filter';

export abstract class LogFilter {
  constructor(
    public innerFilterWidthCss: string,
    public outerFilterWidthCss: string,
  ) {}

  abstract updateFilterValue(value: string[]): void;
  abstract getFilterPredicate(): StringFilterPredicate;
}

export class LogTextFilter extends LogFilter {
  constructor(
    public textFilter: TextFilter,
    innerFilterWidthCss = '100',
    outerFilterWidthCss = '100%',
  ) {
    super(innerFilterWidthCss, outerFilterWidthCss);
  }

  override updateFilterValue(value: string[]): void {
    this.textFilter.filterString = value.at(0) ?? '';
  }

  override getFilterPredicate(): StringFilterPredicate {
    return this.textFilter.getFilterPredicate();
  }
}

export class LogSelectFilter extends LogFilter {
  private filterValue: string[] = [];
  private readonly filterPredicate: StringFilterPredicate;

  constructor(
    public options: string[],
    readonly shouldFilterBySubstring = false,
    innerFilterWidthCss = '100',
    outerFilterWidthCss = '100%',
  ) {
    super(innerFilterWidthCss, outerFilterWidthCss);

    if (this.shouldFilterBySubstring) {
      this.filterPredicate = (entryString) => {
        if (this.filterValue.length === 0) return true;
        return this.filterValue.some((val) => entryString.includes(val));
      };
    } else {
      this.filterPredicate = (entryString) => {
        if (this.filterValue.length === 0) return true;
        return this.filterValue.includes(entryString);
      };
    }
  }

  override updateFilterValue(value: string[]) {
    this.filterValue = value;
  }

  override getFilterPredicate(): StringFilterPredicate {
    return this.filterPredicate;
  }
}
