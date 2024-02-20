/*
 * Copyright (C) 2022 The Android Open Source Project
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

import {Traces} from 'trace/traces';
import {TraceType} from 'trace/trace_type';
import {Viewer} from './viewer';
import {ViewerInputMethodClients} from './viewer_input_method_clients/viewer_input_method_clients';
import {ViewerInputMethodManagerService} from './viewer_input_method_manager_service/viewer_input_method_manager_service';
import {ViewerInputMethodService} from './viewer_input_method_service/viewer_input_method_service';
import {ViewerProtoLog} from './viewer_protolog/viewer_protolog';
import {ViewerScreenshot} from './viewer_screen_recording/viewer_screenshot';
import {ViewerScreenRecording} from './viewer_screen_recording/viewer_screen_recording';
import {ViewerSurfaceFlinger} from './viewer_surface_flinger/viewer_surface_flinger';
import {ViewerTransactions} from './viewer_transactions/viewer_transactions';
import {ViewerTransitions} from './viewer_transitions/viewer_transitions';
import {
  ViewerViewCaptureLauncherActivity,
  ViewerViewCaptureTaskbarDragLayer,
  ViewerViewCaptureTaskbarOverlayDragLayer,
} from './viewer_view_capture/viewer_view_capture';
import {ViewerWindowManager} from './viewer_window_manager/viewer_window_manager';

class ViewerFactory {
  // Note:
  // the final order of tabs/views in the UI corresponds the order of the
  // respective viewers below
  static readonly VIEWERS = [
    ViewerSurfaceFlinger,
    ViewerWindowManager,
    ViewerInputMethodClients,
    ViewerInputMethodManagerService,
    ViewerInputMethodService,
    ViewerTransactions,
    ViewerProtoLog,
    ViewerScreenRecording,
    ViewerScreenshot,
    ViewerTransitions,
    ViewerViewCaptureLauncherActivity,
    ViewerViewCaptureTaskbarDragLayer,
    ViewerViewCaptureTaskbarOverlayDragLayer,
  ];

  createViewers(traces: Traces, storage: Storage): Viewer[] {
    const activeTraceTypes = new Set(traces.mapTrace((trace) => trace.type));
    const viewers: Viewer[] = [];

    for (const Viewer of ViewerFactory.VIEWERS) {
      const areViewerDepsSatisfied = Viewer.DEPENDENCIES.every(
        (traceType: TraceType) => activeTraceTypes.has(traceType),
      );

      if (areViewerDepsSatisfied) {
        viewers.push(new Viewer(traces, storage));
      }
    }

    return viewers;
  }
}

export {ViewerFactory};
