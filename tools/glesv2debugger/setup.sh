source ../../../build/envsetup.sh
pushd ../../../

# need lunch before building jars
if [ -z "$TARGET_PRODUCT" ]; then
    lunch
fi

pushd external/liblzf/
mm
popd

pushd external/protobuf/
mm
popd

pushd sdk/sdkmanager/libs/sdklib
mm
popd

# glsl_compiler is optional
# make glsl_compiler -j3

popd

mkdir -p lib
cp "$ANDROID_HOST_OUT/framework/host-libprotobuf-java-2.3.0-lite.jar" lib/
cp "$ANDROID_HOST_OUT/framework/liblzf.jar" lib/
cp "$ANDROID_HOST_OUT/framework/sdklib.jar" lib/

# optional; usually for linux
#cp "$ANDROID_HOST_OUT/bin/glsl_compiler" ~/

# optional; usually for mac, need to replace eclipse.app with actual path
#cp "$ANDROID_HOST_OUT/bin/glsl_compiler" eclipse.app/Contents/MacOS
