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

export default abstract class DumpBase implements IDump {
  data: any;
  _files: any[];

  constructor(data, files) {
    this.data = data;
    this._files = files;
  }

  get files(): readonly any[] {
    return Object.values(this._files).flat();
  }

  abstract get type(): String;
}

interface IDump {
  files: readonly Object[];
  type: String,
}