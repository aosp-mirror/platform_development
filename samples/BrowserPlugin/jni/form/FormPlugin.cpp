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

#include "FormPlugin.h"

#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*         browser;
extern ANPLogInterfaceV0        gLogI;
extern ANPCanvasInterfaceV0     gCanvasI;
extern ANPPaintInterfaceV0      gPaintI;
extern ANPTypefaceInterfaceV0   gTypefaceI;
extern ANPWindowInterfaceV0     gWindowI;


static void inval(NPP instance) {
    browser->invalidaterect(instance, NULL);
}

static uint16_t rnd16(float x, int inset) {
    int ix = (int)roundf(x) + inset;
    if (ix < 0) {
        ix = 0;
    }
    return static_cast<uint16_t>(ix);
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

///////////////////////////////////////////////////////////////////////////////

FormPlugin::FormPlugin(NPP inst) : SubPlugin(inst) {

    m_hasFocus = false;
    m_activeInput = NULL;

    memset(&m_usernameInput, 0, sizeof(m_usernameInput));
    memset(&m_passwordInput, 0, sizeof(m_passwordInput));

    m_usernameInput.text[0] = '\0';
    m_usernameInput.charPtr = 0;

    m_passwordInput.text[0] = '\0';
    m_passwordInput.charPtr = 0;

    m_paintInput = gPaintI.newPaint();
    gPaintI.setFlags(m_paintInput, gPaintI.getFlags(m_paintInput) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintInput, 0xFFFFFFFF);

    m_paintActive = gPaintI.newPaint();
    gPaintI.setFlags(m_paintActive, gPaintI.getFlags(m_paintActive) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintActive, 0xFFFFFF00);

    m_paintText = gPaintI.newPaint();
    gPaintI.setFlags(m_paintText, gPaintI.getFlags(m_paintText) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintText, 0xFF000000);
    gPaintI.setTextSize(m_paintText, 18);

    ANPTypeface* tf = gTypefaceI.createFromName("serif", kItalic_ANPTypefaceStyle);
    gPaintI.setTypeface(m_paintText, tf);
    gTypefaceI.unref(tf);

    //register for key and visibleRect events
    ANPEventFlags flags = kKey_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(kError_ANPLogType, "Error selecting input events.");
    }
}

FormPlugin::~FormPlugin() {
    gPaintI.deletePaint(m_paintInput);
    gPaintI.deletePaint(m_paintActive);
    gPaintI.deletePaint(m_paintText);
}

bool FormPlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kBitmap_ANPDrawingModel);
}

void FormPlugin::drawPlugin(const ANPBitmap& bitmap, const ANPRectI& clip) {
    ANPCanvas* canvas = gCanvasI.newCanvas(&bitmap);

    ANPRectF clipR;
    clipR.left = clip.left;
    clipR.top = clip.top;
    clipR.right = clip.right;
    clipR.bottom = clip.bottom;
    gCanvasI.clipRect(canvas, &clipR);

    draw(canvas);
    gCanvasI.deleteCanvas(canvas);
}

void FormPlugin::draw(ANPCanvas* canvas) {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;

    const float inputWidth = 60;
    const float inputHeight = 30;
    const int W = obj->window->width;
    const int H = obj->window->height;

    // color the plugin canvas
    gCanvasI.drawColor(canvas, (m_hasFocus) ? 0xFFCDCDCD : 0xFF545454);

    // draw the username box (5 px from the top edge)
    m_usernameInput.rect.left = 5;
    m_usernameInput.rect.top = 5;
    m_usernameInput.rect.right = W - 5;
    m_usernameInput.rect.bottom = m_usernameInput.rect.top + inputHeight;
    gCanvasI.drawRect(canvas, &m_usernameInput.rect, getPaint(&m_usernameInput));
    drawText(canvas, m_usernameInput);

    // draw the password box (5 px from the bottom edge)
    m_passwordInput.rect.left = 5;
    m_passwordInput.rect.top = H - (inputHeight + 5);
    m_passwordInput.rect.right = W - 5;
    m_passwordInput.rect.bottom = m_passwordInput.rect.top + inputHeight;
    gCanvasI.drawRect(canvas, &m_passwordInput.rect, getPaint(&m_passwordInput));
    drawPassword(canvas, m_passwordInput);

    //invalidate the canvas
    //inval(instance);
}

ANPPaint* FormPlugin::getPaint(TextInput* input) {
    return (input == m_activeInput) ? m_paintActive : m_paintInput;
}

void FormPlugin::drawText(ANPCanvas* canvas, TextInput textInput) {

    // get font metrics
    ANPFontMetrics fontMetrics;
    gPaintI.getFontMetrics(m_paintText, &fontMetrics);

    gCanvasI.drawText(canvas, textInput.text, textInput.charPtr,
                      textInput.rect.left + 5,
                      textInput.rect.bottom - fontMetrics.fBottom, m_paintText);
}

void FormPlugin::drawPassword(ANPCanvas* canvas, TextInput passwordInput) {

    // get font metrics
    ANPFontMetrics fontMetrics;
    gPaintI.getFontMetrics(m_paintText, &fontMetrics);

    // comput the circle dimensions and initial location
    float initialX = passwordInput.rect.left + 5;
    float ovalBottom = passwordInput.rect.bottom - 2;
    float ovalTop = ovalBottom - (fontMetrics.fBottom - fontMetrics.fTop);
    float ovalWidth = ovalBottom - ovalTop;
    float ovalSpacing = 3;

    // draw circles instead of the actual text
    for (uint32_t x = 0; x < passwordInput.charPtr; x++) {
        ANPRectF oval;
        oval.left = initialX + ((ovalWidth + ovalSpacing) * (float) x);
        oval.right = oval.left + ovalWidth;
        oval.top = ovalTop;
        oval.bottom = ovalBottom;
        gCanvasI.drawOval(canvas, &oval, m_paintText);
    }
}

int16_t FormPlugin::handleEvent(const ANPEvent* evt) {
    NPP instance = this->inst();

    switch (evt->eventType) {
        case kDraw_ANPEventType:
            switch (evt->data.draw.model) {
                case kBitmap_ANPDrawingModel:
                    drawPlugin(evt->data.draw.data.bitmap, evt->data.draw.clip);
                    return 1;
                default:
                    break;   // unknown drawing model
            }
            break;

        case kLifecycle_ANPEventType:
            if (evt->data.lifecycle.action == kLoseFocus_ANPLifecycleAction) {
                gLogI.log(kDebug_ANPLogType, "----%p Loosing Focus", instance);

                if (m_activeInput) {
                    // hide the keyboard
                    gWindowI.showKeyboard(instance, false);

                    //reset the activeInput
                    m_activeInput = NULL;
                }

                m_hasFocus = false;
                inval(instance);
                return 1;
            }
            else if (evt->data.lifecycle.action == kGainFocus_ANPLifecycleAction) {
                gLogI.log(kDebug_ANPLogType, "----%p Gaining Focus", instance);
                m_hasFocus = true;
                inval(instance);
                return 1;
            }
            break;

        case kMouse_ANPEventType: {

            int x = evt->data.mouse.x;
            int y = evt->data.mouse.y;
            if (kDown_ANPMouseAction == evt->data.mouse.action) {

                TextInput* currentInput = validTap(x,y);

                if (currentInput)
                    gWindowI.showKeyboard(instance, true);
                else if (m_activeInput)
                    gWindowI.showKeyboard(instance, false);

                if (currentInput != m_activeInput)
                    switchActiveInput(currentInput);

                return 1;
            }
            break;
        }

        case kKey_ANPEventType:
            if (evt->data.key.action == kDown_ANPKeyAction) {

                //handle navigation keys
                if (evt->data.key.nativeCode >= kDpadUp_ANPKeyCode
                        && evt->data.key.nativeCode <= kDpadCenter_ANPKeyCode) {
                    return handleNavigation(evt->data.key.nativeCode) ? 1 : 0;
                }

                if (m_activeInput) {
                    handleTextInput(m_activeInput, evt->data.key.nativeCode,
                                    evt->data.key.unichar);
                    inval(instance, m_activeInput->rect, true);
                }
            }
            return 1;

        default:
            break;
    }
    return 0;   // unknown or unhandled event
}

void FormPlugin::switchActiveInput(TextInput* newInput) {
    NPP instance = this->inst();

    if (m_activeInput) {
        inval(instance, m_activeInput->rect, true); // inval the old
        gWindowI.clearVisibleRects(instance);
    }

    m_activeInput = newInput; // set the new active input

    if (m_activeInput) {
        inval(instance, m_activeInput->rect, true); // inval the new
        scrollIntoView(m_activeInput);
    }
}

bool FormPlugin::handleNavigation(ANPKeyCode keyCode) {
    NPP instance = this->inst();

    gLogI.log(kDebug_ANPLogType, "----%p Recvd Nav Key %d", instance, keyCode);

    if (!m_activeInput) {
        gWindowI.showKeyboard(instance, true);
        switchActiveInput(&m_usernameInput);
    }
    else if (m_activeInput == &m_usernameInput) {
        if (keyCode == kDpadDown_ANPKeyCode) {
            switchActiveInput(&m_passwordInput);
        }
        else if (keyCode == kDpadCenter_ANPKeyCode)
            gWindowI.showKeyboard(instance, false);
        else if (keyCode == kDpadUp_ANPKeyCode)
            return false;
    }
    else if (m_activeInput == &m_passwordInput) {
        if (keyCode == kDpadUp_ANPKeyCode) {
            switchActiveInput(&m_usernameInput);
        }
        else if (keyCode == kDpadCenter_ANPKeyCode)
            gWindowI.showKeyboard(instance, false);
        else if (keyCode == kDpadDown_ANPKeyCode)
            return false;
    }

    return true;
}

void FormPlugin::handleTextInput(TextInput* input, ANPKeyCode keyCode, int32_t unichar) {
    NPP instance = this->inst();

    //make sure the input field is in view
    scrollIntoView(input);

    //handle the delete operation
    if (keyCode == kDel_ANPKeyCode) {
        if (input->charPtr > 0) {
            input->charPtr--;
        }
        return;
    }

    //check to see that the input is not full
    if (input->charPtr >= (sizeof(input->text) - 1))
        return;

    //add the character
    input->text[input->charPtr] = static_cast<char>(unichar);
    input->charPtr++;

    gLogI.log(kDebug_ANPLogType, "----%p Text:  %c", instance, unichar);
}

void FormPlugin::scrollIntoView(TextInput* input) {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;
    NPWindow *window = obj->window;

    // find the textInput's global rect coordinates
    ANPRectI visibleRects[1];
    visibleRects[0].left = input->rect.left;
    visibleRects[0].top = input->rect.top;
    visibleRects[0].right = input->rect.right;
    visibleRects[0].bottom = input->rect.bottom;

    gWindowI.setVisibleRects(instance, visibleRects, 1);
}

TextInput* FormPlugin::validTap(int x, int y) {

    if (x > m_usernameInput.rect.left && x < m_usernameInput.rect.right &&
        y > m_usernameInput.rect.top && y < m_usernameInput.rect.bottom)
        return &m_usernameInput;
    else if (x >m_passwordInput.rect.left && x < m_passwordInput.rect.right &&
             y > m_passwordInput.rect.top && y < m_passwordInput.rect.bottom)
        return &m_passwordInput;
    else
        return NULL;
}
