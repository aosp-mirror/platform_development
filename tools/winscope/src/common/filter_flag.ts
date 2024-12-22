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

import {StringUtils} from './string_utils';

export enum FilterFlag {
  MATCH_CASE,
  MATCH_WORD,
  USE_REGEX,
}

export type StringFilterPredicate = (value: string) => boolean;

export function makeFilterPredicate(
  filterString: string,
  flags: FilterFlag[],
): StringFilterPredicate {
  const matchCase = flags.includes(FilterFlag.MATCH_CASE);
  const matchWord = flags.includes(FilterFlag.MATCH_WORD);
  const useRegex = flags.includes(FilterFlag.USE_REGEX);

  let filter: StringFilterPredicate;
  if (useRegex) {
    const regexFlags = useRegex && !matchCase ? 'i' : '';
    const regexString = matchWord
      ? '\\b(?:' + filterString + ')\\b'
      : filterString;
    try {
      const regex = new RegExp(regexString, regexFlags);
      filter = (entryString: string) => {
        if (filterString.length === 0) return true;
        return regex.test(entryString);
      };
    } catch (e) {
      filter = (entryString: string) => false;
    }
  } else {
    const testString = matchCase ? filterString : filterString.toLowerCase();
    filter = (entryString: string) => {
      if (filterString.length === 0) return true;

      let entrySubstring = matchCase ? entryString : entryString.toLowerCase();
      let testStringIndex = entrySubstring.indexOf(testString);

      while (testStringIndex !== -1) {
        if (!matchWord) return true;

        const nextChar = entrySubstring.at(testStringIndex + testString.length);
        if (
          nextChar === undefined ||
          !(StringUtils.isAlpha(nextChar) || StringUtils.isDigit(nextChar))
        ) {
          return true;
        }

        entrySubstring = entrySubstring.slice(
          testStringIndex + testString.length,
        );
        testStringIndex = entrySubstring.indexOf(testString);
      }
      return false;
    };
  }
  return filter;
}
