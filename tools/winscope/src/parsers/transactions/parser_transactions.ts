/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {Timestamp, TimestampType} from 'common/time';
import {AbstractParser} from 'parsers/abstract_parser';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/transactions/udc/json';
import {android} from 'protos/transactions/udc/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {TranslateChanges} from './operations/translate_changes';

class ParserTransactions extends AbstractParser<PropertyTreeNode> {
  private static readonly MAGIC_NUMBER = [
    0x09, 0x54, 0x4e, 0x58, 0x54, 0x52, 0x41, 0x43, 0x45,
  ]; // .TNXTRACE

  private static readonly TransactionsTraceFileProto =
    TamperedMessageType.tamper(
      root.lookupType('android.surfaceflinger.TransactionTraceFile'),
    );
  private static readonly TransactionsTraceEntryField =
    ParserTransactions.TransactionsTraceFileProto.fields['entry'];

  private static readonly OPERATIONS = [
    new AddDefaults(ParserTransactions.TransactionsTraceEntryField),
    new SetFormatters(ParserTransactions.TransactionsTraceEntryField),
    new TranslateChanges(),
  ];

  private realToElapsedTimeOffsetNs: undefined | bigint;

  override getTraceType(): TraceType {
    return TraceType.TRANSACTIONS;
  }

  override getMagicNumber(): number[] {
    return ParserTransactions.MAGIC_NUMBER;
  }

  override decodeTrace(
    buffer: Uint8Array,
  ): android.surfaceflinger.proto.ITransactionTraceEntry[] {
    const decodedProto = ParserTransactions.TransactionsTraceFileProto.decode(
      buffer,
    ) as android.surfaceflinger.proto.ITransactionTraceFile;

    const timeOffset = BigInt(
      decodedProto.realToElapsedTimeOffsetNanos?.toString() ?? '0',
    );
    this.realToElapsedTimeOffsetNs = timeOffset !== 0n ? timeOffset : undefined;

    return decodedProto.entry ?? [];
  }

  override getTimestamp(
    type: TimestampType,
    entryProto: android.surfaceflinger.proto.ITransactionTraceEntry,
  ): undefined | Timestamp {
    const elapsedRealtimeNanos = BigInt(
      assertDefined(entryProto.elapsedRealtimeNanos).toString(),
    );
    if (
      this.timestampFactory.canMakeTimestampFromType(
        type,
        this.realToElapsedTimeOffsetNs,
      )
    ) {
      return this.timestampFactory.makeTimestampFromType(
        type,
        elapsedRealtimeNanos,
        this.realToElapsedTimeOffsetNs,
      );
    }
    return undefined;
  }

  override processDecodedEntry(
    index: number,
    timestampType: TimestampType,
    entryProto: android.surfaceflinger.proto.ITransactionTraceEntry,
  ): PropertyTreeNode {
    return this.makePropertiesTree(entryProto);
  }

  override customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VSYNCID, async () => {
        return this.decodedEntries
          .slice(entriesRange.start, entriesRange.end)
          .map((entry) => {
            return BigInt(entry.vsyncId.toString()); // convert Long to bigint
          });
      })
      .getResult();
  }

  private makePropertiesTree(
    entryProto: android.surfaceflinger.proto.ITransactionTraceEntry,
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData(entryProto)
      .setRootId('TransactionsTraceEntry')
      .setRootName('entry')
      .build();

    ParserTransactions.OPERATIONS.forEach((operation) => {
      operation.apply(tree);
    });
    return tree;
  }
}

export {ParserTransactions};
