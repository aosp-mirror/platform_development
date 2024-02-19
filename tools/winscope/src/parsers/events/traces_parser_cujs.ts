/*
 * Copyright 2023, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertDefined} from 'common/assert_utils';
import {Timestamp, TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {AbstractTracesParser} from 'parsers/abstract_traces_parser';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {PropertyTreeBuilderFromProto} from 'trace/tree_node/property_tree_builder_from_proto';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {CujType} from './cuj_type';
import {EventTag} from './event_tag';
import {AddCujProperties} from './operations/add_cuj_properties';

export class TracesParserCujs extends AbstractTracesParser<PropertyTreeNode> {
  private static readonly AddCujProperties = new AddCujProperties();
  private readonly eventLogTrace: Trace<PropertyTreeNode> | undefined;
  private readonly descriptors: string[];
  private decodedEntries: PropertyTreeNode[] | undefined;

  constructor(traces: Traces) {
    super();

    const eventlogTrace = traces.getTrace(TraceType.EVENT_LOG);
    if (eventlogTrace !== undefined) {
      this.eventLogTrace = eventlogTrace;
      this.descriptors = this.eventLogTrace.getDescriptors();
    } else {
      this.descriptors = [];
    }
  }

  override async parse() {
    if (this.eventLogTrace === undefined) {
      throw new Error('EventLog trace not defined');
    }

    const eventsPromises = this.eventLogTrace.mapEntry((entry) =>
      entry.getValue(),
    );
    const events = await Promise.all(eventsPromises);
    const cujEvents = events.filter((event) => {
      const tag = assertDefined(event.getChildByName('tag')).getValue();
      return (
        tag === EventTag.JANK_CUJ_BEGIN_TAG ||
        tag === EventTag.JANK_CUJ_END_TAG ||
        tag === EventTag.JANK_CUJ_CANCEL_TAG
      );
    });
    this.decodedEntries = this.makeCujsFromEvents(cujEvents);
    await this.parseTimestamps();
  }

  getLengthEntries(): number {
    return assertDefined(this.decodedEntries).length;
  }

  getEntry(
    index: number,
    timestampType: TimestampType,
  ): Promise<PropertyTreeNode> {
    const entry = assertDefined(this.decodedEntries)[index];
    return Promise.resolve(entry);
  }

  override getDescriptors(): string[] {
    return this.descriptors;
  }

  getTraceType(): TraceType {
    return TraceType.CUJS;
  }

  override getTimestamp(
    type: TimestampType,
    cuj: PropertyTreeNode,
  ): undefined | Timestamp {
    const cujTimestamp = assertDefined(cuj.getChildByName('startTimestamp'));
    if (type === TimestampType.ELAPSED) {
      const elapsedNanos = assertDefined(
        cujTimestamp.getChildByName('elapsedNanos'),
      ).getValue();
      return NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(elapsedNanos);
    } else if (type === TimestampType.REAL) {
      const unixNanos = assertDefined(
        cujTimestamp.getChildByName('unixNanos'),
      ).getValue();
      return NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(unixNanos);
    }
    return undefined;
  }

  private makeCujTimestampObject(timestamp: PropertyTreeNode): CujTimestamp {
    return {
      unixNanos: assertDefined(
        timestamp.getChildByName('unixNanos'),
      ).getValue(),
      elapsedNanos: assertDefined(
        timestamp.getChildByName('elapsedNanos'),
      ).getValue(),
      systemUptimeNanos: assertDefined(
        timestamp.getChildByName('systemUptimeNanos'),
      ).getValue(),
    };
  }

  private makeCujsFromEvents(events: PropertyTreeNode[]): PropertyTreeNode[] {
    events.forEach((event) => TracesParserCujs.AddCujProperties.apply(event));

    const startEvents = this.filterEventsByTag(
      events,
      EventTag.JANK_CUJ_BEGIN_TAG,
    );
    const endEvents = this.filterEventsByTag(events, EventTag.JANK_CUJ_END_TAG);
    const canceledEvents = this.filterEventsByTag(
      events,
      EventTag.JANK_CUJ_CANCEL_TAG,
    );

    const cujs: PropertyTreeNode[] = [];

    for (const startEvent of startEvents) {
      const startCujType: CujType = assertDefined(
        startEvent.getChildByName('cujType'),
      ).getValue();
      const startTimestamp = assertDefined(
        startEvent.getChildByName('cujTimestamp'),
      );

      const matchingEndEvent = this.findMatchingEvent(
        endEvents,
        startCujType,
        startTimestamp,
      );
      const matchingCancelEvent = this.findMatchingEvent(
        canceledEvents,
        startCujType,
        startTimestamp,
      );

      if (!matchingEndEvent && !matchingCancelEvent) {
        continue;
      }

      const closingEvent = this.getClosingEvent(
        matchingEndEvent,
        matchingCancelEvent,
      );

      const closingEventTimestamp = assertDefined(
        closingEvent.getChildByName('cujTimestamp'),
      );
      const canceled =
        assertDefined(closingEvent.getChildByName('tag')?.getValue()) ===
        EventTag.JANK_CUJ_CANCEL_TAG;

      const cuj: Cuj = {
        startCujType,
        startTimestamp: this.makeCujTimestampObject(startTimestamp),
        endTimestamp: this.makeCujTimestampObject(closingEventTimestamp),
        canceled,
      };

      cujs.push(this.makeCujPropertyTree(cuj));
    }
    return cujs;
  }

  private filterEventsByTag(
    events: PropertyTreeNode[],
    targetTag: EventTag,
  ): PropertyTreeNode[] {
    return events.filter((event) => {
      const tag = assertDefined(event.getChildByName('tag')).getValue();
      return tag === targetTag;
    });
  }

  private findMatchingEvent(
    events: PropertyTreeNode[],
    startCujType: CujType,
    startTimestamp: PropertyTreeNode,
  ): PropertyTreeNode | undefined {
    return events.find((event) => {
      const cujType = assertDefined(event.getChildByName('cujType')).getValue();
      const timestamp = assertDefined(event.getChildByName('cujTimestamp'));
      return (
        startCujType === cujType &&
        this.cujTimestampIsGreaterThan(timestamp, startTimestamp)
      );
    });
  }

  private cujTimestampIsGreaterThan(
    a: PropertyTreeNode,
    b: PropertyTreeNode,
  ): boolean {
    const aUnixNanos: bigint = assertDefined(
      a.getChildByName('unixNanos'),
    ).getValue();
    const bUnixNanos: bigint = assertDefined(
      b.getChildByName('unixNanos'),
    ).getValue();
    return aUnixNanos > bUnixNanos;
  }

  private getClosingEvent(
    endEvent: PropertyTreeNode | undefined,
    cancelEvent: PropertyTreeNode | undefined,
  ): PropertyTreeNode {
    const endTimestamp = endEvent?.getChildByName('cujTimestamp');
    const cancelTimestamp = cancelEvent?.getChildByName('cujTimestamp');

    let closingEvent: PropertyTreeNode | undefined;
    if (!endTimestamp) {
      closingEvent = cancelEvent;
    } else if (!cancelTimestamp) {
      closingEvent = endEvent;
    } else {
      const canceledBeforeEnd = !this.cujTimestampIsGreaterThan(
        endTimestamp,
        cancelTimestamp,
      );
      closingEvent = canceledBeforeEnd ? cancelEvent : endEvent;
    }

    if (!closingEvent) {
      throw Error('Should have found one matching closing event');
    }

    return closingEvent;
  }

  private makeCujPropertyTree(cuj: Cuj): PropertyTreeNode {
    return new PropertyTreeBuilderFromProto()
      .setData(cuj)
      .setRootId('CujTrace')
      .setRootName('cuj')
      .build();
  }
}

interface Cuj {
  startCujType: CujType;
  startTimestamp: CujTimestamp;
  endTimestamp: CujTimestamp;
  canceled: boolean;
}

interface CujTimestamp {
  unixNanos: bigint;
  elapsedNanos: bigint;
  systemUptimeNanos: bigint;
}
