import {TimeRange, Timestamp} from 'common/time';

export class MiniTimelineDrawerOutput {
  constructor(
    public selectedPosition: Timestamp,
    public selection: TimeRange,
  ) {}
}
