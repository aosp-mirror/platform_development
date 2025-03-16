/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {AdbFileIdentifier, TraceTarget} from 'trace_collection/trace_target';
import {UiTraceTarget} from 'trace_collection/ui/ui_trace_target';
import {UserRequest, UserRequestConfig} from 'trace_collection/user_request';
import {PerfettoSessionModerator} from './perfetto_session_moderator';
import {TracingSession} from './tracing_session';

const WINSCOPE_EXT = '.winscope';
const WINSCOPE_EXT_LEGACY = '.pb';
const WINSCOPE_EXTS = [WINSCOPE_EXT, WINSCOPE_EXT_LEGACY];
const WINSCOPE_DIR = '/data/misc/wmtrace/';

function makeMatchersWithWinscopeExts(matcher: string) {
  return WINSCOPE_EXTS.map((ext) => `${matcher}${ext}`);
}

export class UserRequestParser {
  private readonly targetPerfettoDsMap = new Map([
    [UiTraceTarget.SURFACE_FLINGER_TRACE, 'android.surfaceflinger.layers'],
    [UiTraceTarget.WINDOW_MANAGER_TRACE, 'android.windowmanager'],
    [UiTraceTarget.IME, 'android.inputmethod'],
    [UiTraceTarget.TRANSACTIONS, 'android.surfaceflinger.transactions'],
    [UiTraceTarget.PROTO_LOG, 'android.protolog'],
    [UiTraceTarget.TRANSITIONS, 'com.android.wm.shell.transition'],
    [UiTraceTarget.VIEW_CAPTURE, 'android.viewcapture'],
    [UiTraceTarget.INPUT, 'android.input.inputevent'],
    [UiTraceTarget.SURFACE_FLINGER_DUMP, 'android.surfaceflinger.layers'],
  ]);

  private perfettoModerator: PerfettoSessionModerator | undefined;
  private requests: UserRequest[] | undefined;

  setPerfettoModerator(value: PerfettoSessionModerator) {
    this.perfettoModerator = value;
    return this;
  }

  setRequests(value: UserRequest[]) {
    this.requests = value;
    return this;
  }

  async parse(): Promise<TracingSession[]> {
    const traceTargets: TraceTarget[] = [];
    const perfettoSetup: string[] = [];
    const perfettoModerator = assertDefined(this.perfettoModerator);

    for (const req of assertDefined(this.requests)) {
      const ds = this.targetPerfettoDsMap.get(req.target);
      const dataSourceAvailable =
        ds !== undefined && (await perfettoModerator.isDataSourceAvailable(ds));

      const isPerfetto =
        !(await perfettoModerator.isTooManySessions()) && dataSourceAvailable;

      if (isPerfetto) {
        const cmd = this.getPerfettoSetupCommand(req);
        if (cmd) {
          perfettoSetup.push(cmd);
        }
      } else {
        const targets = this.getNonPerfettoTargets(req);
        if (targets) {
          traceTargets.push(...targets);
        }
      }
    }

    const sessions = traceTargets.map((target) => {
      return new TracingSession(target);
    });
    if (perfettoSetup.length > 0) {
      sessions.push(perfettoModerator.createTracingSession(perfettoSetup));
    }
    return sessions;
  }

  private getPerfettoSetupCommand(req: UserRequest): string | undefined {
    switch (req.target) {
      case UiTraceTarget.SURFACE_FLINGER_TRACE:
        return this.getSfTracePerfettoSetupCommand(req);
      case UiTraceTarget.WINDOW_MANAGER_TRACE:
        return this.getWmTracePerfettoSetupCommand(req);
      case UiTraceTarget.VIEW_CAPTURE:
        return this.getVcPerfettoSetupCommand();
      case UiTraceTarget.TRANSACTIONS:
        return this.getTransactionsPerfettoSetupCommand();
      case UiTraceTarget.PROTO_LOG:
        return this.getProtologPerfettoSetupCommand();
      case UiTraceTarget.IME:
        return this.getImePerfettoSetupCommand();
      case UiTraceTarget.TRANSITIONS:
        return this.getTransitionsPerfettoSetupCommand();
      case UiTraceTarget.INPUT:
        return this.getInputPerfettoSetupCommand();
      case UiTraceTarget.SURFACE_FLINGER_DUMP:
        return this.getSfDumpPerfettoSetupCommand();
      default:
        return undefined;
    }
  }

  private getNonPerfettoTargets(req: UserRequest) {
    switch (req.target) {
      case UiTraceTarget.SURFACE_FLINGER_TRACE:
        return [this.getSfTraceLegacyTarget(req)];
      case UiTraceTarget.WINDOW_MANAGER_TRACE:
        return [this.getWmTraceLegacyTarget(req)];
      case UiTraceTarget.VIEW_CAPTURE:
        return [this.getVcLegacyTarget()];
      case UiTraceTarget.TRANSACTIONS:
        return [this.getTransactionsLegacyTarget()];
      case UiTraceTarget.PROTO_LOG:
        return [this.getProtologLegacyTarget()];
      case UiTraceTarget.IME:
        return [this.getImeLegacyTarget()];
      case UiTraceTarget.TRANSITIONS:
        return [this.getTransitionsLegacyTarget()];
      case UiTraceTarget.SCREEN_RECORDING:
        return this.getScreenRecordingTargets(req);
      case UiTraceTarget.WAYLAND:
        return [this.getWaylandTarget()];
      case UiTraceTarget.EVENTLOG:
        return [this.getEventlogTarget()];
      case UiTraceTarget.SURFACE_FLINGER_DUMP:
        return [this.getSfDumpLegacyTarget()];
      case UiTraceTarget.WINDOW_MANAGER_DUMP:
        return [this.getWmDumpLegacyTarget()];
      case UiTraceTarget.SCREENSHOT:
        return this.getScreenshotTargets(req);
      default:
        return undefined;
    }
  }

  private getSfTracePerfettoSetupCommand(req: UserRequest) {
    const flagsMap: {[key: string]: string} = {
      'input': 'TRACE_FLAG_INPUT',
      'composition': 'TRACE_FLAG_COMPOSITION',
      'metadata': 'TRACE_FLAG_EXTRA',
      'hwc': 'TRACE_FLAG_HWC',
      'tracebuffers': 'TRACE_FLAG_BUFFERS',
      'virtualdisplays': 'TRACE_FLAG_VIRTUAL_DISPLAYS',
    };
    const {flags} = new SfRequestConfigParser(flagsMap).parse(req.config);

    const spacer = '\n      ';
    const flagsCmd = flags
      .map((flag: string) => {
        return `trace_flags: ${flagsMap[flag]}`;
      })
      .join(spacer);
    return this.perfettoModerator?.createSetupCommand(
      'android.surfaceflinger.layers',
      `surfaceflinger_layers_config: {
      mode: MODE_ACTIVE${flagsCmd.length === 0 ? '' : spacer + flagsCmd}
    }`,
    );
  }

  private getSfTraceLegacyTarget(req: UserRequest) {
    const flagsMap: {[key: string]: number} = {
      'input': 1 << 1,
      'composition': 1 << 2,
      'metadata': 1 << 3,
      'hwc': 1 << 4,
      'tracebuffers': 1 << 5,
      'virtualdisplays': 1 << 6,
    };
    const {flags, selectedConfigs} = new SfRequestConfigParser(flagsMap).parse(
      req.config,
    );
    let flagsValue = 0;
    for (const flag of flags) {
      flagsValue |= flagsMap[flag];
    }
    const setupCommands = [
      `su root service call SurfaceFlinger 1029 i32 ${selectedConfigs['sfbuffersize']}$`,
      `su root service call SurfaceFlinger 1033 i32 ${flagsValue}`,
    ];

    return new TraceTarget(
      'SfLegacyTrace',
      setupCommands,
      'su root service call SurfaceFlinger 1025 i32 1' +
        '\necho "SF layers trace (legacy) started."',
      'su root service call SurfaceFlinger 1025 i32 0 >/dev/null 2>&1' +
        '\necho "SF layers trace (legacy) stopped."',
      [
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('layers_trace'),
          'layers_trace',
        ),
      ],
    );
  }

  private getWmTracePerfettoSetupCommand(req: UserRequest) {
    const selectedConfigs = new WmRequestConfigParser().parse(req.config);

    const logLevelMap: {[key: string]: string} = {
      'verbose': 'LOG_LEVEL_VERBOSE',
      'debug': 'LOG_LEVEL_DEBUG',
      'critical': 'LOG_LEVEL_CRITICAL',
    };

    const frequencyMap: {[key: string]: string} = {
      'frame': 'LOG_FREQUENCY_FRAME',
      'transaction': 'LOG_FREQUENCY_TRANSACTION',
    };

    const logLevel = logLevelMap[selectedConfigs['tracinglevel']];
    const logFrequency = frequencyMap[selectedConfigs['tracingtype']];
    return this.perfettoModerator?.createSetupCommand(
      'android.windowmanager',
      `windowmanager_config: {
      log_level: ${logLevel}
      log_frequency: ${logFrequency}
    }`,
    );
  }

  private getWmTraceLegacyTarget(req: UserRequest) {
    const selectedConfigs = new WmRequestConfigParser().parse(req.config);

    const setupCmds = [
      `su root cmd window tracing ${selectedConfigs['tracingtype']}`,
      `su root cmd window tracing level ${selectedConfigs['tracinglevel']}`,
      `su root cmd window tracing size ${selectedConfigs['wmbuffersize']}`,
    ];

    return new TraceTarget(
      'WmLegacyTrace',
      setupCmds,
      'su root cmd window tracing start' +
        '\necho "WM trace (legacy) started."',
      'su root cmd window tracing stop' + '\necho "WM trace (legacy) stopped."',
      [
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('wm_trace'),
          'window_trace',
        ),
      ],
    );
  }

  private getVcPerfettoSetupCommand() {
    return this.perfettoModerator?.createSetupCommand('android.viewcapture');
  }

  private getVcLegacyTarget() {
    return new TraceTarget(
      'VcLegacy',
      [],
      'su root settings put global view_capture_enabled 1' +
        '\necho "ViewCapture tracing (legacy) started."',
      'su root sh -c "cmd launcherapps dump-view-hierarchies >/data/misc/wmtrace/view_capture_trace.zip"' +
        '\nsu root settings put global view_capture_enabled 0' +
        '\necho "ViewCapture tracing (legacy) stopped."',
      [
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          ['view_capture_trace.zip'],
          'view_capture_trace.zip',
        ),
      ],
    );
  }

  private getTransactionsPerfettoSetupCommand() {
    return this.perfettoModerator?.createSetupCommand(
      'android.surfaceflinger.transactions',
      `surfaceflinger_transactions_config: {
      mode: MODE_ACTIVE
    }`,
    );
  }

  private getTransactionsLegacyTarget() {
    return new TraceTarget(
      'TransactionsLegacy',
      [],
      'su root service call SurfaceFlinger 1041 i32 1' +
        '\necho "SF transactions trace (legacy) started."',
      'su root service call SurfaceFlinger 1041 i32 0 >/dev/null 2>&1' +
        '\necho "SF transactions trace (legacy) stopped."',
      [
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('transactions_trace'),
          'transactions',
        ),
      ],
    );
  }

  private getProtologPerfettoSetupCommand() {
    return this.perfettoModerator?.createSetupCommand(
      'android.protolog',
      `protolog_config: {
      tracing_mode: ENABLE_ALL
    }`,
    );
  }

  private getProtologLegacyTarget() {
    return new TraceTarget(
      'ProtologLegacy',
      [],
      'su root cmd window logging start' +
        '\necho "ProtoLog (legacy) started."',
      'su root cmd window logging stop >/dev/null 2>&1' +
        '\necho "ProtoLog (legacy) stopped."',
      [
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('wm_log'),
          'proto_log',
        ),
      ],
    );
  }

  private getImePerfettoSetupCommand() {
    return this.perfettoModerator?.createSetupCommand('android.inputmethod');
  }

  private getImeLegacyTarget() {
    return new TraceTarget(
      'ImeLegacy',
      [],
      'su root ime tracing start\necho "IME tracing (legacy) started."',
      'su root ime tracing stop >/dev/null 2>&1\necho "IME tracing (legacy) stopped."',
      [
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('ime_trace_clients'),
          'ime_trace_clients',
        ),
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('ime_trace_service'),
          'ime_trace_service',
        ),
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('ime_trace_managerservice'),
          'ime_trace_managerservice',
        ),
      ],
    );
  }

  private getTransitionsPerfettoSetupCommand() {
    return this.perfettoModerator?.createSetupCommand(
      'com.android.wm.shell.transition',
    );
  }

  private getTransitionsLegacyTarget() {
    return new TraceTarget(
      'TransitionsLegacy',
      [],
      'su root cmd window shell tracing start ' +
        '&& su root dumpsys activity service SystemUIService WMShell transitions tracing start' +
        '\necho "Transition traces (legacy) started."',
      'su root cmd window shell tracing stop ' +
        '&& su root dumpsys activity service SystemUIService WMShell transitions tracing stop >/dev/null 2>&1' +
        '\n echo "Transition traces (legacy) stopped."',
      [
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('wm_transition_trace'),
          'wm_transition_trace',
        ),
        new AdbFileIdentifier(
          WINSCOPE_DIR,
          makeMatchersWithWinscopeExts('shell_transition_trace'),
          'shell_transition_trace',
        ),
      ],
    );
  }

  private getInputPerfettoSetupCommand() {
    return this.perfettoModerator?.createSetupCommand(
      'android.input.inputevent',
      `android_input_event_config {
      mode: TRACE_MODE_TRACE_ALL
    }`,
    );
  }

  private getScreenRecordingTargets(req: UserRequest) {
    const {identifiers, showPointerAndTouches} =
      new ScreenRecordingConfigParser().parse(req.config);

    const val = showPointerAndTouches ? '1' : '0';
    const setupCmd = `settings put system show_touches ${val} && settings put system pointer_location ${val}`;
    const stopCmd = `settings put system pointer_location 0 && \
      settings put system show_touches 0 && \
      pkill -l SIGINT screenrecord >/dev/null 2>&1`;

    return identifiers.map((id, index) => {
      const startArgs = id === 'active' ? '' : ` --display-id ${id}`;
      const startCmd = `
      screenrecord --bugreport --bit-rate 8M${startArgs} /data/local/tmp/screen_${id}.mp4 & \
      echo "ScreenRecorder started."
      `;

      return new TraceTarget(
        'ScreenRecording' + id,
        index === 0 ? [setupCmd] : [],
        startCmd,
        stopCmd,
        [
          new AdbFileIdentifier(
            `/data/local/tmp/screen_${id}.mp4`,
            [],
            `screen_recording_${id}`,
          ),
        ],
        true,
      );
    });
  }

  private getScreenshotTargets(req: UserRequest) {
    const identifiers = new ScreenshotConfigParser().parse(req.config);

    return identifiers.map((id) => {
      const startArgs = id === 'active' ? '' : ` -d ${id}`;
      const startCmd = `screencap -p${startArgs} > /data/local/tmp/screenshot_${id}.png`;

      return new TraceTarget('Screenshot' + id, [], startCmd, '', [
        new AdbFileIdentifier(
          `/data/local/tmp/screenshot_${id}.png`,
          [],
          `screenshot_${id}.png`,
        ),
      ]);
    });
  }

  private getWaylandTarget() {
    return new TraceTarget(
      'Wayland',
      [],
      'su root service call Wayland 26 i32 1 >/dev/null\necho "Wayland trace started."',
      'su root service call Wayland 26 i32 0 >/dev/null\necho "Wayland trace ended."',
      [
        new AdbFileIdentifier(
          '/data/misc/wltrace',
          makeMatchersWithWinscopeExts('wl_trace'),
          'wl_trace',
        ),
      ],
    );
  }

  private getEventlogTarget() {
    const startTimeSeconds = (Date.now() / 1000).toString();
    return new TraceTarget(
      'Eventlog',
      [],
      'rm -f /data/local/tmp/eventlog.winscope' + '\n echo "EventLog started."',
      'echo "EventLog\\n" > /data/local/tmp/eventlog.winscope ' +
        `&& su root logcat -b events -v threadtime -v printable -v uid -v nsec -v epoch -b events -t ${startTimeSeconds} >> /data/local/tmp/eventlog.winscope`,
      [
        new AdbFileIdentifier(
          '/data/local/tmp',
          makeMatchersWithWinscopeExts('eventlog'),
          'eventlog',
        ),
      ],
    );
  }

  private getSfDumpPerfettoSetupCommand() {
    return this.perfettoModerator?.createSetupCommand(
      'android.surfaceflinger.layers',
      `surfaceflinger_layers_config: {
      mode: MODE_DUMP
      trace_flags: TRACE_FLAG_INPUT
      trace_flags: TRACE_FLAG_COMPOSITION
      trace_flags: TRACE_FLAG_HWC
      trace_flags: TRACE_FLAG_BUFFERS
      trace_flags: TRACE_FLAG_VIRTUAL_DISPLAYS
    }`,
    );
  }

  private getSfDumpLegacyTarget() {
    return new TraceTarget(
      'SfDumpLegacy',
      [],
      `su root dumpsys SurfaceFlinger --proto > /data/local/tmp/sf_dump${WINSCOPE_EXT}`,
      '',
      [
        new AdbFileIdentifier(
          `/data/local/tmp/sf_dump${WINSCOPE_EXT}`,
          [],
          'layers_dump',
        ),
      ],
    );
  }

  private getWmDumpLegacyTarget() {
    return new TraceTarget(
      'WmDumpLegacy',
      [],
      `su root dumpsys window --proto > /data/local/tmp/wm_dump${WINSCOPE_EXT}`,
      '',
      [
        new AdbFileIdentifier(
          `/data/local/tmp/wm_dump${WINSCOPE_EXT}`,
          [],
          'window_dump',
        ),
      ],
    );
  }
}

class SfRequestConfigParser {
  private readonly configs: {[key: string]: string} = {
    'sfbuffersize': '16000',
  };

  constructor(private readonly flagsMap: object) {}

  parse(req: UserRequestConfig[]) {
    const flags: string[] = [];
    req.forEach((config) => {
      if (config.key in this.flagsMap) {
        flags.push(config.key);
      } else if (
        config.key in this.configs &&
        typeof config.value === 'string'
      ) {
        this.configs[config.key] = config.value ?? '';
      }
    });
    return {flags, selectedConfigs: this.configs};
  }
}

class WmRequestConfigParser {
  private readonly configs: {[key: string]: string} = {
    'wmbuffersize': '16000',
    'tracinglevel': 'debug',
    'tracingtype': 'frame',
  };

  parse(req: UserRequestConfig[]) {
    req.forEach((config) => {
      if (config.key in this.configs && typeof config.value === 'string') {
        this.configs[config.key] = config.value ?? this.configs[config.key];
      }
    });
    return this.configs;
  }
}

abstract class MediaBasedConfigParser {
  protected getIdentifiers(req: UserRequestConfig[]): string[] {
    const identifiers = ['active'];
    const config = req.find((c) => c.key === 'displays');
    if (config?.value) {
      if (typeof config.value === 'string') {
        const display = this.parseDisplayId(config.value);
        if (display) return [display];
      } else {
        const displays = config.value.map((v) => this.parseDisplayId(v));
        if (displays.length > 0) return displays;
      }
    }
    return identifiers;
  }

  private parseDisplayId(displayValue: string): string {
    // display value comes in form '"<displayName>" <displayId> <otherInfo>'
    // where '"<displayName>"' is optional
    if (displayValue[0] === '"') {
      displayValue = displayValue.split('"')[2].trim();
    }
    return displayValue.split(' ')[0];
  }
}

class ScreenshotConfigParser extends MediaBasedConfigParser {
  parse(req: UserRequestConfig[]) {
    return this.getIdentifiers(req);
  }
}

class ScreenRecordingConfigParser extends MediaBasedConfigParser {
  parse(req: UserRequestConfig[]) {
    return {
      identifiers: this.getIdentifiers(req),
      showPointerAndTouches: req.find((c) => c.key === 'pointer_and_touches'),
    };
  }
}
