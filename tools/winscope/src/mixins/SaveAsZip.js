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

import JSZip from 'jszip';

export default {
  name: 'SaveAsZip',
  methods: {
    saveAs(blob, filename) {
      const a = document.createElement('a');
      a.style = 'display: none';
      document.body.appendChild(a);

      const url = window.URL.createObjectURL(blob);

      a.href = url;
      a.download = filename;
      a.click();
      window.URL.revokeObjectURL(url);

      document.body.removeChild(a);
    },
    async downloadAsZip(traces) {
      const zip = new JSZip();

      for (const trace of traces) {
        const traceFolder = zip.folder(trace.type);
        for (const file of trace.files) {
          const blob = await fetch(file.blobUrl).then((r) => r.blob());
          traceFolder.file(file.filename, blob);
        }
      }

      const zipFile = await zip.generateAsync({type: 'blob'});

      this.saveAs(zipFile, 'winscope.zip');
    },
  },
};
