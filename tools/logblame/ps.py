import csv
import re
import subprocess

HEADER_RE = re.compile("USER\\s*PID\\s*PPID\\s*VSIZE\\s*RSS\\s*WCHAN\\s*PC\\s*NAME")
PROCESS_RE = re.compile("(\\S+)\\s+(\\d+)\\s+(\\d+)\\s+\\d+\\s+\\d+\\s+\\S+\\s+.\\S+\\s+\\S+\\s+(.*)")

ANDROID_UID_RE = re.compile("u(\\d)+_([0-9a-fA-F]+)")
UID_RE = re.compile("(\\d)+")

class Process(object):
  def __init__(self, uid, pid, ppid, name):
    self.uid = uid
    self.pid = pid
    self.ppid = ppid
    self.name = name

  def DisplayName(self):
    if self.name:
      return self.name
    if self.uid:
      return self.uid.name
    return self.pid

  def __str__(self):
    return "Process(uid=%s, pid=%s, name=%s)" % (self.uid, self.pid, self.name)

class Uid(object):
  def __init__(self, uid, name):
    self.uid = uid
    self.name = name

  def __str__(self):
    return "Uid(id=%s, name=%s)" % (self.uid, self.name)

class ProcessSet(object):
  def __init__(self):
    self._processes = dict()
    self._uids = dict()
    self._pidUpdateCount = 0
    self._uidUpdateCount = 0
    self.doUpdates = False

  def Update(self, force=False):
    self.UpdateUids(force)
    self.UpdateProcesses(force)

  def UpdateProcesses(self, force=False):
    if not (self.doUpdates or force):
      return
    self._pidUpdateCount += 1
    try:
      text = subprocess.check_output(["adb", "shell", "ps"])
    except subprocess.CalledProcessError:
      return # oh well. we won't get the pid
    lines = ParsePs(text)
    for line in lines:
      if not self._processes.has_key(line[1]):
        uid = self.FindUid(ParseUid(line[0]))
        self._processes[line[1]] = Process(uid, line[1], line[2], line[3])

  def UpdateUids(self, force=False):
    if not (self.doUpdates or force):
      return
    self._uidUpdateCount += 1
    try:
      text = subprocess.check_output(["adb", "shell", "dumpsys", "package", "--checkin"])
    except subprocess.CalledProcessError:
      return # oh well. we won't get the pid
    lines = ParseUids(text)
    for line in lines:
      if not self._uids.has_key(line[0]):
        self._uids[line[1]] = Uid(*line)

  def FindPid(self, pid, uid=None):
    """Try to find the Process object for the given pid.
    If it can't be found, do an update. If it still can't be found after that,
    create a syntheitc Process object, add that to the list, and return that.
    That can only happen after the process has died, and we just missed our
    chance to find it.  The pid won't come back.
    """
    result = self._processes.get(pid)
    if not result:
      self.UpdateProcesses()
      result = self._processes.get(pid)
      if not result:
        if uid:
          uid = self._uids.get(uid)
        result = Process(uid, pid, None, None)
        self._processes[pid] = result
    return result

  def FindUid(self, uid):
    result = self._uids.get(uid)
    if not result:
      self.UpdateUids()
      result = self._uids.get(uid)
      if not result:
        result = Uid(uid, uid)
        self._uids[uid] = result
    return result

  def UpdateCount(self):
    return (self._pidUpdateCount, self._uidUpdateCount)

  def Print(self):
    for process in self._processes:
      print process
    for uid in self._uids:
      print uid

def ParsePs(text):
  """Parses the output of ps, and returns it as a list of tuples of (user, pid, ppid, name)"""
  result = []
  for line in text.splitlines():
    m = HEADER_RE.match(line)
    if m:
      continue
    m = PROCESS_RE.match(line)
    if m:
      result.append((m.group(1), m.group(2), m.group(3), m.group(4)))
      continue
  return result


def ParseUids(text):
  """Parses the output of dumpsys package --checkin and returns the uids as a list of
  tuples of (uid, name)"""
  return [(x[2], x[1]) for x in csv.reader(text.split("\n")) if len(x) and x[0] == "pkg"]


def ParseUid(text):
  m = ANDROID_UID_RE.match(text)
  if m:
    result = int("0x" + m.group(2), 16)
    return "(%s/%s/%s)" % (m.group(1), m.group(2), result)
  m = UID_RE.match(text)
  if m:
    return "[%s]" % m.group(1)
  return text

# vim: set ts=2 sw=2 sts=2 tw=100 nocindent autoindent smartindent expandtab:
