#ifndef SHADER_PARSER_H
#define SHADER_PARSER_H

#include "GLESv2Context.h"
#include <string>
#include <GLES2/gl2.h>
#include <GLcommon/objectNameManager.h>

class ShaderParser:public ObjectData{
public:
    ShaderParser();
    ShaderParser(GLenum type);
    void           setSrc(const Version& ver,GLsizei count,const GLchar** strings,const GLint* length);
    const char*    getOriginalSrc();
    const GLchar** parsedLines();
    GLenum         getType();
    ~ShaderParser();

    void setInfoLog(GLchar * infoLog);
    GLchar* getInfoLog();

private:
    void parseOriginalSrc();
    void parseGLSLversion();
    void parseBuiltinConstants();
    void parseOmitPrecision();
    void parseExtendDefaultPrecision();
    void parseLineNumbers();
    void clearParsedSrc();

    GLenum      m_type;
    char*       m_originalSrc;
    std::string m_src;
    std::string m_parsedSrc;
    GLchar*     m_parsedLines;
    GLchar*     m_infoLog;
};
#endif
