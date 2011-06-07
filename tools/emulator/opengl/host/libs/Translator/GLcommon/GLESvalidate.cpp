#include<GLcommon/GLESvalidate.h>


bool  GLESvalidate::textureEnum(GLenum e,unsigned int maxTex) {
    return e >= GL_TEXTURE0 && e <= (GL_TEXTURE0 + maxTex);
}

bool GLESvalidate::pixelType(GLenum type) {
    switch(type) {
    case GL_UNSIGNED_BYTE:
    case GL_UNSIGNED_SHORT_5_6_5:
    case GL_UNSIGNED_SHORT_4_4_4_4:
    case GL_UNSIGNED_SHORT_5_5_5_1:
        return true;
    }
    return false;
}

bool GLESvalidate::pixelOp(GLenum format,GLenum type) {
     switch(type) {
     case GL_UNSIGNED_SHORT_4_4_4_4:
     case GL_UNSIGNED_SHORT_5_5_5_1:
         return format == GL_RGBA;
     case GL_UNSIGNED_SHORT_5_6_5:
         return format == GL_RGB;
     }
     return true;
}

bool GLESvalidate::pixelFrmt(GLenum format) {
    switch(format) {
    case GL_ALPHA:
    case GL_RGB:
    case GL_RGBA:
    case GL_LUMINANCE:
    case GL_LUMINANCE_ALPHA:
        return true;
    }
    return false;
}

bool GLESvalidate::bufferTarget(GLenum target) {
    return target == GL_ARRAY_BUFFER || target == GL_ELEMENT_ARRAY_BUFFER;
}

bool GLESvalidate::bufferParam(GLenum param) {
 return  (param == GL_BUFFER_SIZE) || (param == GL_BUFFER_USAGE);
}

bool GLESvalidate::drawMode(GLenum mode) {
    switch(mode) {
    case GL_POINTS:
    case GL_LINE_STRIP:
    case GL_LINE_LOOP:
    case GL_LINES:
    case GL_TRIANGLE_STRIP:
    case GL_TRIANGLE_FAN:
    case GL_TRIANGLES:
        return true;
    }
    return false;
}

bool GLESvalidate::drawType(GLenum mode) {
    return mode == GL_UNSIGNED_BYTE || mode == GL_UNSIGNED_SHORT;
}
