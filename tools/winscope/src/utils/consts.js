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

const logLevel = {
  INFO: 'info',
  DEBUG: 'debug',
  VERBOSE: 'verbose',
  WARN: 'warn',
  ERROR: 'error',
  WTF: 'wtf',
}

export { WebContentScriptMessageType, NAVIGATION_STYLE, logLevel };
