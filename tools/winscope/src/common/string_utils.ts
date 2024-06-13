/*
 * Copyright (C) 2023 The Android Open Source Project
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
import {assertTrue} from './assert_utils';

class StringUtils {
  static parseBigIntStrippingUnit(s: string): bigint {
    const match = s.match(/^\s*(-?\d+)\D*.*$/);
    if (!match) {
      throw new Error(`Cannot parse '${s}' as bigint`);
    }
    return BigInt(match[1]);
  }

  static convertCamelToSnakeCase(s: string): string {
    const result: string[] = [];

    let prevChar: string | undefined;
    for (const currChar of s) {
      const prevCharCouldBeWordEnd =
        prevChar &&
        (StringUtils.isDigit(prevChar) || StringUtils.isLowerCase(prevChar));
      const currCharCouldBeWordStart = StringUtils.isUpperCase(currChar);
      if (prevCharCouldBeWordEnd && currCharCouldBeWordStart) {
        result.push('_');
        result.push(currChar.toLowerCase());
      } else {
        result.push(currChar);
      }
      prevChar = currChar;
    }

    return result.join('');
  }

  static convertSnakeToCamelCase(s: string): string {
    const tokens = s.split('_').filter((token) => token.length > 0);
    const tokensCapitalized = tokens.map((token) => {
      return StringUtils.capitalizeFirstCharIfAlpha(token);
    });

    const inputStartsWithUnderscore = s[0] === '_';
    let result = inputStartsWithUnderscore ? '_' : '';
    result += tokens[0];
    for (const token of tokensCapitalized.slice(1)) {
      if (!StringUtils.isAlpha(token[0])) {
        result += '_';
      }
      result += token;
    }

    return result;
  }

  static isAlpha(char: string): boolean {
    assertTrue(char.length === 1, () => 'Input must be a single character');
    return char[0].toLowerCase() !== char[0].toUpperCase();
  }

  static isDigit(char: string): boolean {
    assertTrue(char.length === 1, () => 'Input must be a single character');
    return char >= '0' && char <= '9';
  }

  static isLowerCase(char: string): boolean {
    assertTrue(char.length === 1, () => 'Input must be a single character');
    return StringUtils.isAlpha(char) && char === char.toLowerCase();
  }

  static isUpperCase(char: string): boolean {
    assertTrue(char.length === 1, () => 'Input must be a single character');
    return StringUtils.isAlpha(char) && char === char.toUpperCase();
  }

  static isBlank(str: string): boolean {
    return str.replace(/\s/g, '').length === 0;
  }

  static isNumeric(str: string): boolean {
    return Number(str).toString() === str;
  }

  private static capitalizeFirstCharIfAlpha(word: string): string {
    if (word.length === 0) {
      return word;
    }

    if (!StringUtils.isAlpha(word[0])) {
      return word;
    }
    return word[0].toUpperCase() + word.slice(1);
  }
}

export {StringUtils};
