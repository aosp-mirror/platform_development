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
import * as protobuf from 'protobufjs';

export class TamperedMessageType extends protobuf.Type {
  static tamper(protoType: protobuf.Type): TamperedMessageType {
    TamperedMessageType.tamperTypeDfs(protoType);
    return protoType as TamperedMessageType;
  }

  override fields: {[k: string]: TamperedProtoField} = {};

  private static tamperTypeDfs(protoType: protobuf.Type) {
    for (const fieldName of Object.keys(protoType.fields)) {
      const field = protoType.fields[fieldName];
      TamperedMessageType.tamperFieldDfs(field);
    }
  }

  private static tamperFieldDfs(field: protobuf.Field) {
    //TODO: lookupType/lookupEnum are expensive operations. To avoid calling them many times
    // during TreeNode Operation loops (e.g. SetFormatters, TranslateIntDef, AddDefaults),
    // we tamper protobuf.Field and protobuf.Type to provide a path linking a Field with
    // its corresponding Type, greatly improving latency in building a properties tree
    if ((field as TamperedProtoField).tamperedMessageType) {
      return;
    }

    try {
      (field as TamperedProtoField).tamperedMessageType =
        field.parent?.lookupType(field.type) as TamperedMessageType;
    } catch (e) {
      // swallow
    }

    try {
      (field as TamperedProtoField).tamperedEnumType = field.parent?.lookupEnum(
        field.type,
      );
    } catch (e) {
      // swallow
    }

    if ((field as TamperedProtoField).tamperedMessageType === undefined) {
      return;
    }

    TamperedMessageType.tamperTypeDfs(
      assertDefined((field as TamperedProtoField).tamperedMessageType),
    );
  }
}

export class TamperedProtoField extends protobuf.Field {
  tamperedMessageType: TamperedMessageType | undefined;
  tamperedEnumType: protobuf.Enum | undefined;
}
