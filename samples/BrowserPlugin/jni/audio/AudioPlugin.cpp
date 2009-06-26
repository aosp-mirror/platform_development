/*
 * Copyright 2008, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "AudioPlugin.h"

#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*         browser;
extern ANPLogInterfaceV0        gLogI;
extern ANPCanvasInterfaceV0     gCanvasI;
extern ANPPaintInterfaceV0      gPaintI;
extern ANPAudioTrackInterfaceV0 gSoundI;
extern ANPTypefaceInterfaceV0   gTypefaceI;


static void inval(NPP instance) {
    browser->invalidaterect(instance, NULL);
}

static uint16 rnd16(float x, int inset) {
    int ix = (int)roundf(x) + inset;
    if (ix < 0) {
        ix = 0;
    }
    return static_cast<uint16>(ix);
}

static void inval(NPP instance, const ANPRectF& r, bool doAA) {
    const int inset = doAA ? -1 : 0;

    PluginObject *obj = reinterpret_cast<PluginObject*>(instance->pdata);
    NPRect inval;
    inval.left = rnd16(r.left, inset);
    inval.top = rnd16(r.top, inset);
    inval.right = rnd16(r.right, -inset);
    inval.bottom = rnd16(r.bottom, -inset);
    browser->invalidaterect(instance, &inval);
}

static void drawPlugin(SubPlugin* plugin, const ANPBitmap& bitmap, const ANPRectI& clip) {
    ANPCanvas* canvas = gCanvasI.newCanvas(&bitmap);

    ANPRectF clipR;
    clipR.left = clip.left;
    clipR.top = clip.top;
    clipR.right = clip.right;
    clipR.bottom = clip.bottom;
    gCanvasI.clipRect(canvas, &clipR);

    plugin->draw(canvas);
    gCanvasI.deleteCanvas(canvas);
}

struct SoundPlay {
    NPP             instance;
    ANPAudioTrack*  track;
    FILE*           file;
};

static void audioCallback(ANPAudioEvent evt, void* user, ANPAudioBuffer* buffer) {
    switch (evt) {
        case kMoreData_ANPAudioEvent: {
            SoundPlay* play = reinterpret_cast<SoundPlay*>(user);
            size_t amount = fread(buffer->bufferData, 1, buffer->size, play->file);
            buffer->size = amount;
            if (amount == 0) {
                gSoundI.stop(play->track);
                fclose(play->file);
                play->file = NULL;
                // TODO need to notify our main thread to delete the track now
            }
            break;
        }
        default:
            break;
    }
}

static ANPAudioTrack* createTrack(NPP instance, const char path[]) {
    FILE* f = fopen(path, "r");
    gLogI.log(instance, kWarning_ANPLogType, "--- path %s FILE %p", path, f);
    if (NULL == f) {
        return NULL;
    }
    SoundPlay* play = new SoundPlay;
    play->file = f;
    play->track = gSoundI.newTrack(44100, kPCM16Bit_ANPSampleFormat, 2, audioCallback, play);
    if (NULL == play->track) {
        fclose(f);
        delete play;
        return NULL;
    }
    return play->track;
}

///////////////////////////////////////////////////////////////////////////////

AudioPlugin::AudioPlugin(NPP inst) : SubPlugin(inst) {

    m_track = NULL;
    m_activeTouch = false;

    memset(&m_trackRect, 0, sizeof(m_trackRect));
    memset(&m_playRect,  0, sizeof(m_playRect));
    memset(&m_pauseRect, 0, sizeof(m_pauseRect));
    memset(&m_stopRect,  0, sizeof(m_stopRect));

    m_paintTrack = gPaintI.newPaint();
    gPaintI.setFlags(m_paintTrack, gPaintI.getFlags(m_paintTrack) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintTrack, 0xFFC0C0C0);

    m_paintRect = gPaintI.newPaint();
    gPaintI.setFlags(m_paintRect, gPaintI.getFlags(m_paintRect) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintRect, 0xFFA8A8A8);

    m_paintText = gPaintI.newPaint();
    gPaintI.setFlags(m_paintText, gPaintI.getFlags(m_paintText) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintText, 0xFF2F4F4F);
    gPaintI.setTextSize(m_paintText, 18);

    ANPTypeface* tf = gTypefaceI.createFromName("serif", kItalic_ANPTypefaceStyle);
    gPaintI.setTypeface(m_paintText, tf);
    gTypefaceI.unref(tf);

    //register for touch events
    ANPEventFlags flags = kTouch_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(inst, kError_ANPLogType, "Error selecting input events.");
    }
}

AudioPlugin::~AudioPlugin() {
    gPaintI.deletePaint(m_paintTrack);
    gPaintI.deletePaint(m_paintRect);
    gPaintI.deletePaint(m_paintText);
    if(m_track)
        gSoundI.deleteTrack(m_track);
}

void AudioPlugin::draw(ANPCanvas* canvas) {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;

    const float trackHeight = 30;
    const float buttonWidth = 60;
    const float buttonHeight = 30;
    const int W = obj->window->width;
    const int H = obj->window->height;

    // color the plugin canvas
    gCanvasI.drawColor(canvas, 0xFFCDCDCD);

    // get font metrics
    ANPFontMetrics fontMetrics;
    gPaintI.getFontMetrics(m_paintText, &fontMetrics);

    // draw the track box (1 px from the edge)
    inval(instance, m_trackRect, true);
    m_trackRect.left = 1;
    m_trackRect.top = 1;
    m_trackRect.right = W - 2;
    m_trackRect.bottom = 1 + trackHeight;
    gCanvasI.drawRect(canvas, &m_trackRect, m_paintTrack);
    inval(instance, m_trackRect, true);

    // draw the play box (under track box)
    inval(instance, m_playRect, true);
    m_playRect.left = m_trackRect.left + 5;
    m_playRect.top = m_trackRect.bottom + 10;
    m_playRect.right = m_playRect.left + buttonWidth;
    m_playRect.bottom = m_playRect.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_playRect, m_paintRect);
    // draw the play box (under track box)
    const char playText[] = "Play";
    gCanvasI.drawText(canvas, playText, sizeof(playText)-1, m_playRect.left + 5,
                      m_playRect.top - fontMetrics.fTop, m_paintText);
    inval(instance, m_playRect, true);

    // draw the pause box (under track box)
    inval(instance, m_pauseRect, true);
    m_pauseRect.left = m_playRect.right + 20;
    m_pauseRect.top = m_trackRect.bottom + 10;
    m_pauseRect.right = m_pauseRect.left + buttonWidth;
    m_pauseRect.bottom = m_pauseRect.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_pauseRect, m_paintRect);
    // draw the text in the pause box
    const char pauseText[] = "Pause";
    gCanvasI.drawText(canvas, pauseText, sizeof(pauseText)-1, m_pauseRect.left + 5,
                      m_pauseRect.top - fontMetrics.fTop, m_paintText);
    inval(instance, m_pauseRect, true);

    // draw the stop box (under track box)
    inval(instance, m_stopRect, true);
    m_stopRect.left = m_pauseRect.right + 20;
    m_stopRect.top = m_trackRect.bottom + 10;
    m_stopRect.right = m_stopRect.left + buttonWidth;
    m_stopRect.bottom = m_stopRect.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_stopRect, m_paintRect);
    // draw the text in the pause box
    const char stopText[] = "Stop";
    gCanvasI.drawText(canvas, stopText, sizeof(stopText)-1, m_stopRect.left + 5,
                      m_stopRect.top - fontMetrics.fTop, m_paintText);
    inval(instance, m_stopRect, true);

}

int16 AudioPlugin::handleEvent(const ANPEvent* evt) {
    NPP instance = this->inst();

    switch (evt->eventType) {
        case kDraw_ANPEventType:
            switch (evt->data.draw.model) {
                case kBitmap_ANPDrawingModel:
                    drawPlugin(this, evt->data.draw.data.bitmap, evt->data.draw.clip);
                    return 1;
                default:
                    break;   // unknown drawing model
            }

        case kTouch_ANPEventType: {
            int x = evt->data.touch.x;
            int y = evt->data.touch.y;
            if (kDown_ANPTouchAction == evt->data.touch.action) {

                if (m_activeTouch)
                    invalActiveRect();

                m_activeRect = validTouch(x,y);
                if(m_activeRect) {
                    m_activeTouch = true;
                    // TODO color the rect
                    return 1;
                }

            } else if (kUp_ANPTouchAction == evt->data.touch.action && m_activeTouch) {
                handleTouch(x, y);
                invalActiveRect();
                m_activeTouch = false;
                return 1;
            } else if (kCancel_ANPTouchAction == evt->data.touch.action) {
                m_activeTouch = false;
            }
            break;
        }
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}

void AudioPlugin::invalActiveRect() { }

ANPRectF* AudioPlugin::validTouch(int x, int y) {

    if (m_playRect.left && x < m_playRect.right && y > m_playRect.top && y < m_playRect.bottom)
        return &m_playRect;
    else if (m_pauseRect.left && x < m_pauseRect.right && y > m_pauseRect.top && y < m_pauseRect.bottom)
        return &m_pauseRect;
    else if (x > m_stopRect.left && x < m_stopRect.right && y > m_stopRect.top && y < m_stopRect.bottom)
        return &m_stopRect;
    else
        return NULL;
}

void AudioPlugin::handleTouch(int x, int y) {
    NPP instance = this->inst();

    if (NULL == m_track) {
        m_track = createTrack(instance, "/sdcard/sample.raw");
    }

    // if the track is still null then return
    if(NULL == m_track) {
        gLogI.log(instance, kError_ANPLogType, "---- %p unable to create track",
                  instance);
        return;
    }

    // check to make sure the currentRect matches the activeRect
    ANPRectF* currentRect = validTouch(x,y);
    if(m_activeRect != currentRect)
        return;

    if (m_activeRect == &m_playRect) {

        gLogI.log(instance, kDebug_ANPLogType, "---- %p starting track (%d)",
                  m_track, gSoundI.isStopped(m_track));

        if (gSoundI.isStopped(m_track)) {
            gSoundI.start(m_track);
        }
    }
    else if (m_activeRect == &m_pauseRect) {

        gLogI.log(instance, kDebug_ANPLogType, "---- %p pausing track (%d)",
                  m_track, gSoundI.isStopped(m_track));

        if (!gSoundI.isStopped(m_track)) {
            gSoundI.pause(m_track);
        }
    }
    else if (m_activeRect == &m_stopRect) {

        gLogI.log(instance, kDebug_ANPLogType, "---- %p stopping track (%d)",
                  m_track, gSoundI.isStopped(m_track));

        if (!gSoundI.isStopped(m_track)) {
            gSoundI.stop(m_track);
        }
    }
}
