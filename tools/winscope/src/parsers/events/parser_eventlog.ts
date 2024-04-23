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

import {StringUtils} from 'common/string_utils';
import {Timestamp} from 'common/time';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

class ParserEventLog extends AbstractParser<PropertyTreeNode> {
  private static readonly MAGIC_NUMBER_STRING = 'EventLog';
  private static readonly MAGIC_NUMBER: number[] = Array.from(
    new TextEncoder().encode(ParserEventLog.MAGIC_NUMBER_STRING),
  );

  override getTraceType(): TraceType {
    return TraceType.EVENT_LOG;
  }

  override getMagicNumber(): number[] {
    return ParserEventLog.MAGIC_NUMBER;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override decodeTrace(buffer: Uint8Array): Event[] {
    const decodedLogs = this.decodeByteArray(buffer);
    const events = this.parseLogs(decodedLogs);
    return events.sort((a: Event, b: Event) => {
      return a.eventTimestamp < b.eventTimestamp ? -1 : 1;
    });
  }

  protected override getTimestamp(entry: Event): Timestamp {
    return this.timestampConverter.makeTimestampFromRealNs(
      entry.eventTimestamp,
    );
  }

  override processDecodedEntry(index: number, entry: Event): PropertyTreeNode {
    return new PropertyTreeBuilderFromProto()
      .setData(entry)
      .setRootId('EventLogTrace')
      .setRootName('event')
      .build();
  }

  private decodeByteArray(bytes: Uint8Array): string[] {
    const allLogsString = new TextDecoder().decode(bytes);
    const splitLogs = allLogsString.split('\n');

    const firstIndexOfEventLogTrace = splitLogs.findIndex((substring) => {
      return (
        !substring.includes(ParserEventLog.MAGIC_NUMBER_STRING) &&
        !substring.includes('beginning of events') &&
        !StringUtils.isBlank(substring)
      );
    });

    const lastIndexOfEventLogTrace = splitLogs.findIndex((substring, index) => {
      return (
        index > firstIndexOfEventLogTrace && StringUtils.isBlank(substring)
      );
    });

    if (lastIndexOfEventLogTrace === -1) {
      return splitLogs.slice(firstIndexOfEventLogTrace);
    }
    return splitLogs.slice(firstIndexOfEventLogTrace, lastIndexOfEventLogTrace);
  }

  private parseLogs(input: string[]): Event[] {
    return input.map((log) => {
      const [metaData, eventData] = log
        .split(':', 2)
        .map((string) => string.trim());
      const [rawTimestamp, uid, pid, tid, priority, tag] = metaData
        .split(' ')
        .filter((substring) => substring.length > 0);
      const timestampNs = BigInt(rawTimestamp.replace('.', ''));
      return {
        eventTimestamp: timestampNs,
        pid: Number(pid),
        uid: Number(uid),
        tid: Number(tid),
        tag,
        eventData,
      };
    });
  }
}

interface Event {
  eventTimestamp: bigint;
  pid: number;
  uid: number;
  tid: number;
  tag: string;
  eventData: string;
}

export {ParserEventLog};
