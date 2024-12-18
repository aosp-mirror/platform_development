import {TimeRange, Timestamp} from 'common/time/time';

export class MiniTimelineDrawerOutput {
  constructor(
    public selectedPosition: Timestamp,
    public selection: TimeRange,
  ) {}
}
