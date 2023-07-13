/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {AppEvent} from 'app/app_event';
import {AppEventEmitter, EmitAppEvent} from './app_event_emitter';
import {AppEventListener} from './app_event_listener';

export class AppEventListenerEmitterStub implements AppEventListener, AppEventEmitter {
  async onAppEvent(event: AppEvent) {
    // do nothing
  }

  setEmitAppEvent(callback: EmitAppEvent) {
    // do nothing
  }
}
