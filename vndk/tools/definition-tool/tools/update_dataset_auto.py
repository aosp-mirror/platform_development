#!/usr/bin/env python3

# This tool generates `eligible-list-${ver}.csv` and
# `eligible-list-${ver}-properties.csv`.

import argparse
import os
import subprocess
import sys
import tempfile

UPDATE_DATASET = os.path.abspath(os.path.join(
    __file__, '..', 'update_dataset.py'))

LIST_VNDK_MODULE = os.path.abspath(os.path.join(
    __file__, '..', '..', '..', 'sourcedr', 'blueprint', 'list_vndk_module.py'))


def update_eligible_list(path, make_vars, module_info):
    dirname, basename = os.path.split(path)
    tmp_fd, tmp_path = tempfile.mkstemp(prefix=basename + '-', dir=dirname)
    os.close(tmp_fd)

    cmd = [sys.executable, UPDATE_DATASET]
    cmd.extend(['--make-vars', make_vars])
    cmd.extend(['--module-info', module_info])
    cmd.extend(['-o', tmp_path])
    cmd.append(path)

    print('command:', ' '.join(cmd))

    subprocess.check_call(cmd)
    os.rename(tmp_path, path)


def update_eligible_list_properties(path, build_top):
    dirname, basename = os.path.split(path)
    tmp_fd, tmp_path = tempfile.mkstemp(prefix=basename + '-', dir=dirname)
    os.close(tmp_fd)

    cmd = [sys.executable, LIST_VNDK_MODULE]
    cmd.extend(['--exclude', '(?:device/)|(?:vendor/)'])
    cmd.extend(['--namespace', 'hardware/google/av'])
    cmd.extend(['--namespace', 'hardware/google/interfaces'])
    cmd.extend(['-o', tmp_path])
    cmd.append(os.path.join(build_top, 'Android.bp'))

    print('command:', ' '.join(cmd))

    subprocess.check_call(cmd)
    os.rename(tmp_path, path)


def _get_eligible_list_properties_path(eligible_list_path):
    root, ext = os.path.splitext(eligible_list_path)
    return root + '-properties' + ext


def _parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('eligible_list',
                        help='Path to eligible list to be updated')
    return parser.parse_args()


def main():
    args = _parse_args()

    # Read Android product environment variables.
    try:
        build_top = os.environ['ANDROID_BUILD_TOP']
        product = os.environ['TARGET_PRODUCT']
        product_out = os.environ['ANDROID_PRODUCT_OUT']
    except KeyError as e:
        print('error: Failed to read the environment variable', e.args[0],
              file=sys.stderr)
        print('error: Did you lunch an Android target?', file=sys.stderr)
        sys.exit(1)

    print('----------------------------------------')
    print('build_top:', build_top)
    print('product:', product)
    print('product_out:', product_out)

    out_dir = os.path.normpath(os.path.join(product_out, '..', '..', '..'))
    make_vars = os.path.join(out_dir, 'soong', 'make_vars-' + product + '.mk')
    module_info = os.path.join(product_out, 'module-info.json')

    print('----------------------------------------')
    print('make_vars:', make_vars)
    print('module_info:', module_info)
    print('----------------------------------------')

    # Run the commands to update the files.
    update_eligible_list(args.eligible_list, make_vars, module_info)
    update_eligible_list_properties(
        _get_eligible_list_properties_path(args.eligible_list), build_top)


if __name__ == '__main__':
    sys.exit(main())
