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

import {FilesSource} from 'app/files_source';
import {globalConfig} from 'common/global_config';
import {CoarseVersion} from 'trace/coarse_version';
import {Parser} from 'trace/parser';
import {TraceType} from 'trace/trace_type';

/* eslint-disable no-undef */
export class Analytics {
  private static BUGANIZER_OPENED = 'buganizer_opened';
  private static CROSS_TOOL_SYNC = 'cross_tool_sync';
  private static DARK_MODE_ENABLED = 'dark_mode_enabled';
  private static DIFF_COMPUTATION_TIME = 'diff_computation_time';
  private static DOCUMENTATION_OPENED = 'documentation_opened';
  private static EXPANDED_TIMELINE_OPENED = 'expanded_timeline_opened';
  private static FETCH_COMPONENT_DATA_TIME = 'fetch_component_data_time';
  private static FILE_EXTRACTION_TIME = 'file_extraction_time';
  private static FILE_PARSING_TIME = 'file_parsing_time';
  private static FRAME_MAP_BUILD_TIME = 'frame_map_build_time';
  private static FRAME_MAP_ERROR = 'frame_map_error';
  private static GLOBAL_EXCEPTION = 'global_exception';
  private static HIERARCHY_SETTINGS = 'hierarchy_settings';
  private static NAVIGATION_ZOOM_EVENT = 'navigation_zoom';
  private static PROPERTIES_SETTINGS = 'properties_settings';
  private static PROXY_ERROR = 'proxy_error';
  private static PROXY_SERVER_NOT_FOUND = 'proxy_server_not_found';
  private static PROXY_NO_FILES_FOUND = 'proxy_no_files_found';
  private static RECT_SETTINGS = 'rect_settings';
  private static REFRESH_DUMPS = 'refresh_dumps';
  private static TP_GENERAL_QUERY_TIME = 'tp_general_query_time';
  private static TP_QUERY_EXECUTION_TIME = 'tp_query_execution_time';
  private static TP_QUERY_REQUESTED = 'tp_query_requested';
  private static TP_QUERY_FAILED = 'tp_query_failed';
  private static TP_QUERY_SAVED = 'tp_query_saved';
  private static TP_SEARCH_INITIALIZATION_TIME =
    'tp_search_initialization_time';
  private static TIME_BOOKMARK = 'time_bookmark';
  private static TIME_COPIED = 'time_copied';
  private static TIME_INPUT = 'time_input';
  private static TIME_PROPAGATED = 'time_propagated';
  private static TRACE_TAB_SWITCHED = 'trace_tab_switched';
  private static TRACE_TIMELINE_DESELECTED = 'trace_timeline_deselected';
  private static TRACING_COLLECT_DUMP = 'tracing_collect_dump';
  private static TRACING_COLLECT_TRACE = 'tracing_collect_trace';
  private static TRACING_LOADED_EVENT = 'tracing_trace_loaded';
  private static TRACING_OPEN_FROM_ABT = 'tracing_from_abt';
  private static TRACING_START_TIME = 'tracing_start_time';
  private static USER_WARNING = 'user_warning';
  private static VIEWER_INITIALIZATION_TIME = 'viewer_initialization_time';

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
    static logFrameMapError(description: string) {
      Analytics.doLogEvent(Analytics.FRAME_MAP_ERROR, {
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

  static Loading = class {
    static logFileExtractionTime(
      type: 'bugreport' | 'device',
      ms: number,
      file_size: number,
    ) {
      Analytics.logTimeMs(Analytics.FILE_EXTRACTION_TIME, ms, {
        type,
        file_size,
      });
    }

    static logFileParsingTime(
      type: 'perfetto' | 'legacy',
      files_source: FilesSource,
      ms: number,
    ) {
      Analytics.logTimeMs(Analytics.FILE_PARSING_TIME, ms, {
        files_source,
        type,
      });
    }

    static logFrameMapBuildTime(ms: number) {
      Analytics.logTimeMs(Analytics.FRAME_MAP_BUILD_TIME, ms);
    }

    static logViewerInitializationTime(
      traceType: string,
      files_source: FilesSource,
      ms: number,
    ) {
      Analytics.logTimeMs(Analytics.VIEWER_INITIALIZATION_TIME, ms, {
        files_source,
        traceType,
      });
    }
  };

  static Navigation = class {
    static logDiffComputationTime(
      component: 'hierarchy' | 'properties',
      traceType: string,
      ms: number,
    ) {
      Analytics.logTimeMs(Analytics.DIFF_COMPUTATION_TIME, ms, {
        component,
        traceType,
      });
    }

    static logExpandedTimelineOpened() {
      Analytics.doLogEvent(Analytics.EXPANDED_TIMELINE_OPENED);
    }

    static logFetchComponentDataTime(
      component: 'hierarchy' | 'properties' | 'rects',
      traceType: string,
      withDiffs: boolean,
      ms: number,
    ) {
      Analytics.logTimeMs(Analytics.FETCH_COMPONENT_DATA_TIME, ms, {
        component,
        traceType,
        withDiffs,
      });
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

    static logTabSwitched(
      tabTraceType: string,
      ms: number,
      first_switch: boolean,
    ) {
      Analytics.logTimeMs(Analytics.TRACE_TAB_SWITCHED, ms, {
        type: tabTraceType,
        first_switch,
      });
    }

    static logTimeBookmark() {
      Analytics.doLogEvent(Analytics.TIME_BOOKMARK);
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

    static logTimePropagated(target: string, ms: number) {
      Analytics.logTimeMs(Analytics.TIME_PROPAGATED, ms, {target});
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

  static Proxy = class {
    static logServerNotFound() {
      Analytics.doLogEvent(Analytics.PROXY_SERVER_NOT_FOUND);
    }

    static logNoFilesFound() {
      Analytics.doLogEvent(Analytics.PROXY_NO_FILES_FOUND);
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

  static TraceProcessor = class {
    static logQueryExecutionTime(ms: number, waitAllRows: boolean) {
      Analytics.logTimeMs(Analytics.TP_GENERAL_QUERY_TIME, ms, {
        waitAllRows,
      });
    }
  };

  static TraceSearch = class {
    static logInitializationTime(traceType: string, ms: number) {
      Analytics.logTimeMs(Analytics.TP_SEARCH_INITIALIZATION_TIME, ms, {
        traceType,
      });
    }
    static logQueryExecutionTime(ms: number) {
      Analytics.logTimeMs(Analytics.TP_QUERY_EXECUTION_TIME, ms);
    }
    static logQueryFailure() {
      Analytics.doLogEvent(Analytics.TP_QUERY_FAILED);
    }
    static logQueryRequested(type: 'new' | 'saved' | 'recent') {
      Analytics.doLogEvent(Analytics.TP_QUERY_REQUESTED, {
        type,
      } as Gtag.CustomParams);
    }
    static logQuerySaved() {
      Analytics.doLogEvent(Analytics.TP_QUERY_SAVED);
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

    static logStartTime(ms: number) {
      Analytics.logTimeMs(Analytics.TRACING_START_TIME, ms);
    }

    static logOpenFromABT() {
      Analytics.doLogEvent(Analytics.TRACING_OPEN_FROM_ABT);
    }

    static logRefreshDumps() {
      Analytics.doLogEvent(Analytics.REFRESH_DUMPS);
    }
  };

  static UserNotification = class {
    static logUserWarning(description: string, message: string) {
      Analytics.doLogEvent(Analytics.USER_WARNING, {
        description,
        message,
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

  private static logTimeMs(
    eventName: string,
    ms: number,
    params?: Gtag.CustomParams,
  ) {
    if (ms > 0) {
      const finalParams = Object.assign({value: ms}, params);
      Analytics.doLogEvent(eventName, finalParams);
    }
  }
}
