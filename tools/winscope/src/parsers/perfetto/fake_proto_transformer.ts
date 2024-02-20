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

import {
  TamperedMessageType,
  TamperedProtoField,
} from 'parsers/tampered_message_type';
import {FakeProto} from './fake_proto_builder';

export class FakeProtoTransformer {
  constructor(private readonly rootMessageType: TamperedMessageType) {}

  transform(proto: FakeProto): FakeProto {
    return this.transformMessageRec(proto, this.rootMessageType);
  }

  private transformFieldRec(
    proto: FakeProto,
    field: TamperedProtoField,
  ): FakeProto {
    // Leaf (primitive type)
    if (this.shouldCheckIfPrimitiveLeaf(proto, field)) {
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
    const enumId = this.tryGetEnumId(proto);
    if (field.tamperedEnumType && enumId !== undefined) {
      return Number(enumId);
    }

    // Leaf (default value)
    if (proto === null || proto === undefined) {
      return this.getDefaultValue(proto, field);
    }

    if (!field.tamperedMessageType) {
      return proto;
    }

    // Field is message -> continue recursion
    return this.transformMessageRec(proto, field.tamperedMessageType);
  }

  private transformMessageRec(
    proto: FakeProto,
    messageType: TamperedMessageType,
  ): FakeProto {
    for (const childName in messageType.fields) {
      if (
        !Object.prototype.hasOwnProperty.call(messageType.fields, childName)
      ) {
        continue;
      }
      const childField = messageType.fields[childName];

      if (Array.isArray(proto[childName])) {
        for (let i = 0; i < proto[childName].length; ++i) {
          proto[childName][i] = this.transformFieldRec(
            proto[childName][i],
            childField,
          );
        }
      } else {
        proto[childName] = this.transformFieldRec(proto[childName], childField);
      }
    }

    return proto;
  }

  private shouldCheckIfPrimitiveLeaf(
    proto: FakeProto,
    field: TamperedProtoField,
  ): boolean {
    return !field.repeated && proto !== null && proto !== undefined;
  }

  private getDefaultValue(proto: FakeProto, field: TamperedProtoField) {
    return field.repeated ? [] : proto;
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
