/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {Timestamp, TimestampType} from 'common/time';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
  VisitableParserCustomQuery,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {FrameData, TraceType, ViewNode} from 'trace/trace_type';
import {ParsingUtils} from './parsing_utils';

export class ParserViewCaptureWindow implements Parser<FrameData> {
  private timestamps: Map<TimestampType, Timestamp[]> = new Map<TimestampType, Timestamp[]>();

  constructor(
    private readonly descriptors: string[],
    private readonly frameData: FrameData[],
    private readonly traceType: TraceType,
    private readonly realToElapsedTimeOffsetNanos: bigint,
    private readonly packageName: string,
    private readonly classNames: string[]
  ) {
    /*
      TODO: Enable this once multiple ViewCapture Tabs becomes generic. Right now it doesn't matter since
        the title is dependent upon the view capture type.

      this.title = `${ParserViewCapture.shortenAndCapitalize(packageName)} - ${ParserViewCapture.shortenAndCapitalize(
        windowData.title
      )}`;
      */
    this.parse();
  }

  parse() {
    this.frameData.map((it) => ParsingUtils.addDefaultProtoFields(it));
    this.timestamps = this.decodeTimestamps();
  }

  private decodeTimestamps(): Map<TimestampType, Timestamp[]> {
    const timeStampMap = new Map<TimestampType, Timestamp[]>();
    for (const type of [TimestampType.ELAPSED, TimestampType.REAL]) {
      const timestamps: Timestamp[] = [];
      let areTimestampsValid = true;

      for (const entry of this.frameData) {
        const timestamp = Timestamp.from(
          type,
          BigInt(entry.timestamp),
          this.realToElapsedTimeOffsetNanos
        );
        if (timestamp === undefined) {
          areTimestampsValid = false;
          break;
        }
        timestamps.push(timestamp);
      }

      if (areTimestampsValid) {
        timeStampMap.set(type, timestamps);
      }
    }
    return timeStampMap;
  }

  getTraceType(): TraceType {
    return this.traceType;
  }

  getLengthEntries(): number {
    return this.frameData.length;
  }

  getTimestamps(type: TimestampType): Timestamp[] | undefined {
    return this.timestamps.get(type);
  }

  getEntry(index: number, _: TimestampType): Promise<FrameData> {
    ParserViewCaptureWindow.formatProperties(this.frameData[index].node, this.classNames);
    return Promise.resolve(this.frameData[index]);
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    return new VisitableParserCustomQuery(type)
      .visit(CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME, async () => {
        return Promise.resolve(this.packageName);
      })
      .getResult();
  }

  getDescriptors(): string[] {
    return this.descriptors;
  }

  private static formatProperties(root: ViewNode, classNames: string[]): ViewNode {
    const DEPTH_MAGNIFICATION = 4;
    const VISIBLE = 0;

    function inner(
      node: ViewNode,
      leftShift: number,
      topShift: number,
      scaleX: number,
      scaleY: number,
      depth: number,
      isParentVisible: boolean
    ) {
      const newScaleX = scaleX * node.scaleX;
      const newScaleY = scaleY * node.scaleY;

      const l =
        leftShift +
        (node.left + node.translationX) * scaleX +
        (node.width * (scaleX - newScaleX)) / 2;
      const t =
        topShift +
        (node.top + node.translationY) * scaleY +
        (node.height * (scaleY - newScaleY)) / 2;
      node.boxPos = {
        left: l,
        top: t,
        width: node.width * newScaleX,
        height: node.height * newScaleY,
      };

      node.name = `${classNames[node.classnameIndex]}@${node.hashcode}`;

      node.shortName = node.name.split('.');
      node.shortName = node.shortName[node.shortName.length - 1];

      node.isVisible = isParentVisible && VISIBLE === node.visibility;

      for (let i = 0; i < node.children.length; i++) {
        inner(
          node.children[i],
          l - node.scrollX,
          t - node.scrollY,
          newScaleX,
          newScaleY,
          depth + 1,
          node.isVisible
        );
        node.children[i].parent = node;
      }

      // TODO: Audit these properties
      node.depth = depth * DEPTH_MAGNIFICATION;
      node.className = node.name.substring(0, node.name.indexOf('@'));
      node.type = 'ViewNode';
      node.layerId = 0;
      node.isMissing = false;
      node.hwcCompositionType = 0;
      node.zOrderRelativeOfId = -1;
      node.isRootLayer = false;
      node.skip = null;
      node.id = node.name;
      node.stableId = node.id;
      node.equals = (other: ViewNode) => ParserViewCaptureWindow.equals(node, other);
    }

    root.scaleX = root.scaleY = 1;
    root.translationX = root.translationY = 0;
    inner(root, 0, 0, 1, 1, 0, true);

    root.isRootLayer = true;
    return root;
  }

  /** This method is used by the tree_generator to determine if 2 nodes have equivalent properties. */
  private static equals(node: ViewNode, other: ViewNode): boolean {
    if (!node && !other) {
      return true;
    }
    if (!node || !other) {
      return false;
    }
    return (
      node.id === other.id &&
      node.name === other.name &&
      node.hashcode === other.hashcode &&
      node.left === other.left &&
      node.top === other.top &&
      node.height === other.height &&
      node.width === other.width &&
      node.elevation === other.elevation &&
      node.scaleX === other.scaleX &&
      node.scaleY === other.scaleY &&
      node.scrollX === other.scrollX &&
      node.scrollY === other.scrollY &&
      node.translationX === other.translationX &&
      node.translationY === other.translationY &&
      node.alpha === other.alpha &&
      node.visibility === other.visibility &&
      node.willNotDraw === other.willNotDraw &&
      node.clipChildren === other.clipChildren &&
      node.depth === other.depth
    );
  }
}
