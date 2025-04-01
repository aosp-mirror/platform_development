/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {assertDefined,} from 'common/assert_utils';
import {Timestamp} from 'common/time/time';
import {
  ColumnType,
  QueryResult,
  Row,
  RowIterator,
} from 'trace_processor/query_result';
import {TraceProcessorFactory} from 'trace_processor/trace_processor_factory';

export function makeSearchTraceSpies(
    ts?: Timestamp,
    value?: ColumnType,
  ): [jasmine.SpyObj<QueryResult>, jasmine.SpyObj<RowIterator<Row>>] {
  const spyQueryResult = jasmine.createSpyObj<QueryResult>('result', [
    'numRows',
    'columns',
    'iter',
  ]);
  spyQueryResult.numRows.and.returnValue(1);
  const columns: string[] = [];
  if (ts !== undefined) columns.push('ts');
  columns.push('property');
  if (value !== undefined) columns.push('value');
  spyQueryResult.columns.and.returnValue(columns);

  const spyIter = jasmine.createSpyObj<RowIterator<Row>>('iter', [
    'valid',
    'next',
    'get',
  ]);
  if (ts !== undefined) {
    spyIter.get.withArgs('ts').and.returnValue(ts.getValueNs());
  }
  spyIter.get.withArgs('property').and.returnValue('test_property');
  if (value !== undefined) {
    spyIter.get.withArgs('value').and.returnValue(value);
  }
  spyIter.valid.and.returnValue(true);
  spyIter.next.and.callFake(() =>
    assertDefined(spyIter).valid.and.returnValue(false),
  );
  spyQueryResult.iter.and.returnValue(spyIter);

  return [spyQueryResult, spyIter];
}

export async function runQueryAndGetResult(query: string): Promise<QueryResult> {
  const tp = await TraceProcessorFactory.getSingleInstance();
  return tp.queryAllRows(query);
}

function makeSpyQueryResult(): jasmine.SpyObj<QueryResult> {
  return jasmine.createSpyObj<QueryResult>('result', [
    'numRows',
    'firstRow',
  ]);
}

export function setNumRowsSpyQueryResult(
  rows: number,
  spyQueryResult?: jasmine.SpyObj<QueryResult>,
): jasmine.SpyObj<QueryResult> {
  const spy = spyQueryResult ?? makeSpyQueryResult();
  spy.numRows.and.returnValue(rows);
  return spy;
}

export function setFirstRowSpyQueryResult(
  query: string,
  tpSpy: jasmine.Spy,
  result: Row,
  spyQueryResult?: jasmine.SpyObj<QueryResult>,
): jasmine.SpyObj<QueryResult> {
  const spy = spyQueryResult ?? makeSpyQueryResult();
  spy.firstRow.and.returnValue(result);
  tpSpy.withArgs(query).and.returnValue(Promise.resolve(spy));
  return spy;
}
