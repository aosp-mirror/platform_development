/*
 * Copyright 2007 The Android Open Source Project
 *
 * Audio output device.
 */
#include "Common.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <esd.h>

#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/input.h>


/*
 * Input event device state.
 */
typedef struct AudioState {
    int fd;
    int sourceId;
    int esdVol;
    int streamType;
} AudioState;

/*
 * Set some stuff up.
 */
static int configureInitialState(const char* pathName, AudioState* audioState)
{
#if BUILD_SIM_WITHOUT_AUDIO
    return 0;
#else
    esd_player_info_t *pi; 
    audioState->fd = -1;
    audioState->sourceId = -1;
    audioState->esdVol = -1;
    audioState->streamType = 0;

    int format = ESD_BITS16 | ESD_STEREO | ESD_STREAM | ESD_PLAY;
    char namestring[] = "Android Audio XXXXXXXX";
    sprintf(namestring,"Android Audio %08x", (unsigned int)audioState);
    int esd_fd = esd_play_stream_fallback(format, 44100, NULL, namestring);
    if (esd_fd > 0) {
        // find the source_id for this stream
        int mix = esd_open_sound(NULL);
        if (mix > 0) {
            esd_info_t *info = esd_get_all_info(mix);

            if (info) {
                for(pi = info->player_list; pi; pi = pi->next) {
                    if(strcmp(pi->name, namestring) == 0) {
                        audioState->sourceId = pi->source_id;
                        break;
                    }
                }
                esd_free_all_info(info);
            }
            esd_close(mix);
        }
        audioState->fd = esd_fd;
        return 0;
    }
    printf("Couldn't open audio device. Faking it.\n");
    return 0;
#endif
}

/*
 * Return the next available input event.
 *
 * We just pass this through to the real "write", since "fd" is real.
 */
static ssize_t writeAudio(FakeDev* dev, int fd, const void* buf, size_t count)
{
#if BUILD_SIM_WITHOUT_AUDIO
    return 0;
#else
    AudioState *state = (AudioState*)dev->state;
    if (state->fd >= 0)
        return _ws_write(state->fd, buf, count);

    // fake timing
    usleep(count * 10000 / 441 * 4);
    return count;
#endif
}

/*
 * Handle event ioctls.
 */
static int ioctlAudio(FakeDev* dev, int fd, int request, void* argp)
{
    return -1;
}

/*
 * Free up our state before closing down the fake descriptor.
 */
static int closeAudio(FakeDev* dev, int fd)
{
#if BUILD_SIM_WITHOUT_AUDIO
    return 0;
#else
    AudioState *state = (AudioState*)dev->state;
    close(state->fd);
    free(state);
    dev->state = NULL;
    return 0;
#endif
}

/*
 * Open an audio output device.
 */
FakeDev* wsOpenDevAudio(const char* pathName, int flags)
{
    FakeDev* newDev = wsCreateFakeDev(pathName);
    if (newDev != NULL) {
        newDev->write = writeAudio;
        newDev->ioctl = ioctlAudio;
        newDev->close = closeAudio;

        AudioState* eventState = calloc(1, sizeof(AudioState));

        if (configureInitialState(pathName, eventState) != 0) {
            free(eventState);
            return NULL;
        }
        newDev->state = eventState;
    }

    return newDev;
}
