"""
A local HTTP server for Android OTA package generation.
Based on OTA_from_target_files.py

Usage::
  python ./web_server.py [<port>]

API::
  GET /check : check the status of all jobs
  GET /check/<id> : check the status of the job with <id>
  GET /file : fetch the target file list
  GET /file/<path> : Add build file(s) in <path>, and return the target file list
  GET /download/<id> : download the ota package with <id>
  POST /run/<id> : submit a job with <id>,
                 arguments set in a json uploaded together
  POST /file/<filename> : upload a target file
  [TODO] POST /cancel/<id> : cancel a job with <id>

TODO:
  - Avoid unintentionally path leakage
  - Avoid overwriting build when uploading build with same file name

Other GET request will be redirected to the static request under 'dist' directory
"""

from http.server import BaseHTTPRequestHandler, SimpleHTTPRequestHandler, HTTPServer
from socketserver import ThreadingMixIn
from threading import Lock
from ota_interface import ProcessesManagement
from target_lib import TargetLib
import logging
import json
import cgi
import os
import stat
import zipfile

LOCAL_ADDRESS = '0.0.0.0'


class CORSSimpleHTTPHandler(SimpleHTTPRequestHandler):
    def end_headers(self):
        try:
            origin_address, _ = cgi.parse_header(self.headers['Origin'])
            self.send_header('Access-Control-Allow-Credentials', 'true')
            self.send_header('Access-Control-Allow-Origin', origin_address)
        except TypeError:
            pass
        super().end_headers()


class RequestHandler(CORSSimpleHTTPHandler):
    def _set_response(self, code=200, type='text/html'):
        self.send_response(code)
        self.send_header('Content-type', type)
        self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header("Access-Control-Allow-Headers", "X-Requested-With")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        if self.path == '/check' or self.path == '/check/':
            statuses = jobs.get_status()
            self._set_response(type='application/json')
            self.wfile.write(
                json.dumps([status.to_dict_basic()
                            for status in statuses]).encode()
            )
        elif self.path.startswith('/check/'):
            id = self.path[7:]
            status = jobs.get_status_by_ID(id=id)
            self._set_response(type='application/json')
            self.wfile.write(
                json.dumps(status.to_dict_detail(target_lib)).encode()
            )
        elif self.path.startswith('/file') or self.path.startswith("/reconstruct_build_list"):
            if self.path == '/file' or self.path == '/file/':
                file_list = target_lib.get_builds()
            else:
                file_list = target_lib.new_build_from_dir()
            builds_info = [build.to_dict() for build in file_list]
            self._set_response(type='application/json')
            self.wfile.write(
                json.dumps(builds_info).encode()
            )
            logging.debug(
                "GET request:\nPath:%s\nHeaders:\n%s\nBody:\n%s\n",
                str(self.path), str(self.headers), file_list
            )
            return
        elif self.path.startswith('/download'):
            self.path = self.path[10:]
            return CORSSimpleHTTPHandler.do_GET(self)
        else:
            if not os.path.exists('dist' + self.path):
                logging.info('redirect to dist')
                self.path = '/dist/'
            else:
                self.path = '/dist' + self.path
            return CORSSimpleHTTPHandler.do_GET(self)

    def do_POST(self):
        if self.path.startswith('/run'):
            content_type, _ = cgi.parse_header(self.headers['content-type'])
            if content_type != 'application/json':
                self.send_response(400)
                self.end_headers()
                return
            content_length = int(self.headers['Content-Length'])
            post_data = json.loads(self.rfile.read(content_length))
            try:
                jobs.ota_generate(post_data, id=str(self.path[5:]))
                self._set_response(code=200)
                self.send_header("Content-Type", 'application/json')
                self.wfile.write(json.dumps(
                    {"success": True, "msg": "OTA Generator started running"}).encode())
            except Exception as e:
                logging.warning(
                    "Failed to run ota_from_target_files %s", e.__traceback__)
                self.send_error(
                    400, "Failed to run ota_from_target_files", str(e))
            logging.debug(
                "POST request:\nPath:%s\nHeaders:\n%s\nBody:\n%s\n",
                str(self.path), str(self.headers),
                json.dumps(post_data)
            )
        elif self.path.startswith('/file'):
            file_name = os.path.join('target', self.path[6:])
            file_length = int(self.headers['Content-Length'])
            with open(file_name, 'wb') as output_file:
                # Unwrap the uploaded file first (due to the usage of FormData)
                # The wrapper has a boundary line at the top and bottom
                # and some file information in the beginning
                # There are a file content line, a file name line, and an empty line
                # The boundary line in the bottom is 4 bytes longer than the top one
                # Please refer to the following links for more details:
                # https://stackoverflow.com/questions/8659808/how-does-http-file-upload-work
                # https://datatracker.ietf.org/doc/html/rfc1867
                upper_boundary = self.rfile.readline()
                file_length -= len(upper_boundary) * 2 + 4
                file_length -= len(self.rfile.readline())
                file_length -= len(self.rfile.readline())
                file_length -= len(self.rfile.readline())
                BUFFER_SIZE = 1024*1024
                for offset in range(0, file_length, BUFFER_SIZE):
                    chunk = self.rfile.read(
                        min(file_length-offset, BUFFER_SIZE))
                    output_file.write(chunk)
                target_lib.new_build(self.path[6:], file_name)
            self._set_response(code=201)
            self.wfile.write(
                "File received, saved into {}".format(
                    file_name).encode('utf-8')
            )
        else:
            self.send_error(400)


class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    pass


def run_server(SeverClass=ThreadedHTTPServer, HandlerClass=RequestHandler, port=8000):
    server_address = (LOCAL_ADDRESS, port)
    server_instance = SeverClass(server_address, HandlerClass)
    try:
        logging.info(
            'Server is on, address:\n %s',
            'http://' + str(server_address[0]) + ':' + str(port))
        server_instance.serve_forever()
    except KeyboardInterrupt:
        pass
    server_instance.server_close()
    logging.info('Server has been turned off.')


if __name__ == '__main__':
    from sys import argv
    print(argv)
    logging.basicConfig(level=logging.INFO)
    EXTRACT_DIR = None
    if os.path.exists("otatools.zip"):
        logging.info("Found otatools.zip, extracting...")
        EXTRACT_DIR = "/tmp/otatools-" + str(os.getpid())
        os.makedirs(EXTRACT_DIR, exist_ok=True)
        with zipfile.ZipFile("otatools.zip", "r") as zfp:
            zfp.extractall(EXTRACT_DIR)
        # mark all binaries executable by owner
        bin_dir = os.path.join(EXTRACT_DIR, "bin")
        for filename in os.listdir(bin_dir):
            os.chmod(os.path.join(bin_dir, filename), stat.S_IRWXU)
        logging.info("Extracted otatools to {}".format(EXTRACT_DIR))
    if not os.path.isdir('target'):
        os.mkdir('target', 755)
    if not os.path.isdir('output'):
        os.mkdir('output', 755)
    target_lib = TargetLib()
    jobs = ProcessesManagement(otatools_dir=EXTRACT_DIR)
    if len(argv) == 2:
        run_server(port=int(argv[1]))
    else:
        run_server()
