import {TimelineData} from 'app/timeline_data';
import {ElapsedTimestamp, RealTimestamp, TimeRange, Timestamp, TimestampType} from 'common/time';
import {Transition} from 'flickerlib/common';
import {Trace, TraceEntry} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {Segment} from '../../utils';
import {Transformer} from '../transformer';
import {MiniCanvasDrawerData, TimelineEntries} from './mini_canvas_drawer_data';

export class MiniTimelineDrawerInput {
  constructor(
    public fullRange: TimeRange,
    public selectedPosition: Timestamp,
    public selection: TimeRange,
    public zoomRange: TimeRange,
    public traces: Traces,
    public timelineData: TimelineData
  ) {}

  transform(mapToRange: Segment): MiniCanvasDrawerData {
    const transformer = new Transformer(this.zoomRange, mapToRange);

    return new MiniCanvasDrawerData(
      transformer.transform(this.selectedPosition),
      {
        from: transformer.transform(this.selection.from),
        to: transformer.transform(this.selection.to),
      },
      () => {
        return this.transformTracesTimestamps(transformer);
      },
      transformer
    );
  }

  private async transformTracesTimestamps(transformer: Transformer): Promise<TimelineEntries> {
    const transformedTraceSegments = new Map<
      TraceType,
      {
        points: number[];
        segments: Segment[];
        activePoint: number | undefined;
        activeSegment: Segment | undefined;
      }
    >();

    await Promise.all(
      this.traces.mapTrace(async (trace, type) => {
        const activeEntry = this.timelineData.findCurrentEntryFor(trace.type);

        if (type === TraceType.TRANSITION) {
          // Transition trace is a special case, with entries with time ranges
          const transitionTrace = this.traces.getTrace(type)!;
          transformedTraceSegments.set(trace.type, {
            points: [],
            activePoint: undefined,
            segments: await this.transformTransitionTraceTimestamps(transformer, transitionTrace),
            activeSegment: activeEntry
              ? await this.transformTransitionEntry(transformer, activeEntry)
              : undefined,
          });
        } else {
          transformedTraceSegments.set(trace.type, {
            points: this.transformTraceTimestamps(transformer, trace),
            activePoint: activeEntry
              ? transformer.transform(activeEntry.getTimestamp())
              : undefined,
            segments: [],
            activeSegment: undefined,
          });
        }
      })
    );

    return transformedTraceSegments;
  }

  private async transformTransitionTraceTimestamps(
    transformer: Transformer,
    trace: Trace<Transition>
  ): Promise<Segment[]> {
    const promises: Array<Promise<Segment | undefined>> = [];
    trace.forEachEntry((entry) => {
      promises.push(this.transformTransitionEntry(transformer, entry));
    });

    return (await Promise.all(promises)).filter((it) => it !== undefined) as Segment[];
  }

  private async transformTransitionEntry(
    transformer: Transformer,
    entry: TraceEntry<Transition>
  ): Promise<Segment | undefined> {
    const transition = await entry.getValue();
    let createTime: Timestamp;
    let finishTime: Timestamp;

    if (transition.createTime.isMin || transition.finishTime.isMax) {
      return undefined;
    }

    if (entry.getTimestamp().getType() === TimestampType.REAL) {
      createTime = new RealTimestamp(BigInt(transition.createTime.unixNanos.toString()));
      finishTime = new RealTimestamp(BigInt(transition.finishTime.unixNanos.toString()));
    } else if (entry.getTimestamp().getType() === TimestampType.ELAPSED) {
      createTime = new ElapsedTimestamp(BigInt(transition.createTime.elapsedNanos.toString()));
      finishTime = new ElapsedTimestamp(BigInt(transition.finishTime.elapsedNanos.toString()));
    } else {
      throw new Error('Unspported timestamp type');
    }

    return {from: transformer.transform(createTime), to: transformer.transform(finishTime)};
  }

  private transformTraceTimestamps(transformer: Transformer, trace: Trace<{}>): number[] {
    const result: number[] = [];

    trace.forEachTimestamp((timestamp) => {
      result.push(transformer.transform(timestamp));
    });

    return result;
  }
}
