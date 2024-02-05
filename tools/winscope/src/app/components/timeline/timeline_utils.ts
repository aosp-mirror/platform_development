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

import {ElapsedTimestamp, RealTimestamp, TimeRange, Timestamp, TimestampType} from 'common/time';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class TimelineUtils {
  static getTimeRangeForTransition(
    transition: PropertyTreeNode,
    timestampType: TimestampType,
    fullTimeRange: TimeRange
  ): TimeRange | undefined {
    const dispatchTimeLong = transition
      .getChildByName('shellData')
      ?.getChildByName('dispatchTimeNs')
      ?.getValue();
    const createTimeLong = transition
      .getChildByName('wmData')
      ?.getChildByName('createTimeNs')
      ?.getValue();
    const finishOrAbortTimeLong = transition.getChildByName('aborted')?.getValue()
      ? transition.getChildByName('shellData')?.getChildByName('abortTimeNs')?.getValue()
      : transition.getChildByName('wmData')?.getChildByName('finishTimeNs')?.getValue();

    // currently we only render transitions during 'play' stage, so
    // do not render if no dispatch time and no finish/shell abort time
    // or if transition created but never dispatched to shell
    // TODO (b/324056564): visualise transition lifecycle in timeline
    if ((!dispatchTimeLong && !finishOrAbortTimeLong) || (!dispatchTimeLong && createTimeLong)) {
      return undefined;
    }

    let startTime: Timestamp;
    let finishTime: Timestamp;

    if (timestampType === TimestampType.REAL) {
      const realToElapsedTimeOffsetNs =
        transition.getChildByName('realToElapsedTimeOffsetNs')?.getValue() ?? 0n;
      startTime = new RealTimestamp(
        dispatchTimeLong
          ? BigInt(dispatchTimeLong.toString()) + realToElapsedTimeOffsetNs
          : fullTimeRange.from.getValueNs()
      );
      finishTime = new RealTimestamp(
        finishOrAbortTimeLong
          ? BigInt(finishOrAbortTimeLong.toString()) + realToElapsedTimeOffsetNs
          : fullTimeRange.to.getValueNs()
      );
    } else if (timestampType === TimestampType.ELAPSED) {
      startTime = new ElapsedTimestamp(
        dispatchTimeLong ? BigInt(dispatchTimeLong.toString()) : fullTimeRange.from.getValueNs()
      );
      finishTime = new ElapsedTimestamp(
        finishOrAbortTimeLong
          ? BigInt(finishOrAbortTimeLong.toString())
          : fullTimeRange.to.getValueNs()
      );
    } else {
      throw new Error('Unsupported timestamp type');
    }

    return {
      from: startTime,
      to: finishTime,
    };
  }
}
