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

import {TraceType} from 'trace/trace_type';
import {AbstractSearchViewFactory} from './abstract_search_view_factory';
import {SearchView} from './trace_search_initializer';

export class SearchViewFactoryProtoLog extends AbstractSearchViewFactory {
  override readonly traceType = TraceType.PROTO_LOG;
  private static readonly VIEW: SearchView = {
    name: 'protolog',
    dataType: 'ProtoLog',
    columns: [
      {
        name: 'ts',
        desc: 'Timestamp of log',
      },
      {
        name: 'level',
        desc: 'Log level',
      },
      {
        name: 'tag',
        desc: 'Logging group tag',
      },
      {
        name: 'message',
        desc: 'Log message',
      },
      {
        name: 'stacktrace',
        desc: 'Stacktrace (if available)',
      },
      {
        name: 'location',
        desc: 'Code location from which message originates',
      },
    ],
    examples: [
      {
        query: `SELECT ts, message, location FROM protolog
WHERE message LIKE '%transition%'`,
        desc: 'returns logs with message containing "transition"',
      },
    ],
  };

  static getPossibleSearchViews(): SearchView[] {
    return [SearchViewFactoryProtoLog.VIEW];
  }

  override async createSearchViews(): Promise<string[]> {
    return [SearchViewFactoryProtoLog.VIEW.name];
  }
}
