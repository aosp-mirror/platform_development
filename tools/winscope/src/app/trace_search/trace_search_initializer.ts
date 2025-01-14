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

import {Traces} from 'trace/traces';
import {TraceProcessorFactory} from 'trace_processor/trace_processor_factory';
import {SearchViewFactoryProtoLog} from './search_view_factory_protolog';
import {SearchViewFactorySf} from './search_view_factory_sf';
import {SearchViewFactoryTransactions} from './search_view_factory_transactions';
import {SearchViewFactoryTransitions} from './search_view_factory_transitions';

export class TraceSearchInitializer {
  static readonly FACTORIES = [
    SearchViewFactorySf,
    SearchViewFactoryTransactions,
    SearchViewFactoryTransitions,
    SearchViewFactoryProtoLog,
  ];

  static async createSearchViews(traces: Traces): Promise<string[]> {
    const traceProcessor = await TraceProcessorFactory.getSingleInstance();

    const searchViews: string[] = [];
    for (const FactoryType of TraceSearchInitializer.FACTORIES) {
      const factory = new FactoryType(traceProcessor);
      if (traces.getTrace(factory.traceType)?.isPerfetto()) {
        const views = await factory.createSearchViews();
        searchViews.push(...views);
      }
    }
    return searchViews;
  }
}

export interface SearchView {
  name: string;
  dataType: string;
  spec: Array<{name: string; desc: string}>;
  examples: Array<{query: string; desc: string}>;
}

export const SEARCH_VIEWS = TraceSearchInitializer.FACTORIES.flatMap(
  (factory) => factory.getPossibleSearchViews(),
);
