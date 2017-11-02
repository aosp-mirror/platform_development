#!/usr/bin/env python3

import argparse
import collections
import functools
import json
import os
import re

from flask import (
    Blueprint, Flask, current_app, jsonify, render_template, request)

from sourcedr.data_utils import (
    load_data, load_pattern, save_data, save_new_pattern)
from sourcedr.project import Project


codereview = Blueprint('codereview', '__name__', 'templates')


# whether the code segment is exactly in file
def same(fl, code, android_root):
    fl = os.path.join(android_root, fl)
    with open(fl, 'r') as f:
        fc = f.read()
        return code in fc


# check if the file needes to be reiewed again
def check(codes, android_root):
    ret = []
    for item in codes:
        fl = item.split(':')[0]
        code = item[len(fl) + 1:]
        ret.append(same(fl, code, android_root))
    return ret


@codereview.route('/get_started')
def _get_started():
    android_root = current_app.config.project.android_root
    lst, done= [], []
    for key, item in sorted(data.items()):
        lst.append(key)
        if item[0]:
            done.append(all(check(item[1], android_root)))
        else:
            done.append(False)

    pattern_lst = load_pattern()[0]
    abs_path = os.path.abspath(current_app.config.project.android_root)

    return jsonify(lst=json.dumps(lst),
                   done=json.dumps(done),
                   pattern_lst=json.dumps(pattern_lst),
                   path_prefix=os.path.join(abs_path, ''))


@codereview.route('/load_file')
def _load_file():
    android_root = current_app.config.project.android_root
    path = request.args.get('path')

    if path not in data.keys():
        print('No such entry', path)
        return jsonify(result='')
    deps, codes = data[path]

    return jsonify(deps=json.dumps(deps), codes=json.dumps(codes),
                   okays=json.dumps(check(codes, android_root)))


@codereview.route('/get_file')
def _get_file():
    path = request.args.get('path')
    path = os.path.join(current_app.config.project.android_root, path)

    if not os.path.exists(path):
        return jsonify(result='No such file')
    with open(path, 'r') as f:
        code = f.read()

    return jsonify(result=code)


@codereview.route('/save_all')
def _save_all():
    label = request.args.get('label')
    deps = json.loads(request.args.get('deps'))
    codes = json.loads(request.args.get('codes'))
    data[label] = (deps, codes)
    # save update to file
    save_data(data)
    return jsonify(result='done')


# This function add pattern to grep
@codereview.route('/add_pattern')
def _add_pattern():
    patt = request.args.get('pattern')
    is_regex = request.args.get('is_regex')
    engine = current_app.config.project.review_db
    engine.add_pattern(patt, is_regex)
    # update the data
    global data
    data = load_data()
    save_new_pattern(patt, is_regex)
    return jsonify(result='done')


# This function does a temporary grep to the directory
# Not adding the result to database
@codereview.route('/temporary_search')
def _temporary_search():
    path = request.args.get('path')
    patt = request.args.get('pattern')
    is_regex = request.args.get('is_regex')
    codesearch = current_app.config.project.codesearch
    result = codesearch.raw_search(patt, is_regex).decode('utf-8')
    dic = collections.defaultdict(list)
    patt = re.compile('([^:]+):(\\d+):(.*)$')
    for line in result.split('\n'):
        match = patt.match(line)
        if not match:
            continue

        file_path = match.group(1)
        line_no = match.group(2)
        code = match.group(3)
        dic[file_path].append((line_no, code))

    def compare(item1, item2):
        key1, value1 = item1
        key2, value2 = item2
        cnt1 = os.path.commonprefix([path, key1]).count('/')
        cnt2 = os.path.commonprefix([path, key2]).count('/')
        e1 = os.path.relpath(key1, path).count('/')
        e2 = os.path.relpath(key2, path).count('/')
        # prefer smaller edit distance
        if e1 < e2: return -1
        if e2 < e1: return 1
        # prefer deeper common ancestor
        if cnt1 > cnt2: return -1
        if cnt2 > cnt1: return 1
        # lexicographical order
        if key1 < key2: return -1
        if key2 < key1: return 1
        return 0

    result = sorted(dic.items(), key=functools.cmp_to_key(compare))
    return jsonify(result=json.dumps(result))


@codereview.route('/')
def render():
    return render_template('index.html')


def create_app(project):
    app = Flask(__name__)
    app.register_blueprint(codereview)
    app.config.project = project

    @app.before_first_request
    def _run_on_start():
        # load data first for better efficiency
        global data
        data = load_data()

    return app


def _parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('android_root')
    parser.add_argument('--project-dir', default='.sourcedr_data')
    parser.add_argument('--rebuild-csearch-index', action='store_true',
                        help='Re-build the existing csearch index file')
    return parser.parse_args()


def main():
    args = _parse_args()

    project = Project(os.path.expanduser(args.android_root),
                      os.path.expanduser(args.project_dir))
    project.update_csearch_index(args.rebuild_csearch_index)
    project.update_review_db()

    app = create_app(project)
    app.run()

if __name__=='__main__':
    main()
