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

import {EventTag} from 'parsers/events/event_tag';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {AddCujProperties} from './add_cuj_properties';

describe('AddCujProperties', () => {
  let operation: AddCujProperties;

  beforeEach(() => {
    operation = new AddCujProperties();
  });

  it('adds all cuj properties', () => {
    const propertyRoot = new PropertyTreeBuilder()
      .setRootId('EventLogTrace')
      .setName('event')
      .setIsRoot(true)
      .setChildren([
        {name: 'eventTimestamp', value: 1681207048025596830n},
        {name: 'pid', value: 2806},
        {name: 'uid', value: 10227},
        {name: 'tid', value: 3604},
        {name: 'tag', value: EventTag.JANK_CUJ_BEGIN_TAG},
        {
          name: 'eventData',
          value: '[66,1681207048025580000,2661012903966,2661012904007,]',
        },
      ])
      .build();

    const expectedRoot = new PropertyTreeBuilder()
      .setRootId('EventLogTrace')
      .setName('event')
      .setIsRoot(true)
      .setChildren([
        {name: 'eventTimestamp', value: 1681207048025596830n},
        {name: 'pid', value: 2806},
        {name: 'uid', value: 10227},
        {name: 'tid', value: 3604},
        {name: 'tag', value: EventTag.JANK_CUJ_BEGIN_TAG},
        {
          name: 'eventData',
          value: '[66,1681207048025580000,2661012903966,2661012904007,]',
        },
        {name: 'cujType', value: 66},
        {
          name: 'cujTimestamp',
          children: [
            {name: 'unixNanos', value: 1681207048025580000n},
            {name: 'elapsedNanos', value: 2661012903966n},
            {name: 'systemUptimeNanos', value: 2661012904007n},
          ],
        },
        {name: 'cujTag', value: undefined},
      ])
      .build();

    operation.apply(propertyRoot);
    expect(propertyRoot).toEqual(expectedRoot);
  });
});
