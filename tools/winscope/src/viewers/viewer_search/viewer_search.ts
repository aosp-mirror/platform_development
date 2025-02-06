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
import {Store} from 'common/store/store';
import {TimestampConverter} from 'common/time/timestamp_converter';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {QueryResult} from 'trace_processor/query_result';
import {AbstractViewer} from 'viewers/abstract_viewer';
import {ViewType} from 'viewers/viewer';
import {Presenter} from './presenter';
import {UiData} from './ui_data';
import {ViewerSearchComponent} from './viewer_search_component';

export class ViewerSearch extends AbstractViewer<QueryResult> {
  static readonly DEPENDENCIES: TraceType[] = [TraceType.SEARCH];

  private traces: Traces | undefined;

  constructor(
    traces: Traces,
    store: Store,
    timestampConverter: TimestampConverter,
  ) {
    super(undefined, traces, 'viewer-search', store, timestampConverter);
  }

  override getTraces(): Array<Trace<QueryResult>> {
    return assertDefined(this.traces).getTraces(TraceType.SEARCH);
  }

  protected override initializePresenter(
    trace: Trace<QueryResult>,
    traces: Traces,
    store: Store,
    timestampConverter: TimestampConverter,
  ): Presenter {
    this.traces = traces;
    const notifyViewCallback = (data: UiData) => {
      (this.htmlElement as unknown as ViewerSearchComponent).inputData = data;
    };
    return new Presenter(traces, store, notifyViewCallback, timestampConverter);
  }

  protected override getTraceTypeForViewTitle(): TraceType {
    return TraceType.SEARCH;
  }

  protected override getViewType(): ViewType {
    return ViewType.GLOBAL_SEARCH;
  }
}
