#!/usr/bin/env python

#
# Copyright 2007, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
  axl.py: HTTP Client torture tester

"""

import sys, time

from twisted.internet import protocol, reactor, defer
from twisted.internet.protocol import ServerFactory, Protocol

import singletonmixin, log

class BaseProtocol(Protocol):
    def __init__(self):
        self.log = log.Log.getInstance()

    def write(self, data):
        self.log("BaseProtocol.write()", len(data), data)
        return self.transport.write(data)

    def dataReceived(self, data):
        self.log("BaseProtocol.dataReceived()", len(data), data)

    def connectionMade(self):
        self.log("BaseProtocol.connectionMade()")
        self.transport.setTcpNoDelay(1)	# send immediately

    def connectionLost(self, reason):
        self.log("BaseProtocol.connectionLost():", reason)

    def sendResponse(self, response):
        self.write("HTTP/1.1 200 OK\r\n")
        self.write("Content-Length: %d\r\n\r\n" % len(response))
        if len(response) > 0:
            self.write(response)


# Tests
# 8000: test driven by resource request

class Drop(BaseProtocol):
    """Drops connection immediately after connect"""
    PORT = 8001
    def connectionMade(self):
        BaseProtocol.connectionMade(self)
        self.transport.loseConnection()

class ReadAndDrop(BaseProtocol):
    """Read 1st line of request, then drop connection"""
    PORT = 8002
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.transport.loseConnection()

class GarbageStatus(BaseProtocol):
    """Send garbage statusline"""
    PORT = 8003
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.write("welcome to the jungle baby\r\n")

class BadHeader(BaseProtocol):
    """Drop connection after a header is half-sent"""
    PORT = 8004
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.write("HTTP/1.1 200 OK\r\n")
        self.write("Cache-Contr")
        time.sleep(1)
        self.transport.loseConnection()

class PauseHeader(BaseProtocol):
    """Pause for a second in middle of a header"""
    PORT = 8005
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.write("HTTP/1.1 200 OK\r\n")
        self.write("Cache-Contr")
        time.sleep(1)
        self.write("ol: private\r\n\r\nwe've got fun and games")
        time.sleep(1)
        self.transport.loseConnection()

class Redirect(BaseProtocol):
    PORT = 8006
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.write("HTTP/1.1 302 Moved Temporarily\r\n")
        self.write("Content-Length: 0\r\n")
        self.write("Location: http://shopping.yahoo.com/p:Canon PowerShot SD630 Digital Camera:1993588104;_ylc=X3oDMTFhZXNmcjFjBF9TAzI3MTYxNDkEc2VjA2ZwLXB1bHNlBHNsawNyc3NfcHVsc2U0LmluYw--\r\n\r\n")
        self.transport.loseConnection()

class DataDrop(BaseProtocol):
    """Drop connection in body"""
    PORT = 8007
    def dataReceived(self, data):
        if data.find("favico") >= 0:
            self.write("HTTP/1.1 404 Not Found\r\n\r\n")
            self.transport.loseConnection()
            return

        BaseProtocol.dataReceived(self, data)
        self.write("HTTP/1.1 200 OK\r\n")
#        self.write("Content-Length: 100\r\n\r\n")
        self.write("\r\n")
#        self.write("Data cuts off < 100 here!")
#        time.sleep(4)
        self.transport.loseConnection()

class DropOnce(BaseProtocol):
    """Drop every other connection"""
    PORT = 8008
    COUNT = 0
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.write("HTTP/1.1 200 OK\r\n")
        self.write("Content-Length: 5\r\n\r\n")

        if (not(DropOnce.COUNT & 1)):
            self.write("HE")
        else:
            self.write("HELLO")
        self.transport.loseConnection()

        DropOnce.COUNT += 1

class NoCR(BaseProtocol):
    """Send headers without carriage returns"""
    PORT = 8009
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.write("HTTP/1.1 200 OK\n")
        self.write("Content-Length: 5\n\n")

        self.write("HELLO")
        self.transport.loseConnection()

class PipeDrop(BaseProtocol):
    PORT = 8010
    COUNT = 0
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        if not PipeDrop.COUNT % 3:
            self.write("HTTP/1.1 200 OK\n")
            self.write("Content-Length: 943\n\n")

            self.write(open("./stfu.jpg").read())
            PipeDrop.COUNT += 1

        else:
            self.transport.loseConnection()
            PipeDrop.COUNT += 1

class RedirectLoop(BaseProtocol):
    """Redirect back to same resource"""
    PORT = 8011
    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.write("HTTP/1.1 302 Moved Temporarily\r\n")
        self.write("Content-Length: 0\r\n")
        self.write("Location: http://localhost:8011/\r\n")
        self.write("\r\n")
        self.transport.loseConnection()

class ReadAll(BaseProtocol):
    """Read entire request"""
    PORT = 8012

    def connectionMade(self):
        self.count = 0

    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        self.count += len(data)
        if self.count == 190890:
            self.transport.loseConnection()

class Timeout(BaseProtocol):
    """Timout sending body"""
    PORT = 8013

    def connectionMade(self):
        self.count = 0

    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        if self.count == 0: self.write("HTTP/1.1 200 OK\r\n\r\n")
        self.count += 1

class SlowResponse(BaseProtocol):
    """Ensure client does not time out on slow writes"""
    PORT = 8014

    def connectionMade(self):
        self.count = 0

    def dataReceived(self, data):
        BaseProtocol.dataReceived(self, data)
        if self.count == 0: self.write("HTTP/1.1 200 OK\r\n\r\n")
        self.sendPack(0)

    def sendPack(self, count):
        if count > 10:
            self.transport.loseConnection()

        self.write("all work and no play makes jack a dull boy %s\n" % count)
        d = defer.Deferred()
        d.addCallback(self.sendPack)
        reactor.callLater(15, d.callback, count + 1)


# HTTP/1.1 200 OK
# Cache-Control: private
# Content-Type: text/html
# Set-Cookie: PREF=ID=10644de62c423aa5:TM=1155044293:LM=1155044293:S=0lHtymefQRs2j7nD; expires=Sun, 17-Jan-2038 19:14:07 GMT; path=/; domain=.google.com
# Server: GWS/2.1
# Transfer-Encoding: chunked
# Date: Tue, 08 Aug 2006 13:38:13 GMT

def main():
    # Initialize log
    log.Log.getInstance(sys.stdout)

    for protocol in Drop, ReadAndDrop, GarbageStatus, BadHeader, PauseHeader, \
            Redirect, DataDrop, DropOnce, NoCR, PipeDrop, RedirectLoop, ReadAll, \
            Timeout, SlowResponse:
        factory = ServerFactory()
        factory.protocol = protocol
        reactor.listenTCP(protocol.PORT, factory)


    reactor.run()

if __name__ == '__main__':
    main()
