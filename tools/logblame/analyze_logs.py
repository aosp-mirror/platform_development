#!/usr/bin/env python2.7

import argparse
import datetime
import re
import subprocess
import sys

import logs
import ps

DURATION_RE = re.compile("((\\d+)w)?((\\d+)d)?((\\d+)h)?((\\d+)m)?((\\d+)s)?")

class Bucket(object):
  """Bucket of stats for a particular key managed by the Stats object."""
  def __init__(self):
    self.count = 0
    self.memory = 0
    self.lines = []

  def __str__(self):
    return "(%s,%s)" % (self.count, self.memory)


class Stats(object):
  """A group of stats with a particular key, where both memory and count are tracked."""
  def __init__(self):
    self._data = dict()

  def add(self, key, logLine):
    bucket = self._data.get(key)
    if not bucket:
      bucket = Bucket()
      self._data[key] = bucket
    bucket.count += 1
    bucket.memory += logLine.memory()
    bucket.lines.append(logLine)

  def __iter__(self):
    return self._data.iteritems()

  def data(self):
    return [(key, bucket) for key, bucket in self._data.iteritems()]

  def byCount(self):
    result = self.data()
    result.sort(lambda a, b: -cmp(a[1].count, b[1].count))
    return result

  def byMemory(self):
    result = self.data()
    result.sort(lambda a, b: -cmp(a[1].memory, b[1].memory))
    return result


def ParseDuration(s):
  """Parse a date of the format .w.d.h.m.s into the number of seconds."""
  def make_int(index):
    val = m.group(index)
    if val:
      return int(val)
    else:
      return 0
  m = DURATION_RE.match(s)
  if m:
    weeks = make_int(2)
    days = make_int(4)
    hours = make_int(6)
    minutes = make_int(8)
    seconds = make_int(10)
    return (weeks * 604800) + (days * 86400) + (hours * 3600) + (minutes * 60) + seconds
  return 0

def FormatMemory(n):
  """Prettify the number of bytes into gb, mb, etc."""
  if n >= 1024 * 1024 * 1024:
    return "%10d gb" % (n / (1024 * 1024 * 1024))
  elif n >= 1024 * 1024:
    return "%10d mb" % (n / (1024 * 1024))
  elif n >= 1024:
    return "%10d kb" % (n / 1024)
  else:
    return "%10d b " % n

def FormateTimeDelta(td):
  """Format a time duration into the same format we accept on the commandline."""
  seconds = (td.days * 86400) + (td.seconds) + int(td.microseconds / 1000000)
  if seconds == 0:
    return "0s"
  result = ""
  if seconds >= 604800:
    weeks = int(seconds / 604800)
    seconds -= weeks * 604800
    result += "%dw" % weeks
  if seconds >= 86400:
    days = int(seconds / 86400)
    seconds -= days * 86400
    result += "%dd" % days
  if seconds >= 3600:
    hours = int(seconds / 3600)
    seconds -= hours * 3600
    result += "%dh" % hours
  if seconds >= 60:
    minutes = int(seconds / 60)
    seconds -= minutes * 60
    result += "%dm" % minutes
  if seconds > 0:
    result += "%ds" % seconds
  return result


def WriteResult(totalCount, totalMemory, bucket, text):
  """Write a bucket in the normalized format."""
  print "%7d (%2d%%) %s (%2d%%)  %s" % (bucket.count, (100 * bucket.count / totalCount),
      FormatMemory(bucket.memory), (100 * bucket.memory / totalMemory), text)
  

def ParseArgs(argv):
  parser = argparse.ArgumentParser(description="Process some integers.")
  parser.add_argument("input", type=str, nargs="?",
                      help="the logs file to read")
  parser.add_argument("--clear", action="store_true",
                      help="clear the log buffer before running logcat")
  parser.add_argument("--duration", type=str, nargs=1,
                      help="how long to run for (XdXhXmXs)")
  parser.add_argument("--rawlogs", type=str, nargs=1,
                      help="file to put the rawlogs into")

  args = parser.parse_args()

  args.durationSec = ParseDuration(args.duration[0]) if args.duration else 0

  return args


def main(argv):
  args = ParseArgs(argv)

  processes = ps.ProcessSet()

  if args.rawlogs:
    rawlogs = file(args.rawlogs[0], "w")
  else:
    rawlogs = None

  # Choose the input
  if args.input:
    # From a file of raw logs
    try:
      infile = file(args.input, "r")
    except IOError:
      sys.stderr.write("Error opening file for read: %s\n" % args.input[0])
      sys.exit(1)
  else:
    # From running adb logcat on an attached device
    if args.clear:
      subprocess.check_call(["adb", "logcat", "-c"])
    cmd = ["adb", "logcat", "-v", "long", "-D", "-v", "uid"]
    if not args.durationSec:
      cmd.append("-d")
    logcat = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    infile = logcat.stdout

    # Do one update because we know we'll need it, but then don't do it again
    # if we're not streaming them.
    processes.Update(True)
    if args.durationSec:
      processes.doUpdates = True

  totalCount = 0
  totalMemory = 0
  byTag = Stats()
  byPid = Stats()
  byText = Stats()

  startTime = datetime.datetime.now()

  # Read the log lines from the parser and build a big mapping of everything
  for logLine in logs.ParseLogcat(infile, processes, args.durationSec):
    if rawlogs:
      rawlogs.write("%-10s %s %-6s %-6s %-6s %s/%s: %s\n" %(logLine.buf, logLine.timestamp,
          logLine.uid, logLine.pid, logLine.tid, logLine.level, logLine.tag, logLine.text))
    
    totalCount += 1
    totalMemory += logLine.memory()
    byTag.add(logLine.tag, logLine)
    byPid.add(logLine.pid, logLine)
    byText.add(logLine.text, logLine)

  endTime = datetime.datetime.now()

  # Print the log analysis

  # At this point, everything is loaded, don't bother looking
  # for new processes
  processes.doUpdates = False

  print "Top tags by count"
  print "-----------------"
  i = 0
  for k,v in byTag.byCount():
    WriteResult(totalCount, totalMemory, v, k)
    if i >= 10:
      break
    i += 1

  print
  print "Top tags by memory"
  print "------------------"
  i = 0
  for k,v in byTag.byMemory():
    WriteResult(totalCount, totalMemory, v, k)
    if i >= 10:
      break
    i += 1

  print
  print "Top Processes by memory"
  print "-----------------------"
  i = 0
  for k,v in byPid.byMemory():
    WriteResult(totalCount, totalMemory, v,
        "%-8s %s" % (k, processes.FindPid(k).DisplayName()))
    if i >= 10:
      break
    i += 1

  print
  print "Top Duplicates by count"
  print "-----------------------"
  i = 0
  for k,v in byText.byCount():
    logLine = v.lines[0]
    WriteResult(totalCount, totalMemory, v,
        "%s/%s: %s" % (logLine.level, logLine.tag, logLine.text))
    if i >= 10:
      break
    i += 1

  print
  print "Top Duplicates by memory"
  print "-----------------------"
  i = 0
  for k,v in byText.byCount():
    logLine = v.lines[0]
    WriteResult(totalCount, totalMemory, v,
        "%s/%s: %s" % (logLine.level, logLine.tag, logLine.text))
    if i >= 10:
      break
    i += 1

  print
  print "Totals"
  print "------"
  print "%7d  %s" % (totalCount, FormatMemory(totalMemory))

  print "Actual duration: %s" % FormateTimeDelta(endTime-startTime)

if __name__ == "__main__":
  main(sys.argv)

# vim: set ts=2 sw=2 sts=2 tw=100 nocindent autoindent smartindent expandtab:
