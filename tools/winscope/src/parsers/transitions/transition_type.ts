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

const FRST_CUSTOM = 1000;

export enum TransitionType {
  UNDEFINED = -1,
  NONE = 0,
  OPEN = 1,
  CLOSE = 2,
  TO_FRONT = 3,
  TO_BACK = 4,
  RELAUNCH = 5,
  CHANGE = 6,
  KEYGUARD_GOING_AWAY = 7,
  KEYGUARD_OCCLUDE = 8,
  KEYGUARD_UNOCCLUDE = 9,
  PIP = 10,
  WAKE = 11,
  SLEEP = 12,
  PREPARE_BACK_NAVIGATION = 13,
  CLOSE_PREPARE_BACK_NAVIGATION = 14,

  FIRST_CUSTOM = FRST_CUSTOM,
  EXIT_PIP = FIRST_CUSTOM + 1,
  EXIT_PIP_TO_SPLIT = FIRST_CUSTOM + 2,
  REMOVE_PIP = FIRST_CUSTOM + 3,
  SPLIT_SCREEN_PAIR_OPEN = FIRST_CUSTOM + 4,
  SPLIT_SCREEN_OPEN_TO_SIDE = FIRST_CUSTOM + 5,
  SPLIT_DISMISS_SNAP = FIRST_CUSTOM + 6,
  SPLIT_DISMISS = FIRST_CUSTOM + 7,
  MAXIMIZE = FIRST_CUSTOM + 8,
  RESTORE_FROM_MAXIMIZE = FIRST_CUSTOM + 9,
  ENTER_FREEFORM = FIRST_CUSTOM + 10,
  ENTER_DESKTOP_MODE = FIRST_CUSTOM + 11,
  EXIT_DESKTOP_MODE = FIRST_CUSTOM + 12,
}
