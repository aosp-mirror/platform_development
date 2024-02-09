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

import {TimestampType} from 'common/time';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {ELAPSED_TIMESTAMP_FORMATTER, TimestampFormatter} from 'trace/tree_node/formatters';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {LogMessage} from './log_message';

export class ParserProtologUtils {
  static makeMessagePropertiesTree(
    logMessage: LogMessage,
    timestampType: TimestampType,
    realToElapsedTimeOffsetNs: bigint | undefined
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData(logMessage)
      .setRootId('ProtoLogTrace')
      .setRootName('entry')
      .setVisitPrototype(false)
      .build();

    const timestampFormatter =
      timestampType === TimestampType.ELAPSED
        ? ELAPSED_TIMESTAMP_FORMATTER
        : new TimestampFormatter(timestampType, realToElapsedTimeOffsetNs);
    const customFormatters = new Map([['timestamp', timestampFormatter]]);
    new SetFormatters(undefined, customFormatters).apply(tree);
    return tree;
  }
}
