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
# Requirements: python3.10 and ADB installed and in system PATH.
#
# Usage:
#     run: python3 winscope_proxy.py
#

import argparse
import base64
import gzip
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
from logging import DEBUG, INFO
from tempfile import NamedTemporaryFile

version = sys.version_info
assert version.major == 3 and version.minor >= 10, "This script requires Python 3.10+ and ADB installed and in system PATH."

# GLOBALS #

log = None
secret_token = None

# Keep in sync with winscope_proxy_utils VERSION in Winscope
VERSION = '6.0.0'

WINSCOPE_VERSION_HEADER = "Winscope-Proxy-Version"
WINSCOPE_TOKEN_HEADER = "Winscope-Token"

# Location to save the proxy security token
WINSCOPE_TOKEN_LOCATION = os.path.expanduser('~/.config/winscope/.token')

# Tracing handlers
SIGNAL_HANDLER_LOG = "/data/local/tmp/winscope_signal_handler.log"
WINSCOPE_STATUS = "/data/local/tmp/winscope_status"

# Max interval between the client keep-alive requests in seconds
KEEP_ALIVE_INTERVAL_S = 5

# Perfetto's default timeout for getting an ACK from producer processes is 5s
# We need to be sure that the timeout is longer than that with a good margin.
COMMAND_TIMEOUT_S = 15


# CONFIG #

def create_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description='Proxy for go/winscope', prog='winscope_proxy')

    parser.add_argument('--info', '-i', dest='loglevel', action='store_const', const=INFO)
    parser.add_argument('--port', '-p', default=5544, action='store')

    parser.set_defaults(loglevel=DEBUG)

    return parser

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

    def _bad_request(self, error: str):
        log.warning("Bad request: " + error)
        self.request.respond(HTTPStatus.BAD_REQUEST, b"Bad request!\nThis is Winscope ADB proxy.\n\n"
                             + error.encode("utf-8"), 'text/txt')

    def _internal_error(self, error: str):
        log.error("Internal error: " + error)
        self.request.respond(HTTPStatus.INTERNAL_SERVER_ERROR,
                             error.encode("utf-8"), 'text/txt')

    def _bad_token(self):
        log.warning("Bad token")
        self.request.respond(HTTPStatus.FORBIDDEN, b"Bad Winscope authorization token!\nThis is Winscope ADB proxy.\n",
                             'text/txt')

    def process(self, method: RequestType):
        token = self.request.headers[WINSCOPE_TOKEN_HEADER]
        if not token or token != secret_token:
            return self._bad_token()
        path = self.request.path.strip('/').split('/')
        if path and len(path) > 0:
            endpoint_name = path[0]
            try:
                return self.endpoints[(method, endpoint_name)].process(self.request, path[1:])
            except KeyError as ex:
                if "RequestType" in repr(ex):
                    return self._bad_request("Unknown endpoint /{}/".format(endpoint_name))
                return self._internal_error(repr(ex))
            except AdbError as ex:
                return self._internal_error(str(ex))
            except BadRequest as ex:
                return self._bad_request(str(ex))
            except Exception as ex:
                return self._internal_error(repr(ex))
        self._bad_request("No endpoint specified")

def call_adb(params: str, device: str = None):
    command = ['adb'] + (['-s', device] if device else []) + params.split(' ')
    command_str = ' '.join(command)
    try:
        log.debug("Call: " + command_str)
        return subprocess.check_output(command, stderr=subprocess.STDOUT).decode('utf-8')
    except OSError as ex:
        raise AdbError('OS Error executing adb command: {}\n{}'.format(command_str, repr(ex)))
    except subprocess.CalledProcessError as ex:
        return 'Error executing adb command: {}: {}'.format(command_str, ex.output.decode("utf-8"))


# ENDPOINTS #

class ListDevicesEndpoint(RequestEndpoint):
    ADB_INFO_RE = re.compile("^([A-Za-z0-9._:\\-]+)\\s+(\\w+)(.*model:(\\w+))?")

    def process(self, server, path):
        lines = list(filter(None, call_adb('devices -l').split('\n')))
        devices = []
        for m in [ListDevicesEndpoint.ADB_INFO_RE.match(d) for d in lines[1:]]:
            if m:
                authorized = str(m.group(2)) != 'unauthorized'
                device = {
                    'id': m.group(1),
                    'authorized': authorized,
                    'model': m.group(4).replace('_', ' ') if m.group(4) else '',
                }
                devices.append(device)
        j = json.dumps(devices)
        log.info("Detected devices: " + j)
        server.respond(HTTPStatus.OK, j.encode("utf-8"), "text/json")

class DeviceRequestEndpoint(RequestEndpoint):
    def process(self, server, path):
        if len(path) > 0 and re.fullmatch("[A-Za-z0-9._:\\-]+", path[0]):
            self.process_with_device(server, path[1:], path[0])
        else:
            raise BadRequest("Device id not specified")

    @abstractmethod
    def process_with_device(self, server, path, device_id):
        pass

    def get_request(self, server):
        try:
            length = int(server.headers["Content-Length"])
        except KeyError as err:
            raise BadRequest("Missing Content-Length header\n" + str(err))
        except ValueError as err:
            raise BadRequest("Content length unreadable\n" + str(err))
        return json.loads(server.rfile.read(length).decode("utf-8"))

class FetchEndpoint(DeviceRequestEndpoint):
    def process_with_device(self, server, path: list[str], device_id):
        filepath = '/'.join(path)
        log.debug(filepath)
        file_buffer = self.fetch_existing_file(filepath, device_id)
        server.respond(HTTPStatus.OK, json.dumps(file_buffer).encode("utf-8"), "text/json")

    def fetch_existing_file(self, filepath, device_id):
        file_buffer = dict()
        try:
            with NamedTemporaryFile() as tmp:
                log.debug(
                    f"Fetching file {filepath} from device to {tmp.name}")
                try:
                    self.call_adb_outfile('exec-out su root cat ' +
                                        filepath, tmp, device_id)
                except AdbError as ex:
                    log.warning(f"Unable to fetch file {filepath} - {repr(ex)}")
                    return
                log.debug(f"Uploading file {tmp.name}")
                buf = base64.encodebytes(gzip.compress(tmp.read())).decode("utf-8")
                file_buffer[filepath] = buf
        except:
            self.log_no_files_warning()
        return file_buffer

    def log_no_files_warning(self):
        log.warning("Proxy didn't find any file to fetch")

    def call_adb_outfile(self, params: str, outfile, device: str):
        try:
            process = subprocess.Popen(['adb'] + ['-s', device] + params.split(' '), stdout=outfile,
                                    stderr=subprocess.PIPE)
            _, err = process.communicate()
            outfile.seek(0)
            if process.returncode != 0:
                raise AdbError('Error executing adb command: adb {}\n'.format(params) + err.decode(
                    'utf-8') + '\n' + outfile.read().decode('utf-8'))
        except OSError as ex:
            raise AdbError(
                'Error executing adb command: adb {}\n{}'.format(params, repr(ex)))

class TraceThread(threading.Thread):
    def __init__(self, target_id: str, device_id: str, command: str, status_filename: str):
        self.trace_command = command
        self.target_id = target_id
        self.status_filename = status_filename
        self._device_id = device_id
        self._keep_alive_timer = None
        self.out = None,
        self.err = None,
        self._command_timed_out = False
        self._success = False
        try:
            shell = self.get_shell_args()
            self.process = subprocess.Popen(shell, stdout=subprocess.PIPE,
                                            stderr=subprocess.PIPE, stdin=subprocess.PIPE, start_new_session=True)
        except OSError as ex:
            raise AdbError(
                'Error executing adb command for trace {}: {}'.format(target_id, repr(ex)))

        super().__init__()

    def get_shell_args(self) -> list[str]:
        shell = ['adb', '-s', self._device_id, 'shell']
        log.debug(f"Starting trace shell {' '.join(shell)}")
        return shell

    def timeout(self):
        if self.is_alive():
            log.warning("Keep-alive timeout for {} trace on {}".format(self.target_id, self._device_id))
            self.end_trace()

    def reset_timer(self):
        log.info(
            "Resetting keep-alive clock for {} trace on {}".format(self.target_id, self._device_id))
        if self._keep_alive_timer:
            self._keep_alive_timer.cancel()
        self._keep_alive_timer = threading.Timer(
            KEEP_ALIVE_INTERVAL_S, self.timeout)
        self._keep_alive_timer.start()

    def end_trace(self):
        if self._keep_alive_timer:
            self._keep_alive_timer.cancel()
        log.info("Sending SIGINT to the {} process on {}".format(
            self.target_id,
            self._device_id))
        self.process.send_signal(signal.SIGINT)
        try:
            log.debug("Waiting for {} trace shell to exit for {}".format(
                self.target_id,
                self._device_id))
            self.process.wait(timeout=COMMAND_TIMEOUT_S)
        except TimeoutError:
            log.error(
                "TIMEOUT - sending SIGKILL to the {} trace process on {}".format(self.target_id, self._device_id))
            self.process.kill()
        self.join()

    def run(self):
        retry_interval = 0.1
        log.info("Trace {} started on {}".format(self.target_id, self._device_id))
        self.reset_timer()
        self.out, self.err = self.process.communicate(self.trace_command)
        log.info("Trace {} ended on {}, waiting for cleanup".format(self.target_id, self._device_id))
        time.sleep(0.2)
        for i in range(int(COMMAND_TIMEOUT_S / retry_interval)):
            if call_adb(f"shell su root cat {self.status_filename}", device=self._device_id) == 'TRACE_OK\n':
                log.info("Trace {} finished on {}".format(
                    self.target_id,
                    self._device_id))
                if self.target_id == "PerfettoTrace":
                    self._success = True
                else:
                    self._success = len(self.err) == 0
                return
            log.debug("Still waiting for cleanup on {} for {}".format(self._device_id, self.target_id))
            time.sleep(retry_interval)

        self._command_timed_out = True

    def success(self):
        return self._success

    def timed_out(self):
        return self._command_timed_out

TRACE_THREADS: dict[str, dict[str, TraceThread]] = {}

class StartTraceEndpoint(DeviceRequestEndpoint):
    COMMAND = """
set -e

echo "Opening shell..."
echo "TRACE_START" > {winscope_status}

# Do not print anything to stdout/stderr in the handler
function close_shell() {{
  echo "start" >{signal_handler_log}

  # redirect stdout/stderr to log file
  exec 1>>{signal_handler_log}
  exec 2>>{signal_handler_log}

  set -x
  trap - EXIT HUP INT
  {stop_commands}
  echo "TRACE_OK" > {winscope_status}
}}

trap close_shell EXIT HUP INT
echo "Signal handler registered."

{start_commands}

# ADB shell does not handle hung up well and does not call HUP handler when a child is active in foreground,
# as a workaround we sleep for short intervals in a loop so the handler is called after a sleep interval.
while true; do sleep 0.1; done
"""

    def process_with_device(self, server, path, device_id):
        request: dict = self.get_request(server)
        target_id = request.get("targetId")
        start_cmd = request.get("startCmd")
        stop_cmd = request.get("stopCmd")
        status_filename = WINSCOPE_STATUS + "_" + target_id

        command = StartTraceEndpoint.COMMAND.format(
            winscope_status=status_filename,
            signal_handler_log=SIGNAL_HANDLER_LOG,
            stop_commands=stop_cmd,
            start_commands=start_cmd,
        )
        log.debug(f"Executing start command for {target_id} on {device_id}...")
        thread = TraceThread(target_id, device_id, command.encode('utf-8'), status_filename)
        if device_id not in TRACE_THREADS:
            threads = {}
            threads[target_id] = thread
            TRACE_THREADS[device_id] = threads

        else:
            TRACE_THREADS[device_id][target_id] = thread
        thread.start()

        server.respond(HTTPStatus.OK, ''.encode('utf-8'), "text/json")

class EndTraceEndpoint(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        if device_id not in TRACE_THREADS:
            raise BadRequest("No trace in progress for {}".format(device_id))

        request = self.get_request(server)
        target_id = request.get("targetId")
        threads = TRACE_THREADS[device_id]
        if target_id not in threads:
            raise BadRequest("No {} trace in progress for {}".format(target_id, device_id))

        errors: list[str] = []
        thread = threads[target_id]

        if thread.is_alive():
            thread.end_trace()
        success = thread.success()
        signal_handler_log = call_adb(f"shell su root cat {SIGNAL_HANDLER_LOG}", device=device_id).encode('utf-8')

        if (thread.timed_out()):
            timeout_message = "Trace {} timed out during cleanup".format(target_id)
            errors.append(timeout_message)
            log.error(timeout_message)

        if not success:
            log.error("Error ending trace {} on the device".format(target_id))
            errors.append("Error ending trace {} on the device: {}".format(target_id, thread.err))

        out = b"### Shell script's stdout ###\n" + \
            (thread.out if thread.out else b'<no stdout>') + \
            b"\n### Shell script's stderr ###\n" + \
            (thread.err if thread.err else b'<no stderr>') + \
            b"\n### Signal handler log ###\n" + \
            (signal_handler_log if signal_handler_log else b'<no signal handler logs>') + \
            b"\n"
        log.debug("### Output ###\n".format(target_id) + out.decode("utf-8"))

        call_adb(f"shell su root rm {thread.status_filename}", device=device_id)

        threads.pop(target_id)

        if len(threads) == 0:
            TRACE_THREADS.pop(device_id)
        server.respond(HTTPStatus.OK, json.dumps(errors).encode("utf-8"), "text/plain")

class StatusEndpoint(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        if device_id not in TRACE_THREADS:
            raise BadRequest("No trace in progress for {}".format(device_id))

        if path[0] not in TRACE_THREADS[device_id]:
            log.debug(path[0])
            log.debug(TRACE_THREADS[device_id])
            server.respond(HTTPStatus.OK, str(False).encode("utf-8"), "text/plain")
        else:
            thread = TRACE_THREADS[device_id][path[0]]
            thread.reset_timer()
            server.respond(HTTPStatus.OK, str(thread.is_alive()).encode("utf-8"), "text/plain")

class RunAdbCmdEndpoint(DeviceRequestEndpoint):
    def process_with_device(self, server, path, device_id):
        request: dict = self.get_request(server)
        cmd: str = request.get("cmd")
        output = call_adb(cmd, device_id)
        server.respond(HTTPStatus.OK, json.dumps(output).encode("utf-8"), "text/plain")


class ADBWinscopeProxy(BaseHTTPRequestHandler):
    def __init__(self, request, client_address, server):
        self.router = RequestRouter(self)
        listDevicesEndpoint = ListDevicesEndpoint()
        self.router.register_endpoint(
            RequestType.GET, "devices", listDevicesEndpoint)
        self.router.register_endpoint(
            RequestType.GET, "status", StatusEndpoint())
        self.router.register_endpoint(
            RequestType.GET, "fetch", FetchEndpoint())
        self.router.register_endpoint(
            RequestType.POST, "runadbcmd", RunAdbCmdEndpoint())
        self.router.register_endpoint(
            RequestType.POST, "starttrace", StartTraceEndpoint())
        self.router.register_endpoint(
            RequestType.POST, "endtrace", EndTraceEndpoint())
        super().__init__(request, client_address, server)

    def respond(self, code: int, data: bytes, mime: str) -> None:
        self.send_response(code)
        self.send_header('Content-type', mime)
        self.add_standard_headers()
        self.wfile.write(data)

    def do_GET(self):
        self.router.process(RequestType.GET)

    def do_POST(self):
        self.router.process(RequestType.POST)

    def do_OPTIONS(self):
        self.send_response(HTTPStatus.OK)
        self.send_header('Allow', 'GET,POST')
        self.add_standard_headers()
        self.end_headers()
        self.wfile.write(b'GET,POST')

    def log_request(self, code='-', size='-'):
        log.info('{} {} {}'.format(self.requestline, str(code), str(size)))

    def add_standard_headers(self):
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers',
                        WINSCOPE_TOKEN_HEADER + ', Content-Type, Content-Length')
        self.send_header('Access-Control-Expose-Headers',
                        'Winscope-Proxy-Version')
        self.send_header(WINSCOPE_VERSION_HEADER, VERSION)
        self.end_headers()


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
