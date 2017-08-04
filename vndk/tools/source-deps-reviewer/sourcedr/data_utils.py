#!/usr/bin/env python3

import json
import os

data_path = 'data.json'
pattern_path = 'patterns'

def data_exist():
    return os.path.exists(data_path) and os.path.exists(pattern_path)

def load_data():
    with open(data_path, 'r') as f:
        ret = json.load(f)
    return ret

def load_pattern():
    with open(pattern_path, 'r') as f:
        ret = [line for line in f]
    return ret

# save new added pattern
def save_pattern(patt):
    with open(pattern_path, 'a') as f:
        f.write(patt + '\n')

def init_pattern(patt):
    with open(pattern_path, 'w') as f:
        f.write(patt + '\n')

def save_data(data):
    with open(data_path, 'w') as f:
        json.dump(data, f, sort_keys=True, indent=4)

def remove_data():
    for path in (data_path, pattern_path):
        try:
            os.remove(path)
        except IOError:
            pass

