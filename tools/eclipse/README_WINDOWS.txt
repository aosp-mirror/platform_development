[RM 20080623]

1- To build the Eclipse plugin:
Under Linux:
$ cd your-device-directory
$ tools/eclipse/scripts/build_server.sh destination-directory

This will create an "android-eclipse.zip" in the selected destination directory.
Then in Eclipse, you can use Help > Software Updates > Find and Install > Search for new Features > Next > New Archived Site > select the new android-eclipse.zip. Then with the new archive checked, click Finish/Next.


2- To build a Windows SDK, you need two steps:
a- First you need to create a Linux SDK:

Under Linux:
$ cd your-device-directory
$ make sdk
Note: if you get an error when building the javadoc, make sure you use a Java SDK 1.5
Note: if you get an error when building layoutlib, make sure you use a Java SDK 1.5.0-b13.

b- Once you have a Linux SDK, you can create a Windows SDK:

You need a Windows machine with XP or Vista and Cygwin.
- Installer at http://sources.redhat.com/cygwin/
- Set Default Text File Type to DOS/text, not Unix/binary.
- Select packages autoconf, gcc, g++, bison, python, zip, unzip, mingw-zlib
- Suggested extra packages: emacs, wget, openssh, rsync

Then under Cygwin:
$ cd your-device-directory
$ tools/buildbot/_make_windows_sdk.sh path-to-the-linux-sdk.zip destination-directory

