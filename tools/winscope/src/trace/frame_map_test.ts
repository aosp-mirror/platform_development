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

import {FrameMap} from './frame_map';
import {FrameMapBuilder} from './frame_map_builder';

describe('FrameMapTest', () => {
  let map: FrameMap;

  beforeAll(() => {
    // Entry: 0   1-2    3    4    5
    //             |          |
    //             |          |
    // Frame:      1  2  3  4-5-6
    map = new FrameMapBuilder(5, 7)
      .setFrames(1, {start: 1, end: 2})
      .setFrames(2, {start: 1, end: 2})
      .setFrames(4, {start: 4, end: 7})
      .build();
  });

  it('getFramesRange()', () => {
    // empty
    expect(map.getFramesRange({start: -2, end: -1})).toEqual(undefined);
    expect(map.getFramesRange({start: 0, end: 1})).toEqual(undefined);
    expect(map.getFramesRange({start: 5, end: 6})).toEqual(undefined);
    expect(map.getFramesRange({start: 1, end: 1})).toEqual(undefined);

    // full
    expect(map.getFramesRange({start: 0, end: 6})).toEqual({start: 1, end: 7});
    expect(map.getFramesRange({start: 1, end: 5})).toEqual({start: 1, end: 7});

    // middle
    expect(map.getFramesRange({start: 1, end: 4})).toEqual({start: 1, end: 2});
    expect(map.getFramesRange({start: 1, end: 2})).toEqual({start: 1, end: 2});
    expect(map.getFramesRange({start: 2, end: 3})).toEqual({start: 1, end: 2});
    expect(map.getFramesRange({start: 3, end: 4})).toEqual(undefined);
    expect(map.getFramesRange({start: 4, end: 5})).toEqual({start: 4, end: 7});

    // slice away front
    expect(map.getFramesRange({start: 1, end: 6})).toEqual({start: 1, end: 7});
    expect(map.getFramesRange({start: 2, end: 6})).toEqual({start: 1, end: 7});
    expect(map.getFramesRange({start: 3, end: 6})).toEqual({start: 4, end: 7});
    expect(map.getFramesRange({start: 4, end: 6})).toEqual({start: 4, end: 7});
    expect(map.getFramesRange({start: 5, end: 6})).toEqual(undefined);
    expect(map.getFramesRange({start: 6, end: 6})).toEqual(undefined);

    // slice away back
    expect(map.getFramesRange({start: 0, end: 5})).toEqual({start: 1, end: 7});
    expect(map.getFramesRange({start: 0, end: 4})).toEqual({start: 1, end: 2});
    expect(map.getFramesRange({start: 0, end: 3})).toEqual({start: 1, end: 2});
    expect(map.getFramesRange({start: 0, end: 2})).toEqual({start: 1, end: 2});
    expect(map.getFramesRange({start: 0, end: 1})).toEqual(undefined);
    expect(map.getFramesRange({start: 0, end: 0})).toEqual(undefined);

    // query out of bounds
    expect(map.getFramesRange({start: -1, end: 7})).toEqual({start: 1, end: 7});
    expect(map.getFramesRange({start: -10, end: 10})).toEqual({
      start: 1,
      end: 7,
    });
    expect(map.getFramesRange({start: -1, end: 4})).toEqual({start: 1, end: 2});
    expect(map.getFramesRange({start: 4, end: 10})).toEqual({start: 4, end: 7});
  });

  it('getEntriesRange()', () => {
    // empty
    expect(map.getEntriesRange({start: -2, end: -1})).toEqual(undefined);
    expect(map.getEntriesRange({start: 7, end: 8})).toEqual(undefined);
    expect(map.getEntriesRange({start: 2, end: 4})).toEqual(undefined);
    expect(map.getEntriesRange({start: 3, end: 2})).toEqual(undefined);
    expect(map.getEntriesRange({start: 2, end: 2})).toEqual(undefined);

    // full
    expect(map.getEntriesRange({start: 0, end: 7})).toEqual({start: 1, end: 5});
    expect(map.getEntriesRange({start: -1, end: 8})).toEqual({
      start: 1,
      end: 5,
    });

    // middle
    expect(map.getEntriesRange({start: 1, end: 2})).toEqual({start: 1, end: 3});
    expect(map.getEntriesRange({start: 6, end: 7})).toEqual({start: 4, end: 5});
    expect(map.getEntriesRange({start: 1, end: 5})).toEqual({start: 1, end: 5});
    expect(map.getEntriesRange({start: 2, end: 5})).toEqual({start: 4, end: 5});
    expect(map.getEntriesRange({start: 1, end: 4})).toEqual({start: 1, end: 3});

    // slice away front
    expect(map.getEntriesRange({start: 0, end: 7})).toEqual({start: 1, end: 5});
    expect(map.getEntriesRange({start: 1, end: 7})).toEqual({start: 1, end: 5});
    expect(map.getEntriesRange({start: 2, end: 7})).toEqual({start: 4, end: 5});
    expect(map.getEntriesRange({start: 3, end: 7})).toEqual({start: 4, end: 5});
    expect(map.getEntriesRange({start: 4, end: 7})).toEqual({start: 4, end: 5});
    expect(map.getEntriesRange({start: 5, end: 7})).toEqual({start: 4, end: 5});
    expect(map.getEntriesRange({start: 6, end: 7})).toEqual({start: 4, end: 5});
    expect(map.getEntriesRange({start: 7, end: 7})).toEqual(undefined);

    // slice away back
    expect(map.getEntriesRange({start: 1, end: 6})).toEqual({start: 1, end: 5});
    expect(map.getEntriesRange({start: 1, end: 5})).toEqual({start: 1, end: 5});
    expect(map.getEntriesRange({start: 1, end: 4})).toEqual({start: 1, end: 3});
    expect(map.getEntriesRange({start: 1, end: 3})).toEqual({start: 1, end: 3});
    expect(map.getEntriesRange({start: 1, end: 2})).toEqual({start: 1, end: 3});
    expect(map.getEntriesRange({start: 1, end: 1})).toEqual(undefined);

    // query out of bounds
    expect(map.getEntriesRange({start: 0, end: 8})).toEqual({start: 1, end: 5});
    expect(map.getEntriesRange({start: -10, end: 10})).toEqual({
      start: 1,
      end: 5,
    });
    expect(map.getEntriesRange({start: -10, end: 4})).toEqual({
      start: 1,
      end: 3,
    });
    expect(map.getEntriesRange({start: 2, end: 10})).toEqual({
      start: 4,
      end: 5,
    });
  });

  it('getFullTraceFramesRange()', () => {
    expect(map.getFullTraceFramesRange()).toEqual({start: 1, end: 7});
  });
});
