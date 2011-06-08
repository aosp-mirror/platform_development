#include "ShaderParser.h"
#include <string.h>

ShaderParser::ShaderParser():m_type(0),
                             m_src(NULL),
                             m_parsedLines(NULL){};

ShaderParser::ShaderParser(GLenum type):m_type(type),
                                                    m_parsedLines(NULL){};

void ShaderParser::setSrc(const Version& ver,GLsizei count,const GLchar** strings,const GLint* length){
    for(int i = 0;i<count;i++){
        m_src.append(strings[i]);
    }
    clearParsedSrc();
    /*
      version 1.30.10 is the first version of GLSL Language containing precision qualifiers
      if the glsl version is less than 1.30.10 than we will use a shader parser which omits
      all precision qualifiers from the shader source , otherwise we will use a shader parser
      which set the default precisions to be the same as the default precisions of GLSL ES
    */
    if(ver < Version(1,30,10)){
        parseOmitPrecision();
     } else {
        parseExtendDefaultPrecision();
     }
}

const char* ShaderParser::getOriginalSrc(){
    return m_src.c_str();
}

void ShaderParser::parseOmitPrecision(){

    //defines we need to add in order to Omit precisions qualifiers
    static const GLchar defines[] = {
                                         "#define GLES 1\n"
                                         "#define lowp \n"
                                         "#define mediump \n"
                                         "#define highp \n"
                                         "#define precision \n"
                                     };

    //the lengths of defines we need to add in order to Omit precisions qualifiers
    static GLuint definesLength   = strlen(defines);

    const GLchar* origSrc = m_src.c_str();
    unsigned int origLength = strlen(origSrc);

    m_parsedLines = new GLchar[origLength + definesLength + 1];
    strncpy(m_parsedLines,defines,definesLength);
    strcpy(m_parsedLines+definesLength,origSrc);
}

void ShaderParser::parseExtendDefaultPrecision(){

    //the precision lines which we need to add to the shader
    static const GLchar extend[] = {
                                      "#define GLES 1\n"
                                      "precision lowp sampler2D;\n"
                                      "precision lowp samplerCube;\n"
                                   };

    //the length of the precision lines which we need to add to the shader
    static GLint extendLength = strlen(extend);

    const GLchar* origSrc = m_src.c_str();
    unsigned int origLength = strlen(origSrc);

    m_parsedLines = new GLchar[origLength + extendLength + 1];
    strncpy(m_parsedLines,extend,extendLength);
    strcpy(m_parsedLines+extendLength,origSrc);
}

void ShaderParser::clearParsedSrc(){
    if(m_parsedLines){
        delete[] m_parsedLines;
    }
}

ShaderParser::~ShaderParser(){
    clearParsedSrc();
}
