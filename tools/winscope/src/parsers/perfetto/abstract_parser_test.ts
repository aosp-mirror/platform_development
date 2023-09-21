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
import {UnitTestUtils} from 'test/unit/utils';
import {TraceType} from 'trace/trace_type';

describe('Perfetto AbstractParser', () => {
  it('fails parsing if there are no trace entries', async () => {
    const parsers = await UnitTestUtils.getPerfettoParsers(
      'traces/perfetto/no_winscope_traces.perfetto-trace'
    );
    expect(parsers.length).toEqual(0);
  });

  it('retrieves partial trace entries', async () => {
    {
      const parser = await UnitTestUtils.getPerfettoParser(
        TraceType.SURFACE_FLINGER,
        'traces/perfetto/layers_trace.perfetto-trace'
      );
      const entries = await parser.getPartialProtos({start: 0, end: 3}, 'vsyncId');
      expect(entries).toEqual([{vsyncId: 4891n}, {vsyncId: 5235n}, {vsyncId: 5748n}]);
    }
    {
      const parser = await UnitTestUtils.getPerfettoParser(
        TraceType.TRANSACTIONS,
        'traces/perfetto/transactions_trace.perfetto-trace'
      );
      const entries = await parser.getPartialProtos({start: 0, end: 3}, 'vsyncId');
      expect(entries).toEqual([{vsyncId: 1n}, {vsyncId: 2n}, {vsyncId: 3n}]);
    }
  });
});
