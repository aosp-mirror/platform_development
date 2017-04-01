#!/usr/bin/env python2.7 -B

import analyze_logs


def test_ParseDuration(s, expected):
  actual = analyze_logs.ParseDuration(s)
  if actual != expected:
    raise Exception("expected %s, actual %s" % (expected, actual))

def main():
  test_ParseDuration("1w", 604800)
  test_ParseDuration("1d", 86400)
  test_ParseDuration("1h", 3600)
  test_ParseDuration("1m", 60)
  test_ParseDuration("1s", 1)
  test_ParseDuration("1w1d1h1m1s", 694861)


if __name__ == "__main__":
  main()


# vim: set ts=2 sw=2 sts=2 tw=100 nocindent autoindent smartindent expandtab :
