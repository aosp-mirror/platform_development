#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

//#define GL_API
//#define GL_APIENTRY

#undef ANDROID
#include <EGL/egl.h>
#include <GLES2/gl2.h>

#ifdef __APPLE__
extern "C" void * createGLView(void *nsWindowPtr, int x, int y, int width, int height);
#endif

#undef HAVE_MALLOC_H
#include <SDL.h>
#include <SDL_syswm.h>


#define WINDOW_WIDTH    500
#define WINDOW_HEIGHT   500

#define TEX_WIDTH 256
#define TEX_HEIGHT 256


#define F_to_X(d) ((d) > 32767.65535 ? 32767 * 65536 + 65535 :  \
               (d) < -32768.65535 ? -32768 * 65536 + 65535 : \
               ((GLfixed) ((d) * 65536)))
#define X_to_F(x)  ((float)(x))/65536.0f

//#define __FIXED__

const char *def_vShaderStr =
       "attribute vec4 vPosition;   \n"
       "void main()                 \n"
       "{                           \n"
       "   gl_Position = vPosition; \n"
       "}                           \n";

const char *def_fShaderStr =
       "precision mediump float;                   \n"
       "void main()                                \n"
       "{                                          \n"
#ifndef __FIXED__
       " gl_FragColor = vec4(0.2, 0.5, 0.1, 1.0); \n"
#else
       " gl_FragColor = vec4(0.4, 0.3, 0.7, 1.0); \n"
#endif
       "}                                          \n";

static EGLint const attribute_list[] = {
    EGL_RED_SIZE, 1,
    EGL_GREEN_SIZE, 1,
    EGL_BLUE_SIZE, 1,
    EGL_NONE
};

unsigned char *genTexture(int width, int height, int comp)
{
    unsigned char *img = new unsigned char[width * height * comp];
    unsigned char *ptr = img;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            unsigned char col = ((i / 8 + j / 8) % 2) * 255 ;
            for (int c = 0; c < comp; c++) {
                *ptr = col; ptr++;
            }
        }
    }
    return img;
}

unsigned char *genRedTexture(int width, int height, int comp)
{
    unsigned char *img = new unsigned char[width * height * comp];
        memset(img,0,width*height*comp);
    unsigned char *ptr = img;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            unsigned char col = ((i / 8 + j / 8) % 2) * 255 ;
                        *ptr = col;
                         ptr+=comp;
        }
    }
    return img;
}


void printUsage(const char *progname)
{
    fprintf(stderr, "usage: %s [options]\n", progname);
    fprintf(stderr, "\t-vs <filename>  - vertex shader to use\n");
    fprintf(stderr, "\t-fs <filename>  - fragment shader to use\n");
}



GLuint LoadShader(GLenum type,const char *shaderSrc)
{
   GLuint shader;
   GLint compiled;
   // Create the shader object
    shader = glCreateShader(type);
   if(shader == 0)
       return 0;
   // Load the shader source
   glShaderSource(shader, 1, &shaderSrc, NULL);
    // Compile the shader
   glCompileShader(shader);
    // Check the compile status
   glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
   if(!compiled)
   {
       GLint infoLen = 0;
       glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
       if(infoLen > 1)
       {
          char* infoLog = (char*)malloc(sizeof(char) * infoLen);
          glGetShaderInfoLog(shader, infoLen, NULL, infoLog);
          printf("Error compiling shader:\n%s\n", infoLog);
          free(infoLog);
       }
       glDeleteShader(shader);
       return 0;
   }
   return shader;
}

const char *readShader(const char *fileName)
{
    FILE *fp = fopen(fileName, "rb");
    if (!fp) return NULL;
   
    int bSize = 1024;
    int nBufs = 1;
    char *buf = (char *)malloc(bSize);
    int n;
    int len = 0;
    n = fread(&buf[0], 1, bSize, fp);
    while( n == bSize ) {
        len += n;
        nBufs++;
        buf = (char *)realloc(buf, bSize * nBufs);
        n = fread(&buf[len], 1, bSize, fp);
    }
    len += n;

    buf[len] = '\0';
    return (const char *)buf;
}

void dumpUniforms(GLuint program)
{
    GLint numU;
    glGetProgramiv(program, GL_ACTIVE_UNIFORMS, &numU);
    printf("==== Program %d has %d active uniforms ===\n", program, numU);
    char name[512];
    GLsizei len;
    GLint size;
    GLenum type;
    for (int i=0; i<numU; i++) {
        glGetActiveUniform(program, i,
                           512, &len, &size, &type, name);
        printf("\t%s : type=0x%x size=%d\n", name, type, size);
    }
}

///
// Initialize the shader and program object
//
int Init(const char *vShaderStr, const char *fShaderStr)
{
   GLuint vertexShader;
   GLuint fragmentShader;
   GLuint programObject;
   GLint linked;
  // Load the vertex/fragment shaders
  vertexShader = LoadShader(GL_VERTEX_SHADER, vShaderStr);
  fragmentShader = LoadShader(GL_FRAGMENT_SHADER, fShaderStr);
  // Create the program object
  programObject = glCreateProgram();
  if(programObject == 0)
     return -1;
  glAttachShader(programObject, vertexShader);
  glAttachShader(programObject, fragmentShader);
  // Bind vPosition to attribute 0
  glBindAttribLocation(programObject, 0, "vPosition");
  // Link the program
  glLinkProgram(programObject);
  // Check the link status
  glGetProgramiv(programObject, GL_LINK_STATUS, &linked);
  if(!linked)
  {
     GLint infoLen = 0;
     glGetProgramiv(programObject, GL_INFO_LOG_LENGTH, &infoLen);
     if(infoLen > 1)
     {
        char* infoLog = (char*)malloc(sizeof(char) * infoLen);
        glGetProgramInfoLog(programObject, infoLen, NULL, infoLog);
        printf("Error linking program:\n%s\n", infoLog);
        free(infoLog);
     }
     glDeleteProgram(programObject);
     return -1;
  }

  // dump active uniforms
  dumpUniforms(programObject);

  // Store the program object
#ifndef __FIXED__
  glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
#else
  glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
#endif
  return programObject;
}


///
// Draw a triangle using the shader pair created in Init()
//
void Draw(EGLDisplay display,EGLSurface surface,int width,int height,GLuint program)
{
#ifndef __FIXED__
   GLfloat vVertices[] = {0.0f, 0.5f, 0.0f,
                           -0.5f, -0.5f, 0.0f,
                           0.5f, -0.5f, 0.0f};
#else

   GLfixed vVertices[] = {F_to_X(0.0f), F_to_X(0.5f),F_to_X(0.0f),
                           F_to_X(-0.5f),F_to_X(-0.5f), F_to_X(0.0f),
                           F_to_X(0.5f),F_to_X(-0.5f),F_to_X(0.0f)};
#endif    
   
    // Set the viewport
   glViewport(0, 0,width,height);
    // Clear the color buffer
   glClear(GL_COLOR_BUFFER_BIT);
    // Use the program object
   glUseProgram(program);
   // Load the vertex data
#ifndef __FIXED__
   glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, vVertices);
#else
   glVertexAttribPointer(0, 3, GL_FIXED, GL_FALSE, 0, vVertices);
#endif
   glEnableVertexAttribArray(0);
   glDrawArrays(GL_TRIANGLES, 0, 3);
   eglSwapBuffers(display,surface);
}

#ifdef _WIN32
char **parseCmdLine(char *cmdLine, int *argc)
{
    int argvSize = 10;
    char **argv = (char **)malloc(argvSize * sizeof(char *));
    *argc = 0;
    int i=0;
    bool prevIsSpace = true;
    int argStart = 0;

    argv[(*argc)++] = strdup("playdump");

    while(cmdLine[i] != '\0') {
        bool isSpace = (cmdLine[i] == ' ' || cmdLine[i] == '\t');
        if ( !isSpace && prevIsSpace ) {
            argStart = i;
        }
        else if (isSpace && !prevIsSpace) {
            cmdLine[i] = '\0';
            if (*argc >= argvSize) {
                argvSize *= 2;
                argv = (char **)realloc(argv, argvSize * sizeof(char *));
            }
            argv[(*argc)++] = &cmdLine[argStart];
            argStart = i+1;
        }

        prevIsSpace = isSpace;
        i++;
    }

    if (i > argStart) {
        argv[(*argc)++] = &cmdLine[argStart];
    }
    return argv;
}
#endif

#ifdef _WIN32
int WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd)
#else
int main(int argc, char **argv)
#endif
{
    GLuint  ui32Vbo = 0; // Vertex buffer object handle
    GLuint  ui32IndexVbo;
    GLuint  ui32Texture;

    int nframes = 100;
    bool immidateMode     = false;
    bool useIndices       = false;
    bool useTexture       = true;
    bool useCompTexture   = false;
    bool useFixed         = true;
    bool usePoints        = false;
    bool useCopy          = false;
    bool useSubCopy       = false;

#ifdef _WIN32
    int argc;
    char **argv = parseCmdLine(lpCmdLine, &argc);
#endif
    const char *vShader = def_vShaderStr;
    const char *fShader = def_fShaderStr;

    for (int i=1; i<argc; i++) {
        if (!strcmp(argv[i],"-vs")) {
            if (++i >= argc) {
                printUsage(argv[0]);
                return -1;
            }
            vShader = readShader(argv[i]);
            if (!vShader) {
                vShader = def_vShaderStr;
                printf("Failed to load vshader %s, using defualt\n", argv[i]);
            }
            else {
                printf("Using vshader %s\n", argv[i]);
            }
        }
        else if (!strcmp(argv[i],"-fs")) {
            if (++i >= argc) {
                printUsage(argv[0]);
                return -1;
            }
            fShader = readShader(argv[i]);
            if (!fShader) {
                fShader = def_fShaderStr;
                printf("Failed to load fshader %s, using defualt\n", argv[i]);
            }
            else {
                printf("Using fshader %s\n", argv[i]);
            }
        }
        else {
            printUsage(argv[0]);
            return -1;
        }
    }

    #ifdef _WIN32
        HWND   windowId = NULL;
    #elif __linux__
        Window windowId = NULL;
    #elif __APPLE__
        void* windowId  = NULL;
    #endif

        //      // Inialize SDL window
        //
        if (SDL_Init(SDL_INIT_NOPARACHUTE | SDL_INIT_VIDEO)) {
            fprintf(stderr,"SDL init failed: %s\n", SDL_GetError());
            return -1;
        }

        SDL_Surface *surface = SDL_SetVideoMode(WINDOW_WIDTH,WINDOW_HEIGHT, 32, SDL_HWSURFACE);
        if (surface == NULL) {
            fprintf(stderr,"Failed to set video mode: %s\n", SDL_GetError());
            return -1;
        }

        SDL_SysWMinfo  wminfo;
        memset(&wminfo, 0, sizeof(wminfo));
        SDL_GetWMInfo(&wminfo);
    #ifdef _WIN32
        windowId = wminfo.window;
    #elif __linux__
        windowId = wminfo.info.x11.window;
    #elif __APPLE__
        windowId = createGLView(wminfo.nsWindowPtr,0,0,WINDOW_WIDTH,WINDOW_HEIGHT);
    #endif

        int major,minor,num_config;
        int attrib_list[] ={    
                                EGL_CONTEXT_CLIENT_VERSION, 2,
                                EGL_NONE
                           };
        EGLConfig configs[150];
        EGLSurface egl_surface;
        EGLContext ctx;
        EGLDisplay d = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        eglInitialize(d,&major,&minor);
        printf("DISPLAY == %p major =%d minor = %d\n",d,major,minor);
        eglChooseConfig(d, attribute_list, configs, 150, &num_config);
        printf("config returned %d\n",num_config);
        egl_surface = eglCreateWindowSurface(d,configs[0],windowId,NULL);
        ctx = eglCreateContext(d,configs[0],EGL_NO_CONTEXT,attrib_list);
        printf("SURFACE == %p CONTEXT == %p\n",egl_surface,ctx);
        if(eglMakeCurrent(d,egl_surface,egl_surface,ctx)!= EGL_TRUE){
            printf("make current failed\n");
            return false;
        }
        printf("after make current\n");

        GLenum err = glGetError();
        if(err != GL_NO_ERROR) {
        printf("error before drawing ->>> %d  \n",err);
        } else {
        printf("no error before drawing\n");
        }

        int program = Init(vShader, fShader);
        if(program  < 0){
            printf("failed init shaders\n");
            return false;
        }

        Draw(d,egl_surface,WINDOW_WIDTH,WINDOW_HEIGHT,program);

                err = glGetError();
                if(err != GL_NO_ERROR)
            printf("error ->>> %d  \n",err);
        eglDestroySurface(d,egl_surface);
        eglDestroyContext(d,ctx);

// Just wait until the window is closed
        SDL_Event ev;
        while( SDL_WaitEvent(&ev) ) {
            if (ev.type == SDL_QUIT) {
                break;
            }
        }
    return 0;
}


