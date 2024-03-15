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

import {assertDefined} from 'common/assert_utils';
import {WinscopeError, WinscopeErrorType} from 'messaging/winscope_error';
import {WinscopeErrorListener} from 'messaging/winscope_error_listener';
import {TraceFile} from 'trace/trace_file';

export interface FilterResult {
  perfetto?: TraceFile;
  legacy: TraceFile[];
}

export class TraceFileFilter {
  private static readonly BUGREPORT_SYSTRACE_PATH =
    'FS/data/misc/perfetto-traces/bugreport/systrace.pftrace';
  private static readonly BUGREPORT_LEGACY_FILES_ALLOWLIST = [
    'FS/data/misc/wmtrace/',
    'FS/data/misc/perfetto-traces/',
    'proto/window_CRITICAL.proto',
  ];

  async filter(files: TraceFile[], errorListener: WinscopeErrorListener): Promise<FilterResult> {
    const bugreportMainEntry = files.find((file) => file.file.name === 'main_entry.txt');
    const perfettoFiles = files.filter((file) => this.isPerfettoFile(file));
    const legacyFiles = files.filter((file) => !this.isPerfettoFile(file));

    if (!(await this.isBugreport(bugreportMainEntry, files))) {
      const perfettoFile = this.pickLargestFile(perfettoFiles, errorListener);
      return {
        perfetto: perfettoFile,
        legacy: legacyFiles,
      };
    }

    return this.filterBugreport(assertDefined(bugreportMainEntry), perfettoFiles, legacyFiles);
  }

  private async isBugreport(
    bugreportMainEntry: TraceFile | undefined,
    files: TraceFile[]
  ): Promise<boolean> {
    if (!bugreportMainEntry) {
      return false;
    }
    const bugreportName = (await bugreportMainEntry.file.text()).trim();
    return (
      files.find(
        (file) =>
          file.parentArchive === bugreportMainEntry.parentArchive &&
          file.file.name === bugreportName
      ) !== undefined
    );
  }

  private filterBugreport(
    bugreportMainEntry: TraceFile,
    perfettoFiles: TraceFile[],
    legacyFiles: TraceFile[]
  ): FilterResult {
    const isFileAllowlisted = (file: TraceFile) => {
      for (const traceDir of TraceFileFilter.BUGREPORT_LEGACY_FILES_ALLOWLIST) {
        if (file.file.name.startsWith(traceDir)) {
          return true;
        }
      }
      return false;
    };

    const fileBelongsToBugreport = (file: TraceFile) =>
      file.parentArchive === bugreportMainEntry.parentArchive;

    legacyFiles = legacyFiles.filter((file) => {
      return isFileAllowlisted(file) || !fileBelongsToBugreport(file);
    });

    const perfettoFile = perfettoFiles.find(
      (file) => file.file.name === TraceFileFilter.BUGREPORT_SYSTRACE_PATH
    );
    return {perfetto: perfettoFile, legacy: legacyFiles};
  }

  private isPerfettoFile(file: TraceFile): boolean {
    return file.file.name.endsWith('.pftrace') || file.file.name.endsWith('.perfetto-trace');
  }

  private pickLargestFile(
    files: TraceFile[],
    errorListener: WinscopeErrorListener
  ): TraceFile | undefined {
    if (files.length === 0) {
      return undefined;
    }
    return files.reduce((largestSoFar, file) => {
      const [largest, overridden] =
        largestSoFar.file.size > file.file.size ? [largestSoFar, file] : [file, largestSoFar];
      errorListener.onError(
        new WinscopeError(WinscopeErrorType.FILE_OVERRIDDEN, overridden.getDescriptor())
      );
      return largest;
    });
  }
}
