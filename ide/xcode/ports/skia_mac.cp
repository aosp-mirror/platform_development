#if defined(SK_BUILD_FOR_MAC) && !defined(SK_USE_WXWIDGETS)
#include <Carbon/Carbon.h>
#include <unistd.h>
#include <cerrno>
#include "SkApplication.h"
#include "SkTypes.h"

extern void get_preferred_size(int*, int*, int*, int* );

int main(int argc, char* argv[])
{
    
#if 0
{
	FILE* f = ::fopen("/whereami.txt", "w");
	for (int i = 0; i < argc; i++)
		fprintf(f, "[%d] %s\n", i, argv[i]);
	::fclose(f);
}
#else
// argv[0] is set to the execution path of the application, e.g. 
// /Users/caryclark/android/device/build/ide/xcode/animatorTest/build/Debug/animatorTest.app/Contents/MacOS/animatorTest
// the desired directory path is :
// /Users/caryclark/android/device/jsapps
// the variable (client-specific) part is :
// /Users/caryclark/android/
// since different applications share this library, they only have in common:
// {client}/device/build/ide/xcode/{application}
{
	const char* applicationPath = argv[0];
	const char* common = strstr(applicationPath, "build/ide/xcode/");
	const char systemParent[] = "apps/"; 
	if (common != 0) {
		size_t prefixLength = common - applicationPath;
		char* workingDirectory = new char[prefixLength + sizeof(systemParent)];
		strncpy(workingDirectory, applicationPath, prefixLength);
		strcpy(&workingDirectory[prefixLength], systemParent);
		int error = chdir(workingDirectory);
		if (error != 0) {
			error = errno;
			SkASSERT(error != ENOENT);
			SkASSERT(error != ENOTDIR);
			SkASSERT(error != EACCES);
			SkASSERT(error != EIO);
			SkASSERT(0);
		}
		delete workingDirectory;
	}
}
#endif
	IBNibRef 		nibRef;
    WindowRef 		window;
    
    OSStatus		err;

    // Create a Nib reference passing the name of the nib file (without the .nib extension)
    // CreateNibReference only searches into the application bundle.
    err = CreateNibReference(CFSTR("main"), &nibRef);
    require_noerr( err, CantGetNibRef );
    
    // Once the nib reference is created, set the menu bar. "MainMenu" is the name of the menu bar
    // object. This name is set in InterfaceBuilder when the nib is created.
    err = SetMenuBarFromNib(nibRef, CFSTR("MenuBar"));
    require_noerr( err, CantSetMenuBar );
    
    // Then create a window. "MainWindow" is the name of the window object. This name is set in 
    // InterfaceBuilder when the nib is created.
    err = CreateWindowFromNib(nibRef, CFSTR("MainWindow"), &window);
    require_noerr( err, CantCreateWindow );

    // We don't need the nib reference anymore.
    DisposeNibReference(nibRef);
    {
	// if we get here, we can start our normal Skia sequence
	application_init();
	(void)create_sk_window(window);
        int x =0, y =0, width =640, height=480;
        get_preferred_size(&x, &y, &width, &height);
        MoveWindow(window, x, y, false);
        SizeWindow(window, width, height, false);
    }
    // The window was created hidden so show it.
    ShowWindow( window );

    // Call the event loop
    RunApplicationEventLoop();
	
	application_term();

CantCreateWindow:
CantSetMenuBar:
CantGetNibRef:
	return err;
}

#endif