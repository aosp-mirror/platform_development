#!/usr/bin/env python

"""
  chewperf.py: Chew an http perf log
  bucketize

"""

import sys, time

def resets():
    f = open(sys.argv[1]).read()
    rawLines = f.split('\n')

    times = []
    for x in range(len(rawLines)):
        line = rawLines[x].split()
        try:
            if line[-1] == "SIGNAL_STRENGTH":
                ts = int(rawLines[x - 1].split()[-1])
                times.append(ts)
        except:
            pass

    return times

def augment():
    f = open(sys.argv[1]).read()
    rawLines = f.split('\r\n')

    out = []
    t0 = None
    last = 0
    for line in rawLines:
        if "Pulled" in line:
            chewed = [int(line.split()[5]), int(line.split()[7])]
            if not t0: t0 = chewed[1]
            tm = chewed[1] - t0
            out.append("%s %d" % (line, (tm - last)))
            last = tm
        else:
            out.append(line)
    print "\n".join(out)

def chew():
    f = open(sys.argv[1]).read()
    rawLines = f.split('\n')
    lines = [x for x in rawLines if "Pulled" in x]

    sidx = lines[0].split().index("Pulled")
    print "sidx", sidx
    chewed = [[int(x.split()[sidx + 2]), int(x.split()[sidx + 4])] for x in lines]

    t0 = chewed[0][1]
    tLast = chewed[-1][1]
    chewed = [[x[1] - t0, x[0]] for x in chewed]

    totalTime = tLast - t0
    bytes = sum(x[1] for x in chewed)
    print "total time", totalTime, "bytes", bytes, "rate", bytes * 1000 / totalTime

    buckets = {}
    for x in chewed:
        bucket = x[0] / 1000
        bytes = x[1]
        if bucket in buckets:
            buckets[bucket] += bytes
        else:
            buckets[bucket] = bytes

    top = max(buckets.keys())
    for x in range(top):
        if x not in buckets.keys():
            buckets[x] = 0

    # smooth
    window = [0 for x in range(5)]

    for x in range(len(buckets.items())):
        window[x % len(window)] = buckets.items()[x][1]
        print "%s\t%s" % (buckets.items()[x][0], sum(window) / len(window))

def main():
    chew()

if __name__ == '__main__':
    main()
