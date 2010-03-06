#!/bin/bash
# Quick semi-auto file to build Windows SDK tools.
#
# Limitations and requirements:
# - Expects the emulator has been built first, will pick it up from prebuilt.
# - Run in Cygwin
# - Expects to have one of the existing SDK (Darwin or Linux) to build the Windows one
# - Needs Cygwin packages: autoconf, bison, curl, flex, gcc, g++, git,
#   gnupg, make, mingw-zlib, python, zip, unzip
# - Must NOT have cygwin package readline (its GPL license might taint the SDK if
#   it gets compiled in)
# - Does not need a Java Development Kit or any other tools outside of cygwin.
# - If you think you may have Windows versions of tools (e.g. make) installed, it may
#   reduce confusion levels to 'export PATH=/usr/bin'

PROG_NAME="$0"
SDK_ZIP="$1"; shift
DIST_DIR="$1"; shift
TEMP_DIR="$1"; shift
[ -z "$TEMP_DIR" ] && TEMP_DIR=${TMP:-/tmp}

set -e  # Fail this script as soon as a command fails -- fail early, fail fast

function die() {
    echo "Error:" $*
    echo "Aborting"
    exit 1
}

function usage() {
    local NAME
    NAME=`basename ${PROG_NAME}`
    echo "Usage: ${NAME} linux_or_mac_sdk.zip output_dir [temp_dir]"
    echo "If temp_dir is not given, \$TMP is used. If that's missing, /tmp is used."
    status
    exit 2
}

function status() {
    echo "Current values:"
    echo "- Input  SDK: ${SDK_ZIP:-missing}"
    echo "- Output dir: ${DIST_DIR:-missing}"
    echo "- Temp   dir: ${TEMP_DIR:-missing}"
}

function check() {
    [ -f "$SDK_ZIP" ] || usage
    [ -d "$DIST_DIR" ] || usage


    # We need mgwz.dll in the SDK when compiling with Cygwin 1.5
    # Right now we don't support building with Cygwin 1.7 yet, as it lacks this DLL.
    NEED_MGWZ=1
    # We can skip this check for debug purposes.
    echo $*
    [[ "$1" == "-no-mgwz" ]] && NEED_MGWZ=""
    CYG_MGWZ_PATH=/cygdrive/c/cygwin/bin/mgwz.dll
    [[ -n $NEED_MGWZ && ! -f $CYG_MGWZ_PATH ]] && \
        die "Cygwin is missing $CYG_MGWZ_PATH. Use -no-mgwz to override."


    # Use the BUILD_ID as SDK_NUMBER if defined, otherwise try to get it from the
    # provided zip filename.
    if [ -f config/build_id.make ]; then
        BUILD_ID=`cat config/build_id.make | sed -n '/BUILD_ID=/s/^[^=]\+=\(.*\)$/\1/p'`
        [ -n "$BUILD_ID" ] && SDK_NUMBER="$BUILD_ID"
    fi
    if [ -z "$SDK_NUMBER" ]; then
        # Look for a pattern like "anything_sdknumber.extension"
        # The pattern is now "any-thing_sdknumber_anything-else.extension"
        #
        # The bottom line is that the SDK number is whatever is enclosed by
        # the LAST couple of underscores. You can have underscores *before* the
        # SDK number if you want, but not after, e.g these are valid:
        #    android_sdk_4242_platform.zip or blah_42_.zip
        #
        # Note that the root directory name in the zip must match the zip
        # name, too, so there's no point just changing the zip name to match
        # the above format.
        #
        # SDK_NUMBER will be empty if nothing matched.
        filename=`basename "$SDK_ZIP"`
        SDK_NUMBER=`echo $filename | sed -n 's/^.*_\([^_./]\+\)_[^_.]*\..*$/\1/p'`
    fi

    [ -n "$SDK_NUMBER" ] || die "Failed to extract the SDK number from $SDK_ZIP. Check its format."

    [ $OSTYPE == "cygwin" ] || die "This expects to run under Cygwin"
    [ -e `which zip` ] || die "Please install 'zip' package in Cygwin"
    [ -f "build/envsetup.sh" ] || die "Please run this from the 'android' directory"

    echo "Using SDK ${SDK_NUMBER}"
}

function build() {

    # IMPORTANT: For Cygwin to be able to build Android targets here,
    # you will generally need to edit build/core/main.mk and add directories
    # where Android.mk makefiles are to be found to the SDK_ONLY==true section.

    echo 
    echo "Building..."
    [ -n "$MAKE_OPT" ] && echo "Make options: $MAKE_OPT"

    . build/envsetup.sh

    # Disable parallel build: it generates "permission denied" issues when
    # multiple "ar.exe" are running in parallel.
    make \
        aapt adb aidl \
        etc1tool \
        prebuilt \
        dexdump dmtracedump \
        fastboot \
        hprof-conv \
        mksdcard \
        sdklauncher sqlite3 \
        zipalign \
        || die "Build failed"

    # Fix permissions. Git/cygwin may not make this +x as needed.
    chmod +x prebuilt/windows/sdl/bin/sdl-config

    # It's worth building the emulator with -j 4 so do it separately
    make -j 4 emulator || die "Build failed"
}

function package() {
    echo
    echo "Packaging..."
    DEST_NAME="android-sdk_${SDK_NUMBER}_windows"
    DEST_NAME_ZIP="${DEST_NAME}.zip"

    TEMP_SDK_DIR="$TEMP_DIR/$DEST_NAME"

    # Unzip current linux/mac SDK and rename using the windows name
    [ -e "$TEMP_SDK_DIR" ] && rm -rfv "$TEMP_SDK_DIR"  # cleanup dest first if exists
    UNZIPPED=`basename "$SDK_ZIP"`
    UNZIPPED="$TEMP_DIR/${UNZIPPED/.zip/}"
    [ -e "$UNZIPPED" ] && rm -rfv "$UNZIPPED"  # cleanup unzip dir (if exists)
    unzip "$SDK_ZIP" -d "$TEMP_DIR"
    mv -v "$UNZIPPED" "$TEMP_SDK_DIR"
    
    # Assert that the package contains only one platform
    PLATFORMS="$TEMP_SDK_DIR/platforms"
    THE_PLATFORM=`echo $PLATFORMS/*`
    PLATFORM_TOOLS=$THE_PLATFORM/tools
    echo "Platform found: " $THE_PLATFORM
    [[ -d "$THE_PLATFORM" ]] || die \
        "Error: One platform was expected in $SDK_ZIP. " \
        "Instead found " $THE_PLATFORM
    [[ -d "$PLATFORM_TOOLS" ]] || die "Missing folder $PLATFORM_TOOLS."

    # Package USB Driver
    if type package_usb_driver 2>&1 | grep -q function ; then
        package_usb_driver $TEMP_SDK_DIR
    fi

    # Remove obsolete stuff from tools & platform
    TOOLS="$TEMP_SDK_DIR/tools"
    LIB="$TEMP_SDK_DIR/tools/lib"
    rm -v "$TOOLS"/{adb,android,apkbuilder,ddms,dmtracedump,draw9patch,emulator,etc1tool}
    rm -v "$TOOLS"/{hierarchyviewer,hprof-conv,layoutopt,mksdcard,sqlite3,traceview,zipalign}
    rm -v "$LIB"/*/swt.jar
    rm -v "$PLATFORM_TOOLS"/{aapt,aidl,dx,dexdump}

    # Copy all the new stuff in tools
    # Note: some tools are first copied here and then moved in platforms/<name>/tools/
    cp -v out/host/windows-x86/bin/*.{exe,dll} "$TOOLS"/
    mkdir -pv "$LIB"/x86
    cp -v prebuilt/windows/swt/swt.jar         "$LIB"/x86/
    mkdir -pv "$LIB"/x86_64
    cp -v prebuilt/windows-x86_64/swt/swt.jar  "$LIB"/x86_64/

    # Copy the SDK Setup (aka sdklauncher) to the root of the SDK (it was copied in tools above)
    # and move it also in SDK/tools/lib (so that tools updates can update the root one too)
    cp "$TOOLS/sdklauncher.exe" "$TEMP_SDK_DIR/SDK Setup.exe"
    mv "$TOOLS/sdklauncher.exe" "$LIB/SDK Setup.exe"

    # If you want the emulator NOTICE in the tools dir, uncomment the following line:
    # cp -v external/qemu/NOTICE "$TOOLS"/emulator_NOTICE.txt

    # We currently need libz from MinGW for aapt
    [[ -n $NEED_MGWZ ]] && cp -v $CYG_MGWZ_PATH "$TOOLS"/

    # Update a bunch of bat files
    cp -v sdk/files/post_tools_install.bat            "$LIB"/
    cp -v sdk/files/find_java.bat                     "$LIB"/
    cp -v sdk/apkbuilder/etc/apkbuilder.bat           "$TOOLS"/
    cp -v sdk/ddms/app/etc/ddms.bat                   "$TOOLS"/
    cp -v sdk/traceview/etc/traceview.bat             "$TOOLS"/
    cp -v sdk/hierarchyviewer/etc/hierarchyviewer.bat "$TOOLS"/
    cp -v sdk/layoutopt/app/etc/layoutopt.bat         "$TOOLS"/
    cp -v sdk/draw9patch/etc/draw9patch.bat           "$TOOLS"/
    cp -v sdk/sdkmanager/app/etc/android.bat          "$TOOLS"/

    # Put the JetCreator tools, content and docs (not available in the linux SDK)
    JET="$TOOLS/Jet"
    JETCREATOR="$JET/JetCreator"
    JETDEMOCONTENT="$JET/demo_content"
    JETLOGICTEMPLATES="$JET/logic_templates"
    JETDOC="$TEMP_SDK_DIR/docs/JetCreator"

    # need to rm these folders since a Mac SDK will have them and it might create a conflict
    rm -rfv "$JET"
    rm -rfv "$JETDOC"

    # now create fresh folders for JetCreator
    mkdir -v "$JET"
    mkdir -v "$JETDOC"

    cp -rv external/sonivox/jet_tools/JetCreator         "$JETCREATOR"/
    cp -rv external/sonivox/jet_tools/JetCreator_content "$JETDEMOCONTENT"/
    cp -rv external/sonivox/jet_tools/logic_templates    "$JETLOGICTEMPLATES"/
    chmod -vR u+w "$JETCREATOR"  # fixes an issue where Cygwin might copy the above as u+rx only
    cp -v prebuilt/windows/jetcreator/EASDLL.dll         "$JETCREATOR"/
    
    cp -v  external/sonivox/docs/JET_Authoring_Guidelines.html  "$JETDOC"/
    cp -rv external/sonivox/docs/JET_Authoring_Guidelines_files "$JETDOC"/
    cp  -v external/sonivox/docs/JET_Creator_User_Manual.html   "$JETDOC"/
    cp -rv external/sonivox/docs/JET_Creator_User_Manual_files  "$JETDOC"/

    # Copy or move platform specific tools to the default platform.
    cp -v dalvik/dx/etc/dx.bat "$PLATFORM_TOOLS"/
    mv -v "$TOOLS"/{aapt.exe,aidl.exe,dexdump.exe} "$PLATFORM_TOOLS"/
    # Note: mgwz.dll must be both in SDK/tools for zipalign and in SDK/platform/XYZ/tools/ for aapt
    [[ -n $NEED_MGWZ ]] && cp -v "$TOOLS"/mgwz.dll "$PLATFORM_TOOLS"/

    # Fix EOL chars to make window users happy - fix all files at the top level only
    # as well as all batch files including those in platforms/<name>/tools/
    find "$TEMP_SDK_DIR" -maxdepth 1 -type f -writable -print0 | xargs -0 unix2dos -D
    find "$TEMP_SDK_DIR" -maxdepth 3 -name "*.bat" -type f -writable -print0 | xargs -0 unix2dos -D

    # Done.. Zip it. Clean the temp folder ONLY if the zip worked (to ease debugging)
    pushd "$TEMP_DIR" > /dev/null
    [ -e "$DEST_NAME_ZIP" ] && rm -rfv "$DEST_NAME_ZIP"
    zip -9r "$DEST_NAME_ZIP" "$DEST_NAME" && rm -rfv "$DEST_NAME"
    popd > /dev/null

    # Now move the final zip from the temp dest to the final dist dir
    mv -v "$TEMP_DIR/$DEST_NAME_ZIP" "$DIST_DIR/$DEST_NAME_ZIP"

    # We want fastboot and adb (and its DLLs) next to the new SDK
    for i in fastboot.exe adb.exe AdbWinApi.dll AdbWinUsbApi.dll; do
        cp -vf out/host/windows-x86/bin/$i "$DIST_DIR"/$i
    done

    echo "Done"
    echo
    echo "Resulting SDK is in $DIST_DIR/$DEST_NAME_ZIP"
}

check $*
status
build
package

echo "Done"
