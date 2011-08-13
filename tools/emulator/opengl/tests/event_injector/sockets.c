/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#ifdef __linux__ /* Recent versions of glibc only define EAI_NODATA, which is an
                    extension to the POSIX standard, if _GNU_SOURCE is defined. */
#  define _GNU_SOURCE 1
#endif

#include "sockets.h"
#include <fcntl.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
//#include "android/utils/path.h"
//#include "android/utils/debug.h"
//#include "android/utils/misc.h"
//#include "android/utils/system.h"

#define  D(...) ((void)0)

#ifdef _WIN32
#  define xxWIN32_LEAN_AND_MEAN
#  define _WIN32_WINNT 0x501
#  include <windows.h>
#  include <winsock2.h>
#  include <ws2tcpip.h>
#else /* !_WIN32 */
#  include <sys/ioctl.h>
#  include <sys/socket.h>
#  include <netinet/in.h>
#  include <netinet/tcp.h>
#  include <netdb.h>
#  if HAVE_UNIX_SOCKETS
#    include <sys/un.h>
#    ifndef UNIX_PATH_MAX
#      define  UNIX_PATH_MAX  (sizeof(((struct sockaddr_un*)0)->sun_path)-1)
#    endif
#  endif
#endif /* !_WIN32 */

#define MIN(x,y) ({ typeof(x)  _x = (x); typeof(y)  _y = (y); _x <= _y ? _x : _y; })
#define AFREE(p)              free(p)
#define AARRAY_NEW(p,count)   (p) = malloc(sizeof(*(p))*(count))
#define AARRAY_NEW0(p,count)  (p) = calloc(sizeof(*(p)),(count))

/* QSOCKET_CALL is used to deal with the fact that EINTR happens pretty
 * easily in QEMU since we use SIGALRM to implement periodic timers
 */
#ifdef _WIN32
#  define  QSOCKET_CALL(_ret,_cmd)   \
    do { _ret = (_cmd); } while ( _ret < 0 && WSAGetLastError() == WSAEINTR )
#else
#  define  QSOCKET_CALL(_ret,_cmd)   \
    do { \
        errno = 0; \
        do { _ret = (_cmd); } while ( _ret < 0 && errno == EINTR ); \
    } while (0);
#endif

#ifdef _WIN32

#include <errno.h>

static int  winsock_error;

#define  WINSOCK_ERRORS_LIST \
    EE(WSA_INVALID_HANDLE,EINVAL,"invalid handle") \
    EE(WSA_NOT_ENOUGH_MEMORY,ENOMEM,"not enough memory") \
    EE(WSA_INVALID_PARAMETER,EINVAL,"invalid parameter") \
    EE(WSAEINTR,EINTR,"interrupted function call") \
    EE(WSAEALREADY,EALREADY,"operation already in progress") \
    EE(WSAEBADF,EBADF,"bad file descriptor") \
    EE(WSAEACCES,EACCES,"permission denied") \
    EE(WSAEFAULT,EFAULT,"bad address") \
    EE(WSAEINVAL,EINVAL,"invalid argument") \
    EE(WSAEMFILE,EMFILE,"too many opened files") \
    EE(WSAEWOULDBLOCK,EWOULDBLOCK,"resource temporarily unavailable") \
    EE(WSAEINPROGRESS,EINPROGRESS,"operation now in progress") \
    EE(WSAEALREADY,EAGAIN,"operation already in progress") \
    EE(WSAENOTSOCK,EBADF,"socket operation not on socket") \
    EE(WSAEDESTADDRREQ,EDESTADDRREQ,"destination address required") \
    EE(WSAEMSGSIZE,EMSGSIZE,"message too long") \
    EE(WSAEPROTOTYPE,EPROTOTYPE,"wrong protocol type for socket") \
    EE(WSAENOPROTOOPT,ENOPROTOOPT,"bad protocol option") \
    EE(WSAEADDRINUSE,EADDRINUSE,"address already in use") \
    EE(WSAEADDRNOTAVAIL,EADDRNOTAVAIL,"cannot assign requested address") \
    EE(WSAENETDOWN,ENETDOWN,"network is down") \
    EE(WSAENETUNREACH,ENETUNREACH,"network unreachable") \
    EE(WSAENETRESET,ENETRESET,"network dropped connection on reset") \
    EE(WSAECONNABORTED,ECONNABORTED,"software caused connection abort") \
    EE(WSAECONNRESET,ECONNRESET,"connection reset by peer") \
    EE(WSAENOBUFS,ENOBUFS,"no buffer space available") \
    EE(WSAEISCONN,EISCONN,"socket is already connected") \
    EE(WSAENOTCONN,ENOTCONN,"socket is not connected") \
    EE(WSAESHUTDOWN,ESHUTDOWN,"cannot send after socket shutdown") \
    EE(WSAETOOMANYREFS,ETOOMANYREFS,"too many references") \
    EE(WSAETIMEDOUT,ETIMEDOUT,"connection timed out") \
    EE(WSAECONNREFUSED,ECONNREFUSED,"connection refused") \
    EE(WSAELOOP,ELOOP,"cannot translate name") \
    EE(WSAENAMETOOLONG,ENAMETOOLONG,"name too long") \
    EE(WSAEHOSTDOWN,EHOSTDOWN,"host is down") \
    EE(WSAEHOSTUNREACH,EHOSTUNREACH,"no route to host") \

typedef struct {
    int          winsock;
    int          unix;
    const char*  string;
} WinsockError;

static const WinsockError  _winsock_errors[] = {
#define  EE(w,u,s)   { w, u, s },
    WINSOCK_ERRORS_LIST
#undef   EE
    { -1, -1, NULL }
};

/* this function reads the latest winsock error code and updates
 * errno to a matching value. It also returns the new value of
 * errno.
 */
static int
_fix_errno( void )
{
    const WinsockError*  werr = _winsock_errors;
    int                  unix = EINVAL;  /* generic error code */

    winsock_error = WSAGetLastError();

    for ( ; werr->string != NULL; werr++ ) {
        if (werr->winsock == winsock_error) {
            unix = werr->unix;
            break;
        }
    }
    errno = unix;
    return -1;
}

static int
_set_errno( int  code )
{
    winsock_error = -1;
    errno         = code;
    return -1;
}

/* this function returns a string describing the latest Winsock error */
const char*
_errno_str(void)
{
    const WinsockError*  werr   = _winsock_errors;
    const char*          result = NULL;

    for ( ; werr->string; werr++ ) {
        if (werr->winsock == winsock_error) {
            result = werr->string;
            break;
        }
    }

    if (result == NULL) {
        result = "Unknown socket error";
    }
    return result;
}
#else
static int
_fix_errno( void )
{
    return -1;
}

static int
_set_errno( int  code )
{
    errno = code;
    return -1;
}
#endif

/* socket types */

static int
socket_family_to_bsd( SocketFamily  family )
{
    switch (family) {
    case SOCKET_INET: return AF_INET;
    case SOCKET_IN6:  return AF_INET6;
#if HAVE_UNIX_SOCKETS
    case SOCKET_UNIX: return AF_LOCAL;
#endif
    default: return -1;
    }
}

static int
socket_type_to_bsd( SocketType  type )
{
    switch (type) {
        case SOCKET_DGRAM:  return SOCK_DGRAM;
        case SOCKET_STREAM: return SOCK_STREAM;
        default: return 0;
    }
}

static SocketType
socket_type_from_bsd( int  type )
{
    switch (type) {
        case SOCK_DGRAM:  return SOCKET_DGRAM;
        case SOCK_STREAM: return SOCKET_STREAM;
        default:          return (SocketType) SOCKET_UNSPEC;
    }
}

#if 0
static int
socket_type_check( SocketType  type )
{
    return (type == SOCKET_DGRAM || type == SOCKET_STREAM);
}
#endif

typedef union {
    struct sockaddr     sa[1];
    struct sockaddr_in  in[1];
#if HAVE_IN6_SOCKETS
    struct sockaddr_in6 in6[1];
#endif
#if HAVE_UNIX_SOCKETS
    struct sockaddr_un  un[1];
#endif
} sockaddr_storage;

/* socket addresses */

void
sock_address_init_inet( SockAddress*  a, uint32_t  ip, uint16_t  port )
{
    a->family         = SOCKET_INET;
    a->u.inet.port    = port;
    a->u.inet.address = ip;
}

void
sock_address_init_in6 ( SockAddress*  a, const uint8_t*  ip6[16], uint16_t  port )
{
    a->family = SOCKET_IN6;
    a->u.in6.port = port;
    memcpy( a->u.in6.address, ip6, sizeof(a->u.in6.address) );
}

void
sock_address_init_unix( SockAddress*  a, const char*  path )
{
    a->family       = SOCKET_UNIX;
    a->u._unix.path  = strdup(path ? path : "");
    a->u._unix.owner = 1;
}

void  sock_address_done( SockAddress*  a )
{
    if (a->family == SOCKET_UNIX && a->u._unix.owner) {
        a->u._unix.owner = 0;
        free((char*)a->u._unix.path);
    }
}

static char*
format_char( char*  buf, char*  end, int  c )
{
    if (buf < end) {
        if (buf+1 == end) {
            *buf++ = 0;
        } else {
            *buf++ = (char) c;
            *buf    = 0;
        }
    }
    return buf;
}

static char*
format_str( char*  buf, char*  end, const char*  str )
{
    int  len   = strlen(str);
    int  avail = end - buf;

    if (len > avail)
        len = avail;

    memcpy( buf, str, len );
    buf += len;

    if (buf == end)
        buf[-1] = 0;
    else
        buf[0] = 0;

    return buf;
}

static char*
format_unsigned( char*  buf, char*  end, unsigned  val )
{
    char  temp[16];
    int   nn;

    for ( nn = 0; val != 0; nn++ ) {
        int  rem = val % 10;
        temp[nn] = '0'+rem;
        val /= 10;
    }

    if (nn == 0)
        temp[nn++] = '0';

    while (nn > 0)
        buf = format_char(buf, end, temp[--nn]);

    return buf;
}

static char*
format_hex( char*  buf, char*  end, unsigned  val, int  ndigits )
{
    int   shift = 4*ndigits;
    static const char   hex[16] = "0123456789abcdef";

    while (shift >= 0) {
        buf = format_char(buf, end, hex[(val >> shift) & 15]);
        shift -= 4;
    }
    return buf;
}

static char*
format_ip4( char*  buf, char*  end, uint32_t  ip )
{
    buf = format_unsigned( buf, end, (unsigned)(ip >> 24) );
    buf = format_char( buf, end, '.');
    buf = format_unsigned( buf, end, (unsigned)((ip >> 16) & 255));
    buf = format_char( buf, end, '.');
    buf = format_unsigned( buf, end, (unsigned)((ip >> 8) & 255));
    buf = format_char( buf, end, '.');
    buf = format_unsigned( buf, end, (unsigned)(ip & 255));
    return buf;
}

static char*
format_ip6( char*  buf, char*  end, const uint8_t*  ip6 )
{
    int  nn;
    for (nn = 0; nn < 8; nn++) {
        int  val = (ip6[0] << 16) | ip6[1];
        ip6 += 2;
        if (nn > 0)
            buf = format_char(buf, end, ':');
        if (val == 0)
            continue;
        buf  = format_hex(buf, end, val, 4);
    }
    return buf;
}

const char*
sock_address_to_string( const SockAddress*  a )
{
    static char buf0[PATH_MAX];
    char *buf = buf0, *end = buf + sizeof(buf0);

    switch (a->family) {
    case SOCKET_INET:
        buf = format_ip4( buf, end, a->u.inet.address );
        buf = format_char( buf, end, ':' );
        buf = format_unsigned( buf, end, (unsigned) a->u.inet.port );
        break;

    case SOCKET_IN6:
        buf = format_ip6( buf, end, a->u.in6.address );
        buf = format_char( buf, end, ':' );
        buf = format_unsigned( buf, end, (unsigned) a->u.in6.port );
        break;

    case SOCKET_UNIX:
        buf = format_str( buf, end, a->u._unix.path );
        break;

    default:
        return NULL;
    }

    return buf0;
}

int
sock_address_equal( const SockAddress*  a, const SockAddress*  b )
{
    if (a->family != b->family)
        return 0;

    switch (a->family) {
    case SOCKET_INET:
        return (a->u.inet.address == b->u.inet.address &&
                a->u.inet.port    == b->u.inet.port);

    case SOCKET_IN6:
        return (!memcmp(a->u.in6.address, b->u.in6.address, 16) &&
                a->u.in6.port == b->u.in6.port);

    case SOCKET_UNIX:
        return (!strcmp(a->u._unix.path, b->u._unix.path));

    default:
        return 0;
    }
}

int
sock_address_get_port( const SockAddress*  a )
{
    switch (a->family) {
    case SOCKET_INET:
        return a->u.inet.port;
    case SOCKET_IN6:
        return a->u.in6.port;
    default:
        return -1;
    }
}

void
sock_address_set_port( SockAddress*  a, uint16_t  port )
{
    switch (a->family) {
    case SOCKET_INET:
        a->u.inet.port = port;
        break;
    case SOCKET_IN6:
        a->u.in6.port = port;
        break;
    default:
        ;
    }
}

const char*
sock_address_get_path( const SockAddress*  a )
{
    if (a->family == SOCKET_UNIX)
        return a->u._unix.path;
    else
        return NULL;
}

int
sock_address_get_ip( const SockAddress*  a )
{
    if (a->family == SOCKET_INET)
        return a->u.inet.address;

    return -1;
}

#if 0
char*
bufprint_sock_address( char*  p, char*  end, const SockAddress*  a )
{
    switch (a->family) {
    case SOCKET_INET:
        {
            uint32_t  ip = a->u.inet.address;

            return bufprint( p, end, "%d.%d.%d.%d:%d",
                         (ip >> 24) & 255, (ip >> 16) & 255,
                         (ip >> 8) & 255, ip & 255,
                         a->u.inet.port );
        }
    case SOCKET_IN6:
        {
            int             nn     = 0;
            const char*     column = "";
            const uint8_t*  tab    = a->u.in6.address;
            for (nn = 0; nn < 16; nn += 2) {
                p = bufprint(p, end, "%s%04x", column, (tab[n] << 8) | tab[n+1]);
                column = ":";
            }
            return bufprint(p, end, ":%d", a->u.in6.port);
        }
    case SOCKET_UNIX:
        {
            return bufprint(p, end, "%s", a->u._unix.path);
        }
    default:
        return p;
    }
}
#endif

static int
sock_address_to_bsd( const SockAddress*  a, sockaddr_storage*  paddress, socklen_t  *psize )
{
    switch (a->family) {
    case SOCKET_INET:
        {
            struct sockaddr_in*  dst = paddress->in;

            *psize = sizeof(*dst);

            memset( paddress, 0, *psize );

            dst->sin_family      = AF_INET;
            dst->sin_port        = htons(a->u.inet.port);
            dst->sin_addr.s_addr = htonl(a->u.inet.address);
        }
        break;

#if HAVE_IN6_SOCKETS
    case SOCKET_IN6:
        {
            struct sockaddr_in6*  dst = paddress->in6;

            *psize = sizeof(*dst);

            memset( paddress, 0, *psize );

            dst->sin6_family = AF_INET6;
            dst->sin6_port   = htons(a->u.in6.port);
            memcpy( dst->sin6_addr.s6_addr, a->u.in6.address, 16 );
        }
        break;
#endif /* HAVE_IN6_SOCKETS */

#if HAVE_UNIX_SOCKETS
    case SOCKET_UNIX:
        {
            int                  slen = strlen(a->u._unix.path);
            struct sockaddr_un*  dst = paddress->un;

            if (slen >= (int)UNIX_PATH_MAX)
                return -1;

            memset( dst, 0, sizeof(*dst) );

            dst->sun_family = AF_LOCAL;
            memcpy( dst->sun_path, a->u._unix.path, slen );
            dst->sun_path[slen] = 0;

            *psize = (char*)&dst->sun_path[slen+1] - (char*)dst;
        }
        break;
#endif /* HAVE_UNIX_SOCKETS */

    default:
        return _set_errno(EINVAL);
    }

    return 0;
}

static int
sock_address_from_bsd( SockAddress*  a, const void*  from, size_t  fromlen )
{
    switch (((struct sockaddr *)from)->sa_family) {
    case AF_INET:
        {
           const struct sockaddr_in*  src = from;

            if (fromlen < sizeof(*src))
                return _set_errno(EINVAL);

            a->family         = SOCKET_INET;
            a->u.inet.port    = ntohs(src->sin_port);
            a->u.inet.address = ntohl(src->sin_addr.s_addr);
        }
        break;

#ifdef HAVE_IN6_SOCKETS
    case AF_INET6:
        {
            const struct sockaddr_in6*  src = from;

            if (fromlen < sizeof(*src))
                return _set_errno(EINVAL);

            a->family     = SOCKET_IN6;
            a->u.in6.port = ntohs(src->sin6_port);
            memcpy(a->u.in6.address, src->sin6_addr.s6_addr, 16);
        }
        break;
#endif

#ifdef HAVE_UNIX_SOCKETS
    case AF_LOCAL:
        {
            const struct sockaddr_un*  src = from;
            char*                end;

            if (fromlen < sizeof(*src))
                return _set_errno(EINVAL);

            /* check that the path is zero-terminated */
            end = memchr(src->sun_path, 0, UNIX_PATH_MAX);
            if (end == NULL)
                return _set_errno(EINVAL);

            a->family = SOCKET_UNIX;
            a->u._unix.owner = 1;
            a->u._unix.path  = strdup(src->sun_path);
        }
        break;
#endif

    default:
        return _set_errno(EINVAL);
    }
    return 0;
}


int
sock_address_init_resolve( SockAddress*  a, const char*  hostname, uint16_t  port, int  preferIn6 )
{
    struct addrinfo   hints[1];
    struct addrinfo*  res;
    int                ret;

    memset(hints, 0, sizeof(hints));
    hints->ai_family   = preferIn6 ? AF_INET6 : AF_UNSPEC;

    ret = getaddrinfo(hostname, NULL, hints, &res);
    if (ret != 0) {
        int  err;

        switch (ret) {
        case EAI_AGAIN:  /* server is down */
        case EAI_FAIL:   /* server is sick */
            err = EHOSTDOWN;
            break;

#ifdef EAI_NODATA
        case EAI_NODATA:
#endif
        case EAI_NONAME:
            err = ENOENT;
            break;

        case EAI_MEMORY:
            err = ENOMEM;
            break;

        default:
            err = EINVAL;
        }
        return _set_errno(err);
    }

    /* Parse the returned list of addresses. */
    {
        struct addrinfo*  res_ipv4 = NULL;
        struct addrinfo*  res_ipv6 = NULL;
        struct addrinfo*  r;

       /* If preferIn6 is false, we stop on the first IPv4 address,
        * otherwise, we stop on the first IPv6 one
        */
        for (r = res; r != NULL; r = r->ai_next) {
            if (r->ai_family == AF_INET && res_ipv4 == NULL) {
                res_ipv4 = r;
                if (!preferIn6)
                    break;
            }
            else if (r->ai_family == AF_INET6 && res_ipv6 == NULL) {
                res_ipv6 = r;
                if (preferIn6)
                    break;
            }
        }

        /* Select the best address in 'r', which will be NULL
         * if there is no corresponding address.
         */
        if (preferIn6) {
            r = res_ipv6;
            if (r == NULL)
                r = res_ipv4;
        } else {
            r = res_ipv4;
            if (r == NULL)
                r = res_ipv6;
        }

        if (r == NULL) {
            ret = _set_errno(ENOENT);
            goto Exit;
        }

        /* Convert to a SockAddress */
        ret = sock_address_from_bsd( a, r->ai_addr, r->ai_addrlen );
        if (ret < 0)
            goto Exit;
    }

    /* need to set the port */
    switch (a->family) {
    case SOCKET_INET: a->u.inet.port = port; break;
    case SOCKET_IN6:  a->u.in6.port  = port; break;
    default: ;
    }

Exit:
    freeaddrinfo(res);
    return ret;
}

/* The Winsock headers for mingw lack some definitions */
#ifndef AI_ADDRCONFIG
#  define  AI_ADDRCONFIG  0
#endif

SockAddress**
sock_address_list_create( const char*  hostname,
                          const char*  port,
                          unsigned     flags )
{
    SockAddress**    list = NULL;
    SockAddress*     addr;
    int              nn, count, ret;
    struct addrinfo  ai, *res, *e;

    memset(&ai, 0, sizeof(ai));
    ai.ai_flags   |= AI_ADDRCONFIG;
    ai.ai_family   = PF_UNSPEC;

    if (flags & SOCKET_LIST_FORCE_INET)
        ai.ai_family = PF_INET;
    else if (flags & SOCKET_LIST_FORCE_IN6)
        ai.ai_family = PF_INET6;

    if (flags & SOCKET_LIST_PASSIVE)
        ai.ai_flags |= AI_PASSIVE;
    else
        ai.ai_flags |= AI_CANONNAME;

    if (flags & SOCKET_LIST_DGRAM)
        ai.ai_socktype = SOCK_DGRAM;

    while (1) {
        struct addrinfo  hints = ai;

        ret = getaddrinfo(hostname, port, &hints, &res);
        if (ret == 0)
            break;

        switch (ret) {
#ifdef EAI_ADDRFAMILY
        case EAI_ADDRFAMILY:
#endif
        case EAI_NODATA:
            _set_errno(ENOENT);
            break;
        case EAI_FAMILY:
            _set_errno(EAFNOSUPPORT);
            break;
        case EAI_AGAIN:
            _set_errno(EAGAIN);
            break;
#ifdef EAI_SYSTEM
        case EAI_SYSTEM:
            if (errno == EINTR)
                continue;
            break;
#endif
        default:
            _set_errno(EINVAL);
        }
        return NULL;
    }

    /* allocate result list */
    for (count = 0, e = res; e != NULL; e = e->ai_next)
        count += 1;

    AARRAY_NEW(list, count+1);
    AARRAY_NEW(addr, count);

    for (nn = 0, e = res; e != NULL; e = e->ai_next) {

        ret = sock_address_from_bsd(addr, e->ai_addr, e->ai_addrlen);
        if (ret < 0)
            continue;

        list[nn++] = addr++;
    }
    list[nn] = NULL;
    freeaddrinfo(res);
    return list;
}

SockAddress**
sock_address_list_create2(const char* host_and_port, unsigned flags )
{
    char host_name[512];
    const char* actual_host_name = "localhost";
    // Parse host and port name.
    const char* port_name = strchr(host_and_port, ':');
    if (port_name != NULL) {
        int to_copy = MIN((int)sizeof(host_name)-1, port_name - host_and_port);
        if (to_copy != 0) {
            memcpy(host_name, host_and_port, to_copy);
            host_name[to_copy] = '\0';
            actual_host_name = host_name;
            port_name++;
        } else {
            return NULL;
        }
    } else {
        port_name = host_and_port;
    }
    // Make sure that port_name is not empty.
    if (port_name[0] == '\0') {
        return NULL;
    }
    return sock_address_list_create(actual_host_name, port_name, flags);
}

void
sock_address_list_free( SockAddress**  list )
{
    int  nn;
    SockAddress*  addr;

    if (list == NULL)
        return;

    addr = list[0];
    for (nn = 0; list[nn] != NULL; nn++) {
        sock_address_done(list[nn]);
        list[nn] = NULL;
    }
    AFREE(addr);
    AFREE(list);
}

int
sock_address_get_numeric_info( SockAddress*  a,
                               char*         host,
                               size_t        hostlen,
                               char*         serv,
                               size_t        servlen )
{
    struct sockaddr*  saddr;
    socklen_t         slen;
    int               ret;

    switch (a->family) {
    case SOCKET_INET:
        saddr = (struct sockaddr*) &a->u.inet.address;
        slen  = sizeof(a->u.inet.address);
        break;

#if HAVE_IN6_SOCKET
    case SOCKET_IN6:
        saddr = (struct sockaddr*) &a->u.in6.address;
        slen  = sizeof(a->u.in6.address);
        break;
#endif
    default:
        return _set_errno(EINVAL);
    }

    ret = getnameinfo( saddr, slen, host, hostlen, serv, servlen,
                       NI_NUMERICHOST | NI_NUMERICSERV );

    switch (ret) {
    case 0:
        break;
    case EAI_AGAIN:
        ret = EAGAIN;
        break;
    default:
        ret = EINVAL;
    }
    return ret;
}

int
socket_create( SocketFamily  family, SocketType  type )
{
    int   ret;
    int   sfamily = socket_family_to_bsd(family);
    int   stype   = socket_type_to_bsd(type);

    if (sfamily < 0 || stype < 0) {
        return _set_errno(EINVAL);
    }

    QSOCKET_CALL(ret, socket(sfamily, stype, 0));
    if (ret < 0)
        return _fix_errno();

    return ret;
}


int
socket_create_inet( SocketType  type )
{
    return socket_create( SOCKET_INET, type );
}

#if HAVE_IN6_SOCKETS
int
socket_create_in6 ( SocketType  type )
{
    return socket_create( SOCKET_IN6, type );
}
#endif

#if HAVE_UNIX_SOCKETS
int
socket_create_unix( SocketType  type )
{
    return socket_create( SOCKET_UNIX, type );
}
#endif

int  socket_can_read(int  fd)
{
#ifdef _WIN32
    unsigned long  opt;

    if (ioctlsocket(fd, FIONREAD, &opt) < 0)
        return 0;

    return opt;
#else
    int  opt;

    if (ioctl(fd, FIONREAD, &opt) < 0)
        return 0;

    return opt;
#endif
}

#define   SOCKET_CALL(cmd)  \
    int  ret; \
    QSOCKET_CALL(ret, (cmd)); \
    if (ret < 0) \
        return _fix_errno(); \
    return ret; \

int
socket_send(int  fd, const void*  buf, int  buflen)
{
    SOCKET_CALL(send(fd, buf, buflen, 0))
}

int
socket_send_oob( int  fd, const void*  buf, int  buflen )
{
    SOCKET_CALL(send(fd, buf, buflen, MSG_OOB));
}

int
socket_sendto(int  fd, const void*  buf, int  buflen, const SockAddress*  to)
{
    sockaddr_storage  sa;
    socklen_t         salen;

    if (sock_address_to_bsd(to, &sa, &salen) < 0)
        return -1;

    SOCKET_CALL(sendto(fd, buf, buflen, 0, sa.sa, salen));
}

int
socket_recv(int  fd, void*  buf, int  len)
{
    SOCKET_CALL(recv(fd, buf, len, 0));
}

int
socket_recvfrom(int  fd, void*  buf, int  len, SockAddress*  from)
{
    sockaddr_storage  sa;
    socklen_t         salen = sizeof(sa);
    int               ret;

    QSOCKET_CALL(ret,recvfrom(fd,buf,len,0,sa.sa,&salen));
    if (ret < 0)
        return _fix_errno();

    if (sock_address_from_bsd(from, &sa, salen) < 0)
        return -1;

    return ret;
}

int
socket_connect( int  fd, const SockAddress*  address )
{
    sockaddr_storage  addr;
    socklen_t         addrlen;

    if (sock_address_to_bsd(address, &addr, &addrlen) < 0)
        return -1;

    SOCKET_CALL(connect(fd,addr.sa,addrlen));
}

int
socket_bind( int  fd, const SockAddress*  address )
{
    sockaddr_storage  addr;
    socklen_t         addrlen;

    if (sock_address_to_bsd(address, &addr, &addrlen) < 0)
        return -1;

    SOCKET_CALL(bind(fd, addr.sa, addrlen));
}

int
socket_get_address( int  fd, SockAddress*  address )
{
    sockaddr_storage  addr;
    socklen_t         addrlen = sizeof(addr);
    int               ret;

    QSOCKET_CALL(ret, getsockname(fd, addr.sa, &addrlen));
    if (ret < 0)
        return _fix_errno();

    return sock_address_from_bsd(address, &addr, addrlen);
}

int
socket_get_peer_address( int  fd, SockAddress*  address )
{
    sockaddr_storage  addr;
    socklen_t         addrlen = sizeof(addr);
    int               ret;

    QSOCKET_CALL(ret, getpeername(fd, addr.sa, &addrlen));
    if (ret < 0)
        return _fix_errno();

    return sock_address_from_bsd(address, &addr, addrlen);
}

int
socket_listen( int  fd, int  backlog )
{
    SOCKET_CALL(listen(fd, backlog));
}

int
socket_accept( int  fd, SockAddress*  address )
{
    sockaddr_storage  addr;
    socklen_t         addrlen = sizeof(addr);
    int               ret;

    QSOCKET_CALL(ret, accept(fd, addr.sa, &addrlen));
    if (ret < 0)
        return _fix_errno();

    if (address) {
        if (sock_address_from_bsd(address, &addr, addrlen) < 0) {
            socket_close(ret);
            return -1;
        }
    }
    return ret;
}

static int
socket_getoption(int  fd, int  domain, int  option, int  defaut)
{
    int  ret;
    while (1) {
#ifdef _WIN32
        DWORD opt = (DWORD)-1;
#else
        int  opt  = -1;
#endif
        socklen_t  optlen = sizeof(opt);
        ret = getsockopt(fd, domain, option, (char*)&opt, &optlen);
        if (ret == 0)
            return (int)opt;
        if (errno != EINTR)
            return defaut;
    }
#undef OPT_CAST
}


SocketType socket_get_type(int fd)
{
    int   so_type = socket_getoption(fd, SOL_SOCKET, SO_TYPE, -1);
    return socket_type_from_bsd(so_type);
}

int socket_set_nonblock(int fd)
{
#ifdef _WIN32
    unsigned long opt = 1;
    return ioctlsocket(fd, FIONBIO, &opt);
#else
    int   flags = fcntl(fd, F_GETFL);
    return fcntl(fd, F_SETFL, flags | O_NONBLOCK);
#endif
}

int socket_set_blocking(int fd)
{
#ifdef _WIN32
    unsigned long opt = 0;
    return ioctlsocket(fd, FIONBIO, &opt);
#else
    int   flags = fcntl(fd, F_GETFL);
    return fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);
#endif
}

static int
socket_setoption(int  fd, int  domain, int  option, int  _flag)
{
#ifdef _WIN32
    DWORD  flag = (DWORD) _flag;
#else
    int    flag = _flag;
#endif
    return setsockopt( fd, domain, option, (const char*)&flag, sizeof(flag) );
}

int socket_set_xreuseaddr(int  fd)
{
#ifdef _WIN32
   /* on Windows, SO_REUSEADDR is used to indicate that several programs can
    * bind to the same port. this is completely different from the Unix
    * semantics. instead of SO_EXCLUSIVEADDR to ensure that explicitely prevent
    * this.
    */
    return socket_setoption(fd, SOL_SOCKET, SO_EXCLUSIVEADDRUSE, 1);
#else
    return socket_setoption(fd, SOL_SOCKET, SO_REUSEADDR, 1);
#endif
}


int socket_set_oobinline(int  fd)
{
    return socket_setoption(fd, SOL_SOCKET, SO_OOBINLINE, 1);
}


int  socket_set_nodelay(int  fd)
{
    return socket_setoption(fd, IPPROTO_TCP, TCP_NODELAY, 1);
}

int socket_set_ipv6only(int  fd)
{
/* IPV6_ONLY is only supported since Vista on Windows,
 * and the Mingw headers lack its definition anyway.
 */
#if defined(_WIN32) && !defined(IPV6_V6ONLY)
	return 0;
#else
    return socket_setoption(fd, IPPROTO_IPV6, IPV6_V6ONLY, 1);
#endif
}


int socket_get_error(int fd)
{
    return socket_getoption(fd, SOL_SOCKET, SO_ERROR, -1);
}

#ifdef _WIN32
#include <stdlib.h>

static void socket_cleanup(void)
{
    WSACleanup();
}

int socket_init(void)
{
    WSADATA Data;
    int ret, err;

    ret = WSAStartup(MAKEWORD(2,2), &Data);
    if (ret != 0) {
        err = WSAGetLastError();
        return -1;
    }
    atexit(socket_cleanup);
    return 0;
}

#else /* !_WIN32 */

int socket_init(void)
{
   return 0;   /* nothing to do on Unix */
}

#endif /* !_WIN32 */

#ifdef _WIN32

void
socket_close( int  fd )
{
    int  old_errno = errno;

    shutdown( fd, SD_BOTH );
    /* we want to drain the socket before closing it */
    //qemu_set_fd_handler( fd, socket_close_handler, NULL, (void*)fd );
    closesocket(fd);

    errno = old_errno;
}

#else /* !_WIN32 */

#include <unistd.h>

void
socket_close( int  fd )
{
    int  old_errno = errno;

    shutdown( fd, SHUT_RDWR );
    close( fd );

    errno = old_errno;
}

#endif /* !_WIN32 */


static int
socket_bind_server( int  s, const SockAddress*  to, SocketType  type )
{
    socket_set_xreuseaddr(s);

    if (socket_bind(s, to) < 0) {
        D("could not bind server socket address %s: %s",
          sock_address_to_string(to), errno_str);
        goto FAIL;
    }

    if (type == SOCKET_STREAM) {
        if (socket_listen(s, 4) < 0) {
            D("could not listen server socket %s: %s",
              sock_address_to_string(to), errno_str);
            goto FAIL;
        }
    }
    return  s;

FAIL:
    socket_close(s);
    return -1;
}


static int
socket_connect_client( int  s, const SockAddress*  to )
{
    if (socket_connect(s, to) < 0) {
        D( "could not connect client socket to %s: %s\n",
           sock_address_to_string(to), errno_str );
        socket_close(s);
        return -1;
    }

    socket_set_nonblock( s );
    return s;
}


static int
socket_in_server( int  address, int  port, SocketType  type )
{
    SockAddress  addr;
    int          s;

    sock_address_init_inet( &addr, address, port );
    s = socket_create_inet( type );
    if (s < 0)
        return -1;

    return socket_bind_server( s, &addr, type );
}


static int
socket_in_client( SockAddress*  to, SocketType  type )
{
    int  s;

    s = socket_create_inet( type );
    if (s < 0) return -1;

    return socket_connect_client( s, to );
}


int
socket_loopback_server( int  port, SocketType  type )
{
    return socket_in_server( SOCK_ADDRESS_INET_LOOPBACK, port, type );
}

int
socket_loopback_client( int  port, SocketType  type )
{
    SockAddress  addr;

    sock_address_init_inet( &addr, SOCK_ADDRESS_INET_LOOPBACK, port );
    return socket_in_client( &addr, type );
}


int
socket_network_client( const char*  host, int  port, SocketType  type )
{
    SockAddress  addr;

    if (sock_address_init_resolve( &addr, host, port, 0) < 0)
        return -1;

    return socket_in_client( &addr, type );
}


int
socket_anyaddr_server( int  port, SocketType  type )
{
    return socket_in_server( SOCK_ADDRESS_INET_ANY, port, type );
}

int
socket_accept_any( int  server_fd )
{
    int  fd;

    QSOCKET_CALL(fd, accept( server_fd, NULL, 0 ));
    if (fd < 0) {
        D( "could not accept client connection from fd %d: %s",
           server_fd, errno_str );
        return -1;
    }

    /* set to non-blocking */
    socket_set_nonblock( fd );
    return fd;
}


#if HAVE_UNIX_SOCKETS

int
socket_unix_server( const char*  name, SocketType  type )
{
    SockAddress   addr;
    int           s, ret;

    s = socket_create_unix( type );
    if (s < 0)
        return -1;

    sock_address_init_unix( &addr, name );

    do {
        ret = unlink( name );
    } while (ret < 0 && errno == EINTR);

    ret = socket_bind_server( s, &addr, type );

    sock_address_done( &addr );
    return ret;
}

int
socket_unix_client( const char*  name, SocketType  type )
{
    SockAddress   addr;
    int           s, ret;

    s = socket_create_unix(type);
    if (s < 0)
        return -1;

    sock_address_init_unix( &addr, name );

    ret =  socket_connect_client( s, &addr );

    sock_address_done( &addr );
    return ret;
}

#endif /* HAVE_UNIX_SOCKETS */



int
socket_pair(int *fd1, int *fd2)
{
#ifndef _WIN32
    int   fds[2];
    int   ret = socketpair(AF_UNIX, SOCK_STREAM, 0, fds);

    if (!ret) {
        socket_set_nonblock(fds[0]);
        socket_set_nonblock(fds[1]);
        *fd1 = fds[0];
        *fd2 = fds[1];
    }
    return ret;
#else /* _WIN32 */
    /* on Windows, select() only works with network sockets, which
     * means we absolutely cannot use Win32 PIPEs to implement
     * socket pairs with the current event loop implementation.
     * We're going to do like Cygwin: create a random pair
     * of localhost TCP sockets and connect them together
     */
    int                 s0, s1, s2, port;
    struct sockaddr_in  sockin;
    socklen_t           len;

    /* first, create the 'server' socket.
     * a port number of 0 means 'any port between 1024 and 5000.
     * see Winsock bind() documentation for details */
    s0 = socket_loopback_server( 0, SOCK_STREAM );
    if (s0 < 0)
        return -1;

    /* now connect a client socket to it, we first need to
     * extract the server socket's port number */
    len = sizeof sockin;
    if (getsockname(s0, (struct sockaddr*) &sockin, &len) < 0) {
        closesocket (s0);
        return -1;
    }

    port = ntohs(sockin.sin_port);
    s2   = socket_loopback_client( port, SOCK_STREAM );
    if (s2 < 0) {
        closesocket(s0);
        return -1;
    }

    /* we need to accept the connection on the server socket
     * this will create the second socket for the pair
     */
    len = sizeof sockin;
    s1  = accept(s0, (struct sockaddr*) &sockin, &len);
    if (s1 == INVALID_SOCKET) {
        closesocket (s0);
        closesocket (s2);
        return -1;
    }
    socket_set_nonblock(s1);

    /* close server socket */
    closesocket(s0);
    *fd1 = s1;
    *fd2 = s2;
    return 0;
#endif /* _WIN32 */
}



int
socket_mcast_inet_add_membership( int  s, uint32_t  ip )
{
    struct ip_mreq imr;

    imr.imr_multiaddr.s_addr = htonl(ip);
    imr.imr_interface.s_addr = htonl(INADDR_ANY);

    if ( setsockopt( s, IPPROTO_IP, IP_ADD_MEMBERSHIP,
                     (const char *)&imr,
                     sizeof(struct ip_mreq)) < 0 )
    {
        return _fix_errno();
    }
    return 0;
}

int
socket_mcast_inet_drop_membership( int  s, uint32_t  ip )
{
    struct ip_mreq imr;

    imr.imr_multiaddr.s_addr = htonl(ip);
    imr.imr_interface.s_addr = htonl(INADDR_ANY);

    if ( setsockopt( s, IPPROTO_IP, IP_DROP_MEMBERSHIP,
                     (const char *)&imr,
                     sizeof(struct ip_mreq)) < 0 )
    {
        return _fix_errno();
    }
    return 0;
}

int
socket_mcast_inet_set_loop( int  s, int  enabled )
{
    return socket_setoption( s, IPPROTO_IP, IP_MULTICAST_LOOP, !!enabled );
}

int
socket_mcast_inet_set_ttl( int  s, int  ttl )
{
    return socket_setoption( s, IPPROTO_IP, IP_MULTICAST_TTL, ttl );
}


char*
host_name( void )
{
    static char buf[256];  /* 255 is the max host name length supported by DNS */
    int         ret;

    QSOCKET_CALL(ret, gethostname(buf, sizeof(buf)));

    if (ret < 0)
        return "localhost";
    else
        return buf;
}
