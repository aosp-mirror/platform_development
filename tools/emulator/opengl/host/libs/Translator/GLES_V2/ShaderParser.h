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
    const GLchar** parsedLines(){return const_cast<const GLchar**>(&m_parsedLines);};
    ~ShaderParser();

private:
    void parseOmitPrecision();
    void parseExtendDefaultPrecision();
    void clearParsedSrc();

    GLenum      m_type;
    std::string m_src;
    GLchar*     m_parsedLines;
};
#endif
