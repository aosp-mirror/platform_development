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

import {Timestamp, TimestampType} from 'trace/timestamp';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {AbstractParser} from './abstract_parser';
import {ExportedData} from './proto_types';

/* TODO: Support multiple Windows in one file upload.  */
export class ParserViewCapture extends AbstractParser {
  private classNames: string[] = [];
  private realToElapsedTimeOffsetNanos: bigint | undefined = undefined;
  packageName: string = '';
  windowTitle: string = '';

  constructor(trace: TraceFile) {
    super(trace);
  }

  override getTraceType(): TraceType {
    return TraceType.VIEW_CAPTURE;
  }

  override getMagicNumber(): number[] {
    return ParserViewCapture.MAGIC_NUMBER;
  }

  override decodeTrace(buffer: Uint8Array): any[] {
    const exportedData = ExportedData.decode(buffer) as any;
    this.classNames = exportedData.classname;
    this.realToElapsedTimeOffsetNanos = BigInt(exportedData.realToElapsedTimeOffsetNanos);
    this.packageName = this.shortenAndCapitalize(exportedData.package);

    const firstWindowData = exportedData.windowData[0];
    this.windowTitle = this.shortenAndCapitalize(firstWindowData.title);

    return firstWindowData.frameData;
  }

  override processDecodedEntry(index: number, timestampType: TimestampType, decodedEntry: any) {
    this.formatProperties(decodedEntry.node, this.classNames);
    return decodedEntry;
  }

  private shortenAndCapitalize(name: string): string {
    const shortName = name.substring(name.lastIndexOf('.') + 1);
    return shortName.charAt(0).toUpperCase() + shortName.slice(1);
  }

  private formatProperties(root: any /* ViewNode */, classNames: string[]): any /* ViewNode */ {
    const DEPTH_MAGNIFICATION = 4;
    const VISIBLE = 0;

    function inner(
      node: any /* ViewNode */,
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
      node.type = 'ViewNode';
      node.layerId = 0;
      node.isMissing = false;
      node.hwcCompositionType = 0;
      node.zOrderRelativeOfId = -1;
      node.isRootLayer = false;
      node.skip = null;
      node.id = node.name;
      node.stableId = node.id;
      node.equals = (other: any /* ViewNode */) => ParserViewCapture.equals(node, other);
    }

    root.scaleX = root.scaleY = 1;
    root.translationX = root.translationY = 0;
    inner(root, 0, 0, 1, 1, 0, true);

    root.isRootLayer = true;
    return root;
  }

  override getTimestamp(timestampType: TimestampType, frameData: any): undefined | Timestamp {
    return Timestamp.from(
      timestampType,
      BigInt(frameData.timestamp),
      this.realToElapsedTimeOffsetNanos
    );
  }

  private static readonly MAGIC_NUMBER = [0x9, 0x78, 0x65, 0x90, 0x65, 0x73, 0x82, 0x65, 0x68];

  /** This method is used by the tree_generator to determine if 2 nodes have equivalent properties. */
  private static equals(node: any /* ViewNode */, other: any /* ViewNode */): boolean {
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
