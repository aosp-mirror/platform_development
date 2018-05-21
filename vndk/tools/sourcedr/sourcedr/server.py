#!/usr/bin/env python3

import collections
import functools
import json
import os
import re

from flask import (
    Blueprint, Flask, current_app, jsonify, render_template, request)


codereview = Blueprint('codereview', '__name__', 'templates')


# whether the code segment is exactly in file
def same(fl, code, source_dir):
    fl = os.path.join(source_dir, fl)
    with open(fl, 'r') as f:
        fc = f.read()
        return code in fc


# check if the file needes to be reiewed again
def check(codes, source_dir):
    ret = []
    for item in codes:
        fl = item.split(':')[0]
        code = item[len(fl) + 1:]
        ret.append(same(fl, code, source_dir))
    return ret


@codereview.route('/get_started')
def _get_started():
    project = current_app.config.project
    source_dir = project.source_dir
    review_db = project.review_db

    lst, done= [], []
    for key, item in sorted(review_db.data.items()):
        lst.append(key)
        if item[0]:
            done.append(all(check(item[1], source_dir)))
        else:
            done.append(False)

    pattern_lst = project.pattern_db.load()[0]
    abs_path = os.path.abspath(source_dir)

    return jsonify(lst=json.dumps(lst),
                   done=json.dumps(done),
                   pattern_lst=json.dumps(pattern_lst),
                   path_prefix=os.path.join(abs_path, ''))


@codereview.route('/load_file')
def _load_file():
    project = current_app.config.project
    source_dir = project.source_dir
    review_db = project.review_db

    path = request.args.get('path')

    if path not in review_db.data.keys():
        print('No such entry', path)
        return jsonify(result='')
    deps, codes = review_db.data[path]

    return jsonify(deps=json.dumps(deps), codes=json.dumps(codes),
                   okays=json.dumps(check(codes, source_dir)))


@codereview.route('/get_file')
def _get_file():
    path = request.args.get('path')
    path = os.path.join(current_app.config.project.source_dir, path)

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

    project = current_app.config.project
    review_db = project.review_db
    review_db.add_label(label, deps, codes)

    return jsonify(result='done')


# This function add pattern to grep
@codereview.route('/add_pattern')
def _add_pattern():
    patt = request.args.get('pattern')
    is_regex = request.args.get('is_regex')
    engine = current_app.config.project.review_db
    engine.add_pattern(patt, is_regex)

    project = current_app.config.project
    project.pattern_db.save_new_pattern(patt, is_regex)
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
    return app
