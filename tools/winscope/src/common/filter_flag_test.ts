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

import {FilterFlag, makeFilterPredicate} from './filter_flag';

describe('makeFilterPredicate', () => {
  it('without flags', () => {
    const predicate = makeFilterPredicate('foo', []);
    expect(predicate('foo')).toBeTrue();
    expect(predicate('Foo')).toBeTrue();
    expect(predicate('foobar')).toBeTrue();
    expect(predicate('fo')).toBeFalse();
    expect(predicate('fo o')).toBeFalse();

    // does not interpret regex
    const predicateWithRegexExp = makeFilterPredicate('foo|bar', []);
    expect(predicateWithRegexExp('foo')).toBeFalse();
    expect(predicateWithRegexExp('foo|bar')).toBeTrue();
  });

  it('with MATCH_CASE', () => {
    const predicate = makeFilterPredicate('Foo', [FilterFlag.MATCH_CASE]);
    expect(predicate('Foo')).toBeTrue();
    expect(predicate('Foobar')).toBeTrue();
    expect(predicate('foo')).toBeFalse();
  });

  it('with MATCH_WORD', () => {
    const predicate = makeFilterPredicate('Foo', [FilterFlag.MATCH_WORD]);
    expect(predicate('Foo')).toBeTrue();
    expect(predicate('foo')).toBeTrue();
    expect(predicate('Foo.bar')).toBeTrue();
    expect(predicate('Foo_bar')).toBeTrue();
    expect(predicate('Foo bar')).toBeTrue();
    expect(predicate('Foobar')).toBeFalse();
    expect(predicate('Foo123')).toBeFalse();
  });

  it('with USE_REGEX', () => {
    const predicate = makeFilterPredicate('foo|bar', [FilterFlag.USE_REGEX]);
    expect(predicate('foo')).toBeTrue();
    expect(predicate('bar')).toBeTrue();
    expect(predicate('Foo')).toBeTrue();
    expect(predicate('foobar')).toBeTrue();
    expect(predicate('foo123bar123')).toBeTrue();

    const predicateWithInvalidRegex = makeFilterPredicate('foo|bar)', [
      FilterFlag.USE_REGEX,
    ]);
    expect(predicateWithInvalidRegex('foo')).toBeFalse();
    expect(predicateWithInvalidRegex('bar')).toBeFalse();
    expect(predicateWithInvalidRegex('Foo')).toBeFalse();
    expect(predicateWithInvalidRegex('foobar')).toBeFalse();
    expect(predicateWithInvalidRegex('foo123bar123')).toBeFalse();
  });

  it('with MATCH_CASE and MATCH_WORD', () => {
    const predicate = makeFilterPredicate('foo', [
      FilterFlag.MATCH_CASE,
      FilterFlag.MATCH_WORD,
    ]);
    expect(predicate('foo')).toBeTrue();
    expect(predicate('Foo')).toBeFalse();
    expect(predicate('Foobar')).toBeFalse();
  });

  it('with MATCH_CASE and USE_REGEX', () => {
    const predicate = makeFilterPredicate('foo|bar', [
      FilterFlag.MATCH_CASE,
      FilterFlag.USE_REGEX,
    ]);
    expect(predicate('bar')).toBeTrue();
    expect(predicate('foobar')).toBeTrue();
    expect(predicate('Bar')).toBeFalse();
  });

  it('with MATCH_WORD and USE_REGEX', () => {
    const predicate = makeFilterPredicate('foo|bar', [
      FilterFlag.MATCH_WORD,
      FilterFlag.USE_REGEX,
    ]);
    expect(predicate('foo')).toBeTrue();
    expect(predicate('Bar')).toBeTrue();
    expect(predicate('123 Bar')).toBeTrue();
    expect(predicate('foobar')).toBeFalse();
    expect(predicate('foo123bar123')).toBeFalse();
  });

  it('with MATCH_CASE, MATCH_WORD and USE_REGEX', () => {
    const predicate = makeFilterPredicate('foo|bar', [
      FilterFlag.MATCH_CASE,
      FilterFlag.MATCH_WORD,
      FilterFlag.USE_REGEX,
    ]);
    expect(predicate('foo')).toBeTrue();
    expect(predicate('123 bar')).toBeTrue();

    expect(predicate('foobar')).toBeFalse();
    expect(predicate('Bar')).toBeFalse();
    expect(predicate('123 Bar')).toBeFalse();
  });
});
