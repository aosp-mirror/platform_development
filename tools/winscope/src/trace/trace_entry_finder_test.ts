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

import {TimestampConverterUtils} from 'common/time/test_utils';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {TraceEntryFinder} from './trace_entry_finder';
import {TracePosition} from './trace_position';
import {TraceType} from './trace_type';

describe('TraceEntryFinder', () => {
  const emptyTrace = UnitTestUtils.makeEmptyTrace(TraceType.TEST_TRACE_STRING);
  const ts10 = TimestampConverterUtils.makeRealTimestamp(10n);
  const ts14 = TimestampConverterUtils.makeRealTimestamp(14n);
  const ts16 = TimestampConverterUtils.makeRealTimestamp(16n);
  const posTs10 = TracePosition.fromTimestamp(ts10);
  const trace = new TraceBuilder<string>()
    .setType(TraceType.SURFACE_FLINGER)
    .setTimestamps([
      ts10,
      TimestampConverterUtils.makeRealTimestamp(11n),
      TimestampConverterUtils.makeRealTimestamp(12n),
      ts14,
      TimestampConverterUtils.makeRealTimestamp(15n),
      ts16,
    ])
    .setEntries([
      'entry-0',
      'entry-1',
      'entry-2',
      'entry-3',
      'entry-4',
      'entry-5',
    ])
    .build();
  const traceWithFrames = new TraceBuilder<string>()
    .setType(TraceType.WINDOW_MANAGER)
    .setTimestamps([ts10, ts14, ts16])
    .setEntries(['entry-0', 'entry-1', 'entry-2'])
    .setFrame(0, 1)
    .setFrame(1, 1)
    .build();

  it('handles empty trace', () => {
    expect(
      TraceEntryFinder.findCorrespondingEntry(emptyTrace, posTs10),
    ).toBeUndefined();
  });

  it('returns sole entry of dump regardless of position', () => {
    const dump = new TraceBuilder<string>()
      .setTimestamps([TimestampConverterUtils.makeZeroTimestamp()])
      .setEntries(['entry-0'])
      .build();
    expect(TraceEntryFinder.findCorrespondingEntry(dump, posTs10)).toEqual(
      dump.getEntry(0),
    );
  });

  it('returns position entry only if from same trace', () => {
    const posFromEntry = TracePosition.fromTraceEntry(trace.getEntry(1));
    expect(
      TraceEntryFinder.findCorrespondingEntry(trace, posFromEntry),
    ).toEqual(posFromEntry.entry);
    expect(
      TraceEntryFinder.findCorrespondingEntry(traceWithFrames, posFromEntry),
    ).not.toEqual(posFromEntry.entry);
  });

  it('returns corresponding frame if available', () => {
    spyOn(trace, 'hasFrameInfo').and.returnValue(true);
    const frameSpy = spyOn(trace, 'getFrame').and.returnValue(trace);
    const posWithFrame = TracePosition.fromTraceEntry(
      traceWithFrames.getEntry(1),
    );
    expect(
      TraceEntryFinder.findCorrespondingEntry(trace, posWithFrame),
    ).toEqual(trace.getEntry(0));

    // defaults to finding by time if corresponding frame has no entries
    frameSpy.and.returnValue(emptyTrace);
    const correspondingEntryByTime = trace.getEntry(4);
    expect(
      TraceEntryFinder.findCorrespondingEntry(trace, posWithFrame),
    ).toEqual(correspondingEntryByTime);

    // robust to errors in finding corresponding frame
    frameSpy.and.throwError('');
    expect(
      TraceEntryFinder.findCorrespondingEntry(trace, posWithFrame),
    ).toEqual(correspondingEntryByTime);
  });

  it("finds first greater (else first equal) entry if position's trace precedes trace in ui pipeline", () => {
    const posFromEntry14 = TracePosition.fromTraceEntry(
      traceWithFrames.getEntry(1),
    );
    expect(
      TraceEntryFinder.findCorrespondingEntry(trace, posFromEntry14),
    ).toEqual(trace.getEntry(4));
    const posFromEntry16 = TracePosition.fromTraceEntry(
      traceWithFrames.getEntry(2),
    );
    expect(
      TraceEntryFinder.findCorrespondingEntry(trace, posFromEntry16),
    ).toEqual(trace.getEntry(5));
  });

  it("finds first lower (else first equal) entry if trace precedes position's trace in ui pipeline", () => {
    const posFromEntry14 = TracePosition.fromTraceEntry(trace.getEntry(3));
    expect(
      TraceEntryFinder.findCorrespondingEntry(traceWithFrames, posFromEntry14),
    ).toEqual(traceWithFrames.getEntry(0));

    const posFromEntry10 = TracePosition.fromTraceEntry(trace.getEntry(0));
    expect(
      TraceEntryFinder.findCorrespondingEntry(traceWithFrames, posFromEntry10),
    ).toEqual(traceWithFrames.getEntry(0));
  });

  it('finds last lower or equal entry if position has no entry', () => {
    expect(TraceEntryFinder.findCorrespondingEntry(trace, posTs10)).toEqual(
      trace.getEntry(0),
    );

    const ts13 = TimestampConverterUtils.makeRealTimestamp(13n);
    const posTs13 = TracePosition.fromTimestamp(ts13);
    expect(TraceEntryFinder.findCorrespondingEntry(trace, posTs13)).toEqual(
      trace.getEntry(2),
    );
  });
});
