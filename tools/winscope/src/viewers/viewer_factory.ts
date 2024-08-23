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

import {assertTrue} from 'common/assert_utils';
import {Trace} from 'trace/trace';
import {Traces} from 'trace/traces';
import {TraceType, TraceTypeUtils} from 'trace/trace_type';
import {Viewer} from './viewer';
import {ViewerInput} from './viewer_input/viewer_input';
import {ViewerInputMethodClients} from './viewer_input_method_clients/viewer_input_method_clients';
import {ViewerInputMethodManagerService} from './viewer_input_method_manager_service/viewer_input_method_manager_service';
import {ViewerInputMethodService} from './viewer_input_method_service/viewer_input_method_service';
import {ViewerJankCujs} from './viewer_jank_cujs/viewer_jank_cujs';
import {ViewerScreenshot} from './viewer_media_based/viewer_screenshot';
import {ViewerScreenRecording} from './viewer_media_based/viewer_screen_recording';
import {ViewerProtoLog} from './viewer_protolog/viewer_protolog';
import {ViewerSurfaceFlinger} from './viewer_surface_flinger/viewer_surface_flinger';
import {ViewerTransactions} from './viewer_transactions/viewer_transactions';
import {ViewerTransitions} from './viewer_transitions/viewer_transitions';
import {ViewerViewCapture} from './viewer_view_capture/viewer_view_capture';
import {ViewerWindowManager} from './viewer_window_manager/viewer_window_manager';

class ViewerFactory {
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
    ViewerJankCujs,
  ];

  createViewers(traces: Traces, storage: Storage): Viewer[] {
    const viewers: Viewer[] = [];

    // regular viewers
    traces.forEachTrace((trace) => {
      ViewerFactory.VIEWERS.forEach((Viewer) => {
        assertTrue(Viewer.DEPENDENCIES.length === 1);
        const isViewerDepSatisfied = trace.type === Viewer.DEPENDENCIES[0];
        if (isViewerDepSatisfied) {
          viewers.push(new Viewer(trace as Trace<any>, traces, storage));
        }
      });
    });

    const availableTraceTypes = new Set(traces.mapTrace((trace) => trace.type));

    {
      // ViewCapture viewer (instantiate one viewer for N ViewCapture traces)
      const isViewerDepSatisfied = ViewerViewCapture.DEPENDENCIES.some(
        (traceType: TraceType) => availableTraceTypes.has(traceType),
      );
      if (isViewerDepSatisfied) {
        viewers.push(new ViewerViewCapture(traces, storage));
      }
    }

    {
      // Input viewer
      const isViewerDepSatisfied = ViewerInput.DEPENDENCIES.some(
        (traceType: TraceType) => availableTraceTypes.has(traceType),
      );
      if (isViewerDepSatisfied) {
        viewers.push(new ViewerInput(traces, storage));
      }
    }

    // Note:
    // the final order of tabs/views in the UI corresponds the order of the
    // respective viewers below
    return viewers.sort((a, b) =>
      TraceTypeUtils.compareByDisplayOrder(
        a.getTraces()[0].type,
        b.getTraces()[0].type,
      ),
    );
  }
}

export {ViewerFactory};
