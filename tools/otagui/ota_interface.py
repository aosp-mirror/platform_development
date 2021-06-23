import subprocess
import os
import json
import pipes
import threading
from dataclasses import dataclass, asdict, field
import logging
import sqlite3
import time


@dataclass
class JobInfo:
    """
    A class for ota job information
    """
    id: str
    target: str
    incremental: str = ''
    verbose: bool = False
    partial: list[str] = field(default_factory=list)
    output: str = ''
    status: str = 'Running'
    downgrade: bool = False
    extra: str = ''
    stdout: str = ''
    stderr: str = ''
    start_time: int = 0
    finish_time: int = 0
    isPartial: bool = False
    isIncremental: bool = False

    def __post_init__(self):
        if not self.output:
            self.output = os.path.join('output', self.id, '.zip')
        if not self.stdout:
            self.stdout = os.path.join('output/stdout.'+self.id)
        if not self.stderr:
            self.stderr = os.path.join('output/stderr.'+self.id)

        def enforce_bool(t): return t if isinstance(t, bool) else bool(t)
        self.verbose, self.downgrade = map(
            enforce_bool,
            [self.verbose, self.downgrade])
        if self.incremental:
            self.isIncremental = True
        if self.partial:
            self.isPartial = True

    def to_sql_form_dict(self):
        sql_form_dict = asdict(self)
        sql_form_dict['partial'] = ','.join(sql_form_dict['partial'])
        def bool_to_int(t): return 1 if t else 0
        sql_form_dict['verbose'], sql_form_dict['downgrade'] = map(
            bool_to_int,
            [sql_form_dict['verbose'], sql_form_dict['downgrade']])
        return sql_form_dict

    def to_dict_basic(self):
        basic_info = asdict(self)
        basic_info['target_name'] = self.target.split('/')[-1]
        if self.isIncremental:
            basic_info['incremental_name'] = self.incremental.split('/')[-1]
        return basic_info

    def to_dict_detail(self, target_lib, offset=0):
        detail_info = asdict(self)
        try:
            with open(self.stdout, 'r') as fout:
                detail_info['stdout'] = fout.read()
            with open(self.stderr, 'r') as ferr:
                detail_info['stderr'] = ferr.read()
        except FileNotFoundError:
            detail_info['stdout'] = 'NO STD OUTPUT IS FOUND'
            detail_info['stderr'] = 'NO STD ERROR IS FOUND'
        target_info = target_lib.get_build_by_path(self.target)
        detail_info['target_name'] = target_info.file_name
        detail_info['target_build_version'] = target_info.build_version
        if self.incremental:
            incremental_info = target_lib.get_build_by_path(
                self.incremental)
            detail_info['incremental_name'] = incremental_info.file_name
            detail_info['incremental_build_version'] = incremental_info.build_version
        return detail_info


class ProcessesManagement:
    """
    A class manage the ota generate process
    """

    def __init__(self, path='ota_database.db'):
        """
        create a table if not exist
        """
        self.path = path
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
                CREATE TABLE if not exists Jobs (
                ID TEXT,
                TargetPath TEXT,
                IncrementalPath TEXT,
                Verbose INTEGER,
                Partial TEXT,
                OutputPath TEXT,
                Status TEXT,
                Downgrade INTEGER,
                OtherFlags TEXT,
                STDOUT TEXT,
                STDERR TEXT,
                StartTime INTEGER,
                FinishTime INTEGER
            )
            """)

    def get_status_by_ID(self, id):
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            logging.info(id)
            cursor.execute("""
            SELECT ID, TargetPath, IncrementalPath, Verbose, Partial, OutputPath, Status, Downgrade, OtherFlags, STDOUT, STDERR, StartTime, FinishTime
            FROM Jobs WHERE ID=(?)
            """, (id,))
            row = cursor.fetchone()
        status = JobInfo(*row)
        return status

    def get_status(self):
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
            SELECT ID, TargetPath, IncrementalPath, Verbose, Partial, OutputPath, Status, Downgrade, OtherFlags, STDOUT, STDERR, StartTime, FinishTime
            FROM Jobs
            """)
            rows = cursor.fetchall()
        statuses = [JobInfo(*row) for row in rows]
        return statuses

    def update_status(self, id, status, finish_time):
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
                UPDATE Jobs SET Status=(?), FinishTime=(?)
                WHERE ID=(?)
                """,
                           (status, finish_time, id))

    def ota_run(self, command, id):
        # Start a subprocess and collect the output
        stderr_pipes = pipes.Template()
        stdout_pipes = pipes.Template()
        ferr = stderr_pipes.open(os.path.join(
            'output', 'stderr.'+str(id)), 'w')
        fout = stdout_pipes.open(os.path.join(
            'output', 'stdout.'+str(id)), 'w')
        try:
            proc = subprocess.Popen(
                command, stderr=ferr, stdout=fout)
        except FileNotFoundError:
            logging.error('ota_from_target_files is not set properly')
            self.update_status(id, 'Error', int(time.time()))
            return
        exit_code = proc.wait()
        if exit_code == 0:
            self.update_status(id, 'Finished', int(time.time()))
        else:
            self.update_status(id, 'Error', int(time.time()))

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
        job_info = JobInfo(id,
                           target=args['target'],
                           incremental=args['incremental'] if args['isIncremental'] else '',
                           verbose=args['verbose'],
                           partial=args['partial'].split(
                               ' ') if args['isPartial'] else [],
                           output=args['output'],
                           status='Running',
                           extra=args['extra'],
                           start_time=int(time.time())
                           )
        try:
            thread = threading.Thread(target=self.ota_run, args=(command, id))
            with sqlite3.connect(self.path) as connect:
                cursor = connect.cursor()
                cursor.execute("""
                    INSERT INTO Jobs (ID, TargetPath, IncrementalPath, Verbose, Partial, OutputPath, Status, Downgrade, OtherFlags, STDOUT, STDERR, StartTime)
                    VALUES (:id, :target, :incremental, :verbose, :partial, :output, :status, :downgrade, :extra, :stdout, :stderr, :start_time)
                """, job_info.to_sql_form_dict())
            thread.start()
        except AssertionError:
            raise SyntaxError
        logging.info(
            'Starting generating OTA package with id {}: \n {}'
            .format(id, command))
