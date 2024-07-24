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

import {assertDefined, assertTrue} from './assert_utils';

class Key {
  constructor(public key: string, public index?: number) {}

  isArrayKey(): boolean {
    return this.index !== undefined;
  }
}

export class ObjectUtils {
  static readonly ARRAY_KEY_REGEX = new RegExp('(.+)\\[(\\d+)\\]');

  static getProperty(obj: object, path: string): any {
    const keys = ObjectUtils.parseKeys(path);
    keys.forEach((key) => {
      if (obj === undefined) {
        return;
      }

      if (key.isArrayKey()) {
        if ((obj as any)[key.key] === undefined) {
          return;
        }
        assertTrue(
          Array.isArray((obj as any)[key.key]),
          () => 'Expected to be array',
        );
        obj = (obj as any)[key.key][assertDefined(key.index)];
      } else {
        obj = (obj as any)[key.key];
      }
    });
    return obj;
  }

  static setProperty(obj: object, path: string, value: any) {
    const keys = ObjectUtils.parseKeys(path);

    keys.slice(0, -1).forEach((key) => {
      if (key.isArrayKey()) {
        ObjectUtils.initializePropertyArrayIfNeeded(obj, key);
        obj = (obj as any)[key.key][assertDefined(key.index)];
      } else {
        ObjectUtils.initializePropertyIfNeeded(obj, key.key);
        obj = (obj as any)[key.key];
      }
    });

    const lastKey = assertDefined(keys.at(-1));
    if (lastKey.isArrayKey()) {
      ObjectUtils.initializePropertyArrayIfNeeded(obj, lastKey);
      (obj as any)[lastKey.key][assertDefined(lastKey.index)] = value;
    } else {
      (obj as any)[lastKey.key] = value;
    }
  }

  private static parseKeys(path: string): Key[] {
    return path.split('.').map((rawKey) => {
      const match = ObjectUtils.ARRAY_KEY_REGEX.exec(rawKey);
      if (match) {
        return new Key(match[1], Number(match[2]));
      }
      return new Key(rawKey);
    });
  }

  private static initializePropertyIfNeeded(obj: object, key: string) {
    if ((obj as any)[key] === undefined) {
      (obj as any)[key] = {};
    }
    assertTrue(
      typeof (obj as any)[key] === 'object',
      () => 'Expected to be object',
    );
  }

  private static initializePropertyArrayIfNeeded(obj: object, key: Key) {
    if ((obj as any)[key.key] === undefined) {
      (obj as any)[key.key] = [];
    }
    if ((obj as any)[key.key][assertDefined(key.index)] === undefined) {
      (obj as any)[key.key][assertDefined(key.index)] = {};
    }
    assertTrue(
      Array.isArray((obj as any)[key.key]),
      () => 'Expected to be array',
    );
  }
}
