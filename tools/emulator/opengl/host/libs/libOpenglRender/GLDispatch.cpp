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
#include "GLDispatch.h"
#include <stdio.h>
#include <stdlib.h>
#include "osDynLibrary.h"

GLDispatch s_gl;

static osUtils::dynLibrary *s_gles_lib = NULL;

//
// This function is called only once during initialiation before
// any thread has been created - hence it should NOT be thread safe.
//

#ifdef _WIN32
#define DEFAULT_GLES_CM_LIB "libGLES_CM_translator"
#elif defined(__APPLE__)
#define DEFAULT_GLES_CM_LIB "libGLES_CM_translator.dylib"
#else
#define DEFAULT_GLES_CM_LIB "libGLES_CM_translator.so"
#endif

bool init_gl_dispatch()
{
    const char *libName = getenv("ANDROID_GLESv1_LIB");
    if (!libName) libName = DEFAULT_GLES_CM_LIB;

    s_gles_lib = osUtils::dynLibrary::open(libName);
    if (!s_gles_lib) return false;

    s_gl.glAlphaFunc = (glAlphaFunc_t) s_gles_lib->findSymbol("glAlphaFunc");
    s_gl.glClearColor = (glClearColor_t) s_gles_lib->findSymbol("glClearColor");
    s_gl.glClearDepthf = (glClearDepthf_t) s_gles_lib->findSymbol("glClearDepthf");
    s_gl.glClipPlanef = (glClipPlanef_t) s_gles_lib->findSymbol("glClipPlanef");
    s_gl.glColor4f = (glColor4f_t) s_gles_lib->findSymbol("glColor4f");
    s_gl.glDepthRangef = (glDepthRangef_t) s_gles_lib->findSymbol("glDepthRangef");
    s_gl.glFogf = (glFogf_t) s_gles_lib->findSymbol("glFogf");
    s_gl.glFogfv = (glFogfv_t) s_gles_lib->findSymbol("glFogfv");
    s_gl.glFrustumf = (glFrustumf_t) s_gles_lib->findSymbol("glFrustumf");
    s_gl.glGetClipPlanef = (glGetClipPlanef_t) s_gles_lib->findSymbol("glGetClipPlanef");
    s_gl.glGetFloatv = (glGetFloatv_t) s_gles_lib->findSymbol("glGetFloatv");
    s_gl.glGetLightfv = (glGetLightfv_t) s_gles_lib->findSymbol("glGetLightfv");
    s_gl.glGetMaterialfv = (glGetMaterialfv_t) s_gles_lib->findSymbol("glGetMaterialfv");
    s_gl.glGetTexEnvfv = (glGetTexEnvfv_t) s_gles_lib->findSymbol("glGetTexEnvfv");
    s_gl.glGetTexParameterfv = (glGetTexParameterfv_t) s_gles_lib->findSymbol("glGetTexParameterfv");
    s_gl.glLightModelf = (glLightModelf_t) s_gles_lib->findSymbol("glLightModelf");
    s_gl.glLightModelfv = (glLightModelfv_t) s_gles_lib->findSymbol("glLightModelfv");
    s_gl.glLightf = (glLightf_t) s_gles_lib->findSymbol("glLightf");
    s_gl.glLightfv = (glLightfv_t) s_gles_lib->findSymbol("glLightfv");
    s_gl.glLineWidth = (glLineWidth_t) s_gles_lib->findSymbol("glLineWidth");
    s_gl.glLoadMatrixf = (glLoadMatrixf_t) s_gles_lib->findSymbol("glLoadMatrixf");
    s_gl.glMaterialf = (glMaterialf_t) s_gles_lib->findSymbol("glMaterialf");
    s_gl.glMaterialfv = (glMaterialfv_t) s_gles_lib->findSymbol("glMaterialfv");
    s_gl.glMultMatrixf = (glMultMatrixf_t) s_gles_lib->findSymbol("glMultMatrixf");
    s_gl.glMultiTexCoord4f = (glMultiTexCoord4f_t) s_gles_lib->findSymbol("glMultiTexCoord4f");
    s_gl.glNormal3f = (glNormal3f_t) s_gles_lib->findSymbol("glNormal3f");
    s_gl.glOrthof = (glOrthof_t) s_gles_lib->findSymbol("glOrthof");
    s_gl.glPointParameterf = (glPointParameterf_t) s_gles_lib->findSymbol("glPointParameterf");
    s_gl.glPointParameterfv = (glPointParameterfv_t) s_gles_lib->findSymbol("glPointParameterfv");
    s_gl.glPointSize = (glPointSize_t) s_gles_lib->findSymbol("glPointSize");
    s_gl.glPolygonOffset = (glPolygonOffset_t) s_gles_lib->findSymbol("glPolygonOffset");
    s_gl.glRotatef = (glRotatef_t) s_gles_lib->findSymbol("glRotatef");
    s_gl.glScalef = (glScalef_t) s_gles_lib->findSymbol("glScalef");
    s_gl.glTexEnvf = (glTexEnvf_t) s_gles_lib->findSymbol("glTexEnvf");
    s_gl.glTexEnvfv = (glTexEnvfv_t) s_gles_lib->findSymbol("glTexEnvfv");
    s_gl.glTexParameterf = (glTexParameterf_t) s_gles_lib->findSymbol("glTexParameterf");
    s_gl.glTexParameterfv = (glTexParameterfv_t) s_gles_lib->findSymbol("glTexParameterfv");
    s_gl.glTranslatef = (glTranslatef_t) s_gles_lib->findSymbol("glTranslatef");
    s_gl.glActiveTexture = (glActiveTexture_t) s_gles_lib->findSymbol("glActiveTexture");
    s_gl.glAlphaFuncx = (glAlphaFuncx_t) s_gles_lib->findSymbol("glAlphaFuncx");
    s_gl.glBindBuffer = (glBindBuffer_t) s_gles_lib->findSymbol("glBindBuffer");
    s_gl.glBindTexture = (glBindTexture_t) s_gles_lib->findSymbol("glBindTexture");
    s_gl.glBlendFunc = (glBlendFunc_t) s_gles_lib->findSymbol("glBlendFunc");
    s_gl.glBufferData = (glBufferData_t) s_gles_lib->findSymbol("glBufferData");
    s_gl.glBufferSubData = (glBufferSubData_t) s_gles_lib->findSymbol("glBufferSubData");
    s_gl.glClear = (glClear_t) s_gles_lib->findSymbol("glClear");
    s_gl.glClearColorx = (glClearColorx_t) s_gles_lib->findSymbol("glClearColorx");
    s_gl.glClearDepthx = (glClearDepthx_t) s_gles_lib->findSymbol("glClearDepthx");
    s_gl.glClearStencil = (glClearStencil_t) s_gles_lib->findSymbol("glClearStencil");
    s_gl.glClientActiveTexture = (glClientActiveTexture_t) s_gles_lib->findSymbol("glClientActiveTexture");
    s_gl.glClipPlanex = (glClipPlanex_t) s_gles_lib->findSymbol("glClipPlanex");
    s_gl.glColor4ub = (glColor4ub_t) s_gles_lib->findSymbol("glColor4ub");
    s_gl.glColor4x = (glColor4x_t) s_gles_lib->findSymbol("glColor4x");
    s_gl.glColorMask = (glColorMask_t) s_gles_lib->findSymbol("glColorMask");
    s_gl.glColorPointer = (glColorPointer_t) s_gles_lib->findSymbol("glColorPointer");
    s_gl.glCompressedTexImage2D = (glCompressedTexImage2D_t) s_gles_lib->findSymbol("glCompressedTexImage2D");
    s_gl.glCompressedTexSubImage2D = (glCompressedTexSubImage2D_t) s_gles_lib->findSymbol("glCompressedTexSubImage2D");
    s_gl.glCopyTexImage2D = (glCopyTexImage2D_t) s_gles_lib->findSymbol("glCopyTexImage2D");
    s_gl.glCopyTexSubImage2D = (glCopyTexSubImage2D_t) s_gles_lib->findSymbol("glCopyTexSubImage2D");
    s_gl.glCullFace = (glCullFace_t) s_gles_lib->findSymbol("glCullFace");
    s_gl.glDeleteBuffers = (glDeleteBuffers_t) s_gles_lib->findSymbol("glDeleteBuffers");
    s_gl.glDeleteTextures = (glDeleteTextures_t) s_gles_lib->findSymbol("glDeleteTextures");
    s_gl.glDepthFunc = (glDepthFunc_t) s_gles_lib->findSymbol("glDepthFunc");
    s_gl.glDepthMask = (glDepthMask_t) s_gles_lib->findSymbol("glDepthMask");
    s_gl.glDepthRangex = (glDepthRangex_t) s_gles_lib->findSymbol("glDepthRangex");
    s_gl.glDisable = (glDisable_t) s_gles_lib->findSymbol("glDisable");
    s_gl.glDisableClientState = (glDisableClientState_t) s_gles_lib->findSymbol("glDisableClientState");
    s_gl.glDrawArrays = (glDrawArrays_t) s_gles_lib->findSymbol("glDrawArrays");
    s_gl.glDrawElements = (glDrawElements_t) s_gles_lib->findSymbol("glDrawElements");
    s_gl.glEnable = (glEnable_t) s_gles_lib->findSymbol("glEnable");
    s_gl.glEnableClientState = (glEnableClientState_t) s_gles_lib->findSymbol("glEnableClientState");
    s_gl.glFinish = (glFinish_t) s_gles_lib->findSymbol("glFinish");
    s_gl.glFlush = (glFlush_t) s_gles_lib->findSymbol("glFlush");
    s_gl.glFogx = (glFogx_t) s_gles_lib->findSymbol("glFogx");
    s_gl.glFogxv = (glFogxv_t) s_gles_lib->findSymbol("glFogxv");
    s_gl.glFrontFace = (glFrontFace_t) s_gles_lib->findSymbol("glFrontFace");
    s_gl.glFrustumx = (glFrustumx_t) s_gles_lib->findSymbol("glFrustumx");
    s_gl.glGetBooleanv = (glGetBooleanv_t) s_gles_lib->findSymbol("glGetBooleanv");
    s_gl.glGetBufferParameteriv = (glGetBufferParameteriv_t) s_gles_lib->findSymbol("glGetBufferParameteriv");
    s_gl.glGetClipPlanex = (glGetClipPlanex_t) s_gles_lib->findSymbol("glGetClipPlanex");
    s_gl.glGenBuffers = (glGenBuffers_t) s_gles_lib->findSymbol("glGenBuffers");
    s_gl.glGenTextures = (glGenTextures_t) s_gles_lib->findSymbol("glGenTextures");
    s_gl.glGetError = (glGetError_t) s_gles_lib->findSymbol("glGetError");
    s_gl.glGetFixedv = (glGetFixedv_t) s_gles_lib->findSymbol("glGetFixedv");
    s_gl.glGetIntegerv = (glGetIntegerv_t) s_gles_lib->findSymbol("glGetIntegerv");
    s_gl.glGetLightxv = (glGetLightxv_t) s_gles_lib->findSymbol("glGetLightxv");
    s_gl.glGetMaterialxv = (glGetMaterialxv_t) s_gles_lib->findSymbol("glGetMaterialxv");
    s_gl.glGetPointerv = (glGetPointerv_t) s_gles_lib->findSymbol("glGetPointerv");
    s_gl.glGetString = (glGetString_t) s_gles_lib->findSymbol("glGetString");
    s_gl.glGetTexEnviv = (glGetTexEnviv_t) s_gles_lib->findSymbol("glGetTexEnviv");
    s_gl.glGetTexEnvxv = (glGetTexEnvxv_t) s_gles_lib->findSymbol("glGetTexEnvxv");
    s_gl.glGetTexParameteriv = (glGetTexParameteriv_t) s_gles_lib->findSymbol("glGetTexParameteriv");
    s_gl.glGetTexParameterxv = (glGetTexParameterxv_t) s_gles_lib->findSymbol("glGetTexParameterxv");
    s_gl.glHint = (glHint_t) s_gles_lib->findSymbol("glHint");
    s_gl.glIsBuffer = (glIsBuffer_t) s_gles_lib->findSymbol("glIsBuffer");
    s_gl.glIsEnabled = (glIsEnabled_t) s_gles_lib->findSymbol("glIsEnabled");
    s_gl.glIsTexture = (glIsTexture_t) s_gles_lib->findSymbol("glIsTexture");
    s_gl.glLightModelx = (glLightModelx_t) s_gles_lib->findSymbol("glLightModelx");
    s_gl.glLightModelxv = (glLightModelxv_t) s_gles_lib->findSymbol("glLightModelxv");
    s_gl.glLightx = (glLightx_t) s_gles_lib->findSymbol("glLightx");
    s_gl.glLightxv = (glLightxv_t) s_gles_lib->findSymbol("glLightxv");
    s_gl.glLineWidthx = (glLineWidthx_t) s_gles_lib->findSymbol("glLineWidthx");
    s_gl.glLoadIdentity = (glLoadIdentity_t) s_gles_lib->findSymbol("glLoadIdentity");
    s_gl.glLoadMatrixx = (glLoadMatrixx_t) s_gles_lib->findSymbol("glLoadMatrixx");
    s_gl.glLogicOp = (glLogicOp_t) s_gles_lib->findSymbol("glLogicOp");
    s_gl.glMaterialx = (glMaterialx_t) s_gles_lib->findSymbol("glMaterialx");
    s_gl.glMaterialxv = (glMaterialxv_t) s_gles_lib->findSymbol("glMaterialxv");
    s_gl.glMatrixMode = (glMatrixMode_t) s_gles_lib->findSymbol("glMatrixMode");
    s_gl.glMultMatrixx = (glMultMatrixx_t) s_gles_lib->findSymbol("glMultMatrixx");
    s_gl.glMultiTexCoord4x = (glMultiTexCoord4x_t) s_gles_lib->findSymbol("glMultiTexCoord4x");
    s_gl.glNormal3x = (glNormal3x_t) s_gles_lib->findSymbol("glNormal3x");
    s_gl.glNormalPointer = (glNormalPointer_t) s_gles_lib->findSymbol("glNormalPointer");
    s_gl.glOrthox = (glOrthox_t) s_gles_lib->findSymbol("glOrthox");
    s_gl.glPixelStorei = (glPixelStorei_t) s_gles_lib->findSymbol("glPixelStorei");
    s_gl.glPointParameterx = (glPointParameterx_t) s_gles_lib->findSymbol("glPointParameterx");
    s_gl.glPointParameterxv = (glPointParameterxv_t) s_gles_lib->findSymbol("glPointParameterxv");
    s_gl.glPointSizex = (glPointSizex_t) s_gles_lib->findSymbol("glPointSizex");
    s_gl.glPolygonOffsetx = (glPolygonOffsetx_t) s_gles_lib->findSymbol("glPolygonOffsetx");
    s_gl.glPopMatrix = (glPopMatrix_t) s_gles_lib->findSymbol("glPopMatrix");
    s_gl.glPushMatrix = (glPushMatrix_t) s_gles_lib->findSymbol("glPushMatrix");
    s_gl.glReadPixels = (glReadPixels_t) s_gles_lib->findSymbol("glReadPixels");
    s_gl.glRotatex = (glRotatex_t) s_gles_lib->findSymbol("glRotatex");
    s_gl.glSampleCoverage = (glSampleCoverage_t) s_gles_lib->findSymbol("glSampleCoverage");
    s_gl.glSampleCoveragex = (glSampleCoveragex_t) s_gles_lib->findSymbol("glSampleCoveragex");
    s_gl.glScalex = (glScalex_t) s_gles_lib->findSymbol("glScalex");
    s_gl.glScissor = (glScissor_t) s_gles_lib->findSymbol("glScissor");
    s_gl.glShadeModel = (glShadeModel_t) s_gles_lib->findSymbol("glShadeModel");
    s_gl.glStencilFunc = (glStencilFunc_t) s_gles_lib->findSymbol("glStencilFunc");
    s_gl.glStencilMask = (glStencilMask_t) s_gles_lib->findSymbol("glStencilMask");
    s_gl.glStencilOp = (glStencilOp_t) s_gles_lib->findSymbol("glStencilOp");
    s_gl.glTexCoordPointer = (glTexCoordPointer_t) s_gles_lib->findSymbol("glTexCoordPointer");
    s_gl.glTexEnvi = (glTexEnvi_t) s_gles_lib->findSymbol("glTexEnvi");
    s_gl.glTexEnvx = (glTexEnvx_t) s_gles_lib->findSymbol("glTexEnvx");
    s_gl.glTexEnviv = (glTexEnviv_t) s_gles_lib->findSymbol("glTexEnviv");
    s_gl.glTexEnvxv = (glTexEnvxv_t) s_gles_lib->findSymbol("glTexEnvxv");
    s_gl.glTexImage2D = (glTexImage2D_t) s_gles_lib->findSymbol("glTexImage2D");
    s_gl.glTexParameteri = (glTexParameteri_t) s_gles_lib->findSymbol("glTexParameteri");
    s_gl.glTexParameterx = (glTexParameterx_t) s_gles_lib->findSymbol("glTexParameterx");
    s_gl.glTexParameteriv = (glTexParameteriv_t) s_gles_lib->findSymbol("glTexParameteriv");
    s_gl.glTexParameterxv = (glTexParameterxv_t) s_gles_lib->findSymbol("glTexParameterxv");
    s_gl.glTexSubImage2D = (glTexSubImage2D_t) s_gles_lib->findSymbol("glTexSubImage2D");
    s_gl.glTranslatex = (glTranslatex_t) s_gles_lib->findSymbol("glTranslatex");
    s_gl.glVertexPointer = (glVertexPointer_t) s_gles_lib->findSymbol("glVertexPointer");
    s_gl.glViewport = (glViewport_t) s_gles_lib->findSymbol("glViewport");
    s_gl.glPointSizePointerOES = (glPointSizePointerOES_t) s_gles_lib->findSymbol("glPointSizePointerOES");
    s_gl.glBlendEquationSeparateOES = (glBlendEquationSeparateOES_t) s_gles_lib->findSymbol("glBlendEquationSeparateOES");
    s_gl.glBlendFuncSeparateOES = (glBlendFuncSeparateOES_t) s_gles_lib->findSymbol("glBlendFuncSeparateOES");
    s_gl.glBlendEquationOES = (glBlendEquationOES_t) s_gles_lib->findSymbol("glBlendEquationOES");
    s_gl.glDrawTexsOES = (glDrawTexsOES_t) s_gles_lib->findSymbol("glDrawTexsOES");
    s_gl.glDrawTexiOES = (glDrawTexiOES_t) s_gles_lib->findSymbol("glDrawTexiOES");
    s_gl.glDrawTexxOES = (glDrawTexxOES_t) s_gles_lib->findSymbol("glDrawTexxOES");
    s_gl.glDrawTexsvOES = (glDrawTexsvOES_t) s_gles_lib->findSymbol("glDrawTexsvOES");
    s_gl.glDrawTexivOES = (glDrawTexivOES_t) s_gles_lib->findSymbol("glDrawTexivOES");
    s_gl.glDrawTexxvOES = (glDrawTexxvOES_t) s_gles_lib->findSymbol("glDrawTexxvOES");
    s_gl.glDrawTexfOES = (glDrawTexfOES_t) s_gles_lib->findSymbol("glDrawTexfOES");
    s_gl.glDrawTexfvOES = (glDrawTexfvOES_t) s_gles_lib->findSymbol("glDrawTexfvOES");
    s_gl.glEGLImageTargetTexture2DOES = (glEGLImageTargetTexture2DOES_t) s_gles_lib->findSymbol("glEGLImageTargetTexture2DOES");
    s_gl.glEGLImageTargetRenderbufferStorageOES = (glEGLImageTargetRenderbufferStorageOES_t) s_gles_lib->findSymbol("glEGLImageTargetRenderbufferStorageOES");
    s_gl.glAlphaFuncxOES = (glAlphaFuncxOES_t) s_gles_lib->findSymbol("glAlphaFuncxOES");
    s_gl.glClearColorxOES = (glClearColorxOES_t) s_gles_lib->findSymbol("glClearColorxOES");
    s_gl.glClearDepthxOES = (glClearDepthxOES_t) s_gles_lib->findSymbol("glClearDepthxOES");
    s_gl.glClipPlanexOES = (glClipPlanexOES_t) s_gles_lib->findSymbol("glClipPlanexOES");
    s_gl.glColor4xOES = (glColor4xOES_t) s_gles_lib->findSymbol("glColor4xOES");
    s_gl.glDepthRangexOES = (glDepthRangexOES_t) s_gles_lib->findSymbol("glDepthRangexOES");
    s_gl.glFogxOES = (glFogxOES_t) s_gles_lib->findSymbol("glFogxOES");
    s_gl.glFogxvOES = (glFogxvOES_t) s_gles_lib->findSymbol("glFogxvOES");
    s_gl.glFrustumxOES = (glFrustumxOES_t) s_gles_lib->findSymbol("glFrustumxOES");
    s_gl.glGetClipPlanexOES = (glGetClipPlanexOES_t) s_gles_lib->findSymbol("glGetClipPlanexOES");
    s_gl.glGetFixedvOES = (glGetFixedvOES_t) s_gles_lib->findSymbol("glGetFixedvOES");
    s_gl.glGetLightxvOES = (glGetLightxvOES_t) s_gles_lib->findSymbol("glGetLightxvOES");
    s_gl.glGetMaterialxvOES = (glGetMaterialxvOES_t) s_gles_lib->findSymbol("glGetMaterialxvOES");
    s_gl.glGetTexEnvxvOES = (glGetTexEnvxvOES_t) s_gles_lib->findSymbol("glGetTexEnvxvOES");
    s_gl.glGetTexParameterxvOES = (glGetTexParameterxvOES_t) s_gles_lib->findSymbol("glGetTexParameterxvOES");
    s_gl.glLightModelxOES = (glLightModelxOES_t) s_gles_lib->findSymbol("glLightModelxOES");
    s_gl.glLightModelxvOES = (glLightModelxvOES_t) s_gles_lib->findSymbol("glLightModelxvOES");
    s_gl.glLightxOES = (glLightxOES_t) s_gles_lib->findSymbol("glLightxOES");
    s_gl.glLightxvOES = (glLightxvOES_t) s_gles_lib->findSymbol("glLightxvOES");
    s_gl.glLineWidthxOES = (glLineWidthxOES_t) s_gles_lib->findSymbol("glLineWidthxOES");
    s_gl.glLoadMatrixxOES = (glLoadMatrixxOES_t) s_gles_lib->findSymbol("glLoadMatrixxOES");
    s_gl.glMaterialxOES = (glMaterialxOES_t) s_gles_lib->findSymbol("glMaterialxOES");
    s_gl.glMaterialxvOES = (glMaterialxvOES_t) s_gles_lib->findSymbol("glMaterialxvOES");
    s_gl.glMultMatrixxOES = (glMultMatrixxOES_t) s_gles_lib->findSymbol("glMultMatrixxOES");
    s_gl.glMultiTexCoord4xOES = (glMultiTexCoord4xOES_t) s_gles_lib->findSymbol("glMultiTexCoord4xOES");
    s_gl.glNormal3xOES = (glNormal3xOES_t) s_gles_lib->findSymbol("glNormal3xOES");
    s_gl.glOrthoxOES = (glOrthoxOES_t) s_gles_lib->findSymbol("glOrthoxOES");
    s_gl.glPointParameterxOES = (glPointParameterxOES_t) s_gles_lib->findSymbol("glPointParameterxOES");
    s_gl.glPointParameterxvOES = (glPointParameterxvOES_t) s_gles_lib->findSymbol("glPointParameterxvOES");
    s_gl.glPointSizexOES = (glPointSizexOES_t) s_gles_lib->findSymbol("glPointSizexOES");
    s_gl.glPolygonOffsetxOES = (glPolygonOffsetxOES_t) s_gles_lib->findSymbol("glPolygonOffsetxOES");
    s_gl.glRotatexOES = (glRotatexOES_t) s_gles_lib->findSymbol("glRotatexOES");
    s_gl.glSampleCoveragexOES = (glSampleCoveragexOES_t) s_gles_lib->findSymbol("glSampleCoveragexOES");
    s_gl.glScalexOES = (glScalexOES_t) s_gles_lib->findSymbol("glScalexOES");
    s_gl.glTexEnvxOES = (glTexEnvxOES_t) s_gles_lib->findSymbol("glTexEnvxOES");
    s_gl.glTexEnvxvOES = (glTexEnvxvOES_t) s_gles_lib->findSymbol("glTexEnvxvOES");
    s_gl.glTexParameterxOES = (glTexParameterxOES_t) s_gles_lib->findSymbol("glTexParameterxOES");
    s_gl.glTexParameterxvOES = (glTexParameterxvOES_t) s_gles_lib->findSymbol("glTexParameterxvOES");
    s_gl.glTranslatexOES = (glTranslatexOES_t) s_gles_lib->findSymbol("glTranslatexOES");
    s_gl.glIsRenderbufferOES = (glIsRenderbufferOES_t) s_gles_lib->findSymbol("glIsRenderbufferOES");
    s_gl.glBindRenderbufferOES = (glBindRenderbufferOES_t) s_gles_lib->findSymbol("glBindRenderbufferOES");
    s_gl.glDeleteRenderbuffersOES = (glDeleteRenderbuffersOES_t) s_gles_lib->findSymbol("glDeleteRenderbuffersOES");
    s_gl.glGenRenderbuffersOES = (glGenRenderbuffersOES_t) s_gles_lib->findSymbol("glGenRenderbuffersOES");
    s_gl.glRenderbufferStorageOES = (glRenderbufferStorageOES_t) s_gles_lib->findSymbol("glRenderbufferStorageOES");
    s_gl.glGetRenderbufferParameterivOES = (glGetRenderbufferParameterivOES_t) s_gles_lib->findSymbol("glGetRenderbufferParameterivOES");
    s_gl.glIsFramebufferOES = (glIsFramebufferOES_t) s_gles_lib->findSymbol("glIsFramebufferOES");
    s_gl.glBindFramebufferOES = (glBindFramebufferOES_t) s_gles_lib->findSymbol("glBindFramebufferOES");
    s_gl.glDeleteFramebuffersOES = (glDeleteFramebuffersOES_t) s_gles_lib->findSymbol("glDeleteFramebuffersOES");
    s_gl.glGenFramebuffersOES = (glGenFramebuffersOES_t) s_gles_lib->findSymbol("glGenFramebuffersOES");
    s_gl.glCheckFramebufferStatusOES = (glCheckFramebufferStatusOES_t) s_gles_lib->findSymbol("glCheckFramebufferStatusOES");
    s_gl.glFramebufferRenderbufferOES = (glFramebufferRenderbufferOES_t) s_gles_lib->findSymbol("glFramebufferRenderbufferOES");
    s_gl.glFramebufferTexture2DOES = (glFramebufferTexture2DOES_t) s_gles_lib->findSymbol("glFramebufferTexture2DOES");
    s_gl.glGetFramebufferAttachmentParameterivOES = (glGetFramebufferAttachmentParameterivOES_t) s_gles_lib->findSymbol("glGetFramebufferAttachmentParameterivOES");
    s_gl.glGenerateMipmapOES = (glGenerateMipmapOES_t) s_gles_lib->findSymbol("glGenerateMipmapOES");
    s_gl.glMapBufferOES = (glMapBufferOES_t) s_gles_lib->findSymbol("glMapBufferOES");
    s_gl.glUnmapBufferOES = (glUnmapBufferOES_t) s_gles_lib->findSymbol("glUnmapBufferOES");
    s_gl.glGetBufferPointervOES = (glGetBufferPointervOES_t) s_gles_lib->findSymbol("glGetBufferPointervOES");
    s_gl.glCurrentPaletteMatrixOES = (glCurrentPaletteMatrixOES_t) s_gles_lib->findSymbol("glCurrentPaletteMatrixOES");
    s_gl.glLoadPaletteFromModelViewMatrixOES = (glLoadPaletteFromModelViewMatrixOES_t) s_gles_lib->findSymbol("glLoadPaletteFromModelViewMatrixOES");
    s_gl.glMatrixIndexPointerOES = (glMatrixIndexPointerOES_t) s_gles_lib->findSymbol("glMatrixIndexPointerOES");
    s_gl.glWeightPointerOES = (glWeightPointerOES_t) s_gles_lib->findSymbol("glWeightPointerOES");
    s_gl.glQueryMatrixxOES = (glQueryMatrixxOES_t) s_gles_lib->findSymbol("glQueryMatrixxOES");
    s_gl.glDepthRangefOES = (glDepthRangefOES_t) s_gles_lib->findSymbol("glDepthRangefOES");
    s_gl.glFrustumfOES = (glFrustumfOES_t) s_gles_lib->findSymbol("glFrustumfOES");
    s_gl.glOrthofOES = (glOrthofOES_t) s_gles_lib->findSymbol("glOrthofOES");
    s_gl.glClipPlanefOES = (glClipPlanefOES_t) s_gles_lib->findSymbol("glClipPlanefOES");
    s_gl.glGetClipPlanefOES = (glGetClipPlanefOES_t) s_gles_lib->findSymbol("glGetClipPlanefOES");
    s_gl.glClearDepthfOES = (glClearDepthfOES_t) s_gles_lib->findSymbol("glClearDepthfOES");
    s_gl.glTexGenfOES = (glTexGenfOES_t) s_gles_lib->findSymbol("glTexGenfOES");
    s_gl.glTexGenfvOES = (glTexGenfvOES_t) s_gles_lib->findSymbol("glTexGenfvOES");
    s_gl.glTexGeniOES = (glTexGeniOES_t) s_gles_lib->findSymbol("glTexGeniOES");
    s_gl.glTexGenivOES = (glTexGenivOES_t) s_gles_lib->findSymbol("glTexGenivOES");
    s_gl.glTexGenxOES = (glTexGenxOES_t) s_gles_lib->findSymbol("glTexGenxOES");
    s_gl.glTexGenxvOES = (glTexGenxvOES_t) s_gles_lib->findSymbol("glTexGenxvOES");
    s_gl.glGetTexGenfvOES = (glGetTexGenfvOES_t) s_gles_lib->findSymbol("glGetTexGenfvOES");
    s_gl.glGetTexGenivOES = (glGetTexGenivOES_t) s_gles_lib->findSymbol("glGetTexGenivOES");
    s_gl.glGetTexGenxvOES = (glGetTexGenxvOES_t) s_gles_lib->findSymbol("glGetTexGenxvOES");
    s_gl.glBindVertexArrayOES = (glBindVertexArrayOES_t) s_gles_lib->findSymbol("glBindVertexArrayOES");
    s_gl.glDeleteVertexArraysOES = (glDeleteVertexArraysOES_t) s_gles_lib->findSymbol("glDeleteVertexArraysOES");
    s_gl.glGenVertexArraysOES = (glGenVertexArraysOES_t) s_gles_lib->findSymbol("glGenVertexArraysOES");
    s_gl.glIsVertexArrayOES = (glIsVertexArrayOES_t) s_gles_lib->findSymbol("glIsVertexArrayOES");
    s_gl.glDiscardFramebufferEXT = (glDiscardFramebufferEXT_t) s_gles_lib->findSymbol("glDiscardFramebufferEXT");
    s_gl.glMultiDrawArraysEXT = (glMultiDrawArraysEXT_t) s_gles_lib->findSymbol("glMultiDrawArraysEXT");
    s_gl.glMultiDrawElementsEXT = (glMultiDrawElementsEXT_t) s_gles_lib->findSymbol("glMultiDrawElementsEXT");
    s_gl.glClipPlanefIMG = (glClipPlanefIMG_t) s_gles_lib->findSymbol("glClipPlanefIMG");
    s_gl.glClipPlanexIMG = (glClipPlanexIMG_t) s_gles_lib->findSymbol("glClipPlanexIMG");
    s_gl.glRenderbufferStorageMultisampleIMG = (glRenderbufferStorageMultisampleIMG_t) s_gles_lib->findSymbol("glRenderbufferStorageMultisampleIMG");
    s_gl.glFramebufferTexture2DMultisampleIMG = (glFramebufferTexture2DMultisampleIMG_t) s_gles_lib->findSymbol("glFramebufferTexture2DMultisampleIMG");
    s_gl.glDeleteFencesNV = (glDeleteFencesNV_t) s_gles_lib->findSymbol("glDeleteFencesNV");
    s_gl.glGenFencesNV = (glGenFencesNV_t) s_gles_lib->findSymbol("glGenFencesNV");
    s_gl.glIsFenceNV = (glIsFenceNV_t) s_gles_lib->findSymbol("glIsFenceNV");
    s_gl.glTestFenceNV = (glTestFenceNV_t) s_gles_lib->findSymbol("glTestFenceNV");
    s_gl.glGetFenceivNV = (glGetFenceivNV_t) s_gles_lib->findSymbol("glGetFenceivNV");
    s_gl.glFinishFenceNV = (glFinishFenceNV_t) s_gles_lib->findSymbol("glFinishFenceNV");
    s_gl.glSetFenceNV = (glSetFenceNV_t) s_gles_lib->findSymbol("glSetFenceNV");
    s_gl.glGetDriverControlsQCOM = (glGetDriverControlsQCOM_t) s_gles_lib->findSymbol("glGetDriverControlsQCOM");
    s_gl.glGetDriverControlStringQCOM = (glGetDriverControlStringQCOM_t) s_gles_lib->findSymbol("glGetDriverControlStringQCOM");
    s_gl.glEnableDriverControlQCOM = (glEnableDriverControlQCOM_t) s_gles_lib->findSymbol("glEnableDriverControlQCOM");
    s_gl.glDisableDriverControlQCOM = (glDisableDriverControlQCOM_t) s_gles_lib->findSymbol("glDisableDriverControlQCOM");
    s_gl.glExtGetTexturesQCOM = (glExtGetTexturesQCOM_t) s_gles_lib->findSymbol("glExtGetTexturesQCOM");
    s_gl.glExtGetBuffersQCOM = (glExtGetBuffersQCOM_t) s_gles_lib->findSymbol("glExtGetBuffersQCOM");
    s_gl.glExtGetRenderbuffersQCOM = (glExtGetRenderbuffersQCOM_t) s_gles_lib->findSymbol("glExtGetRenderbuffersQCOM");
    s_gl.glExtGetFramebuffersQCOM = (glExtGetFramebuffersQCOM_t) s_gles_lib->findSymbol("glExtGetFramebuffersQCOM");
    s_gl.glExtGetTexLevelParameterivQCOM = (glExtGetTexLevelParameterivQCOM_t) s_gles_lib->findSymbol("glExtGetTexLevelParameterivQCOM");
    s_gl.glExtTexObjectStateOverrideiQCOM = (glExtTexObjectStateOverrideiQCOM_t) s_gles_lib->findSymbol("glExtTexObjectStateOverrideiQCOM");
    s_gl.glExtGetTexSubImageQCOM = (glExtGetTexSubImageQCOM_t) s_gles_lib->findSymbol("glExtGetTexSubImageQCOM");
    s_gl.glExtGetBufferPointervQCOM = (glExtGetBufferPointervQCOM_t) s_gles_lib->findSymbol("glExtGetBufferPointervQCOM");
    s_gl.glExtGetShadersQCOM = (glExtGetShadersQCOM_t) s_gles_lib->findSymbol("glExtGetShadersQCOM");
    s_gl.glExtGetProgramsQCOM = (glExtGetProgramsQCOM_t) s_gles_lib->findSymbol("glExtGetProgramsQCOM");
    s_gl.glExtIsProgramBinaryQCOM = (glExtIsProgramBinaryQCOM_t) s_gles_lib->findSymbol("glExtIsProgramBinaryQCOM");
    s_gl.glExtGetProgramBinarySourceQCOM = (glExtGetProgramBinarySourceQCOM_t) s_gles_lib->findSymbol("glExtGetProgramBinarySourceQCOM");
    s_gl.glStartTilingQCOM = (glStartTilingQCOM_t) s_gles_lib->findSymbol("glStartTilingQCOM");
    s_gl.glEndTilingQCOM = (glEndTilingQCOM_t) s_gles_lib->findSymbol("glEndTilingQCOM");

    return true;
}

//
// This function is called only during initialiation before
// any thread has been created - hence it should NOT be thread safe.
//
void *gl_dispatch_get_proc_func(const char *name, void *userData)
{
    if (!s_gles_lib) {
        return NULL;
    }
    return (void *)s_gles_lib->findSymbol(name);
}
