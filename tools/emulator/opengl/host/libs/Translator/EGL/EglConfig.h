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
#ifndef EGL_CONFIG_H
#define EGL_CONFIG_H

#include<EGL/egl.h>
#include<EGL/eglinternalplatform.h>

#define MIN_SWAP_INTERVAL 1
#define MAX_SWAP_INTERVAL 10


class EglConfig {
public:
    bool getConfAttrib(EGLint attrib,EGLint* val) const;
    bool operator<(const EglConfig& conf)   const;
    bool operator>=(const EglConfig& conf)  const;
    bool compitableWith(const EglConfig& conf)  const; //compitability
    bool choosen(const EglConfig& dummy);
    EGLint surfaceType(){ return m_surface_type;};
    EGLint id(){return m_config_id;};
    EGLint nativeId(){return m_native_config_id;};
    EGLNativePixelFormatType nativeConfig(){ return m_nativeFormat;}

    EglConfig(EGLint red_size,
              EGLint green_size,
              EGLint blue_size,
              EGLint alpha_size,
              EGLenum  caveat,
              EGLint config_id,
              EGLint depth_size,
              EGLint frame_buffer_level,
              EGLint max_pbuffer_width,
              EGLint max_pbuffer_height,
              EGLint max_pbuffer_size,
              EGLBoolean native_renderable,
              EGLint renderable_type,
              EGLint native_visual_id,
              EGLint native_visual_type,
              EGLint samples_per_pixel,
              EGLint stencil_size,
              EGLint surface_type,
              EGLenum transparent_type,
              EGLint trans_red_val,
              EGLint trans_green_val,
              EGLint trans_blue_val,
              EGLNativePixelFormatType frmt);

    EglConfig(const EglConfig& conf);

    EglConfig(const EglConfig& conf,
              EGLint config_id,
              EGLint red_size,
              EGLint green_size,
              EGLint blue_size,
              EGLint alpha_size);

private:

    const EGLint                    m_buffer_size;
    const EGLint                    m_red_size;
    const EGLint                    m_green_size;
    const EGLint                    m_blue_size;
    const EGLint                    m_alpha_size;
    const EGLBoolean                m_bind_to_tex_rgb;
    const EGLBoolean                m_bind_to_tex_rgba;
    const EGLenum                   m_caveat;
    const EGLint                    m_config_id;
    const EGLint                    m_native_config_id;
    const EGLint                    m_frame_buffer_level;
    const EGLint                    m_depth_size;
    const EGLint                    m_max_pbuffer_width;
    const EGLint                    m_max_pbuffer_height;
    const EGLint                    m_max_pbuffer_size;
    const EGLint                    m_max_swap_interval;
    const EGLint                    m_min_swap_interval;
    const EGLBoolean                m_native_renderable;
    const EGLint                    m_renderable_type;
    const EGLint                    m_native_visual_id;
    const EGLint                    m_native_visual_type;
    const EGLint                    m_sample_buffers_num;
    const EGLint                    m_samples_per_pixel;
    const EGLint                    m_stencil_size;
    const EGLint                    m_surface_type;
    const EGLenum                   m_transparent_type;
    const EGLint                    m_trans_red_val;
    const EGLint                    m_trans_green_val;
    const EGLint                    m_trans_blue_val;
    const EGLenum                   m_conformant;

    const EGLNativePixelFormatType  m_nativeFormat;
};

#endif
