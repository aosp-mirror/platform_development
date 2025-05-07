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

import {assertDefined} from 'common/assert_utils';
import {NOT_IMPLEMENTED_ERROR} from 'common/errors';
import {utf8Encode} from 'common/string_utils';
import {Timestamp} from 'common/time/time';
import {ParserTimestampConverter} from 'common/time/timestamp_converter';
import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import {com} from 'protos/viewcapture/udc/static';
import {CoarseVersion} from 'trace/coarse_version';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from 'trace/custom_query';
import {EntriesRange} from 'trace/index_types';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';

export class ParserViewCaptureWindow implements Parser<HierarchyTreeNode> {
  private static readonly PACKAGE_OR_WINDOW_IID = 1;

  private timestamps: Timestamp[] | undefined;
  private viewIdToIid = new Map<string, number>();

  constructor(
    private readonly descriptors: string[],
    private readonly frameData: FrameData[],
    private readonly realToBootTimeOffsetNs: bigint,
    private readonly packageName: string,
    private readonly windowName: string,
    private readonly classNames: string[],
    private readonly timestampConverter: ParserTimestampConverter,
  ) {}

  parse() {
    throw NOT_IMPLEMENTED_ERROR;
  }

  getTraceType(): TraceType {
    return TraceType.VIEW_CAPTURE;
  }

  getCoarseVersion(): CoarseVersion {
    return CoarseVersion.LEGACY;
  }

  getLengthEntries(): number {
    return this.frameData.length;
  }

  getRealToMonotonicTimeOffsetNs(): bigint | undefined {
    return undefined;
  }

  getRealToBootTimeOffsetNs(): bigint | undefined {
    return this.realToBootTimeOffsetNs;
  }

  createTimestamps() {
    this.timestamps = this.decodeTimestamps();
  }

  getTimestamps(): Timestamp[] | undefined {
    return this.timestamps;
  }

  getEntry(index: number): Promise<HierarchyTreeNode> {
    throw NOT_IMPLEMENTED_ERROR;
  }

  convertToPerfettoPackets(
    sequenceId: number,
    trustedUid = 1,
    trustedPid = 1,
  ): perfetto.protos.TracePacket[] {
    if (this.frameData.length === 0) {
      return [];
    }
    const packets = this.frameData.map((frame, index) => {
      const packet = perfetto.protos.TracePacket.create();
      packet.trustedPacketSequenceId = sequenceId;
      packet.timestamp = assertDefined(frame.timestamp);
      packet.timestampClockId =
        perfetto.protos.ClockSnapshot.Clock.BuiltinClocks.BOOTTIME;
      packet.trustedUid = trustedUid;
      packet.trustedPid = trustedPid;
      packet.sequenceFlags =
        index === 0
          ? 3
          : perfetto.protos.TracePacket.SequenceFlags
              .SEQ_NEEDS_INCREMENTAL_STATE;
      packet.winscopeExtensions = {
        '.perfetto.protos.WinscopeExtensionsImpl.viewcapture':
          this.convertToPerfettoViewCapture(frame),
      };
      return packet;
    });
    packets[0].internedData = this.makeInternedData();
    return packets;
  }

  customQuery<Q extends CustomQueryType>(
    type: Q,
    entriesRange: EntriesRange,
  ): Promise<CustomQueryParserResultTypeMap[Q]> {
    throw NOT_IMPLEMENTED_ERROR;
  }

  getDescriptors(): string[] {
    return [this.windowName, ...this.descriptors];
  }

  private decodeTimestamps(): Timestamp[] {
    return this.frameData.map((entry) =>
      this.timestampConverter.makeTimestampFromBootTimeNs(
        BigInt(assertDefined(entry.timestamp).toString()),
      ),
    );
  }

  private convertToPerfettoView(
    node: ViewNode,
    parentId: number,
    perfettoViews: PerfettoView[],
  ) {
    if (node.id && !this.viewIdToIid.has(node.id)) {
      this.viewIdToIid.set(node.id, this.viewIdToIid.size + 1);
    }
    const nodeId = perfettoViews.length;
    const perfettoView: perfetto.protos.ViewCapture.IView = {
      id: nodeId,
      parentId,
      hashcode: node.hashcode,
      viewIdIid: node.id ? this.viewIdToIid.get(node.id) : undefined,
      classNameIid: node.classnameIndex,
      left: node.left,
      top: node.top,
      width: node.width,
      height: node.height,
      scrollX: node.scrollX,
      scrollY: node.scrollY,
      translationX: node.translationX,
      translationY: node.translationY,
      scaleX: node.scaleX,
      scaleY: node.scaleY,
      alpha: node.alpha,
      willNotDraw: node.willNotDraw,
      clipChildren: node.clipChildren,
      visibility: node.visibility,
      elevation: node.elevation,
    };
    perfettoViews.push(perfettoView);

    node.children?.forEach((child) => {
      this.convertToPerfettoView(child, nodeId, perfettoViews);
    });
  }

  private convertToPerfettoViewCapture(
    frame: FrameData,
  ): perfetto.protos.ViewCapture {
    const perfettoViews: PerfettoView[] = [];
    this.convertToPerfettoView(assertDefined(frame.node), -1, perfettoViews);
    return perfetto.protos.ViewCapture.fromObject({
      packageNameIid: ParserViewCaptureWindow.PACKAGE_OR_WINDOW_IID,
      windowNameIid: ParserViewCaptureWindow.PACKAGE_OR_WINDOW_IID,
      views: perfettoViews,
    });
  }

  private makeInternedData() {
    const internedWindowNames: perfetto.protos.InternedString[] = [
      perfetto.protos.InternedString.fromObject({
        iid: Long.fromNumber(ParserViewCaptureWindow.PACKAGE_OR_WINDOW_IID),
        str: utf8Encode(this.windowName),
      }),
    ];

    const internedClassNames: perfetto.protos.InternedString[] =
      this.classNames.map((className, index) => {
        return perfetto.protos.InternedString.fromObject({
          iid: Long.fromNumber(index),
          str: utf8Encode(className),
        });
      });

    const internedPackageNames: perfetto.protos.InternedString[] = [
      perfetto.protos.InternedString.fromObject({
        iid: Long.fromNumber(ParserViewCaptureWindow.PACKAGE_OR_WINDOW_IID),
        str: utf8Encode(this.packageName),
      }),
    ];

    const internedViewIds: perfetto.protos.InternedString[] = [];
    assertDefined(this.viewIdToIid).forEach((iid, viewId) => {
      internedViewIds.push(
        perfetto.protos.InternedString.fromObject({
          iid: Long.fromNumber(iid),
          str: utf8Encode(viewId),
        }),
      );
    });

    return perfetto.protos.InternedData.fromObject({
      viewcaptureWindowName: internedWindowNames,
      viewcaptureClassName: internedClassNames,
      viewcapturePackageName: internedPackageNames,
      viewcaptureViewId: internedViewIds,
    });
  }
}

type FrameData = com.android.app.viewcapture.data.IFrameData;
type ViewNode = com.android.app.viewcapture.data.IViewNode;
type PerfettoView = perfetto.protos.ViewCapture.IView;
