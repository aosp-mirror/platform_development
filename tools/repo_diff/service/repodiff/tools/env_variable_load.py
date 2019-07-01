import json
import os
import sys


if __name__ == "__main__":
  try:
    serialized_env_vars = sys.argv[1]
  except IndexError:
    print "Usage: python env_variable_load.py <serialized json>"
    print "Obtain JSON dict from https://valentine.corp.google.com/#/show/1521659275969805"
    sys.exit(1)

  with open("%s/.bashrc" % os.environ["HOME"], "rb") as f:
    existing_contents = f.read()

  with open("%s/.bashrc" % os.environ["HOME"], "a") as f:
    f.write("\n")

    for key, value in sorted(json.loads(serialized_env_vars).items()):
      if key in existing_contents:
        continue
      f.write("export %s=\"%s\"\n" % (key, value))
