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
import {FunctionUtils} from 'common/function_utils';
import {utf8Decode} from 'common/string_utils';
import {TimezoneInfo} from 'common/time/time';
import {UserNotifier} from 'common/user_notifier';
import {UserWarning} from 'messaging/user_warning';
import {MissingPersistentTrace, TraceOverridden} from 'messaging/user_warnings';
import {
  BugreportFileSelectionRequest,
  WinscopeEvent,
  WinscopeEventType,
} from 'messaging/winscope_event';
import {
  EmitEvent,
  WinscopeEventEmitter,
} from 'messaging/winscope_event_emitter';
import {WinscopeEventListener} from 'messaging/winscope_event_listener';
import {TraceFile} from 'trace/trace_file';
import {TraceMetadata} from 'trace/trace_metadata';

export enum BuildType {
  USER = 'user',
  USERDEBUG = 'userdebug',
  ENG = 'eng',
}

export interface BugreportData {
  timezoneInfo?: TimezoneInfo;
  buildType?: BuildType;
  isPersistentTracingEnabled: boolean;
}

export interface FilterResult {
  legacy: TraceFile[];
  metadata: TraceMetadata;
  perfetto?: TraceFile;
  timezoneInfo?: TimezoneInfo;
  criticalWarnings?: UserWarning[];
}

export class TraceFileFilter
  implements WinscopeEventListener, WinscopeEventEmitter
{
  private static readonly BUGREPORT_PERFETTO_TRACE_DIR =
    'FS/data/misc/perfetto-traces/bugreport';
  private static readonly BUGREPORT_PERFETTO_TRACE_ORDER = [
    TraceFileFilter.BUGREPORT_PERFETTO_TRACE_DIR + '/systrace.pftrace',
    TraceFileFilter.BUGREPORT_PERFETTO_TRACE_DIR + '/sysui.pftrace',
  ];
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

  private emitEvent: EmitEvent = FunctionUtils.DO_NOTHING_ASYNC;
  private selectedFile: string | undefined;

  setEmitEvent(callback: EmitEvent) {
    this.emitEvent = callback;
  }

  async onWinscopeEvent(event: WinscopeEvent) {
    await event.visit(
      WinscopeEventType.BUGREPORT_FILE_SELECTED,
      async (event) => {
        this.selectedFile = event.filename;
      },
    );
  }

  async filter(files: TraceFile[]): Promise<FilterResult> {
    const bugreportMainEntry = files.find((file) =>
      file.file.name.endsWith('main_entry.txt'),
    );

    const perfettoFiles = files.filter((file) => this.isPerfettoFile(file));
    const {mFiles, metadata} = await this.extractAndAnalyzeMetadata(files);
    const legacyFiles = files.filter(
      (file) => !this.isPerfettoFile(file) && !mFiles.includes(file),
    );

    const isBugReportArchive = await this.isBugreport(
      bugreportMainEntry,
      files,
    );

    if (!isBugReportArchive) {
      const perfettoFile = this.pickLargestFile(perfettoFiles);
      return {
        perfetto: perfettoFile,
        legacy: legacyFiles,
        metadata,
      };
    }

    const bugreportData = await this.getBugreportData(
      assertDefined(bugreportMainEntry),
      files,
    );

    return await this.filterBugreport(
      assertDefined(bugreportMainEntry),
      perfettoFiles,
      legacyFiles,
      metadata,
      bugreportData,
    );
  }

  private async getBugreportData(
    bugreportMainEntry: TraceFile,
    files: TraceFile[],
  ): Promise<BugreportData | undefined> {
    const bugreportName = (await bugreportMainEntry.file.text()).trim();
    const mainBugreportFile = files.find(
      (file) => file.file.name === bugreportName,
    );
    if (!mainBugreportFile) {
      return undefined;
    }

    const traceBuffer = new Uint8Array(
      await mainBugreportFile.file.arrayBuffer(),
    );
    const fileData = utf8Decode(traceBuffer);

    const timezone = this.extractBugreportProperty(
      fileData,
      'persist.sys.timezone',
    );
    const timezoneInfo = timezone ? {timezone, locale: 'en-US'} : undefined;
    const buildTypeString = this.extractBugreportProperty(
      fileData,
      'ro.build.type',
    );
    const persistentTracingFlag = this.extractBugreportProperty(
      fileData,
      'persist.debug.perfetto.persistent_sysui_tracing_for_bugreport',
    );
    const isPersistentTracingEnabled = persistentTracingFlag === '1';

    return {
      timezoneInfo,
      buildType: this.parseBuildType(buildTypeString),
      isPersistentTracingEnabled,
    };
  }

  private parseBuildType(
    buildTypeString: string | undefined,
  ): BuildType | undefined {
    if (!buildTypeString) {
      return undefined;
    }
    const lowerCaseBuildType = buildTypeString.toLowerCase();
    if (Object.values(BuildType).includes(lowerCaseBuildType as BuildType)) {
      return lowerCaseBuildType as BuildType;
    }
    console.warn(`Unknown build type found in bugreport: ${buildTypeString}`);
    return undefined;
  }

  private extractBugreportProperty(
    fileData: string,
    propertyKey: string,
  ): string | undefined {
    const keyWithBrackets = `[${propertyKey}]`;
    const startIndex = fileData.indexOf(keyWithBrackets);
    if (startIndex === -1) {
      return undefined;
    }
    return this.extractValueFromRawBugReport(fileData, startIndex);
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
    return files.some((file) => {
      return (
        file.parentArchive === bugreportMainEntry.parentArchive &&
        file.file.name === bugreportName
      );
    });
  }

  private async filterBugreport(
    bugreportMainEntry: TraceFile,
    perfettoFiles: TraceFile[],
    legacyFiles: TraceFile[],
    metadata: TraceMetadata,
    bugreportData?: BugreportData,
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
    const brPerfettoFiles = perfettoFiles.filter(
      (file) =>
        FileUtils.getFileDirectory(file.file.name) ===
        TraceFileFilter.BUGREPORT_PERFETTO_TRACE_DIR,
    );

    const getIndex = (fileName: string) => {
      return TraceFileFilter.BUGREPORT_PERFETTO_TRACE_ORDER.findIndex(
        (name) => name === fileName,
      );
    };
    let perfettoFile = brPerfettoFiles
      .filter((file) =>
        TraceFileFilter.BUGREPORT_PERFETTO_TRACE_ORDER.includes(file.file.name),
      )
      .sort((f1, f2) => getIndex(f1.file.name) - getIndex(f2.file.name))
      .at(0);

    if (!perfettoFile && brPerfettoFiles.length === 1) {
      perfettoFile = brPerfettoFiles[0];
    }

    if (!perfettoFile && brPerfettoFiles.length > 1) {
      // emitEvent must be set to propagate event to mediator, which routes file selection
      // request to AppComponent. User is prompted by dialog to select which file to
      // process. Once dialog is closed, selected file is sent back to TraceFileFilter
      // via BugreportFileSelected event and handled above in onWinscopeEvent, where
      // it is stored in selectedFile. Promise below only resolves after BugreportFileSelected
      // event has been handled.
      await this.emitEvent(
        new BugreportFileSelectionRequest(
          brPerfettoFiles.map((file) => file.file.name),
        ),
      );
      if (this.selectedFile) {
        perfettoFile = brPerfettoFiles.find(
          (file) => file.file.name === this.selectedFile,
        );
        this.selectedFile = undefined;
      }
    }

    const criticalWarnings: UserWarning[] = [];
    if (!perfettoFile && bugreportData) {
      criticalWarnings.push(new MissingPersistentTrace(bugreportData));
    }

    return {
      perfetto: perfettoFile,
      legacy: unzippedLegacyFiles,
      metadata,
      timezoneInfo: bugreportData?.timezoneInfo,
      criticalWarnings,
    };
  }

  private isPerfettoFile(file: TraceFile): boolean {
    return TraceFileFilter.PERFETTO_EXTENSIONS.some((perfettoExt) => {
      return (
        file.file.name.endsWith(perfettoExt) ||
        file.file.name.endsWith(`${perfettoExt}.gz`)
      );
    });
  }

  private async extractAndAnalyzeMetadata(
    files: TraceFile[],
  ): Promise<{mFiles: TraceFile[]; metadata: TraceMetadata}> {
    const mFiles = [];
    const metadata: TraceMetadata = {};
    for (const file of files) {
      const buffer = new Uint8Array(await file.file.arrayBuffer());
      const text = utf8Decode(buffer);
      try {
        const data = JSON.parse(text);
        if (
          data.realToElapsedTimeOffsetNanos !== undefined &&
          data.elapsedRealTimeNanos !== undefined
        ) {
          metadata.screenRecordingOffsets = {
            realToElapsedTimeOffsetNanos: BigInt(
              data.realToElapsedTimeOffsetNanos,
            ),
            elapsedRealTimeNanos: BigInt(data.elapsedRealTimeNanos),
          };
          mFiles.push(file);
          break;
        }
      } catch (e) {
        // swallow - looking for metadata json
      }
    }
    return {metadata, mFiles};
  }

  private pickLargestFile(files: TraceFile[]): TraceFile | undefined {
    if (files.length === 0) {
      return undefined;
    }
    return files.reduce((largestSoFar, file) => {
      const [largest, overridden] =
        largestSoFar.file.size > file.file.size
          ? [largestSoFar, file]
          : [file, largestSoFar];
      UserNotifier.add(new TraceOverridden(overridden.getDescriptor()));
      return largest;
    });
  }
}
