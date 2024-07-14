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

import {TraceEntry} from 'trace/trace';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';
import {
  LogEntry,
  LogField,
  LogFieldType,
  UiDataLog,
} from 'viewers/common/ui_data_log';
import {UiPropertyTreeNode} from 'viewers/common/ui_property_tree_node';

export class UiData implements UiDataLog {
  constructor(
    public headers: LogFieldType[],
    public entries: LogEntry[],
    public selectedIndex: undefined | number,
    public scrollToIndex: undefined | number,
    public propertiesTree: undefined | UiPropertyTreeNode,
  ) {}

  static createEmpty() {
    return new UiData([], [], undefined, undefined, undefined);
  }
}
export class CujEntry implements LogEntry {
  constructor(
    public traceEntry: TraceEntry<PropertyTreeNode>,
    public fields: LogField[],
    public propertiesTree: PropertyTreeNode | undefined,
  ) {}
}

export enum CujStatus {
  EXECUTED = 'EXECUTED',
  CANCELLED = 'CANCELLED',
}

/**
 * Source of truth found at:`frameworks/base/core/java/com/android/internal/jank/Cuj.java`
 */
export enum CujType {
  NOTIFICATION_SHADE_EXPAND_COLLAPSE = 0,
  NOTIFICATION_SHADE_SCROLL_FLING = 2,
  NOTIFICATION_SHADE_ROW_EXPAND = 3,
  NOTIFICATION_SHADE_ROW_SWIPE = 4,
  NOTIFICATION_SHADE_QS_EXPAND_COLLAPSE = 5,
  NOTIFICATION_SHADE_QS_SCROLL_SWIPE = 6,
  LAUNCHER_APP_LAUNCH_FROM_RECENTS = 7,
  LAUNCHER_APP_LAUNCH_FROM_ICON = 8,
  LAUNCHER_APP_CLOSE_TO_HOME = 9,
  LAUNCHER_APP_CLOSE_TO_PIP = 10,
  LAUNCHER_QUICK_SWITCH = 11,
  NOTIFICATION_HEADS_UP_APPEAR = 12,
  NOTIFICATION_HEADS_UP_DISAPPEAR = 13,
  NOTIFICATION_ADD = 14,
  NOTIFICATION_REMOVE = 15,
  NOTIFICATION_APP_START = 16,
  LOCKSCREEN_PASSWORD_APPEAR = 17,
  LOCKSCREEN_PATTERN_APPEAR = 18,
  LOCKSCREEN_PIN_APPEAR = 19,
  LOCKSCREEN_PASSWORD_DISAPPEAR = 20,
  LOCKSCREEN_PATTERN_DISAPPEAR = 21,
  LOCKSCREEN_PIN_DISAPPEAR = 22,
  LOCKSCREEN_TRANSITION_FROM_AOD = 23,
  LOCKSCREEN_TRANSITION_TO_AOD = 24,
  LAUNCHER_OPEN_ALL_APPS = 25,
  LAUNCHER_ALL_APPS_SCROLL = 26,
  LAUNCHER_APP_LAUNCH_FROM_WIDGET = 27,
  SETTINGS_PAGE_SCROLL = 28,
  LOCKSCREEN_UNLOCK_ANIMATION = 29,
  SHADE_APP_LAUNCH_FROM_HISTORY_BUTTON = 30,
  SHADE_APP_LAUNCH_FROM_MEDIA_PLAYER = 31,
  SHADE_APP_LAUNCH_FROM_QS_TILE = 32,
  SHADE_APP_LAUNCH_FROM_SETTINGS_BUTTON = 33,
  STATUS_BAR_APP_LAUNCH_FROM_CALL_CHIP = 34,
  PIP_TRANSITION = 35,
  WALLPAPER_TRANSITION = 36,
  USER_SWITCH = 37,
  SPLASHSCREEN_AVD = 38,
  SPLASHSCREEN_EXIT_ANIM = 39,
  SCREEN_OFF = 40,
  SCREEN_OFF_SHOW_AOD = 41,
  ONE_HANDED_ENTER_TRANSITION = 42,
  ONE_HANDED_EXIT_TRANSITION = 43,
  UNFOLD_ANIM = 44,
  SUW_LOADING_TO_SHOW_INFO_WITH_ACTIONS = 45,
  SUW_SHOW_FUNCTION_SCREEN_WITH_ACTIONS = 46,
  SUW_LOADING_TO_NEXT_FLOW = 47,
  SUW_LOADING_SCREEN_FOR_STATUS = 48,
  SPLIT_SCREEN_ENTER = 49,
  SPLIT_SCREEN_EXIT = 50,
  LOCKSCREEN_LAUNCH_CAMERA = 51, // reserved.
  SPLIT_SCREEN_RESIZE = 52,
  SETTINGS_SLIDER = 53,
  TAKE_SCREENSHOT = 54,
  VOLUME_CONTROL = 55,
  BIOMETRIC_PROMPT_TRANSITION = 56,
  SETTINGS_TOGGLE = 57,
  SHADE_DIALOG_OPEN = 58,
  USER_DIALOG_OPEN = 59,
  TASKBAR_EXPAND = 60,
  TASKBAR_COLLAPSE = 61,
  SHADE_CLEAR_ALL = 62,
  LAUNCHER_UNLOCK_ENTRANCE_ANIMATION = 63,
  LOCKSCREEN_OCCLUSION = 64,
  RECENTS_SCROLLING = 65,
  LAUNCHER_APP_SWIPE_TO_RECENTS = 66,
  LAUNCHER_CLOSE_ALL_APPS_SWIPE = 67,
  LAUNCHER_CLOSE_ALL_APPS_TO_HOME = 68,
  LOCKSCREEN_CLOCK_MOVE_ANIMATION = 70,
  LAUNCHER_OPEN_SEARCH_RESULT = 71,
  // 72 - 77 are reserved for b/281564325.

  /**
   * In some cases when we do not have any end-target, we play a simple slide-down animation.
   * eg: Open an app from Overview/Task switcher such that there is no home-screen icon.
   * eg: Exit the app using back gesture.
   */
  LAUNCHER_APP_CLOSE_TO_HOME_FALLBACK = 78,
  // 79 is reserved.
  IME_INSETS_SHOW_ANIMATION = 80,
  IME_INSETS_HIDE_ANIMATION = 81,

  SPLIT_SCREEN_DOUBLE_TAP_DIVIDER = 82,

  LAUNCHER_UNFOLD_ANIM = 83,

  PREDICTIVE_BACK_CROSS_ACTIVITY = 84,
  PREDICTIVE_BACK_CROSS_TASK = 85,
  PREDICTIVE_BACK_HOME = 86,
  // 87 is reserved - previously assigned to deprecated CUJ_LAUNCHER_SEARCH_QSB_OPEN.
  BACK_PANEL_ARROW = 88,
  LAUNCHER_CLOSE_ALL_APPS_BACK = 89,
  LAUNCHER_SEARCH_QSB_WEB_SEARCH = 90,
  LAUNCHER_LAUNCH_APP_PAIR_FROM_WORKSPACE = 91,
  LAUNCHER_LAUNCH_APP_PAIR_FROM_TASKBAR = 92,
  LAUNCHER_SAVE_APP_PAIR = 93,
  LAUNCHER_ALL_APPS_SEARCH_BACK = 95,
  LAUNCHER_TASKBAR_ALL_APPS_CLOSE_BACK = 96,
  LAUNCHER_TASKBAR_ALL_APPS_SEARCH_BACK = 97,
  LAUNCHER_WIDGET_PICKER_CLOSE_BACK = 98,
  LAUNCHER_WIDGET_PICKER_SEARCH_BACK = 99,
  LAUNCHER_WIDGET_BOTTOM_SHEET_CLOSE_BACK = 100,
  LAUNCHER_WIDGET_EDU_SHEET_CLOSE_BACK = 101,
  LAUNCHER_PRIVATE_SPACE_LOCK = 102,
  LAUNCHER_PRIVATE_SPACE_UNLOCK = 103,
}
