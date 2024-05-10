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

import * as protobuf from 'protobufjs';
import {FakeProto} from './fake_proto_builder';

export class FakeProtoTransformer {
  private root: protobuf.Root;
  private rootField: protobuf.Field;

  constructor(protoDefinitionJson: protobuf.INamespace, parentType: string, fieldName: string) {
    this.root = protobuf.Root.fromJSON(protoDefinitionJson);
    this.rootField = this.root.lookupType(parentType).fields[fieldName];
  }

  transform(proto: FakeProto): FakeProto {
    return this.transformRec(proto, this.rootField);
  }

  private transformRec(proto: FakeProto, field: protobuf.Field): FakeProto {
    // Leaf (primitive type)
    if (!field.repeated) {
      switch (field.type) {
        case 'double':
          return Number(proto ?? 0);
        case 'float':
          return Number(proto ?? 0);
        case 'int32':
          return Number(proto ?? 0);
        case 'uint32':
          return Number(proto ?? 0);
        case 'sint32':
          return Number(proto ?? 0);
        case 'fixed32':
          return Number(proto ?? 0);
        case 'sfixed32':
          return Number(proto ?? 0);
        case 'int64':
          return BigInt(proto ?? 0);
        case 'uint64':
          return BigInt(proto ?? 0);
        case 'sint64':
          return BigInt(proto ?? 0);
        case 'fixed64':
          return BigInt(proto ?? 0);
        case 'sfixed64':
          return BigInt(proto ?? 0);
        case 'string':
          return proto;
        case 'bool':
          return Boolean(proto);
        case 'bytes':
          return proto;
        default:
        // do nothing
      }
    }

    // Leaf (enum)
    if (
      field.resolvedType &&
      field.resolvedType instanceof protobuf.Enum &&
      field.resolvedType.valuesById
    ) {
      return field.resolvedType.valuesById[Number(proto)];
    }

    // Leaf (enum)
    let enumType: protobuf.Enum | undefined;
    try {
      enumType = field.parent?.lookupEnum(field.type);
    } catch (e) {
      // do nothing
    }
    const enumId = this.tryGetEnumId(proto);
    if (enumType && enumId !== undefined) {
      return enumType.valuesById[enumId];
    }

    // Leaf (default value)
    if (proto === null || proto === undefined) {
      return field.repeated ? [] : field.defaultValue;
    }

    let protoType: protobuf.Type | undefined;
    try {
      protoType = this.root.lookupType(field.type);
    } catch (e) {
      return proto;
    }

    for (const childName in protoType.fields) {
      if (!Object.prototype.hasOwnProperty.call(protoType.fields, childName)) {
        continue;
      }
      const childField = protoType.fields[childName];

      if (Array.isArray(proto[childName])) {
        for (let i = 0; i < proto[childName].length; ++i) {
          proto[childName][i] = this.transformRec(proto[childName][i], childField);
        }
      } else {
        proto[childName] = this.transformRec(proto[childName], childField);
      }
    }

    return proto;
  }

  private tryGetEnumId(proto: FakeProto): number | undefined {
    if (proto === null || proto === undefined) {
      return 0;
    }

    switch (typeof proto) {
      case 'number':
        return proto;
      case 'bigint':
        return Number(proto);
      default:
        return undefined;
    }
  }
}
