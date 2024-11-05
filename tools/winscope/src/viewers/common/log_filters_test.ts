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

import {TextFilter} from 'viewers/common/text_filter';
import {LogSelectFilter, LogTextFilter} from './log_filters';

describe('LogFilters', () => {
  it('text filter filters by substring', () => {
    const filter = new LogTextFilter(new TextFilter());
    let predicate = filter.getFilterPredicate();
    expect(predicate('test')).toBeTrue();

    filter.updateFilterValue(['match']);
    predicate = filter.getFilterPredicate();
    expect(predicate('test')).toBeFalse();
    expect(predicate('match')).toBeTrue();
    expect(predicate('test match')).toBeTrue();

    filter.updateFilterValue([]);
    predicate = filter.getFilterPredicate();
    expect(predicate('test')).toBeTrue();
  });

  it('select filter filters by exact match', () => {
    const filter = new LogSelectFilter([]);
    let predicate = filter.getFilterPredicate();
    expect(predicate('test')).toBeTrue();

    filter.updateFilterValue(['match']);
    predicate = filter.getFilterPredicate();
    expect(predicate('test')).toBeFalse();
    expect(predicate('match')).toBeTrue();
    expect(predicate('test match')).toBeFalse();

    filter.updateFilterValue([]);
    predicate = filter.getFilterPredicate();
    expect(predicate('test')).toBeTrue();
  });

  it('select filter filters by substring', () => {
    const filter = new LogSelectFilter([], true);
    filter.updateFilterValue(['match']);
    const predicate = filter.getFilterPredicate();
    expect(predicate('test')).toBeFalse();
    expect(predicate('match')).toBeTrue();
    expect(predicate('test match')).toBeTrue();
  });
});
