/*
* Copyright 2011 The Android Open Source Project
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

	
