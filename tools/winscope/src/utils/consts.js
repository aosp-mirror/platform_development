/*
 * Copyright 2020, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import TransitionType from "../flickerlib/tags/TransitionType";

/**
 * Should be kept in sync with ENUM is in Google3 under:
 * google3/wireless/android/tools/android_bug_tool/extension/common/actions
 */
const WebContentScriptMessageType = {
  UNKNOWN: 0,
  CONVERT_OBJECT_URL: 1,
  CONVERT_OBJECT_URL_RESPONSE: 2,
};

const NAVIGATION_STYLE = {
  GLOBAL: 'Global',
  FOCUSED: 'Focused',
  CUSTOM: 'Custom',
  TARGETED: 'Targeted',
};

const SEARCH_TYPE = {
  TRANSITIONS: 'Transitions',
  ERRORS: 'Errors',
  TIMESTAMP: 'Timestamp',
};

const logLevel = {
  INFO: 'info',
  DEBUG: 'debug',
  VERBOSE: 'verbose',
  WARN: 'warn',
  ERROR: 'error',
  WTF: 'wtf',
};

const transitionMap = new Map([
  [TransitionType.ROTATION, {desc: 'Rotation', color: '#9900ffff'}],
  [TransitionType.PIP_ENTER, {desc: 'Entering PIP mode', color: '#4a86e8ff'}],
  [TransitionType.PIP_RESIZE, {desc: 'Resizing PIP mode', color: '#2b9e94ff'}],
  [TransitionType.PIP_EXPAND, {desc: 'Expanding PIP mode', color: 'rgb(57, 57, 182)'}],
  [TransitionType.PIP_EXIT, {desc: 'Exiting PIP mode', color: 'darkblue'}],
  [TransitionType.APP_LAUNCH, {desc: 'Launching app', color: '#ef6befff'}],
  [TransitionType.APP_CLOSE, {desc: 'Closing app', color: '#d10ddfff'}],
  [TransitionType.IME_APPEAR, {desc: 'IME appearing', color: '#ff9900ff'}],
  [TransitionType.IME_DISAPPEAR, {desc: 'IME disappearing', color: '#ad6800ff'}],
  [TransitionType.APP_PAIRS_ENTER, {desc: 'Entering app pairs mode', color: 'rgb(58, 151, 39)'}],
  [TransitionType.APP_PAIRS_EXIT, {desc: 'Exiting app pairs mode', color: 'rgb(45, 110, 32)'}],
])

//used to split timestamp search input by unit, to convert to nanoseconds
const regExpTimestampSearch = new RegExp(/^\d+$/);

export { WebContentScriptMessageType, NAVIGATION_STYLE, SEARCH_TYPE, logLevel, transitionMap, regExpTimestampSearch };
