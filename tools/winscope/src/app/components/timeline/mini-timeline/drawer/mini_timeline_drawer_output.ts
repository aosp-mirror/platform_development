import {TimeRange, Timestamp} from 'trace/timestamp';

export class MiniTimelineDrawerOutput {
  constructor(public selectedPosition: Timestamp, public selection: TimeRange) {}
}
