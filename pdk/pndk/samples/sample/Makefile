
NDK_BASE   := ../..

include $(NDK_BASE)/config/config.mk

EXECUTABLE := hello
SOURCES    := hello.c
OBJECTS    := $(SOURCES:.c=.o)
LIBS       := -lc -lm

all: $(EXECUTABLE)

# need $(LINK) before all objects and $(POSTLINK) after all objects for 
# android runtime setup.

hello: $(OBJECTS)
	$(CC) $(LINK) -o $@ $(OBJECTS) $(LIBS) $(POSTLINK)

clean:
	rm -rf *.o hello

