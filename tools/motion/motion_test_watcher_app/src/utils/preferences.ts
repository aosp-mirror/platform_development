/*
 * Copyright 2024 Google LLC
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

import { Injectable } from '@angular/core';

export const LOOP_VIDEO = 'loop_video';

type PreferencesData = {
  loopVideo: boolean;
  playbackRate: number;
};

const PREFERENCES_DEFAULT_JSON = JSON.stringify({
  loopVideo: false,
  playbackRate: 1,
});

@Injectable()
export class Preferences {
  private _preferences: PreferencesData;

  constructor() {
    this._preferences = loadPreferences();
  }

  get loopVideo(): boolean {
    return this._preferences.loopVideo;
  }

  set loopVideo(value: boolean) {
    if (this.loopVideo == value) return;
    this._preferences.loopVideo = value;
    storePreferences(this._preferences);
  }

  get playbackRate(): number {
    return this._preferences.playbackRate;
  }

  set playbackRate(value: number) {
    if (this.playbackRate == value) return;
    this._preferences.playbackRate = value;
    storePreferences(this._preferences);
  }
}

function loadPreferences(): PreferencesData {
  const defaults = JSON.parse(PREFERENCES_DEFAULT_JSON);
  try {
    return {
      ...defaults,
      ...JSON.parse(localStorage.getItem('preferences') ?? '{}'),
    };
  } catch (e) {
    console.error('Unable to restore preferences', e);
    return defaults;
  }
}

function storePreferences(preferences: PreferencesData) {
  localStorage.setItem('preferences', JSON.stringify(preferences));
}
