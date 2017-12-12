
import datetime
import re

BUFFER_BEGIN = re.compile("^--------- beginning of (.*)$")
BUFFER_SWITCH = re.compile("^--------- switch to (.*)$")
HEADER = re.compile("^\\[ (\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d) +(.+?): *(\\d+): *(\\d+) *([EWIDV])/(.*?) *\\]$")
HEADER_TYPE2 = re.compile("^(\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d) *(\\d+) *(\\d+) *([EWIDV]) ([^ :]*?): (.*?)$")
CHATTY_IDENTICAL = re.compile("^.* identical (\\d+) lines$")

STATE_BEGIN = 0
STATE_BUFFER = 1
STATE_HEADER = 2
STATE_TEXT = 3
STATE_BLANK = 4

class LogLine(object):
  """Represents a line of android logs."""
  def __init__(self, buf=None, timestamp=None, uid=None, pid=None, tid=None, level=None,
      tag=None, text=""):
    self.buf = buf
    self.timestamp = timestamp
    self.uid = uid
    self.pid = pid
    self.tid = tid
    self.level = level
    self.tag = tag
    self.text = text
    self.process = None

  def __str__(self):
    return "{%s} {%s} {%s} {%s} {%s} {%s}/{%s}: {%s}" % (self.buf, self.timestamp, self.uid,
        self.pid, self.tid, self.level, self.tag, self.text)

  def __eq__(self, other):
      return (
            self.buf == other.buf
            and self.timestamp == other.timestamp 
            and self.uid == other.uid 
            and self.pid == other.pid 
            and self.tid == other.tid 
            and self.level == other.level 
            and self.tag == other.tag 
            and self.text == other.text 
          )

  def clone(self):
    logLine = LogLine(self.buf, self.timestamp, self.uid, self.pid, self.tid, self.level,
        self.tag, self.text)
    logLine.process = self.process
    return logLine

  def memory(self):
    """Return an estimate of how much memory is used for the log.
      32 bytes of header + 8 bytes for the pointer + the length of the tag and the text.
      This ignores the overhead of the list of log lines itself."""
    return 32 + 8 + len(self.tag) + 1 + len(self.text) + 1


def ParseLogcat(f, processes, duration=None):
  previous = None
  for logLine in ParseLogcatInner(f, processes, duration):
    if logLine.tag == "chatty" and logLine.level == "I":
      m = CHATTY_IDENTICAL.match(logLine.text)
      if m:
        for i in range(int(m.group(1))):
          clone = previous.clone()
          clone.timestamp = logLine.timestamp
          yield clone
        continue
    previous = logLine
    yield logLine


def ParseLogcatInner(f, processes, duration=None):
  """Parses a file object containing log text and returns a list of LogLine objects."""
  result = []

  buf = None
  timestamp = None
  uid = None
  pid = None
  tid = None
  level = None
  tag = None

  state = STATE_BEGIN
  logLine = None
  previous = None

  if duration:
    endTime = datetime.datetime.now() + datetime.timedelta(seconds=duration)

  # TODO: use a nonblocking / timeout read so we stop if there are
  # no logs coming out (haha joke, right!)
  for line in f:
    if duration and endTime <= datetime.datetime.now():
      break

    if len(line) > 0 and line[-1] == '\n':
      line = line[0:-1]

    m = BUFFER_BEGIN.match(line)
    if m:
      if logLine:
        yield logLine
        logLine = None
      buf = m.group(1)
      state = STATE_BUFFER
      continue

    m = BUFFER_SWITCH.match(line)
    if m:
      if logLine:
        yield logLine
        logLine = None
      buf = m.group(1)
      state = STATE_BUFFER
      continue

    m = HEADER.match(line)
    if m:
      if logLine:
        yield logLine
      logLine = LogLine(
            buf=buf,
            timestamp=m.group(1),
            uid=m.group(2),
            pid=m.group(3),
            tid=m.group(4),
            level=m.group(5),
            tag=m.group(6)
          )
      previous = logLine
      logLine.process = processes.FindPid(logLine.pid, logLine.uid)
      state = STATE_HEADER
      continue

    m = HEADER_TYPE2.match(line)
    if m:
      if logLine:
        yield logLine
      logLine = LogLine(
            buf=buf,
            timestamp=m.group(1),
            uid="0",
            pid=m.group(2),
            tid=m.group(3),
            level=m.group(4),
            tag=m.group(5),
            text=m.group(6)
          )
      previous = logLine
      logLine.process = processes.FindPid(logLine.pid, logLine.uid)
      state = STATE_BEGIN
      continue

    if not len(line):
      if state == STATE_BLANK:
        if logLine:
          logLine.text += "\n"
      state = STATE_BLANK
      continue

    if logLine:
      if state == STATE_HEADER:
        logLine.text += line
      elif state == STATE_TEXT:
        logLine.text += "\n"
        logLine.text += line
      elif state == STATE_BLANK:
        if len(logLine.text):
          logLine.text += "\n"
        logLine.text += "\n"
        logLine.text += line
    state = STATE_TEXT

  if logLine:
    yield logLine


# vim: set ts=2 sw=2 sts=2 tw=100 nocindent autoindent smartindent expandtab:
