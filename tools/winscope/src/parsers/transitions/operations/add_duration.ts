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
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TIMESTAMP_FORMATTER} from 'trace/tree_node/formatters';
import {AddOperation} from 'trace/tree_node/operations/add_operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';

export class AddDuration extends AddOperation<PropertyTreeNode> {
  protected override makeProperties(
    value: PropertyTreeNode,
  ): PropertyTreeNode[] {
    const wmDataNode = assertDefined(value.getChildByName('wmData'));

    const sendTime: Timestamp | null | undefined = wmDataNode
      .getChildByName('sendTimeNs')
      ?.getValue();
    const finishTime: Timestamp | null | undefined = wmDataNode
      .getChildByName('finishTimeNs')
      ?.getValue();

    if (!sendTime || !finishTime) {
      return [];
    }

    const timeDiffNs = finishTime.minus(sendTime).getValueNs();

    const timeDiff =
      NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(timeDiffNs);

    const durationNode =
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeCalculatedProperty(
        value.id,
        'duration',
        timeDiff,
      );
    durationNode.setFormatter(TIMESTAMP_FORMATTER);

    return [durationNode];
  }
}
