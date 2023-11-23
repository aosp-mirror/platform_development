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

import {FileUtils} from 'common/file_utils';
import {WinscopeError, WinscopeErrorType} from 'messaging/winscope_error';
import {WinscopeErrorListener} from 'messaging/winscope_error_listener';
import {FileAndParser} from 'parsers/file_and_parser';
import {FileAndParsers} from 'parsers/file_and_parsers';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';
import {TraceType} from 'trace/trace_type';
import {TRACE_INFO} from './trace_info';

export class LoadedParsers {
  private legacyParsers = new Map<TraceType, FileAndParser>();
  private perfettoParsers = new Map<TraceType, FileAndParser>();

  addParsers(
    legacyParsers: FileAndParser[],
    perfettoParsers: FileAndParsers | undefined,
    errorListener: WinscopeErrorListener
  ) {
    this.addLegacyParsers(legacyParsers, errorListener);

    if (perfettoParsers) {
      this.addPerfettoParsers(perfettoParsers);
    }
  }

  getParsers(): Array<Parser<object>> {
    const fileAndParsers = [...this.legacyParsers.values(), ...this.perfettoParsers.values()];
    return fileAndParsers.map((fileAndParser) => fileAndParser.parser);
  }

  remove(type: TraceType) {
    this.legacyParsers.delete(type);
    this.perfettoParsers.delete(type);
  }

  clear() {
    this.legacyParsers.clear();
    this.perfettoParsers.clear();
  }

  async makeZipArchive(): Promise<Blob> {
    const archiveFiles: File[] = [];

    if (this.perfettoParsers.size > 0) {
      const file: TraceFile = this.perfettoParsers.values().next().value.file;
      const filenameInArchive = FileUtils.removeDirFromFileName(file.file.name);
      const archiveFile = new File([file.file], filenameInArchive);
      archiveFiles.push(archiveFile);
    }

    this.legacyParsers.forEach(({file, parser}, traceType) => {
      const archiveDir =
        TRACE_INFO[traceType].downloadArchiveDir.length > 0
          ? TRACE_INFO[traceType].downloadArchiveDir + '/'
          : '';
      const filenameInArchive = archiveDir + FileUtils.removeDirFromFileName(file.file.name);
      const archiveFile = new File([file.file], filenameInArchive);
      archiveFiles.push(archiveFile);
    });

    // Remove duplicates because some traces (e.g. view capture) could share the same file
    const uniqueArchiveFiles = archiveFiles.filter(
      (file, index, fileList) => fileList.indexOf(file) === index
    );

    return await FileUtils.createZipArchive(uniqueArchiveFiles);
  }

  private addLegacyParsers(parsers: FileAndParser[], errorListener: WinscopeErrorListener) {
    const legacyParsersBeingLoaded = new Map<TraceType, Parser<object>>();

    parsers.forEach((fileAndParser) => {
      const {parser} = fileAndParser;
      if (this.shouldUseLegacyParser(parser, legacyParsersBeingLoaded, errorListener)) {
        legacyParsersBeingLoaded.set(parser.getTraceType(), parser);
        this.legacyParsers.set(parser.getTraceType(), fileAndParser);
        this.perfettoParsers.delete(parser.getTraceType());
      }
    });
  }

  private addPerfettoParsers({file, parsers}: FileAndParsers) {
    // We currently run only one Perfetto TP WebWorker at a time, so Perfetto parsers previously loaded
    // are now invalid and must be removed (previous WebWorker is not running anymore).
    this.perfettoParsers.clear();

    parsers.forEach((parser) => {
      // While transitioning to the Perfetto format, devices might still have old legacy trace files dangling in the
      // disk that get automatically included into bugreports. Hence, Perfetto parsers must always override legacy ones
      // so that dangling legacy files are ignored.
      this.perfettoParsers.set(parser.getTraceType(), new FileAndParser(file, parser));
      this.legacyParsers.delete(parser.getTraceType());
    });
  }

  private shouldUseLegacyParser(
    newParser: Parser<object>,
    parsersBeingLoaded: Map<TraceType, Parser<object>>,
    errorListener: WinscopeErrorListener
  ): boolean {
    const oldParser = this.legacyParsers.get(newParser.getTraceType())?.parser;
    const currParser = parsersBeingLoaded.get(newParser.getTraceType());
    if (!oldParser && !currParser) {
      return true;
    }

    if (oldParser && !currParser) {
      errorListener.onError(
        new WinscopeError(
          WinscopeErrorType.FILE_OVERRIDDEN,
          oldParser.getDescriptors().join(),
          oldParser.getTraceType()
        )
      );
      return true;
    }

    if (currParser && newParser.getLengthEntries() > currParser.getLengthEntries()) {
      errorListener.onError(
        new WinscopeError(
          WinscopeErrorType.FILE_OVERRIDDEN,
          currParser.getDescriptors().join(),
          currParser.getTraceType()
        )
      );
      return true;
    }

    errorListener.onError(
      new WinscopeError(
        WinscopeErrorType.FILE_OVERRIDDEN,
        newParser.getDescriptors().join(),
        newParser.getTraceType()
      )
    );
    return false;
  }
}
