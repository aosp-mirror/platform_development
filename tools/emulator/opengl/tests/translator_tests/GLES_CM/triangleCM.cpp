#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>

//#define GL_API
//#define GL_APIENTRY

#undef ANDROID
#include <EGL/egl.h>
#include <GLES/gl.h>

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
            if (j>(width/2)) col/=2;
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

//mip 0;
unsigned char *genPalette4_rgb8(int width, int height,int color)
{
        int size = width*height/2 + 16*3/*palette size*/;
    unsigned char *img = new unsigned char[size];

        memset(img,0,size);
        img[0] = 255; img[1] = 0; img[2] = 0;     // red
        img[3] = 0; img[4] = 0; img[5] = 255;    //blue
        img[7] = 128; img[8] = 0; img[9] = 128;  //fucsia
        //rest of the palette is empty

    unsigned char *ptr = img+(16*3);
    for (int i = 0; i < (height*width/2); i++) {
             ptr[i] = (i%2)?0x0|color:0x11|color;
    }
    return img;
}

void usage(const char *progname)
{
    fprintf(stderr, "usage: %s [-n <nframes> -i -h]\n", progname);
    fprintf(stderr, "\t-h: this message\n");
    fprintf(stderr, "\t-i: immidate mode\n");
    fprintf(stderr, "\t-n nframes: generate nframes\n");
    fprintf(stderr, "\t-e: use index arrays\n");
    fprintf(stderr, "\t-t: use texture\n");
    fprintf(stderr, "\t-f: use fixed points\n");
    fprintf(stderr, "\t-p: use point size OES extention\n");
}

#define SWITCH_SOURCE(add)\
            if(useConvertedType){                            \
                if(useFixed){                                \
                      data = (GLvoid*)(fixedVertices+(add)); \
                } else {                                     \
                      data = (GLvoid*)(byteVertices +(add)); \
                }                                            \
            } else {                                         \
                      data = (GLvoid*)(afVertices+(add));    \
            }                                                \

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
    bool useIndices       = true;
    bool useTexture       = false;
    bool useCompTexture   = false;
    bool useConvertedType = true;
    bool useFixed         = false;
    bool usePoints        = false;
    bool useCopy          = false;
    bool useSubCopy       = false;

    int c;
    extern char *optarg;

    #ifdef _WIN32
        HWND   windowId = NULL;
    #elif __linux__
        Window windowId = NULL;
    #elif __APPLE__
        void* windowId = NULL;
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
        EGLConfig configs[150];
        EGLSurface egl_surface;
        EGLContext ctx;
        EGLDisplay d = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        eglInitialize(d,&major,&minor);
        printf("DISPLAY == %p major =%d minor = %d\n",d,major,minor);
        eglChooseConfig(d, attribute_list, configs, 150, &num_config);
        printf("config returned %d\n",num_config);
        egl_surface = eglCreateWindowSurface(d,configs[0],windowId,NULL);
        printf("before creating context..\n");
        ctx = eglCreateContext(d,configs[0],EGL_NO_CONTEXT,NULL);
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

    if (useTexture) {

        glEnable(GL_TEXTURE_2D);
        ui32Texture = 1;
        glBindTexture(GL_TEXTURE_2D, ui32Texture);
                GLenum err = glGetError();

        unsigned char *pixels = NULL;
                if(useCompTexture) {
                        pixels = genPalette4_rgb8(TEX_WIDTH,TEX_HEIGHT,3);
                        glCompressedTexImage2D(GL_TEXTURE_2D,0,GL_PALETTE4_RGB8_OES,TEX_WIDTH,TEX_HEIGHT,0,3*16+TEX_WIDTH*TEX_HEIGHT/2,pixels);

                } else {
                        pixels = genTexture(TEX_WIDTH, TEX_HEIGHT, 4);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, TEX_WIDTH, TEX_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                }

        delete pixels;

                err = glGetError();
                if(err != GL_NO_ERROR)
            printf("error %d after image \n",err);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                err = glGetError();
                if(err != GL_NO_ERROR)
            printf("error after min filter \n");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                err = glGetError();
                if(err != GL_NO_ERROR)
            printf("error after mag filter \n");
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
                err = glGetError();
                if(err != GL_NO_ERROR)
            printf("error after env mode \n");

                if(useCompTexture) {
                    pixels = genPalette4_rgb8(TEX_WIDTH,TEX_HEIGHT,1);
                    glCompressedTexSubImage2D(GL_TEXTURE_2D,0,TEX_WIDTH/4,TEX_HEIGHT/4,TEX_WIDTH/8,TEX_HEIGHT/8,GL_PALETTE4_RGB8_OES,3*16+(TEX_WIDTH*TEX_HEIGHT/128),pixels);
                } else {
            pixels = genRedTexture(TEX_WIDTH/8, TEX_HEIGHT/8, 4);
                    glTexSubImage2D(GL_TEXTURE_2D,0,TEX_WIDTH/4,TEX_HEIGHT/4,TEX_WIDTH/8,TEX_HEIGHT/8,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
                }
                 err = glGetError();
                if(err != GL_NO_ERROR)
            printf("error %d after subimage \n",err);
        delete pixels;

    }

    glClearColor(0.6f, 0.8f, 1.0f, 1.0f); // clear blue

    float afVertices[] = {  -0.4f,-0.4f,0.0f, // Position
                 1.0f,0.0f,0.0f,1.0f, // Color
                 0.0f, 0.0f, // texture
                 12.f, //point size

                 0.4f,-0.4f,0.0f,
                 0.0f,1.0f,0.0f,1.0f,
                 1.0f, 0.0f,
                 47.0f,

                 0.0f,0.4f,0.0f,
                 0.0f,0.0f,1.0f,1.0f,
                 0.5f, 1.0f,
                 14.0f
    };

#define MAX_T 1
#define MID_T 0
#define MIN_T 0

    GLbyte byteVertices[] = { -1,-1,0, // Position
                             255,0,0,255, // Color
                             MIN_T, MIN_T, // texture
                             12, //point size

                             1,-1,0,
                             0,255,0,255,
                             MAX_T,MIN_T,
                             47,

                            0,1,0,
                            0,0,255,255,
                            MID_T, MAX_T,
                            14
    };

    GLfixed fixedVertices[] = { F_to_X(-0.4f),F_to_X(-0.4f),F_to_X(0.0f), // Position
                    F_to_X(1.0f),F_to_X(0.0f),F_to_X(0.0f),F_to_X(1.0f), // Color
                    F_to_X(0.0f),F_to_X(0.0f), // texture
                    F_to_X(12.0f),//points size

                    F_to_X(0.4f),F_to_X(-0.4f),F_to_X(0.0f),
                    F_to_X(0.0f),F_to_X(1.0f),F_to_X(0.0f),F_to_X(1.0f),
                    F_to_X(1.0f),F_to_X( 0.0f),
                    F_to_X(30.0f),

                    F_to_X(0.0f),F_to_X(0.4f),F_to_X(0.0f),
                    F_to_X(0.0f),F_to_X(0.0f),F_to_X(1.0f),F_to_X(1.0f),
                    F_to_X(0.5f), F_to_X(1.0f),
                    F_to_X(30.0)
    };

    unsigned short indices[] = { 2, 1, 0 };

    if (!immidateMode) {
        glGenBuffers(1, &ui32Vbo);
        ui32Vbo = 1;
         printf("ui32Vbo = %d\n", ui32Vbo);

        glBindBuffer(GL_ARRAY_BUFFER, ui32Vbo);
        void* data = (void*)afVertices;
        unsigned int uiSize = 3*(sizeof(float)*10);
        if(useConvertedType){
           if(useFixed){
               data = (void*)fixedVertices;
           } else {
               data   = (void*)byteVertices;
               uiSize = 3*(sizeof(GLbyte)*10);
           }
        }
        glBufferData(GL_ARRAY_BUFFER, uiSize,data, GL_STATIC_DRAW);

        ui32IndexVbo = 2;
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ui32IndexVbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices, GL_STATIC_DRAW);
    }

    // Draws a triangle for 800 frames
    float angle = 0.0;
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();

    GLvoid* arr = NULL;
    GLenum  type;
    GLenum  drawType;
    GLenum  colorType;
    int     size_of;

    if(useConvertedType){
        if(useFixed)
        {
            arr = fixedVertices;
            colorType = type = GL_FIXED;
            size_of = sizeof(GLfixed);
        } else {
            arr = byteVertices;
            colorType = GL_UNSIGNED_BYTE;
            type = GL_BYTE;
            size_of = sizeof(GLbyte);
        }
    }else {
        arr = afVertices;
        colorType = type = GL_FLOAT;
        size_of = sizeof(float);
    }

    if(usePoints)
    {
        drawType = GL_POINTS;
    }
    else
        drawType = GL_TRIANGLES;

    GLvoid* data = NULL;
    for (int i = 0; i < 100; i++) {

        glClear(GL_COLOR_BUFFER_BIT);
        glPushMatrix();
        glRotatef(angle, 0.0, 0.0, 1.0);
        angle += 360.0 / nframes;
        // Enable vertex arrays
        glEnableClientState(GL_VERTEX_ARRAY);
        if (immidateMode) {
            glVertexPointer(3,type, size_of * 10, arr);
        } else {
            glVertexPointer(3,type, size_of * 10, 0);
        }

        // Set color data in the same way
        glEnableClientState(GL_COLOR_ARRAY);
        if (immidateMode) {
            SWITCH_SOURCE(3)
            glColorPointer(4, colorType, size_of * 10, data);
        } else {
            glColorPointer(4,colorType,size_of * 10, (GLvoid*) (size_of * 3) );
        }
        if (useTexture) {
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            if (immidateMode) {
                SWITCH_SOURCE(7)
                glTexCoordPointer(2, type, size_of * 10,data);
            } else {
                glTexCoordPointer(2, type, size_of * 10, (GLvoid*)(size_of * 7));
            }
        }
        if(usePoints)
        {
            glEnableClientState(GL_POINT_SIZE_ARRAY_OES);
            if (immidateMode) {
                SWITCH_SOURCE(9)
                glPointSizePointerOES(type,size_of * 10,data);
                        } else {
                glPointSizePointerOES(type,size_of * 10,(GLvoid*)(size_of * 9));
                        }
        }

        if (useIndices) {
            if (immidateMode) {
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
                glDrawElements(drawType, 3, GL_UNSIGNED_SHORT, indices);
            } else {
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ui32IndexVbo);
                glDrawElements(drawType, 3, GL_UNSIGNED_SHORT, 0);
            }
        } else {
            glDrawArrays(drawType, 0, 3);
        }

                GLenum err = glGetError();
                if(err != GL_NO_ERROR)
            printf(" error %d has occured while drawing\n",err);


        glPopMatrix();
                eglSwapBuffers(d,egl_surface);

                if(useTexture && useCopy)
                    glCopyTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,0,0,256,256,0);
                else if(useTexture && useSubCopy)
                    glCopyTexSubImage2D(GL_TEXTURE_2D,0,100,100,WINDOW_WIDTH/2,WINDOW_HEIGHT/2,50,50);
    }
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


