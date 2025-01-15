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

import {assertDefined} from 'common/assert_utils';
import {UnitTestUtils} from 'test/unit/utils';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {SEARCH_VIEWS, TraceSearchInitializer} from './trace_search_initializer';

describe('TraceSearchInitializer', () => {
  it('robust to no searchable traces', async () => {
    const views = await TraceSearchInitializer.createSearchViews(new Traces());
    expect(views).toEqual([]);
  });

  it('initializes surface flinger', async () => {
    const parser = await UnitTestUtils.getPerfettoParser(
      TraceType.SURFACE_FLINGER,
      'traces/perfetto/layers_trace.perfetto-trace',
    );
    await createViewsAndTestExamples(parser, [
      'sf_layer_search',
      'sf_hierarchy_root_search',
    ]);
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

  it('initializes transactions', async () => {
    const parser = await UnitTestUtils.getPerfettoParser(
      TraceType.TRANSACTIONS,
      'traces/perfetto/transactions_trace.perfetto-trace',
    );
    await createViewsAndTestExamples(parser, ['transactions_search']);
    const queryResultTransaction = await UnitTestUtils.runQueryAndGetResult(`
      SELECT * FROM transactions_search
        WHERE flat_property='transactions.layer_changes.x'
        AND value!='0.0'
    `);
    expect(queryResultTransaction.numRows()).toEqual(3);

    const queryResultAddedLayer = await UnitTestUtils.runQueryAndGetResult(`
      SELECT * FROM transactions_search
        WHERE flat_property='added_layers.name'
        AND value='ImeContainer'
    `);
    expect(queryResultAddedLayer.numRows()).toEqual(1);
  });

  it('initializes protolog', async () => {
    const parser = await UnitTestUtils.getPerfettoParser(
      TraceType.PROTO_LOG,
      'traces/perfetto/protolog.perfetto-trace',
    );
    await createViewsAndTestExamples(parser, ['protolog']);
    const queryResult = await UnitTestUtils.runQueryAndGetResult(`
      SELECT * FROM protolog WHERE message LIKE '%string%'
    `);
    expect(queryResult.numRows()).toEqual(2);
  });

  it('initializes transitions', async () => {
    const parser = await UnitTestUtils.getPerfettoParser(
      TraceType.TRANSITION,
      'traces/perfetto/shell_transitions_trace.perfetto-trace',
    );
    await createViewsAndTestExamples(parser, ['transitions_search']);
    const queryResult = await UnitTestUtils.runQueryAndGetResult(`
      SELECT * FROM transitions_search
        WHERE flat_property='handler'
        AND value LIKE '%DefaultMixedHandler'
    `);
    expect(queryResult.numRows()).toEqual(2);
  });

  async function createViewsAndTestExamples(
    parser: Parser<object>,
    expectedViews: string[],
  ) {
    const trace = Trace.fromParser(parser);
    const traces = new Traces();
    traces.addTrace(trace);
    const views = await TraceSearchInitializer.createSearchViews(traces);
    expect(views).toEqual(expectedViews);
    for (const viewName of views) {
      const view = assertDefined(
        SEARCH_VIEWS.find((view) => view.name === viewName),
      );
      for (const example of view.examples) {
        await expectAsync(
          UnitTestUtils.runQueryAndGetResult(example.query),
        ).not.toBeRejected();
      }
    }
  }
});
