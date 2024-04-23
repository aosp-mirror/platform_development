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

import {globalConfig} from 'common/global_config';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';

/* eslint-disable no-undef */
export class Analytics {
  private static NAVIGATION_ZOOM_EVENT = 'navigation_zoom';
  private static TRACING_LOADED_EVENT = 'tracing_trace_loaded';
  private static TRACING_COLLECT_DUMP = 'tracing_collect_dump';
  private static TRACING_COLLECT_TRACE = 'tracing_collect_trace';
  private static TRACING_OPEN_FROM_ABT = 'tracing_from_abt';
  private static USER_WARNING = 'user_warning';
  private static GLOBAL_EXCEPTION = 'global_exception';

  static Tracing = class {
    static logTraceLoaded(parser: Parser<object>) {
      Analytics.doLogEvent(Analytics.TRACING_LOADED_EVENT, {
        type: TraceType[parser.getTraceType()],
        coarse_version: CoarseVersion[parser.getCoarseVersion()],
      } as Gtag.CustomParams);
    }

    static logCollectDumps(requestedDumps: string[]) {
      requestedDumps.forEach((dumpType) => {
        Analytics.doLogEvent(Analytics.TRACING_COLLECT_DUMP, {
          type: dumpType,
        } as Gtag.CustomParams);
      });
    }

    static logCollectTraces(requestedTraces: string[]) {
      requestedTraces.forEach((traceType) => {
        Analytics.doLogEvent(Analytics.TRACING_COLLECT_TRACE, {
          type: traceType,
        } as Gtag.CustomParams);
      });
    }

    static logOpenFromABT() {
      Analytics.doLogEvent(Analytics.TRACING_OPEN_FROM_ABT);
    }
  };

  static Navigation = class {
    static logZoom(
      type: 'scroll' | 'button' | 'reset',
      direction?: 'in' | 'out',
    ) {
      Analytics.doLogEvent(Analytics.NAVIGATION_ZOOM_EVENT, {
        direction,
        type,
      } as Gtag.CustomParams);
    }
  };

  static UserNotification = class {
    static logUserWarning(description: string) {
      Analytics.doLogEvent(Analytics.USER_WARNING, {
        description,
      } as Gtag.CustomParams);
    }
  };

  static Error = class {
    static logGlobalException(description: string) {
      Analytics.doLogEvent(Analytics.GLOBAL_EXCEPTION, {
        description,
      } as Gtag.CustomParams);
    }
  };

  private static doLogEvent(
    eventName: Gtag.EventNames | (string & {}),
    eventParams?: Gtag.ControlParams | Gtag.EventParams | Gtag.CustomParams,
  ) {
    if (globalConfig.MODE === 'PROD') {
      gtag('event', eventName, eventParams);
    }
  }
}
