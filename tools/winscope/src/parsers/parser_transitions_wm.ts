import {
  CrossPlatform,
  ShellTransitionData,
  Transition,
  TransitionChange,
  TransitionType,
  WmTransitionData,
} from 'trace/flickerlib/common';
import {ElapsedTimestamp, RealTimestamp, Timestamp, TimestampType} from 'trace/timestamp';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';
import {WmTransitionsTraceFileProto} from './proto_types';

export class ParserTransitionsWm extends AbstractParser {
  private realToElapsedTimeOffsetNs: undefined | bigint;

  override getTraceType(): TraceType {
    return TraceType.WM_TRANSITION;
  }

  override processDecodedEntry(index: number, timestampType: TimestampType, entryProto: any): any {
    return this.parseWmTransitionEntry(entryProto);
  }

  override decodeTrace(buffer: Uint8Array): any[] {
    const decodedProto = WmTransitionsTraceFileProto.decode(buffer) as any;
    if (Object.prototype.hasOwnProperty.call(decodedProto, 'realToElapsedTimeOffsetNanos')) {
      this.realToElapsedTimeOffsetNs = BigInt(decodedProto.realToElapsedTimeOffsetNanos);
    } else {
      console.warn('Missing realToElapsedTimeOffsetNanos property on SF trace proto');
      this.realToElapsedTimeOffsetNs = undefined;
    }
    return decodedProto.transitions;
  }

  override getMagicNumber(): number[] | undefined {
    return [0x09, 0x54, 0x52, 0x4e, 0x54, 0x52, 0x41, 0x43, 0x45]; // .TRNTRACE
  }

  override getTimestamp(type: TimestampType, decodedEntry: Transition): undefined | Timestamp {
    decodedEntry = this.parseWmTransitionEntry(decodedEntry);

    if (type === TimestampType.ELAPSED) {
      return new ElapsedTimestamp(BigInt(decodedEntry.timestamp.elapsedNanos.toString()));
    }

    if (type === TimestampType.REAL) {
      return new RealTimestamp(BigInt(decodedEntry.timestamp.unixNanos.toString()));
    }

    throw new Error('Timestamp type unsupported');
  }

  private parseWmTransitionEntry(entry: any): Transition {
    this.validateWmTransitionEntry(entry);
    let changes: TransitionChange[] | null;
    if (entry.targets.length === 0) {
      changes = null;
    } else {
      changes = entry.targets.map(
        (it: any) =>
          new TransitionChange(TransitionType.Companion.fromInt(it.mode), it.layerId, it.windowId)
      );
    }

    if (this.realToElapsedTimeOffsetNs === undefined) {
      throw new Error('missing realToElapsedTimeOffsetNs');
    }

    let createTime = null;
    if (entry.createTimeNs && BigInt(entry.createTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.createTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      createTime = CrossPlatform.timestamp.fromString(
        entry.createTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    let sendTime = null;
    if (entry.sendTimeNs && BigInt(entry.sendTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.sendTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      sendTime = CrossPlatform.timestamp.fromString(
        entry.sendTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    let abortTime = null;
    if (entry.abortTimeNs && BigInt(entry.abortTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.abortTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      abortTime = CrossPlatform.timestamp.fromString(
        entry.abortTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    let finishTime = null;
    if (entry.finishTimeNs && BigInt(entry.finishTimeNs.toString()) !== 0n) {
      const unixNs = BigInt(entry.finishTimeNs.toString()) + this.realToElapsedTimeOffsetNs;
      finishTime = CrossPlatform.timestamp.fromString(
        entry.finishTimeNs.toString(),
        null,
        unixNs.toString()
      );
    }

    let startTransactionId = null;
    if (entry.startTransactionId && BigInt(entry.startTransactionId.toString()) !== 0n) {
      startTransactionId = BigInt(entry.startTransactionId.toString());
    }

    let finishTransactionId = null;
    if (entry.finishTransactionId && BigInt(entry.finishTransactionId.toString()) !== 0n) {
      finishTransactionId = BigInt(entry.finishTransactionId.toString());
    }

    let type = null;
    if (entry.type !== 0) {
      type = TransitionType.Companion.fromInt(entry.type);
    }

    return new Transition(
      entry.id,
      new WmTransitionData(
        createTime,
        sendTime,
        abortTime,
        finishTime,
        startTransactionId?.toString(),
        finishTransactionId?.toString(),
        type,
        changes
      ),
      new ShellTransitionData()
    );
  }

  private validateWmTransitionEntry(entry: any) {
    if (entry.id === 0) {
      throw new Error('Entry need a non null id');
    }
    if (!entry.createTimeNs && !entry.sendTimeNs && !entry.abortTimeNs && !entry.finishTimeNs) {
      throw new Error('Requires at least one non-null timestamp');
    }
  }
}
