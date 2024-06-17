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

import {StringUtils} from './string_utils';

describe('StringUtils', () => {
  it('parses bigint', () => {
    expect(StringUtils.parseBigIntStrippingUnit('-10')).toEqual(-10n);
    expect(StringUtils.parseBigIntStrippingUnit('-10 unit')).toEqual(-10n);
    expect(StringUtils.parseBigIntStrippingUnit('-10unit')).toEqual(-10n);
    expect(StringUtils.parseBigIntStrippingUnit(' -10 unit ')).toEqual(-10n);

    expect(StringUtils.parseBigIntStrippingUnit('0')).toEqual(0n);
    expect(StringUtils.parseBigIntStrippingUnit('0 unit')).toEqual(0n);
    expect(StringUtils.parseBigIntStrippingUnit('0unit')).toEqual(0n);
    expect(StringUtils.parseBigIntStrippingUnit(' 0 unit ')).toEqual(0n);

    expect(StringUtils.parseBigIntStrippingUnit('10')).toEqual(10n);
    expect(StringUtils.parseBigIntStrippingUnit('10 unit')).toEqual(10n);
    expect(StringUtils.parseBigIntStrippingUnit('10unit')).toEqual(10n);
    expect(StringUtils.parseBigIntStrippingUnit(' 10 unit ')).toEqual(10n);

    expect(() => StringUtils.parseBigIntStrippingUnit('invalid')).toThrow();
    expect(() =>
      StringUtils.parseBigIntStrippingUnit('invalid 10 unit'),
    ).toThrow();
  });

  it('convertCamelToSnakeCase()', () => {
    expect(StringUtils.convertCamelToSnakeCase('aaa')).toEqual('aaa');
    expect(StringUtils.convertCamelToSnakeCase('Aaa')).toEqual('Aaa');
    expect(StringUtils.convertCamelToSnakeCase('_aaa')).toEqual('_aaa');
    expect(StringUtils.convertCamelToSnakeCase('_Aaa')).toEqual('_Aaa');

    expect(StringUtils.convertCamelToSnakeCase('aaaBbb')).toEqual('aaa_bbb');
    expect(StringUtils.convertCamelToSnakeCase('AaaBbb')).toEqual('Aaa_bbb');
    expect(StringUtils.convertCamelToSnakeCase('aaa_bbb')).toEqual('aaa_bbb');
    expect(StringUtils.convertCamelToSnakeCase('aaa_Bbb')).toEqual('aaa_Bbb');

    expect(StringUtils.convertCamelToSnakeCase('aaaBbbCcc')).toEqual(
      'aaa_bbb_ccc',
    );
    expect(StringUtils.convertCamelToSnakeCase('aaaBbb_ccc')).toEqual(
      'aaa_bbb_ccc',
    );
    expect(StringUtils.convertCamelToSnakeCase('aaaBbb_Ccc')).toEqual(
      'aaa_bbb_Ccc',
    );

    expect(StringUtils.convertCamelToSnakeCase('aaaBBBccc')).toEqual(
      'aaa_bBBccc',
    );
    expect(StringUtils.convertCamelToSnakeCase('aaaBBBcccDDD')).toEqual(
      'aaa_bBBccc_dDD',
    );
    expect(StringUtils.convertCamelToSnakeCase('aaaBBB_ccc')).toEqual(
      'aaa_bBB_ccc',
    );
    expect(StringUtils.convertCamelToSnakeCase('aaaBbb_CCC')).toEqual(
      'aaa_bbb_CCC',
    );

    expect(StringUtils.convertCamelToSnakeCase('_field_32')).toEqual(
      '_field_32',
    );
    expect(StringUtils.convertCamelToSnakeCase('field_32')).toEqual('field_32');
    expect(StringUtils.convertCamelToSnakeCase('field_32Bits')).toEqual(
      'field_32_bits',
    );
    expect(StringUtils.convertCamelToSnakeCase('field_32BitsLsb')).toEqual(
      'field_32_bits_lsb',
    );
    expect(StringUtils.convertCamelToSnakeCase('field_32bits')).toEqual(
      'field_32bits',
    );
    expect(StringUtils.convertCamelToSnakeCase('field_32bitsLsb')).toEqual(
      'field_32bits_lsb',
    );

    expect(StringUtils.convertCamelToSnakeCase('_aaaAaa.bbbBbb')).toEqual(
      '_aaa_aaa.bbb_bbb',
    );
    expect(StringUtils.convertCamelToSnakeCase('aaaAaa.bbbBbb')).toEqual(
      'aaa_aaa.bbb_bbb',
    );
    expect(
      StringUtils.convertCamelToSnakeCase('aaaAaa.field_32bitsLsb.bbbBbb'),
    ).toEqual('aaa_aaa.field_32bits_lsb.bbb_bbb');
  });

  it('convertSnakeToCamelCase()', () => {
    expect(StringUtils.convertSnakeToCamelCase('_aaa')).toEqual('_aaa');
    expect(StringUtils.convertSnakeToCamelCase('aaa')).toEqual('aaa');

    expect(StringUtils.convertSnakeToCamelCase('aaa_bbb')).toEqual('aaaBbb');
    expect(StringUtils.convertSnakeToCamelCase('_aaa_bbb')).toEqual('_aaaBbb');

    expect(StringUtils.convertSnakeToCamelCase('aaa_bbb_ccc')).toEqual(
      'aaaBbbCcc',
    );
    expect(StringUtils.convertSnakeToCamelCase('_aaa_bbb_ccc')).toEqual(
      '_aaaBbbCcc',
    );

    expect(StringUtils.convertSnakeToCamelCase('_field_32')).toEqual(
      '_field_32',
    );
    expect(StringUtils.convertSnakeToCamelCase('field_32')).toEqual('field_32');
    expect(StringUtils.convertSnakeToCamelCase('field_32_bits')).toEqual(
      'field_32Bits',
    );
    expect(StringUtils.convertSnakeToCamelCase('field_32_bits_lsb')).toEqual(
      'field_32BitsLsb',
    );
    expect(StringUtils.convertSnakeToCamelCase('field_32bits')).toEqual(
      'field_32bits',
    );
    expect(StringUtils.convertSnakeToCamelCase('field_32bits_lsb')).toEqual(
      'field_32bitsLsb',
    );

    expect(StringUtils.convertSnakeToCamelCase('_aaa_aaa.bbb_bbb')).toEqual(
      '_aaaAaa.bbbBbb',
    );
    expect(StringUtils.convertSnakeToCamelCase('aaa_aaa.bbb_bbb')).toEqual(
      'aaaAaa.bbbBbb',
    );
    expect(
      StringUtils.convertSnakeToCamelCase('aaa_aaa.field_32bits_lsb.bbb_bbb'),
    ).toEqual('aaaAaa.field_32bitsLsb.bbbBbb');
  });

  it('isAlpha()', () => {
    expect(StringUtils.isAlpha('a')).toBeTrue();
    expect(StringUtils.isAlpha('A')).toBeTrue();
    expect(StringUtils.isAlpha('_')).toBeFalse();
    expect(StringUtils.isAlpha('0')).toBeFalse();
    expect(StringUtils.isAlpha('9')).toBeFalse();
  });

  it('isDigit()', () => {
    expect(StringUtils.isDigit('a')).toBeFalse();
    expect(StringUtils.isDigit('A')).toBeFalse();
    expect(StringUtils.isDigit('_')).toBeFalse();
    expect(StringUtils.isDigit('0')).toBeTrue();
    expect(StringUtils.isDigit('9')).toBeTrue();
  });

  it('isLowerCase()', () => {
    expect(StringUtils.isLowerCase('a')).toBeTrue();
    expect(StringUtils.isLowerCase('z')).toBeTrue();
    expect(StringUtils.isLowerCase('A')).toBeFalse();
    expect(StringUtils.isLowerCase('Z')).toBeFalse();
    expect(StringUtils.isLowerCase('_')).toBeFalse();
    expect(StringUtils.isLowerCase('0')).toBeFalse();
    expect(StringUtils.isLowerCase('9')).toBeFalse();
  });

  it('isUpperCase()', () => {
    expect(StringUtils.isUpperCase('A')).toBeTrue();
    expect(StringUtils.isUpperCase('Z')).toBeTrue();
    expect(StringUtils.isUpperCase('a')).toBeFalse();
    expect(StringUtils.isUpperCase('z')).toBeFalse();
    expect(StringUtils.isUpperCase('_')).toBeFalse();
    expect(StringUtils.isUpperCase('0')).toBeFalse();
    expect(StringUtils.isUpperCase('9')).toBeFalse();
  });

  it('isBlank()', () => {
    expect(StringUtils.isBlank('')).toBeTrue();
    expect(StringUtils.isBlank(' ')).toBeTrue();
    expect(StringUtils.isBlank('  ')).toBeTrue();
    expect(StringUtils.isBlank(' a')).toBeFalse();
    expect(StringUtils.isBlank('a ')).toBeFalse();
    expect(StringUtils.isBlank(' a ')).toBeFalse();
    expect(StringUtils.isBlank('a  a')).toBeFalse();
  });

  it('isNumeric()', () => {
    expect(StringUtils.isNumeric('0')).toBeTrue();
    expect(StringUtils.isNumeric('1')).toBeTrue();
    expect(StringUtils.isNumeric('0.1')).toBeTrue();
    expect(StringUtils.isNumeric('')).toBeFalse();
    expect(StringUtils.isNumeric('a')).toBeFalse();
    expect(StringUtils.isNumeric('4n')).toBeFalse();
  });
});
