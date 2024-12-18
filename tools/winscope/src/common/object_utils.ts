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

/**
 * Represents a key in an object, which may be a simple key or an array key.
 */
class Key {
  /**
   * @param key The key name.
   * @param index The index of the key in an array, or undefined if it's not an array key.
   */
  constructor(public key: string, public index?: number) {}

  /**
   * Returns true if the key is an array key.
   */
  isArrayKey(): boolean {
    return this.index !== undefined;
  }
}

/**
 * Utility class for working with objects.
 */
export class ObjectUtils {
  private static readonly ARRAY_KEY_REGEX = new RegExp('(.+)\\[(\\d+)\\]');

  /**
   * Gets the property at the given path in the object.
   *
   * @param obj The object to get the property from.
   * @param path The path to the property, using dot notation for nested objects.
   * @return The value of the property at the given path.
   */
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

  /**
   * Sets the property at the given path in the object.
   *
   * @param obj The object to set the property on.
   * @param path The path to the property, using dot notation for nested objects.
   * @param value The value to set the property to.
   */
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
