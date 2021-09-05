import subprocess
import os
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

        def enforce_bool(t): return t if isinstance(t, bool) else bool(t)
        self.verbose, self.downgrade = map(
            enforce_bool,
            [self.verbose, self.downgrade])
        if self.incremental:
            self.isIncremental = True
        if self.partial:
            self.isPartial = True
        else:
            self.partial = []
        if type(self.partial) == str:
            self.partial = self.partial.split(',')

    def to_sql_form_dict(self):
        """
        Convert this instance to a dict, which can be later used to insert into
        the SQL database.
        Format:
            id: string, target: string, incremental: string, verbose: int,
            partial: string, output:string, status:string,
            downgrade: bool, extra: string, stdout: string, stderr:string,
            start_time:int, finish_time: int(not required)
        """
        sql_form_dict = asdict(self)
        sql_form_dict['partial'] = ','.join(sql_form_dict['partial'])
        def bool_to_int(t): return 1 if t else 0
        sql_form_dict['verbose'], sql_form_dict['downgrade'] = map(
            bool_to_int,
            [sql_form_dict['verbose'], sql_form_dict['downgrade']])
        return sql_form_dict

    def to_dict_basic(self):
        """
        Convert the instance to a dict, which includes the file name of target.
        """
        basic_info = asdict(self)
        basic_info['target_name'] = self.target.split('/')[-1]
        if self.isIncremental:
            basic_info['incremental_name'] = self.incremental.split('/')[-1]
        return basic_info

    def to_dict_detail(self, target_lib, offset=0):
        """
        Convert this instance into a dict, which includes some detailed information
        of the target/source build, i.e. build version and file name.
        """
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


class DependencyError(Exception):
    pass


class ProcessesManagement:
    """
    A class manage the ota generate process
    """

    @staticmethod
    def check_external_dependencies():
        try:
            java_version = subprocess.check_output(["java", "--version"])
            print("Java version:", java_version.decode())
        except Exception as e:
            raise DependencyError(
                "java not found in PATH. Attempt to generate OTA might fail. " + str(e))
        try:
            zip_version = subprocess.check_output(["zip", "-v"])
            print("Zip version:", zip_version.decode())
        except Exception as e:
            raise DependencyError(
                "zip command not found in PATH. Attempt to generate OTA might fail. " + str(e))

    def __init__(self, *, working_dir='output', db_path=None, otatools_dir=None):
        """
        create a table if not exist
        """
        ProcessesManagement.check_external_dependencies()
        self.working_dir = working_dir
        self.logs_dir = os.path.join(working_dir, 'logs')
        self.otatools_dir = otatools_dir
        os.makedirs(self.working_dir, exist_ok=True)
        os.makedirs(self.logs_dir, exist_ok=True)
        if not db_path:
            db_path = os.path.join(self.working_dir, "ota_database.db")
        self.path = db_path
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

    def insert_database(self, job_info):
        """
        Insert the job_info into the database
        Args:
            job_info: JobInfo
        """
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
                    INSERT INTO Jobs (ID, TargetPath, IncrementalPath, Verbose, Partial, OutputPath, Status, Downgrade, OtherFlags, STDOUT, STDERR, StartTime, Finishtime)
                    VALUES (:id, :target, :incremental, :verbose, :partial, :output, :status, :downgrade, :extra, :stdout, :stderr, :start_time, :finish_time)
                """, job_info.to_sql_form_dict())

    def get_status_by_ID(self, id):
        """
        Return the status of job <id> as a instance of JobInfo
        Args:
            id: string
        Return:
            JobInfo
        """
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            logging.info(id)
            cursor.execute("""
            SELECT ID, TargetPath, IncrementalPath, Verbose, Partial, OutputPath, Status, Downgrade, OtherFlags, STDOUT, STDERR, StartTime, FinishTime
            FROM Jobs WHERE ID=(?)
            """, (str(id),))
            row = cursor.fetchone()
        status = JobInfo(*row)
        return status

    def get_status(self):
        """
        Return the status of all jobs as a list of JobInfo
        Return:
            List[JobInfo]
        """
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
        """
        Change the status and finish time of job <id> in the database
        Args:
            id: string
            status: string
            finish_time: int
        """
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
                UPDATE Jobs SET Status=(?), FinishTime=(?)
                WHERE ID=(?)
                """,
                           (status, finish_time, id))

    def ota_run(self, command, id, stdout_path, stderr_path):
        """
        Initiate a subprocess to run the ota generation. Wait until it finished and update
        the record in the database.
        """
        stderr_pipes = pipes.Template()
        stdout_pipes = pipes.Template()
        ferr = stderr_pipes.open(stdout_path, 'w')
        fout = stdout_pipes.open(stderr_path, 'w')
        env = {}
        if self.otatools_dir:
            env['PATH'] = os.path.join(
                self.otatools_dir, "bin") + ":" + os.environ["PATH"]
        # TODO(lishutong): Enable user to use self-defined stderr/stdout path
        try:
            proc = subprocess.Popen(
                command, stderr=ferr, stdout=fout, shell=False, env=env, cwd=self.otatools_dir)
        except FileNotFoundError as e:
            logging.error('ota_from_target_files is not set properly %s', e)
            self.update_status(id, 'Error', int(time.time()))
            raise
        except Exception as e:
            logging.error('Failed to execute ota_from_target_files %s', e)
            self.update_status(id, 'Error', int(time.time()))
            raise

        def wait_result():
            exit_code = proc.wait()
            if exit_code == 0:
                self.update_status(id, 'Finished', int(time.time()))
            else:
                self.update_status(id, 'Error', int(time.time()))
        threading.Thread(target=wait_result).start()

    def ota_generate(self, args, id):
        """
        Read in the arguments from the frontend and start running the OTA
        generation process, then update the records in database.
        Format of args:
            output: string, extra_keys: List[string], extra: string,
            isIncremental: bool, isPartial: bool, partial: List[string],
            incremental: string, target: string, verbose: bool
        args:
            args: dict
            id: string
        """
        command = ['ota_from_target_files']
        # Check essential configuration is properly set
        if not os.path.isfile(args['target']):
            raise FileNotFoundError
        if not 'output' in args:
            args['output'] = os.path.join(self.working_dir, str(id) + '.zip')
        if args['verbose']:
            command.append('-v')
        if args['extra_keys']:
            args['extra'] = '--' + \
                ' --'.join(args['extra_keys']) + ' ' + args['extra']
        if args['extra']:
            command += args['extra'].strip().split(' ')
        if args['isIncremental']:
            if not os.path.isfile(args['incremental']):
                raise FileNotFoundError
            command.append('-i')
            command.append(os.path.realpath(args['incremental']))
        if args['isPartial']:
            command.append('--partial')
            command.append(' '.join(args['partial']))
        command.append(os.path.realpath(args['target']))
        command.append(os.path.realpath(args['output']))
        stdout = os.path.join(self.logs_dir, 'stdout.' + str(id))
        stderr = os.path.join(self.logs_dir, 'stderr.' + str(id))
        job_info = JobInfo(id,
                           target=args['target'],
                           incremental=args['incremental'] if args['isIncremental'] else '',
                           verbose=args['verbose'],
                           partial=args['partial'] if args['isPartial'] else [
                           ],
                           output=args['output'],
                           status='Running',
                           extra=args['extra'],
                           start_time=int(time.time()),
                           stdout=stdout,
                           stderr=stderr
                           )
        self.ota_run(command, id, job_info.stdout, job_info.stderr)
        self.insert_database(job_info)
        logging.info(
            'Starting generating OTA package with id {}: \n {}'
            .format(id, command))
