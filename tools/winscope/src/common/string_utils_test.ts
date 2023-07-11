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
    expect(() => StringUtils.parseBigIntStrippingUnit('invalid 10 unit')).toThrow();
  });

  it('convertSnakeToCamelCase()', () => {
    expect(StringUtils.convertSnakeToCamelCase('_aaa')).toEqual('_aaa');
    expect(StringUtils.convertSnakeToCamelCase('aaa')).toEqual('aaa');

    expect(StringUtils.convertSnakeToCamelCase('aaa_bbb')).toEqual('aaaBbb');
    expect(StringUtils.convertSnakeToCamelCase('_aaa_bbb')).toEqual('_aaaBbb');

    expect(StringUtils.convertSnakeToCamelCase('aaa_bbb_ccc')).toEqual('aaaBbbCcc');
    expect(StringUtils.convertSnakeToCamelCase('_aaa_bbb_ccc')).toEqual('_aaaBbbCcc');

    expect(StringUtils.convertSnakeToCamelCase('_field_32')).toEqual('_field_32');
    expect(StringUtils.convertSnakeToCamelCase('field_32')).toEqual('field_32');
    expect(StringUtils.convertSnakeToCamelCase('field_32_bits')).toEqual('field_32Bits');
    expect(StringUtils.convertSnakeToCamelCase('field_32_bits_lsb')).toEqual('field_32BitsLsb');
    expect(StringUtils.convertSnakeToCamelCase('field_32bits')).toEqual('field_32bits');
    expect(StringUtils.convertSnakeToCamelCase('field_32bits_lsb')).toEqual('field_32bitsLsb');
  });
});
