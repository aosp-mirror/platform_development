#!/usr/bin/python

import commands
import sys


def run_command(command):
  return_code, output = commands.getstatusoutput(command)
  if return_code != 0:
    raise ValueError("Failed to execute command: %s" % command)
  return output


def list_key_ids_for_service_account(service_account):
  return parse_list_key_output(
    run_command("gcloud iam service-accounts keys list --iam-account %s" % service_account)
  )

def parse_list_key_output(output):
  for line in [l for l in output.splitlines() if l][1:-1]:
    key_id, created_at, expires_at = line.split()
    yield key_id


def delete_keys(key_ids, service_account):
  for key_id in key_ids:
    run_command(
      "gcloud iam service-accounts keys delete %s --iam-account %s --quiet" % (key_id, service_account),
    )
    print "Deleted key %s" % key_id


if __name__ == "__main__":
  service_account = sys.argv[1]
  delete_keys(
    list_key_ids_for_service_account(service_account),
    service_account,
  )
