#include "ShaderParser.h"
#include <string.h>

ShaderParser::ShaderParser():ObjectData(SHADER_DATA),
                             m_type(0),
                             m_parsedLines(NULL) {};

ShaderParser::ShaderParser(GLenum type):ObjectData(SHADER_DATA), 
                                        m_type(type),
                                        m_parsedLines(NULL) {};

void ShaderParser::setSrc(const Version& ver,GLsizei count,const GLchar** strings,const GLint* length){
    for(int i = 0;i<count;i++){
        m_src.append(strings[i]);
    }
    clearParsedSrc();

    if(getenv("NV_WAR"))
    {
        fprintf(stderr, "Workaround for nVidia's liberal shader compilation - adding #version token to shader\n");
        parseGLSLversion();
    }
    /*
      version 1.30.10 is the first version of GLSL Language containing precision qualifiers
      if the glsl version is less than 1.30.10 than we will use a shader parser which omits
      all precision qualifiers from the shader source , otherwise we will use a shader parser
      which set the default precisions to be the same as the default precisions of GLSL ES
    */
#if 0
    if(ver < Version(1,30,10)){
        parseOmitPrecision();
     } else {
        parseExtendDefaultPrecision();
     }
#else
    //XXX: Until proved otherwise, glsl doesn't know/use those precision macros, so we omit then
    parseOmitPrecision();
#endif

    parseOriginalSrc();
}
const GLchar** ShaderParser::parsedLines() {
      m_parsedLines = (GLchar*)m_parsedSrc.c_str();
      return const_cast<const GLchar**> (&m_parsedLines);
};

const char* ShaderParser::getOriginalSrc(){
    return m_src.c_str();
}

void ShaderParser::parseOriginalSrc() {
    m_parsedSrc+=m_src;
}

void ShaderParser::parseGLSLversion() {
    //if no version definition is found
    if (m_src.find("#version ", 0) == std::string::npos) {
        m_parsedSrc += "#version 100\n";
    }
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
    m_parsedSrc+=defines;
}

void ShaderParser::parseExtendDefaultPrecision(){

    //the precision lines which we need to add to the shader
    static const GLchar extend[] = {
                                      "#define GLES 1\n"
                                      "precision lowp sampler2D;\n"
                                      "precision lowp samplerCube;\n"
                                   };

    m_parsedSrc+=extend;
}

void ShaderParser::clearParsedSrc(){
    m_parsedSrc.clear();
}

GLenum ShaderParser::getType() {
    return m_type;
}

ShaderParser::~ShaderParser(){
    clearParsedSrc();
}
