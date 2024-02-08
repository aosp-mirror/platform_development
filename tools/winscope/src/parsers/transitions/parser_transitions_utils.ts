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
import {TimestampType} from 'common/time';
import {SetFormatters} from 'parsers/operations/set_formatters';
import {perfetto} from 'protos/transitions/latest/static';
import {com} from 'protos/transitions/udc/static';
import {EnumFormatter, PropertyFormatter, TimestampFormatter} from 'trace/tree_node/formatters';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {AddDuration} from './operations/add_duration';
import {AddRealToElapsedTimeOffsetNs} from './operations/add_real_to_elapsed_time_offset_ns';
import {AddRootProperties} from './operations/add_root_properties';
import {AddStatus} from './operations/add_status';
import {UpdateAbortTimeNodes} from './operations/update_abort_time_nodes';
import {TransitionType} from './transition_type';

interface TransitionInfo {
  entry:
    | com.android.server.wm.shell.ITransition
    | com.android.wm.shell.ITransition
    | perfetto.protos.IShellTransition;
  realToElapsedTimeOffsetNs: bigint | undefined;
  timestampType: TimestampType;
  handlerMapping?: {[key: number]: string};
}

export class ParserTransitionsUtils {
  static readonly TRANSITION_OPERATIONS = [
    new AddDuration(),
    new AddStatus(),
    new AddRootProperties(),
  ];
  private static readonly PERFETTO_TRANSITION_OPERATIONS = [new UpdateAbortTimeNodes()];
  private static readonly TRANSITION_TYPE_FORMATTER = new EnumFormatter(TransitionType);

  static makeTransitionPropertiesTree(
    shellEntryTree: PropertyTreeNode,
    wmEntryTree: PropertyTreeNode
  ): PropertyTreeNode {
    const transitionTree = new PropertyTreeNode(
      wmEntryTree.id,
      wmEntryTree.name,
      wmEntryTree.source,
      undefined
    );

    transitionTree.addOrReplaceChild(assertDefined(shellEntryTree.getChildByName('shellData')));
    transitionTree.addOrReplaceChild(assertDefined(wmEntryTree.getChildByName('wmData')));
    ParserTransitionsUtils.TRANSITION_OPERATIONS.forEach((operation) =>
      operation.apply(transitionTree)
    );
    return transitionTree;
  }

  static makeWmPropertiesTree(
    info?: TransitionInfo,
    denylistProperties: string[] = []
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData({wmData: info?.entry ?? null})
      .setRootId('TransitionTraceEntry')
      .setRootName('Selected Transition')
      .setDenyList(denylistProperties)
      .setVisitPrototype(false)
      .build();

    if (!info) {
      new SetFormatters().apply(tree);
      return tree;
    }

    if (denylistProperties.length > 0) {
      ParserTransitionsUtils.PERFETTO_TRANSITION_OPERATIONS.forEach((operation) =>
        operation.apply(tree)
      );
    }

    const timestampFormatter = new TimestampFormatter(
      info.timestampType,
      info.realToElapsedTimeOffsetNs
    );

    const customFormatters = new Map<string, PropertyFormatter>([
      ['type', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
      ['mode', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
      ['abortTimeNs', timestampFormatter],
      ['createTimeNs', timestampFormatter],
      ['sendTimeNs', timestampFormatter],
      ['finishTimeNs', timestampFormatter],
      ['startingWindowRemoveTimeNs', timestampFormatter],
    ]);

    const wmDataNode = assertDefined(tree.getChildByName('wmData'));
    new AddRealToElapsedTimeOffsetNs(info.realToElapsedTimeOffsetNs).apply(wmDataNode);

    new SetFormatters(undefined, customFormatters).apply(tree);
    return tree;
  }

  static makeShellPropertiesTree(
    info?: TransitionInfo,
    denylistProperties: string[] = []
  ): PropertyTreeNode {
    const tree = new PropertyTreeBuilderFromProto()
      .setData({shellData: info?.entry ?? null})
      .setRootId('TransitionTraceEntry')
      .setRootName('Selected Transition')
      .setDenyList(denylistProperties)
      .setVisitPrototype(false)
      .build();

    if (!info) {
      new SetFormatters().apply(tree);
      return tree;
    }

    if (denylistProperties.length > 0) {
      ParserTransitionsUtils.PERFETTO_TRANSITION_OPERATIONS.forEach((operation) =>
        operation.apply(tree)
      );
    }

    const timestampFormatter = new TimestampFormatter(
      info.timestampType,
      info.realToElapsedTimeOffsetNs
    );

    const customFormatters = new Map<string, PropertyFormatter>([
      ['type', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
      ['mode', ParserTransitionsUtils.TRANSITION_TYPE_FORMATTER],
      ['dispatchTimeNs', timestampFormatter],
      ['mergeRequestTimeNs', timestampFormatter],
      ['mergeTimeNs', timestampFormatter],
      ['abortTimeNs', timestampFormatter],
    ]);

    if (info.handlerMapping) {
      customFormatters.set('handler', new EnumFormatter(info.handlerMapping));
    }

    const shellDataNode = assertDefined(tree.getChildByName('shellData'));
    new AddRealToElapsedTimeOffsetNs(info.realToElapsedTimeOffsetNs).apply(shellDataNode);
    new SetFormatters(undefined, customFormatters).apply(tree);

    return tree;
  }
}
