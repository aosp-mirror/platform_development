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
import {StringUtils} from 'common/string_utils';
import {EventTag} from 'parsers/events/event_tag';
import {CujType} from 'trace/cuj_type';
import {AddOperation} from 'trace/tree_node/operations/add_operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {DEFAULT_PROPERTY_TREE_NODE_FACTORY} from 'trace/tree_node/property_tree_node_factory';

export class AddCujProperties extends AddOperation<PropertyTreeNode> {
  protected override makeProperties(
    value: PropertyTreeNode,
  ): PropertyTreeNode[] {
    const data = assertDefined(value.getChildByName('eventData')).getValue();
    const tag = assertDefined(value.getChildByName('tag')).getValue();
    const dataEntries = this.getDataEntries(data, tag);
    const cujType = this.getCujTypeFromData(dataEntries);
    const cujTag = this.getCujTagFromData(dataEntries, tag);

    const cujTimestamp = {
      unixNanos: this.getUnixNanosFromData(dataEntries),
      elapsedNanos: this.getElapsedNanosFromData(dataEntries),
      systemUptimeNanos: this.getSystemUptimeNanosFromData(dataEntries),
    };

    return [
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(
        value.id,
        'cujType',
        cujType,
      ),
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(
        value.id,
        'cujTimestamp',
        cujTimestamp,
      ),
      DEFAULT_PROPERTY_TREE_NODE_FACTORY.makeProtoProperty(
        value.id,
        'cujTag',
        cujTag,
      ),
    ];
  }

  private getDataEntries(data: string, tag: EventTag): string[] {
    let [cujType, unixNs, elapsedNs, uptimeNs, _tag] = ['', '', '', '', '', ''];
    if (tag === EventTag.JANK_CUJ_BEGIN_TAG) {
      // (CUJ Type|1|5),(Unix Time Ns|2|3),(Elapsed Time Ns|2|3),(Uptime Time Ns|2|3)
      [cujType, unixNs, elapsedNs, uptimeNs, _tag] = data
        .replace('[', '')
        .replace(']', '')
        .split(',');
    } else {
      [cujType, unixNs, elapsedNs, uptimeNs] = data
        .replace('[', '')
        .replace(']', '')
        .split(',');
    }

    if (
      !StringUtils.isNumeric(cujType) ||
      !StringUtils.isNumeric(unixNs) ||
      !StringUtils.isNumeric(elapsedNs) ||
      !StringUtils.isNumeric(uptimeNs)
    ) {
      throw new Error(`CUJ Data ${data} didn't match expected format`);
    }

    return data.slice(1, data.length - 2).split(',');
  }

  private getCujTypeFromData(dataEntries: string[]): number {
    const eventId = Number(dataEntries[0]);
    if (eventId in CujType) {
      return eventId;
    }
    return -1;
  }

  private getUnixNanosFromData(dataEntries: string[]): bigint {
    return BigInt(dataEntries[1]);
  }

  private getElapsedNanosFromData(dataEntries: string[]): bigint {
    return BigInt(dataEntries[2]);
  }

  private getSystemUptimeNanosFromData(dataEntries: string[]): bigint {
    return BigInt(dataEntries[3]);
  }

  private getCujTagFromData(
    dataEntries: string[],
    tag: EventTag,
  ): string | null {
    return tag === EventTag.JANK_CUJ_BEGIN_TAG ? dataEntries[4] : null;
  }
}
