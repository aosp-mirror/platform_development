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
import {
  decode as protobufBase64Decode,
  encode as protobufBase64Encode,
  length as protobufBase64Length,
} from '@protobufjs/base64';
import {assertTrue} from './assert_utils';

/**
 * String utility functions.
 */
export function parseBigIntStrippingUnit(s: string): bigint {
  const match = s.match(/^\s*(-?\d+)\D*.*$/);
  if (!match) {
    throw new Error(`Cannot parse '${s}' as bigint`);
  }
  return BigInt(match[1]);
}

export function convertCamelToSnakeCase(s: string): string {
  const result: string[] = [];

  let prevChar: string | undefined;
  for (const currChar of s) {
    const prevCharCouldBeWordEnd =
      prevChar && (isDigit(prevChar) || isLowerCase(prevChar));
    const currCharCouldBeWordStart = isUpperCase(currChar);
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

export function convertSnakeToCamelCase(s: string): string {
  const tokens = s.split('_').filter((token) => token.length > 0);
  const tokensCapitalized = tokens.map((token) => {
    return capitalizeFirstCharIfAlpha(token);
  });

  const inputStartsWithUnderscore = s[0] === '_';
  let result = inputStartsWithUnderscore ? '_' : '';
  result += tokens[0];
  for (const token of tokensCapitalized.slice(1)) {
    if (!isAlpha(token[0])) {
      result += '_';
    }
    result += token;
  }

  return result;
}

export function isAlpha(char: string): boolean {
  assertTrue(char.length === 1, () => 'Input must be a single character');
  return char[0].toLowerCase() !== char[0].toUpperCase();
}

export function isDigit(char: string): boolean {
  assertTrue(char.length === 1, () => 'Input must be a single character');
  return char >= '0' && char <= '9';
}

export function isLowerCase(char: string): boolean {
  assertTrue(char.length === 1, () => 'Input must be a single character');
  return isAlpha(char) && char === char.toLowerCase();
}

export function isUpperCase(char: string): boolean {
  assertTrue(char.length === 1, () => 'Input must be a single character');
  return isAlpha(char) && char === char.toUpperCase();
}

export function isBlank(str: string): boolean {
  return str.replace(/\s/g, '').length === 0;
}

export function isNumeric(str: string): boolean {
  return Number(str).toString() === str;
}

export function binaryEncode(str: string): Uint8Array {
  const data = new Uint8Array(str.length);
  for (let i = 0; i < str.length; ++i) {
    data[i] = str.charCodeAt(i);
  }
  return data;
}

export function binaryDecode(buf: Uint8Array): string {
  let str = '';
  for (let i = 0; i < buf.length; i++) {
    str += String.fromCharCode(buf[i]);
  }
  return str;
}

export function utf8Encode(data: string): Uint8Array {
  return new TextEncoder().encode(data);
}

export function utf8Decode(data: Uint8Array): string {
  return new TextDecoder('utf-8').decode(data);
}

export function hexEncode(bytes: Uint8Array): string {
  return bytes.reduce(
    (prev, curr) => prev + ('0' + curr.toString(16)).slice(-2),
    '',
  );
}

export function base64Decode(str: string): Uint8Array {
  // if the string is in base64url format, convert to base64
  const b64 = str.replace('-', '+').replace('_', '/');
  const arr = new Uint8Array(protobufBase64Length(b64));
  const written = protobufBase64Decode(b64, arr, 0);
  assertTrue(written === arr.length);
  return arr;
}

export function base64Encode(buffer: Uint8Array): string {
  return protobufBase64Encode(buffer, 0, buffer.length);
}

function capitalizeFirstCharIfAlpha(word: string): string {
  if (word.length === 0) {
    return word;
  }

  if (!isAlpha(word[0])) {
    return word;
  }
  return word[0].toUpperCase() + word.slice(1);
}
