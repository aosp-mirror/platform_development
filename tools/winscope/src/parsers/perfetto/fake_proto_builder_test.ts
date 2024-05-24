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
import {FakeProto, FakeProtoBuilder} from './fake_proto_builder';
import {FakeProtoTransformer} from './fake_proto_transformer';

interface Arg {
  key: string;
  value_type: string;
  int_value?: bigint;
  real_value?: number;
  string_value?: string;
}

describe('FakeProtoBuilder', () => {
  it('boolean', () => {
    const args = [makeArg('a', false), makeArg('b', true)];
    const proto = buildFakeProto(args);
    expect(proto.a).toEqual(false);
    expect(proto.b).toEqual(true);
  });

  it('number', () => {
    const args = [makeArg('a', 1), makeArg('b', 10)];
    const proto = buildFakeProto(args);
    expect(proto.a).toEqual(1);
    expect(proto.b).toEqual(10);
  });

  it('bigint', () => {
    const args = [makeArg('a', 1n), makeArg('b', 10n)];
    const proto = buildFakeProto(args);
    expect(proto.a).toEqual(1n);
    expect(proto.b).toEqual(10n);
  });

  it('string', () => {
    const args = [makeArg('a', 'valuea'), makeArg('b', 'valueb')];
    const proto = buildFakeProto(args);
    expect(proto.a).toEqual('valuea');
    expect(proto.b).toEqual('valueb');
  });

  it('array', () => {
    const args = [
      // Note: intentional random order + gap
      makeArg('array[3]', 13),
      makeArg('array[1]', 11),
      makeArg('array[0]', 10),
    ];
    const proto = buildFakeProto(args);
    expect(proto.array).toEqual([10, 11, undefined, 13]);
  });

  it('handles complex structure', () => {
    const args = [
      makeArg('a.b', false),
      makeArg('a.numbers[0]', 10),
      makeArg('a.numbers[1]', 11),
      makeArg('a.objects[0].c.d', '20'),
      makeArg('a.objects[0].c.e', '21'),
      makeArg('a.objects[1].c', 21n),
    ];
    const proto = buildFakeProto(args);
    expect(proto.a.b).toEqual(false);
    expect(proto.a.numbers[0]).toEqual(10);
    expect(proto.a.numbers[1]).toEqual(11);
    expect(proto.a.objects[0].c.d).toEqual('20');
    expect(proto.a.objects[0].c.e).toEqual('21');
    expect(proto.a.objects[1].c).toEqual(21n);
  });

  it('converts snake_case to camelCase', () => {
    const args = [
      makeArg('_case_64bit', 10),
      makeArg('case_64bit', 11),
      makeArg('case_64bit_lsb', 12),
      makeArg('case_64_bit', 13),
      makeArg('case_64_bit_lsb', 14),
    ];
    const proto = buildFakeProto(args);

    // Check it matches the snake_case to camelCase conversion performed by protobuf library (within the transformer)
    const transformed = new FakeProtoTransformer(
      TamperedMessageType.tamper(root.lookupType('Entry')),
    ).transform(proto);

    expect(transformed._case_64bit).toEqual(10n);
    expect(transformed.case_64bit).toEqual(11n);
    expect(transformed.case_64bitLsb).toEqual(12n);
    expect(transformed.case_64Bit).toEqual(13n);
    expect(transformed.case_64BitLsb).toEqual(14n);
  });

  const makeArg = (key: string, value: any): Arg => {
    if (value === null) {
      return {key, value_type: 'null'};
    }

    switch (typeof value) {
      case 'boolean':
        return {key, value_type: 'bool', int_value: BigInt(value)};
      case 'bigint':
        return {key, value_type: 'int', int_value: value};
      case 'number':
        return {key, value_type: 'real', real_value: value};
      case 'string':
        return {key, value_type: 'string', string_value: value};
      default:
        throw new Error(`Unexpected value type: ${typeof value}`);
    }
  };

  const buildFakeProto = (args: Arg[]): FakeProto => {
    const builder = new FakeProtoBuilder();
    args.forEach((arg) => {
      builder.addArg(
        arg.key,
        arg.value_type,
        arg.int_value,
        arg.real_value,
        arg.string_value,
      );
    });
    return builder.build();
  };
});
