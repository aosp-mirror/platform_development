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

import {AbstractParser} from 'parsers/perfetto/abstract_parser';
import {LogMessage} from 'parsers/protolog/log_message';
import {ParserProtologUtils} from 'parsers/protolog/parser_protolog_utils';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

class PerfettoLogMessageTableRow {
  message = '<NO_MESSAGE>';
  tag = '<NO_TAG>';
  level = '<NO_LEVEL>';
  location = '<NO_LOC>';
  timestamp: bigint = 0n;

  constructor(
      timestamp: bigint,
      tag: string,
      level: string,
      message: string,
      location: string
  ) {
    this.timestamp = timestamp ?? this.timestamp;
    this.tag = tag ?? this.tag;
    this.level = level ?? this.level;
    this.message = message ?? this.message;
    this.location = location ?? this.location;
  }
}

export class ParserProtolog extends AbstractParser<PropertyTreeNode> {
  override getTraceType(): TraceType {
    return TraceType.PROTO_LOG;
  }

  override async getEntry(index: number): Promise<PropertyTreeNode> {
    const protologEntry = await this.queryEntry(index);
    const logMessage: LogMessage = {
      text: protologEntry.message,
      tag: protologEntry.tag,
      level: protologEntry.level,
      at: protologEntry.location,
      timestamp: protologEntry.timestamp,
    };

    return ParserProtologUtils.makeMessagePropertiesTree(
      logMessage,
      this.timestampConverter,
      this.getRealToMonotonicTimeOffsetNs() !== undefined,
    );
  }

  protected override getTableName(): string {
    return 'protolog';
  }

  private async queryEntry(index: number): Promise<PerfettoLogMessageTableRow> {
    const sql = `
      SELECT
        ts, tag, level, message
      FROM
        protolog
      WHERE protolog.id = ${this.entryIndexToRowIdMap[index]};
    `;
    const result = await this.traceProcessor.query(sql).waitAllRows();

    if (result.numRows() !== 1) {
      throw new Error(
        `Expected exactly 1 protolog message with id ${index} but got ${result.numRows()}`,
      );
    }

    const entry = result.iter({});

    return new PerfettoLogMessageTableRow(
      entry.get('ts') as bigint,
      entry.get('tag') as string,
      entry.get('level') as string,
      entry.get('message') as string,
      entry.get('location') as string,
    );
  }
}
