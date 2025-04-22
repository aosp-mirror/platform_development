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
  convertCamelToSnakeCase,
  convertSnakeToCamelCase,
  isAlpha,
  isBlank,
  isDigit,
  isLowerCase,
  isNumeric,
  isUpperCase,
  parseBigIntStrippingUnit,
} from './string_utils';

describe('StringUtils', () => {
  it('parses bigint', () => {
    expect(parseBigIntStrippingUnit('-10')).toEqual(-10n);
    expect(parseBigIntStrippingUnit('-10 unit')).toEqual(-10n);
    expect(parseBigIntStrippingUnit('-10unit')).toEqual(-10n);
    expect(parseBigIntStrippingUnit(' -10 unit ')).toEqual(-10n);

    expect(parseBigIntStrippingUnit('0')).toEqual(0n);
    expect(parseBigIntStrippingUnit('0 unit')).toEqual(0n);
    expect(parseBigIntStrippingUnit('0unit')).toEqual(0n);
    expect(parseBigIntStrippingUnit(' 0 unit ')).toEqual(0n);

    expect(parseBigIntStrippingUnit('10')).toEqual(10n);
    expect(parseBigIntStrippingUnit('10 unit')).toEqual(10n);
    expect(parseBigIntStrippingUnit('10unit')).toEqual(10n);
    expect(parseBigIntStrippingUnit(' 10 unit ')).toEqual(10n);

    expect(() => parseBigIntStrippingUnit('invalid')).toThrow();
    expect(() => parseBigIntStrippingUnit('invalid 10 unit')).toThrow();
  });

  it('convertCamelToSnakeCase()', () => {
    expect(convertCamelToSnakeCase('aaa')).toEqual('aaa');
    expect(convertCamelToSnakeCase('Aaa')).toEqual('Aaa');
    expect(convertCamelToSnakeCase('_aaa')).toEqual('_aaa');
    expect(convertCamelToSnakeCase('_Aaa')).toEqual('_Aaa');

    expect(convertCamelToSnakeCase('aaaBbb')).toEqual('aaa_bbb');
    expect(convertCamelToSnakeCase('AaaBbb')).toEqual('Aaa_bbb');
    expect(convertCamelToSnakeCase('aaa_bbb')).toEqual('aaa_bbb');
    expect(convertCamelToSnakeCase('aaa_Bbb')).toEqual('aaa_Bbb');

    expect(convertCamelToSnakeCase('aaaBbbCcc')).toEqual('aaa_bbb_ccc');
    expect(convertCamelToSnakeCase('aaaBbb_ccc')).toEqual('aaa_bbb_ccc');
    expect(convertCamelToSnakeCase('aaaBbb_Ccc')).toEqual('aaa_bbb_Ccc');

    expect(convertCamelToSnakeCase('aaaBBBccc')).toEqual('aaa_bBBccc');
    expect(convertCamelToSnakeCase('aaaBBBcccDDD')).toEqual('aaa_bBBccc_dDD');
    expect(convertCamelToSnakeCase('aaaBBB_ccc')).toEqual('aaa_bBB_ccc');
    expect(convertCamelToSnakeCase('aaaBbb_CCC')).toEqual('aaa_bbb_CCC');

    expect(convertCamelToSnakeCase('_field_32')).toEqual('_field_32');
    expect(convertCamelToSnakeCase('field_32')).toEqual('field_32');
    expect(convertCamelToSnakeCase('field_32Bits')).toEqual('field_32_bits');
    expect(convertCamelToSnakeCase('field_32BitsLsb')).toEqual(
      'field_32_bits_lsb',
    );
    expect(convertCamelToSnakeCase('field_32bits')).toEqual('field_32bits');
    expect(convertCamelToSnakeCase('field_32bitsLsb')).toEqual(
      'field_32bits_lsb',
    );

    expect(convertCamelToSnakeCase('_aaaAaa.bbbBbb')).toEqual(
      '_aaa_aaa.bbb_bbb',
    );
    expect(convertCamelToSnakeCase('aaaAaa.bbbBbb')).toEqual('aaa_aaa.bbb_bbb');
    expect(convertCamelToSnakeCase('aaaAaa.field_32bitsLsb.bbbBbb')).toEqual(
      'aaa_aaa.field_32bits_lsb.bbb_bbb',
    );
  });

  it('convertSnakeToCamelCase()', () => {
    expect(convertSnakeToCamelCase('_aaa')).toEqual('_aaa');
    expect(convertSnakeToCamelCase('aaa')).toEqual('aaa');

    expect(convertSnakeToCamelCase('aaa_bbb')).toEqual('aaaBbb');
    expect(convertSnakeToCamelCase('_aaa_bbb')).toEqual('_aaaBbb');

    expect(convertSnakeToCamelCase('aaa_bbb_ccc')).toEqual('aaaBbbCcc');
    expect(convertSnakeToCamelCase('_aaa_bbb_ccc')).toEqual('_aaaBbbCcc');

    expect(convertSnakeToCamelCase('_field_32')).toEqual('_field_32');
    expect(convertSnakeToCamelCase('field_32')).toEqual('field_32');
    expect(convertSnakeToCamelCase('field_32_bits')).toEqual('field_32Bits');
    expect(convertSnakeToCamelCase('field_32_bits_lsb')).toEqual(
      'field_32BitsLsb',
    );
    expect(convertSnakeToCamelCase('field_32bits')).toEqual('field_32bits');
    expect(convertSnakeToCamelCase('field_32bits_lsb')).toEqual(
      'field_32bitsLsb',
    );

    expect(convertSnakeToCamelCase('_aaa_aaa.bbb_bbb')).toEqual(
      '_aaaAaa.bbbBbb',
    );
    expect(convertSnakeToCamelCase('aaa_aaa.bbb_bbb')).toEqual('aaaAaa.bbbBbb');
    expect(convertSnakeToCamelCase('aaa_aaa.field_32bits_lsb.bbb_bbb')).toEqual(
      'aaaAaa.field_32bitsLsb.bbbBbb',
    );
  });

  it('isAlpha()', () => {
    expect(isAlpha('a')).toBeTrue();
    expect(isAlpha('A')).toBeTrue();
    expect(isAlpha('_')).toBeFalse();
    expect(isAlpha('0')).toBeFalse();
    expect(isAlpha('9')).toBeFalse();
  });

  it('isDigit()', () => {
    expect(isDigit('a')).toBeFalse();
    expect(isDigit('A')).toBeFalse();
    expect(isDigit('_')).toBeFalse();
    expect(isDigit('0')).toBeTrue();
    expect(isDigit('9')).toBeTrue();
  });

  it('isLowerCase()', () => {
    expect(isLowerCase('a')).toBeTrue();
    expect(isLowerCase('z')).toBeTrue();
    expect(isLowerCase('A')).toBeFalse();
    expect(isLowerCase('Z')).toBeFalse();
    expect(isLowerCase('_')).toBeFalse();
    expect(isLowerCase('0')).toBeFalse();
    expect(isLowerCase('9')).toBeFalse();
  });

  it('isUpperCase()', () => {
    expect(isUpperCase('A')).toBeTrue();
    expect(isUpperCase('Z')).toBeTrue();
    expect(isUpperCase('a')).toBeFalse();
    expect(isUpperCase('z')).toBeFalse();
    expect(isUpperCase('_')).toBeFalse();
    expect(isUpperCase('0')).toBeFalse();
    expect(isUpperCase('9')).toBeFalse();
  });

  it('isBlank()', () => {
    expect(isBlank('')).toBeTrue();
    expect(isBlank(' ')).toBeTrue();
    expect(isBlank('  ')).toBeTrue();
    expect(isBlank(' a')).toBeFalse();
    expect(isBlank('a ')).toBeFalse();
    expect(isBlank(' a ')).toBeFalse();
    expect(isBlank('a  a')).toBeFalse();
  });

  it('isNumeric()', () => {
    expect(isNumeric('0')).toBeTrue();
    expect(isNumeric('1')).toBeTrue();
    expect(isNumeric('0.1')).toBeTrue();
    expect(isNumeric('')).toBeFalse();
    expect(isNumeric('a')).toBeFalse();
    expect(isNumeric('4n')).toBeFalse();
  });
});
