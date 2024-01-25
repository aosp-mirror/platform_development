import {TraceType} from 'trace/trace_type';
import {Segment} from '../../utils';
import {Transformer} from '../transformer';
import {MiniTimelineDrawerOutput} from './mini_timeline_drawer_output';

export type TimelineEntries = Map<
  TraceType,
  {
    points: number[];
    segments: Segment[];
    activePoint: number | undefined;
    activeSegment: Segment | undefined;
  }
>;

export class MiniCanvasDrawerData {
  constructor(
    public selectedPosition: number,
    public selection: Segment,
    private timelineEntriesGetter: () => Promise<TimelineEntries>,
    public transformer: Transformer
  ) {}

  private entries: TimelineEntries | undefined = undefined;

  async getTimelineEntries(): Promise<TimelineEntries> {
    if (this.entries === undefined) {
      this.entries = await this.timelineEntriesGetter();
    }
    return this.entries;
  }

  toOutput(): MiniTimelineDrawerOutput {
    return new MiniTimelineDrawerOutput(this.transformer.untransform(this.selectedPosition), {
      from: this.transformer.untransform(this.selection.from),
      to: this.transformer.untransform(this.selection.to),
    });
  }
}
