/*
 * Copyright 2007 The Android Open Source Project
 *
 * Audio output device.
 */
#include "Common.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <alsa/asoundlib.h>

#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/input.h>


/*
 * Input event device state.
 */
typedef struct AudioState {
    snd_pcm_t *handle;
} AudioState;

/*
 * Set some stuff up.
 */
static int configureInitialState(const char* pathName, AudioState* audioState)
{
#if BUILD_SIM_WITHOUT_AUDIO
    return 0;
#else
    audioState->handle = NULL;

    snd_pcm_open(&audioState->handle, "default", SND_PCM_STREAM_PLAYBACK, 0);

    if (audioState->handle) {
        snd_pcm_hw_params_t *params;
        snd_pcm_hw_params_malloc(&params);
        snd_pcm_hw_params_any(audioState->handle, params);
        snd_pcm_hw_params_set_access(audioState->handle, params, SND_PCM_ACCESS_RW_INTERLEAVED);
        snd_pcm_hw_params_set_format(audioState->handle, params, SND_PCM_FORMAT_S16_LE);
        unsigned int rate = 44100;
        snd_pcm_hw_params_set_rate_near(audioState->handle, params, &rate, NULL);
        snd_pcm_hw_params_set_channels(audioState->handle, params, 2);
        snd_pcm_hw_params(audioState->handle, params);
        snd_pcm_hw_params_free(params);
    } else {
        wsLog("Couldn't open audio hardware, faking it\n");
    }

    return 0;
#endif
}

/*
 * Write audio data.
 */
static ssize_t writeAudio(FakeDev* dev, int fd, const void* buf, size_t count)
{
#if BUILD_SIM_WITHOUT_AUDIO
    return 0;
#else
    AudioState *state = (AudioState*)dev->state;
    if (state->handle != NULL) {
        snd_pcm_writei(state->handle, buf, count / 4);
        return count;
    }

    // fake timing
    usleep(count * 10000 / (441 * 4));
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
    snd_pcm_close(state->handle);
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
