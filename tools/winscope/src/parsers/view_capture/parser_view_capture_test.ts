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
import {Timestamp, TimestampType} from 'common/time';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';

describe('ParserViewCapture', () => {
  let parser: Parser<object>;
  let trace: Trace<object>;

  beforeAll(async () => {
    parser = await UnitTestUtils.getParser(
      'traces/elapsed_and_real_timestamp/com.google.android.apps.nexuslauncher_0.vc'
    );
    trace = new TraceBuilder<object>()
      .setType(TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER)
      .setParser(parser)
      .build();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER);
  });

  it('provides elapsed timestamps', () => {
    const expected = [
      new Timestamp(TimestampType.ELAPSED, 181114412436130n),
      new Timestamp(TimestampType.ELAPSED, 181114421012750n),
      new Timestamp(TimestampType.ELAPSED, 181114429047540n),
    ];
    expect(parser.getTimestamps(TimestampType.ELAPSED)!.slice(0, 3)).toEqual(expected);
  });

  it('provides real timestamps', () => {
    const expected = [
      new Timestamp(TimestampType.REAL, 1691692936292808460n),
      new Timestamp(TimestampType.REAL, 1691692936301385080n),
      new Timestamp(TimestampType.REAL, 1691692936309419870n),
    ];
    expect(parser.getTimestamps(TimestampType.REAL)!.slice(0, 3)).toEqual(expected);
  });

  it('retrieves trace entry', async () => {
    const entry = (await parser.getEntry(1, TimestampType.REAL)) as any;
    expect(entry.timestamp).toBeTruthy();
    expect(entry.node).toBeTruthy();
  });

  it('supports VIEW_CAPTURE_PACKAGE_NAME custom query', async () => {
    const packageName = await trace.customQuery(CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME);
    expect(packageName).toEqual('com.google.android.apps.nexuslauncher');
  });
});
