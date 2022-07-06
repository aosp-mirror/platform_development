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
import {Parser} from "./parser";
import {ParserAccessibility} from "./parser_accessibility";
import {ParserInputMethodClients} from "./parser_input_method_clients";
import {ParserInputMethodManagerService} from "./parser_input_method_manager_service";
import {ParserInputMethodService} from "./parser_input_method_service";
import {ParserProtoLog} from "./parser_protolog"
import {ParserSurfaceFlinger} from "./parser_surface_flinger"
import {ParserTransactions} from "./parser_transactions";
import {ParserWindowManager} from "./parser_window_manager"

class ParserFactory {
  static readonly PARSERS = [
    ParserAccessibility,
    ParserInputMethodClients,
    ParserInputMethodManagerService,
    ParserInputMethodService,
    ParserProtoLog,
    ParserSurfaceFlinger,
    ParserTransactions,
    ParserWindowManager,
  ]

  createParsers(buffers: Uint8Array[]): Parser[] {
    const parsers: Parser[] = [];

    for (const buffer of buffers) {
      for (const ParserType of ParserFactory.PARSERS) {
        try {
          const parser = new ParserType(buffer);
          parsers.push(parser);
          break;
        } catch(error) {
        }
      }
    }

    return parsers;
  }
}

export {ParserFactory};
