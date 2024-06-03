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
import {FileUtils} from 'common/file_utils';
import {TimezoneInfo} from 'common/time';
import {UserNotificationsListener} from 'messaging/user_notifications_listener';
import {TraceOverridden} from 'messaging/user_warnings';
import {TraceFile} from 'trace/trace_file';

export interface FilterResult {
  legacy: TraceFile[];
  perfetto?: TraceFile;
  timezoneInfo?: TimezoneInfo;
}

export class TraceFileFilter {
  private static readonly BUGREPORT_SYSTRACE_PATH =
    'FS/data/misc/perfetto-traces/bugreport/systrace.pftrace';
  private static readonly BUGREPORT_LEGACY_FILES_ALLOWLIST = [
    'FS/data/misc/wmtrace/',
    'FS/data/misc/perfetto-traces/',
    'proto/window_CRITICAL.proto',
    'proto/input_method_CRITICAL.proto',
    'proto/SurfaceFlinger_CRITICAL.proto',
  ];
  private static readonly PERFETTO_EXTENSIONS = [
    '.pftrace',
    '.perfetto-trace',
    '.perfetto',
  ];

  async filter(
    files: TraceFile[],
    UserNotificationsListener: UserNotificationsListener,
  ): Promise<FilterResult> {
    const bugreportMainEntry = files.find((file) =>
      file.file.name.endsWith('main_entry.txt'),
    );

    const perfettoFiles = files.filter((file) => this.isPerfettoFile(file));
    const legacyFiles = files.filter((file) => !this.isPerfettoFile(file));
    if (!(await this.isBugreport(bugreportMainEntry, files))) {
      const perfettoFile = this.pickLargestFile(
        perfettoFiles,
        UserNotificationsListener,
      );
      return {
        perfetto: perfettoFile,
        legacy: legacyFiles,
      };
    }

    const timezoneInfo = await this.processRawBugReport(
      assertDefined(bugreportMainEntry),
      files,
    );

    return await this.filterBugreport(
      assertDefined(bugreportMainEntry),
      perfettoFiles,
      legacyFiles,
      timezoneInfo,
    );
  }

  private async processRawBugReport(
    bugreportMainEntry: TraceFile,
    files: TraceFile[],
  ): Promise<TimezoneInfo | undefined> {
    const bugreportName = (await bugreportMainEntry.file.text()).trim();
    const rawBugReport = files.find((file) => file.file.name === bugreportName);
    if (!rawBugReport) {
      return undefined;
    }

    const traceBuffer = new Uint8Array(await rawBugReport.file.arrayBuffer());
    const fileData = new TextDecoder().decode(traceBuffer);

    const timezoneStartIndex = fileData.indexOf('[persist.sys.timezone]');
    if (timezoneStartIndex === -1) {
      return undefined;
    }
    const timezone = this.extractValueFromRawBugReport(
      fileData,
      timezoneStartIndex,
    );

    return {timezone, locale: 'en-US'};
  }

  private extractValueFromRawBugReport(
    fileData: string,
    startIndex: number,
  ): string {
    return fileData
      .slice(startIndex)
      .split(']', 2)
      .map((substr) => {
        const start = substr.lastIndexOf('[');
        return substr.slice(start + 1);
      })[1];
  }

  private async isBugreport(
    bugreportMainEntry: TraceFile | undefined,
    files: TraceFile[],
  ): Promise<boolean> {
    if (!bugreportMainEntry) {
      return false;
    }
    const bugreportName = (await bugreportMainEntry.file.text()).trim();
    return (
      files.find((file) => {
        return (
          file.parentArchive === bugreportMainEntry.parentArchive &&
          file.file.name === bugreportName
        );
      }) !== undefined
    );
  }

  private async filterBugreport(
    bugreportMainEntry: TraceFile,
    perfettoFiles: TraceFile[],
    legacyFiles: TraceFile[],
    timezoneInfo?: TimezoneInfo,
  ): Promise<FilterResult> {
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

    const unzippedLegacyFiles: TraceFile[] = [];

    for (const file of legacyFiles) {
      if (await FileUtils.isZipFile(file.file)) {
        try {
          const subFiles = await FileUtils.unzipFile(file.file);
          const subTraceFiles = subFiles.map((subFile) => {
            return new TraceFile(subFile, file.file);
          });
          unzippedLegacyFiles.push(...subTraceFiles);
        } catch {
          unzippedLegacyFiles.push(file);
        }
      } else {
        unzippedLegacyFiles.push(file);
      }
    }
    const perfettoFile = perfettoFiles.find(
      (file) => file.file.name === TraceFileFilter.BUGREPORT_SYSTRACE_PATH,
    );
    return {perfetto: perfettoFile, legacy: unzippedLegacyFiles, timezoneInfo};
  }

  private isPerfettoFile(file: TraceFile): boolean {
    return TraceFileFilter.PERFETTO_EXTENSIONS.some((perfettoExt) => {
      return (
        file.file.name.endsWith(perfettoExt) ||
        file.file.name.endsWith(`${perfettoExt}.gz`)
      );
    });
  }

  private pickLargestFile(
    files: TraceFile[],
    UserNotificationsListener: UserNotificationsListener,
  ): TraceFile | undefined {
    if (files.length === 0) {
      return undefined;
    }
    return files.reduce((largestSoFar, file) => {
      const [largest, overridden] =
        largestSoFar.file.size > file.file.size
          ? [largestSoFar, file]
          : [file, largestSoFar];
      UserNotificationsListener.onNotifications([
        new TraceOverridden(overridden.getDescriptor()),
      ]);
      return largest;
    });
  }
}
