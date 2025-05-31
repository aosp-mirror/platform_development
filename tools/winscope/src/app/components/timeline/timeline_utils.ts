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

import {assertDefined} from 'common/assert_utils';
import {TimeRange, Timestamp} from 'common/time/time';
import {ComponentTimestampConverter} from 'common/time/timestamp_converter';
import {TransitionStatus} from 'trace/transitions/status';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

export function isTransitionWithUnknownStart(
  transition: HierarchyTreeNode,
): boolean {
  const dispatchTimestamp: Timestamp | undefined = transition
    ?.getEagerPropertyByName('dispatchTimeNs')
    ?.getValue();
  return dispatchTimestamp === undefined;
}

export function isTransitionWithUnknownEnd(
  transition: HierarchyTreeNode,
): boolean {
  const aborted = isAborted(transition);
  const finishOrAbortTimestamp: Timestamp | undefined = aborted
    ? transition?.getEagerPropertyByName('shellAbortTimeNs')?.getValue()
    : transition?.getEagerPropertyByName('finishTimeNs')?.getValue();
  return finishOrAbortTimestamp === undefined;
}

function isAborted(transition: HierarchyTreeNode): boolean {
  return (
    transition.getEagerPropertyByName('status')?.formattedValue() ===
    TransitionStatus.ABORTED
  );
}

export function getTimeRangeForTransition(
  transition: HierarchyTreeNode,
  fullTimeRange: TimeRange,
  converter: ComponentTimestampConverter,
): TimeRange | undefined {
  const aborted = isAborted(transition);

  const dispatchTimestamp: Timestamp | undefined = transition
    ?.getEagerPropertyByName('dispatchTimeNs')
    ?.getValue();
  const createTimestamp: Timestamp | undefined = transition
    ?.getEagerPropertyByName('createTimeNs')
    ?.getValue();
  const finishOrAbortTimestamp: Timestamp | undefined = aborted
    ? transition?.getEagerPropertyByName('shellAbortTimeNs')?.getValue()
    : transition?.getEagerPropertyByName('finishTimeNs')?.getValue();

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

  const timeRangeMin = fullTimeRange.from.getValueNs();
  const timeRangeMax = fullTimeRange.to.getValueNs();

  if (
    finishOrAbortTimestamp &&
    finishOrAbortTimestamp.getValueNs() < timeRangeMin
  ) {
    return undefined;
  }

  if (
    !finishOrAbortTimestamp &&
    assertDefined(dispatchTimestamp).getValueNs() < timeRangeMin
  ) {
    return undefined;
  }

  if (
    dispatchTimestamp &&
    finishOrAbortTimestamp &&
    dispatchTimestamp.getValueNs() > timeRangeMax
  ) {
    return undefined;
  }

  const dispatchTimeNs = dispatchTimestamp
    ? dispatchTimestamp.getValueNs()
    : assertDefined(finishOrAbortTimestamp).getValueNs() - 1n;

  const finishTimeNs = finishOrAbortTimestamp
    ? finishOrAbortTimestamp.getValueNs()
    : dispatchTimeNs + 1n;

  const startTime = converter.makeTimestampFromNs(
    dispatchTimeNs > timeRangeMin ? dispatchTimeNs : timeRangeMin,
  );
  const finishTime = converter.makeTimestampFromNs(finishTimeNs);

  return new TimeRange(startTime, finishTime);
}

export function convertHexToRgb(
  hex: string,
): {r: number; g: number; b: number} | undefined {
  // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
  const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
  hex = hex.replace(shorthandRegex, (m, r, g, b) => {
    return r + r + g + g + b + b;
  });

  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result
    ? {
        // tslint:disable-next-line:ban
        r: parseInt(result[1], 16),
        // tslint:disable-next-line:ban
        g: parseInt(result[2], 16),
        // tslint:disable-next-line:ban
        b: parseInt(result[3], 16),
      }
    : undefined;
}
