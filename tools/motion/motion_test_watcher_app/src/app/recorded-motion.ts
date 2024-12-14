import { Feature } from './feature';
import { Timeline } from './timeline';
import { VideoSource } from './video-source';

export class RecordedMotion {
  constructor(
    readonly videoSource: VideoSource,
    readonly timeline: Timeline,
    readonly features: ReadonlyArray<Feature>,
  ) {}
}
