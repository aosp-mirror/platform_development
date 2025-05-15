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
import {MakeTimestampStrategyType} from 'common/time/time';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {TransformToTimestamp} from 'parsers/operations/transform_to_timestamp';
import {TranslateIntDef} from 'parsers/operations/translate_intdef';
import {PropertyTreeBuilderFromProto} from 'parsers/property_tree_builder_from_proto';
import {TAMPERED_TRACE_PACKET} from 'parsers/tampered_message_type';
import {AddDuration} from 'parsers/transitions/operations/add_duration';
import {AddRootProperties} from 'parsers/transitions/operations/add_root_properties';
import {AddStatus} from 'parsers/transitions/operations/add_status';
import {UpdateAbortTimeNodes} from 'parsers/transitions/operations/update_abort_time_nodes';
import {TransitionType} from 'parsers/transitions/transition_type';
import {perfetto} from 'protos/perfetto/trace/static';
import {
  EnumFormatter,
  PropertyFormatter,
  TIMESTAMP_NODE_FORMATTER,
} from 'trace/tree_node/formatters';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

interface TransitionInfo {
  entry: perfetto.protos.IShellTransition;
  timestampConverter: ParserTimestampConverter;
  handlerMapping?: {[key: number]: string};
}

export class EntryPropertiesTreeFactory {
  private static readonly SHELL_PROPERTIES = [
    'dispatchTimeNs',
    'mergeTimeNs',
    'mergeRequestTimeNs',
    'shellAbortTimeNs',
    'handler',
    'mergeTarget',
  ];
  private static readonly WM_PROPERTIES = [
    'createTimeNs',
    'sendTimeNs',
    'wmAbortTimeNs',
    'finishTimeNs',
    'startTransactionId',
    'finishTransactionId',
    'type',
    'targets',
    'flags',
    'startingWindowRemoveTimeNs',
  ];

  private static readonly TRANSITION_OPERATIONS = [
    new AddDuration(),
    new AddStatus(),
    new AddRootProperties(),
  ];
  private static readonly TransitionField =
    TAMPERED_TRACE_PACKET.fields['shellTransition'];
  private static readonly WM_ADD_DEFAULTS_OPERATION = new AddDefaults(
    EntryPropertiesTreeFactory.TransitionField,
    ['type', 'targets'],
  );
  private static readonly WM_INTDEF_OPERATION = new TranslateIntDef(
    EntryPropertiesTreeFactory.TransitionField,
  );
  private static readonly SET_FORMATTERS_OPERATION = new SetFormatters();
  private static readonly UPDATE_ABORT_TIME_OPERATION =
    new UpdateAbortTimeNodes();
  private static readonly TRANSITION_TYPE_FORMATTER = new EnumFormatter(
    TransitionType,
  );

  static makeTransitionPropertiesTree(
    shellEntryTree: PropertyTreeNode,
    wmEntryTree: PropertyTreeNode,
  ): PropertyTreeNode {
    const transitionTree = new PropertyTreeNode(
      wmEntryTree.id,
      wmEntryTree.name,
      wmEntryTree.source,
      undefined,
    );

    transitionTree.addOrReplaceChild(
      assertDefined(shellEntryTree.getChildByName('shellData')),
    );
    transitionTree.addOrReplaceChild(
      assertDefined(wmEntryTree.getChildByName('wmData')),
    );
    EntryPropertiesTreeFactory.TRANSITION_OPERATIONS.forEach((operation) =>
      operation.apply(transitionTree),
    );
    return transitionTree;
  }

  static makeWmPropertiesTree(info?: TransitionInfo): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData({wmData: info?.entry ?? null})
      .setRootId('TransitionTraceEntry')
      .setRootName('Selected Transition')
      .setDenyList(EntryPropertiesTreeFactory.SHELL_PROPERTIES)
      .build();

    if (!info) {
      EntryPropertiesTreeFactory.SET_FORMATTERS_OPERATION.apply(tree);
      return tree;
    }

    EntryPropertiesTreeFactory.UPDATE_ABORT_TIME_OPERATION.apply(tree);

    const wmDataNode = assertDefined(tree.getChildByName('wmData'));
    EntryPropertiesTreeFactory.WM_ADD_DEFAULTS_OPERATION.apply(wmDataNode);
    new TransformToTimestamp(
      [
        'abortTimeNs',
        'createTimeNs',
        'sendTimeNs',
        'finishTimeNs',
        'startingWindowRemoveTimeNs',
      ],
      EntryPropertiesTreeFactory.makeTimestampStrategy(info.timestampConverter),
    ).apply(wmDataNode);

    const customFormatters = new Map<string, PropertyFormatter>([
      ['type', EntryPropertiesTreeFactory.TRANSITION_TYPE_FORMATTER],
      ['mode', EntryPropertiesTreeFactory.TRANSITION_TYPE_FORMATTER],
      ['abortTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['createTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['sendTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['finishTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['startingWindowRemoveTimeNs', TIMESTAMP_NODE_FORMATTER],
    ]);

    new SetFormatters(undefined, customFormatters).apply(tree);

    EntryPropertiesTreeFactory.WM_INTDEF_OPERATION.apply(tree);
    return tree;
  }

  static makeShellPropertiesTree(info?: TransitionInfo): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData({shellData: info?.entry ?? null})
      .setRootId('TransitionTraceEntry')
      .setRootName('Selected Transition')
      .setDenyList(EntryPropertiesTreeFactory.WM_PROPERTIES)
      .build();

    if (!info) {
      EntryPropertiesTreeFactory.SET_FORMATTERS_OPERATION.apply(tree);
      return tree;
    }

    EntryPropertiesTreeFactory.UPDATE_ABORT_TIME_OPERATION.apply(tree);

    const shellDataNode = assertDefined(tree.getChildByName('shellData'));
    new TransformToTimestamp(
      ['dispatchTimeNs', 'mergeRequestTimeNs', 'mergeTimeNs', 'abortTimeNs'],
      EntryPropertiesTreeFactory.makeTimestampStrategy(info.timestampConverter),
    ).apply(shellDataNode);

    const customFormatters = new Map<string, PropertyFormatter>([
      ['type', EntryPropertiesTreeFactory.TRANSITION_TYPE_FORMATTER],
      ['mode', EntryPropertiesTreeFactory.TRANSITION_TYPE_FORMATTER],
      ['dispatchTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['mergeRequestTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['mergeTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['abortTimeNs', TIMESTAMP_NODE_FORMATTER],
    ]);

    if (info.handlerMapping) {
      customFormatters.set('handler', new EnumFormatter(info.handlerMapping));
    }

    new SetFormatters(undefined, customFormatters).apply(tree);

    return tree;
  }

  private static makeTimestampStrategy(
    timestampConverter: ParserTimestampConverter,
  ): MakeTimestampStrategyType {
    return (valueNs: bigint) => {
      return timestampConverter.makeTimestampFromBootTimeNs(valueNs);
    };
  }
}
