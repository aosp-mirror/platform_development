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

import {CustomQueryType} from 'trace/custom_query';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';

export class ViewCaptureUtils {
  static readonly NEXUS_LAUNCHER_PACKAGE_NAME =
    'com.google.android.apps.nexuslauncher';

  static async getPackageNames(traces: Traces): Promise<string[]> {
    const packageNames: string[] = [];

    const viewCaptureTraces = [
      traces.getTrace(TraceType.VIEW_CAPTURE_LAUNCHER_ACTIVITY),
      traces.getTrace(TraceType.VIEW_CAPTURE_TASKBAR_DRAG_LAYER),
      traces.getTrace(TraceType.VIEW_CAPTURE_TASKBAR_OVERLAY_DRAG_LAYER),
    ].filter((trace) => trace !== undefined) as Array<Trace<object>>;

    for (const trace of viewCaptureTraces) {
      const packageName = await trace.customQuery(
        CustomQueryType.VIEW_CAPTURE_PACKAGE_NAME,
      );
      if (packageNames.includes(packageName)) {
        continue;
      }
      packageNames.push(packageName);
    }

    return packageNames;
  }
}
