/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {InMemoryStorage} from 'common/in_memory_storage';
import {PersistentStoreProxy} from './persistent_store_proxy';

describe('PersistentStoreObject', () => {
  it('uses defaults when no store is available', () => {
    const mockStorage = new InMemoryStorage();

    const defaultValues = {
      key1: 'value',
      key2: true,
    };
    const storeObject = PersistentStoreProxy.new(
      'storeKey',
      defaultValues,
      mockStorage,
    );

    expect(storeObject['key1']).toBe('value');
    expect(storeObject['key2']).toBe(true);
  });

  it('can update properties', () => {
    const mockStorage = new InMemoryStorage();

    const defaultValues = {
      key1: 'value',
      key2: true,
    };
    const storeObject = PersistentStoreProxy.new(
      'storeKey',
      defaultValues,
      mockStorage,
    );

    storeObject['key1'] = 'someOtherValue';
    storeObject['key2'] = false;
    expect(storeObject['key1']).toBe('someOtherValue');
    expect(storeObject['key2']).toBe(false);
  });

  it('uses explicitly set store data', () => {
    const mockStorage = new InMemoryStorage();

    const defaultValues = {
      key1: 'value',
      key2: true,
    };
    const storeObject = PersistentStoreProxy.new(
      'storeKey',
      defaultValues,
      mockStorage,
    );
    storeObject['key1'] = 'someOtherValue';
    storeObject['key2'] = false;

    const newStoreObject = PersistentStoreProxy.new(
      'storeKey',
      defaultValues,
      mockStorage,
    );
    expect(newStoreObject['key1']).toBe('someOtherValue');
    expect(newStoreObject['key2']).toBe(false);
  });

  it('uses default values if not explicitly set', () => {
    const mockStorage = new InMemoryStorage();

    const defaultValues = {
      key1: 'value',
      key2: true,
    };
    const storeObject = PersistentStoreProxy.new(
      'storeKey',
      defaultValues,
      mockStorage,
    );
    expect(storeObject['key1']).toBe('value');
    expect(storeObject['key2']).toBe(true);

    const newDefaultValues = {
      key1: 'someOtherValue',
      key2: false,
    };
    const newStoreObject = PersistentStoreProxy.new(
      'storeKey',
      newDefaultValues,
      mockStorage,
    );
    expect(newStoreObject['key1']).toBe('someOtherValue');
    expect(newStoreObject['key2']).toBe(false);
  });

  it("can't update non leaf configs", () => {
    const mockStorage = new InMemoryStorage();

    const defaultValues = {
      key1: 'value',
      key2: {
        key3: true,
      },
    };
    const storeObject = PersistentStoreProxy.new(
      'storeKey',
      defaultValues,
      mockStorage,
    );
    expect(() => (storeObject['key2'] = {key3: false})).toThrow();
  });

  it('can get nested configs', () => {
    const mockStorage = new InMemoryStorage();

    const defaultValues = {
      key1: 'value',
      key2: {
        key3: true,
      },
    };
    const storeObject = PersistentStoreProxy.new(
      'storeKey',
      defaultValues,
      mockStorage,
    );
    expect(storeObject['key2']['key3']).toBe(true);
  });

  it('can update schema', () => {
    const mockStorage = new InMemoryStorage();

    const schema1 = {
      key1: 'value1',
      key2: {
        key3: true,
      },
    };
    const storeObject1 = PersistentStoreProxy.new(
      'storeKey',
      schema1,
      mockStorage,
    );
    expect(storeObject1['key1']).toBe('value1');
    expect(storeObject1['key2']['key3']).toBe(true);

    // Change from default value to ensure we update the storage
    storeObject1['key1'] = 'someOtherValue';
    storeObject1['key2']['key3'] = false;

    const schema2 = {
      key1: {
        key3: 'value2',
      },
      key2: true,
    };
    const storeObject2 = PersistentStoreProxy.new(
      'storeKey',
      schema2,
      mockStorage,
    );
    expect(storeObject2['key1']['key3']).toBe('value2');
    expect(storeObject2['key2']).toBe(true);
  });
});
