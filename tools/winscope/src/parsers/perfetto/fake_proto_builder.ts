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

import {assertTrue} from 'common/assert_utils';
import {StringUtils} from 'common/string_utils';

export type FakeProto = any;

interface ArrayKey {
  key: string;
  index: number;
}

export class FakeProtoBuilder {
  private proto = {};
  static readonly ARRAY_KEY_REGEX = new RegExp('(.+)\\[(\\d+)\\]');

  addArg(
    key: string,
    valueType: string,
    intValue: bigint | undefined,
    realValue: number | undefined,
    stringValue: string | undefined
  ): FakeProtoBuilder {
    const keyTokens = key.split('.').map((token) => {
      return StringUtils.convertSnakeToCamelCase(token);
    });
    const value = this.makeValue(valueType, intValue, realValue, stringValue);
    this.addArgRec(this.proto, keyTokens, 0, value);
    return this;
  }

  build(): FakeProto {
    return this.proto;
  }

  private makeValue(
    valueType: string,
    intValue: bigint | undefined,
    realValue: number | undefined,
    stringValue: string | undefined
  ): any {
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
      // do nothing
    }
  }

  private addArgRec(proto: FakeProto, keys: string[], keyIndex: number, value: any) {
    const key = keys[keyIndex];
    const arrayKey = this.tryParseArrayKey(key);

    const isLeaf = keyIndex === keys.length - 1;
    if (isLeaf) {
      if (arrayKey) {
        this.initializeChildArrayIfNeeded(proto, arrayKey);
        proto[arrayKey.key][arrayKey.index] = value;
      } else {
        proto[key] = value;
      }
      return;
    }

    if (arrayKey) {
      this.initializeChildArrayIfNeeded(proto, arrayKey);
      this.addArgRec(proto[arrayKey.key][arrayKey.index], keys, keyIndex + 1, value);
    } else {
      this.initializeChildIfNeeded(proto, key);
      this.addArgRec(proto[key], keys, keyIndex + 1, value);
    }
  }

  private tryParseArrayKey(key: string): ArrayKey | undefined {
    const match = FakeProtoBuilder.ARRAY_KEY_REGEX.exec(key);
    if (!match) {
      return undefined;
    }
    return {
      key: match[1],
      index: Number(match[2]),
    };
  }

  private initializeChildIfNeeded(proto: FakeProto, key: string) {
    if (proto[key] === undefined) {
      proto[key] = {};
    }
    assertTrue(typeof proto[key] === 'object', () => 'Expected to be object');
  }

  private initializeChildArrayIfNeeded(proto: FakeProto, arrayKey: ArrayKey) {
    if (proto[arrayKey.key] === undefined) {
      proto[arrayKey.key] = [];
    }
    if (proto[arrayKey.key][arrayKey.index] === undefined) {
      proto[arrayKey.key][arrayKey.index] = {};
    }
    assertTrue(Array.isArray(proto[arrayKey.key]), () => 'Expected to be array');
  }
}
