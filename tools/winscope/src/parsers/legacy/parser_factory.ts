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
import {ParserTimestampConverter} from 'common/timestamp_converter';
import {ProgressListener} from 'messaging/progress_listener';
import {UserNotificationsListener} from 'messaging/user_notifications_listener';
import {UnsupportedFileFormat} from 'messaging/user_warnings';
import {ParserEventLog} from 'parsers/events/parser_eventlog';
import {FileAndParser} from 'parsers/file_and_parser';
import {ParserInputMethodClients} from 'parsers/input_method/legacy/parser_input_method_clients';
import {ParserInputMethodManagerService} from 'parsers/input_method/legacy/parser_input_method_manager_service';
import {ParserInputMethodService} from 'parsers/input_method/legacy/parser_input_method_service';
import {ParserProtoLog} from 'parsers/protolog/legacy/parser_protolog';
import {ParserScreenshot} from 'parsers/screen_recording/parser_screenshot';
import {ParserScreenRecording} from 'parsers/screen_recording/parser_screen_recording';
import {ParserScreenRecordingLegacy} from 'parsers/screen_recording/parser_screen_recording_legacy';
import {ParserSurfaceFlinger} from 'parsers/surface_flinger/legacy/parser_surface_flinger';
import {ParserTransactions} from 'parsers/transactions/legacy/parser_transactions';
import {ParserTransitionsShell} from 'parsers/transitions/legacy/parser_transitions_shell';
import {ParserTransitionsWm} from 'parsers/transitions/legacy/parser_transitions_wm';
import {ParserViewCapture} from 'parsers/view_capture/legacy/parser_view_capture';
import {ParserWindowManager} from 'parsers/window_manager/parser_window_manager';
import {ParserWindowManagerDump} from 'parsers/window_manager/parser_window_manager_dump';
import {Parser} from 'trace/parser';
import {TraceFile} from 'trace/trace_file';

export class ParserFactory {
  static readonly PARSERS = [
    ParserInputMethodClients,
    ParserInputMethodManagerService,
    ParserInputMethodService,
    ParserProtoLog,
    ParserScreenRecording,
    ParserScreenRecordingLegacy,
    ParserSurfaceFlinger,
    ParserTransactions,
    ParserWindowManager,
    ParserWindowManagerDump,
    ParserEventLog,
    ParserTransitionsWm,
    ParserTransitionsShell,
    ParserViewCapture,
    ParserScreenshot,
  ];

  async createParsers(
    traceFiles: TraceFile[],
    timestampConverter: ParserTimestampConverter,
    progressListener?: ProgressListener,
    notificationListener?: UserNotificationsListener,
  ): Promise<FileAndParser[]> {
    const parsers = new Array<{file: TraceFile; parser: Parser<object>}>();

    for (const [index, traceFile] of traceFiles.entries()) {
      progressListener?.onProgressUpdate(
        'Parsing proto files',
        (index / traceFiles.length) * 100,
      );

      let hasFoundParser = false;

      for (const ParserType of ParserFactory.PARSERS) {
        try {
          const p = new ParserType(traceFile, timestampConverter);
          await p.parse();
          hasFoundParser = true;

          if (p instanceof ParserViewCapture) {
            p.getWindowParsers().forEach((subParser) =>
              parsers.push(new FileAndParser(traceFile, subParser)),
            );
          } else {
            parsers.push({file: traceFile, parser: p});
          }
          break;
        } catch (error) {
          // skip current parser
        }
      }

      if (!hasFoundParser) {
        notificationListener?.onNotifications([
          new UnsupportedFileFormat(traceFile.getDescriptor()),
        ]);
      }
    }
    return parsers;
  }
}
