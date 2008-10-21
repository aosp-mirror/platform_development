#!/usr/bin/env python

##
## chewie.py
## chews browser http log.  draws graph of connections
## Be sure there is only one pageload in the log.
##
## you'll want to
##   sudo apt-get install python-matplotlib
## before running this
##

import sys, pylab

# can't just use a dict, because there can be dups
class Queue:
    def __init__(self):
        self.queue = []

    def add(self, url, time):
        self.queue.append([url, time])

    def get(self, url):
        for x in range(len(self.queue)):
            rec = self.queue[x]
            if rec[0] == url:
                del self.queue[x]
                return rec[1]

## pull out request lag -- queue to start to done
def lag():

    font = {'color': '#909090', 'fontsize': 6}
    extractMe = {
        'RequestQueue.queueRequest': "Q",
        'Connection.openHttpConnection()': "O",
        'Request.sendRequest()': "S",
        'Request.requestSent()': "T",
        'processRequests()': 'R',
        'Request.readResponse():': "D",         # done
        'clearPipe()': 'U',	                    # unqueue
        'Request.readResponse()': 'B',          # read data block
        'Request.readResponseStatus():': 'HR',  # read http response line
        'hdr': 'H',                             # http header
        }
    keys = extractMe.keys()

    f = open(sys.argv[1], "r")

    t0 = None

    # thread, queued, opened, send, sent, reading, read, uri, server, y
    # 0       1       2       3     4     5        6     7    8       9
    vals = []

    queued = Queue()
    opened = {"http0": None,
              "http1": None,
              "http2": None,
              "http3": None,
              "http4": None,
              "http5": None}
    active = {"http0": [],
              "http1": [],
              "http2": [],
              "http3": [],
              "http4": [],
              "http5": []}
    connectionCount = 0
    byteCount = 0
    killed = [[], []]

    while (True):
        line = f.readline()
        if len(line) == 0: break

        splitup = line.split()

        # http only
        if splitup[0] != "V/http": continue

        x = splitup[3:]

        # filter to named lines
        if x[2] not in keys: continue
        x[2] = extractMe[x[2]]

        # normalize time
        if t0 == None: t0 = int(x[0])
        x[0] = int(x[0]) - t0

        thread, action = x[1], x[2]
        if action == "Q":
            time, url = x[0], x[3]
            queued.add(url, time)
        elif action == "O":
            # save opened time and server for this thread, so we can stuff it in l8r
            time, thread, host = x[0], x[1], x[4]
            opened[thread] = [time, host, connectionCount]
            connectionCount += 1
        elif action == "S":
            time, thread, url = x[0], x[1], x[3]
            opentime, host, connection = opened[thread]
            qtime = queued.get(url)
            record = [thread, qtime, opentime, time, None, None, None, url, host, connection]
            active[thread].append(record)
        elif action == "T":
            time, thread = x[0], x[1]
            record = active[thread][-1]
            record[4] = time
        elif action == "R":
            print x
            if x[3] in ["sleep", "no", "wait"]: continue
            time, thread, = x[0], x[1]
            record = active[thread][0]
            record[5] = time
        elif action == 'U':
            thread = x[1]
            record = active[thread][0]
            killed[0].append(record[9])
            killed[1].append(x[0])
            queued.add(record[7], record[1])
            del active[thread][0]
        elif action == "D":
            time, thread = x[0], x[1]
            record = active[thread][0]
            record[6] = time
            vals.append(record)
            del active[thread][0]
            print record
            # print record[3] / 1000, record[6] / 1000, record[7]
        elif action == "B":
            byteCount += int(x[3])
        elif action == "HR":
            byteCount += int(x[2])

    f.close()

    rng = range(connectionCount)

    opened = []
    drawn = [False for x in rng]
    for val in vals:
        y= val[9]
        if not drawn[y]:
            drawn[y] = True
            opened.append(val[2])
            pylab.text(0, y - 0.25, "%s %s %s" % (val[9], val[0][4], val[8]), font)

    # define limits
    # pylab.plot([vals[-1][6]], rng)

    print opened, rng
    pylab.plot(opened, rng, 'ro')
    pylab.plot(killed[1], killed[0], 'rx')

    for val in vals:
        thread, queued, opened, send, sent, reading, read, uri, server, y = val
        # send arrow
        arrow = pylab.Arrow(send, y, sent - send, 0)
        arrow.set_facecolor("g")
        ax = pylab.gca()
        ax.add_patch(arrow)
        # read arrow
        arrow = pylab.Arrow(reading, y, read - reading, 0)
        arrow.set_facecolor("r")
        ax = pylab.gca()
        ax.add_patch(arrow)

    caption = \
            "\nrequests: %s\n" % len(vals) + \
            "byteCount: %s\n" % byteCount + \
            "data rate: %s\n" % (1000 * byteCount / vals[-1][6])+ \
            "connections: %s\n" % connectionCount

    pylab.figtext(0.82, 0.30, caption, bbox=dict(facecolor='lightgrey', alpha=0.5))

    # print lines, [[x, x] for x in range(len(vals))]
    # pylab.plot(lines, [[x, x] for x in range(len(vals))], 'r-')

    pylab.grid()
    pylab.show()

if __name__ == '__main__': lag()
