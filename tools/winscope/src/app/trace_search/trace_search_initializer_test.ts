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

import {UnitTestUtils} from 'test/unit/utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {TraceSearchInitializer} from './trace_search_initializer';

describe('TraceSearchInitializer', () => {
  let traces: Traces;

  beforeEach(() => {
    traces = new Traces();
  });

  it('robust to no searchable traces', async () => {
    const views = await TraceSearchInitializer.createSearchViews(traces);
    expect(views).toEqual([]);
  });

  it('initializes surface flinger', async () => {
    const parser = await UnitTestUtils.getPerfettoParser(
      TraceType.SURFACE_FLINGER,
      'traces/perfetto/layers_trace.perfetto-trace',
    );
    const trace = Trace.fromParser(parser);
    traces.addTrace(trace);
    const views = await TraceSearchInitializer.createSearchViews(traces);
    expect(views).toEqual(['sf_layer_search', 'sf_hierarchy_root_search']);
    const queryResult = await UnitTestUtils.runQueryAndGetResult(`
      SELECT * FROM sf_layer_search
        WHERE layer_name LIKE 'Task%'
        AND property='flags'
        AND value!=previous_value
    `);
    expect(queryResult.numRows()).toEqual(2);

    const queryResultEntry = await UnitTestUtils.runQueryAndGetResult(`
      SELECT * FROM sf_hierarchy_root_search
        WHERE property LIKE 'displays[1]%'
        AND (
          flat_property='displays.layer_stack'
          OR flat_property='displays.is_virtual'
        )
    `);
    expect(queryResultEntry.numRows()).toEqual(40);
  });
});
