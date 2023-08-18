import {TimeRange} from 'app/timeline_data';
import {Timestamp} from 'trace/timestamp';

export class MiniTimelineDrawerOutput {
  constructor(public selectedPosition: Timestamp, public selection: TimeRange) {}
}
