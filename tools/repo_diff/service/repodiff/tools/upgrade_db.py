import commands
import os
import sys


GET_VERSION_QUERY = "SELECT latest_revision FROM application_meta LIMIT 1;"
NULL_VERSION = "0000_00_00__00_00_00"


class Commands(object):
  UPGRADE = "upgrade"
  DOWNGRADE = "downgrade"

  @classmethod
  def all(cls):
    return (
      cls.UPGRADE,
      cls.DOWNGRADE,
    )


def run_command(command):
  exit_code, output = commands.getstatusoutput(command)

  if exit_code != 0:
    raise ValueError("Error running command: %s" % command)

  return output


def run_query(query):
  return run_command(
    "make db_shell EXTRA=\"-e '{query}'\"".format(
      query=query,
    )
  )


def parse_version_query(mysql_output):
  print "MYSQL OUTPUT: %s" % mysql_output
  lines = (mysql_output.splitlines())
  if len(lines) == 3:
    # table not yet populated
    return NULL_VERSION
  return lines[3]


def get_db_version():
  return parse_version_query(
    run_query(GET_VERSION_QUERY)
  )


def get_upgrade_files_gt(db_version, sql_scripts_dir):
  return sorted(
    filter(
      lambda fname: "upgrade" in fname and fname[:len(db_version)] > db_version,
      os.listdir(
        sql_scripts_dir,
      )
    )
  )


def get_downgrade_file(db_version):
  if db_version == NULL_VERSION:
    return None
  return "%s_downgrade.sql" % db_version


def get_previous_db_version(current_db_version, sql_scripts_dir):
  try:
    return sorted(
      filter(
        lambda fname: "downgrade" in fname and fname[:len(current_db_version)] < current_db_version,
        os.listdir(
          sql_scripts_dir,
        )
      )
    )[-1].replace("_downgrade.sql", "")
  except IndexError:
    return NULL_VERSION


def run_sql_file(sql_scripts_dir, sql_fname):
  run_command(
    "make db_shell < %s" % os.path.join(sql_scripts_dir, sql_fname)
  )


def update_db_version(canonical_version):
  run_query(
    """
      START TRANSACTION;
      TRUNCATE TABLE application_meta;
      INSERT INTO application_meta (latest_revision) VALUES(\\"{canonical_version}\\");
      COMMIT;
    """.format(
      canonical_version=canonical_version,
    ).replace("\n", "")
  )


def fname_to_canonical_version(fname):
  return fname.replace(
    "_upgrade.sql",
    "",
  ).replace(
    "_downgrade.sql",
    "",
  )


def upgrade_db(sql_scripts_dir):
  print "Upgrading..."
  upgrade_files = get_upgrade_files_gt(
    get_db_version(),
    sql_scripts_dir,
  )
  if not upgrade_files:
    print "Database is upgraded to the latest revision"
    return
  for upgrade_file in upgrade_files:
    run_sql_file(sql_scripts_dir, upgrade_file)
    update_db_version(
      fname_to_canonical_version(upgrade_file),
    )
  print "Database version is now set to %s" % get_db_version()


def downgrade_db(sql_scripts_dir):
  print "Downgrading..."
  current_version = get_db_version()
  downgrade_file = get_downgrade_file(current_version)
  if downgrade_file is None:
    print "Database is already in an empty state"
    return
  run_sql_file(sql_scripts_dir, downgrade_file)
  update_db_version(
    get_previous_db_version(
      current_version,
      sql_scripts_dir,
    ),
  )


if __name__ == "__main__":
  try:
    {
      Commands.UPGRADE: upgrade_db,
      Commands.DOWNGRADE: downgrade_db,
    }[sys.argv[1]](sys.argv[2])
  except (IndexError, KeyError):
    print "Usage: python tools/upgrade_db.py [%s|%s] {sql_script_dir}" % Commands.all()
    sys.exit(1)
