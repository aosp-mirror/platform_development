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
#include <GLES/gl.h>
#include <GLES/glext.h>
#include <GLcommon/FramebufferData.h>

RenderbufferData::RenderbufferData() : sourceEGLImage(0),
                         eglImageDetach(NULL),
                         attachedFB(0),
                         attachedPoint(0),
                         eglImageGlobalTexName(0) {
}

RenderbufferData::~RenderbufferData() {
    if (sourceEGLImage && eglImageDetach) (*eglImageDetach)(sourceEGLImage);
}


FramebufferData::FramebufferData(GLuint name) {
    m_fbName = name;
    for (int i=0; i<MAX_ATTACH_POINTS; i++) {
        m_attachPoints[i].target = 0;
        m_attachPoints[i].name = 0;
        m_attachPoints[i].obj = ObjectDataPtr(NULL);
    }
}

FramebufferData::~FramebufferData() {
for (int i=0; i<MAX_ATTACH_POINTS; i++) {
    detachObject(i);
}
}

void FramebufferData::setAttachment(GLenum attachment,
               GLenum target,
               GLuint name,
               ObjectDataPtr obj) {
int idx = attachmentPointIndex(attachment);

    if (m_attachPoints[idx].target != target ||
        m_attachPoints[idx].name != name ||
        m_attachPoints[idx].obj.Ptr() != obj.Ptr()) {

        detachObject(idx); 

        m_attachPoints[idx].target = target;
        m_attachPoints[idx].name = name;
        m_attachPoints[idx].obj = obj;

        if (target == GL_RENDERBUFFER_OES && obj.Ptr() != NULL) {
            RenderbufferData *rbData = (RenderbufferData *)obj.Ptr();
            rbData->attachedFB = m_fbName;
            rbData->attachedPoint = attachment;
        }
    }
}

GLuint FramebufferData::getAttachment(GLenum attachment,
                 GLenum *outTarget,
                 ObjectDataPtr *outObj) {
    int idx = attachmentPointIndex(attachment);
    if (outTarget) *outTarget = m_attachPoints[idx].target;
    if (outObj) *outObj = m_attachPoints[idx].obj;
    return m_attachPoints[idx].name;
}

int FramebufferData::attachmentPointIndex(GLenum attachment)
{
    switch(attachment) {
    case GL_COLOR_ATTACHMENT0_OES:
        return 0;
    case GL_DEPTH_ATTACHMENT_OES:
        return 1;
    case GL_STENCIL_ATTACHMENT_OES:
        return 3;
    default:
        return MAX_ATTACH_POINTS;
    }
}

void FramebufferData::detachObject(int idx) {
    if (m_attachPoints[idx].target == GL_RENDERBUFFER_OES && m_attachPoints[idx].obj.Ptr() != NULL) {
        RenderbufferData *rbData = (RenderbufferData *)m_attachPoints[idx].obj.Ptr();
        rbData->attachedFB = 0;
        rbData->attachedPoint = 0;
    }
    m_attachPoints[idx].target = 0;
    m_attachPoints[idx].name = 0;
    m_attachPoints[idx].obj = ObjectDataPtr(NULL);
}
