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

import {assertTrue} from 'common/assert_utils';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {UserNotifier} from 'common/user_notifier';
import {FailedToCreateTracesParser} from 'messaging/user_warnings';
import {TracesParserCujs} from 'parsers/events/traces_parser_cujs';
import {TracesParserInput} from 'parsers/input/perfetto/traces_parser_input';
import {TracesParserTransitions} from 'parsers/transitions/legacy/traces_parser_transitions';
import {Parser} from 'trace/parser';
import {Traces} from 'trace/traces';

export class TracesParserFactory {
  static readonly PARSERS = [
    TracesParserCujs,
    TracesParserTransitions,
    TracesParserInput,
  ];

  async createParsers(
    traces: Traces,
    timestampConverter: ParserTimestampConverter,
  ): Promise<Array<Parser<object>>> {
    const parsers: Array<Parser<object>> = [];

    for (const ParserType of TracesParserFactory.PARSERS) {
      let hasFoundParser = false;
      const parser = new ParserType(traces, timestampConverter);
      try {
        await parser.parse();
        hasFoundParser = true;
        assertTrue(parser.getLengthEntries() > 0, () => {
          const descriptors = parser.getDescriptors();
          return `${descriptors.join(', ')} ${
            descriptors.length > 1 ? 'files have' : 'has'
          } no relevant entries`;
        });
        parsers.push(parser);
      } catch (error) {
        if (hasFoundParser) {
          UserNotifier.add(
            new FailedToCreateTracesParser(
              parser.getTraceType(),
              (error as Error).message,
            ),
          );
        }
      }
    }

    return parsers;
  }
}
