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
  private static BUGANIZER_OPENED = 'buganizer_opened';
  private static CROSS_TOOL_SYNC = 'cross_tool_sync';
  private static DARK_MODE_ENABLED = 'dark_mode_enabled';
  private static DOCUMENTATION_OPENED = 'documentation_opened';
  private static EXPANDED_TIMELINE_OPENED = 'expanded_timeline_opened';
  private static GLOBAL_EXCEPTION = 'global_exception';
  private static HIERARCHY_SETTINGS = 'hierarchy_settings';
  private static NAVIGATION_ZOOM_EVENT = 'navigation_zoom';
  private static PROPERTIES_SETTINGS = 'properties_settings';
  private static PROXY_ERROR = 'proxy_error';
  private static RECT_SETTINGS = 'rect_settings';
  private static REFRESH_DUMPS = 'refresh_dumps';
  private static TIME_BOOKMARK = 'time_bookmark';
  private static TIME_COPIED = 'time_copied';
  private static TIME_INPUT = 'time_input';
  private static TRACE_TAB_SWITCHED = 'trace_tab_switched';
  private static TRACE_TIMELINE_DESELECTED = 'trace_timeline_deselected';
  private static TRACING_LOADED_EVENT = 'tracing_trace_loaded';
  private static TRACING_COLLECT_DUMP = 'tracing_collect_dump';
  private static TRACING_COLLECT_TRACE = 'tracing_collect_trace';
  private static TRACING_OPEN_FROM_ABT = 'tracing_from_abt';
  private static USER_WARNING = 'user_warning';

  static Error = class {
    static logGlobalException(description: string) {
      Analytics.doLogEvent(Analytics.GLOBAL_EXCEPTION, {
        description,
      } as Gtag.CustomParams);
    }
    static logProxyError(description: string) {
      Analytics.doLogEvent(Analytics.PROXY_ERROR, {
        description,
      } as Gtag.CustomParams);
    }
  };

  static Help = class {
    static logDocumentationOpened() {
      Analytics.doLogEvent(Analytics.DOCUMENTATION_OPENED);
    }

    static logBuganizerOpened() {
      Analytics.doLogEvent(Analytics.BUGANIZER_OPENED);
    }
  };

  static Settings = class {
    static logDarkModeEnabled() {
      Analytics.doLogEvent(Analytics.DARK_MODE_ENABLED);
    }
    static logCrossToolSync(value: boolean) {
      Analytics.doLogEvent(Analytics.CROSS_TOOL_SYNC, {
        value,
      } as Gtag.CustomParams);
    }
  };

  static Navigation = class {
    static logExpandedTimelineOpened() {
      Analytics.doLogEvent(Analytics.EXPANDED_TIMELINE_OPENED);
    }

    static logHierarchySettingsChanged(
      option: string,
      value: boolean,
      traceType: string,
    ) {
      Analytics.doLogEvent(Analytics.HIERARCHY_SETTINGS, {
        option,
        value,
        traceType,
      } as Gtag.CustomParams);
    }

    static logPropertiesSettingsChanged(
      option: string,
      value: boolean,
      traceType: string,
    ) {
      Analytics.doLogEvent(Analytics.PROPERTIES_SETTINGS, {
        option,
        value,
        traceType,
      } as Gtag.CustomParams);
    }

    static logRectSettingsChanged(
      option: string,
      value: string | number | boolean,
      traceType: string,
    ) {
      Analytics.doLogEvent(Analytics.RECT_SETTINGS, {
        option,
        value,
        traceType,
      } as Gtag.CustomParams);
    }

    static logTabSwitched(tabTraceType: string) {
      Analytics.doLogEvent(Analytics.TRACE_TAB_SWITCHED, {
        type: tabTraceType,
      } as Gtag.CustomParams);
    }

    static logTimeCopied(type: 'ns' | 'human') {
      Analytics.doLogEvent(Analytics.TIME_COPIED, {
        type,
      } as Gtag.CustomParams);
    }

    static logTimeInput(type: 'ns' | 'human') {
      Analytics.doLogEvent(Analytics.TIME_INPUT, {
        type,
      } as Gtag.CustomParams);
    }

    static logTimeBookmark() {
      Analytics.doLogEvent(Analytics.TIME_BOOKMARK);
    }

    static logTraceTimelineDeselected(type: string) {
      Analytics.doLogEvent(Analytics.TRACE_TIMELINE_DESELECTED, {
        type,
      } as Gtag.CustomParams);
    }

    static logZoom(
      type: 'scroll' | 'button' | 'reset' | 'key',
      component: 'rects' | 'timeline',
      direction?: 'in' | 'out',
    ) {
      Analytics.doLogEvent(Analytics.NAVIGATION_ZOOM_EVENT, {
        direction,
        component,
        type,
      } as Gtag.CustomParams);
    }
  };

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

    static logRefreshDumps() {
      Analytics.doLogEvent(Analytics.REFRESH_DUMPS);
    }
  };

  static UserNotification = class {
    static logUserWarning(description: string) {
      Analytics.doLogEvent(Analytics.USER_WARNING, {
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
