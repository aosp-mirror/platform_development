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

import {TamperedMessageType} from 'parsers/tampered_message_type';
import root from 'protos/test/fake_proto/json';
import {FakeProtoTransformer} from './fake_proto_transformer';

describe('FakeProtoTransformer', () => {
  let transformer: FakeProtoTransformer;

  beforeAll(() => {
    transformer = new FakeProtoTransformer(
      TamperedMessageType.tamper(root.lookupType('Entry')),
    );
  });

  it('sets default value (empty array) of array fields', () => {
    const proto = {};
    const transformed = transformer.transform(proto);
    expect(transformed.array).toEqual([]);
  });

  it('does not decode enum fields', () => {
    const proto = {
      enum0: 1n,
      enum1: 1n,
    };
    const transformed = transformer.transform(proto);
    expect(transformed.enum0).toEqual(1);
    expect(transformed.enum1).toEqual(1);
  });

  it('converts fields to number if 32-bits type', () => {
    const proto = {
      number_32bit: 32n,
    };
    const transformed = transformer.transform(proto);
    expect(transformed.number_32bit).toEqual(32);
  });

  it('converts fields to bigint if 64-bits type', () => {
    const proto = {
      number_64bit: 64,
    };
    const transformed = transformer.transform(proto);
    expect(transformed.number_64bit).toEqual(64n);
  });
});
