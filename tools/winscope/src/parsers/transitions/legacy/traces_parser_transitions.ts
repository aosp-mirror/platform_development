/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined, assertTrue} from 'common/assert_utils';
import {getMax} from 'common/bigint_math';
import {NOT_IMPLEMENTED_ERROR} from 'common/errors';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import Long from 'long';
import {AbstractTracesParser} from 'parsers/traces/abstract_traces_parser';
import {perfetto} from 'protos/perfetto/trace/static';
import {com} from 'protos/transitions/udc/static';
import {CoarseVersion} from 'trace/coarse_version';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {ParserTransitionsShell} from './parser_transitions_shell';

export class TracesParserTransitions extends AbstractTracesParser<PropertyTreeNode> {
  private readonly wmTransitionTrace: Trace<WmTransition> | undefined;
  private readonly shellTransitionTrace: Trace<ShellTransition> | undefined;
  private readonly descriptors: string[];
  private decodedEntries: PerfettoTransition[] | undefined;
  private shellHandlerMapping:
    | com.android.wm.shell.IHandlerMapping[]
    | undefined;
  private realToBootTimeOffsetNs: bigint | undefined;

  constructor(traces: Traces, timestampConverter: ParserTimestampConverter) {
    super(timestampConverter);
    const wmTransitionTrace = traces.getTrace(TraceType.WM_TRANSITION);
    const shellTransitionTrace = traces.getTrace(TraceType.SHELL_TRANSITION);
    if (wmTransitionTrace && shellTransitionTrace) {
      this.wmTransitionTrace = wmTransitionTrace;
      this.shellTransitionTrace = shellTransitionTrace;
      this.descriptors = this.wmTransitionTrace
        .getDescriptors()
        .concat(this.shellTransitionTrace.getDescriptors());
    } else {
      this.descriptors = [];
    }
  }

  override getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LEGACY;
  }

  override async parse() {
    if (this.wmTransitionTrace === undefined) {
      throw new Error('Missing WM Transition trace');
    }

    if (this.shellTransitionTrace === undefined) {
      throw new Error('Missing Shell Transition trace');
    }

    const shellParser = this.shellTransitionTrace.getParser();
    assertTrue(shellParser instanceof ParserTransitionsShell);
    this.shellHandlerMapping = (
      shellParser as ParserTransitionsShell
    ).getShellHandlerMapping();

    const wmOffset = this.wmTransitionTrace
      .getParser()
      .getRealToBootTimeOffsetNs();
    const shellOffset = shellParser.getRealToBootTimeOffsetNs();

    this.realToBootTimeOffsetNs = getMax([wmOffset ?? 0n, shellOffset ?? 0n]);
    if (this.realToBootTimeOffsetNs === 0n) {
      this.realToBootTimeOffsetNs = undefined;
    }

    const wmTransitionEntries = (
      await Promise.all(
        this.wmTransitionTrace.mapEntry((entry) => entry.getValue()),
      )
    ).map((entry) => this.convertWmToPerfettoTransition(entry));

    const shellTransitionEntries = (
      await Promise.all(
        this.shellTransitionTrace.mapEntry((entry) => entry.getValue()),
      )
    ).map((entry) => this.convertShellToPerfettoTransition(entry));

    this.decodedEntries = this.compressEntries(
      wmTransitionEntries.concat(shellTransitionEntries),
    );

    await this.createTimestamps();
  }

  override async createTimestamps() {
    this.timestamps = [];
    const zeroTs = this.timestampConverter.makeZeroTimestamp();
    for (let index = 0; index < this.getLengthEntries(); index++) {
      const entry = assertDefined(this.decodedEntries)[index];
      const ns = this.getTimestampNsFromTransitionProperties(entry);
      const ts =
        ns && ns !== 0n
          ? this.timestampConverter.makeTimestampFromBootTimeNs(ns)
          : zeroTs;
      this.timestamps.push(ts);
    }
  }

  override getLengthEntries(): number {
    return assertDefined(this.decodedEntries).length;
  }

  override getEntry(index: number): Promise<PropertyTreeNode> {
    // Legacy parsers that implement convertToPerfettoPackets should not
    // parser and provide individual trace entries, as they should be
    // converted to perfetto using LegacyToPerfettoConverter
    throw NOT_IMPLEMENTED_ERROR;
  }

  override getDescriptors(): string[] {
    return this.descriptors;
  }

  override getTraceType(): TraceType {
    return TraceType.TRANSITION;
  }

  override getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  override getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  convertToPerfettoPackets?(sequenceId: number): perfetto.protos.TracePacket[] {
    const packets = [];
    packets.push(this.createHandlerMappingPacket(sequenceId));

    for (const entry of assertDefined(this.decodedEntries)) {
      const packet = perfetto.protos.TracePacket.create();
      packet.trustedPacketSequenceId = sequenceId;
      const ns = this.getTimestampNsFromTransitionProperties(entry) ?? 0n;
      packet.timestamp = Long.fromString(ns.toString());
      packet.timestampClockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME;
      packet.shellTransition = entry;
      packets.push(packet);
    }

    return packets;
  }

  private compressEntries(
    transitions: PerfettoTransition[],
  ): PerfettoTransition[] {
    const idToTransition = new Map<number, PerfettoTransition>();
    for (const transition of transitions) {
      const id = assertDefined(transition.id);
      const accumulatedTransition = idToTransition.get(id);
      if (!accumulatedTransition) {
        idToTransition.set(id, transition);
      } else {
        const mergedTransition = this.mergePartialTransitions(
          accumulatedTransition,
          transition,
        );
        idToTransition.set(id, mergedTransition);
      }
    }
    const compressedTransitions = Array.from(idToTransition.values());
    return compressedTransitions.sort((a, b) => this.compareByTimestamp(a, b));
  }

  private convertWmToPerfettoTransition(
    wmTransition: WmTransition,
  ): PerfettoTransition {
    const perfettoTransition: PerfettoTransition = {
      id: wmTransition.id,
      createTimeNs: this.nullifyIfDefaultValue(wmTransition.createTimeNs),
      sendTimeNs: this.nullifyIfDefaultValue(wmTransition.sendTimeNs),
      wmAbortTimeNs: this.nullifyIfDefaultValue(wmTransition.abortTimeNs),
      finishTimeNs: this.nullifyIfDefaultValue(wmTransition.finishTimeNs),
      startTransactionId: this.nullifyIfDefaultValue(
        wmTransition.startTransactionId,
      ),
      finishTransactionId: this.nullifyIfDefaultValue(
        wmTransition.finishTransactionId,
      ),
      type: this.nullifyIfDefaultValue(wmTransition.type),
      targets: this.nullifyIfDefaultValue(wmTransition.targets),
      flags: this.nullifyIfDefaultValue(wmTransition.flags),
      startingWindowRemoveTimeNs: this.nullifyIfDefaultValue(
        wmTransition.startingWindowRemoveTimeNs,
      ),
    };
    return perfettoTransition;
  }

  private convertShellToPerfettoTransition(
    shellTransition: ShellTransition,
  ): PerfettoTransition {
    const perfettoTransition: PerfettoTransition = {
      id: shellTransition.id,
      dispatchTimeNs: this.nullifyIfDefaultValue(
        shellTransition.dispatchTimeNs,
      ),
      mergeTimeNs: this.nullifyIfDefaultValue(shellTransition.mergeTimeNs),
      mergeRequestTimeNs: this.nullifyIfDefaultValue(
        shellTransition.mergeRequestTimeNs,
      ),
      shellAbortTimeNs: this.nullifyIfDefaultValue(shellTransition.abortTimeNs),
      handler: this.nullifyIfDefaultValue(shellTransition.handler),
      mergeTarget: this.nullifyIfDefaultValue(shellTransition.mergeTarget),
    };
    return perfettoTransition;
  }

  private nullifyIfDefaultValue<T extends TransitionProperty>(
    value: T,
  ): T | undefined {
    if (this.isDefaultValue(value)) {
      return undefined;
    }
    return value;
  }

  private isDefaultValue(value: TransitionProperty): boolean {
    if (value instanceof Long && value.isZero()) {
      return true;
    } else if (typeof value === 'number' && value === 0) {
      return true;
    } else if (Array.isArray(value) && value.length === 0) {
      return true;
    }
    return false;
  }

  private compareByTimestamp(
    a: PerfettoTransition,
    b: PerfettoTransition,
  ): number {
    const aNs = this.getTimestampNsFromTransitionProperties(a) ?? 0n;
    const bNs = this.getTimestampNsFromTransitionProperties(b) ?? 0n;
    if (aNs !== bNs) {
      return aNs < bNs ? -1 : 1;
    }
    // fallback to id
    assertTrue(a.id !== b.id);
    return assertDefined(a.id) < assertDefined(b.id) ? -1 : 1;
  }

  private getTimestampNsFromTransitionProperties(
    transition: PerfettoTransition,
  ): bigint | undefined {
    // Entry timestamps are defined as shell dispatch time - if this is
    // null and send time is not null we fall back on send time
    const ns = transition.dispatchTimeNs ?? transition.sendTimeNs;
    if (!ns) {
      return undefined;
    }
    return BigInt(ns.toString());
  }

  private mergePartialTransitions(
    transition1: PerfettoTransition,
    transition2: PerfettoTransition,
  ): PerfettoTransition {
    assertTrue(transition1.id === transition2.id);
    const mergedTransition = Object.assign({}, transition1);
    Object.entries(transition2).forEach(([key, value]) => {
      if (value && !this.isDefaultValue(value)) {
        Object.assign(mergedTransition, {[key]: value});
      }
    });
    return mergedTransition;
  }

  private createHandlerMappingPacket(
    sequenceId: number,
  ): perfetto.protos.TracePacket {
    const packet = perfetto.protos.TracePacket.create();
    packet.trustedPacketSequenceId = sequenceId;
    packet.shellHandlerMappings =
      perfetto.protos.ShellHandlerMappings.fromObject({
        mapping: this.shellHandlerMapping,
      });
    return packet;
  }
}

type WmTransition = com.android.server.wm.shell.ITransition;
type ShellTransition = com.android.wm.shell.ITransition;
type PerfettoTransition = perfetto.protos.IShellTransition;
type TransitionTarget = perfetto.protos.ShellTransition.ITarget;
type TransitionProperty = number | Long | TransitionTarget[] | null | undefined;
