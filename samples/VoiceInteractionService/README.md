setup:
1. Set the KEYPHRASE constant in SampleVoiceInteractionService.java to something the device's
   default assistant supports.
2. m -j SampleVoiceInteractor
4. adb root; adb remount
5. adb push development/samples/VoiceInteractionService/com.example.android.voiceinteractor.xml /system/etc/permissions/com.example.android.voiceinteractor.xml
6. adb shell mkdir /system/priv-app/SampleVoiceInteractor
7. adb push out/target/product/$TARGET_PRODUCT/system/priv-app/SampleVoiceInteractor/SampleVoiceInteractor.apk /system/priv-app/SampleVoiceInteractor/
8. adb reboot
9. Go to the sample app info/settings.
10. Tap on Permissions and grant Mic access.
11. Reboot.
12. Set the "Digital assistant app" to "Sample Voice Interactor" in the Android settings
13. Check for this in the logs to make sure it worked:
     com.example.android.voiceinteractor I/VIS: onAvailabilityChanged: 2
14. If it didn't, check if the pregrant worked:
     adb shell dumpsys package com.example.android.voiceinteractor | grep CAPTURE_AUDIO_HOTWORD

Iterating:
* adb install like usual
* If syncing changes to the system image, either first copy the permissions file into
  out/target/product/system/etc/permissions/ or push it again after syncing. Sometimes you might
  need to uninstall the app (go to the sample app info/settings -> 3 dots menu -> uninstall
  updates).

to test:
1. Say "1,2,Ok Poodle,3,4.."
2. Check the logs for the app and wait till it finishes recording.
3. Either check the logs for the sampled bytes to match, e.g. "sample=[95, 2, 97, ...]" should
   appear twice; or open the sample app activity and click the button to play back the recorded
   audio.
Tap directRecord to simulate the non-DSP case (must be done after a dsp trigger since it
    reuses the previous data).

Debugging:
*  Set DEBUG to true in AlwaysOnHotwordDetector
*  uncomment LOG_NDEBUG lines at the top in AudioFlinger.cpp, Threads.cpp, Tracks.cpp,
   AudioPolicyInterfaceImpl.cpp, AudioPolicyService.cpp
*  Use this logcat filter:
   com.example.android.voiceinteractor|AlwaysOnHotword|SoundTrigger|RecordingActivityMonitor|soundtrigger|AudioPolicyManager|AudioFlinger|AudioPolicyIntefaceImpl|AudioPolicyService|VIS|SHotwordDetectionSrvc|Hotword-AudioUtils