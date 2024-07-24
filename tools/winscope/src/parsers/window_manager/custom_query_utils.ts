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
import {com} from 'protos/windowmanager/latest/static';
import {
  CustomQueryParserResultTypeMap,
  CustomQueryType,
} from 'trace/custom_query';

type WindowsTokenAndTitle =
  CustomQueryParserResultTypeMap[CustomQueryType.WM_WINDOWS_TOKEN_AND_TITLE];

export class WmCustomQueryUtils {
  private static readonly NA = 'n/a';

  static parseWindowsTokenAndTitle(
    rootWindowContainer: com.android.server.wm.IRootWindowContainerProto,
    result: WindowsTokenAndTitle,
  ) {
    const token =
      rootWindowContainer?.windowContainer?.identifier?.hashCode?.toString(
        16,
      ) ?? WmCustomQueryUtils.NA;
    const title =
      rootWindowContainer?.windowContainer?.identifier?.title ??
      WmCustomQueryUtils.NA;
    result.push({token, title});
    WmCustomQueryUtils.parseWindowContainerProto(
      rootWindowContainer?.windowContainer,
      result,
    );
  }

  private static parseWindowContainerProto(
    proto: com.android.server.wm.IWindowContainerProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }

    for (const windowContainerChildProto of proto?.children ?? []) {
      WmCustomQueryUtils.parseActivityRecordProto(
        windowContainerChildProto.activity,
        result,
      );
      WmCustomQueryUtils.parseDisplayAreaProto(
        windowContainerChildProto.displayArea,
        result,
      );
      WmCustomQueryUtils.parseDisplayContentProto(
        windowContainerChildProto.displayContent,
        result,
      );
      WmCustomQueryUtils.parseTaskProto(windowContainerChildProto.task, result);
      WmCustomQueryUtils.parseTaskFragmentProto(
        windowContainerChildProto.taskFragment,
        result,
      );
      WmCustomQueryUtils.parseWindowStateProto(
        windowContainerChildProto.window,
        result,
      );
      WmCustomQueryUtils.parseWindowContainerProto(
        windowContainerChildProto.windowContainer,
        result,
      );
      WmCustomQueryUtils.parseWindowTokenProto(
        windowContainerChildProto.windowToken,
        result,
      );
    }
  }

  private static parseActivityRecordProto(
    proto: com.android.server.wm.IActivityRecordProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }
    const token =
      proto.windowToken?.hashCode?.toString(16) ?? WmCustomQueryUtils.NA;
    const title = proto.name ?? WmCustomQueryUtils.NA;
    result.push({token, title});
    WmCustomQueryUtils.parseWindowContainerProto(
      proto.windowToken?.windowContainer,
      result,
    );
  }

  private static parseDisplayAreaProto(
    proto: com.android.server.wm.IDisplayAreaProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }
    const token =
      proto.windowContainer?.identifier?.hashCode?.toString(16) ??
      WmCustomQueryUtils.NA;
    const title = proto.name ?? WmCustomQueryUtils.NA;
    result.push({token, title});
    WmCustomQueryUtils.parseWindowContainerProto(proto.windowContainer, result);
  }

  private static parseDisplayContentProto(
    proto: com.android.server.wm.IDisplayContentProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }
    const token =
      proto.rootDisplayArea?.windowContainer?.identifier?.hashCode?.toString(
        16,
      ) ?? WmCustomQueryUtils.NA;
    const title = proto.displayInfo?.name ?? WmCustomQueryUtils.NA;
    result.push({token, title});
    WmCustomQueryUtils.parseWindowContainerProto(
      proto.rootDisplayArea?.windowContainer,
      result,
    );
  }

  private static parseTaskProto(
    proto: com.android.server.wm.ITaskProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }
    const token =
      proto.taskFragment?.windowContainer?.identifier?.hashCode?.toString(16) ??
      WmCustomQueryUtils.NA;
    const title =
      proto.taskFragment?.windowContainer?.identifier?.title ??
      WmCustomQueryUtils.NA;
    result.push({token, title});
    for (const activity of proto.activities ?? []) {
      WmCustomQueryUtils.parseActivityRecordProto(activity, result);
    }
    WmCustomQueryUtils.parseTaskFragmentProto(proto.taskFragment, result);
  }

  private static parseTaskFragmentProto(
    proto: com.android.server.wm.ITaskFragmentProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }
    WmCustomQueryUtils.parseWindowContainerProto(
      proto?.windowContainer,
      result,
    );
  }

  private static parseWindowStateProto(
    proto: com.android.server.wm.IWindowStateProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }
    const token =
      proto.windowContainer?.identifier?.hashCode?.toString(16) ??
      WmCustomQueryUtils.NA;
    const title =
      proto.windowContainer?.identifier?.title ?? WmCustomQueryUtils.NA;
    result.push({token, title});
    WmCustomQueryUtils.parseWindowContainerProto(proto.windowContainer, result);
  }

  private static parseWindowTokenProto(
    proto: com.android.server.wm.IWindowTokenProto | null | undefined,
    result: WindowsTokenAndTitle,
  ) {
    if (!proto) {
      return;
    }
    const hash = proto.hashCode?.toString(16) ?? WmCustomQueryUtils.NA;
    result.push({token: hash, title: hash});
    WmCustomQueryUtils.parseWindowContainerProto(proto.windowContainer, result);
  }
}
