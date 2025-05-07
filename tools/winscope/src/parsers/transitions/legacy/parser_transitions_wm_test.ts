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

import {assertDefined} from 'common/assert_utils';
import {
  TimestampConverterUtils,
  timestampEqualityTester,
} from 'common/time/test_utils';
import {com} from 'protos/transitions/udc/static';
import {LegacyParserProvider} from 'test/unit/fixture_utils';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';

describe('ParserTransitionsWm', () => {
  let parser: Parser<com.android.server.wm.shell.ITransition>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = await new LegacyParserProvider()
      .addFilename('traces/elapsed_and_real_timestamp/wm_transition_trace.pb')
      .getParser<com.android.server.wm.shell.ITransition>();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.WM_TRANSITION);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());
    expect(timestamps.length).toEqual(8);
    const expected = TimestampConverterUtils.makeZeroTimestamp();
    timestamps.forEach((timestamp) => expect(timestamp).toEqual(expected));
  });

  it('provides decoded proto', async () => {
    const entry = await parser.getEntry(0);
    expect(entry.id).toEqual(6);
    expect(entry.startTransactionId?.toString()).toEqual('13086765351818');
    expect(entry.sendTimeNs?.toString()).toEqual('57649646973488');
  });
});
