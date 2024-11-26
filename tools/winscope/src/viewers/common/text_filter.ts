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

import {FilterFlag} from 'common/filter_flag';
import {StringUtils} from 'common/string_utils';
import {StringFilterPredicate} from 'viewers/common/string_filter_predicate';

export class TextFilter {
  constructor(
    public filterString: string = '',
    public flags: FilterFlag[] = [],
  ) {}

  getFilterPredicate(): StringFilterPredicate {
    const matchCase = this.flags.includes(FilterFlag.MATCH_CASE);
    const matchWord = this.flags.includes(FilterFlag.MATCH_WORD);
    const useRegex = this.flags.includes(FilterFlag.USE_REGEX);

    if (useRegex) {
      const regexFlags = useRegex && !matchCase ? 'i' : '';
      const regexString = matchWord
        ? '\\b(?:' + this.filterString + ')\\b'
        : this.filterString;
      try {
        const regex = new RegExp(regexString, regexFlags);
        return (entryString: string) => {
          if (this.filterString.length === 0) return true;
          return regex.test(entryString);
        };
      } catch (e) {
        return (entryString: string) => false;
      }
    } else {
      const testString = matchCase
        ? this.filterString
        : this.filterString.toLowerCase();
      return (entryString: string) => {
        if (this.filterString.length === 0) return true;

        let entrySubstring = matchCase
          ? entryString
          : entryString.toLowerCase();
        let testStringIndex = entrySubstring.indexOf(testString);

        while (testStringIndex !== -1) {
          if (!matchWord) return true;

          const nextChar = entrySubstring.at(
            testStringIndex + testString.length,
          );
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
  }
}
