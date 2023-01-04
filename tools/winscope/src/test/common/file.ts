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

// This class is needed for testing because Node.js doesn't provide the Web API's File type
import {Blob} from './blob';

class File extends Blob {
  constructor(buffer: ArrayBuffer, fileName: string) {
    super(buffer);
    this.name = fileName;
  }

  readonly lastModified: number = 0;
  readonly name: string;
  readonly webkitRelativePath: string = '';
}

export {File};
