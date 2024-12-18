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
import {Timestamp} from 'common/time';
import {ParserTimestampConverter} from 'common/timestamp_converter';
import {AddDefaults} from 'parsers/operations/add_defaults';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {
  MakeTimestampStrategyType,
  TransformToTimestamp,
} from 'parsers/operations/transform_to_timestamp';
import {TamperedMessageType} from 'parsers/tampered_message_type';
import {perfetto} from 'protos/transitions/latest/static';
import root from 'protos/transitions/udc/json';
import {com} from 'protos/transitions/udc/static';
import {
  EnumFormatter,
  PropertyFormatter,
  TIMESTAMP_NODE_FORMATTER,
} from 'trace/tree_node/formatters';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AddDuration} from './operations/add_duration';
import {AddRealToBootTimeOffsetTimestamp} from './operations/add_real_to_elapsed_time_offset_timestamp';
import {AddRootProperties} from './operations/add_root_properties';
import {AddStatus} from './operations/add_status';
import {UpdateAbortTimeNodes} from './operations/update_abort_time_nodes';
import {TransitionType} from './transition_type';

interface TransitionInfo {
  entry:
    | com.android.server.wm.shell.ITransition
    | com.android.wm.shell.ITransition
    | perfetto.protos.IShellTransition;
  realToBootTimeOffsetNs: bigint | undefined;
  timestampConverter: ParserTimestampConverter;
  handlerMapping?: {[key: number]: string};
}

export class ParserTransitionsUtils {
  static readonly TRANSITION_OPERATIONS = [
    new AddDuration(),
    new AddStatus(),
    new AddRootProperties(),
  ];

  private static readonly TransitionTraceProto = TamperedMessageType.tamper(
    root.lookupType('com.android.server.wm.shell.TransitionTraceProto'),
  );
  private static readonly TransitionField =
    ParserTransitionsUtils.TransitionTraceProto.fields['transitions'];
  private static readonly WM_ADD_DEFAULTS_OPERATION = new AddDefaults(
    ParserTransitionsUtils.TransitionField,
    ['type', 'targets'],
  );
  private static readonly SET_FORMATTERS_OPERATION = new SetFormatters();
  private static readonly PERFETTO_TRANSITION_OPERATIONS = [
    new UpdateAbortTimeNodes(),
  ];
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
    ParserTransitionsUtils.TRANSITION_OPERATIONS.forEach((operation) =>
      operation.apply(transitionTree),
    );
    return transitionTree;
  }

  static makeWmPropertiesTree(
    info?: TransitionInfo,
    denylistProperties: string[] = [],
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData({wmData: info?.entry ?? null})
      .setRootId('TransitionTraceEntry')
      .setRootName('Selected Transition')
      .setDenyList(denylistProperties)
      .setVisitPrototype(false)
      .build();

    if (!info) {
      ParserTransitionsUtils.SET_FORMATTERS_OPERATION.apply(tree);
      return tree;
    }

    if (denylistProperties.length > 0) {
      ParserTransitionsUtils.PERFETTO_TRANSITION_OPERATIONS.forEach(
        (operation) => operation.apply(tree),
      );
    }

    let realToBootTimeOffsetTimestamp: Timestamp | undefined;

    if (info.realToBootTimeOffsetNs !== undefined) {
      realToBootTimeOffsetTimestamp =
        info.timestampConverter.makeTimestampFromRealNs(
          info.realToBootTimeOffsetNs,
        );
    }

    const wmDataNode = assertDefined(tree.getChildByName('wmData'));
    new AddRealToBootTimeOffsetTimestamp(realToBootTimeOffsetTimestamp).apply(
      wmDataNode,
    );
    ParserTransitionsUtils.WM_ADD_DEFAULTS_OPERATION.apply(wmDataNode);
    new TransformToTimestamp(
      [
        'abortTimeNs',
        'createTimeNs',
        'sendTimeNs',
        'finishTimeNs',
        'startingWindowRemoveTimeNs',
      ],
      ParserTransitionsUtils.makeTimestampStrategy(info.timestampConverter),
    ).apply(wmDataNode);

    const customFormatters = new Map<string, PropertyFormatter>([
      ['type', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
      ['mode', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
      ['abortTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['createTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['sendTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['finishTimeNs', TIMESTAMP_NODE_FORMATTER],
      ['startingWindowRemoveTimeNs', TIMESTAMP_NODE_FORMATTER],
    ]);

    new SetFormatters(undefined, customFormatters).apply(tree);
    return tree;
  }

  static makeShellPropertiesTree(
    info?: TransitionInfo,
    denylistProperties: string[] = [],
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData({shellData: info?.entry ?? null})
      .setRootId('TransitionTraceEntry')
      .setRootName('Selected Transition')
      .setDenyList(denylistProperties)
      .setVisitPrototype(false)
      .build();

    if (!info) {
      ParserTransitionsUtils.SET_FORMATTERS_OPERATION.apply(tree);
      return tree;
    }

    if (denylistProperties.length > 0) {
      ParserTransitionsUtils.PERFETTO_TRANSITION_OPERATIONS.forEach(
        (operation) => operation.apply(tree),
      );
    }

    let realToBootTimeOffsetTimestamp: Timestamp | undefined;
    if (info.realToBootTimeOffsetNs !== undefined) {
      realToBootTimeOffsetTimestamp =
        info.timestampConverter.makeTimestampFromRealNs(
          info.realToBootTimeOffsetNs,
        );
    }

    const shellDataNode = assertDefined(tree.getChildByName('shellData'));
    new AddRealToBootTimeOffsetTimestamp(realToBootTimeOffsetTimestamp).apply(
      shellDataNode,
    );
    new TransformToTimestamp(
      ['dispatchTimeNs', 'mergeRequestTimeNs', 'mergeTimeNs', 'abortTimeNs'],
      ParserTransitionsUtils.makeTimestampStrategy(info.timestampConverter),
    ).apply(shellDataNode);

    const customFormatters = new Map<string, PropertyFormatter>([
      ['type', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
      ['mode', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
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
