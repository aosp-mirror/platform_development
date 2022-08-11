setup:
1. Set the KEYPHRASE constant in SampleVoiceInteractionService.java to something the device's
   default assistant supports.
2. m -j SampleVoiceInteractor
3. adb pull ./system/etc/permissions/privapp-permissions-platform.xml
4. Add: 
    <privapp-permissions package="com.example.android.voiceinteractor">
        <permission name="android.permission.CAPTURE_AUDIO_HOTWORD"/>
    </privapp-permissions>
5. adb remount
6. adb push privapp-permissions-platform.xml /system/etc/permissions/privapp-permissions-platform.xml
7. adb shell mkdir /system/priv-app/SampleVoiceInteractor
8. adb push out/target/product/$TARGET_PRODUCT/system/priv-app/SampleVoiceInteractor/SampleVoiceInteractor.apk /system/priv-app/SampleVoiceInteractor/
9. adb reboot
10. Go to the sample app info/settings.
11. Tap on Permissions and grant Mic access.
12. Reboot.
13. Set the sample app as the assistant.
14. Check for this in the logs to make sure it worked:
     com.example.android.voiceinteractor I/VIS: onAvailabilityChanged: 2
15. If it didn't, check if the pregrant worked:
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
   com.example.android.voiceinteractor|AlwaysOnHotword|SoundTrigger|RecordingActivityMonitor|soundtrigger|AudioPolicyManager|AudioFlinger|AudioPolicyIntefaceImpl|AudioPolicyService