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

import {TRACE_TYPES, DUMP_TYPES} from '@/decode.js';

const mixin = {
  showInTraceView(file) {
    return file.type == TRACE_TYPES.WINDOW_MANAGER ||
      file.type == TRACE_TYPES.ACCESSIBILITY ||
      file.type == TRACE_TYPES.SURFACE_FLINGER ||
      file.type == TRACE_TYPES.TRANSACTION ||
      file.type == TRACE_TYPES.WAYLAND ||
      file.type == TRACE_TYPES.SYSTEM_UI ||
      file.type == TRACE_TYPES.LAUNCHER ||
      file.type == TRACE_TYPES.IME_CLIENTS ||
      file.type == TRACE_TYPES.IME_SERVICE ||
      file.type == TRACE_TYPES.IME_MANAGERSERVICE ||
      file.type == DUMP_TYPES.WINDOW_MANAGER ||
      file.type == DUMP_TYPES.SURFACE_FLINGER ||
      file.type == DUMP_TYPES.WAYLAND;
  },
  showInAccessibilityTraceView(file) {
    return file.type == TRACE_TYPES.ACCESSIBILITY;
  },
  showInWindowManagerTraceView(file) {
    return file.type == TRACE_TYPES.WINDOW_MANAGER ||
        file.type == DUMP_TYPES.WINDOW_MANAGER;
  },
  showInSurfaceFlingerTraceView(file) {
    return file.type == TRACE_TYPES.SURFACE_FLINGER ||
        file.type == DUMP_TYPES.SURFACE_FLINGER;
  },
  isVideo(file) {
    return file.type == TRACE_TYPES.SCREEN_RECORDING;
  },
  isTransactions(file) {
    return file.type == TRACE_TYPES.TRANSACTION;
  },
  isTransactionsLegacy(file) {
    return file.type == TRACE_TYPES.TRANSACTION_LEGACY;
  },
  isLog(file) {
    return file.type == TRACE_TYPES.PROTO_LOG;
  },
  hasDataView(file) {
    return this.isLog(file) || this.showInTraceView(file) ||
      this.isTransactionsLegacy(file);
  },
};

export {mixin};

export default {
  name: 'FileType',
  methods: mixin,
};
