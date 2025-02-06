/*
 * Copyright 2022 Google LLC
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

/**
 * Tracks activity that should show a progress bar.
 */
@Injectable()
export class ProgressTracker extends EventTarget {
  private progressCount = 0;

  get isActive() {
    return this.progressCount > 0;
  }

  trackPromise<T>(promise: Promise<T>): Promise<T> {
    queueMicrotask(() => {
      this.beginProgress();
      promise.finally(() => this.endProgress());
    });
    return promise;
  }

  /**
   *  Marks the beginning of the progress.
   *
   *  Must be followed by exactly one endProgress` call.
   */
  beginProgress() {
    this.progressCount++;
    if (this.progressCount == 1) {
      this.dispatchEvent(new Event('progress-started'));
    }
  }

  /**
   *  Ends a previously started progress.
   */
  endProgress() {
    console.assert(this.progressCount > 0);
    this.progressCount--;
    if (this.progressCount == 0) {
      this.dispatchEvent(new Event('progress-ended'));
    }
  }
}
