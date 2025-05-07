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
import {ParserTransitionsShell} from './parser_transitions_shell';

describe('ParserTransitionsShell', () => {
  let parser: Parser<com.android.wm.shell.Transition>;

  beforeAll(async () => {
    jasmine.addCustomEqualityTester(timestampEqualityTester);
    parser = await new LegacyParserProvider()
      .addFilename(
        'traces/elapsed_and_real_timestamp/shell_transition_trace.pb',
      )
      .getParser<com.android.wm.shell.Transition>();
  });

  it('has expected trace type', () => {
    expect(parser.getTraceType()).toEqual(TraceType.SHELL_TRANSITION);
  });

  it('has expected coarse version', () => {
    expect(parser.getCoarseVersion()).toEqual(CoarseVersion.LEGACY);
  });

  it('provides timestamps', () => {
    const timestamps = assertDefined(parser.getTimestamps());
    const expected = [
      TimestampConverterUtils.makeRealTimestamp(1683188477607285317n),
      TimestampConverterUtils.makeRealTimestamp(1683130827957362976n),
      TimestampConverterUtils.makeRealTimestamp(1683130827957362976n),
      TimestampConverterUtils.makeRealTimestamp(1683188479256449868n),
      TimestampConverterUtils.makeRealTimestamp(1683130827957362976n),
      TimestampConverterUtils.makeRealTimestamp(1683130827957362976n),
    ];
    expect(timestamps).toEqual(expected);
  });

  it('provides decoded proto', async () => {
    const entry = await parser.getEntry(0);
    expect(entry.id).toEqual(6);
    expect(entry.dispatchTimeNs.toString()).toEqual('57649649922341');
    expect(entry.handler).toEqual(2);
  });

  it('provides shell mapping', async () => {
    expect(parser).toBeInstanceOf(ParserTransitionsShell);
    const mapping = (
      parser as unknown as ParserTransitionsShell
    ).getShellHandlerMapping();
    expect(mapping.length).toEqual(2);
    expect(mapping[0].id).toEqual(2);
    expect(mapping[0].name).toEqual(
      'com.android.wm.shell.transition.DefaultMixedHandler',
    );
    expect(mapping[1].id).toEqual(3);
    expect(mapping[1].name).toEqual(
      'com.android.wm.shell.recents.RecentsTransitionHandler',
    );
  });
});
