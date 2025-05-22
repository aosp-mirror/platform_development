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
import {NOT_IMPLEMENTED_ERROR} from 'common/errors';
import {UserNotifier} from 'common/user_notifier';
import Long from 'long';
import {FailedToConvertLegacyTraces} from 'messaging/user_warnings';
import {perfetto} from 'protos/perfetto/trace/static';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {
  getParserWithLatestRealToBootTimeOffset,
  getParserWithLatestRealToMonotonicTimeOffset,
} from './parser_time_utils';

export interface ClockSnapshot {
  realtime: bigint;
  boottime: bigint | undefined;
  monotonic: bigint | undefined;
}

export class LegacyToPerfettoConverter {
  static async convertToSinglePerfettoFile(
    legacyParsers: Array<Parser<object>>,
    allParsers: Array<Parser<object>>,
    perfettoFile?: TraceFile,
  ): Promise<TraceFile | undefined> {
    let trace: perfetto.protos.Trace;
    try {
      trace = await LegacyToPerfettoConverter.makePerfettoTrace(
        allParsers,
        perfettoFile,
      );
    } catch (e) {
      console.error(e);
      UserNotifier.add(
        new FailedToConvertLegacyTraces((e as Error).message),
      ).notify();
      return perfettoFile;
    }
    const legacyPackets = LegacyToPerfettoConverter.makeTraceDataPackets(
      legacyParsers,
      trace,
    );
    if (legacyPackets.length === 0) {
      return undefined;
    }
    trace.packet.push(...legacyPackets);

    const data = perfetto.protos.Trace.encode(trace).finish();
    return new TraceFile(
      new File([data], 'combined_winscope_trace.perfetto-trace'),
    );
  }

  private static async makePerfettoTrace(
    allParsers: Array<Parser<object>>,
    perfettoFile?: TraceFile,
  ): Promise<perfetto.protos.Trace> {
    let trace: perfetto.protos.Trace;
    if (!perfettoFile) {
      const clockSnapshots =
        LegacyToPerfettoConverter.makeClockSnapshots(allParsers);
      trace = perfetto.protos.Trace.create();
      if (clockSnapshots.length === 0) {
        throw new Error('no parsers or Perfetto file provided');
      }
      clockSnapshots.forEach((snapshot) => {
        const clockSnapshot =
          LegacyToPerfettoConverter.makeTracePacketWithClockSnapshot(snapshot);
        trace.packet.push(clockSnapshot);
      });
    } else {
      const fileBuffer = new Uint8Array(await perfettoFile.file.arrayBuffer());
      trace = perfetto.protos.Trace.decode(fileBuffer);
    }

    return trace;
  }

  private static makeClockSnapshots(
    parsers: Array<Parser<object>>,
  ): ClockSnapshot[] {
    if (parsers.length === 0) {
      return [];
    }
    const clockSnapshots: ClockSnapshot[] = [];

    const boottimeParser = getParserWithLatestRealToBootTimeOffset(parsers);
    const monotonicParser =
      getParserWithLatestRealToMonotonicTimeOffset(parsers);

    const boottimeSnapshots: ClockSnapshot[] = [];
    const monotonicSnapshots: ClockSnapshot[] = [];

    if (boottimeParser === undefined && monotonicParser === undefined) {
      LegacyToPerfettoConverter.getRealTimestampsForClockSnapshots(
        parsers[0],
      ).forEach((realtime) => {
        clockSnapshots.push({
          realtime,
          boottime: realtime,
          monotonic: realtime,
        });
      });
    }

    if (boottimeParser) {
      const boottimeOffset = boottimeParser?.getRealToBootTimeOffsetNs();
      LegacyToPerfettoConverter.getRealTimestampsForClockSnapshots(
        boottimeParser,
      ).forEach((realtime) => {
        const boottime = realtime - assertDefined(boottimeOffset);
        boottimeSnapshots.push({realtime, boottime, monotonic: undefined});
      });
    }

    if (monotonicParser) {
      const monotonicOffset = monotonicParser?.getRealToMonotonicTimeOffsetNs();
      LegacyToPerfettoConverter.getRealTimestampsForClockSnapshots(
        monotonicParser,
      ).forEach((realtime) => {
        const monotonic = realtime - assertDefined(monotonicOffset);

        // Monotonic snapshots must contain a boottime timestamp for TP to be able
        // to convert monotonic timestamps to boottime
        let boottime: bigint;
        if (boottimeParser) {
          const snapshotB = boottimeSnapshots[boottimeSnapshots.length - 1];
          const realtimeDiff = snapshotB.realtime - realtime;
          boottime = assertDefined(snapshotB.boottime) - realtimeDiff;
        } else {
          boottime = monotonic;
        }

        monotonicSnapshots.push({realtime, boottime, monotonic});
      });
    }

    clockSnapshots.push(...boottimeSnapshots);
    clockSnapshots.push(...monotonicSnapshots);

    return clockSnapshots;
  }

  private static getRealTimestampsForClockSnapshots(
    parser: Parser<object>,
  ): Array<bigint> {
    const ts = assertDefined(parser.getTimestamps());
    const realTs: Array<bigint> = [];
    if (ts.length > 0) {
      realTs.push(ts[0].getValueNs());
    }
    if (ts.length > 1) {
      // to adjust against drift in TP, we add clock snapshots at the
      // start and end of the trace
      realTs.push(ts[parser.getLengthEntries() - 1].getValueNs());
    }
    return realTs;
  }

  private static makeTracePacketWithClockSnapshot(
    legacySnapshot: ClockSnapshot,
  ): perfetto.protos.TracePacket {
    const packet = perfetto.protos.TracePacket.create();
    packet.trustedPacketSequenceId = 1;

    const snapshot = perfetto.protos.ClockSnapshot.create();

    const realtime = Long.fromString(legacySnapshot.realtime.toString());

    const clockRealtimeCoarse = perfetto.protos.ClockSnapshot.Clock.create();
    clockRealtimeCoarse.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME_COARSE;
    clockRealtimeCoarse.timestamp = realtime;
    snapshot.clocks.push(clockRealtimeCoarse);

    const clockRealtime = perfetto.protos.ClockSnapshot.Clock.create();
    clockRealtime.clockId =
      perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.REALTIME;
    clockRealtime.timestamp = realtime;
    snapshot.clocks.push(clockRealtime);

    if (legacySnapshot.boottime !== undefined) {
      const boottime = Long.fromString(legacySnapshot.boottime.toString());
      const clockBoottime = perfetto.protos.ClockSnapshot.Clock.create();
      clockBoottime.clockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME;
      clockBoottime.timestamp = boottime;
      snapshot.clocks.push(clockBoottime);
    }

    if (legacySnapshot.monotonic !== undefined) {
      const monotonic = Long.fromString(legacySnapshot.monotonic.toString());
      const clockMonotonic = perfetto.protos.ClockSnapshot.Clock.create();
      clockMonotonic.clockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC;
      clockMonotonic.timestamp = monotonic;
      snapshot.clocks.push(clockMonotonic);

      const clockMonotonicCoarse = perfetto.protos.ClockSnapshot.Clock.create();
      clockMonotonicCoarse.clockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC_COARSE;
      clockMonotonicCoarse.timestamp = monotonic;
      snapshot.clocks.push(clockMonotonicCoarse);

      const clockMonotonicRaw = perfetto.protos.ClockSnapshot.Clock.create();
      clockMonotonicRaw.clockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.MONOTONIC_RAW;
      clockMonotonicRaw.timestamp = monotonic;
      snapshot.clocks.push(clockMonotonicRaw);
    }

    packet.clockSnapshot = snapshot;

    return packet;
  }

  private static makeTraceDataPackets(
    legacyParsers: Array<Parser<object>>,
    trace: perfetto.protos.Trace,
  ): perfetto.protos.TracePacket[] {
    const [largestUid, largestPid] = trace.packet.reduce(
      ([uid, pid], packet) => {
        return [
          Math.max(packet.trustedUid ?? 0, uid),
          Math.max(packet.trustedPid ?? 0, pid),
        ];
      },
      [0, 0],
    );
    let [trustedUid, trustedPid] = [largestUid + 1, largestPid + 1];

    const packets: perfetto.protos.TracePacket[] = [];
    let sequenceId =
      Math.max(
        ...trace.packet.map((packet) => packet.trustedPacketSequenceId ?? 0),
      ) + 1;
    for (const parser of legacyParsers) {
      if (parser.convertToPerfettoPackets) {
        try {
          const legacyPackets = parser.convertToPerfettoPackets(
            sequenceId,
            trustedUid,
            trustedPid,
          );
          if (legacyPackets.length > 0) {
            legacyPackets[0].firstPacketOnSequence = true;
            packets.push(...legacyPackets);
            sequenceId++;
            trustedUid++;
            trustedPid++;
          }
        } catch (e) {
          // swallow
          if (e !== NOT_IMPLEMENTED_ERROR) {
            console.error(e);
          }
        }
      }
    }
    return packets;
  }
}
