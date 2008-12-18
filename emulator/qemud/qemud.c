#include <stdint.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <termios.h>
#include <cutils/sockets.h>

/*
 *  the qemud program is only used within the Android emulator as a bridge
 *  between the emulator program and the emulated system. it really works as
 *  a simple stream multiplexer that works as follows:
 *
 *    - qemud communicates with the emulator program through a single serial
 *      port, whose name is passed through a kernel boot parameter
 *      (e.g. android.qemud=ttyS1)
 *
 *    - qemud setups one or more unix local stream sockets in the
 *      emulated system each one of these represent a different communication
 *      'channel' between the emulator program and the emulated system.
 *
 *      as an example, one channel is used for the emulated GSM modem
 *      (AT command channel), another channel is used for the emulated GPS,
 *      etc...
 *
 *    - the protocol used on the serial connection is pretty simple:
 *
 *          offset    size    description
 *              0       4     4-char hex string giving the payload size
 *              4       2     2-char hex string giving the destination or
 *                            source channel
 *              6       n     the message payload
 *
 *      for emulator->system messages, the 'channel' index indicates
 *      to which channel the payload must be sent
 *
 *      for system->emulator messages, the 'channel' index indicates from
 *      which channel the payload comes from.
 *
 *   - a special channel index (0) is used to communicate with the qemud
 *     program directly from the emulator. this is used for the following
 *     commands:  (content of the payload):
 *
 *        request:  connect:<name>
 *        answer:   ok:connect:<name>:XX       // succesful name lookup
 *        answer:   ko:connect:bad name        // failed lookup
 *
 *           the emulator queries the index of a given channel given
 *           its human-readable name. the answer contains a 2-char hex
 *           string for the channel index.
 *
 *           not all emulated systems may need the same communication
 *           channels, so this function may fail.
 *
 *     any invalid request will get an answer of:
 *
 *           ko:unknown command
 *
 *
 *  here's a diagram of how things work:
 *
 *
 *                                                  _________
 *                        _____________   creates  |         |
 *         ________      |             |==========>| Channel |--*--
 *        |        |---->| Multiplexer |           |_________|
 *   --*--| Serial |     |_____________|               || creates
 *        |________|            |                 _____v___
 *             A                +--------------->|         |
 *             |                                 | Client  |--*--
 *             +---------------------------------|_________|
 *
 *  which really means that:
 *
 *    - the multiplexer creates one Channel object per control socket qemud
 *      handles (e.g. /dev/socket/qemud_gsm, /dev/socket/qemud_gps)
 *
 *    - each Channel object has a numerical index that is >= 1, and waits
 *      for client connection. it will create a Client object when this
 *      happens
 *
 *    - the Serial object receives packets from the serial port and sends them
 *      to the multiplexer
 *
 *    - the multiplexer tries to find a channel the packet is addressed to,
 *      and will send the packet to all clients that correspond to it
 *
 *    - when a Client receives data, it sends it directly to the Serial object
 *
 *    - there are two kinds of Channel objects:
 *
 *         CHANNEL_BROADCAST :: used for emulator -> clients broadcasts only
 *
 *         CHANNEL_DUPLEX    :: used for bidirectional communication with the
 *                              emulator, with only *one* client allowed per
 *                              duplex channel
 */

#define  DEBUG  0

#if DEBUG
#  define LOG_TAG  "qemud"
#  include <cutils/log.h>
#  define  D(...)   LOGD(__VA_ARGS__)
#else
#  define  D(...)  ((void)0)
#endif

/** UTILITIES
 **/

static void
fatal( const char*  fmt, ... )
{
    va_list  args;
    va_start(args, fmt);
    fprintf(stderr, "PANIC: ");
    vfprintf(stderr, fmt, args);
    fprintf(stderr, "\n" );
    va_end(args);
    exit(1);
}

static void*
xalloc( size_t   sz )
{
    void*  p;

    if (sz == 0)
        return NULL;

    p = malloc(sz);
    if (p == NULL)
        fatal( "not enough memory" );

    return p;
}

#define  xnew(p)   (p) = xalloc(sizeof(*(p)))

static void*
xalloc0( size_t  sz )
{
    void*  p = xalloc(sz);
    memset( p, 0, sz );
    return p;
}

#define  xnew0(p)   (p) = xalloc0(sizeof(*(p)))

#define  xfree(p)    (free((p)), (p) = NULL)

static void*
xrealloc( void*  block, size_t  size )
{
    void*  p = realloc( block, size );

    if (p == NULL && size > 0)
        fatal( "not enough memory" );

    return p;
}

#define  xrenew(p,count)  (p) = xrealloc((p),sizeof(*(p))*(count))

static int
hex2int( const uint8_t*  data, int  len )
{
    int  result = 0;
    while (len > 0) {
        int       c = *data++;
        unsigned  d;

        result <<= 4;
        do {
            d = (unsigned)(c - '0');
            if (d < 10)
                break;

            d = (unsigned)(c - 'a');
            if (d < 6) {
                d += 10;
                break;
            }

            d = (unsigned)(c - 'A');
            if (d < 6) {
                d += 10;
                break;
            }

            return -1;
        }
        while (0);

        result |= d;
        len    -= 1;
    }
    return  result;
}


static void
int2hex( int  value, uint8_t*  to, int  width )
{
    int  nn = 0;
    static const char hexchars[16] = "0123456789abcdef";

    for ( --width; width >= 0; width--, nn++ ) {
        to[nn] = hexchars[(value >> (width*4)) & 15];
    }
}

static int
fd_read(int  fd, void*  to, int  len)
{
    int  ret;

    do {
        ret = read(fd, to, len);
    } while (ret < 0 && errno == EINTR);

    return ret;
}

static int
fd_write(int  fd, const void*  from, int  len)
{
    int  ret;

    do {
        ret = write(fd, from, len);
    } while (ret < 0 && errno == EINTR);

    return ret;
}

static void
fd_setnonblock(int  fd)
{
    int  ret, flags;

    do {
        flags = fcntl(fd, F_GETFD);
    } while (flags < 0 && errno == EINTR);

    if (flags < 0) {
        fatal( "%s: could not get flags for fd %d: %s",
               __FUNCTION__, fd, strerror(errno) );
    }

    do {
        ret = fcntl(fd, F_SETFD, flags | O_NONBLOCK);
    } while (ret < 0 && errno == EINTR);

    if (ret < 0) {
        fatal( "%s: could not set fd %d to non-blocking: %s",
               __FUNCTION__, fd, strerror(errno) );
    }
}

/** FD EVENT LOOP
 **/

#include <sys/epoll.h>

#define  MAX_CHANNELS  16
#define  MAX_EVENTS    (MAX_CHANNELS+1)  /* each channel + the serial fd */

typedef void (*EventFunc)( void*  user, int  events );

enum {
    HOOK_PENDING = (1 << 0),
    HOOK_CLOSING = (1 << 1),
};

typedef struct {
    int        fd;
    int        wanted;
    int        events;
    int        state;
    void*      ev_user;
    EventFunc  ev_func;
} LoopHook;

typedef struct {
    int                  epoll_fd;
    int                  num_fds;
    int                  max_fds;
    struct epoll_event*  events;
    LoopHook*            hooks;
} Looper;

static void
looper_init( Looper*  l )
{
    l->epoll_fd = epoll_create(4);
    l->num_fds  = 0;
    l->max_fds  = 0;
    l->events   = NULL;
    l->hooks    = NULL;
}

static void
looper_done( Looper*  l )
{
    xfree(l->events);
    xfree(l->hooks);
    l->max_fds = 0;
    l->num_fds = 0;

    close(l->epoll_fd);
    l->epoll_fd  = -1;
}

static LoopHook*
looper_find( Looper*  l, int  fd )
{
    LoopHook*  hook = l->hooks;
    LoopHook*  end  = hook + l->num_fds;

    for ( ; hook < end; hook++ ) {
        if (hook->fd == fd)
            return hook;
    }
    return NULL;
}

static void
looper_grow( Looper*  l )
{
    int  old_max = l->max_fds;
    int  new_max = old_max + (old_max >> 1) + 4;
    int  n;

    xrenew( l->events, new_max );
    xrenew( l->hooks,  new_max );
    l->max_fds = new_max;

    /* now change the handles to all events */
    for (n = 0; n < l->num_fds; n++) {
        struct epoll_event ev;
        LoopHook*          hook = l->hooks + n;

        ev.events   = hook->wanted;
        ev.data.ptr = hook;
        epoll_ctl( l->epoll_fd, EPOLL_CTL_MOD, hook->fd, &ev );
    }
}

static void
looper_add( Looper*  l, int  fd, EventFunc  func, void*  user )
{
    struct epoll_event  ev;
    LoopHook*           hook;

    if (l->num_fds >= l->max_fds)
        looper_grow(l);

    hook = l->hooks + l->num_fds;

    hook->fd      = fd;
    hook->ev_user = user;
    hook->ev_func = func;
    hook->state   = 0;
    hook->wanted  = 0;
    hook->events  = 0;

    fd_setnonblock(fd);

    ev.events   = 0;
    ev.data.ptr = hook;
    epoll_ctl( l->epoll_fd, EPOLL_CTL_ADD, fd, &ev );

    l->num_fds += 1;
}

static void
looper_del( Looper*  l, int  fd )
{
    LoopHook*  hook = looper_find( l, fd );

    if (!hook) {
        D( "%s: invalid fd: %d", __FUNCTION__, fd );
        return;
    }
    /* don't remove the hook yet */
    hook->state |= HOOK_CLOSING;

    epoll_ctl( l->epoll_fd, EPOLL_CTL_DEL, fd, NULL );
}

static void
looper_enable( Looper*  l, int  fd, int  events )
{
    LoopHook*  hook = looper_find( l, fd );

    if (!hook) {
        D("%s: invalid fd: %d", __FUNCTION__, fd );
        return;
    }

    if (events & ~hook->wanted) {
        struct epoll_event  ev;

        hook->wanted |= events;
        ev.events   = hook->wanted;
        ev.data.ptr = hook;

        epoll_ctl( l->epoll_fd, EPOLL_CTL_MOD, fd, &ev );
    }
}

static void
looper_disable( Looper*  l, int  fd, int  events )
{
    LoopHook*  hook = looper_find( l, fd );

    if (!hook) {
        D("%s: invalid fd: %d", __FUNCTION__, fd );
        return;
    }

    if (events & hook->wanted) {
        struct epoll_event  ev;

        hook->wanted &= ~events;
        ev.events   = hook->wanted;
        ev.data.ptr = hook;

        epoll_ctl( l->epoll_fd, EPOLL_CTL_MOD, fd, &ev );
    }
}

static void
looper_loop( Looper*  l )
{
    for (;;) {
        int  n, count;

        do {
            count = epoll_wait( l->epoll_fd, l->events, l->num_fds, -1 );
        } while (count < 0 && errno == EINTR);

        if (count < 0) {
            D("%s: error: %s", __FUNCTION__, strerror(errno) );
            return;
        }

        /* mark all pending hooks */
        for (n = 0; n < count; n++) {
            LoopHook*  hook = l->events[n].data.ptr;
            hook->state  = HOOK_PENDING;
            hook->events = l->events[n].events;
        }

        /* execute hook callbacks. this may change the 'hooks'
         * and 'events' array, as well as l->num_fds, so be careful */
        for (n = 0; n < l->num_fds; n++) {
            LoopHook*  hook = l->hooks + n;
            if (hook->state & HOOK_PENDING) {
                hook->state &= ~HOOK_PENDING;
                hook->ev_func( hook->ev_user, hook->events );
            }
        }

        /* now remove all the hooks that were closed by
         * the callbacks */
        for (n = 0; n < l->num_fds;) {
            LoopHook*  hook = l->hooks + n;

            if (!(hook->state & HOOK_CLOSING)) {
                n++;
                continue;
            }

            hook[0]     = l->hooks[l->num_fds-1];
            l->num_fds -= 1;
        }
    }
}

/** PACKETS
 **/

typedef struct Packet   Packet;

/* we want to ensure that Packet is no more than a single page */
#define  MAX_PAYLOAD  (4096-16-6)

struct Packet {
    Packet*   next;
    int       len;
    int       channel;
    uint8_t   data[ MAX_PAYLOAD ];
};

static Packet*   _free_packets;

static Packet*
packet_alloc(void)
{
    Packet*  p = _free_packets;
    if (p != NULL) {
        _free_packets = p->next;
    } else {
        xnew(p);
    }
    p->next = NULL;
    p->len  = 0;
    p->channel = -1;
    return p;
}

static void
packet_free( Packet*  *ppacket )
{
    Packet*  p = *ppacket;
    if (p) {
        p->next       = _free_packets;
        _free_packets = p;
        *ppacket = NULL;
    }
}

static Packet*
packet_dup( Packet*  p )
{
    Packet*  p2 = packet_alloc();

    p2->len     = p->len;
    p2->channel = p->channel;
    memcpy(p2->data, p->data, p->len);
    return p2;
}

/** PACKET RECEIVER
 **/

typedef void (*PostFunc) ( void*  user, Packet*  p );
typedef void (*CloseFunc)( void*  user );

typedef struct {
    PostFunc   post;
    CloseFunc  close;
    void*      user;
} Receiver;

static __inline__ void
receiver_post( Receiver*  r, Packet*  p )
{
    r->post( r->user, p );
}

static __inline__ void
receiver_close( Receiver*  r )
{
    r->close( r->user );
}


/** FD HANDLERS
 **
 ** these are smart listeners that send incoming packets to a receiver
 ** and can queue one or more outgoing packets and send them when possible
 **/

typedef struct FDHandler {
    int          fd;
    Looper*      looper;
    Receiver     receiver[1];
    int          out_pos;
    Packet*      out_first;
    Packet**     out_ptail;

} FDHandler;


static void
fdhandler_done( FDHandler*  f )
{
    /* get rid of unsent packets */
    if (f->out_first) {
        Packet*  p;
        while ((p = f->out_first) != NULL) {
            f->out_first = p->next;
            packet_free(&p);
        }
    }

    /* get rid of file descriptor */
    if (f->fd >= 0) {
        looper_del( f->looper, f->fd );
        close(f->fd);
        f->fd = -1;
    }
    f->looper = NULL;
}


static void
fdhandler_enqueue( FDHandler*  f, Packet*  p )
{
    Packet*  first = f->out_first;

    p->next         = NULL;
    f->out_ptail[0] = p;
    f->out_ptail    = &p->next;

    if (first == NULL) {
        f->out_pos = 0;
        looper_enable( f->looper, f->fd, EPOLLOUT );
    }
}


static void
fdhandler_event( FDHandler*  f, int  events )
{
   int  len;

    if (events & EPOLLIN) {
        Packet*  p = packet_alloc();
        int      len;

        if ((len = fd_read(f->fd, p->data, MAX_PAYLOAD)) < 0) {
            D("%s: can't recv: %s", __FUNCTION__, strerror(errno));
            packet_free(&p);
        } else {
            p->len     = len;
            p->channel = -101;  /* special debug value */
            receiver_post( f->receiver, p );
        }
    }

    /* in certain cases, it's possible to have both EPOLLIN and
     * EPOLLHUP at the same time. This indicates that there is incoming
     * data to read, but that the connection was nonetheless closed
     * by the sender. Be sure to read the data before closing
     * the receiver to avoid packet loss.
     */
    if (events & (EPOLLHUP|EPOLLERR)) {
        /* disconnection */
        D("%s: disconnect on fd %d", __FUNCTION__, f->fd);
        receiver_close( f->receiver );
        return;
    }

    if (events & EPOLLOUT && f->out_first) {
        Packet*  p = f->out_first;
        int      avail, len;

        avail = p->len - f->out_pos;
        if ((len = fd_write(f->fd, p->data + f->out_pos, avail)) < 0) {
            D("%s: can't send: %s", __FUNCTION__, strerror(errno));
        } else {
            f->out_pos += len;
            if (f->out_pos >= p->len) {
                f->out_pos   = 0;
                f->out_first = p->next;
                packet_free(&p);
                if (f->out_first == NULL) {
                    f->out_ptail = &f->out_first;
                    looper_disable( f->looper, f->fd, EPOLLOUT );
                }
            }
        }
    }
}


static void
fdhandler_init( FDHandler*      f,
                int             fd,
                Looper*         looper,
                Receiver*       receiver )
{
    f->fd          = fd;
    f->looper      = looper;
    f->receiver[0] = receiver[0];
    f->out_first   = NULL;
    f->out_ptail   = &f->out_first;
    f->out_pos     = 0;

    looper_add( looper, fd, (EventFunc) fdhandler_event, f );
    looper_enable( looper, fd, EPOLLIN );
}


static void
fdhandler_accept_event( FDHandler*  f, int  events )
{
    if (events & EPOLLIN) {
        /* this is an accept - send a dummy packet to the receiver */
        Packet*  p = packet_alloc();

        D("%s: accepting on fd %d", __FUNCTION__, f->fd);
        p->data[0] = 1;
        p->len     = 1;
        receiver_post( f->receiver, p );
    }

    if (events & (EPOLLHUP|EPOLLERR)) {
        /* disconnecting !! */
        D("%s: closing fd %d", __FUNCTION__, f->fd);
        receiver_close( f->receiver );
        return;
    }
}


static void
fdhandler_init_accept( FDHandler*  f,
                       int         fd,
                       Looper*     looper,
                       Receiver*   receiver )
{
    f->fd          = fd;
    f->looper      = looper;
    f->receiver[0] = receiver[0];

    looper_add( looper, fd, (EventFunc) fdhandler_accept_event, f );
    looper_enable( looper, fd, EPOLLIN );
}

/** CLIENTS
 **/

typedef struct Client {
    struct Client*   next;
    struct Client**  pref;
    int              channel;
    FDHandler        fdhandler[1];
    Receiver         receiver[1];
} Client;

static Client*   _free_clients;

static void
client_free( Client*  c )
{
    c->pref[0] = c->next;
    c->next    = NULL;
    c->pref    = &c->next;

    fdhandler_done( c->fdhandler );
    free(c);
}

static void
client_receive( Client*  c, Packet*  p )
{
    p->channel = c->channel;
    receiver_post( c->receiver, p );
}

static void
client_send( Client*  c, Packet*  p )
{
    fdhandler_enqueue( c->fdhandler, p );
}

static void
client_close( Client*  c )
{
    D("disconnecting client on fd %d", c->fdhandler->fd);
    client_free(c);
}

static Client*
client_new( int         fd,
            int         channel,
            Looper*     looper,
            Receiver*   receiver )
{
    Client*   c;
    Receiver  recv;

    xnew(c);

    c->next = NULL;
    c->pref = &c->next;
    c->channel = channel;
    c->receiver[0] = receiver[0];

    recv.user  = c;
    recv.post  = (PostFunc)  client_receive;
    recv.close = (CloseFunc) client_close;

    fdhandler_init( c->fdhandler, fd, looper, &recv );
    return c;
}

static void
client_link( Client*  c, Client**  plist )
{
    c->next  = plist[0];
    c->pref  = plist;
    plist[0] = c;
}


/** CHANNELS
 **/

typedef enum {
    CHANNEL_BROADCAST = 0,
    CHANNEL_DUPLEX,

    CHANNEL_MAX  /* do not remove */

} ChannelType;

#define  CHANNEL_CONTROL   0

typedef struct Channel {
    struct Channel*     next;
    struct Channel**    pref;
    FDHandler           fdhandler[1];
    ChannelType         ctype;
    const char*         name;
    int                 index;
    Receiver            receiver[1];
    Client*             clients;
} Channel;

static void
channel_free( Channel*  c )
{
    while (c->clients)
        client_free(c->clients);

    c->pref[0] = c->next;
    c->pref    = &c->next;
    c->next    = NULL;

    fdhandler_done( c->fdhandler );
    free(c);
}

static void
channel_close( Channel*  c )
{
    D("closing channel '%s' on fd %d", c->name, c->fdhandler->fd);
    channel_free(c);
}


static void
channel_accept( Channel*  c, Packet*  p )
{
    int   fd;
    struct sockaddr  from;
    socklen_t        fromlen = sizeof(from);

    /* get rid of dummy packet (see fdhandler_event_accept) */
    packet_free(&p);

    do {
        fd = accept( c->fdhandler->fd, &from, &fromlen );
    } while (fd < 0 && errno == EINTR);

    if (fd >= 0) {
        Client*  client;

        /* DUPLEX channels can only have one client at a time */
        if (c->ctype == CHANNEL_DUPLEX && c->clients != NULL) {
            D("refusing client connection on duplex channel '%s'", c->name);
            close(fd);
            return;
        }
        client = client_new( fd, c->index, c->fdhandler->looper, c->receiver );
        client_link( client, &c->clients );
        D("new client for channel '%s' on fd %d", c->name, fd);
    }
    else
        D("could not accept connection: %s", strerror(errno));
}


static Channel*
channel_new( int          fd,
             ChannelType  ctype,
             const char*  name,
             int          index,
             Looper*      looper,
             Receiver*    receiver )
{
    Channel*  c;
    Receiver  recv;

    xnew(c);

    c->next  = NULL;
    c->pref  = &c->next;
    c->ctype = ctype;
    c->name  = name;
    c->index = index;

    /* saved for future clients */
    c->receiver[0] = receiver[0];

    recv.user  = c;
    recv.post  = (PostFunc)  channel_accept;
    recv.close = (CloseFunc) channel_close;

    fdhandler_init_accept( c->fdhandler, fd, looper, &recv );
    listen( fd, 5 );

    return c;
}

static void
channel_link( Channel*  c, Channel** plist )
{
    c->next  = plist[0];
    c->pref  = plist;
    plist[0] = c;
}

static void
channel_send( Channel*  c, Packet*  p )
{
    Client*  client = c->clients;
    for ( ; client; client = client->next ) {
        Packet*  q = packet_dup(p);
        client_send( client, q );
    }
    packet_free( &p );
}


/* each packet is made of a 6 byte header followed by a payload
 * the header looks like:
 *
 *   offset   size    description
 *       0       4    a 4-char hex string for the size of the payload
 *       4       2    a 2-byte hex string for the channel number
 *       6       n    the payload itself
 */
#define  HEADER_SIZE    6
#define  LENGTH_OFFSET  0
#define  LENGTH_SIZE    4
#define  CHANNEL_OFFSET 4
#define  CHANNEL_SIZE   2

#define  CHANNEL_INDEX_NONE     0
#define  CHANNEL_INDEX_CONTROL  1

#define  TOSTRING(x)   _TOSTRING(x)
#define  _TOSTRING(x)  #x

/** SERIAL HANDLER
 **/

typedef struct Serial {
    FDHandler   fdhandler[1];
    Receiver    receiver[1];
    int         in_len;
    int         in_datalen;
    int         in_channel;
    Packet*     in_packet;
} Serial;

static void
serial_done( Serial*  s )
{
    packet_free(&s->in_packet);
    s->in_len     = 0;
    s->in_datalen = 0;
    s->in_channel = 0;
    fdhandler_done(s->fdhandler);
}

static void
serial_close( Serial*  s )
{
    fatal("unexpected serial port close !!");
}

/* receive packets from the serial port */
static void
serial_receive( Serial*  s, Packet*  p )
{
    int      rpos  = 0, rcount = p->len;
    Packet*  inp   = s->in_packet;
    int      inpos = s->in_len;

    //D("received from serial: %d bytes: '%.*s'", p->len, p->len, p->data);

    while (rpos < rcount)
    {
        int  avail = rcount - rpos;

        /* first, try to read the header */
        if (s->in_datalen == 0) {
            int  wanted = HEADER_SIZE - inpos;
            if (avail > wanted)
                avail = wanted;

            memcpy( inp->data + inpos, p->data + rpos, avail );
            inpos += avail;
            rpos  += avail;

            if (inpos == HEADER_SIZE) {
                s->in_datalen = hex2int( inp->data + LENGTH_OFFSET,  LENGTH_SIZE );
                s->in_channel = hex2int( inp->data + CHANNEL_OFFSET, CHANNEL_SIZE );

                if (s->in_datalen <= 0)
                    D("ignoring empty packet from serial port");

                //D("received %d bytes packet for channel %d", s->in_datalen, s->in_channel);
                inpos = 0;
            }
        }
        else /* then, populate the packet itself */
        {
            int   wanted = s->in_datalen - inpos;

            if (avail > wanted)
                avail = wanted;

            memcpy( inp->data + inpos, p->data + rpos, avail );
            inpos += avail;
            rpos  += avail;

            if (inpos == s->in_datalen) {
                if (s->in_channel < 0) {
                    D("ignoring %d bytes addressed to channel %d",
                       inpos, s->in_channel);
                } else {
                    inp->len     = inpos;
                    inp->channel = s->in_channel;
                    receiver_post( s->receiver, inp );
                    s->in_packet  = inp = packet_alloc();
                }
                s->in_datalen = 0;
                inpos         = 0;
            }
        }
    }
    s->in_len = inpos;
    packet_free(&p);
}


/* send a packet to the serial port */
static void
serial_send( Serial*  s, Packet*  p )
{
    Packet*  h = packet_alloc();

    //D("sending to serial %d bytes from channel %d: '%.*s'", p->len, p->channel, p->len, p->data);

    /* insert a small header before this packet */
    h->len = HEADER_SIZE;
    int2hex( p->len,     h->data + LENGTH_OFFSET,  LENGTH_SIZE );
    int2hex( p->channel, h->data + CHANNEL_OFFSET, CHANNEL_SIZE );

    fdhandler_enqueue( s->fdhandler, h );
    fdhandler_enqueue( s->fdhandler, p );
}


static void
serial_init( Serial*    s,
             int        fd,
             Looper*    looper,
             Receiver*  receiver )
{
    Receiver  recv;

    recv.user  = s;
    recv.post  = (PostFunc)  serial_receive;
    recv.close = (CloseFunc) serial_close;

    s->receiver[0] = receiver[0];

    fdhandler_init( s->fdhandler, fd, looper, &recv );
    s->in_len     = 0;
    s->in_datalen = 0;
    s->in_channel = 0;
    s->in_packet  = packet_alloc();
}

/**  GLOBAL MULTIPLEXER
 **/

typedef struct {
    Looper     looper[1];
    Serial     serial[1];
    Channel*   channels;
    uint16_t   channel_last;
} Multiplexer;

/* receive a packet from the serial port, send it to the relevant client/channel */
static void  multiplexer_receive_serial( Multiplexer*  m, Packet*  p );

static void
multiplexer_init( Multiplexer*  m, const char*  serial_dev )
{
    int       fd;
    Receiver  recv;

    looper_init( m->looper );

    fd = open(serial_dev, O_RDWR);
    if (fd < 0) {
        fatal( "%s: could not open '%s': %s", __FUNCTION__, serial_dev,
               strerror(errno) );
    }
    // disable echo on serial lines
    if ( !memcmp( serial_dev, "/dev/ttyS", 9 ) ) {
        struct termios  ios;
        tcgetattr( fd, &ios );
        ios.c_lflag = 0;  /* disable ECHO, ICANON, etc... */
        tcsetattr( fd, TCSANOW, &ios );
    }

    recv.user  = m;
    recv.post  = (PostFunc) multiplexer_receive_serial;
    recv.close = NULL;

    serial_init( m->serial, fd, m->looper, &recv );

    m->channels     = NULL;
    m->channel_last = CHANNEL_CONTROL+1;
}

static void
multiplexer_add_channel( Multiplexer*  m, int  fd, const char*  name, ChannelType  ctype )
{
    Channel*  c;
    Receiver  recv;

    /* send channel client data directly to the serial port */
    recv.user  = m->serial;
    recv.post  = (PostFunc) serial_send;
    recv.close = (CloseFunc) client_close;

    /* connect each channel directly to the serial port */
    c = channel_new( fd, ctype, name, m->channel_last, m->looper, &recv );
    channel_link( c, &m->channels );

    m->channel_last += 1;
    if (m->channel_last <= CHANNEL_CONTROL)
        m->channel_last += 1;
}


static void
multiplexer_done( Multiplexer*  m )
{
    while (m->channels)
        channel_close(m->channels);

    serial_done( m->serial );
    looper_done( m->looper );
}


static void
multiplexer_send_answer( Multiplexer*  m, Packet*  p, const char*  answer )
{
    p->len = strlen( answer );
    if (p->len >= MAX_PAYLOAD)
        p->len = MAX_PAYLOAD-1;

    memcpy( (char*)p->data, answer, p->len );
    p->channel = CHANNEL_CONTROL;

    serial_send( m->serial, p );
}


static void
multiplexer_handle_connect( Multiplexer*  m, Packet*  p, char*  name )
{
    int       n;
    Channel*  c;

    if (p->len >= MAX_PAYLOAD) {
        multiplexer_send_answer( m, p, "ko:connect:bad name" );
        return;
    }
    p->data[p->len] = 0;

    for (c = m->channels; c != NULL; c = c->next)
        if ( !strcmp(c->name, name) )
            break;

    if (c == NULL) {
        D("can't connect to unknown channel '%s'", name);
        multiplexer_send_answer( m, p, "ko:connect:bad name" );
        return;
    }

    p->channel = CHANNEL_CONTROL;
    p->len     = snprintf( (char*)p->data, MAX_PAYLOAD,
                       "ok:connect:%s:%02x", c->name, c->index );

    serial_send( m->serial, p );
}


static void
multiplexer_receive_serial( Multiplexer*  m, Packet*  p )
{
    Channel*  c = m->channels;

    /* check the destination channel index */
    if (p->channel != CHANNEL_CONTROL) {
        Channel*  c;

        for (c = m->channels; c; c = c->next ) {
            if (c->index == p->channel) {
                channel_send( c, p );
                break;
            }
        }
        if (c == NULL) {
            D("ignoring %d bytes packet for unknown channel index %d",
                p->len, p->channel );
            packet_free(&p);
        }
    }
    else  /* packet addressed to the control channel */
    {
        D("received control message:  '%.*s'", p->len, p->data);
        if (p->len > 8 && strncmp( (char*)p->data, "connect:", 8) == 0) {
            multiplexer_handle_connect( m, p, (char*)p->data + 8 );
        } else {
            /* unknown command */
            multiplexer_send_answer( m, p, "ko:unknown command" );
        }
        return;
    }
}


/** MAIN LOOP
 **/

static Multiplexer  _multiplexer[1];

#define  QEMUD_PREFIX  "qemud_"

static const struct { const char* name; ChannelType  ctype; }   default_channels[] = {
    { "gsm", CHANNEL_DUPLEX },       /* GSM AT command channel, used by commands/rild/rild.c */
    { "gps", CHANNEL_BROADCAST },    /* GPS NMEA commands, used by libs/hardware/qemu_gps.c  */
    { "control", CHANNEL_DUPLEX },   /* Used for power/leds/vibrator/etc... */
    { NULL, 0 }
};

int  main( void )
{
    Multiplexer*  m = _multiplexer;

   /* extract the name of our serial device from the kernel
    * boot options that are stored in /proc/cmdline
    */
#define  KERNEL_OPTION  "android.qemud="

    {
        char          buff[1024];
        int           fd, len;
        char*         p;
        char*         q;

        fd = open( "/proc/cmdline", O_RDONLY );
        if (fd < 0) {
            D("%s: can't open /proc/cmdline !!: %s", __FUNCTION__,
            strerror(errno));
            exit(1);
        }

        len = fd_read( fd, buff, sizeof(buff)-1 );
        close(fd);
        if (len < 0) {
            D("%s: can't read /proc/cmdline: %s", __FUNCTION__,
            strerror(errno));
            exit(1);
        }
        buff[len] = 0;

        p = strstr( buff, KERNEL_OPTION );
        if (p == NULL) {
            D("%s: can't find '%s' in /proc/cmdline",
            __FUNCTION__, KERNEL_OPTION );
            exit(1);
        }

        p += sizeof(KERNEL_OPTION)-1;  /* skip option */
        q  = p;
        while ( *q && *q != ' ' && *q != '\t' )
            q += 1;

        snprintf( buff, sizeof(buff), "/dev/%.*s", q-p, p );

        multiplexer_init( m, buff );
    }

    D("multiplexer inited, creating default channels");

    /* now setup all default channels */
    {
        int  nn;

        for (nn = 0; default_channels[nn].name != NULL; nn++) {
            char         control_name[32];
            int          fd;
            Channel*     chan;
            const char*  name  = default_channels[nn].name;
            ChannelType  ctype = default_channels[nn].ctype;

            snprintf(control_name, sizeof(control_name), "%s%s",
                     QEMUD_PREFIX, name);

            if ((fd = android_get_control_socket(control_name)) < 0) {
                D("couldn't get fd for control socket '%s'", name);
                continue;
            }
            D( "got control socket '%s' on fd %d", control_name, fd);
            multiplexer_add_channel( m, fd, name, ctype );
        }
    }

    D( "entering main loop");
    looper_loop( m->looper );
    D( "unexpected termination !!" );
    return 0;
}
