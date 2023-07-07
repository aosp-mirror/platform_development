setup:

(If a log error "VisualQueryDetector is only available if multiple detectors are allowed" , set target_sdk_version: "10000" in Android.bp for now.)
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
* Set DEBUG to true in AlwaysOnHotwordDetector
* uncomment LOG_NDEBUG lines at the top in AudioFlinger.cpp, Threads.cpp, Tracks.cpp,
   AudioPolicyInterfaceImpl.cpp, AudioPolicyService.cpp
* Use this logcat filter:
   com.example.android.voiceinteractor|AlwaysOnHotword|SoundTrigger|RecordingActivityMonitor|soundtrigger|AudioPolicyManager|AudioFlinger|AudioPolicyIntefaceImpl|AudioPolicyService|VIS|SHotwordDetectionSrvc|Hotword-AudioUtils

Collecting trace events: \
Trace events are used throughout the test app to measure the time it takes to read the AudioRecord
data in both the VoiceInteractionService and the trusted HotwordDetectionService. This section can
be used as a guide to collect and observe this trace data.

* Trace events:
    * 'VIS.onDetected' and 'HDS.onDetected'
    * 'VIS.createAudioRecord' and 'HDS.createAudioRecord'
    * 'VIS.startRecording' and 'HDS.startRecording'
    * 'AudioUtils.read' and 'AudioRecord.read'
    * 'AudioUtils.bytesRead'
      * Counter trace value increasing as the AudioUtils.read call progresses. This value is reset after each new call.

* How to capture a trace:
  * Follow this guide or a similar one: https://developer.android.com/topic/performance/tracing/on-device
  * Open https://perfetto.dev/#/running.md and upload a trace report
  * Search for the events manually or run the below example SQL query to pull out the events.

* Perfetto trace SQL query
  * How to run a SQL query: https://perfetto.dev/docs/quickstart/trace-analysis
    * Covers both command line and HTML implementations
```
WITH 
    audio_events AS (
        SELECT 
            ts, 
            (dur / 1000000) as dur_ms, 
            name 
        FROM 
            slice 
        WHERE 
            (name LIKE "%AudioUtils.read%"
             OR name LIKE "%AudioRecord.read%"
             OR name LIKE "%onDetected%"
             OR name LIKE "%startRecording%"
             OR name LIKE "%createAudioRecord%")
    ),
    audio_counters AS (
        SELECT ts, name, value
        FROM counter
        INNER JOIN track ON counter.track_id = track.id
        WHERE name LIKE "%AudioUtils.bytesRead%"
    )
SELECT ts, 'event' as type, name, dur_ms as value
FROM audio_events
UNION ALL
SELECT ts, 'counter' as type, name, value
FROM audio_counters
ORDER BY ts
```