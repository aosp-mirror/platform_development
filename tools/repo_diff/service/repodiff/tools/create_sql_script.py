import datetime
import os

SQL_SCRIPTS_DIR = "tools/migrations"


def create_filenames():
  for ftype in ("upgrade", "downgrade"):
    yield "%s/%s_%s.sql" % (
      SQL_SCRIPTS_DIR,
      datetime.datetime.utcnow().strftime("%Y_%m_%d__%H_%M_%S"),
      ftype,
    )


def touch_files(fnames):
  for fname in fnames:
    print "Creating file %s" % fname
    os.system("touch %s" % fname)


if __name__ == "__main__":
  touch_files(
    create_filenames(),
  )
