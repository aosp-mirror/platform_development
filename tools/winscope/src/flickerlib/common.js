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

// Imports all the compiled common Flicker library classes and exports them
// as clean es6 modules rather than having them be commonjs modules

// Event Log
const EventLog = require('flickerlib/flicker').android.tools.common.traces.events.EventLog;
const CujEvent = require('flickerlib/flicker').android.tools.common.traces.events.CujEvent;
const Event = require('flickerlib/flicker').android.tools.common.traces.events.Event;
const EventLogParser =
  require('flickerlib/flicker').android.tools.common.parsers.events.EventLogParser;
const Cuj = require('flickerlib/flicker').android.tools.common.parsers.events.Cuj;

// Transitions
const Transition = require('flickerlib/flicker').android.tools.common.traces.wm.Transition;
const TransitionType = require('flickerlib/flicker').android.tools.common.traces.wm.TransitionType;
const TransitionChange =
  require('flickerlib/flicker').android.tools.common.traces.wm.TransitionChange;
const TransitionsTrace =
  require('flickerlib/flicker').android.tools.common.traces.wm.TransitionsTrace;
const ShellTransitionData =
  require('flickerlib/flicker').android.tools.common.traces.wm.ShellTransitionData;
const WmTransitionData =
  require('flickerlib/flicker').android.tools.common.traces.wm.WmTransitionData;

// Common
const CrossPlatform = require('flickerlib/flicker').android.tools.common.CrossPlatform;
const Timestamp = require('flickerlib/flicker').android.tools.common.Timestamp;

const NoCache = require('flickerlib/flicker').android.tools.common.NoCache;

export {
  EventLog,
  CujEvent,
  Event,
  EventLogParser,
  Cuj,
  // Transitions
  Transition,
  TransitionType,
  TransitionChange,
  TransitionsTrace,
  ShellTransitionData,
  WmTransitionData,
  // Common
  CrossPlatform,
  Timestamp,
  NoCache,
};
