/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {InMemoryStorage} from 'common/store/in_memory_storage';
import {TraceType} from 'trace/trace_type';
import {
  TraceConfigurationMap,
  updateConfigsFromStore,
} from './trace_configuration';

describe('updateConfigsFromStore', () => {
  const traceKey = 'test_trace';
  const testKeyPrefix = 'test key';
  const testKey = testKeyPrefix + traceKey;
  let target: TraceConfigurationMap;

  beforeEach(() => {
    target = {
      'test_trace': {
        name: 'Test Trace',
        config: {
          enabled: true,
          checkboxConfigs: [{name: 'test1', key: 'test1', enabled: false}],
          selectionConfigs: [
            {name: 'test2', key: 'test2', options: [], value: '1'},
            {name: 'test3', key: 'test3', options: [], value: ['1']},
          ],
        },
        available: true,
        types: [TraceType.TEST_TRACE_NUMBER],
      },
    };
  });

  it('updates target enabled field', () => {
    const sourceConfig = {enabled: false};
    updateConfigs(sourceConfig);
    expect(target[traceKey].config.enabled).toBeFalse();
  });

  it('does not override names or options of advanced config', () => {
    const sourceConfig = {
      checkboxConfigs: [{name: 'new1', key: 'test1', enabled: false}],
      selectionConfigs: [{name: 'new2', key: 'test2', options: ['1']}],
    };
    updateConfigs(sourceConfig);
    expect(target[traceKey].config.checkboxConfigs[0].name).toEqual('test1');
    const newSelectionConfig = target[traceKey].config.selectionConfigs[0];
    expect(newSelectionConfig.name).toEqual('test2');
    expect(newSelectionConfig.options).toEqual([]);
  });

  it('updates only configs that are present in target already', () => {
    updateSelectionConfigOptions();
    const sourceConfig = {
      checkboxConfigs: [
        {name: 'test1', key: 'test1', enabled: true},
        {name: 'test2', key: 'test2', enabled: false},
      ],
      selectionConfigs: [{name: 'test4', key: 'test4', value: ['1']}],
    };
    updateConfigs(sourceConfig);
    expect(target[traceKey].config).toEqual({
      enabled: true,
      checkboxConfigs: [{name: 'test1', key: 'test1', enabled: true}],
      selectionConfigs: [
        {name: 'test2', key: 'test2', options: ['1', '2'], value: '1'},
        {name: 'test3', key: 'test3', options: ['1', '2'], value: ['1']},
      ],
    });
  });

  it('updates select config values', () => {
    updateSelectionConfigOptions();
    const sourceConfig = {
      selectionConfigs: [
        {name: 'test2', key: 'test2', value: '2'},
        {name: 'test3', key: 'test3', value: ['1', '2']},
      ],
    };
    updateConfigs(sourceConfig);
    expect(target[traceKey].config.selectionConfigs).toEqual([
      {name: 'test2', key: 'test2', options: ['1', '2'], value: '2'},
      {name: 'test3', key: 'test3', options: ['1', '2'], value: ['1', '2']},
    ]);
  });

  it('does not update select config if stored value not in options', () => {
    const sourceConfig = {
      selectionConfigs: [
        {name: 'test2', key: 'test2', value: '2'},
        {name: 'test3', key: 'test3', value: ['1', '2']},
      ],
    };
    updateConfigs(sourceConfig);
    expect(target[traceKey].config.selectionConfigs).toEqual([
      {name: 'test2', key: 'test2', options: [], value: '1'},
      {name: 'test3', key: 'test3', options: [], value: ['1']},
    ]);
  });

  it('does not update select config if stored value different type to default value', () => {
    updateSelectionConfigOptions();
    const sourceConfig = {
      selectionConfigs: [
        {name: 'test2', key: 'test2', value: ['1']},
        {name: 'test3', key: 'test3', value: '1'},
      ],
    };
    updateConfigs(sourceConfig);
    expect(target[traceKey].config.selectionConfigs).toEqual([
      {name: 'test2', key: 'test2', options: ['1', '2'], value: '1'},
      {name: 'test3', key: 'test3', options: ['1', '2'], value: ['1']},
    ]);
  });

  function updateSelectionConfigOptions() {
    target[traceKey].config.selectionConfigs[0].options = ['1', '2'];
    target[traceKey].config.selectionConfigs[1].options = ['1', '2'];
  }

  function updateConfigs(sourceConfig: object) {
    const storage = new InMemoryStorage();
    storage.add(testKey, JSON.stringify(sourceConfig));
    updateConfigsFromStore(target, storage, testKeyPrefix);
  }
});
