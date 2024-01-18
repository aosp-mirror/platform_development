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

import {TamperedMessageType} from 'parsers/tampered_message_type';
import {AddOperation} from 'trace/tree_node/operations/add_operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {PropertyTreeNodeFactory} from 'trace/tree_node/property_tree_node_factory';

export class AddDefaults extends AddOperation<PropertyTreeNode> {
  constructor(
    private readonly protoType: TamperedMessageType,
    private readonly propertyAllowlist?: string[],
    private readonly propertyDenylist?: string[]
  ) {
    super();
  }

  override makeProperties(
    factory: PropertyTreeNodeFactory,
    value: PropertyTreeNode
  ): PropertyTreeNode[] {
    const defaultPropertyNodes: PropertyTreeNode[] = [];

    for (const fieldName in this.protoType.fields) {
      if (this.propertyAllowlist && !this.propertyAllowlist.includes(fieldName)) {
        continue;
      }

      if (this.propertyDenylist && this.propertyDenylist.includes(fieldName)) {
        continue;
      }

      if (!Object.prototype.hasOwnProperty.call(this.protoType.fields, fieldName)) {
        continue;
      }

      const field = this.protoType.fields[fieldName];
      let existingNode = value.getChildByName(fieldName);
      let defaultValue: any = field.repeated ? [] : field.defaultValue;

      if (!field.repeated && defaultValue === null) {
        switch (field.type) {
          case 'double':
            defaultValue = 0;
            break;
          case 'float':
            defaultValue = 0;
            break;
          case 'int32':
            defaultValue = 0;
            break;
          case 'uint32':
            defaultValue = 0;
            break;
          case 'sint32':
            defaultValue = 0;
            break;
          case 'fixed32':
            defaultValue = 0;
            break;
          case 'sfixed32':
            defaultValue = 0;
            break;
          case 'int64':
            defaultValue = BigInt(0);
            break;
          case 'uint64':
            defaultValue = BigInt(0);
            break;
          case 'sint64':
            defaultValue = BigInt(0);
            break;
          case 'fixed64':
            defaultValue = BigInt(0);
            break;
          case 'sfixed64':
            defaultValue = BigInt(0);
            break;
          case 'bool':
            defaultValue = Boolean(defaultValue);
            break;
          default:
          //do nothing
        }
      }

      if (
        existingNode &&
        (existingNode.getValue() === field.defaultValue ||
          (existingNode.getValue() === undefined && existingNode.getAllChildren().length === 0))
      ) {
        value.removeChild(existingNode.id);
        existingNode = undefined;
      }

      if (!existingNode) {
        existingNode = factory.makeDefaultProperty(value.id, fieldName, defaultValue);
        defaultPropertyNodes.push(existingNode);
        continue;
      }

      if (field.tamperedMessageType) {
        const operation = new AddDefaults(field.tamperedMessageType);
        if (field.repeated) {
          existingNode.getAllChildren().forEach((child) => operation.apply(child));
        } else {
          operation.apply(existingNode);
        }
      }
    }

    return defaultPropertyNodes;
  }
}
