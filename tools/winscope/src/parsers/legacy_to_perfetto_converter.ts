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

import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import {TraceFile} from 'trace/trace_file';
import {FileAndParser} from './file_and_parser';

export class LegacyToPerfettoConverter {
  static async convertToSinglePerfettoFile(
    legacy: FileAndParser[],
    latestElapsedToRealTimeOffsetNs: bigint,
    perfettoFile?: TraceFile,
  ): Promise<TraceFile | undefined> {
    let trace: perfetto.protos.Trace;
    let fileBuffer: Uint8Array;

    if (!perfettoFile) {
      trace = perfetto.protos.Trace.create();
      fileBuffer = Uint8Array.from([]);

      const clockSnapshot =
        LegacyToPerfettoConverter.makeTracePacketWithClockSnapshot(
          Long.fromString(latestElapsedToRealTimeOffsetNs.toString()),
        );
      trace.packet.push(clockSnapshot);
    } else {
      fileBuffer = new Uint8Array(await perfettoFile.file.arrayBuffer());
      trace = perfetto.protos.Trace.decode(fileBuffer);
    }

    const legacyPackets = [];
    let sequenceId =
      Math.max(
        ...trace.packet.map((packet) => packet.trustedPacketSequenceId ?? 0),
      ) + 1;
    for (const fileAndParser of legacy) {
      if (fileAndParser.parser.convertToPerfettoPackets) {
        try {
          const packets =
            fileAndParser.parser.convertToPerfettoPackets(sequenceId);
          if (packets.length > 0) {
            packets[0].firstPacketOnSequence = true;
            legacyPackets.push(...packets);
            sequenceId++;
          }
        } catch (e) {
          // swallow
        }
      }
    }
    if (legacyPackets.length === 0) {
      return undefined;
    }

    trace.packet.push(...legacyPackets);

    const data = perfetto.protos.Trace.encode(trace).finish();
    return new TraceFile(
      new File([data], 'combined_winscope_trace.perfetto-trace'),
    );
  }

  private static makeTracePacketWithClockSnapshot(
    realToElapsedTimeOffsetNs: Long,
  ): perfetto.protos.TracePacket {
    const packet = perfetto.protos.TracePacket.create();
    packet.trustedPacketSequenceId = 1;

    const snapshot = perfetto.protos.ClockSnapshot.create();

    const clockBoottime = perfetto.protos.ClockSnapshot.Clock.create();
    clockBoottime.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME;
    clockBoottime.timestamp = new Long(0);
    snapshot.clocks.push(clockBoottime);

    const clockRealtimeCoarse = perfetto.protos.ClockSnapshot.Clock.create();
    clockRealtimeCoarse.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME_COARSE;
    clockRealtimeCoarse.timestamp = realToElapsedTimeOffsetNs;
    snapshot.clocks.push(clockRealtimeCoarse);

    const clockMonotonicCoarse = perfetto.protos.ClockSnapshot.Clock.create();
    clockMonotonicCoarse.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC_COARSE;
    clockMonotonicCoarse.timestamp = new Long(0);
    snapshot.clocks.push(clockMonotonicCoarse);

    const clockRealtime = perfetto.protos.ClockSnapshot.Clock.create();
    clockRealtime.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME;
    clockRealtime.timestamp = realToElapsedTimeOffsetNs;
    snapshot.clocks.push(clockRealtime);

    const clockMonotonic = perfetto.protos.ClockSnapshot.Clock.create();
    clockMonotonic.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC;
    clockMonotonic.timestamp = new Long(0);
    snapshot.clocks.push(clockMonotonic);

    const clockMonotonicRaw = perfetto.protos.ClockSnapshot.Clock.create();
    clockMonotonicRaw.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC_RAW;
    clockMonotonicRaw.timestamp = new Long(0);
    snapshot.clocks.push(clockMonotonicRaw);

    packet.clockSnapshot = snapshot;

    return packet;
  }
}
