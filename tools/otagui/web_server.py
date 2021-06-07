"""
A simple local HTTP server for Android OTA package generation.
Based on OTA_from_target_files.

Usage::
  python ./web_server.py [<port>]
API::
  GET /check : check the status of all jobs
  [TODO] GET /check/id : check the status of the job with <id>
  POST /run/id : submit a job with <id>,
                 arguments set in a json uploaded together
  [TODO] POST /cancel/id : cancel a job with <id>
"""

from http.server import BaseHTTPRequestHandler, HTTPServer
from socketserver import ThreadingMixIn
from threading import Lock
import logging
import json
import pipes
import cgi
import subprocess
import os

LOCAL_ADDRESS = '0.0.0.0'


class ThreadSafeContainer:
    def __init__(self):
        self.__container = {}
        self.__lock = Lock()

    def set(self, name, value):
        with self.__lock:
            self.__container[name] = value

    def get(self, name):
        with self.__lock:
            return self.__container[name]

    def get_keys(self):
        with self.__lock:
            return self.__container.keys()


class RequestHandler(BaseHTTPRequestHandler):
    def get_status(self):
        statusList = []
        for id in PROCESSES.get_keys():
            status = {}
            status['id'] = id
            if PROCESSES.get(id).poll() == None:
                status['status'] = 'Running'
            elif PROCESSES.get(id).poll() == 0:
                status['status'] = 'Finished'
            else:
                status['status'] = 'Error'
            statusList.append(json.dumps(status))
        return '['+','.join(statusList)+']'

    def ota_generate(self, args, id=0):
        command = ['ota_from_target_files']
        # Check essential configuration is properly set
        if not os.path.isfile(args['target']):
            raise FileNotFoundError
        if not args['output']:
            raise SyntaxError
        if args['verbose']:
            command.append('-v')
        command.append('-k')
        command.append(
            '../../../build/make/target/product/security/testkey')
        if args['incremental']:
            command.append('-i')
            command.append(args['incremental'])
        command.append(args['target'])
        command.append(args['output'])
        stderr_pipes = pipes.Template()
        stdout_pipes = pipes.Template()
        ferr = stderr_pipes.open('stderr', 'w')
        fout = stdout_pipes.open('stdout', 'w')
        PROCESSES.set(id, subprocess.Popen(
            command, stderr=ferr, stdout=fout))
        logging.info(
            'Starting generating OTA package with id {}: \n {}'
            .format(id, command))

    def _set_response(self, type='text/html'):
        self.send_response(200)
        self.send_header('Content-type', type)
        try:
            origin_address, _ = cgi.parse_header(self.headers['Origin'])
            self.send_header('Access-Control-Allow-Credentials', 'true')
            self.send_header('Access-Control-Allow-Origin', origin_address)
        except TypeError:
            pass
        self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header("Access-Control-Allow-Headers", "X-Requested-With")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        if str(self.path) == '/check':
            status = self.get_status()
            self._set_response('application/json')
            self.wfile.write(
                status.encode()
            )
        else:
            self.send_error(404)
            return
        logging.info(
            "GET request:\nPath:%s\nHeaders:\n%s\nBody:\n%s\n",
            str(self.path), str(self.headers), status
        )

    def do_POST(self):
        content_type, _ = cgi.parse_header(self.headers['content-type'])
        if content_type != 'application/json':
            self.send_response(400)
            self.end_headers()
            return
        content_length = int(self.headers['Content-Length'])
        post_data = json.loads(self.rfile.read(content_length))
        if str(self.path)[:4] == '/run':
            try:
                self.ota_generate(post_data, id=str(self.path[5:]))
                self._set_response()
                self.wfile.write(
                    "ota generator start running".encode('utf-8'))
            except SyntaxError:
                self.send_error(400)
        else:
            self.send_error(400)
        logging.info(
            "POST request:\nPath:%s\nHeaders:\n%s\nBody:\n%s\n",
            str(self.path), str(self.headers),
            json.dumps(post_data)
        )


class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    pass


def run_server(SeverClass=ThreadedHTTPServer, HandlerClass=RequestHandler, port=8000):
    logging.basicConfig(level=logging.DEBUG)
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
    PROCESSES = ThreadSafeContainer()
    if len(argv) == 2:
        run_server(port=int(argv[1]))
    else:
        run_server()
