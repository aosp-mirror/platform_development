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

import {FakeProto} from 'parsers/perfetto/fake_proto_builder';
import {FakeProtoTransformer} from 'parsers/perfetto/fake_proto_transformer';
import {TamperedProtoField} from 'parsers/tampered_message_type';

export class FakeProtoTransformerSf extends FakeProtoTransformer {
  protected override shouldCheckIfPrimitiveLeaf(
    proto: FakeProto,
    field: TamperedProtoField
  ): boolean {
    return !field.repeated && proto !== null && proto !== undefined;
  }

  protected override getDefaultValue(proto: FakeProto, field: TamperedProtoField) {
    return field.repeated ? [] : proto;
  }

  protected override getValuesByIdProto(
    enumId: FakeProto,
    valuesById: {[key: number]: string}
  ): FakeProto {
    return Number(enumId);
  }
}
