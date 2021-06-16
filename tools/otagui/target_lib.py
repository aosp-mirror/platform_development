from dataclasses import dataclass, asdict, field
import sqlite3
import time
import logging
import os
import zipfile
import re
import json


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

        build = zipfile.ZipFile(self.path)
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
        except KeyError:
            pass
        try:
            with build.open('META/ab_partitions.txt', 'r') as partition_info:
                raw_info = partition_info.readlines()
                for line in raw_info:
                    self.partitions.append(line.decode('utf-8').rstrip())
        except KeyError:
            pass

    def to_sql_form_dict(self):
        sql_form_dict = asdict(self)
        sql_form_dict['partitions'] = ','.join(sql_form_dict['partitions'])
        return sql_form_dict

    def to_dict(self):
        return asdict(self)


class TargetLib:
    def __init__(self, path='ota_database.db'):
        """
        Create a build table if not existing
        """
        self.path = path
        with sqlite3.connect(self.path) as connect:
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
        with sqlite3.connect(self.path) as connect:
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

    def new_build_from_dir(self, path):
        """
        Update the database using files under a directory
        Args:
            path: a directory
        """
        if os.path.isdir(path):
            builds_name = os.listdir(path)
            for build_name in builds_name:
                self.new_build(build_name, os.path.join(path, build_name))
        elif os.path.isfile(path):
            self.new_build(os.path.split(path)[-1], path)
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
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
            SELECT FileName, Path, UploadTime, BuildID, BuildVersion, BuildFlavor, Partitions
            FROM Builds""")
            return list(map(self.sql_to_buildinfo, cursor.fetchall()))

    def get_builds_by_path(self, path):
        """
        Get a build in the database by its path
        Return:
            A build_info, which is an object:
            (FileName, UploadTime, Path, Build ID, Build Version, Build Flavor, Partitions)
        """
        with sqlite3.connect(self.path) as connect:
            cursor = connect.cursor()
            cursor.execute("""
            SELECT FileName, Path, UploadTime, BuildID, BuildVersion, BuildFlavor, Partitions
            WHERE Path==(?)
            """, (path, ))
        return self.sql_to_buildinfo(cursor.fetchone())
