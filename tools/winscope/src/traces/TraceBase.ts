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

type File = {
  blobUrl: string,
  filename: string,
}

import JSZip from 'jszip';

export default abstract class Trace implements ITrace {
  selectedIndex: Number;
  readonly data: Object;
  readonly timeline: Array<Number>;
  readonly _files: File[];

  constructor(data: any, timeline: Number[], files: any[]) {
    this.selectedIndex = 0;
    this.data = data;
    this.timeline = timeline;
    this._files = files;
  }

  get files(): readonly File[] {
    return Object.values(this._files).flat();
  }

  abstract get type(): String;

  get blobUrl() {
    if (this.files.length == 0) {
      return null;
    }

    if (this.files.length == 1) {
      return this.files[0].blobUrl;
    }

    const zip = new JSZip();

    return (async () => {
      for (const file of this.files) {
        const blob = await fetch(file.blobUrl).then((r) => r.blob());
        zip.file(file.filename, blob);
      }

      return await zip.generateAsync({ type: 'blob' });
    })();

  }
}

interface ITrace {
  files: readonly Object[];
  type: String,
}
