#!/usr/bin/env python3

import json
import os

data_path = 'data.json'
pattern_path = 'patterns'

def remove_all_data(index_path):
    os.remove(index_path)
    os.remove(data_path)
    os.remove(pattern_path)

def data_exist():
    return os.path.exists(data_path)

def save_data(data):
    with open(data_path, 'w') as f:
        json.dump(data, f, sort_keys=True, indent=4)

def remove_data():
    for path in (data_path, pattern_path):
        try:
            os.remove(path)
        except IOError:
            pass

def load_data():
    with open(data_path, 'r') as f:
        ret = json.load(f)
    return ret

def merge(old_data, data):
    for key, item in data.items():
        try:
            prev_result = old_data[key]
            data[key] = prev_result
        except KeyError:
            pass
    return data

def patterns_exist():
    return os.path.exists(pattern_path)

# init the first pattern
def init_pattern(patt, is_regex):
    with open(pattern_path, 'w') as f:
        f.write(str(int(is_regex)) + ',' + patt + '\n')

def load_pattern():
    with open(pattern_path, 'r') as f:
        patterns = []
        is_regexs = []
        for line in f:
            line = line.rstrip('\n')
            sp = line.split(',')
            is_regexs.append(sp[0])
            patterns.append(','.join(sp[1:]))
    return patterns, is_regexs

# save new added pattern
def save_new_pattern(patt, is_regex):
    with open(pattern_path, 'a') as f:
        f.write(str(int(is_regex)) + ',' + patt + '\n')
