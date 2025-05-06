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
import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import {ParserBuilder} from 'test/unit/parser_builder';
import {TraceFile} from 'trace/trace_file';
import {FileAndParser} from './file_and_parser';
import {LegacyToPerfettoConverter} from './legacy_to_perfetto_converter';

describe('LegacyToPerfettoConverter', () => {
  const testPacket1 = perfetto.protos.TracePacket.create({
    trustedPacketSequenceId: 1,
    timestamp: Long.fromInt(10, true),
    timestampClockId: 4,
  });
  const testPacket2 = perfetto.protos.TracePacket.create({
    trustedPacketSequenceId: 2,
    timestamp: Long.fromInt(20, true),
    timestampClockId: 5,
  });
  const clockSnapshot0 = makeExpectedClockSnapshot(new Long(0));
  const clockSnapshot20 = makeExpectedClockSnapshot(new Long(20));
  const emptyPacket = perfetto.protos.TracePacket.create();
  const existingPerfettoFile = makeExistingPerfettoFile(
    clockSnapshot20,
    emptyPacket,
  );

  it('converts multiple legacy files to new perfetto file', async () => {
    const fileAndParser1 = makeFileAndParser(testPacket1);
    const fileAndParser2 = makeFileAndParser(testPacket2);

    const perfettoFile =
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [fileAndParser1, fileAndParser2],
        0n,
      );
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([clockSnapshot0, testPacket1, testPacket2]);
  });

  it('applies latest offset to new perfetto file clock snapshot', async () => {
    const fileAndParser1 = makeFileAndParser(testPacket1);
    const fileAndParser2 = makeFileAndParser(testPacket2);

    const perfettoFile =
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [fileAndParser1, fileAndParser2],
        20n,
      );
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([clockSnapshot20, testPacket1, testPacket2]);
  });

  it('adds multiple legacy files to existing perfetto file', async () => {
    const fileAndParser1 = makeFileAndParser(testPacket1);
    const fileAndParser2 = makeFileAndParser(testPacket2);

    const perfettoFile =
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [fileAndParser1, fileAndParser2],
        0n,
        existingPerfettoFile,
      );
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([
      clockSnapshot20,
      emptyPacket,
      testPacket1,
      testPacket2,
    ]);
  });

  it('ignores legacy file that cannot be converted to perfetto format', async () => {
    const fileAndParser1 = makeFileAndParser();

    expect(
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [fileAndParser1],
        0n,
      ),
    ).toBeUndefined();

    expect(
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [fileAndParser1],
        0n,
        existingPerfettoFile,
      ),
    ).toBeUndefined();

    const fileAndParser2 = makeFileAndParser(testPacket2);
    const perfettoFile =
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [fileAndParser1, fileAndParser2],
        0n,
        existingPerfettoFile,
      );
    const trace = await checkAndDecodePerfettoFile(assertDefined(perfettoFile));
    expect(trace.packet).toEqual([clockSnapshot20, emptyPacket, testPacket2]);
  });

  it('robust to errors in packet conversion', async () => {
    const fileAndParser = makeFileAndParser(undefined, true);
    expect(
      await LegacyToPerfettoConverter.convertToSinglePerfettoFile(
        [fileAndParser],
        0n,
      ),
    ).toBeUndefined();
  });

  function makeExistingPerfettoFile(
    clockSnapshot20: perfetto.protos.TracePacket,
    emptyPacket: perfetto.protos.TracePacket,
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

  function makeFileAndParser(
    testPacket?: perfetto.protos.TracePacket,
    conversionError = false,
  ): FileAndParser {
    const parser = new ParserBuilder<object>()
      .setEntries([])
      .setTimestamps([])
      .build();
    if (testPacket) {
      const parserConvertSpy = jasmine.createSpy();
      parserConvertSpy.and.returnValue([testPacket]);
      parser.convertToPerfettoPackets = parserConvertSpy;
    } else if (conversionError) {
      const parserConvertSpy = jasmine.createSpy();
      parserConvertSpy.and.throwError(new Error('conversion failed'));
      parser.convertToPerfettoPackets = parserConvertSpy;
    }
    return new FileAndParser(new TraceFile(new File([], '')), parser);
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
    offset: Long,
  ): perfetto.protos.TracePacket {
    return perfetto.protos.TracePacket.fromObject({
      trustedPacketSequenceId: 1,
      clockSnapshot: {
        clocks: [
          {
            clockId: perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME,
            timestamp: new Long(0),
          },
          {
            clockId:
              perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME_COARSE,
            timestamp: offset,
          },
          {
            clockId:
              perfetto.protos.ClockSnapshot.Clock.BuiltinClocks
                .MONOTONIC_COARSE,
            timestamp: new Long(0),
          },
          {
            clockId: perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME,
            timestamp: offset,
          },
          {
            clockId:
              perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC,
            timestamp: new Long(0),
          },
          {
            clockId:
              perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC_RAW,
            timestamp: new Long(0),
          },
        ],
      },
    });
  }
});
