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

import {TimestampConverterUtils} from 'common/time/test_utils';
import {TIME_UNIT_TO_NANO} from 'common/time/time_units';
import {ParserBuilder} from 'test/unit/parser_builder';
import {TraceBuilder} from 'test/unit/trace_builder';
import {
  extractEntries,
  extractFrames,
  extractTimestamps,
  makeEmptyTrace,
} from 'test/unit/trace_utils';
import {FrameMapBuilder} from './frame_map_builder';
import {AbsoluteFrameIndex} from './index_types';
import {Trace} from './trace';
import {TraceType} from './trace_type';

describe('Trace', () => {
  let trace: Trace<string>;

  const time9 = TimestampConverterUtils.makeRealTimestamp(9n);
  const time10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const time11 = TimestampConverterUtils.makeRealTimestamp(11n);
  const time12 = TimestampConverterUtils.makeRealTimestamp(12n);
  const time13 = TimestampConverterUtils.makeRealTimestamp(13n);
  const time14 = TimestampConverterUtils.makeRealTimestamp(14n);
  const time15 = TimestampConverterUtils.makeRealTimestamp(15n);

  beforeAll(() => {
    // Time:       10    11                 12    13
    // Entry:      0    1-2                 3     4
    //             |     |                  |     |
    // Frame:      0     1     2     3     4-5    6
    trace = new TraceBuilder<string>()
      .setEntries(['entry-0', 'entry-1', 'entry-2', 'entry-3', 'entry-4'])
      .setTimestamps([time10, time11, time11, time12, time13])
      .setFrame(0, 0)
      .setFrame(1, 1)
      .setFrame(2, 1)
      .setFrame(3, 4)
      .setFrame(3, 5)
      .setFrame(4, 6)
      .build();
  });

  it('getEntry()', async () => {
    expect(await trace.getEntry(0).getValue()).toEqual('entry-0');
    expect(await trace.getEntry(4).getValue()).toEqual('entry-4');
    expect(() => {
      trace.getEntry(5);
    }).toThrow();

    expect(await trace.getEntry(-1).getValue()).toEqual('entry-4');
    expect(await trace.getEntry(-5).getValue()).toEqual('entry-0');
    expect(() => {
      trace.getEntry(-6);
    }).toThrow();
  });

  it('getFrame()', async () => {
    expect(await extractFrames(trace.getFrame(0))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[0, ['entry-0']]]),
    );
    expect(await extractFrames(trace.getFrame(1))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[1, ['entry-1', 'entry-2']]]),
    );
    expect(await extractFrames(trace.getFrame(2))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[2, []]]),
    );
    expect(await extractFrames(trace.getFrame(3))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[3, []]]),
    );
    expect(await extractFrames(trace.getFrame(4))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[4, ['entry-3']]]),
    );
    expect(await extractFrames(trace.getFrame(5))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[5, ['entry-3']]]),
    );
    expect(await extractFrames(trace.getFrame(6))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[6, ['entry-4']]]),
    );
  });

  it('findClosestEntry()', async () => {
    // empty
    expect(trace.sliceEntries(0, 0).findClosestEntry(time10)).toBeUndefined();

    // slice
    const slice = trace.sliceEntries(1, -1);
    expect(await slice.findClosestEntry(time9)?.getValue()).toEqual('entry-1');
    expect(await slice.findClosestEntry(time10)?.getValue()).toEqual('entry-1');
    expect(await slice.findClosestEntry(time11)?.getValue()).toEqual('entry-1');
    expect(await slice.findClosestEntry(time12)?.getValue()).toEqual('entry-3');
    expect(await slice.findClosestEntry(time13)?.getValue()).toEqual('entry-3');
    expect(await slice.findClosestEntry(time14)?.getValue()).toEqual('entry-3');

    // full trace
    expect(await trace.findClosestEntry(time9)?.getValue()).toEqual('entry-0');
    expect(await trace.findClosestEntry(time10)?.getValue()).toEqual('entry-0');
    expect(await trace.findClosestEntry(time11)?.getValue()).toEqual('entry-1');
    expect(await trace.findClosestEntry(time12)?.getValue()).toEqual('entry-3');
    expect(await trace.findClosestEntry(time13)?.getValue()).toEqual('entry-4');
    expect(await trace.findClosestEntry(time14)?.getValue()).toEqual('entry-4');
  });

  it('findFirstGreaterOrEqualEntry()', async () => {
    // empty
    expect(
      trace.sliceEntries(0, 0).findFirstGreaterOrEqualEntry(time10),
    ).toBeUndefined();

    // slice
    const slice = trace.sliceEntries(1, -1);
    expect(await slice.findFirstGreaterOrEqualEntry(time9)?.getValue()).toEqual(
      'entry-1',
    );
    expect(
      await slice.findFirstGreaterOrEqualEntry(time10)?.getValue(),
    ).toEqual('entry-1');
    expect(
      await slice.findFirstGreaterOrEqualEntry(time11)?.getValue(),
    ).toEqual('entry-1');
    expect(
      await slice.findFirstGreaterOrEqualEntry(time12)?.getValue(),
    ).toEqual('entry-3');
    expect(await slice.findFirstGreaterOrEqualEntry(time13)).toBeUndefined();

    // full trace
    expect(await trace.findFirstGreaterOrEqualEntry(time9)?.getValue()).toEqual(
      'entry-0',
    );
    expect(
      await trace.findFirstGreaterOrEqualEntry(time10)?.getValue(),
    ).toEqual('entry-0');
    expect(
      await trace.findFirstGreaterOrEqualEntry(time11)?.getValue(),
    ).toEqual('entry-1');
    expect(
      await trace.findFirstGreaterOrEqualEntry(time12)?.getValue(),
    ).toEqual('entry-3');
    expect(
      await trace.findFirstGreaterOrEqualEntry(time13)?.getValue(),
    ).toEqual('entry-4');
    expect(await trace.findFirstGreaterOrEqualEntry(time14)).toBeUndefined();
  });

  it('findFirstGreaterEntry()', async () => {
    // empty
    expect(
      trace.sliceEntries(0, 0).findFirstGreaterEntry(time10),
    ).toBeUndefined();

    // slice
    const slice = trace.sliceEntries(1, -1);
    expect(await slice.findFirstGreaterEntry(time9)?.getValue()).toEqual(
      'entry-1',
    );
    expect(await slice.findFirstGreaterEntry(time10)?.getValue()).toEqual(
      'entry-1',
    );
    expect(await slice.findFirstGreaterEntry(time11)?.getValue()).toEqual(
      'entry-3',
    );
    expect(slice.findFirstGreaterEntry(time12)).toBeUndefined();

    // full trace
    expect(await trace.findFirstGreaterEntry(time9)?.getValue()).toEqual(
      'entry-0',
    );
    expect(await trace.findFirstGreaterEntry(time10)?.getValue()).toEqual(
      'entry-1',
    );
    expect(await trace.findFirstGreaterEntry(time11)?.getValue()).toEqual(
      'entry-3',
    );
    expect(await trace.findFirstGreaterEntry(time12)?.getValue()).toEqual(
      'entry-4',
    );
    expect(trace.findFirstGreaterEntry(time13)).toBeUndefined();
  });

  it('findLastLowerOrEqualEntry()', async () => {
    // empty
    expect(
      trace.sliceEntries(0, 0).findLastLowerOrEqualEntry(time10),
    ).toBeUndefined();

    // slice
    const slice = trace.sliceEntries(1, -1);
    expect(slice.findLastLowerOrEqualEntry(time9)).toBeUndefined();
    expect(slice.findLastLowerOrEqualEntry(time10)).toBeUndefined();
    expect(await slice.findLastLowerOrEqualEntry(time11)?.getValue()).toEqual(
      'entry-2',
    );
    expect(await slice.findLastLowerOrEqualEntry(time12)?.getValue()).toEqual(
      'entry-3',
    );
    expect(await slice.findLastLowerOrEqualEntry(time13)?.getValue()).toEqual(
      'entry-3',
    );

    // full trace
    expect(trace.findLastLowerOrEqualEntry(time9)).toBeUndefined();
    expect(await trace.findLastLowerOrEqualEntry(time10)?.getValue()).toEqual(
      'entry-0',
    );
    expect(await trace.findLastLowerOrEqualEntry(time11)?.getValue()).toEqual(
      'entry-2',
    );
    expect(await trace.findLastLowerOrEqualEntry(time12)?.getValue()).toEqual(
      'entry-3',
    );
    expect(await trace.findLastLowerOrEqualEntry(time13)?.getValue()).toEqual(
      'entry-4',
    );
    expect(await trace.findLastLowerOrEqualEntry(time14)?.getValue()).toEqual(
      'entry-4',
    );
  });

  it('findLastLowerEntry()', async () => {
    // empty
    expect(trace.sliceEntries(0, 0).findLastLowerEntry(time10)).toBeUndefined();

    // slice
    const slice = trace.sliceEntries(1, -1);
    expect(slice.findLastLowerEntry(time9)).toBeUndefined();
    expect(slice.findLastLowerEntry(time10)).toBeUndefined();
    expect(slice.findLastLowerEntry(time11)).toBeUndefined();
    expect(await slice.findLastLowerEntry(time12)?.getValue()).toEqual(
      'entry-2',
    );
    expect(await slice.findLastLowerEntry(time13)?.getValue()).toEqual(
      'entry-3',
    );
    expect(await slice.findLastLowerEntry(time14)?.getValue()).toEqual(
      'entry-3',
    );
    expect(await slice.findLastLowerEntry(time15)?.getValue()).toEqual(
      'entry-3',
    );

    // full trace
    expect(trace.findLastLowerEntry(time9)).toBeUndefined();
    expect(trace.findLastLowerEntry(time10)).toBeUndefined();
    expect(await trace.findLastLowerEntry(time11)?.getValue()).toEqual(
      'entry-0',
    );
    expect(await trace.findLastLowerEntry(time12)?.getValue()).toEqual(
      'entry-2',
    );
    expect(await trace.findLastLowerEntry(time13)?.getValue()).toEqual(
      'entry-3',
    );
    expect(await trace.findLastLowerEntry(time14)?.getValue()).toEqual(
      'entry-4',
    );
    expect(await trace.findLastLowerEntry(time15)?.getValue()).toEqual(
      'entry-4',
    );
  });

  // Hint: look at frame mapping specified in test's set up to fully understand the assertions
  it('sliceEntries()', async () => {
    const slice = trace.sliceEntries(1, 4);

    const expectedEntriesFull = ['entry-1', 'entry-2', 'entry-3'];
    const expectedFramesEmpty = new Map<AbsoluteFrameIndex, string[]>();
    const expectedFramesFull = new Map<AbsoluteFrameIndex, string[]>([
      [1, ['entry-1', 'entry-2']],
      [2, []],
      [3, []],
      [4, ['entry-3']],
      [5, ['entry-3']],
    ]);

    // empty
    {
      expect(await extractFrames(slice.sliceEntries(1, 1))).toEqual(
        expectedFramesEmpty,
      );
      expect(await extractEntries(slice.sliceEntries(1, 1))).toEqual([]);

      expect(await extractFrames(slice.sliceEntries(-1, -1))).toEqual(
        expectedFramesEmpty,
      );
      expect(await extractEntries(slice.sliceEntries(-1, -1))).toEqual([]);

      expect(await extractFrames(slice.sliceEntries(2, 1))).toEqual(
        expectedFramesEmpty,
      );
      expect(await extractEntries(slice.sliceEntries(2, 1))).toEqual([]);

      expect(await extractFrames(slice.sliceEntries(-1, -2))).toEqual(
        expectedFramesEmpty,
      );
      expect(await extractEntries(slice.sliceEntries(-1, -2))).toEqual([]);
    }

    // full
    {
      expect(await extractEntries(slice.sliceEntries())).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceEntries())).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceEntries(0))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceEntries(0))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceEntries(0, 3))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceEntries(0, 3))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceEntries(-3))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceEntries(-3))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceEntries(-3, 3))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceEntries(-3, 3))).toEqual(
        expectedFramesFull,
      );
    }

    // slice away front (positive index)
    {
      expect(await extractEntries(slice.sliceEntries(1))).toEqual([
        'entry-2',
        'entry-3',
      ]);
      expect(await extractFrames(slice.sliceEntries(1))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [1, ['entry-2']],
          [2, []],
          [3, []],
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );

      expect(await extractEntries(slice.sliceEntries(2))).toEqual(['entry-3']);
      expect(await extractFrames(slice.sliceEntries(2))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );

      expect(await extractEntries(slice.sliceEntries(3))).toEqual([]);
      expect(await extractFrames(slice.sliceEntries(3))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceEntries(4))).toEqual([]);
      expect(await extractFrames(slice.sliceEntries(4))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceEntries(1000000))).toEqual([]);
      expect(await extractFrames(slice.sliceEntries(1000000))).toEqual(
        expectedFramesEmpty,
      );
    }

    // slice away front (negative index)
    {
      expect(await extractEntries(slice.sliceEntries(-3))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceEntries(-3))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceEntries(-2))).toEqual([
        'entry-2',
        'entry-3',
      ]);
      expect(await extractFrames(slice.sliceEntries(-2))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [1, ['entry-2']],
          [2, []],
          [3, []],
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );

      expect(await extractEntries(slice.sliceEntries(-1))).toEqual(['entry-3']);
      expect(await extractFrames(slice.sliceEntries(-1))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );
    }

    // slice away back (positive index)
    {
      expect(await extractEntries(slice.sliceEntries(undefined, 2))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(slice.sliceEntries(undefined, 2))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[1, ['entry-1', 'entry-2']]]),
      );

      expect(await extractEntries(slice.sliceEntries(undefined, 1))).toEqual([
        'entry-1',
      ]);
      expect(await extractFrames(slice.sliceEntries(undefined, 1))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[1, ['entry-1']]]),
      );

      expect(await extractEntries(slice.sliceEntries(undefined, 0))).toEqual(
        [],
      );
      expect(await extractFrames(slice.sliceEntries(undefined, 0))).toEqual(
        expectedFramesEmpty,
      );
    }

    // slice away back (negative index)
    {
      expect(await extractEntries(slice.sliceEntries(undefined, -1))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(slice.sliceEntries(undefined, -1))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[1, ['entry-1', 'entry-2']]]),
      );

      expect(await extractEntries(slice.sliceEntries(undefined, -2))).toEqual([
        'entry-1',
      ]);
      expect(await extractFrames(slice.sliceEntries(undefined, -2))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[1, ['entry-1']]]),
      );

      expect(await extractEntries(slice.sliceEntries(undefined, -3))).toEqual(
        [],
      );
      expect(await extractFrames(slice.sliceEntries(undefined, -3))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceEntries(undefined, -4))).toEqual(
        [],
      );
      expect(await extractFrames(slice.sliceEntries(undefined, -4))).toEqual(
        expectedFramesEmpty,
      );

      expect(
        await extractEntries(slice.sliceEntries(undefined, -1000000)),
      ).toEqual([]);
      expect(
        await extractFrames(slice.sliceEntries(undefined, -1000000)),
      ).toEqual(expectedFramesEmpty);
    }
  });

  // Hint: look at frame mapping specified in test's set up to fully understand the assertions
  it('sliceTime()', async () => {
    const slice = trace.sliceTime(time11, time13); // drop first + last entries

    const expectedEntriesFull = ['entry-1', 'entry-2', 'entry-3'];
    const expectedFramesEmpty = new Map<AbsoluteFrameIndex, string[]>();
    const expectedFramesFull = new Map<AbsoluteFrameIndex, string[]>([
      [1, ['entry-1', 'entry-2']],
      [2, []],
      [3, []],
      [4, ['entry-3']],
      [5, ['entry-3']],
    ]);

    // empty
    {
      expect(await extractEntries(slice.sliceTime(time11, time11))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time11, time11))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(time11, time10))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time11, time10))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(time9, time10))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time9, time10))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(time10, time9))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time10, time9))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(time14, time15))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time14, time15))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(time15, time14))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time15, time14))).toEqual(
        expectedFramesEmpty,
      );
    }

    // full
    {
      expect(await extractEntries(slice.sliceTime())).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceTime())).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceTime(time9))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceTime(time9))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceTime(time10))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceTime(time10))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceTime(undefined, time14))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceTime(undefined, time14))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceTime(undefined, time15))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceTime(undefined, time15))).toEqual(
        expectedFramesFull,
      );

      expect(await extractEntries(slice.sliceTime(time10, time14))).toEqual(
        expectedEntriesFull,
      );
      expect(await extractFrames(slice.sliceTime(time10, time14))).toEqual(
        expectedFramesFull,
      );
    }

    // middle
    {
      expect(await extractEntries(slice.sliceTime(time12, time13))).toEqual([
        'entry-3',
      ]);
      expect(await extractFrames(slice.sliceTime(time12, time13))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );
    }

    // slice away front
    {
      expect(await extractEntries(slice.sliceTime(time12))).toEqual([
        'entry-3',
      ]);
      expect(await extractFrames(slice.sliceTime(time12))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );

      expect(await extractEntries(slice.sliceTime(time13))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time13))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(time14))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time14))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(time15))).toEqual([]);
      expect(await extractFrames(slice.sliceTime(time15))).toEqual(
        expectedFramesEmpty,
      );
    }

    // slice away back
    {
      expect(await extractEntries(slice.sliceTime(undefined, time12))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(slice.sliceTime(undefined, time12))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[1, ['entry-1', 'entry-2']]]),
      );

      expect(await extractEntries(slice.sliceTime(undefined, time11))).toEqual(
        [],
      );
      expect(await extractFrames(slice.sliceTime(undefined, time11))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(undefined, time10))).toEqual(
        [],
      );
      expect(await extractFrames(slice.sliceTime(undefined, time10))).toEqual(
        expectedFramesEmpty,
      );

      expect(await extractEntries(slice.sliceTime(undefined, time9))).toEqual(
        [],
      );
      expect(await extractFrames(slice.sliceTime(undefined, time9))).toEqual(
        expectedFramesEmpty,
      );
    }
  });

  // Hint: look at frame mapping specified in test's set up to fully understand the assertions
  it('sliceFrames()', async () => {
    const slice = trace.sliceEntries(1, -1);

    // empty
    {
      const expectedEntries = new Array<string>();
      const expectedFrames = new Map<AbsoluteFrameIndex, string[]>([]);
      expect(await extractEntries(slice.sliceFrames(1, 1))).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames(1, 1))).toEqual(
        expectedFrames,
      );
      expect(await extractEntries(slice.sliceFrames(5, 1))).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames(5, 1))).toEqual(
        expectedFrames,
      );
      expect(await extractEntries(slice.sliceFrames(3, 2))).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames(3, 2))).toEqual(
        expectedFrames,
      );
    }

    // middle
    {
      expect(await extractEntries(slice.sliceFrames(2, 3))).toEqual([]);
      expect(await extractFrames(slice.sliceFrames(2, 3))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[2, []]]),
      );
      expect(await extractEntries(slice.sliceFrames(2, 4))).toEqual([]);
      expect(await extractFrames(slice.sliceFrames(2, 4))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [2, []],
          [3, []],
        ]),
      );
      expect(await extractEntries(slice.sliceFrames(2, 5))).toEqual([
        'entry-3',
      ]);
      expect(await extractFrames(slice.sliceFrames(2, 5))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [2, []],
          [3, []],
          [4, ['entry-3']],
        ]),
      );
    }

    // full
    {
      const expectedEntries = ['entry-1', 'entry-2', 'entry-3'];
      const expectedFrames = new Map<AbsoluteFrameIndex, string[]>([
        [1, ['entry-1', 'entry-2']],
        [2, []],
        [3, []],
        [4, ['entry-3']],
        [5, ['entry-3']],
      ]);
      expect(await extractEntries(slice.sliceFrames())).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames())).toEqual(expectedFrames);
      expect(await extractEntries(slice.sliceFrames(0))).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames(0))).toEqual(expectedFrames);
      expect(await extractEntries(slice.sliceFrames(undefined, 6))).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames(undefined, 6))).toEqual(
        expectedFrames,
      );
      expect(await extractEntries(slice.sliceFrames(1, 6))).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames(1, 6))).toEqual(
        expectedFrames,
      );
      expect(await extractEntries(slice.sliceFrames(0, 7))).toEqual(
        expectedEntries,
      );
      expect(await extractFrames(slice.sliceFrames(0, 7))).toEqual(
        expectedFrames,
      );
    }

    // slice away front
    {
      expect(await extractEntries(slice.sliceFrames(2))).toEqual(['entry-3']);
      expect(await extractFrames(slice.sliceFrames(2))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [2, []],
          [3, []],
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );
      expect(await extractEntries(slice.sliceFrames(4))).toEqual(['entry-3']);
      expect(await extractFrames(slice.sliceFrames(4))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );
      expect(await extractEntries(slice.sliceFrames(5))).toEqual(['entry-3']);
      expect(await extractFrames(slice.sliceFrames(5))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[5, ['entry-3']]]),
      );
      expect(await extractEntries(slice.sliceFrames(6))).toEqual([]);
      expect(await extractFrames(slice.sliceFrames(6))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([]),
      );
      expect(await extractEntries(slice.sliceFrames(1000))).toEqual([]);
      expect(await extractFrames(slice.sliceFrames(1000))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([]),
      );
    }

    // slice away back
    {
      expect(await extractEntries(slice.sliceFrames(undefined, 6))).toEqual([
        'entry-1',
        'entry-2',
        'entry-3',
      ]);
      expect(await extractFrames(slice.sliceFrames(undefined, 6))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [1, ['entry-1', 'entry-2']],
          [2, []],
          [3, []],
          [4, ['entry-3']],
          [5, ['entry-3']],
        ]),
      );
      expect(await extractEntries(slice.sliceFrames(undefined, 5))).toEqual([
        'entry-1',
        'entry-2',
        'entry-3',
      ]);
      expect(await extractFrames(slice.sliceFrames(undefined, 5))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [1, ['entry-1', 'entry-2']],
          [2, []],
          [3, []],
          [4, ['entry-3']],
        ]),
      );
      expect(await extractEntries(slice.sliceFrames(undefined, 4))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(slice.sliceFrames(undefined, 4))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [1, ['entry-1', 'entry-2']],
          [2, []],
          [3, []],
        ]),
      );
      expect(await extractEntries(slice.sliceFrames(undefined, 3))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(slice.sliceFrames(undefined, 3))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([
          [1, ['entry-1', 'entry-2']],
          [2, []],
        ]),
      );
      expect(await extractEntries(slice.sliceFrames(undefined, 2))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(slice.sliceFrames(undefined, 2))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>([[1, ['entry-1', 'entry-2']]]),
      );
      expect(await extractEntries(slice.sliceFrames(undefined, 1))).toEqual([]);
      expect(await extractFrames(slice.sliceFrames(undefined, 1))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );
      expect(await extractEntries(slice.sliceFrames(undefined, 0))).toEqual([]);
      expect(await extractFrames(slice.sliceFrames(undefined, 0))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );
    }
  });

  it('can slice full trace', async () => {
    // entries
    expect(await extractEntries(trace.sliceEntries(1, 1))).toEqual([]);
    expect(await extractEntries(trace.sliceEntries())).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractEntries(trace.sliceEntries(2))).toEqual([
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractEntries(trace.sliceEntries(-3))).toEqual([
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractEntries(trace.sliceEntries(undefined, 3))).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
    ]);
    expect(await extractEntries(trace.sliceEntries(undefined, -2))).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
    ]);
    expect(await extractEntries(trace.sliceEntries(1, 4))).toEqual([
      'entry-1',
      'entry-2',
      'entry-3',
    ]);

    // time
    const time12 = TimestampConverterUtils.makeRealTimestamp(12n);
    const time13 = TimestampConverterUtils.makeRealTimestamp(13n);
    expect(await extractEntries(trace.sliceTime(time12, time12))).toEqual([]);
    expect(await extractEntries(trace.sliceTime())).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractEntries(trace.sliceTime(time12, time13))).toEqual([
      'entry-3',
    ]);
    expect(await extractEntries(trace.sliceTime(time12))).toEqual([
      'entry-3',
      'entry-4',
    ]);
    expect(await extractEntries(trace.sliceTime(undefined, time12))).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
    ]);

    // frames
    expect(await extractEntries(trace.sliceFrames(1, 1))).toEqual([]);
    expect(await extractEntries(trace.sliceFrames())).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractEntries(trace.sliceFrames(2))).toEqual([
      'entry-3',
      'entry-4',
    ]);
    expect(await extractEntries(trace.sliceFrames(undefined, 5))).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
    ]);
    expect(await extractEntries(trace.sliceFrames(2, 5))).toEqual(['entry-3']);
  });

  it('can slice empty trace', async () => {
    const empty = trace.sliceEntries(0, 0);

    // entries
    expect(await extractEntries(empty.sliceEntries())).toEqual([]);
    expect(await extractEntries(empty.sliceEntries(1))).toEqual([]);
    expect(await extractEntries(empty.sliceEntries(1, 2))).toEqual([]);

    // time
    const time12 = TimestampConverterUtils.makeRealTimestamp(12n);
    const time13 = TimestampConverterUtils.makeRealTimestamp(13n);
    expect(await extractEntries(empty.sliceTime())).toEqual([]);
    expect(await extractEntries(empty.sliceTime(time12))).toEqual([]);
    expect(await extractEntries(empty.sliceTime(time12, time13))).toEqual([]);

    // frames
    expect(await extractEntries(empty.sliceFrames())).toEqual([]);
    expect(await extractEntries(empty.sliceFrames(1))).toEqual([]);
    expect(await extractEntries(empty.sliceFrames(1, 2))).toEqual([]);
  });

  it('forEachEntry()', async () => {
    expect(await extractEntries(trace)).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
  });

  it('forEachTimestamp()', () => {
    expect(extractTimestamps(trace)).toEqual([
      time10,
      time11,
      time11,
      time12,
      time13,
    ]);
    expect(extractTimestamps(trace.sliceEntries(1, -1))).toEqual([
      time11,
      time11,
      time12,
    ]);
  });

  // Hint: look at frame mapping specified in test's set up to fully understand the assertions
  it('forEachFrame()', async () => {
    // full trace
    {
      const expected = new Map<AbsoluteFrameIndex, string[]>([
        [0, ['entry-0']],
        [1, ['entry-1', 'entry-2']],
        [2, []],
        [3, []],
        [4, ['entry-3']],
        [5, ['entry-3']],
        [6, ['entry-4']],
      ]);
      expect(await extractFrames(trace)).toEqual(expected);
    }
    // slice
    {
      const slice = trace.sliceFrames(1, 5);
      const expected = new Map<AbsoluteFrameIndex, string[]>([
        [1, ['entry-1', 'entry-2']],
        [2, []],
        [3, []],
        [4, ['entry-3']],
      ]);
      expect(await extractFrames(slice)).toEqual(expected);
    }
  });

  it('updates frames range when slicing', () => {
    expect(trace.sliceEntries(0).getFramesRange()).toEqual({start: 0, end: 7});
    expect(trace.sliceEntries(1).getFramesRange()).toEqual({start: 1, end: 7});
    expect(trace.sliceEntries(2).getFramesRange()).toEqual({start: 1, end: 7});
    expect(trace.sliceEntries(3).getFramesRange()).toEqual({start: 4, end: 7});
    expect(trace.sliceEntries(4).getFramesRange()).toEqual({start: 6, end: 7});
    expect(trace.sliceEntries(5).getFramesRange()).toEqual(undefined);

    expect(trace.sliceEntries(undefined, 5).getFramesRange()).toEqual({
      start: 0,
      end: 7,
    });
    expect(trace.sliceEntries(undefined, 4).getFramesRange()).toEqual({
      start: 0,
      end: 6,
    });
    expect(trace.sliceEntries(undefined, 3).getFramesRange()).toEqual({
      start: 0,
      end: 2,
    });
    expect(trace.sliceEntries(undefined, 2).getFramesRange()).toEqual({
      start: 0,
      end: 2,
    });
    expect(trace.sliceEntries(undefined, 1).getFramesRange()).toEqual({
      start: 0,
      end: 1,
    });
    expect(trace.sliceEntries(undefined, 0).getFramesRange()).toEqual(
      undefined,
    );
  });

  it('can handle some trace entries with unavailable frame info', async () => {
    // Entry:      0     1     2     3     4
    //                   |           |
    // Frame:            0           2
    // Time:       10    11    12    13    14
    const trace = new TraceBuilder<string>()
      .setEntries(['entry-0', 'entry-1', 'entry-2', 'entry-3', 'entry-4'])
      .setTimestamps([time10, time11, time12, time13, time14])
      .setFrame(1, 0)
      .setFrame(3, 2)
      .build();

    // Slice entries
    expect(await extractEntries(trace.sliceEntries())).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceEntries())).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([
        [0, ['entry-1']],
        [1, []],
        [2, ['entry-3']],
      ]),
    );

    expect(await extractEntries(trace.sliceEntries(1))).toEqual([
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceEntries(1))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([
        [0, ['entry-1']],
        [1, []],
        [2, ['entry-3']],
      ]),
    );

    expect(await extractEntries(trace.sliceEntries(2))).toEqual([
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceEntries(2))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[2, ['entry-3']]]),
    );

    expect(await extractEntries(trace.sliceEntries(3))).toEqual([
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceEntries(3))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[2, ['entry-3']]]),
    );

    expect(await extractEntries(trace.sliceEntries(4))).toEqual(['entry-4']);
    expect(await extractFrames(trace.sliceEntries(4))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>(),
    );

    // Slice time
    expect(await extractEntries(trace.sliceTime())).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceTime())).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([
        [0, ['entry-1']],
        [1, []],
        [2, ['entry-3']],
      ]),
    );

    expect(await extractEntries(trace.sliceTime(time11))).toEqual([
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceTime(time11))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([
        [0, ['entry-1']],
        [1, []],
        [2, ['entry-3']],
      ]),
    );

    expect(await extractEntries(trace.sliceTime(time12))).toEqual([
      'entry-2',
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceTime(time12))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[2, ['entry-3']]]),
    );

    expect(await extractEntries(trace.sliceTime(time13))).toEqual([
      'entry-3',
      'entry-4',
    ]);
    expect(await extractFrames(trace.sliceTime(time13))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([[2, ['entry-3']]]),
    );

    expect(await extractEntries(trace.sliceTime(time14))).toEqual(['entry-4']);
    expect(await extractFrames(trace.sliceTime(time14))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>(),
    );

    // Slice frames
    expect(await extractEntries(trace.sliceFrames())).toEqual([
      'entry-1',
      'entry-2',
      'entry-3',
    ]);
    expect(await extractFrames(trace.sliceFrames())).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([
        [0, ['entry-1']],
        [1, []],
        [2, ['entry-3']],
      ]),
    );

    expect(await extractEntries(trace.sliceFrames(1))).toEqual(['entry-3']);
    expect(await extractFrames(trace.sliceFrames(1))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([
        [1, []],
        [2, ['entry-3']],
      ]),
    );

    expect(await extractEntries(trace.sliceFrames(undefined, 2))).toEqual([
      'entry-1',
    ]);
    expect(await extractFrames(trace.sliceFrames(undefined, 2))).toEqual(
      new Map<AbsoluteFrameIndex, string[]>([
        [0, ['entry-1']],
        [1, []],
      ]),
    );
  });

  it('can handle unavailable frame info', async () => {
    const trace = new TraceBuilder<string>()
      .setTimestamps([time10, time11, time12])
      .setEntries(['entry-0', 'entry-1', 'entry-2'])
      .setFrameMap(undefined)
      .build();

    expect(await trace.getEntry(0).getValue()).toEqual('entry-0');
    expect(await extractEntries(trace)).toEqual([
      'entry-0',
      'entry-1',
      'entry-2',
    ]);
    expect(await extractEntries(trace.sliceEntries(1, 2))).toEqual(['entry-1']);
    expect(await extractEntries(trace.sliceTime(time11, time12))).toEqual([
      'entry-1',
    ]);

    expect(() => {
      trace.getFrame(0);
    }).toThrow();
    expect(() => {
      trace.sliceFrames(0, 1000);
    }).toThrow();
  });

  it('can handle empty frame info', async () => {
    // empty trace
    {
      const trace = new TraceBuilder<string>()
        .setEntries([])
        .setTimestamps([])
        .setFrameMap(new FrameMapBuilder(0, 0).build())
        .build();

      expect(await extractEntries(trace)).toEqual([]);
      expect(await extractFrames(trace)).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );

      expect(await extractEntries(trace.sliceEntries(1))).toEqual([]);
      expect(await extractFrames(trace.sliceEntries(1))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );

      expect(await extractEntries(trace.sliceTime(time11))).toEqual([]);
      expect(await extractFrames(trace.sliceTime(time11))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );

      expect(await extractEntries(trace.sliceFrames())).toEqual([]);
      expect(await extractFrames(trace.sliceFrames())).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );
    }
    // non-empty trace
    {
      const trace = new TraceBuilder<string>()
        .setEntries(['entry-0', 'entry-1', 'entry-2'])
        .setTimestamps([time10, time11, time12])
        .setFrameMap(new FrameMapBuilder(3, 0).build())
        .build();

      expect(await extractEntries(trace)).toEqual([
        'entry-0',
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(trace)).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );

      expect(await extractEntries(trace.sliceEntries(1))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(trace.sliceEntries(1))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );

      expect(await extractEntries(trace.sliceTime(time11))).toEqual([
        'entry-1',
        'entry-2',
      ]);
      expect(await extractFrames(trace.sliceTime(time11))).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );

      expect(await extractEntries(trace.sliceFrames())).toEqual([]);
      expect(await extractFrames(trace.sliceFrames())).toEqual(
        new Map<AbsoluteFrameIndex, string[]>(),
      );
    }
  });

  it('isDump()', () => {
    const trace = new TraceBuilder<string>()
      .setEntries(['entry-0'])
      .setTimestamps([time10])
      .build();
    expect(trace.isDump()).toBeTrue();
    expect(trace.isDumpWithoutTimestamp()).toBeFalse();
  });

  it('isDumpWithoutTimestamp()', () => {
    const trace = new TraceBuilder<string>()
      .setEntries(['entry-0'])
      .setTimestamps([TimestampConverterUtils.makeZeroTimestamp()])
      .build();
    expect(trace.isDumpWithoutTimestamp()).toBeTrue();
  });

  it('updates corruptedState on failure to parse entry', async () => {
    const trace = new TraceBuilder<string>()
      .setParser(
        new ParserBuilder<string>()
          .setIsCorrupted(true)
          .setEntries(['entry-0'])
          .setTimestamps([TimestampConverterUtils.makeZeroTimestamp()])
          .build(),
      )
      .build();
    expect(trace.isCorrupted()).toBeFalse();
    expect(trace.getCorruptedReason()).toBeUndefined();

    expectAsync(trace.getEntry(0).getValue()).toBeRejected();
    try {
      await trace.getEntry(0).getValue();
    } catch (e) {
      expect(trace.isCorrupted()).toBeTrue();
      expect(trace.getCorruptedReason()).toEqual(
        'Cannot parse entry at index 0',
      );
    }
  });

  it('spansMultipleDates()', () => {
    const time0 = TimestampConverterUtils.makeZeroTimestamp();
    const emptyTrace = makeEmptyTrace(TraceType.TEST_TRACE_STRING);
    expect(emptyTrace.spansMultipleDates()).toBeFalse();

    const traceWithElapsedTimestamps = new TraceBuilder<string>()
      .setEntries(['entry-0', 'entry-1'])
      .setTimestamps([
        time0,
        TimestampConverterUtils.makeElapsedTimestamp(
          BigInt(TIME_UNIT_TO_NANO.d),
        ),
      ])
      .build();
    expect(traceWithElapsedTimestamps.spansMultipleDates()).toBeFalse();

    const traceWithRealTimestampsOneDate = new TraceBuilder<string>()
      .setEntries(['entry-0', 'entry-1'])
      .setTimestamps([time10, time15])
      .build();
    expect(traceWithRealTimestampsOneDate.spansMultipleDates()).toBeFalse();

    const traceWitMultipleDates = new TraceBuilder<string>()
      .setEntries(['entry-0', 'entry-1'])
      .setTimestamps([
        TimestampConverterUtils.makeRealTimestamp(
          BigInt(TIME_UNIT_TO_NANO.h * 23),
        ),
        TimestampConverterUtils.makeRealTimestamp(
          BigInt(TIME_UNIT_TO_NANO.h * 25),
        ),
      ])
      .build();
    expect(traceWitMultipleDates.spansMultipleDates()).toBeTrue();

    const traceNoValidTimestamps = new TraceBuilder<string>()
      .setEntries(['entry-0', 'entry-1'])
      .setTimestamps([time0, time0])
      .build();
    expect(traceNoValidTimestamps.spansMultipleDates()).toBeFalse();
  });
});
