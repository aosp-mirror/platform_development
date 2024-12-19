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

/**
 * A simple key-value store interface.
 */
export interface Store {
  /**
   * Adds a key-value pair to the store.
   * @param key The key of the key-value pair.
   * @param value The value of the key-value pair.
   */
  add(key: string, value: string): void;

  /**
   * Retrieves the value associated with a key.
   * @param key The key to retrieve the value for.
   * @return The value associated with the key, or undefined if the key is not found.
   */
  get(key: string): string | undefined;

  /**
   * Clears all key-value pairs from the store that match a given substring.
   * @param keySubstring The substring to match keys against. Only keys that contain this substring will be cleared.
   */
  clear(keySubstring: string): void;
}
