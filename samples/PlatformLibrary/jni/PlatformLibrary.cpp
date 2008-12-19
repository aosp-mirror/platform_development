/*
 * Copyright (C) 2008 The Android Open Source Project
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

#define LOG_TAG "PlatformLibrary"
#include "utils/Log.h"

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>

#include "jni.h"


// ----------------------------------------------------------------------------

/*
 * Field/method IDs and class object references.
 *
 * You should not need to store the JNIEnv pointer in here.  It is
 * thread-specific and will be passed back in on every call.
 */
static struct {
    jclass      platformLibraryClass;
    jfieldID    jniInt;
    jmethodID   yodel;
} gCachedState;

// ----------------------------------------------------------------------------

/*
 * Helper function to throw an arbitrary exception.
 *
 * Takes the exception class name, a format string, and one optional integer
 * argument (useful for including an error code, perhaps from errno).
 */
static void throwException(JNIEnv* env, const char* ex, const char* fmt,
    int data) {

    if (jclass cls = env->FindClass(ex)) {
        if (fmt != NULL) {
            char msg[1000];
            snprintf(msg, sizeof(msg), fmt, data);
            env->ThrowNew(cls, msg);
        } else {
            env->ThrowNew(cls, NULL);
        }

        /*
         * This is usually not necessary -- local references are released
         * automatically when the native code returns to the VM.  It's
         * required if the code doesn't actually return, e.g. it's sitting
         * in a native event loop.
         */
        env->DeleteLocalRef(cls);
    }
}

/*
 * Trivial sample method.
 *
 * If "bad" is true, this throws an exception.  Otherwise, this sets the
 * "mJniInt" field to 42 and returns 24.
 */
static jint PlatformLibrary_getJniInt(JNIEnv* env, jobject thiz, jboolean bad) {
    if (bad) {
        throwException(env, "java/lang/IllegalStateException",
                "you are bad", 0);
        return 0;       /* return value will be ignored */
    }
    env->SetIntField(thiz, gCachedState.jniInt, 42);
    return (jint)24;
}

/*
 * A more complex sample method.
 *
 * This takes a String as an argument, and returns a new String with
 * characters in reverse order.  The new string is passed to another method.
 * This demonstrates basic String manipulation functions and method
 * invocation.
 *
 * This method is declared "static", so there's no "this" pointer; instead,
 * we get a pointer to the class object.
 */
static jstring PlatformLibrary_reverseString(JNIEnv* env, jclass clazz,
    jstring str) {

    if (str == NULL) {
        throwException(env, "java/lang/NullPointerException", NULL, 0);
        return NULL;
    }

    /*
     * Get a pointer to the string's UTF-16 character data.  The data
     * may be a copy or a pointer to the original.  Since String data
     * is immutable, we're not allowed to touch it.
     */
    const jchar* strChars = env->GetStringChars(str, NULL);
    if (strChars == NULL) {
        /* something went wrong */
        LOGW("Couldn't get string chars\n");
        return NULL;
    }
    jsize strLength = env->GetStringLength(str);

    /*
     * Write a progress message to the log.  Log messages are UTF-8, so
     * we want to convert the string to show it.
     */
    const char* printable = env->GetStringUTFChars(str, NULL);
    if (printable != NULL) {
        LOGD("Reversing string '%s'\n", printable);
        env->ReleaseStringUTFChars(str, printable);
    }

    /*
     * Copy the characters to temporary storage, reversing as we go.
     */
    jchar tempChars[strLength];
    for (int i = 0; i < strLength; i++) {
        tempChars[i] = strChars[strLength -1 -i];
    }

    /*
     * Release the original String.  That way, if something fails later on,
     * we don't have to worry about this leading to a memory leak.
     */
    env->ReleaseStringChars(str, strChars);
    strChars = NULL;            /* this pointer no longer valid */

    /*
     * Create a new String with the chars.
     */
    jstring result = env->NewString(tempChars, strLength);
    if (result == NULL) {
        LOGE("NewString failed\n");
        return NULL;
    }

    /*
     * Now let's do something with it.  We already have the methodID for
     * "yodel", so we can invoke it directly.  It's in our class, so we
     * can use the Class object reference that was passed in.
     */
    env->CallStaticVoidMethod(clazz, gCachedState.yodel, result);

    return result;
}


// ----------------------------------------------------------------------------

/*
 * Array of methods.
 *
 * Each entry has three fields: the name of the method, the method
 * signature, and a pointer to the native implementation.
 */
static const JNINativeMethod gMethods[] = {
    { "getJniInt",          "(Z)I",
                        (void*)PlatformLibrary_getJniInt },
    { "reverseString",      "(Ljava/lang/String;)Ljava/lang/String;",
                        (void*)PlatformLibrary_reverseString },
};

/*
 * Do some (slow-ish) lookups now and save the results.
 *
 * Returns 0 on success.
 */
static int cacheIds(JNIEnv* env, jclass clazz) {
    /*
     * Save the class in case we want to use it later.  Because this is a
     * reference to the Class object, we need to convert it to a JNI global
     * reference.
     */
    gCachedState.platformLibraryClass = (jclass) env->NewGlobalRef(clazz);
    if (clazz == NULL) {
        LOGE("Can't create new global ref\n");
        return -1;
    }

    /*
     * Cache field and method IDs.  IDs are not references, which means we
     * don't need to call NewGlobalRef on them.
     */
    gCachedState.jniInt = env->GetFieldID(clazz, "mJniInt", "I");
    if (gCachedState.jniInt == NULL) {
        LOGE("Can't find PlatformLibrary.mJniInt\n");
        return -1;
    }

    gCachedState.yodel = env->GetStaticMethodID(clazz, "yodel",
        "(Ljava/lang/String;)V");
    if (gCachedState.yodel == NULL) {
        LOGE("Can't find PlatformLibrary.yodel\n");
        return -1;
    }

    return 0;
}

/*
 * Explicitly register all methods for our class.
 *
 * While we're at it, cache some class references and method/field IDs.
 *
 * Returns 0 on success.
 */
static int registerMethods(JNIEnv* env) {
    static const char* const kClassName =
        "com/example/android/platform_library/PlatformLibrary";
    jclass clazz;

    /* look up the class */
    clazz = env->FindClass(kClassName);
    if (clazz == NULL) {
        LOGE("Can't find class %s\n", kClassName);
        return -1;
    }

    /* register all the methods */
    if (env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(gMethods[0])) != JNI_OK)
    {
        LOGE("Failed registering methods for %s\n", kClassName);
        return -1;
    }

    /* fill out the rest of the ID cache */
    return cacheIds(env, clazz);
}

// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (registerMethods(env) != 0) {
        LOGE("ERROR: PlatformLibrary native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}
