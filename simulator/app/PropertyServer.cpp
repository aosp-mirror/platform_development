//
// Copyright 2007 The Android Open Source Project
//
// Property sever.  Mimics behavior provided on the device by init(8) and
// some code built into libc.
//
    
// For compilers that support precompilation, include "wx/wx.h".
#include "wx/wxprec.h"
    
// Otherwise, include all standard headers
#ifndef WX_PRECOMP
# include "wx/wx.h"
#endif
#include "wx/image.h"
    
#include "PropertyServer.h"
#include "MyApp.h"
#include "Preferences.h"
#include "MainFrame.h"
#include "utils.h"

#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <assert.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>


using namespace android;

const char* PropertyServer::kPropCheckJni = "ro.kernel.android.checkjni";

/*
 * Destructor.
 */
PropertyServer::~PropertyServer(void)
{
    if (IsRunning()) {
        // TODO: cause thread to stop, then Wait for it
    }
    printf("Sim: in ~PropertyServer()\n");
}

/*
 * Create and run the thread.
 */
bool PropertyServer::StartThread(void)
{
    if (Create() != wxTHREAD_NO_ERROR) {
        fprintf(stderr, "Sim: ERROR: can't create PropertyServer thread\n");
        return false;
    }

    Run();
    return true;
}


/*
 * Clear out the list.
 */
void PropertyServer::ClearProperties(void)
{
    typedef List<Property>::iterator PropIter;

    for (PropIter pi = mPropList.begin(); pi != mPropList.end(); ++pi) {
        pi = mPropList.erase(pi);
    }
}

/*
 * Set default values for several properties.
 */
void PropertyServer::SetDefaultProperties(void)
{
    static const struct {
        const char* key;
        const char* value;
    } propList[] = {
        { "net.bt.name", "Android" },
        { "ro.kernel.mem", "60M" },
        { "ro.kernel.board_sardine.version", "4" },
        { "ro.kernel.console", "null" },
        { "ro.build.id", "engineering" },
        { "ro.build.date", "Wed Nov 28 07:44:14 PST 2007" },
        { "ro.build.date.utc", "1196264654" },
        { "ro.build.type", "eng" },
        { "ro.build.version.sdk", "8" },
        { "ro.build.version.codename", "Honeycomb" },
        { "ro.build.version.release", "Honeycomb" },
        { "ro.product.device", "simulator" /*"sooner"*/ },
        { "ro.product.brand", "generic" },
        { "ro.build.user", "fadden" },
        { "ro.build.host", "marathon" },
        { "ro.config.nocheckin", "yes" },
        { "ro.product.manufacturer", "" },
        { "ro.radio.use-ppp", "no" },
        { "ro.FOREGROUND_APP_ADJ", "0" },
        { "ro.VISIBLE_APP_ADJ", "1" },
        { "ro.PERCEPTIBLE_APP_ADJ", "2" },
        { "ro.HEAVY_WEIGHT_APP_ADJ", "3" },
        { "ro.SECONDARY_SERVER_ADJ", "2" },
        { "ro.HIDDEN_APP_MIN_ADJ", "7" },
        { "ro.CONTENT_PROVIDER_ADJ", "14" },
        { "ro.EMPTY_APP_ADJ", "15" },
        { "ro.FOREGROUND_APP_MEM", "1536" },
        { "ro.VISIBLE_APP_MEM", "2048" },
        { "ro.PERCEPTIBLE_APP_MEM", "4096" },
        { "ro.HEAVY_WEIGHT_APP_MEM", "4096" },
        { "ro.SECONDARY_SERVER_MEM", "4096" },
        { "ro.HIDDEN_APP_MEM", "8192" },
        { "ro.EMPTY_APP_MEM", "16384" },
        { "ro.HOME_APP_ADJ", "4" },
        { "ro.HOME_APP_MEM", "4096" },
        { "ro.BACKUP_APP_ADJ", "2" },
        { "ro.BACKUP_APP_MEM", "4096" },
        { "ro.PERCEPTIBLE_APP_ADJ", "2" },
        { "ro.PERCEPTIBLE_APP_MEM", "4096" },
        { "ro.HEAVY_WEIGHT_APP_ADJ", "3" },
        { "ro.HEAVY_WEIGHT_APP_MEM", "4096" },
        //{ "init.svc.adbd", "running" },       // causes ADB-JDWP
        { "init.svc.usbd", "running" },
        { "init.svc.debuggerd", "running" },
        { "init.svc.ril-daemon", "running" },
        { "init.svc.zygote", "running" },
        { "init.svc.runtime", "running" },
        { "init.svc.dbus", "running" },
        { "init.svc.pppd_gprs", "running" },
        { "adb.connected", "0" },
        /*
        { "status.battery.state", "Slow" },
        { "status.battery.level", "5" },
        { "status.battery.level_raw", "50" },
        { "status.battery.level_scale", "9" },
        */

        /* disable the annoying setup wizard */
        { "app.setupwizard.disable", "1" },

        /* Dalvik options, set by AndroidRuntime */
        { "dalvik.vm.stack-trace-file", "/data/anr/traces.txt" },
        //{ "dalvik.vm.execution-mode", "int:portable" },
        { "dalvik.vm.enableassertions", "all" },    // -ea
        { "dalvik.vm.dexopt-flags", "" },           // e.g. "v=a,o=v,m=n"
        { "dalvik.vm.deadlock-predict", "off" },    // -Xdeadlockpredict
        //{ "dalvik.vm.jniopts", "forcecopy" },       // -Xjniopts
        { "log.redirect-stdio", "false" },          // -Xlog-stdio

        /* SurfaceFlinger options */
        { "ro.sf.lcd_density", "160" },
        { "debug.sf.nobootanimation", "1" },
        { "debug.sf.showupdates", "0" },
        { "debug.sf.showcpu", "0" },
        { "debug.sf.showbackground", "0" },
        { "debug.sf.showfps", "0" },
        { "default", "default" },

        /* Stagefright options */
        { "media.stagefright.enable-player", "true" },
        { "media.stagefright.enable-meta", "true" },
        { "media.stagefright.enable-scan", "true" },
        { "media.stagefright.enable-http", "true" },
    };

    for (int i = 0; i < NELEM(propList); i++)
        SetProperty(propList[i].key, propList[i].value);

    Preferences* pPrefs = ((MyApp*)wxTheApp)->GetPrefs();
    bool doCheckJni = false;

    pPrefs->GetBool("check-jni", &doCheckJni);
    if (doCheckJni)
        SetProperty(kPropCheckJni, "1");
    else
        SetProperty(kPropCheckJni, "0");
}

/*
 * Get the value of a property.
 *
 * "valueBuf" must hold at least PROPERTY_VALUE_MAX bytes.
 *
 * Returns "true" if the property was found.
 */
bool PropertyServer::GetProperty(const char* key, char* valueBuf)
{
    typedef List<Property>::iterator PropIter;

    assert(key != NULL);
    assert(valueBuf != NULL);

    for (PropIter pi = mPropList.begin(); pi != mPropList.end(); ++pi) {
        Property& prop = *pi;
        if (strcmp(prop.key, key) == 0) {
            if (strlen(prop.value) >= PROPERTY_VALUE_MAX) {
                fprintf(stderr,
                    "GLITCH: properties table holds '%s' '%s' (len=%d)\n",
                    prop.key, prop.value, (int) strlen(prop.value));
                abort();
            }
            assert(strlen(prop.value) < PROPERTY_VALUE_MAX);
            strcpy(valueBuf, prop.value);
            return true;
        }
    }

    //printf("Prop: get [%s] not found\n", key);
    return false;
}

/*
 * Set the value of a property, replacing it if it already exists.
 *
 * If "value" is NULL, the property is removed.
 *
 * If the property is immutable, this returns "false" without doing
 * anything.  (Not implemented.)
 */
bool PropertyServer::SetProperty(const char* key, const char* value)
{
    typedef List<Property>::iterator PropIter;

    assert(key != NULL);
    assert(value != NULL);

    for (PropIter pi = mPropList.begin(); pi != mPropList.end(); ++pi) {
        Property& prop = *pi;
        if (strcmp(prop.key, key) == 0) {
            if (value != NULL) {
                //printf("Prop: replacing [%s]: [%s] with [%s]\n",
                //    prop.key, prop.value, value);
                strcpy(prop.value, value);
            } else {
                //printf("Prop: removing [%s]\n", prop.key);
                mPropList.erase(pi);
            }
            return true;
        }
    }

    //printf("Prop: adding [%s]: [%s]\n", key, value);
    Property tmp;
    strcpy(tmp.key, key);
    strcpy(tmp.value, value);
    mPropList.push_back(tmp);
    return true;
}

/*
 * Create a UNIX domain socket, carefully removing it if it already
 * exists.
 */
bool PropertyServer::CreateSocket(const char* fileName)
{
    struct stat sb;
    bool result = false;
    int sock = -1;
    int cc;

    cc = stat(fileName, &sb);
    if (cc < 0) {
        if (errno != ENOENT) {
            LOG(LOG_ERROR, "sim-prop",
                "Unable to stat '%s' (errno=%d)\n", fileName, errno);
            goto bail;
        }
    } else {
        /* don't touch it if it's not a socket */
        if (!(S_ISSOCK(sb.st_mode))) {
            LOG(LOG_ERROR, "sim-prop",
                "File '%s' exists and is not a socket\n", fileName);
            goto bail;
        }

        /* remove the cruft */
        if (unlink(fileName) < 0) {
            LOG(LOG_ERROR, "sim-prop",
                "Unable to remove '%s' (errno=%d)\n", fileName, errno);
            goto bail;
        }
    }

    struct sockaddr_un addr;

    sock = ::socket(AF_UNIX, SOCK_STREAM, 0);
    if (sock < 0) {
        LOG(LOG_ERROR, "sim-prop",
            "UNIX domain socket create failed (errno=%d)\n", errno);
        goto bail;
    }

    /* bind the socket; this creates the file on disk */
    strcpy(addr.sun_path, fileName);    // max 108 bytes
    addr.sun_family = AF_UNIX;
    cc = ::bind(sock, (struct sockaddr*) &addr, SUN_LEN(&addr));
    if (cc < 0) {
        LOG(LOG_ERROR, "sim",
            "AF_UNIX bind failed for '%s' (errno=%d)\n", fileName, errno);
        goto bail;
    }

    cc = ::listen(sock, 5);
    if (cc < 0) {
        LOG(LOG_ERROR, "sim", "AF_UNIX listen failed (errno=%d)\n", errno);
        goto bail;
    }

    mListenSock = sock;
    sock = -1;
    result = true;

bail:
    if (sock >= 0)
        close(sock);
    return result;
}

/*
 * Handle a client request.
 *
 * Returns true on success, false if the fd should be closed.
 */
bool PropertyServer::HandleRequest(int fd)
{
    char reqBuf[PROPERTY_KEY_MAX + PROPERTY_VALUE_MAX];
    char valueBuf[1 + PROPERTY_VALUE_MAX];
    ssize_t actual;

    memset(valueBuf, 'x', sizeof(valueBuf));        // placate valgrind

    /* read the command byte; this determines the message length */
    actual = read(fd, reqBuf, 1);
    if (actual <= 0)
        return false;

    if (reqBuf[0] == kSystemPropertyGet) {
        actual = read(fd, reqBuf, PROPERTY_KEY_MAX);

        if (actual != PROPERTY_KEY_MAX) {
            fprintf(stderr, "Bad read on get: %d of %d\n",
                (int) actual, PROPERTY_KEY_MAX);
            return false;
        }
        if (GetProperty(reqBuf, valueBuf+1))
            valueBuf[0] = 1;
        else
            valueBuf[0] = 0;
        //printf("GET property [%s]: (found=%d) [%s]\n",
        //    reqBuf, valueBuf[0], valueBuf+1);
        if (write(fd, valueBuf, sizeof(valueBuf)) != sizeof(valueBuf)) {
            fprintf(stderr, "Bad write on get\n");
            return false;
        }
    } else if (reqBuf[0] == kSystemPropertySet) {
        actual = read(fd, reqBuf, PROPERTY_KEY_MAX + PROPERTY_VALUE_MAX);
        if (actual != PROPERTY_KEY_MAX + PROPERTY_VALUE_MAX) {
            fprintf(stderr, "Bad read on set: %d of %d\n",
                (int) actual, PROPERTY_KEY_MAX + PROPERTY_VALUE_MAX);
            return false;
        }
        //printf("SET property '%s'\n", reqBuf);
        if (SetProperty(reqBuf, reqBuf + PROPERTY_KEY_MAX))
            valueBuf[0] = 1;
        else
            valueBuf[0] = 0;
        if (write(fd, valueBuf, 1) != 1) {
            fprintf(stderr, "Bad write on set\n");
            return false;
        }
    } else if (reqBuf[0] == kSystemPropertyList) {
        /* TODO */
        assert(false);
    } else {
        fprintf(stderr, "Unexpected request %d from prop client\n", reqBuf[0]);
        return false;
    }

    return true;
}

/*
 * Serve up properties.
 */
void PropertyServer::ServeProperties(void)
{
    typedef List<int>::iterator IntIter;
    fd_set readfds;
    int maxfd;

    while (true) {
        int cc;

        FD_ZERO(&readfds);
        FD_SET(mListenSock, &readfds);
        maxfd = mListenSock;

        for (IntIter ii = mClientList.begin(); ii != mClientList.end(); ++ii) {
            int fd = (*ii);

            FD_SET(fd, &readfds);
            if (maxfd < fd)
                maxfd = fd;
        }

        cc = select(maxfd+1, &readfds, NULL, NULL, NULL);
        if (cc < 0) {
            if (errno == EINTR) {
                printf("hiccup!\n");
                continue;
            }
            return;
        }
        if (FD_ISSET(mListenSock, &readfds)) {
            struct sockaddr_un from;
            socklen_t fromlen;
            int newSock;

            fromlen = sizeof(from);
            newSock = ::accept(mListenSock, (struct sockaddr*) &from, &fromlen);
            if (newSock < 0) {
                LOG(LOG_WARN, "sim",
                    "AF_UNIX accept failed (errno=%d)\n", errno);
            } else {
                //printf("new props connection on %d --> %d\n",
                //    mListenSock, newSock);

                mClientList.push_back(newSock);
            }
        }

        for (IntIter ii = mClientList.begin(); ii != mClientList.end(); ) {
            int fd = (*ii);
            bool ok = true;

            if (FD_ISSET(fd, &readfds)) {
                //printf("--- activity on %d\n", fd);

                ok = HandleRequest(fd);
            }

            if (ok) {
                ++ii;
            } else {
                //printf("--- closing %d\n", fd);
                close(fd);
                ii = mClientList.erase(ii);
            }
        }
    }
}

/*
 * Thread entry point.
 *
 * This just sits and waits for a new connection.  It hands it off to the
 * main thread and then goes back to waiting.
 *
 * There is currently no "polite" way to shut this down.
 */
void* PropertyServer::Entry(void)
{
    if (CreateSocket(SYSTEM_PROPERTY_PIPE_NAME)) {
        assert(mListenSock >= 0);
        SetDefaultProperties();

        /* loop until it's time to exit or we fail */
        ServeProperties();

        ClearProperties();

        /*
         * Close listen socket and all clients.
         */
        LOG(LOG_INFO, "sim", "Cleaning up socket list\n");
        typedef List<int>::iterator IntIter;
        for (IntIter ii = mClientList.begin(); ii != mClientList.end(); ++ii)
            close((*ii));
        close(mListenSock);
    }

    LOG(LOG_INFO, "sim", "PropertyServer thread exiting\n");
    return NULL;
}

