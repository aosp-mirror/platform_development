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
import Long from 'long';
import {PropertiesTreeGenerator} from 'viewers/common/properties_tree_generator';
import {PropertiesTreeNode} from './ui_tree_utils';

describe('PropertiesTreeGenerator', () => {
  it('handles boolean', () => {
    const input = true;
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      propertyValue: 'true',
    };

    expect(actual).toEqual(expected);
  });

  it('handles number', () => {
    const input = 10;
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      propertyValue: '10',
    };

    expect(actual).toEqual(expected);
  });

  it('handles longs', () => {
    const input = new Long(10, 100, false);
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      propertyValue: '429496729610',
    };

    expect(actual).toEqual(expected);
  });

  it('handles string', () => {
    const input = 'value';
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      propertyValue: 'value',
    };

    expect(actual).toEqual(expected);
  });

  it('handles empty array', () => {
    const input: any[] = [];
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      propertyValue: '[]',
    };

    expect(actual).toEqual(expected);
  });

  it('handles array', () => {
    const input = ['value0', 'value1'];
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      children: [
        {
          propertyKey: '0',
          propertyValue: 'value0',
        },
        {
          propertyKey: '1',
          propertyValue: 'value1',
        },
      ],
    };

    expect(actual).toEqual(expected);
  });

  it('handles empty object', () => {
    const input = {};
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      propertyValue: '{}',
    };

    expect(actual).toEqual(expected);
  });

  it('handles object', () => {
    const input = {
      key0: 'value0',
      key1: 'value1',
    };
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      children: [
        {
          propertyKey: 'key0',
          propertyValue: 'value0',
        },
        {
          propertyKey: 'key1',
          propertyValue: 'value1',
        },
      ],
    };

    expect(actual).toEqual(expected);
  });

  it('handles nested objects', () => {
    const input = {
      object: {
        key: 'object_value',
      },
      array: ['array_value'],
    };
    const actual = new PropertiesTreeGenerator().generate('root', input);

    const expected: PropertiesTreeNode = {
      propertyKey: 'root',
      children: [
        {
          propertyKey: 'object',
          children: [
            {
              propertyKey: 'key',
              propertyValue: 'object_value',
            },
          ],
        },
        {
          propertyKey: 'array',
          children: [
            {
              propertyKey: '0',
              propertyValue: 'array_value',
            },
          ],
        },
      ],
    };

    expect(actual).toEqual(expected);
  });
});
