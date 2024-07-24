/*
 * Copyright 2022, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export class PersistentStoreProxy {
  static new<T extends object>(
    key: string,
    defaultState: T,
    storage: Storage,
  ): T {
    const storedState = JSON.parse(storage.getItem(key) ?? '{}');
    const currentState = mergeDeep({}, structuredClone(defaultState));
    mergeDeepKeepingStructure(currentState, storedState);
    return wrapWithPersistentStoreProxy(key, currentState, storage) as T;
  }
}

function wrapWithPersistentStoreProxy(
  storeKey: string,
  object: object,
  storage: Storage,
  baseObject: object = object,
): object {
  const updatableProps: string[] = [];

  for (const [key, value] of Object.entries(object)) {
    if (
      typeof value === 'string' ||
      typeof value === 'boolean' ||
      value === undefined
    ) {
      if (!Array.isArray(object)) {
        updatableProps.push(key);
      }
    } else {
      (object as any)[key] = wrapWithPersistentStoreProxy(
        storeKey,
        value,
        storage,
        baseObject,
      );
    }
  }

  const proxyObj = new Proxy(object, {
    set: (target, prop, newValue) => {
      if (typeof prop === 'symbol') {
        throw Error("Can't use symbol keys only strings");
      }
      if (Array.isArray(target) && typeof prop === 'number') {
        target[prop] = newValue;
        storage.setItem(storeKey, JSON.stringify(baseObject));
        return true;
      }
      if (!Array.isArray(target) && updatableProps.includes(prop)) {
        (target as any)[prop] = newValue;
        storage.setItem(storeKey, JSON.stringify(baseObject));
        return true;
      }
      throw Error(
        `Object property '${prop}' is not updatable. Can only update leaf keys: [${updatableProps}]`,
      );
    },
  });

  return proxyObj;
}

function isObject(item: any): boolean {
  return item && typeof item === 'object' && !Array.isArray(item);
}

/**
 * Merge sources into the target keeping the structure of the target.
 * @param target the object we mutate by merging the data from source into, but keep the object structure of
 * @param source the object we merge into target
 * @return the mutated target object
 */
function mergeDeepKeepingStructure(target: any, source: any): any {
  if (isObject(target) && isObject(source)) {
    for (const key in target) {
      if (source[key] === undefined) {
        continue;
      }

      if (isObject(target[key]) && isObject(source[key])) {
        mergeDeepKeepingStructure(target[key], source[key]);
        continue;
      }

      if (!isObject(target[key]) && !isObject(source[key])) {
        Object.assign(target, {[key]: source[key]});
        continue;
      }
    }
  }

  return target;
}

function mergeDeep(target: any, ...sources: any): any {
  if (!sources.length) return target;
  const source = sources.shift();

  if (isObject(target) && isObject(source)) {
    for (const key in source) {
      if (isObject(source[key])) {
        if (!target[key]) Object.assign(target, {[key]: {}});
        mergeDeep(target[key], source[key]);
      } else {
        Object.assign(target, {[key]: source[key]});
      }
    }
  }

  return mergeDeep(target, ...sources);
}
