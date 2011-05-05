/*
 * Copyright 2006 The Android Open Source Project
 *
 * JDWP spy.  This is a rearranged version of the JDWP code from the VM.
 */
#include "Common.h"
#include "jdwp/JdwpConstants.h"

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <time.h>
#include <errno.h>
#include <assert.h>

#define kInputBufferSize    (256*1024)

#define kMagicHandshakeLen  14      /* "JDWP-Handshake" */
#define kJDWPHeaderLen      11
#define kJDWPFlagReply      0x80


/*
 * Information about the remote end.
 */
typedef struct Peer {
    char    label[2];           /* 'D' or 'V' */

    int     sock;
    unsigned char   inputBuffer[kInputBufferSize];
    int     inputCount;

    bool    awaitingHandshake;  /* waiting for "JDWP-Handshake" */
} Peer;


/*
 * Network state.
 */
typedef struct NetState {
    /* listen here for connection from debugger */
    int     listenSock;

    /* connect here to contact VM */
    struct in_addr vmAddr;
    short   vmPort;

    Peer    dbg;
    Peer    vm;
} NetState;

/*
 * Function names.
 */
typedef struct {
    u1  cmdSet;
    u1  cmd;
    const char* descr;
} JdwpHandlerMap;

/*
 * Map commands to names.
 *
 * Command sets 0-63 are incoming requests, 64-127 are outbound requests,
 * and 128-256 are vendor-defined.
 */
static const JdwpHandlerMap gHandlerMap[] = {
    /* VirtualMachine command set (1) */
    { 1,    1,  "VirtualMachine.Version" },
    { 1,    2,  "VirtualMachine.ClassesBySignature" },
    { 1,    3,  "VirtualMachine.AllClasses" },
    { 1,    4,  "VirtualMachine.AllThreads" },
    { 1,    5,  "VirtualMachine.TopLevelThreadGroups" },
    { 1,    6,  "VirtualMachine.Dispose" },
    { 1,    7,  "VirtualMachine.IDSizes" },
    { 1,    8,  "VirtualMachine.Suspend" },
    { 1,    9,  "VirtualMachine.Resume" },
    { 1,    10, "VirtualMachine.Exit" },
    { 1,    11, "VirtualMachine.CreateString" },
    { 1,    12, "VirtualMachine.Capabilities" },
    { 1,    13, "VirtualMachine.ClassPaths" },
    { 1,    14, "VirtualMachine.DisposeObjects" },
    { 1,    15, "VirtualMachine.HoldEvents" },
    { 1,    16, "VirtualMachine.ReleaseEvents" },
    { 1,    17, "VirtualMachine.CapabilitiesNew" },
    { 1,    18, "VirtualMachine.RedefineClasses" },
    { 1,    19, "VirtualMachine.SetDefaultStratum" },
    { 1,    20, "VirtualMachine.AllClassesWithGeneric"},
    { 1,    21, "VirtualMachine.InstanceCounts"},

    /* ReferenceType command set (2) */
    { 2,    1,  "ReferenceType.Signature" },
    { 2,    2,  "ReferenceType.ClassLoader" },
    { 2,    3,  "ReferenceType.Modifiers" },
    { 2,    4,  "ReferenceType.Fields" },
    { 2,    5,  "ReferenceType.Methods" },
    { 2,    6,  "ReferenceType.GetValues" },
    { 2,    7,  "ReferenceType.SourceFile" },
    { 2,    8,  "ReferenceType.NestedTypes" },
    { 2,    9,  "ReferenceType.Status" },
    { 2,    10, "ReferenceType.Interfaces" },
    { 2,    11, "ReferenceType.ClassObject" },
    { 2,    12, "ReferenceType.SourceDebugExtension" },
    { 2,    13, "ReferenceType.SignatureWithGeneric" },
    { 2,    14, "ReferenceType.FieldsWithGeneric" },
    { 2,    15, "ReferenceType.MethodsWithGeneric" },
    { 2,    16, "ReferenceType.Instances" },
    { 2,    17, "ReferenceType.ClassFileVersion" },
    { 2,    18, "ReferenceType.ConstantPool" },

    /* ClassType command set (3) */
    { 3,    1,  "ClassType.Superclass" },
    { 3,    2,  "ClassType.SetValues" },
    { 3,    3,  "ClassType.InvokeMethod" },
    { 3,    4,  "ClassType.NewInstance" },

    /* ArrayType command set (4) */
    { 4,    1,  "ArrayType.NewInstance" },

    /* InterfaceType command set (5) */

    /* Method command set (6) */
    { 6,    1,  "Method.LineTable" },
    { 6,    2,  "Method.VariableTable" },
    { 6,    3,  "Method.Bytecodes" },
    { 6,    4,  "Method.IsObsolete" },
    { 6,    5,  "Method.VariableTableWithGeneric" },

    /* Field command set (8) */

    /* ObjectReference command set (9) */
    { 9,    1,  "ObjectReference.ReferenceType" },
    { 9,    2,  "ObjectReference.GetValues" },
    { 9,    3,  "ObjectReference.SetValues" },
    { 9,    4,  "ObjectReference.UNUSED" },
    { 9,    5,  "ObjectReference.MonitorInfo" },
    { 9,    6,  "ObjectReference.InvokeMethod" },
    { 9,    7,  "ObjectReference.DisableCollection" },
    { 9,    8,  "ObjectReference.EnableCollection" },
    { 9,    9,  "ObjectReference.IsCollected" },
    { 9,    10, "ObjectReference.ReferringObjects" },

    /* StringReference command set (10) */
    { 10,   1,  "StringReference.Value" },

    /* ThreadReference command set (11) */
    { 11,   1,  "ThreadReference.Name" },
    { 11,   2,  "ThreadReference.Suspend" },
    { 11,   3,  "ThreadReference.Resume" },
    { 11,   4,  "ThreadReference.Status" },
    { 11,   5,  "ThreadReference.ThreadGroup" },
    { 11,   6,  "ThreadReference.Frames" },
    { 11,   7,  "ThreadReference.FrameCount" },
    { 11,   8,  "ThreadReference.OwnedMonitors" },
    { 11,   9,  "ThreadReference.CurrentContendedMonitor" },
    { 11,   10, "ThreadReference.Stop" },
    { 11,   11, "ThreadReference.Interrupt" },
    { 11,   12, "ThreadReference.SuspendCount" },
    { 11,   13, "ThreadReference.OwnedMonitorsStackDepthInfo" },
    { 11,   14, "ThreadReference.ForceEarlyReturn" },

    /* ThreadGroupReference command set (12) */
    { 12,   1,  "ThreadGroupReference.Name" },
    { 12,   2,  "ThreadGroupReference.Parent" },
    { 12,   3,  "ThreadGroupReference.Children" },

    /* ArrayReference command set (13) */
    { 13,   1,  "ArrayReference.Length" },
    { 13,   2,  "ArrayReference.GetValues" },
    { 13,   3,  "ArrayReference.SetValues" },

    /* ClassLoaderReference command set (14) */
    { 14,   1,  "ArrayReference.VisibleClasses" },

    /* EventRequest command set (15) */
    { 15,   1,  "EventRequest.Set" },
    { 15,   2,  "EventRequest.Clear" },
    { 15,   3,  "EventRequest.ClearAllBreakpoints" },

    /* StackFrame command set (16) */
    { 16,   1,  "StackFrame.GetValues" },
    { 16,   2,  "StackFrame.SetValues" },
    { 16,   3,  "StackFrame.ThisObject" },
    { 16,   4,  "StackFrame.PopFrames" },

    /* ClassObjectReference command set (17) */
    { 17,   1,  "ClassObjectReference.ReflectedType" },

    /* Event command set (64) */
    { 64,  100, "Event.Composite" },

    /* DDMS */
    { 199,  1,  "DDMS.Chunk" },
};

/*
 * Look up a command's name.
 */
static const char* getCommandName(int cmdSet, int cmd)
{
    for (int i = 0; i < (int) NELEM(gHandlerMap); i++) {
        if (gHandlerMap[i].cmdSet == cmdSet &&
            gHandlerMap[i].cmd == cmd)
        {
            return gHandlerMap[i].descr;
        }
    }

    return "?UNKNOWN?";
}


void jdwpNetFree(NetState* netState);       /* fwd */

/*
 * Allocate state structure and bind to the listen port.
 *
 * Returns 0 on success.
 */
NetState* jdwpNetStartup(unsigned short listenPort, const char* connectHost,
    unsigned short connectPort)
{
    NetState* netState = (NetState*) malloc(sizeof(*netState));
    memset(netState, 0, sizeof(*netState));
    netState->listenSock = -1;
    netState->dbg.sock = netState->vm.sock = -1;

    strcpy(netState->dbg.label, "D");
    strcpy(netState->vm.label, "V");

    /*
     * Set up a socket to listen for connections from the debugger.
     */

    netState->listenSock = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (netState->listenSock < 0) {
        fprintf(stderr, "Socket create failed: %s\n", strerror(errno));
        goto fail;
    }

    /* allow immediate re-use if we die */
    {
        int one = 1;
        if (setsockopt(netState->listenSock, SOL_SOCKET, SO_REUSEADDR, &one,
                sizeof(one)) < 0)
        {
            fprintf(stderr, "setsockopt(SO_REUSEADDR) failed: %s\n",
                strerror(errno));
            goto fail;
        }
    }

    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(listenPort);
    addr.sin_addr.s_addr = INADDR_ANY;

    if (bind(netState->listenSock, (struct sockaddr*) &addr, sizeof(addr)) != 0)
    {
        fprintf(stderr, "attempt to bind to port %u failed: %s\n",
            listenPort, strerror(errno));
        goto fail;
    }

    fprintf(stderr, "+++ bound to port %u\n", listenPort);

    if (listen(netState->listenSock, 5) != 0) {
        fprintf(stderr, "Listen failed: %s\n", strerror(errno));
        goto fail;
    }

    /*
     * Do the hostname lookup for the VM.
     */
    struct hostent* pHost;

    pHost = gethostbyname(connectHost);
    if (pHost == NULL) {
        fprintf(stderr, "Name lookup of '%s' failed: %s\n",
            connectHost, strerror(h_errno));
        goto fail;
    }

    netState->vmAddr = *((struct in_addr*) pHost->h_addr_list[0]);
    netState->vmPort = connectPort;

    fprintf(stderr, "+++ connect host resolved to %s\n",
        inet_ntoa(netState->vmAddr));

    return netState;

fail:
    jdwpNetFree(netState);
    return NULL;
}

/*
 * Shut down JDWP listener.  Don't free state.
 *
 * Note that "netState" may be partially initialized if "startup" failed.
 */
void jdwpNetShutdown(NetState* netState)
{
    int listenSock = netState->listenSock;
    int dbgSock = netState->dbg.sock;
    int vmSock = netState->vm.sock;

    /* clear these out so it doesn't wake up and try to reuse them */
    /* (important when multi-threaded) */
    netState->listenSock = netState->dbg.sock = netState->vm.sock = -1;

    if (listenSock >= 0) {
        shutdown(listenSock, SHUT_RDWR);
        close(listenSock);
    }
    if (dbgSock >= 0) {
        shutdown(dbgSock, SHUT_RDWR);
        close(dbgSock);
    }
    if (vmSock >= 0) {
        shutdown(vmSock, SHUT_RDWR);
        close(vmSock);
    }
}

/*
 * Shut down JDWP listener and free its state.
 */
void jdwpNetFree(NetState* netState)
{
    if (netState == NULL)
        return;

    jdwpNetShutdown(netState);
    free(netState);
}

/*
 * Disable the TCP Nagle algorithm, which delays transmission of outbound
 * packets until the previous transmissions have been acked.  JDWP does a
 * lot of back-and-forth with small packets, so this may help.
 */
static int setNoDelay(int fd)
{
    int cc, on = 1;

    cc = setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &on, sizeof(on));
    assert(cc == 0);
    return cc;
}

/*
 * Accept a connection.  This will block waiting for somebody to show up.
 */
bool jdwpAcceptConnection(NetState* netState)
{
    struct sockaddr_in addr;
    socklen_t addrlen;
    int sock;

    if (netState->listenSock < 0)
        return false;       /* you're not listening! */

    assert(netState->dbg.sock < 0);     /* must not already be talking */

    addrlen = sizeof(addr);
    do {
        sock = accept(netState->listenSock, (struct sockaddr*) &addr, &addrlen);
        if (sock < 0 && errno != EINTR) {
            fprintf(stderr, "accept failed: %s\n", strerror(errno));
            return false;
        }
    } while (sock < 0);

    fprintf(stderr, "+++ accepted connection from %s:%u\n",
        inet_ntoa(addr.sin_addr), ntohs(addr.sin_port));

    netState->dbg.sock = sock;
    netState->dbg.awaitingHandshake = true;
    netState->dbg.inputCount = 0;

    setNoDelay(sock);

    return true;
}

/*
 * Close the connections to the debugger and VM.
 *
 * Reset the state so we're ready to receive a new connection.
 */
void jdwpCloseConnection(NetState* netState)
{
    if (netState->dbg.sock >= 0) {
        fprintf(stderr, "+++ closing connection to debugger\n");
        close(netState->dbg.sock);
        netState->dbg.sock = -1;
    }
    if (netState->vm.sock >= 0) {
        fprintf(stderr, "+++ closing connection to vm\n");
        close(netState->vm.sock);
        netState->vm.sock = -1;
    }
}

/*
 * Figure out if we have a full packet in the buffer.
 */
static bool haveFullPacket(Peer* pPeer)
{
    long length;

    if (pPeer->awaitingHandshake)
        return (pPeer->inputCount >= kMagicHandshakeLen);

    if (pPeer->inputCount < 4)
        return false;

    length = get4BE(pPeer->inputBuffer);
    return (pPeer->inputCount >= length);
}

/*
 * Consume bytes from the buffer.
 *
 * This would be more efficient with a circular buffer.  However, we're
 * usually only going to find one packet, which is trivial to handle.
 */
static void consumeBytes(Peer* pPeer, int count)
{
    assert(count > 0);
    assert(count <= pPeer->inputCount);

    if (count == pPeer->inputCount) {
        pPeer->inputCount = 0;
        return;
    }

    memmove(pPeer->inputBuffer, pPeer->inputBuffer + count,
        pPeer->inputCount - count);
    pPeer->inputCount -= count;
}

/*
 * Get the current time.
 */
static void getCurrentTime(int* pMin, int* pSec)
{
    time_t now;
    struct tm* ptm;

    now = time(NULL);
    ptm = localtime(&now);
    *pMin = ptm->tm_min;
    *pSec = ptm->tm_sec;
}

/*
 * Dump the contents of a packet to stdout.
 */
static void dumpPacket(const unsigned char* packetBuf, const char* srcName,
    const char* dstName)
{
    const unsigned char* buf = packetBuf;
    char prefix[3];
    u4 length, id;
    u1 flags, cmdSet=0, cmd=0;
    JdwpError error = ERR_NONE;
    bool reply;
    int dataLen;

    length = get4BE(buf+0);
    id = get4BE(buf+4);
    flags = get1(buf+8);
    if ((flags & kJDWPFlagReply) != 0) {
        reply = true;
        error = static_cast<JdwpError>(get2BE(buf+9));
    } else {
        reply = false;
        cmdSet = get1(buf+9);
        cmd = get1(buf+10);
    }

    buf += kJDWPHeaderLen;
    dataLen = length - (buf - packetBuf);

    if (!reply) {
        prefix[0] = srcName[0];
        prefix[1] = '>';
    } else {
        prefix[0] = dstName[0];
        prefix[1] = '<';
    }
    prefix[2] = '\0';

    int min, sec;
    getCurrentTime(&min, &sec);

    if (!reply) {
        printf("%s REQUEST dataLen=%-5u id=0x%08x flags=0x%02x cmd=%d/%d [%02d:%02d]\n",
            prefix, dataLen, id, flags, cmdSet, cmd, min, sec);
        printf("%s   --> %s\n", prefix, getCommandName(cmdSet, cmd));
    } else {
        printf("%s REPLY   dataLen=%-5u id=0x%08x flags=0x%02x err=%d (%s) [%02d:%02d]\n",
            prefix, dataLen, id, flags, error, dvmJdwpErrorStr(error), min,sec);
    }
    if (dataLen > 0)
        printHexDump2(buf, dataLen, prefix);
    printf("%s ----------\n", prefix);
}

/*
 * Handle a packet.  Returns "false" if we encounter a connection-fatal error.
 */
static bool handlePacket(Peer* pDst, Peer* pSrc)
{
    const unsigned char* buf = pSrc->inputBuffer;
    u4 length;
    u1 flags;
    int cc;

    length = get4BE(buf+0);
    flags = get1(buf+9);

    assert((int) length <= pSrc->inputCount);

    dumpPacket(buf, pSrc->label, pDst->label);

    cc = write(pDst->sock, buf, length);
    if (cc != (int) length) {
        fprintf(stderr, "Failed sending packet: %s\n", strerror(errno));
        return false;
    }
    /*printf("*** wrote %d bytes from %c to %c\n",
        cc, pSrc->label[0], pDst->label[0]);*/

    consumeBytes(pSrc, length);
    return true;
}

/*
 * Handle incoming data.  If we have a full packet in the buffer, process it.
 */
static bool handleIncoming(Peer* pWritePeer, Peer* pReadPeer)
{
    if (haveFullPacket(pReadPeer)) {
        if (pReadPeer->awaitingHandshake) {
            printf("Handshake [%c]: %.14s\n",
                pReadPeer->label[0], pReadPeer->inputBuffer);
            if (write(pWritePeer->sock, pReadPeer->inputBuffer,
                    kMagicHandshakeLen) != kMagicHandshakeLen)
            {
                fprintf(stderr,
                    "+++ [%c] handshake write failed\n", pReadPeer->label[0]);
                goto fail;
            }
            consumeBytes(pReadPeer, kMagicHandshakeLen);
            pReadPeer->awaitingHandshake = false;
        } else {
            if (!handlePacket(pWritePeer, pReadPeer))
                goto fail;
        }
    } else {
        /*printf("*** %c not full yet\n", pReadPeer->label[0]);*/
    }

    return true;

fail:
    return false;
}

/*
 * Process incoming data.  If no data is available, this will block until
 * some arrives.
 *
 * Returns "false" on error (indicating that the connection has been severed).
 */
bool jdwpProcessIncoming(NetState* netState)
{
    int cc;

    assert(netState->dbg.sock >= 0);
    assert(netState->vm.sock >= 0);

    while (!haveFullPacket(&netState->dbg) && !haveFullPacket(&netState->vm)) {
        /* read some more */
        int highFd;
        fd_set readfds;

        highFd = (netState->dbg.sock > netState->vm.sock) ?
            netState->dbg.sock+1 : netState->vm.sock+1;
        FD_ZERO(&readfds);
        FD_SET(netState->dbg.sock, &readfds);
        FD_SET(netState->vm.sock, &readfds);

        errno = 0;
        cc = select(highFd, &readfds, NULL, NULL, NULL);
        if (cc < 0) {
            if (errno == EINTR) {
                fprintf(stderr, "+++ EINTR on select\n");
                continue;
            }
            fprintf(stderr, "+++ select failed: %s\n", strerror(errno));
            goto fail;
        }

        if (FD_ISSET(netState->dbg.sock, &readfds)) {
            cc = read(netState->dbg.sock,
                netState->dbg.inputBuffer + netState->dbg.inputCount,
                sizeof(netState->dbg.inputBuffer) - netState->dbg.inputCount);
            if (cc < 0) {
                if (errno == EINTR) {
                    fprintf(stderr, "+++ EINTR on read\n");
                    continue;
                }
                fprintf(stderr, "+++ dbg read failed: %s\n", strerror(errno));
                goto fail;
            }
            if (cc == 0) {
                if (sizeof(netState->dbg.inputBuffer) ==
                        netState->dbg.inputCount)
                    fprintf(stderr, "+++ debugger sent huge message\n");
                else
                    fprintf(stderr, "+++ debugger disconnected\n");
                goto fail;
            }

            /*printf("*** %d bytes from dbg\n", cc);*/
            netState->dbg.inputCount += cc;
        }

        if (FD_ISSET(netState->vm.sock, &readfds)) {
            cc = read(netState->vm.sock,
                netState->vm.inputBuffer + netState->vm.inputCount,
                sizeof(netState->vm.inputBuffer) - netState->vm.inputCount);
            if (cc < 0) {
                if (errno == EINTR) {
                    fprintf(stderr, "+++ EINTR on read\n");
                    continue;
                }
                fprintf(stderr, "+++ vm read failed: %s\n", strerror(errno));
                goto fail;
            }
            if (cc == 0) {
                if (sizeof(netState->vm.inputBuffer) ==
                        netState->vm.inputCount)
                    fprintf(stderr, "+++ vm sent huge message\n");
                else
                    fprintf(stderr, "+++ vm disconnected\n");
                goto fail;
            }

            /*printf("*** %d bytes from vm\n", cc);*/
            netState->vm.inputCount += cc;
        }
    }

    if (!handleIncoming(&netState->dbg, &netState->vm))
        goto fail;
    if (!handleIncoming(&netState->vm, &netState->dbg))
        goto fail;

    return true;

fail:
    jdwpCloseConnection(netState);
    return false;
}

/*
 * Connect to the VM.
 */
bool jdwpConnectToVm(NetState* netState)
{
    struct sockaddr_in addr;
    int sock = -1;

    sock = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (sock < 0) {
        fprintf(stderr, "Socket create failed: %s\n", strerror(errno));
        goto fail;
    }

    addr.sin_family = AF_INET;
    addr.sin_addr = netState->vmAddr;
    addr.sin_port = htons(netState->vmPort);
    if (connect(sock, (struct sockaddr*) &addr, sizeof(addr)) != 0) {
        fprintf(stderr, "Connection to %s:%u failed: %s\n",
            inet_ntoa(addr.sin_addr), ntohs(addr.sin_port), strerror(errno));
        goto fail;
    }
    fprintf(stderr, "+++ connected to VM %s:%u\n",
        inet_ntoa(addr.sin_addr), ntohs(addr.sin_port));

    netState->vm.sock = sock;
    netState->vm.awaitingHandshake = true;
    netState->vm.inputCount = 0;

    setNoDelay(netState->vm.sock);
    return true;

fail:
    if (sock >= 0)
        close(sock);
    return false;
}

/*
 * Establish network connections and start things running.
 *
 * We wait for a new connection from the debugger.  When one arrives we
 * open a connection to the VM.  If one side or the other goes away, we
 * drop both ends and go back to listening.
 */
int run(const char* connectHost, int connectPort, int listenPort)
{
    NetState* state;

    state = jdwpNetStartup(listenPort, connectHost, connectPort);
    if (state == NULL)
        return -1;

    while (true) {
        if (!jdwpAcceptConnection(state))
            break;

        if (jdwpConnectToVm(state)) {
            while (true) {
                if (!jdwpProcessIncoming(state))
                    break;
            }
        }

        jdwpCloseConnection(state);
    }

    jdwpNetFree(state);

    return 0;
}
