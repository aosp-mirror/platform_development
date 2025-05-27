/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {utf8Encode} from 'common/string_utils';
import {Timestamp} from 'common/time/time';
import Long from 'long';
import {AbstractParser} from 'parsers/legacy/abstract_parser';
import {perfetto} from 'protos/perfetto/trace/static';
import root from 'protos/protolog/udc/json';
import {com} from 'protos/protolog/udc/static';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import configJson32 from '../../../../configs/services.core.protolog32.json'; // eslint-disable-line no-restricted-imports
import configJson64 from '../../../../configs/services.core.protolog64.json'; // eslint-disable-line no-restricted-imports
import {CONFIG_32, CONFIG_64} from './legacy_to_perfetto_configs';

type ProtoLogMessage = com.android.internal.protolog.IProtoLogMessage;

export class ParserProtoLog extends AbstractParser<
  PropertyTreeNode,
  ProtoLogMessage
> {
  private static readonly ProtoLogFileProto = root.lookupType(
    'com.android.internal.protolog.ProtoLogFileProto',
  );
  private static readonly MAGIC_NUMBER = [
    0x09, 0x50, 0x52, 0x4f, 0x54, 0x4f, 0x4c, 0x4f, 0x47,
  ]; // .PROTOLOG
  private static readonly PROTOLOG_32_BIT_VERSION = '1.0.0';
  private static readonly PROTOLOG_64_BIT_VERSION = '2.0.0';

  private realToBootTimeOffsetNs: bigint | undefined;

  override getTraceType(): TraceType {
    return TraceType.PROTO_LOG;
  }

  override getMagicNumber(): number[] {
    return ParserProtoLog.MAGIC_NUMBER;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  override decodeTrace(buffer: Uint8Array): ProtoLogMessage[] {
    const fileProto = ParserProtoLog.ProtoLogFileProto.decode(
      buffer,
    ) as com.android.internal.protolog.IProtoLogFileProto;

    if (this.is32BitVersion(fileProto.log?.at(0))) {
      if (configJson32.version !== ParserProtoLog.PROTOLOG_32_BIT_VERSION) {
        const message = `Unsupported ProtoLog JSON config version ${configJson32.version}. Expected ${ParserProtoLog.PROTOLOG_32_BIT_VERSION}`;
        console.log(message);
        throw new TypeError(message);
      }
    } else if (this.is64BitVersion(fileProto.log?.at(0))) {
      if (configJson64.version !== ParserProtoLog.PROTOLOG_64_BIT_VERSION) {
        const message = `Unsupported ProtoLog JSON config version ${configJson64.version}. Expected ${ParserProtoLog.PROTOLOG_64_BIT_VERSION}`;
        console.log(message);
        throw new TypeError(message);
      }
    } else {
      const message = 'Unsupported ProtoLog trace version';
      console.log(message);
      throw new TypeError(message);
    }

    this.realToBootTimeOffsetNs =
      BigInt(
        assertDefined(fileProto.realTimeToElapsedTimeOffsetMillis).toString(),
      ) * 1000000n;

    if (!fileProto.log) {
      return [];
    }

    fileProto.log.sort((a: ProtoLogMessage, b: ProtoLogMessage) => {
      return Number(a.elapsedRealtimeNanos) - Number(b.elapsedRealtimeNanos);
    });

    return fileProto.log;
  }

  private is32BitVersion(entry: ProtoLogMessage | undefined): boolean {
    return (entry?.messageHashLegacy ?? 0) > 0;
  }

  private is64BitVersion(entry: ProtoLogMessage | undefined): boolean {
    return (
      entry?.messageHash instanceof Long &&
      (entry.messageHash.toString() ?? '0') !== '0'
    );
  }

  override canConvertToPerfetto(): boolean {
    return true;
  }

  override convertToPerfettoPackets(
    sequenceId: number,
    trustedUid = 1,
    trustedPid = 1,
  ): perfetto.protos.TracePacket[] {
    const packets = [];
    const firstPacket = this.createPacket(sequenceId, trustedUid, trustedPid);
    firstPacket.sequenceFlags =
      perfetto.protos.TracePacket.SequenceFlags.SEQ_INCREMENTAL_STATE_CLEARED;
    packets.push(firstPacket);
    packets.push(this.makeViewerConfigPacket(sequenceId, trustedUid));

    const stringToIid = new Map<string, number>();
    let stringIid = 1;

    for (const entry of this.decodedEntries) {
      const packet = this.createPacket(sequenceId, trustedUid, trustedPid);
      packet.timestamp = assertDefined(entry.elapsedRealtimeNanos);
      packet.timestampClockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME;

      let messageId: Long;
      if (this.is64BitVersion(entry)) {
        messageId = assertDefined(entry.messageHash);
      } else {
        messageId = Long.fromNumber(assertDefined(entry.messageHashLegacy));
      }

      const strParamIids: number[] = [];

      entry.strParams?.forEach((param) => {
        const iid = stringToIid.get(param);
        if (iid !== undefined) {
          strParamIids.push(iid);
        } else {
          stringToIid.set(param, stringIid);
          const packet = this.createPacket(sequenceId, trustedUid, trustedPid);
          this.updateInternedDataPacket(packet, param, stringIid);
          packets.push(packet);
          strParamIids.push(stringIid);
          stringIid++;
        }
      });

      if (strParamIids.length > 0) {
        packet.sequenceFlags =
          perfetto.protos.TracePacket.SequenceFlags.SEQ_NEEDS_INCREMENTAL_STATE;
      }

      packet.protologMessage = perfetto.protos.ProtoLogMessage.create({
        messageId,
        strParamIids,
        sint64Params: entry.sint64Params,
        doubleParams: entry.doubleParams,
        booleanParams: entry.booleanParams?.map((param) => {
          return param ? 1 : 0;
        }),
      });
      packets.push(packet);
    }

    return packets;
  }

  protected override getTimestamp(entry: ProtoLogMessage): Timestamp {
    return this.timestampConverter.makeTimestampFromBootTimeNs(
      BigInt(assertDefined(entry.elapsedRealtimeNanos).toString()),
    );
  }

  private makeViewerConfigPacket(
    sequenceId: number,
    trustedUid: number,
  ): perfetto.protos.TracePacket {
    const packet = this.createPacket(sequenceId, trustedUid, undefined);
    if (this.is64BitVersion(this.decodedEntries[0])) {
      packet.protologViewerConfig = CONFIG_64;
    } else {
      packet.protologViewerConfig = CONFIG_32;
    }
    return packet;
  }

  private updateInternedDataPacket(
    packet: perfetto.protos.TracePacket,
    str: string,
    iid: number,
  ): perfetto.protos.TracePacket {
    const internedString = perfetto.protos.InternedString.fromObject({
      iid: Long.fromNumber(iid),
      str: utf8Encode(str),
    });
    packet.internedData = perfetto.protos.InternedData.fromObject({
      protologStringArgs: [internedString],
    });
    return packet;
  }

  private createPacket(
    sequenceId: number,
    trustedUid: number | undefined,
    trustedPid: number | undefined,
  ): perfetto.protos.TracePacket {
    const packet = perfetto.protos.TracePacket.create();
    packet.trustedPacketSequenceId = sequenceId;
    packet.trustedUid = trustedUid;
    if (trustedPid) {
      packet.trustedPid = trustedPid;
    }
    return packet;
  }
}
