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
export enum ProxyState {
  ERROR = 0,
  CONNECTING = 1,
  NO_PROXY = 2,
  INVALID_VERSION = 3,
  UNAUTH = 4,
  DEVICES = 5,
  START_TRACE = 6,
  END_TRACE = 7,
  LOAD_DATA = 8,
}
