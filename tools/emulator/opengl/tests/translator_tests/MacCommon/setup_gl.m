#include <stdio.h>
#include <Cocoa/Cocoa.h>

 void * createGLView(void *nsWindowPtr, int x, int y, int width, int height)
{
	NSRect contentRect = NSMakeRect(x, y, width, height);
	NSView *glView = [[NSView alloc] initWithFrame:contentRect];
	if (glView == nil) {
		printf("couldn't create opengl view\n");
		return nil;
	}
	[glView setAutoresizingMask: NSViewWidthSizable | NSViewHeightSizable];
	NSWindow *win = (NSWindow *)nsWindowPtr;
	[[win contentView] addSubview:glView];
	[win makeKeyAndOrderFront:nil];
	return (void *)glView;
}

	
