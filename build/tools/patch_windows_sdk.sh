#!/bin/bash

# This script takes a Linux SDK, cleans it and injects the necessary Windows
# binaries needed by the SDK. The script has 2 parts:
# - development/tools/build/path_windows_sdk.sh to process the
#   platform-dependent folders and files.
# - sdk/build/patch_windows_sdk.sh to process folder and files which
#   depend on the sdk.git repo. This file will be invoked by this one.
#
# Input arguments:
# -q = Optional arg to make this silent. Must be given first.
# $1 = Temporary SDK directory, that is the Linux SDK being patched into
#      a Windows one.
# $2 = The out/host/windows directory, which contains the new Windows
#      binaries to use.
# $3 = An optional replacement for $TOPDIR (inherited from the Android
#      build system), which is the top directory where Android is located.

# Verbose by default. Use -q to make more silent.
V="-v"
if [[ "$1" == "-q" ]]; then V=""; shift; fi

TEMP_SDK_DIR=$1
WIN_OUT_DIR=$2
TOPDIR=${TOPDIR:-$3}

# The unix2dos is provided by the APT package "tofrodos". However
# as of ubuntu lucid, the package renamed the command to "todos".
UNIX2DOS=`which unix2dos`
if [[ ! -x $UNIX2DOS ]]; then
  UNIX2DOS=`which todos`
fi

PLATFORMS=( $TEMP_SDK_DIR/platforms/* )
if [[ ${#PLATFORMS[@]} != 1 ]]; then
    echo "Error: Too many platforms found in $TEMP_SDK_DIR"
    echo "Only one was expected."
    echo "Instead, found: ${PLATFORMS[@]}"
    exit 1
fi

# Package USB Driver
if [[ -n "$USB_DRIVER_HOOK" ]]; then
    $USB_DRIVER_HOOK $V $TEMP_SDK_DIR $TOPDIR
fi

# Remove obsolete stuff from tools & platform
TOOLS=$TEMP_SDK_DIR/tools
PLATFORM_TOOLS=$TEMP_SDK_DIR/platform-tools
LIB=$TEMP_SDK_DIR/tools/lib
rm $V $TOOLS/{android,apkbuilder,ddms,dmtracedump,draw9patch,emulator,etc1tool}
rm $V $TOOLS/{hierarchyviewer,hprof-conv,layoutopt,mksdcard,sqlite3,traceview,zipalign}
rm $V $LIB/*/swt.jar
rm $V $PLATFORM_TOOLS/{adb,aapt,aidl,dx,dexdump}

# Copy all the new stuff in tools
# Note: some tools are first copied here and then moved in platforms/<name>/tools/
cp $V $WIN_OUT_DIR/host/windows-x86/bin/*.{exe,dll} $TOOLS/
mkdir -pv $LIB/x86
cp $V ${TOPDIR}prebuilt/windows/swt/swt.jar         $LIB/x86/
mkdir -pv $LIB/x86_64
cp $V ${TOPDIR}prebuilt/windows-x86_64/swt/swt.jar  $LIB/x86_64/

# Copy the SDK Manager (aka sdklauncher) to the root of the SDK (it was copied in tools above)
# and move it also in SDK/tools/lib (so that tools updates can update the root one too)
cp $TOOLS/sdklauncher.exe $TEMP_SDK_DIR/"SDK Manager.exe"
mv $TOOLS/sdklauncher.exe $LIB/"SDK Manager.exe"

# Copy the emulator NOTICE in the tools dir
cp $V ${TOPDIR}external/qemu/NOTICE $TOOLS/emulator_NOTICE.txt

# aapt under cygwin needs to have mgwz.dll
[[ -n $NEED_MGWZ ]] && cp $V $CYG_MGWZ_PATH $TOOLS/

# Update a bunch of bat files
cp $V ${TOPDIR}sdk/files/post_tools_install.bat                 $LIB/
cp $V ${TOPDIR}sdk/files/find_java.bat                          $LIB/
cp $V ${TOPDIR}sdk/apkbuilder/etc/apkbuilder.bat                $TOOLS/
cp $V ${TOPDIR}sdk/ddms/app/etc/ddms.bat                        $TOOLS/
cp $V ${TOPDIR}sdk/traceview/etc/traceview.bat                  $TOOLS/
if [ -f ${TOPDIR}sdk/hierarchyviewer2/app/etc/hierarchyviewer.bat ]; then
  cp $V ${TOPDIR}sdk/hierarchyviewer2/app/etc/hierarchyviewer.bat $TOOLS/
else
  # That's ok because currently GB uses Tools_r7 but we'll ship Tools_r8 from master-open.
  echo "WARNING: Ignoring ${TOPDIR}sdk/hierarchyviewer2/app/etc/hierarchyviewer.bat [ok for GB+Tools r8]"
fi
cp $V ${TOPDIR}sdk/layoutopt/app/etc/layoutopt.bat              $TOOLS/
cp $V ${TOPDIR}sdk/draw9patch/etc/draw9patch.bat                $TOOLS/
cp $V ${TOPDIR}sdk/sdkmanager/app/etc/android.bat               $TOOLS/

# Put the JetCreator tools, content and docs (not available in the linux SDK)
JET=$TOOLS/Jet
JETCREATOR=$JET/JetCreator
JETDEMOCONTENT=$JET/demo_content
JETLOGICTEMPLATES=$JET/logic_templates
JETDOC=$TEMP_SDK_DIR/docs/JetCreator

# need to rm these folders since a Mac SDK will have them and it might create a conflict
rm -rf $V $JET
rm -rf $V $JETDOC

# now create fresh folders for JetCreator
mkdir $V $JET
mkdir $V $JETDOC

cp -r $V ${TOPDIR}external/sonivox/jet_tools/JetCreator         $JETCREATOR/
cp -r $V ${TOPDIR}external/sonivox/jet_tools/JetCreator_content $JETDEMOCONTENT/
cp -r $V ${TOPDIR}external/sonivox/jet_tools/logic_templates    $JETLOGICTEMPLATES/
chmod $V -R u+w $JETCREATOR  # fixes an issue where Cygwin might copy the above as u+rx only
cp $V ${TOPDIR}prebuilt/windows/jetcreator/EASDLL.dll           $JETCREATOR/

cp    $V ${TOPDIR}external/sonivox/docs/JET_Authoring_Guidelines.html  $JETDOC/
cp -r $V ${TOPDIR}external/sonivox/docs/JET_Authoring_Guidelines_files $JETDOC/
cp    $V ${TOPDIR}external/sonivox/docs/JET_Creator_User_Manual.html   $JETDOC/
cp -r $V ${TOPDIR}external/sonivox/docs/JET_Creator_User_Manual_files  $JETDOC/

# Copy or move platform specific tools to the default platform.
cp $V ${TOPDIR}dalvik/dx/etc/dx.bat $PLATFORM_TOOLS/
mv $V $TOOLS/{adb.exe,aapt.exe,aidl.exe,dexdump.exe} $TOOLS/Adb*.dll $PLATFORM_TOOLS/

# When building under cygwin, mgwz.dll must be both in SDK/tools for zipalign
# and in SDK/platform/XYZ/tools/ for aapt
[[ -n $NEED_MGWZ ]] && cp $V $TOOLS/mgwz.dll $PLATFORM_TOOLS/

# Fix EOL chars to make window users happy - fix all files at the top level
# as well as all batch files including those in platforms/<name>/tools/
if [[ -x $UNIX2DOS ]]; then
  find $TEMP_SDK_DIR -maxdepth 1 -name "*.[ht]*" -type f -print0 | xargs -0 $UNIX2DOS
  find $TEMP_SDK_DIR -maxdepth 3 -name "*.bat"   -type f -print0 | xargs -0 $UNIX2DOS
fi

# Just to make it easier on the build servers, we want fastboot and adb (and its DLLs)
# next to the new SDK, so up one dir.
for i in fastboot.exe adb.exe AdbWinApi.dll AdbWinUsbApi.dll; do
    cp -f $V $WIN_OUT_DIR/host/windows-x86/bin/$i $TEMP_SDK_DIR/../$i
done

