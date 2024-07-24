/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {TimeRange, Timestamp, TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class TimelineUtils {
  static getTimeRangeForTransition(
    transition: PropertyTreeNode,
    timestampType: TimestampType,
    fullTimeRange: TimeRange,
  ): TimeRange | undefined {
    const shellData = transition.getChildByName('shellData');
    const wmData = transition.getChildByName('wmData');

    const aborted = transition.getChildByName('aborted')?.getValue() ?? false;

    const dispatchTimestamp: Timestamp | undefined = shellData
      ?.getChildByName('dispatchTimeNs')
      ?.getValue();
    const createTimestamp: Timestamp | undefined = wmData
      ?.getChildByName('createTimeNs')
      ?.getValue();
    const finishOrAbortTimestamp: Timestamp | undefined = aborted
      ? shellData?.getChildByName('abortTimeNs')?.getValue()
      : wmData?.getChildByName('finishTimeNs')?.getValue();

    // currently we only render transitions during 'play' stage, so
    // do not render if no dispatch time and no finish/shell abort time
    // or if transition created but never dispatched to shell
    // TODO (b/324056564): visualise transition lifecycle in timeline
    if (
      (!dispatchTimestamp && !finishOrAbortTimestamp) ||
      (!dispatchTimestamp && createTimestamp)
    ) {
      return undefined;
    }

    let dispatchTimeNs: bigint | undefined;
    let finishTimeNs: bigint | undefined;

    const timeRangeMin = fullTimeRange.from.getValueNs();
    const timeRangeMax = fullTimeRange.to.getValueNs();

    if (timestampType === TimestampType.ELAPSED) {
      const startOffset =
        shellData
          ?.getChildByName('realToElapsedTimeOffsetTimestamp')
          ?.getValue()
          .getValueNs() ?? 0n;
      const finishOffset = aborted
        ? startOffset
        : shellData
            ?.getChildByName('realToElapsedTimeOffsetTimestamp')
            ?.getValue()
            .getValueNs() ?? 0n;

      dispatchTimeNs = dispatchTimestamp
        ? dispatchTimestamp.getValueNs() - startOffset
        : timeRangeMin;
      finishTimeNs = finishOrAbortTimestamp
        ? finishOrAbortTimestamp.getValueNs() - finishOffset
        : timeRangeMax;
    } else {
      dispatchTimeNs = dispatchTimestamp
        ? dispatchTimestamp.getValueNs()
        : timeRangeMin;
      finishTimeNs = finishOrAbortTimestamp
        ? finishOrAbortTimestamp.getValueNs()
        : timeRangeMax;
    }

    const startTime = NO_TIMEZONE_OFFSET_FACTORY.makeTimestampFromType(
      timestampType,
      dispatchTimeNs > timeRangeMin ? dispatchTimeNs : timeRangeMin,
      0n,
    );
    const finishTime = NO_TIMEZONE_OFFSET_FACTORY.makeTimestampFromType(
      timestampType,
      finishTimeNs,
      0n,
    );

    return {
      from: startTime,
      to: finishTime,
    };
  }
}
