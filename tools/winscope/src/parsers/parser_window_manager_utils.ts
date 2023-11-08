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
import {CustomQueryParserResultTypeMap, CustomQueryType} from 'trace/custom_query';

type WindowsTokenAndTitle =
  CustomQueryParserResultTypeMap[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE];

export class ParserWindowManagerUtils {
  private static readonly NA = 'n/a';

  static parseWindowsTokenAndTitle(rootWindowContainerProto: any, result: WindowsTokenAndTitle) {
    const token =
      rootWindowContainerProto?.windowContainer?.identifier?.hashCode?.toString(16) ??
      ParserWindowManagerUtils.NA;
    const title =
      rootWindowContainerProto?.windowContainer?.identifier?.title ?? ParserWindowManagerUtils.NA;
    result.push({token, title});
    ParserWindowManagerUtils.parseWindowContainerProto(
      rootWindowContainerProto?.windowContainer,
      result
    );
  }

  private static parseWindowContainerProto(proto: any, result: WindowsTokenAndTitle) {
    for (const windowContainerChildProto of proto?.children ?? []) {
      if (windowContainerChildProto.activity) {
        ParserWindowManagerUtils.parseActivityRecordProto(
          windowContainerChildProto.activity,
          result
        );
      }
      if (windowContainerChildProto.displayArea) {
        ParserWindowManagerUtils.parseDisplayAreaProto(
          windowContainerChildProto.displayArea,
          result
        );
      }
      if (windowContainerChildProto.displayContent) {
        ParserWindowManagerUtils.parseDisplayContentProto(
          windowContainerChildProto.displayContent,
          result
        );
      }
      if (windowContainerChildProto.task) {
        ParserWindowManagerUtils.parseTaskProto(windowContainerChildProto.task, result);
      }
      if (windowContainerChildProto.taskFragment) {
        ParserWindowManagerUtils.parseTaskFragmentProto(
          windowContainerChildProto.taskFragment,
          result
        );
      }
      if (windowContainerChildProto.window) {
        ParserWindowManagerUtils.parseWindowStateProto(windowContainerChildProto.window, result);
      }
      if (windowContainerChildProto.windowContainer) {
        ParserWindowManagerUtils.parseWindowContainerProto(
          windowContainerChildProto.windowContainer,
          result
        );
      }
      if (windowContainerChildProto.windowToken) {
        ParserWindowManagerUtils.parseWindowTokenProto(
          windowContainerChildProto.windowToken,
          result
        );
      }
    }
  }

  private static parseActivityRecordProto(proto: any, result: WindowsTokenAndTitle) {
    if (proto === undefined) {
      return;
    }
    const token = proto.windowToken?.hashCode?.toString(16) ?? ParserWindowManagerUtils.NA;
    const title = proto.name ?? ParserWindowManagerUtils.NA;
    result.push({token, title});
    ParserWindowManagerUtils.parseWindowContainerProto(proto.windowToken?.windowContainer, result);
  }

  private static parseDisplayAreaProto(proto: any, result: WindowsTokenAndTitle) {
    if (proto === undefined) {
      return;
    }
    const token =
      proto.windowContainer?.identifier?.hashCode?.toString(16) ?? ParserWindowManagerUtils.NA;
    const title = proto.name ?? ParserWindowManagerUtils.NA;
    result.push({token, title});
    ParserWindowManagerUtils.parseWindowContainerProto(proto.windowContainer, result);
  }

  private static parseDisplayContentProto(proto: any, result: WindowsTokenAndTitle) {
    if (proto === undefined) {
      return;
    }
    const token =
      proto.rootDisplayArea?.windowContainer?.identifier?.hashCode?.toString(16) ??
      ParserWindowManagerUtils.NA;
    const title = proto.displayInfo?.name ?? ParserWindowManagerUtils.NA;
    result.push({token, title});
    ParserWindowManagerUtils.parseWindowContainerProto(
      proto.rootDisplayArea?.windowContainer,
      result
    );
  }

  private static parseTaskProto(proto: any, result: WindowsTokenAndTitle) {
    if (proto === undefined) {
      return;
    }
    const token =
      proto.taskFragment?.windowContainer?.identifier?.hashCode?.toString(16) ??
      ParserWindowManagerUtils.NA;
    const title =
      proto.taskFragment?.windowContainer?.identifier?.title ?? ParserWindowManagerUtils.NA;
    result.push({token, title});
    for (const activity of proto.activities ?? []) {
      ParserWindowManagerUtils.parseActivityRecordProto(activity, result);
    }
    ParserWindowManagerUtils.parseTaskFragmentProto(proto.taskFragment, result);
  }

  private static parseTaskFragmentProto(proto: any, result: WindowsTokenAndTitle) {
    if (proto === undefined) {
      return;
    }
    ParserWindowManagerUtils.parseWindowContainerProto(proto?.windowContainer, result);
  }

  private static parseWindowStateProto(proto: any, result: WindowsTokenAndTitle) {
    if (proto === undefined) {
      return;
    }
    const token =
      proto.windowContainer?.identifier?.hashCode?.toString(16) ?? ParserWindowManagerUtils.NA;
    const title = proto.windowContainer?.identifier?.title ?? ParserWindowManagerUtils.NA;
    result.push({token, title});
    ParserWindowManagerUtils.parseWindowContainerProto(proto.windowContainer, result);
  }

  private static parseWindowTokenProto(proto: any, result: WindowsTokenAndTitle) {
    if (proto === undefined) {
      return;
    }
    const hash = proto.hashCode?.toString(16) ?? ParserWindowManagerUtils.NA;
    result.push({token: hash, title: hash});
    ParserWindowManagerUtils.parseWindowContainerProto(proto.windowContainer, result);
  }
}
