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

/**
 * Utility class for URL-related functions.
 */
export class UrlUtils {
  /**
   * Returns the root URL of the current page.
   *
   * @return The root URL.
   */
  static getRootUrl(): string {
    const fullUrl = window.location.href;
    const posLastSlash = fullUrl.lastIndexOf('/');
    return fullUrl.slice(0, posLastSlash + 1);
  }
}
