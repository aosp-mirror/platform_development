#!/usr/bin/env python3

import argparse
import fnmatch
import json
import os
import re
import sys

HELP_MSG = '''
This script analyses the usage of build-time variables which are defined in BoardConfig*.mk
and used by framework modules (installed in system.img). Please 'lunch' and 'make' before
running it.
'''

TOP = os.environ.get('ANDROID_BUILD_TOP')
OUT = os.environ.get('OUT')

white_list = [
    'TARGET_ARCH',
    'TARGET_ARCH_VARIANT',
    'TARGET_CPU_VARIANT',
    'TARGET_CPU_ABI',
    'TARGET_CPU_ABI2',

    'TARGET_2ND_ARCH',
    'TARGET_2ND_ARCH_VARIANT',
    'TARGET_2ND_CPU_VARIANT',
    'TARGET_2ND_CPU_ABI',
    'TARGET_2ND_CPU_ABI2',

    'TARGET_NO_BOOTLOADER',
    'TARGET_NO_KERNEL',
    'TARGET_NO_RADIOIMAGE',
    'TARGET_NO_RECOVERY',

    'TARGET_BOARD_PLATFORM',

    'ARCH_ARM_HAVE_ARMV7A',
    'ARCH_ARM_HAVE_NEON',
    'ARCH_ARM_HAVE_VFP',
    'ARCH_ARM_HAVE_VFP_D32',

    'BUILD_NUMBER'
]


# used by find_board_configs_mks() and find_makefiles()
def find_files(folders, filter):
  ret = []

  for folder in folders:
    for root, dirs, files in os.walk(os.path.join(TOP, folder), topdown=True):
      dirs[:] = [d for d in dirs if not d[0] == '.']
      for file in files:
        if filter(file):
          ret.append(os.path.join(root, file))

  return ret

# find board configs (BoardConfig*.mk)
def find_board_config_mks(folders = ['build', 'device', 'vendor', 'hardware']):
  return find_files(folders, lambda x:
                    fnmatch.fnmatch(x, 'BoardConfig*.mk'))

# find makefiles (*.mk or Makefile) under specific folders
def find_makefiles(folders = ['system', 'frameworks', 'external']):
  return find_files(folders, lambda x:
                    fnmatch.fnmatch(x, '*.mk') or fnmatch.fnmatch(x, 'Makefile'))

# read module-info.json and find makefiles of modules in system image
def find_system_module_makefiles():
  makefiles = []
  out_system_path = os.path.join(OUT[len(TOP) + 1:], 'system')

  with open(os.path.join(OUT, 'module-info.json')) as module_info_json:
    module_info = json.load(module_info_json)
    for module in module_info:
      installs = module_info[module]['installed']
      paths = module_info[module]['path']

      installed_in_system = False

      for install in installs:
        if install.startswith(out_system_path):
          installed_in_system = True
          break

      if installed_in_system:
        for path in paths:
          makefile = os.path.join(TOP, path, 'Android.mk')
          makefiles.append(makefile)

  return makefiles

# find variables defined in board_config_mks
def find_defined_variables(board_config_mks):
  re_def = re.compile('^[\s]*([\w\d_]*)[\s]*:=')
  variables = dict()

  for board_config_mk in board_config_mks:
    for line in open(board_config_mk, encoding='latin1'):
      mo = re_def.search(line)
      if mo is None:
        continue

      variable = mo.group(1)
      if variable in white_list:
        continue

      if variable not in variables:
        variables[variable] = set()

      variables[variable].add(board_config_mk[len(TOP) + 1:])

  return variables

# count variable usage in makefiles
def find_usage(variable, makefiles):
  re_usage = re.compile('\$\(' + variable + '\)')
  usage = set()

  for makefile in makefiles:
    if not os.path.isfile(makefile):
      # TODO: support bp
      continue

    with open(makefile, encoding='latin1') as mk_file:
      mk_str = mk_file.read()

    if re_usage.search(mk_str) is not None:
      usage.add(makefile[len(TOP) + 1:])

  return usage

def main():
  parser = argparse.ArgumentParser(description=HELP_MSG)
  parser.add_argument("-v", "--verbose",
                      help="print definition and usage locations",
                      action="store_true")
  args = parser.parse_args()

  print('TOP : ' + TOP)
  print('OUT : ' + OUT)
  print()

  sfe_makefiles = find_makefiles()
  system_module_makefiles = find_system_module_makefiles()
  board_config_mks = find_board_config_mks()
  variables = find_defined_variables(board_config_mks)

  if args.verbose:
    print('sfe_makefiles', len(sfe_makefiles))
    print('system_module_makefiles', len(system_module_makefiles))
    print('board_config_mks', len(board_config_mks))
    print('variables', len(variables))
    print()

  glossary = (
      '*Output in CSV format\n\n'

      '*definition count      :'
      ' This variable is defined in how many BoardConfig*.mk\'s\n'

      '*usage in SFE          :'
      ' This variable is used by how many makefiles under system/, frameworks/ and external/ folders\n'

      '*usage in system image :'
      ' This variable is used by how many system image modules\n')

  csv_string = (
      'variable name,definition count,usage in SFE,usage in system image\n')

  for variable, locations in sorted(variables.items()):
    usage_in_sfe = find_usage(variable, sfe_makefiles)
    usage_of_system_modules = find_usage(variable, system_module_makefiles)
    usage = usage_in_sfe | usage_of_system_modules

    if len(usage) == 0:
      continue

    csv_string += ','.join([variable,
                            str(len(locations)),
                            str(len(usage_in_sfe)),
                            str(len(usage_of_system_modules))]) + '\n'

    if args.verbose:
      print((variable + ' ').ljust(80, '='))

      print('Defined in (' + str(len(locations)) + ') :')
      for location in sorted(locations):
        print('  ' + location)

      print('Used in (' + str(len(usage)) + ') :')
      for location in sorted(usage):
        print('  ' + location)

      print()

  if args.verbose:
    print('\n')

  print(glossary)
  print(csv_string)

if __name__ == '__main__':
  if TOP is None:
    sys.exit('$ANDROID_BUILD_TOP is undefined, please lunch and make before running this script')

  if OUT is None:
    sys.exit('$OUT is undefined, please lunch and make before running this script')

  if not os.path.isfile(os.path.join(OUT, 'module-info.json')):
    sys.exit('module-info.json is missing, please lunch and make before running this script')

  main()

