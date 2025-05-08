/*
 * Copyright (C) 2025 The Android Open Source Project
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
import {TimestampConverterUtils} from 'common/time/test_utils';
import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import {ParserBuilder} from 'test/unit/parser_builder';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {
  ClockSnapshot,
  LegacyToPerfettoConverter,
} from './legacy_to_perfetto_converter';

type TracePacket = perfetto.protos.TracePacket;

describe('LegacyToPerfettoConverter', () => {
  const packetB1 = makePacketWithBoottimeTs(10);
  const packetB2 = makePacketWithBoottimeTs(15);
  const packetB3 = makePacketWithBoottimeTs(18);
  const packetM1 = makePacketWithMonotonicTs(14);
  const packetM2 = makePacketWithMonotonicTs(20);
  const packetM3 = makePacketWithMonotonicTs(25);

  const perfettoClock = {realtime: 50n, boottime: 30n, monotonic: 40n};
  const perfettoSnapshot = makeExpectedClockSnapshot(perfettoClock);
  const emptyPacket = perfetto.protos.TracePacket.create();
  const existingFile = makeExistingPerfettoFile(perfettoSnapshot, emptyPacket);

  it('converts multiple legacy files to new perfetto file', async () => {
    const parser1 = makeParser([packetB1]);
    spyOn(parser1, 'getRealToMonotonicTimeOffsetNs').and.returnValue(undefined);
    const parser2 = makeParser([packetB2]);
    spyOn(parser2, 'getRealToMonotonicTimeOffsetNs').and.returnValue(undefined);

    const perfettoFile = await convertToPerfetto([parser1, parser2]);
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([
      makeExpectedClockSnapshot({
        realtime: 15n,
        boottime: 15n,
        monotonic: undefined,
      }),
      packetB1,
      packetB2,
    ]);
  });

  it('adds multiple legacy files to existing perfetto file', async () => {
    const parsers = [makeParser([packetB1]), makeParser([packetM1])];
    const perfettoFile = await convertToPerfetto(parsers, existingFile);
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([
      perfettoSnapshot,
      emptyPacket,
      packetB1,
      packetM1,
    ]);
  });

  it('ignores legacy file that cannot be converted to perfetto format', async () => {
    const parser1 = makeParser([]);
    expect(await convertToPerfetto([parser1])).toBeUndefined();
    expect(await convertToPerfetto([parser1], existingFile)).toBeUndefined();

    const parser2 = makeParser([packetM1]);
    const parsers = [parser1, parser2];
    const perfettoFile = await convertToPerfetto(parsers, existingFile);
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([perfettoSnapshot, emptyPacket, packetM1]);
  });

  it('converts elapsed legacy trace to new perfetto trace', async () => {
    await testElapsedParsers([packetB1]);
  });

  it('converts elapsed legacy trace with multiple entries', async () => {
    const packets = [packetB1, packetB2, packetB3];
    await testElapsedParsers(packets);
  });

  it('converts legacy trace with real-to-boottime offset', async () => {
    await testBoottimeParsers([packetB1]);
  });

  it('converts legacy trace with real-to-boottime offset with multiple entries', async () => {
    const packets = [packetB1, packetB2, packetB3];
    await testBoottimeParsers(packets);
  });

  it('converts legacy trace with real-to-monotonic offset', async () => {
    await testMonotonicParsers([packetM1]);
  });

  it('converts legacy trace with real-to-monotonic offset with multiple entries', async () => {
    const packets = [packetM1, packetM2, packetM3];
    await testMonotonicParsers(packets);
  });

  it('with boot-time and monotonically offset parsers loaded', async () => {
    const parserB = makeParser([packetB1]);
    spyOn(parserB, 'getRealToBootTimeOffsetNs').and.returnValue(2n);
    spyOn(parserB, 'getRealToMonotonicTimeOffsetNs').and.returnValue(undefined);

    const parserM = makeParser([packetM1]);
    spyOn(parserM, 'getRealToBootTimeOffsetNs').and.returnValue(undefined);
    spyOn(parserM, 'getRealToMonotonicTimeOffsetNs').and.returnValue(3n);

    const perfettoFile =
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [parserM],
        [parserM, parserB],
      );
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([
      makeExpectedClockSnapshot({
        realtime: 10n,
        boottime: 8n,
        monotonic: undefined,
      }),
      makeExpectedClockSnapshot({
        realtime: 14n,
        boottime: 12n,
        monotonic: 11n,
      }),
      packetM1,
    ]);
  });

  it('converts legacy trace with zero timestamp', async () => {
    const packet = makePacketWithBoottimeTs(0);
    const parser = makeParser([packet]);
    spyOn(parser, 'getRealToBootTimeOffsetNs').and.returnValue(undefined);
    spyOn(parser, 'getRealToMonotonicTimeOffsetNs').and.returnValue(undefined);
    const perfettoFile = await convertToPerfetto([parser]);
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    const clockSnapshot = makeExpectedClockSnapshot({
      realtime: 0n,
      boottime: 0n,
      monotonic: 0n,
    });
    expect(trace.packet).toEqual([clockSnapshot, packet]);
  });

  it('robust to errors in packet conversion', async () => {
    const parser = makeParser([], true);
    expect(await convertToPerfetto([parser])).toBeUndefined();
  });

  it('throws error if allParsers empty and no Perfetto file provided', async () => {
    const parser = makeParser([], true);
    await expectAsync(
      LegacyToPerfettoConverter.convertToSinglePerfettoFile([parser], []),
    ).toBeRejectedWithError('allParsers empty and no Perfetto file provided');
  });

  function makePacketWithMonotonicTs(ts: number) {
    return perfetto.protos.TracePacket.create({
      trustedPacketSequenceId: 1,
      timestamp: Long.fromInt(ts, true),
      timestampClockId:
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC,
    });
  }

  function makePacketWithBoottimeTs(ts: number) {
    return perfetto.protos.TracePacket.create({
      trustedPacketSequenceId: 1,
      timestamp: Long.fromInt(ts, true),
      timestampClockId:
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME,
    });
  }

  async function convertToPerfetto(
    parsers: Array<Parser<{}>>,
    perfettoFile?: TraceFile,
  ) {
    return await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
      parsers,
      parsers,
      perfettoFile,
    );
  }

  async function testElapsedParsers(packets: TracePacket[]) {
    const parser = makeParser(packets);
    spyOn(parser, 'getRealToBootTimeOffsetNs').and.returnValue(undefined);
    spyOn(parser, 'getRealToMonotonicTimeOffsetNs').and.returnValue(undefined);
    const perfettoFile = await convertToPerfetto([parser]);
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    const snapshotPackets = [
      makeExpectedClockSnapshot({
        realtime: 10n,
        boottime: 10n,
        monotonic: 10n,
      }),
    ];
    if (packets.length > 1) {
      snapshotPackets.push(
        makeExpectedClockSnapshot({
          realtime: 18n,
          boottime: 18n,
          monotonic: 18n,
        }),
      );
    }
    expect(trace.packet).toEqual([...snapshotPackets, ...packets]);
  }

  async function testBoottimeParsers(packets: TracePacket[]) {
    const parser = makeParser(packets);
    spyOn(parser, 'getRealToBootTimeOffsetNs').and.returnValue(3n);
    spyOn(parser, 'getRealToMonotonicTimeOffsetNs').and.returnValue(undefined);
    await testConversion(parser, packets, false);
  }

  async function testMonotonicParsers(packets: TracePacket[]) {
    const parser = makeParser(packets);
    spyOn(parser, 'getRealToBootTimeOffsetNs').and.returnValue(undefined);
    spyOn(parser, 'getRealToMonotonicTimeOffsetNs').and.returnValue(3n);
    await testConversion(parser, packets, true);
  }

  async function testConversion(
    parser: Parser<{}>,
    packets: TracePacket[],
    isMonotonic: boolean,
  ) {
    const perfettoFile = await convertToPerfetto([parser]);
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    const snapshotPackets = [makeSnapshotFromPacket(packets[0], isMonotonic)];
    if (packets.length > 1) {
      snapshotPackets.push(
        makeSnapshotFromPacket(packets[packets.length - 1], isMonotonic),
      );
    }
    expect(trace.packet).toEqual([...snapshotPackets, ...packets]);
  }

  function makeSnapshotFromPacket(packet: TracePacket, isMonotonic = false) {
    const realtime = BigInt(packet.timestamp?.toString() ?? 0n);
    return makeExpectedClockSnapshot({
      realtime,
      boottime: realtime - 3n,
      monotonic: isMonotonic ? realtime - 3n : undefined,
    });
  }

  function makeExistingPerfettoFile(
    clockSnapshot20: TracePacket,
    emptyPacket: TracePacket,
  ) {
    const existingTrace = perfetto.protos.Trace.fromObject({
      packet: [clockSnapshot20, emptyPacket],
    });
    return new TraceFile(
      new File(
        [perfetto.protos.Trace.encode(existingTrace).finish()],
        'existing_trace',
      ),
    );
  }

  function makeParser(
    testPackets: TracePacket[],
    conversionError = false,
  ): Parser<{}> {
    const ts =
      testPackets.length === 0
        ? [TimestampConverterUtils.makeRealTimestamp(0n)]
        : testPackets.map((testPacket) => {
            const ns = BigInt(testPacket?.timestamp.toString() ?? 0n);
            return TimestampConverterUtils.makeRealTimestamp(ns);
          });
    const parser = new ParserBuilder<string>()
      .setEntries(ts.length === 0 ? [''] : ts.map((t) => ''))
      .setTimestamps(ts)
      .build();

    if (testPackets.length > 0) {
      const parserConvertSpy = jasmine.createSpy();
      parserConvertSpy.and.returnValue(testPackets);
      parser.convertToPerfettoPackets = parserConvertSpy;
    } else if (conversionError) {
      const parserConvertSpy = jasmine.createSpy();
      parserConvertSpy.and.throwError(new Error('conversion failed'));
      parser.convertToPerfettoPackets = parserConvertSpy;
    }

    return parser;
  }

  async function checkAndDecodePerfettoFile(
    perfettoFile: TraceFile,
  ): Promise<perfetto.protos.Trace> {
    const expectedPerfettoTraceName = 'combined_winscope_trace.perfetto-trace';
    expect(perfettoFile.getDescriptor()).toEqual(expectedPerfettoTraceName);
    const fileBuffer = new Uint8Array(await perfettoFile.file.arrayBuffer());
    return perfetto.protos.Trace.decode(fileBuffer);
  }

  function makeExpectedClockSnapshot(
    clockSnapshot: ClockSnapshot,
  ): TracePacket {
    const realtime = Long.fromString(clockSnapshot.realtime.toString());
    const clocks = [
      {
        clockId:
          perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME_COARSE,
        timestamp: realtime,
      },
      {
        clockId: perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME,
        timestamp: realtime,
      },
    ];

    if (clockSnapshot.boottime !== undefined) {
      clocks.push({
        clockId: perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME,
        timestamp: Long.fromString(clockSnapshot.boottime.toString()),
      });
    }

    if (clockSnapshot.monotonic !== undefined) {
      const monotonic = Long.fromString(clockSnapshot.monotonic.toString());
      clocks.push(
        ...[
          {
            clockId:
              perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC,
            timestamp: monotonic,
          },
          {
            clockId:
              perfetto.protos.ClockSnapshot.Clock.BuiltinClocks
                .MONOTONIC_COARSE,
            timestamp: monotonic,
          },
          {
            clockId:
              perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC_RAW,
            timestamp: monotonic,
          },
        ],
      );
    }

    return perfetto.protos.TracePacket.fromObject({
      trustedPacketSequenceId: 1,
      clockSnapshot: {
        clocks,
      },
    });
  }
});
