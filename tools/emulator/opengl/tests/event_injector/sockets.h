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
/* headers to use the BSD sockets */
#ifndef ANDROID_SOCKET_H
#define ANDROID_SOCKET_H

#include <stddef.h>
#include <stdint.h>
#include <errno.h>

#ifdef __cplusplus
extern "C" {
#endif

/* we're going to hide the implementation details of sockets behind
 * a simple wrapper interface declared here.
 *
 * all socket operations set the global 'errno' variable on error.
 * this is unlike Winsock which instead modifies another internal
 * variable accessed through WSAGetLastError() and WSASetLastError()
 */

/* the wrapper will convert any Winsock error message into an errno
 * code for you. There are however a few standard Unix error codes
 * that are not defined by the MS C library headers, so we add them
 * here. We use the official Winsock error codes, which are documented
 * even though we don't want to include the Winsock headers
 */
#ifdef _WIN32
#  ifndef EINTR
#    define EINTR        10004
#  endif
#  ifndef EAGAIN
#    define EAGAIN       10035
#  endif
#  ifndef EWOULDBLOCK
#    define EWOULDBLOCK  EAGAIN
#  endif
#  ifndef EINPROGRESS
#    define EINPROGRESS  10036
#  endif
#  ifndef EALREADY
#    define EALREADY     10037
#  endif
#  ifndef EDESTADDRREQ
#    define EDESTADDRREQ 10039
#  endif
#  ifndef EMSGSIZE
#    define EMSGSIZE     10040
#  endif
#  ifndef EPROTOTYPE
#    define EPROTOTYPE   10041
#  endif
#  ifndef ENOPROTOOPT
#    define ENOPROTOOPT  10042
#  endif
#  ifndef EAFNOSUPPORT
#    define EAFNOSUPPORT 10047
#  endif
#  ifndef EADDRINUSE
#    define EADDRINUSE   10048
#  endif
#  ifndef EADDRNOTAVAIL
#    define EADDRNOTAVAIL 10049
#  endif
#  ifndef ENETDOWN
#    define ENETDOWN     10050
#  endif
#  ifndef ENETUNREACH
#    define ENETUNREACH  10051
#  endif
#  ifndef ENETRESET
#    define ENETRESET    10052
#  endif
#  ifndef ECONNABORTED
#    define ECONNABORTED 10053
#  endif
#  ifndef ECONNRESET
#    define ECONNRESET   10054
#  endif
#  ifndef ENOBUFS
#    define ENOBUFS      10055
#  endif
#  ifndef EISCONN
#    define EISCONN      10056
#  endif
#  ifndef ENOTCONN
#    define ENOTCONN     10057
#  endif
#  ifndef ESHUTDOWN
#    define ESHUTDOWN     10058
#  endif
#  ifndef ETOOMANYREFS
#    define ETOOMANYREFS  10059
#  endif
#  ifndef ETIMEDOUT
#    define ETIMEDOUT     10060
#  endif
#  ifndef ECONNREFUSED
#    define ECONNREFUSED  10061
#  endif
#  ifndef ELOOP
#    define ELOOP         10062
#  endif
#  ifndef EHOSTDOWN
#    define EHOSTDOWN     10064
#  endif
#  ifndef EHOSTUNREACH
#    define EHOSTUNREACH  10065
#  endif
#endif /* _WIN32 */

/* Define 'errno_str' as a handy macro to return the string
 * corresponding to a given errno code. On Unix, this is
 * equivalent to strerror(errno), but on Windows, this will
 * take care of Winsock-originated errors as well.
 */
#ifdef _WIN32
  extern const char*  _errno_str(void);
#  define  errno_str   _errno_str()
#else
#  define  errno_str   strerror(errno)
#endif

/* always enable IPv6 sockets for now.
 * the QEMU internal router is not capable of
 * supporting them, but we plan to replace it
 * with something better in the future.
 */
#define  HAVE_IN6_SOCKETS   1

/* Unix sockets are not available on Win32 */
#ifndef _WIN32
#  define  HAVE_UNIX_SOCKETS  1
#endif

/* initialize the socket sub-system. this must be called before
 * using any of the declarations below.
 */
int  socket_init( void );

/* return the name of the current host */
char*  host_name( void );

/* supported socket types */
typedef enum {
    SOCKET_DGRAM = 0,
    SOCKET_STREAM
} SocketType;

/* supported socket families */
typedef enum {
    SOCKET_UNSPEC,
    SOCKET_INET,
    SOCKET_IN6,
    SOCKET_UNIX
} SocketFamily;

/* Generic socket address structure. Note that for Unix
 * sockets, the path is stored in a heap-allocated block,
 * unless the 'owner' field is cleared. If this is the case,
 */
typedef struct {
    SocketFamily  family;
    union {
        struct {
            uint16_t   port;
            uint32_t   address;
        } inet;
        struct {
            uint16_t   port;
            uint8_t    address[16];
        } in6;
        struct {
            int          owner;
            const char*  path;
        } _unix;
    } u;
} SockAddress;

#define  SOCK_ADDRESS_INET_ANY       0x00000000
#define  SOCK_ADDRESS_INET_LOOPBACK  0x7f000001

/* initialize a new IPv4 socket address, the IP address and port are
 * in host endianess.
 */
void  sock_address_init_inet( SockAddress*  a, uint32_t  ip, uint16_t  port );

/* Initialize an IPv6 socket address, the address is in network order
 * and the port in host endianess.
 */
#if HAVE_IN6_SOCKETS
void  sock_address_init_in6 ( SockAddress*  a, const uint8_t*  ip6[16], uint16_t  port );
#endif

/* Intialize a Unix socket address, this will copy the 'path' string into the
 * heap. You need to call sock_address_done() to release the copy
 */
#if HAVE_UNIX_SOCKETS
void  sock_address_init_unix( SockAddress*  a, const char*  path );
#endif

/* Finalize a socket address, only needed for now for Unix addresses */
void  sock_address_done( SockAddress*  a );

int   sock_address_equal( const SockAddress*  a, const SockAddress*  b );

/* return a static string describing the address */
const char*  sock_address_to_string( const SockAddress*  a );

static __inline__
SocketFamily  sock_address_get_family( const SockAddress*  a )
{
    return a->family;
}

/* return the port number of a given socket address, or -1 if it's a Unix one */
int   sock_address_get_port( const SockAddress*  a );

/* set the port number of a given socket address, don't do anything for Unix ones */
void  sock_address_set_port( SockAddress*  a, uint16_t  port );

/* return the path of a given Unix socket, returns NULL for non-Unix ones */
const char*  sock_address_get_path( const SockAddress*  a );

/* return the inet address, or -1 if it's not SOCKET_INET */
int   sock_address_get_ip( const SockAddress*  a );

/* bufprint a socket address into a human-readable string */
char* bufprint_sock_address( char*  p, char*  end, const SockAddress*  a );

/* resolve a hostname or decimal IPv4/IPv6 address into a socket address.
 * returns 0 on success, or -1 on failure. Note that the values or errno
 * set by this function are the following:
 *
 *   EINVAL    : invalid argument
 *   EHOSTDOWN : could not reach DNS server
 *   ENOENT    : no host with this name, or host doesn't have any IP address
 *   ENOMEM    : not enough memory to perform request
 */
int   sock_address_init_resolve( SockAddress*  a,
                                 const char*   hostname,
                                 uint16_t      port,
                                 int           preferIn6 );

int  sock_address_get_numeric_info( SockAddress*  a,
                                    char*         host,
                                    size_t        hostlen,
                                    char*         serv,
                                    size_t        servlen );

/* Support for listing all socket addresses of a given host */
enum {
    SOCKET_LIST_PASSIVE    = (1 << 0),
    SOCKET_LIST_FORCE_INET = (1 << 1),
    SOCKET_LIST_FORCE_IN6  = (1 << 2),
    SOCKET_LIST_DGRAM      = (1 << 3),
};

/* resolve a host and service/port name into a list of SockAddress objects.
 * returns a NULL-terminated array of SockAddress pointers on success,
 * or NULL in case of failure, with the value of errno set to one of the
 * following:
 *
 *    EINVAL    : invalid argument
 *    EHOSTDOWN : could not reach DNS server
 *    ENOENT    : no host with this name, or host doesn't have IP address
 *    ENOMEM    : not enough memory to perform request
 *
 * other system-level errors can also be set depending on the host sockets
 * implementation.
 *
 * This function loops on EINTR so the caller shouldn't have to check for it.
 */
SockAddress**  sock_address_list_create( const char*  hostname,
                                         const char*  port,
                                         unsigned     flags );

/* resolve a string containing host and port name into a list of SockAddress
 * objects. Parameter host_and_port should be in format [host:]port, where
 * 'host' addresses the machine and must be resolvable into an IP address, and
 * 'port' is a decimal numeric value for the port. 'host' is optional, and if
 * ommited, localhost will be used.
 * returns a NULL-terminated array of SockAddress pointers on success,
 * or NULL in case of failure, with the value of errno set to one of the
 * following:
 *
 *    EINVAL    : invalid argument
 *    EHOSTDOWN : could not reach DNS server
 *    ENOENT    : no host with this name, or host doesn't have IP address
 *    ENOMEM    : not enough memory to perform request
 *
 * other system-level errors can also be set depending on the host sockets
 * implementation.
 *
 * This function loops on EINTR so the caller shouldn't have to check for it.
 */
SockAddress**  sock_address_list_create2(const char*  host_and_port,
                                         unsigned     flags );

void sock_address_list_free( SockAddress**  list );

/* create a new socket, return the socket number of -1 on failure */
int  socket_create( SocketFamily  family, SocketType  type );

/* create a new socket intended for IPv4 communication. returns the socket number,
 * or -1 on failure.
 */
int   socket_create_inet( SocketType  type );

/* create a new socket intended for IPv6 communication. returns the socket number,
 * or -1 on failure.
 */
#if HAVE_IN6_SOCKETS
int   socket_create_in6 ( SocketType  type );
#endif

/* create a unix/local domain socket. returns the socket number,
 * or -1 on failure.
 */
#if HAVE_UNIX_SOCKETS
int   socket_create_unix( SocketType  type );
#endif

/* return the type of a given socket */
SocketType  socket_get_type(int  fd);

/* set SO_REUSEADDR on Unix, SO_EXCLUSIVEADDR on Windows */
int  socket_set_xreuseaddr(int  fd);

/* set socket in non-blocking mode */
int  socket_set_nonblock(int fd);

/* set socket in blocking mode */
int  socket_set_blocking(int fd);

/* disable the TCP Nagle algorithm for lower latency */
int  socket_set_nodelay(int fd);

/* send OOB data inline for this socket */
int  socket_set_oobinline(int  fd);

/* force listening to IPv6 interfaces only */
int  socket_set_ipv6only(int  fd);

/* retrieve last socket error code */
int  socket_get_error(int  fd);

/* close an opened socket. Note that this is unlike the Unix 'close' because:
 * - it will properly shutdown the socket in the background
 * - it does not modify errno
 */
void  socket_close( int  fd );

/* the following functions are equivalent to the BSD sockets ones
 */
int   socket_recv    ( int  fd, void*  buf, int  buflen );
int   socket_recvfrom( int  fd, void*  buf, int  buflen, SockAddress*  from );

int   socket_send  ( int  fd, const void*  buf, int  buflen );
int   socket_send_oob( int  fd, const void*  buf, int  buflen );
int   socket_sendto( int  fd, const void*  buf, int  buflen, const SockAddress*  to );

int   socket_connect( int  fd, const SockAddress*  address );
int   socket_bind( int  fd, const SockAddress*  address );
int   socket_get_address( int  fd, SockAddress*  address );
int   socket_get_peer_address( int  fd, SockAddress*  address );
int   socket_listen( int  fd, int  backlog );
int   socket_accept( int  fd, SockAddress*  address );

/* returns the number of bytes that can be read from a socket */
int   socket_can_read( int  fd );

/* this call creates a pair of non-blocking sockets connected
 * to each other. this is equivalent to calling the Unix function:
 * socketpair(AF_LOCAL,SOCK_STREAM,0,&fds)
 *
 * on Windows, this will use a pair of TCP loopback sockets instead
 * returns 0 on success, -1 on error.
 */
int  socket_pair(int  *fd1, int *fd2);

/* create a server socket listening on the host's loopback interface */
int  socket_loopback_server( int  port, SocketType  type );

/* connect to a port on the host's loopback interface */
int  socket_loopback_client( int  port, SocketType  type );

/* create a server socket listening to a Unix domain path */
#if HAVE_UNIX_SOCKETS
int  socket_unix_server( const char*  name, SocketType  type );
#endif

/* create a Unix sockets and connects it to a Unix server */
#if HAVE_UNIX_SOCKETS
int  socket_unix_client( const char*  name, SocketType  type );
#endif

/* create an IPv4 client socket and connect it to a given host */
int  socket_network_client( const char*  host, int  port, SocketType  type );

/* create an IPv4 socket and binds it to a given port of the host's interface */
int  socket_anyaddr_server( int  port, SocketType  type );

/* accept a connection from the host's any interface, return the new socket
 * descriptor or -1 */
int  socket_accept_any( int  server_fd );


int  socket_mcast_inet_add_membership( int  s, uint32_t  ip );
int  socket_mcast_inet_drop_membership( int  s, uint32_t  ip );
int  socket_mcast_inet_set_loop( int  s, int  enabled );
int  socket_mcast_inet_set_ttl( int  s, int  ttl );

#ifdef __cplusplus
}
#endif

#endif /* ANDROID_SOCKET_H */
