#!/usr/bin/env python2.7 -B

import ps


def test_pids():
  text = """USER      PID   PPID  VSIZE  RSS   WCHAN              PC  NAME
root      1     0     10632  776   SyS_epoll_ 0000000000 S /init
root      2     0     0      0       kthreadd 0000000000 S kthreadd
u0_a22    7308  633   1808572 79760 SyS_epoll_ 0000000000 S com.google.android.dialer
u0_a19    7370  633   1841228 37828 SyS_epoll_ 0000000000 S com.google.android.gms.feedback
u0_a136   7846  634   1320656 119964 SyS_epoll_ 0000000000 S com.sonos.acr
"""

  actual = ps.ParsePs(text)

  expected = [
      ('root', '1', '0', '/init'),
      ('root', '2', '0', 'kthreadd'),
      ('u0_a22', '7308', '633', 'com.google.android.dialer'),
      ('u0_a19', '7370', '633', 'com.google.android.gms.feedback'),
      ('u0_a136', '7846', '634', 'com.sonos.acr')
    ]

  if actual != expected:
    print "Expected:"
    print expected
    print
    print "Actual:"
    print actual
    raise Exception("test failed")


def test_uids():
  text = """vers,1
vrfy,com.android.vending,10035
ifv,com.google.android.gms,10019
lib,com.vzw.apnlib,jar,/system/app/VZWAPNLib/VZWAPNLib.apk
lib,com.google.android.media.effects,jar,/system/framework/com.google.android.media.effects.jar
pkg,com.amazon.mShop.android.shopping,10118,116434610,1486361139496,1491403362196,com.android.vending
pkg-splt,base,0
pkg-usr,0,IbsusL,0,com.android.vending
pkg,com.getgoodcode.bart,10129,21,1486361637815,1486361637815,com.android.vending
pkg-splt,base,0
pkg-usr,0,IbsuSl,0,?
pkg,com.flightaware.android.liveFlightTracker,10115,138,1486361042695,1486361042695,com.android.vending
pkg-splt,base,0
pkg-usr,0,IbsuSl,0,?
pkg,com.android.cts.priv.ctsshim,10010,24,1230796800000,1230796800000,?
pkg-splt,base,0
pkg-usr,0,IbsusL,0,?
"""
  actual = ps.ParseUids(text)

  expected = [
    ('10118', 'com.amazon.mShop.android.shopping'),
    ('10129', 'com.getgoodcode.bart'),
    ('10115', 'com.flightaware.android.liveFlightTracker'),
    ('10010', 'com.android.cts.priv.ctsshim')
  ]

  if actual != expected:
    print "Expected:"
    print expected
    print
    print "Actual:"
    print actual
    raise Exception("test failed")


def test_update():
  """Requires an attached device."""
  processes = ps.ProcessSet()
  processes.Update()
  processes.Update()
  processes.Print()
  process = processes.FindPid("0", "0")
  print "process:", process
  print "uid:", process.uid.uid
  print "username:", process.uid.name
  print "pid:", process.pid
  print "ppid:", process.ppid
  print "name:", process.name
  print "displayName:", process.DisplayName()


def main():
  #test_uids()
  #test_pids()
  test_update()


if __name__ == "__main__":
    main()


# vim: set ts=2 sw=2 sts=2 tw=100 nocindent autoindent smartindent expandtab:
