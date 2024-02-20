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

import {ObjectUtils} from 'common/object_utils';
import {StringUtils} from 'common/string_utils';

export type FakeProto = any;

export class FakeProtoBuilder {
  private proto = {};

  addArg(
    key: string,
    valueType: string,
    intValue: bigint | undefined,
    realValue: number | undefined,
    stringValue: string | undefined,
  ): FakeProtoBuilder {
    const keyCamelCase = key
      .split('.')
      .map((token) => {
        return StringUtils.convertSnakeToCamelCase(token);
      })
      .join('.');
    const value = this.makeValue(valueType, intValue, realValue, stringValue);
    ObjectUtils.setProperty(this.proto, keyCamelCase, value);
    return this;
  }

  build(): FakeProto {
    return this.proto;
  }

  private makeValue(
    valueType: string,
    intValue: bigint | undefined,
    realValue: number | undefined,
    stringValue: string | undefined,
  ): string | bigint | number | boolean | null | undefined {
    switch (valueType) {
      case 'bool':
        return Boolean(intValue);
      case 'int':
        return intValue;
      case 'null':
        return null;
      case 'real':
        return realValue;
      case 'string':
        return stringValue;
      case 'uint':
        return intValue;
      default:
        throw new Error(`Unsupported type ${valueType}`);
    }
  }
}
