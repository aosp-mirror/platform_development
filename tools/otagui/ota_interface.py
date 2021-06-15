import subprocess
import os
import json
import pipes
from threading import Lock
import logging

class ProcessesManagement:
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

    def get_status_by_ID(self, id=0, details=False):
        status = {}
        if not id in self.get_keys():
            return '{}'
        else:
            status['id'] = id
            if self.get(id).poll() == None:
                status['status'] = 'Running'
            elif self.get(id).poll() == 0:
                status['status'] = 'Finished'
                status['path'] = os.path.join('output', str(id) + '.zip')
            else:
                status['status'] = 'Error'
            try:
                if details:
                    with open(os.path.join('output', 'stdout.' + str(id)), 'r') as fout:
                        status['stdout'] = fout.read()
                    with open(os.path.join('output', 'stderr.' + str(id)), 'r') as ferr:
                        status['stderr'] = ferr.read()
            except FileNotFoundError:
                status['stdout'] = 'NO STD OUTPUT IS FOUND'
                status['stderr'] = 'NO STD OUTPUT IS FOUND'
            return status

    def get_status(self):
        return [self.get_status_by_ID(id=id) for id in self.get_keys()]

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
        if args['isIncremental']:
            if not os.path.isfile(args['incremental']):
                raise FileNotFoundError
            command.append('-i')
            command.append(args['incremental'])
        if args['isPartial']:
            command.append('--partial')
            command.append(args['partial'])
        command.append(args['target'])
        command.append(args['output'])
        # Start a subprocess and collect the output
        stderr_pipes = pipes.Template()
        stdout_pipes = pipes.Template()
        ferr = stderr_pipes.open(os.path.join(
            'output', 'stderr.'+str(id)), 'w')
        fout = stdout_pipes.open(os.path.join(
            'output', 'stdout.'+str(id)), 'w')
        self.set(id, subprocess.Popen(
            command, stderr=ferr, stdout=fout))
        logging.info(
            'Starting generating OTA package with id {}: \n {}'
            .format(id, command))