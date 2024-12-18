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

import {Store} from './store';

/**
 * A persistent store implementation that uses localStorage to store data.
 */
export class PersistentStore implements Store {
  add(key: string, value: string) {
    localStorage.setItem(key, value);
  }

  get(key: string): string | undefined {
    return localStorage.getItem(key) ?? undefined;
  }

  clear(keySubstring: string) {
    Object.keys(localStorage).forEach((key) => {
      if (key.includes(keySubstring)) localStorage.removeItem(key);
    });
  }
}
