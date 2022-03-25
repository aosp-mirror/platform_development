from dataclasses import dataclass, asdict, field
import sqlite3
import time
import logging
import os
import zipfile
import re
import json


class BuildFileInvalidError(Exception):
    pass


@dataclass
class BuildInfo:
    """
    A class for Android build information.
    """
    file_name: str
    path: str
    time: int
    build_id: str = ''
    build_version: str = ''
    build_flavor: str = ''
    partitions: list[str] = field(default_factory=list)

    def analyse_buildprop(self):
        """
        Analyse the build's version info and partitions included
        Then write them into the build_info
        """
        def extract_info(pattern, lines):
            # Try to match a regex in a list of string
            line = list(filter(pattern.search, lines))[0]
            if line:
                return pattern.search(line).group(0)
            else:
                return ''

        with zipfile.ZipFile(self.path) as build:
            try:
                with build.open('SYSTEM/build.prop', 'r') as build_prop:
                    raw_info = build_prop.readlines()
                    pattern_id = re.compile(b'(?<=ro\.build\.id\=).+')
                    pattern_version = re.compile(
                        b'(?<=ro\.build\.version\.incremental\=).+')
                    pattern_flavor = re.compile(b'(?<=ro\.build\.flavor\=).+')
                    self.build_id = extract_info(
                        pattern_id, raw_info).decode('utf-8')
                    self.build_version = extract_info(
                        pattern_version, raw_info).decode('utf-8')
                    self.build_flavor = extract_info(
                        pattern_flavor, raw_info).decode('utf-8')
                with build.open('META/ab_partitions.txt', 'r') as partition_info:
                    raw_info = partition_info.readlines()
                    for line in raw_info:
                        self.partitions.append(line.decode('utf-8').rstrip())
            except KeyError as e:
                raise BuildFileInvalidError("Invalid build due to " + str(e))

    def to_sql_form_dict(self):
        """
        Because sqlite can only store text but self.partitions is a list
        Turn the list into a string joined by ',', for example:
        ['system', 'vendor'] => 'system,vendor'
        """
        sql_form_dict = asdict(self)
        sql_form_dict['partitions'] = ','.join(sql_form_dict['partitions'])
        return sql_form_dict

    def to_dict(self):
        """
        Return as a normal dict.
        """
        return asdict(self)


class TargetLib:
    """
    A class that manages the builds in database.
    """

    def __init__(self, working_dir="target", db_path=None):
        """
        Create a build table if not existing
        """
        self.working_dir = working_dir
        if db_path is None:
            db_path = os.path.join(working_dir, "ota_database.db")
        self.db_path = db_path
        with sqlite3.connect(self.db_path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
                CREATE TABLE if not exists Builds (
                FileName TEXT,
                UploadTime INTEGER,
                Path TEXT,
                BuildID TEXT,
                BuildVersion TEXT,
                BuildFlavor TEXT,
                Partitions TEXT
            )
            """)

    def new_build(self, filename, path):
        """
        Insert a new build into the database
        Args:
            filename: the name of the file
            path: the relative path of the file
        """
        build_info = BuildInfo(filename, path, int(time.time()))
        build_info.analyse_buildprop()
        # Ignore name specified by user, instead use a standard format
        build_info.path = os.path.join(self.working_dir, "{}-{}-{}.zip".format(
            build_info.build_flavor, build_info.build_id, build_info.build_version))
        if path != build_info.path:
            os.rename(path, build_info.path)
        with sqlite3.connect(self.db_path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
            SELECT * FROM Builds WHERE FileName=:file_name and Path=:path
            """, build_info.to_sql_form_dict())
            if cursor.fetchall():
                cursor.execute("""
                DELETE FROM Builds WHERE FileName=:file_name and Path=:path
                """, build_info.to_sql_form_dict())
            cursor.execute("""
            INSERT INTO Builds (FileName, UploadTime, Path, BuildID, BuildVersion, BuildFlavor, Partitions)
            VALUES (:file_name, :time, :path, :build_id, :build_version, :build_flavor, :partitions)
            """, build_info.to_sql_form_dict())

    def new_build_from_dir(self):
        """
        Update the database using files under a directory
        Args:
            path: a directory
        """
        build_dir = self.working_dir
        if os.path.isdir(build_dir):
            builds_name = os.listdir(build_dir)
            for build_name in builds_name:
                path = os.path.join(build_dir, build_name)
                if build_name.endswith(".zip") and zipfile.is_zipfile(path):
                    self.new_build(build_name, path)
        elif os.path.isfile(build_dir) and build_dir.endswith(".zip"):
            self.new_build(os.path.split(build_dir)[-1], build_dir)
        return self.get_builds()

    def sql_to_buildinfo(self, row):
        build_info = BuildInfo(*row[:6], row[6].split(','))
        return build_info

    def get_builds(self):
        """
        Get a list of builds in the database
        Return:
            A list of build_info, each of which is an object:
            (FileName, UploadTime, Path, Build ID, Build Version, Build Flavor, Partitions)
        """
        with sqlite3.connect(self.db_path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
            SELECT FileName, Path, UploadTime, BuildID, BuildVersion, BuildFlavor, Partitions
            FROM Builds""")
            return list(map(self.sql_to_buildinfo, cursor.fetchall()))

    def get_build_by_path(self, path):
        """
        Get a build in the database by its path
        Return:
            A build_info, which is an object:
            (FileName, UploadTime, Path, Build ID, Build Version, Build Flavor, Partitions)
        """
        with sqlite3.connect(self.db_path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
            SELECT FileName, Path, UploadTime, BuildID, BuildVersion, BuildFlavor, Partitions
            FROM Builds WHERE Path==(?)
            """, (path, ))
        return self.sql_to_buildinfo(cursor.fetchone())
