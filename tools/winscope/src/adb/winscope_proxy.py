#!/usr/bin/python3

# Copyright (C) 2019 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# This is an ADB proxy for Winscope.
#
# Requirements: python3.5 and ADB installed and in system PATH.
#
# Usage:
#     run: python3 winscope_proxy.py
#

import argparse
import base64
import json
import logging
import os
import re
import secrets
import signal
import subprocess
import sys
import threading
import time
from abc import abstractmethod
from enum import Enum
from http import HTTPStatus
from http.server import HTTPServer, BaseHTTPRequestHandler
from logging import DEBUG, INFO, WARNING
from tempfile import NamedTemporaryFile

# GLOBALS #

log = None
secret_token = None

# CONFIG #

def create_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description='Proxy for go/winscope', prog='winscope_proxy')

    parser.add_argument('--verbose', '-v', dest='loglevel', action='store_const', const=INFO)
    parser.add_argument('--debug', '-d', dest='loglevel', action='store_const', const=DEBUG)
    parser.add_argument('--port', '-p', default=5544, action='store')

    parser.set_defaults(loglevel=WARNING)

    return parser

# Keep in sync with ProxyClient#VERSION in Winscope
VERSION = '1.2'

PERFETTO_TRACE_CONFIG_FILE = '/data/misc/perfetto-configs/winscope-proxy-trace.conf'
PERFETTO_DUMP_CONFIG_FILE = '/data/misc/perfetto-configs/winscope-proxy-dump.conf'
PERFETTO_SF_CONFIG_FILE = '/data/misc/perfetto-configs/winscope-proxy.surfaceflinger.conf'
PERFETTO_TRACE_FILE = '/data/misc/perfetto-traces/winscope-proxy-trace.perfetto-trace'
PERFETTO_DUMP_FILE = '/data/misc/perfetto-traces/winscope-proxy-dump.perfetto-trace'
PERFETTO_UTILS = """
function is_perfetto_data_source_available {
    local data_source_name=$1
    if perfetto --query | grep $data_source_name 2>&1 >/dev/null; then
        return 0
    else
        return 1
    fi
}

function is_any_perfetto_data_source_available {
    if is_perfetto_data_source_available android.surfaceflinger.layers || \
       is_perfetto_data_source_available android.surfaceflinger.transactions || \
       is_perfetto_data_source_available com.android.wm.shell.transition || \
       is_perfetto_data_source_available android.protolog; then
        return 0
    else
        return 1
    fi
}

function is_flag_set {
    local flag_name=$1
    if dumpsys device_config | grep $flag_name=true 2>&1 >/dev/null; then
        return 0
    else
        return 1
    fi
}
"""

WINSCOPE_VERSION_HEADER = "Winscope-Proxy-Version"
WINSCOPE_TOKEN_HEADER = "Winscope-Token"

# Location to save the proxy security token
WINSCOPE_TOKEN_LOCATION = os.path.expanduser('~/.config/winscope/.token')

# Winscope traces extensions
WINSCOPE_EXT = ".winscope"
WINSCOPE_EXT_LEGACY = ".pb"
WINSCOPE_EXTS = [WINSCOPE_EXT, WINSCOPE_EXT_LEGACY]

# Winscope traces directory
WINSCOPE_DIR = "/data/misc/wmtrace/"

# Max interval between the client keep-alive requests in seconds
KEEP_ALIVE_INTERVAL_S = 5

class File:
    def __init__(self, file, filetype) -> None:
        self.file = file
        self.type = filetype

    def get_filepaths(self, device_id):
        return [self.file]

    def get_filetype(self):
        return self.type


class FileMatcher:
    def __init__(self, path, matcher, filetype) -> None:
        self.path = path
        self.matcher = matcher
        self.type = filetype

    def get_filepaths(self, device_id):
        matchingFiles = call_adb(
            f"shell su root find {self.path} -name {self.matcher}", device_id)

        log.debug("Found file %s", matchingFiles.split('\n')[:-1])
        return matchingFiles.split('\n')[:-1]

    def get_filetype(self):
        return self.type


class WinscopeFileMatcher(FileMatcher):
    def __init__(self, path, matcher, filetype) -> None:
        self.path = path
        self.internal_matchers = list(map(lambda ext: FileMatcher(path, f'{matcher}{ext}', filetype),
            WINSCOPE_EXTS))
        self.type = filetype

    def get_filepaths(self, device_id):
        for matcher in self.internal_matchers:
            files = matcher.get_filepaths(device_id)
            if len(files) > 0:
                return files
        log.debug("No files found")
        return []


class TraceTarget:
    """Defines a single parameter to trace.

    Attributes:
        file_matchers: the matchers used to identify the paths on the device the trace results are saved to.
        trace_start: command to start the trace from adb shell, must not block.
        trace_stop: command to stop the trace, should block until the trace is stopped.
    """

    def __init__(self, files, trace_start: str, trace_stop: str) -> None:
        if type(files) is not list:
            files = [files]
        self.files = files
        self.trace_start = trace_start
        self.trace_stop = trace_stop


# Order of files matters as they will be expected in that order and decoded in that order
TRACE_TARGETS = {
    "view_capture_trace": TraceTarget(
        File('/data/misc/wmtrace/view_capture_trace.zip', "view_capture_trace.zip"),
        'su root settings put global view_capture_enabled 1\necho "View capture trace started."',
        "su root sh -c 'cmd launcherapps dump-view-hierarchies >/data/misc/wmtrace/view_capture_trace.zip'; su root settings put global view_capture_enabled 0"
    ),
    "window_trace": TraceTarget(
        WinscopeFileMatcher(WINSCOPE_DIR, "wm_trace", "window_trace"),
        'su root cmd window tracing start\necho "WM trace started."',
        'su root cmd window tracing stop >/dev/null 2>&1'
    ),
    "layers_trace": TraceTarget(
        WinscopeFileMatcher(WINSCOPE_DIR, "layers_trace", "layers_trace"),
        f"""
if is_perfetto_data_source_available android.surfaceflinger.layers; then
    cat {PERFETTO_SF_CONFIG_FILE} >> {PERFETTO_TRACE_CONFIG_FILE}
    echo 'SF trace (perfetto) configured to start along the other perfetto traces'
else
    su root service call SurfaceFlinger 1025 i32 1
    echo 'SF layers trace (legacy) started'
fi
        """,
        """
if ! is_perfetto_data_source_available android.surfaceflinger.layers; then
    su root service call SurfaceFlinger 1025 i32 0 >/dev/null 2>&1
    echo 'SF layers trace (legacy) stopped.'
fi
"""
),
    "screen_recording": TraceTarget(
        File(f'/data/local/tmp/screen.mp4', "screen_recording"),
        f'''
        settings put system show_touches 1 && \
        settings put system pointer_location 1 && \
        screenrecord --bugreport --bit-rate 8M /data/local/tmp/screen.mp4 >/dev/null 2>&1 & \
        echo "ScreenRecorder started."
        ''',
        '''settings put system pointer_location 0 && \
        settings put system show_touches 0 && \
        pkill -l SIGINT screenrecord >/dev/null 2>&1
        '''.strip()
    ),
    "transactions": TraceTarget(
        WinscopeFileMatcher(WINSCOPE_DIR, "transactions_trace", "transactions"),
        f"""
if is_perfetto_data_source_available android.surfaceflinger.transactions; then
    cat << EOF >> {PERFETTO_TRACE_CONFIG_FILE}
data_sources: {{
    config {{
        name: "android.surfaceflinger.transactions"
        surfaceflinger_transactions_config: {{
            mode: MODE_ACTIVE
        }}
    }}
}}
EOF
    echo 'SF transactions trace (perfetto) configured to start along the other perfetto traces'
else
    su root service call SurfaceFlinger 1041 i32 1
    echo 'SF transactions trace (legacy) started'
fi
""",
        """
if ! is_perfetto_data_source_available android.surfaceflinger.transactions; then
    su root service call SurfaceFlinger 1041 i32 0 >/dev/null 2>&1
fi
"""
    ),
    "transactions_legacy": TraceTarget(
        [
            WinscopeFileMatcher(WINSCOPE_DIR, "transaction_trace", "transactions_legacy"),
            FileMatcher(WINSCOPE_DIR, f'transaction_merges_*', "transaction_merges"),
        ],
        'su root service call SurfaceFlinger 1020 i32 1\necho "SF transactions recording started."',
        'su root service call SurfaceFlinger 1020 i32 0 >/dev/null 2>&1'
    ),
    "proto_log": TraceTarget(
        WinscopeFileMatcher(WINSCOPE_DIR, "wm_log", "proto_log"),
        f"""
if is_perfetto_data_source_available android.protolog && \
    is_flag_set windowing_tools/android.tracing.perfetto_protolog_tracing; then
    cat << EOF >> {PERFETTO_TRACE_CONFIG_FILE}
data_sources: {{
    config {{
        name: "android.protolog"
    }}
}}
EOF
    echo 'ProtoLog (perfetto) configured to start along the other perfetto traces'
else
    su root cmd window logging start
    echo "ProtoLog (legacy) started."
fi
        """,
        """
if ! is_perfetto_data_source_available android.protolog && \
    ! is_flag_set windowing_tools/android.tracing.perfetto_protolog_tracing; then
    su root cmd window logging stop >/dev/null 2>&1
    echo "ProtoLog (legacy) stopped."
fi
        """
    ),
    "ime_trace_clients": TraceTarget(
        WinscopeFileMatcher(WINSCOPE_DIR, "ime_trace_clients", "ime_trace_clients"),
        'su root ime tracing start\necho "Clients IME trace started."',
        'su root ime tracing stop >/dev/null 2>&1'
    ),
   "ime_trace_service": TraceTarget(
        WinscopeFileMatcher(WINSCOPE_DIR, "ime_trace_service", "ime_trace_service"),
        'su root ime tracing start\necho "Service IME trace started."',
        'su root ime tracing stop >/dev/null 2>&1'
    ),
    "ime_trace_managerservice": TraceTarget(
        WinscopeFileMatcher(WINSCOPE_DIR, "ime_trace_managerservice", "ime_trace_managerservice"),
        'su root ime tracing start\necho "ManagerService IME trace started."',
        'su root ime tracing stop >/dev/null 2>&1'
    ),
    "wayland_trace": TraceTarget(
        WinscopeFileMatcher("/data/misc/wltrace", "wl_trace", "wl_trace"),
        'su root service call Wayland 26 i32 1 >/dev/null\necho "Wayland trace started."',
        'su root service call Wayland 26 i32 0 >/dev/null'
    ),
    "eventlog": TraceTarget(
        WinscopeFileMatcher("/data/local/tmp", "eventlog", "eventlog"),
        'rm -f /data/local/tmp/eventlog.winscope && EVENT_LOG_TRACING_START_TIME=$EPOCHREALTIME\necho "Event Log trace started."',
        'echo "EventLog\\n" > /data/local/tmp/eventlog.winscope && su root logcat -b events -v threadtime -v printable -v uid -v nsec -v epoch -b events -t $EVENT_LOG_TRACING_START_TIME >> /data/local/tmp/eventlog.winscope',
    ),
    "transition_traces": TraceTarget(
        [WinscopeFileMatcher(WINSCOPE_DIR, "wm_transition_trace", "wm_transition_trace"),
         WinscopeFileMatcher(WINSCOPE_DIR, "shell_transition_trace", "shell_transition_trace")],
         f"""
if is_perfetto_data_source_available com.android.wm.shell.transition && \
    is_flag_set windowing_tools/android.tracing.perfetto_transition_tracing; then
    cat << EOF >> {PERFETTO_TRACE_CONFIG_FILE}
data_sources: {{
    config {{
        name: "com.android.wm.shell.transition"
    }}
}}
EOF
    echo 'Transition trace (perfetto) configured to start along the other perfetto traces'
else
    su root cmd window shell tracing start && su root dumpsys activity service SystemUIService WMShell transitions tracing start
    echo "Transition traces (legacy) started."
fi
        """,
        """
if ! is_perfetto_data_source_available com.android.wm.shell.transition && \
    ! is_flag_set windowing_tools/android.tracing.perfetto_transition_tracing; then
    su root cmd window shell tracing stop && su root dumpsys activity service SystemUIService WMShell transitions tracing stop >/dev/null 2>&1
    echo 'Transition traces (legacy) stopped.'
fi
"""
    ),
    "perfetto_trace": TraceTarget(
        File(PERFETTO_TRACE_FILE, "trace.perfetto-trace"),
        f"""
if is_any_perfetto_data_source_available; then
    cat << EOF >> {PERFETTO_TRACE_CONFIG_FILE}
buffers: {{
    size_kb: 50000
    fill_policy: RING_BUFFER
}}
duration_ms: 0
flush_period_ms: 1000
write_into_file: true
max_file_size_bytes: 1000000000
EOF

    rm -f {PERFETTO_TRACE_FILE}
    perfetto --out {PERFETTO_TRACE_FILE} --txt --config {PERFETTO_TRACE_CONFIG_FILE} --detach=WINSCOPE-PROXY-TRACING-SESSION
    echo 'Started perfetto trace'
fi
""",
        """
if is_any_perfetto_data_source_available; then
    perfetto --attach=WINSCOPE-PROXY-TRACING-SESSION --stop
    echo 'Stopped perfetto trace'
fi
""",
    )
}


class SurfaceFlingerTraceConfig:
    """Handles optional configuration for surfaceflinger traces.
    """

    def __init__(self) -> None:
        self.flags = []
        self.perfetto_flags = []

    def add(self, config: str) -> None:
        self.flags.append(config)

    def is_valid(self, config: str) -> bool:
        return config in SF_LEGACY_FLAGS_MAP

    def command(self) -> str:
        legacy_flags = 0
        for flag in self.flags:
            legacy_flags |= SF_LEGACY_FLAGS_MAP[flag]

        perfetto_flags = "\n".join([f"""trace_flags: {SF_PERFETTO_FLAGS_MAP[flag]}""" for flag in self.flags])

        return f"""
{PERFETTO_UTILS}

if is_perfetto_data_source_available android.surfaceflinger.layers; then
    cat << EOF > {PERFETTO_SF_CONFIG_FILE}
data_sources: {{
    config {{
        name: "android.surfaceflinger.layers"
        surfaceflinger_layers_config: {{
            mode: MODE_ACTIVE
            {perfetto_flags}
        }}
    }}
}}
EOF
    echo 'SF trace (perfetto) configured.'
else
    su root service call SurfaceFlinger 1033 i32 {legacy_flags}
    echo 'SF trace (legacy) configured'
fi
"""

class SurfaceFlingerTraceSelectedConfig:
    """Handles optional selected configuration for surfaceflinger traces.
    """

    def __init__(self) -> None:
        # defaults set for all configs
        self.selectedConfigs = {
            "sfbuffersize": "16000"
        }

    def add(self, configType, configValue) -> None:
        self.selectedConfigs[configType] = configValue

    def is_valid(self, configType) -> bool:
        return configType in CONFIG_SF_SELECTION

    def setBufferSize(self) -> str:
        return f'su root service call SurfaceFlinger 1029 i32 {self.selectedConfigs["sfbuffersize"]}'

class WindowManagerTraceSelectedConfig:
    """Handles optional selected configuration for windowmanager traces.
    """

    def __init__(self) -> None:
        # defaults set for all configs
        self.selectedConfigs = {
            "wmbuffersize": "16000",
            "tracinglevel": "debug",
            "tracingtype": "frame",
        }

    def add(self, configType, configValue) -> None:
        self.selectedConfigs[configType] = configValue

    def is_valid(self, configType) -> bool:
        return configType in CONFIG_WM_SELECTION

    def setBufferSize(self) -> str:
        return f'su root cmd window tracing size {self.selectedConfigs["wmbuffersize"]}'

    def setTracingLevel(self) -> str:
        return f'su root cmd window tracing level {self.selectedConfigs["tracinglevel"]}'

    def setTracingType(self) -> str:
        return f'su root cmd window tracing {self.selectedConfigs["tracingtype"]}'


SF_LEGACY_FLAGS_MAP = {
    "input": 1 << 1,
    "composition": 1 << 2,
    "metadata": 1 << 3,
    "hwc": 1 << 4,
    "tracebuffers": 1 << 5,
    "virtualdisplays": 1 << 6
}

SF_PERFETTO_FLAGS_MAP = {
    "input": "TRACE_FLAG_INPUT",
    "composition": "TRACE_FLAG_COMPOSITION",
    "metadata": "TRACE_FLAG_EXTRA",
    "hwc": "TRACE_FLAG_HWC",
    "tracebuffers": "TRACE_FLAG_BUFFERS",
    "virtualdisplays": "TRACE_FLAG_VIRTUAL_DISPLAYS",
}

#Keep up to date with options in trace_collection_utils.ts
CONFIG_SF_SELECTION = [
    "sfbuffersize",
]

#Keep up to date with options in trace_collection_utils.ts
CONFIG_WM_SELECTION = [
    "wmbuffersize",
    "tracingtype",
    "tracinglevel",
]

class DumpTarget:
    """Defines a single parameter to trace.

    Attributes:
        file: the path on the device the dump results are saved to.
        dump_command: command to dump state to file.
    """

    def __init__(self, files, dump_command: str) -> None:
        if type(files) is not list:
            files = [files]
        self.files = files
        self.dump_command = dump_command


DUMP_TARGETS = {
    "window_dump": DumpTarget(
        File(f'/data/local/tmp/wm_dump{WINSCOPE_EXT}', "window_dump"),
        f'su root dumpsys window --proto > /data/local/tmp/wm_dump{WINSCOPE_EXT}'
    ),

    "layers_dump": DumpTarget(
        File(f'/data/local/tmp/sf_dump{WINSCOPE_EXT}', "layers_dump"),
        f"""
if is_perfetto_data_source_available android.surfaceflinger.layers; then
    cat << EOF >> {PERFETTO_DUMP_CONFIG_FILE}
data_sources: {{
    config {{
        name: "android.surfaceflinger.layers"
        surfaceflinger_layers_config: {{
            mode: MODE_DUMP
            trace_flags: TRACE_FLAG_INPUT
            trace_flags: TRACE_FLAG_COMPOSITION
            trace_flags: TRACE_FLAG_HWC
            trace_flags: TRACE_FLAG_BUFFERS
            trace_flags: TRACE_FLAG_VIRTUAL_DISPLAYS
        }}
    }}
}}
EOF
    echo 'SF transactions trace (perfetto) configured to start along the other perfetto traces'
else
    su root dumpsys SurfaceFlinger --proto > /data/local/tmp/sf_dump{WINSCOPE_EXT}
fi
"""
    ),

    "screenshot": DumpTarget(
        File("/data/local/tmp/screenshot.png", "screenshot.png"),
        "screencap -p > /data/local/tmp/screenshot.png"
    ),

    "perfetto_dump": DumpTarget(
        File(PERFETTO_DUMP_FILE, "dump.perfetto-trace"),
        f"""
if is_any_perfetto_data_source_available; then
    cat << EOF >> {PERFETTO_DUMP_CONFIG_FILE}
buffers: {{
    size_kb: 50000
    fill_policy: RING_BUFFER
}}
duration_ms: 1
EOF

    rm -f {PERFETTO_DUMP_FILE}
    perfetto --out {PERFETTO_DUMP_FILE} --txt --config {PERFETTO_DUMP_CONFIG_FILE}
    echo 'Recorded perfetto dump'
fi
        """
    )
}


# END OF CONFIG #


def get_token() -> str:
    """Returns saved proxy security token or creates new one"""
    try:
        with open(WINSCOPE_TOKEN_LOCATION, 'r') as token_file:
            token = token_file.readline()
            log.debug("Loaded token {} from {}".format(
                token, WINSCOPE_TOKEN_LOCATION))
            return token
    except IOError:
        token = secrets.token_hex(32)
        os.makedirs(os.path.dirname(WINSCOPE_TOKEN_LOCATION), exist_ok=True)
        try:
            with open(WINSCOPE_TOKEN_LOCATION, 'w') as token_file:
                log.debug("Created and saved token {} to {}".format(
                    token, WINSCOPE_TOKEN_LOCATION))
                token_file.write(token)
            os.chmod(WINSCOPE_TOKEN_LOCATION, 0o600)
        except IOError:
            log.error("Unable to save persistent token {} to {}".format(
                token, WINSCOPE_TOKEN_LOCATION))
        return token


class RequestType(Enum):
    GET = 1
    POST = 2
    HEAD = 3


def add_standard_headers(server):
    server.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
    server.send_header('Access-Control-Allow-Origin', '*')
    server.send_header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
    server.send_header('Access-Control-Allow-Headers',
                       WINSCOPE_TOKEN_HEADER + ', Content-Type, Content-Length')
    server.send_header('Access-Control-Expose-Headers',
                       'Winscope-Proxy-Version')
    server.send_header(WINSCOPE_VERSION_HEADER, VERSION)
    server.end_headers()


class RequestEndpoint:
    """Request endpoint to use with the RequestRouter."""

    @abstractmethod
    def process(self, server, path):
        pass


class AdbError(Exception):
    """Unsuccessful ADB operation"""
    pass


class BadRequest(Exception):
    """Invalid client request"""
    pass


class RequestRouter:
    """Handles HTTP request authentication and routing"""

    def __init__(self, handler):
        self.request = handler
        self.endpoints = {}

    def register_endpoint(self, method: RequestType, name: str, endpoint: RequestEndpoint):
        self.endpoints[(method, name)] = endpoint

    def __bad_request(self, error: str):
        log.warning("Bad request: " + error)
        self.request.respond(HTTPStatus.BAD_REQUEST, b"Bad request!\nThis is Winscope ADB proxy.\n\n"
                             + error.encode("utf-8"), 'text/txt')

    def __internal_error(self, error: str):
        log.error("Internal error: " + error)
        self.request.respond(HTTPStatus.INTERNAL_SERVER_ERROR,
                             error.encode("utf-8"), 'text/txt')

    def __bad_token(self):
        log.info("Bad token")
        self.request.respond(HTTPStatus.FORBIDDEN, b"Bad Winscope authorisation token!\nThis is Winscope ADB proxy.\n",
                             'text/txt')

    def process(self, method: RequestType):
        token = self.request.headers[WINSCOPE_TOKEN_HEADER]
        if not token or token != secret_token:
            return self.__bad_token()
        path = self.request.path.strip('/').split('/')
        if path and len(path) > 0:
            endpoint_name = path[0]
            try:
                return self.endpoints[(method, endpoint_name)].process(self.request, path[1:])
            except KeyError:
                return self.__bad_request("Unknown endpoint /{}/".format(endpoint_name))
            except AdbError as ex:
                return self.__internal_error(str(ex))
            except BadRequest as ex:
                return self.__bad_request(str(ex))
            except Exception as ex:
                return self.__internal_error(repr(ex))
        self.__bad_request("No endpoint specified")


def call_adb(params: str, device: str = None, stdin: bytes = None):
    command = ['adb'] + (['-s', device] if device else []) + params.split(' ')
    try:
        log.debug("Call: " + ' '.join(command))
        return subprocess.check_output(command, stderr=subprocess.STDOUT, input=stdin).decode('utf-8')
    except OSError as ex:
        log.debug('Error executing adb command: {}\n{}'.format(
            ' '.join(command), repr(ex)))
        raise AdbError('Error executing adb command: {}\n{}'.format(
            ' '.join(command), repr(ex)))
    except subprocess.CalledProcessError as ex:
        log.debug('Error executing adb command: {}\n{}'.format(
            ' '.join(command), ex.output.decode("utf-8")))
        raise AdbError('Error executing adb command: adb {}\n{}'.format(
            params, ex.output.decode("utf-8")))


def call_adb_outfile(params: str, outfile, device: str = None, stdin: bytes = None):
    try:
        process = subprocess.Popen(['adb'] + (['-s', device] if device else []) + params.split(' '), stdout=outfile,
                                   stderr=subprocess.PIPE)
        _, err = process.communicate(stdin)
        outfile.seek(0)
        if process.returncode != 0:
            log.debug('Error executing adb command: adb {}\n'.format(params) + err.decode(
                'utf-8') + '\n' + outfile.read().decode('utf-8'))
            raise AdbError('Error executing adb command: adb {}\n'.format(params) + err.decode(
                'utf-8') + '\n' + outfile.read().decode('utf-8'))
    except OSError as ex:
        log.debug('Error executing adb command: adb {}\n{}'.format(
            params, repr(ex)))
        raise AdbError(
            'Error executing adb command: adb {}\n{}'.format(params, repr(ex)))


class CheckWaylandServiceEndpoint(RequestEndpoint):
    _listDevicesEndpoint = None

    def __init__(self, listDevicesEndpoint):
      self._listDevicesEndpoint = listDevicesEndpoint

    def process(self, server, path):
        self._listDevicesEndpoint.process(server, path)
        foundDevices = self._listDevicesEndpoint._foundDevices

        if len(foundDevices) > 1:
          res = 'false'
        else:
          raw_res = call_adb('shell service check Wayland')
          res = 'false' if 'not found' in raw_res else 'true'
        server.respond(HTTPStatus.OK, res.encode("utf-8"), "text/json")


class ListDevicesEndpoint(RequestEndpoint):
    ADB_INFO_RE = re.compile("^([A-Za-z0-9._:\\-]+)\\s+(\\w+)(.*model:(\\w+))?")
    _foundDevices = None

    def process(self, server, path):
        lines = list(filter(None, call_adb('devices -l').split('\n')))
        devices = {m.group(1): {
            'authorised': str(m.group(2)) != 'unauthorized',
            'model': m.group(4).replace('_', ' ') if m.group(4) else ''
        } for m in [ListDevicesEndpoint.ADB_INFO_RE.match(d) for d in lines[1:]] if m}
        self._foundDevices = devices
        j = json.dumps(devices)
        log.debug("Detected devices: " + j)
        server.respond(HTTPStatus.OK, j.encode("utf-8"), "text/json")


class DeviceRequestEndpoint(RequestEndpoint):
    def process(self, server, path):
        if len(path) > 0 and re.fullmatch("[A-Za-z0-9.:\\-]+", path[0]):
            self.process_with_device(server, path[1:], path[0])
        else:
            raise BadRequest("Device id not specified")

    @abstractmethod
    def process_with_device(self, server, path, device_id):
        pass

    def get_request(self, server) -> str:
        try:
            length = int(server.headers["Content-Length"])
        except KeyError as err:
            raise BadRequest("Missing Content-Length header\n" + str(err))
        except ValueError as err:
            raise BadRequest("Content length unreadable\n" + str(err))
        return json.loads(server.rfile.read(length).decode("utf-8"))

    def move_perfetto_target_to_end_of_list(self, targets):
        # Make sure a perfetto target (if present) comes last in the list of targets, i.e. will
        # be processed last.
        # A perfetto target must be processed last, so that perfetto tracing is started only after
        # the other targets have been processed and have configured the perfetto config file.
        def is_perfetto_target(target):
            return target == TRACE_TARGETS["perfetto_trace"] or target == DUMP_TARGETS["perfetto_dump"]
        non_perfetto_targets = [t for t in targets if not is_perfetto_target(t)]
        perfetto_targets = [t for t in targets if is_perfetto_target(t)]
        return non_perfetto_targets + perfetto_targets



class FetchFilesEndpoint(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        if len(path) != 1:
            raise BadRequest("File not specified")
        if path[0] in TRACE_TARGETS:
            files = TRACE_TARGETS[path[0]].files
        elif path[0] in DUMP_TARGETS:
            files = DUMP_TARGETS[path[0]].files
        else:
            raise BadRequest("Unknown file specified")

        file_buffers = dict()

        for f in files:
            file_type = f.get_filetype()
            file_paths = f.get_filepaths(device_id)

            for file_path in file_paths:
                with NamedTemporaryFile() as tmp:
                    log.debug(
                        f"Fetching file {file_path} from device to {tmp.name}")
                    call_adb_outfile('exec-out su root cat ' +
                                     file_path, tmp, device_id)
                    log.debug(f"Deleting file {file_path} from device")
                    call_adb('shell su root rm -f ' + file_path, device_id)
                    log.debug(f"Uploading file {tmp.name}")
                    if file_type not in file_buffers:
                        file_buffers[file_type] = []
                    buf = base64.encodebytes(tmp.read()).decode("utf-8")
                    file_buffers[file_type].append(buf)

        if (len(file_buffers) == 0):
            log.error("Proxy didn't find any file to fetch")

        # server.send_header('X-Content-Type-Options', 'nosniff')
        # add_standard_headers(server)
        j = json.dumps(file_buffers)
        server.respond(HTTPStatus.OK, j.encode("utf-8"), "text/json")


def check_root(device_id):
    log.debug("Checking root access on {}".format(device_id))
    return int(call_adb('shell su root id -u', device_id)) == 0


TRACE_THREADS = {}


class TraceThread(threading.Thread):
    def __init__(self, device_id, command):
        self._keep_alive_timer = None
        self.trace_command = command
        self._device_id = device_id
        self.out = None,
        self.err = None,
        self._success = False
        try:
            shell = ['adb', '-s', self._device_id, 'shell']
            log.debug("Starting trace shell {}".format(' '.join(shell)))
            self.process = subprocess.Popen(shell, stdout=subprocess.PIPE,
                                            stderr=subprocess.PIPE, stdin=subprocess.PIPE, start_new_session=True)
        except OSError as ex:
            raise AdbError(
                'Error executing adb command: adb shell\n{}'.format(repr(ex)))

        super().__init__()

    def timeout(self):
        if self.is_alive():
            log.warning(
                "Keep-alive timeout for trace on {}".format(self._device_id))
            self.end_trace()
            if self._device_id in TRACE_THREADS:
                TRACE_THREADS.pop(self._device_id)

    def reset_timer(self):
        log.debug(
            "Resetting keep-alive clock for trace on {}".format(self._device_id))
        if self._keep_alive_timer:
            self._keep_alive_timer.cancel()
        self._keep_alive_timer = threading.Timer(
            KEEP_ALIVE_INTERVAL_S, self.timeout)
        self._keep_alive_timer.start()

    def end_trace(self):
        if self._keep_alive_timer:
            self._keep_alive_timer.cancel()
        log.debug("Sending SIGINT to the trace process on {}".format(
            self._device_id))
        self.process.send_signal(signal.SIGINT)
        try:
            log.debug("Waiting for trace shell to exit for {}".format(
                self._device_id))
            self.process.wait(timeout=5)
        except TimeoutError:
            log.debug(
                "TIMEOUT - sending SIGKILL to the trace process on {}".format(self._device_id))
            self.process.kill()
        self.join()

    def run(self):
        log.debug("Trace started on {}".format(self._device_id))
        self.reset_timer()
        self.out, self.err = self.process.communicate(self.trace_command)
        log.debug("Trace ended on {}, waiting for cleanup".format(self._device_id))
        time.sleep(0.2)
        for i in range(50):
            if call_adb("shell su root cat /data/local/tmp/winscope_status", device=self._device_id) == 'TRACE_OK\n':
                call_adb(
                    "shell su root rm /data/local/tmp/winscope_status", device=self._device_id)
                log.debug("Trace finished successfully on {}".format(
                    self._device_id))
                self._success = True
                break
            log.debug("Still waiting for cleanup on {}".format(self._device_id))
            time.sleep(0.1)

    def success(self):
        return self._success


class StartTrace(DeviceRequestEndpoint):
    TRACE_COMMAND = """
set -e

{perfetto_utils}

echo "Starting trace..."
echo "TRACE_START" > /data/local/tmp/winscope_status

# Do not print anything to stdout/stderr in the handler
function stop_trace() {{
  echo "start" >/data/local/tmp/winscope_signal_handler.log

  # redirect stdout/stderr to log file
  exec 1>>/data/local/tmp/winscope_signal_handler.log
  exec 2>>/data/local/tmp/winscope_signal_handler.log

  set -x
  trap - EXIT HUP INT
  {stop_commands}
  echo "TRACE_OK" > /data/local/tmp/winscope_status
}}

trap stop_trace EXIT HUP INT
echo "Signal handler registered."

# Clear perfetto config file. The start commands below are going to populate it.
rm -f {perfetto_config_file}

{start_commands}

# ADB shell does not handle hung up well and does not call HUP handler when a child is active in foreground,
# as a workaround we sleep for short intervals in a loop so the handler is called after a sleep interval.
while true; do sleep 0.1; done
"""

    def process_with_device(self, server, path, device_id):
        try:
            requested_types = self.get_request(server)
            log.debug(f"Clienting requested trace types {requested_types}")
            requested_traces = [TRACE_TARGETS[t] for t in requested_types]
            requested_traces = self.move_perfetto_target_to_end_of_list(requested_traces)
        except KeyError as err:
            raise BadRequest("Unsupported trace target\n" + str(err))
        if device_id in TRACE_THREADS:
            log.warning("Trace already in progress for {}", device_id)
            server.respond(HTTPStatus.OK, b'', "text/plain")
        if not check_root(device_id):
            raise AdbError(
                "Unable to acquire root privileges on the device - check the output of 'adb -s {} shell su root id'".format(
                    device_id))
        command = StartTrace.TRACE_COMMAND.format(
            perfetto_utils=PERFETTO_UTILS,
            stop_commands='\n'.join([t.trace_stop for t in requested_traces]),
            perfetto_config_file=PERFETTO_TRACE_CONFIG_FILE,
            start_commands='\n'.join([t.trace_start for t in requested_traces]))
        log.debug("Trace requested for {} with targets {}".format(
            device_id, ','.join(requested_types)))
        log.debug(f"Executing command \"{command}\" on {device_id}...")
        TRACE_THREADS[device_id] = TraceThread(
            device_id, command.encode('utf-8'))
        TRACE_THREADS[device_id].start()
        server.respond(HTTPStatus.OK, b'', "text/plain")


class EndTrace(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        if device_id not in TRACE_THREADS:
            raise BadRequest("No trace in progress for {}".format(device_id))
        if TRACE_THREADS[device_id].is_alive():
            TRACE_THREADS[device_id].end_trace()

        success = TRACE_THREADS[device_id].success()

        signal_handler_log = call_adb("shell su root cat /data/local/tmp/winscope_signal_handler.log", device=device_id).encode('utf-8')

        out = b"### Shell script's stdout - start\n" + \
            TRACE_THREADS[device_id].out + \
            b"### Shell script's stdout - end\n" + \
            b"### Shell script's stderr - start\n" + \
            TRACE_THREADS[device_id].err + \
            b"### Shell script's stderr - end\n" + \
            b"### Signal handler log - start\n" + \
            signal_handler_log + \
            b"### Signal handler log - end\n"
        command = TRACE_THREADS[device_id].trace_command
        TRACE_THREADS.pop(device_id)
        if success:
            server.respond(HTTPStatus.OK, out, "text/plain")
        else:
            raise AdbError(
                "Error tracing the device\n### Output ###\n" + out.decode(
                    "utf-8") + "\n### Command: adb -s {} shell ###\n### Input ###\n".format(device_id) + command.decode(
                    "utf-8"))


def execute_command(server, device_id, shell, configType, configValue):
    process = subprocess.Popen(shell, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   stdin=subprocess.PIPE, start_new_session=True)
    log.debug(f"Changing trace config on device {device_id} {configType}:{configValue}")
    out, err = process.communicate(configValue.encode('utf-8'))
    if process.returncode != 0:
        raise AdbError(
            f"Error executing command:\n {configValue}\n\n### OUTPUT ###{out.decode('utf-8')}\n{err.decode('utf-8')}")
    log.debug(f"Changing trace config finished on device {device_id}")
    server.respond(HTTPStatus.OK, b'', "text/plain")


class ConfigTrace(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        try:
            requested_configs = self.get_request(server)
            config = SurfaceFlingerTraceConfig()
            for requested_config in requested_configs:
                if not config.is_valid(requested_config):
                    raise BadRequest(
                        f"Unsupported config {requested_config}\n")
                config.add(requested_config)
        except KeyError as err:
            raise BadRequest("Unsupported trace target\n" + str(err))
        if device_id in TRACE_THREADS:
            BadRequest(f"Trace in progress for {device_id}")
        if not check_root(device_id):
            raise AdbError(
                f"Unable to acquire root privileges on the device - check the output of 'adb -s {device_id} shell su root id'")
        command = config.command()
        shell = ['adb', '-s', device_id, 'shell']
        log.debug(f"Starting shell {' '.join(shell)}")
        execute_command(server, device_id, shell, "sf buffer size", command)


def add_selected_request_to_config(self, server, device_id, config):
    try:
        requested_configs = self.get_request(server)
        for requested_config in requested_configs:
            if config.is_valid(requested_config):
                config.add(requested_config, requested_configs[requested_config])
            else:
                raise BadRequest(
                        f"Unsupported config {requested_config}\n")
    except KeyError as err:
        raise BadRequest("Unsupported trace target\n" + str(err))
    if device_id in TRACE_THREADS:
        BadRequest(f"Trace in progress for {device_id}")
    if not check_root(device_id):
        raise AdbError(
            f"Unable to acquire root privileges on the device - check the output of 'adb -s {device_id} shell su root id'")
    return config


class SurfaceFlingerSelectedConfigTrace(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        config = SurfaceFlingerTraceSelectedConfig()
        config = add_selected_request_to_config(self, server, device_id, config)
        setBufferSize = config.setBufferSize()
        shell = ['adb', '-s', device_id, 'shell']
        log.debug(f"Starting shell {' '.join(shell)}")
        execute_command(server, device_id, shell, "sf buffer size", setBufferSize)


class WindowManagerSelectedConfigTrace(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        config = WindowManagerTraceSelectedConfig()
        config = add_selected_request_to_config(self, server, device_id, config)
        setBufferSize = config.setBufferSize()
        setTracingType = config.setTracingType()
        setTracingLevel = config.setTracingLevel()
        shell = ['adb', '-s', device_id, 'shell']
        log.debug(f"Starting shell {' '.join(shell)}")
        execute_command(server, device_id, shell, "tracing type", setTracingType)
        execute_command(server, device_id, shell, "tracing level", setTracingLevel)
        # /!\ buffer size must be configured last
        # otherwise the other configurations might override it
        execute_command(server, device_id, shell, "wm buffer size", setBufferSize)


class StatusEndpoint(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        if device_id not in TRACE_THREADS:
            raise BadRequest("No trace in progress for {}".format(device_id))
        TRACE_THREADS[device_id].reset_timer()
        server.respond(HTTPStatus.OK, str(
            TRACE_THREADS[device_id].is_alive()).encode("utf-8"), "text/plain")


class DumpEndpoint(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        try:
            requested_types = self.get_request(server)
            requested_traces = [DUMP_TARGETS[t] for t in requested_types]
            requested_traces = self.move_perfetto_target_to_end_of_list(requested_traces)
        except KeyError as err:
            raise BadRequest("Unsupported trace target\n" + str(err))
        if device_id in TRACE_THREADS:
            BadRequest("Trace in progress for {}".format(device_id))
        if not check_root(device_id):
            raise AdbError(
                "Unable to acquire root privileges on the device - check the output of 'adb -s {} shell su root id'"
                .format(device_id))
        dump_commands = '\n'.join(t.dump_command for t in requested_traces)
        command = f"""
{PERFETTO_UTILS}

# Clear perfetto config file. The commands below are going to populate it.
rm -f {PERFETTO_DUMP_CONFIG_FILE}

{dump_commands}
"""
        shell = ['adb', '-s', device_id, 'shell']
        log.debug("Starting dump shell {}".format(' '.join(shell)))
        process = subprocess.Popen(shell, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                   stdin=subprocess.PIPE, start_new_session=True)
        log.debug("Starting dump on device {}".format(device_id))
        out, err = process.communicate(command.encode('utf-8'))
        if process.returncode != 0:
            raise AdbError("Error executing command:\n" + command + "\n\n### OUTPUT ###" + out.decode('utf-8') + "\n"
                           + err.decode('utf-8'))
        log.debug("Dump finished on device {}".format(device_id))
        server.respond(HTTPStatus.OK, b'', "text/plain")


class ADBWinscopeProxy(BaseHTTPRequestHandler):
    def __init__(self, request, client_address, server):
        self.router = RequestRouter(self)
        listDevicesEndpoint = ListDevicesEndpoint()
        self.router.register_endpoint(
            RequestType.GET, "devices", listDevicesEndpoint)
        self.router.register_endpoint(
            RequestType.GET, "status", StatusEndpoint())
        self.router.register_endpoint(
            RequestType.GET, "fetch", FetchFilesEndpoint())
        self.router.register_endpoint(RequestType.POST, "start", StartTrace())
        self.router.register_endpoint(RequestType.POST, "end", EndTrace())
        self.router.register_endpoint(RequestType.POST, "dump", DumpEndpoint())
        self.router.register_endpoint(
            RequestType.POST, "configtrace", ConfigTrace())
        self.router.register_endpoint(
            RequestType.POST, "selectedsfconfigtrace", SurfaceFlingerSelectedConfigTrace())
        self.router.register_endpoint(
            RequestType.POST, "selectedwmconfigtrace", WindowManagerSelectedConfigTrace())
        self.router.register_endpoint(
            RequestType.GET, "checkwayland", CheckWaylandServiceEndpoint(listDevicesEndpoint))
        super().__init__(request, client_address, server)

    def respond(self, code: int, data: bytes, mime: str) -> None:
        self.send_response(code)
        self.send_header('Content-type', mime)
        add_standard_headers(self)
        self.wfile.write(data)

    def do_GET(self):
        self.router.process(RequestType.GET)

    def do_POST(self):
        self.router.process(RequestType.POST)

    def do_OPTIONS(self):
        self.send_response(HTTPStatus.OK)
        self.send_header('Allow', 'GET,POST')
        add_standard_headers(self)
        self.end_headers()
        self.wfile.write(b'GET,POST')

    def log_request(self, code='-', size='-'):
        log.info('{} {} {}'.format(self.requestline, str(code), str(size)))


if __name__ == '__main__':
    args = create_argument_parser().parse_args()

    logging.basicConfig(stream=sys.stderr, level=args.loglevel,
                        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    log = logging.getLogger("ADBProxy")
    secret_token = get_token()

    print("Winscope ADB Connect proxy version: " + VERSION)
    print('Winscope token: ' + secret_token)

    httpd = HTTPServer(('localhost', args.port), ADBWinscopeProxy)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        log.info("Shutting down")
