android_winusb.inf file contained in this folder must be used to install
WinUsb framework on customers machines. In order to build installation
package that is compliant with android_winusb.inf, you need to create
the following tree:

Root of the installation folder must contain:
  * File android_winusb.inf - Installation file
  * File androidwinusb86.cat - Signed catalog for 32-bit package
  * File androidwinusba64.cat - Signed catalog for AMD 64-bit package
  * Subfolder i386 containing files for 32-bit installation:
    * WdfCoInstaller01007.dll
    * WinUSBCoInstaller.dll
    * WUDFUpdate_01007.dll
  * Subfolder amd64 containing files for AMD 64-bit installation:
    * WdfCoInstaller01007.dll
    * WinUSBCoInstaller.dll
    * WUDFUpdate_01007.dll
    
File contained in i386 and amd64 subfolders are Microsoft distributives needed
to install WinUsb framework. These files can be obtained from WDK 'redist'
folder, respectively to the OS: copy x86 files to i386 subfolder, and amd64
files to amd64 subfolder.

android_winusb.inf file can be modified in order to provide support for the
devices that were not available when Android SDK was shipped. To do that,
modify [Google.NTx86], [Google.NTamd64], and [Strings] sections of .inf
file to add descriptions for new devices and interfaces. Note that when .inf
file is modified, .cat files must be rebuilt and resigned in order to keep
integrity of the installation. Failure to rebuild and resign .cat files will
not break the installation, but it will cause security warnings (that can be
dismissed) to pop up at the installation time.

As an alternative to modification, android_winusb.inf file can be used as a
template to create new .inf file for new devices. Note that you also need
to build and sign new .cat files for that custom .inf file of yours.

The simplest way to create .cat files would be using inf2cat.exe utility,
available in WDK at bin\SelfSign folder. To use this utility you will need to
create an installation folder a sdesribed at the beginning of this document,
and run inf2cat.exe on .inf file at the root of installation folder.
