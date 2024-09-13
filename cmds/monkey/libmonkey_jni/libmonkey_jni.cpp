/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "NativeMonkey"

#include <aidl/com/android/commands/monkey/BnMonkey.h>
#include <android-base/logging.h>
#include <android/binder_ibinder_jni.h>
#include <input/VirtualInputDevice.h>

namespace {

constexpr int32_t GOOGLE_VENDOR_ID = 0x18d1;
constexpr int32_t PRODUCT_ID = 0x0001;

android::base::unique_fd openUinputTouchscreen(int width, int height) {
  return openUinput("Monkey touch", GOOGLE_VENDOR_ID, PRODUCT_ID,
                    /*phys=*/"monkeydevice", android::DeviceType::TOUCHSCREEN,
                    height, width);
}
} // namespace

class MonkeyService : public aidl::com::android::commands::monkey::BnMonkey {
public:
  MonkeyService(int width, int height)
      : mTouchScreen(openUinputTouchscreen(width, height)) {}

private:
  ::ndk::ScopedAStatus writeTouchEvent(int32_t pointerId, int32_t toolType,
                                       int32_t action, float x, float y,
                                       float pressure, float majorAxisSize,
                                       int64_t eventTime,
                                       bool *_aidl_return) override {
    *_aidl_return = mTouchScreen.writeTouchEvent(
        pointerId, toolType, action, x, y, pressure, majorAxisSize,
        std::chrono::nanoseconds(eventTime));
    return ndk::ScopedAStatus::ok();
  }

  android::VirtualTouchscreen mTouchScreen;
};

static jobject createNativeService(JNIEnv *env, jclass, jint width,
                                   jint height) {
  std::shared_ptr<MonkeyService> service =
      ndk::SharedRefBase::make<MonkeyService>(width, height);
  // The call `AIBinder_toJavaBinder` increments the refcount, so this will
  // prevent "service" from getting destructed. The ownership will now be
  // transferred to Java.
  return AIBinder_toJavaBinder(env, service->asBinder().get());
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_android_commands_monkey_Monkey_createNativeService(JNIEnv *env,
                                                            jclass clazz,
                                                            jint width,
                                                            jint height) {
  return createNativeService(env, clazz, width, height);
}
