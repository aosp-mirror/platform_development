# VirtualDeviceManager Demo Apps

#### Page contents

[Overview](#overview) \
[Prerequisites](#prerequisites) \
[Build & Install](#build-and-install) \
[Run](#run) \
[Host Settings](#host-settings) \
[Client Settings](#client-settings) \
[Demos](#demos)

## Overview

The VDM Demo Apps allow for showcasing VDM features, rapid prototyping and
testing of new features.

The VDM Demo contains 3 apps:

*   **Host**: installed on the host device, creates and manages a virtual
    device, which represents the client device and communicates with the
    physical client device by sending audio and frames of the virtual displays,
    receiving input and sensor data that is injected into the framework. It can
    launch apps on the virtual device, which are streamed to the client.

*   **Client**: installed on the client device. It receives the audio and frames
    from the host device, which it renders, and sends back input and sensor
    data. For best experience with app streaming on multiple displays at the
    same time, it's best to use a large screen device as a client, like a Pixel
    Tablet.

*   **Demos**: installed on the host, meant to showcase specific VDM features.
    The demos can be also run natively on the host to illustrate better the
    difference in the behavior when they are streamed to a virtual device.

## Prerequisites

*   An Android device running Android 13 or newer to act as a client device.

*   A *rooted* Android device running Android 14 or newer (e.g. a `userdebug` or
    `eng` build) to act as a host device.

*   Both devices need to support
    [Wi-Fi Aware](https://developer.android.com/develop/connectivity/wifi/wifi-aware)

<!-- TODO(b/314429442): Make the host app work on older Android versions. -->

Note: This example app uses an Android device as a client, but there's no such
general requirement. The client device, its capabilities, the connectivity layer
and the communication protocol are entirely up to the virtual device owner.

## Build and Install

### Using the script

Simply connect your devices, navigate to the root of your Android checkout and
run

```
./development/samples/VirtualDeviceManager/setup.sh
```

The interactive script will prompt you which apps to install to which of the
available devices, build the APKs and install them.

### Manually

1.  Source `build/envsetup.sh` and run `lunch` or set
    `UNBUNDLED_BUILD_SDKS_FROM_SOURCE=true` if there's no local build because
    the APKs need to be built against a locally built SDK.

1.  Build the Host app.

    ```
    m -j VdmHost
    ```

1.  Install the application as a system app on the host device.
    <!-- TODO(b/314436863): Add a bash script for easy host app install. -->

    ```
    adb root && adb disable-verity && adb reboot  # one time
    adb root && adb remount
    adb push $ANDROID_BUILD_TOP/development/samples/VirtualDeviceManager/host/com.example.android.vdmdemo.host.xml /system/etc/permissions/com.example.android.vdmdemo.host.xml
    adb shell mkdir /system/priv-app/VdmDemoHost
    adb push $OUT/system/priv-app/VdmHost/VdmHost.apk /system/priv-app/VdmDemoHost/
    adb reboot
    ```

    **Tip:** Subsequent installs without changes to permissions, etc. do not
    need all the commands above - you can just do \
    `adb install -r -d -g $OUT/system/priv-app/VdmHost/VdmHost.apk`

1.  Build and install the Demo app on the host device.

    ```
    m -j VdmDemos && adb install -r -d -g $OUT/system/app/VdmDemos/VdmDemos.apk
    ```

1.  Build and install the Client app on the client device.

    ```
    m -j VdmClient && adb install -r -d -g $OUT/system/app/VdmClient/VdmClient.apk
    ```

## Run

1.  Start both the Client and the Host apps on each respective device.

1.  They should find each other and connect automatically. On the first launch
    the Host app will ask to create a CDM association: allow it.

    WARNING: If there are other devices in the vicinity with one of these apps
    running, they might interfere.

1.  Once the connection switches to high bandwidth medium, the Host app will
    show a launcher-like list of installed apps on the host device.

1.  Clicking an app icon will create a new virtual display, launch the app there
    and start streaming the display contents to the client. The client will show
    the surface of that display and render its contents.

1.  Long pressing on an app icon will open a dialog to select an existing
    display to launch the app on instead of creating a new one.

1.  Each display on the Client app has a "Back" and "Close" buttons. When a
    display becomes empty, it's automatically removed.

1.  Each display on the Client app has a "Rotate" button to switch between
    portrait and landscape orientation. This simulates the physical rotation of
    the display of the streamed activity. The "Resize" button can be used to
    change the display dimensions.

1.  The Host app has a "CREATE HOME DISPLAY" button, clicking it will create a
    new virtual display, launch the secondary home activity there and start
    streaming the display contents to the client. The display on the Client app
    will have a home button, clicking it will navigate the streaming experience
    back to the home activity.

1.  The Host app has a "CREATE MIRROR DISPLAY" button, clicking it will create a
    new virtual display, mirror the default host display there and start
    streaming the display contents to the client.

1.  Check out the different [Host Settings](#host-settings) and
    [Client Settings](#client-settings) that allow for changing the behavior of
    the streamed apps and the virtual device in general.

1.  Check out the [Demo apps](#demos) that are specifically meant to showcase
    the VDM features.

<!-- LINT.IfChange(host_settings) -->

## Host Settings

-   **Client Sensors**: Enables sensor injection from the client device into the
    host device. Any context that is associated with the virtual device will
    access the virtual sensors by default. \
    *Changing this will recreate the virtual device.*

-   **Client Audio**: Enables audio output on the client device. Any context
    that is associated with the virtual device will play audio on the client by
    default. \
    *This can be changed dynamically.*

-   **Include streamed apps in recents**: Whether streamed apps should show up
    in the host device's recent apps. Run the command below to enable this
    functionality. \
    *This can be changed dynamically.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.dynamic_policy true
    ```

-   **Cross-device clipboard**: Whether to share the clipboard between the host
    and the virtual device. If disabled, both devices will have their own
    isolated clipboards. Run the command below to enable this functionality. \
    *This can be changed dynamically.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.cross_device_clipboard true
    ```

-   **Display rotation**: Whether orientation change requests from streamed apps
    should trigger orientation change of the relevant display. The client will
    automatically rotate the relevant display upon such request. Disabling this
    simulates a fixed orientation display that cannot physically rotate. Then
    any streamed apps on that display will be letterboxed/pillarboxed if they
    request orientation change. \
    *This can be changed dynamically but only applies to newly created
    displays.*

-   **Always unlocked**: Whether the virtual displays should remain unlocked and
    interactive when the host device is locked. Disabling this will result in a
    simple lock screen shown on these displays when the host device is locked. \
    *Changing this will recreate the virtual device.*

-   **Device streaming profile**: Enables device streaming CDM role as opposed
    to app streaming role, with all differences in policies that this entails. \
    *Changing this will recreate the virtual device.*

-   **Record encoder output**: Enables recording the output of the encoder on
    the host device to a local file on the device. This can be helpful with
    debugging Encoding related issues. To download and play the file locally:

    ```shell
    adb pull /sdcard/Download/vdmdemo_encoder_output_<displayId>.h264
    ffplay -f h264 vdmdemo_encoder_output_<displayId>.h264
    ```

-   **Show pointer icon**: Whether pointer icon should be shown for virtual
    input pointer devices. \
    *This can be changed dynamically.*

-   **Immersive mode**: Makes the streamed activities fullscreen on the client
    device. The client's back button/gesture results in sending back to the
    streamed app and Volume Up acts as a home key, if the streamed display is a
    home display. Use Volume Down on the client device to exit the activity. \
    *Changing this will recreate the virtual device.*

-   **Custom home**: Whether to use a custom activity as home on home displays,
    or use the device-default secondary home activity. Run the command below to
    enable this functionality. \
    *Changing this will recreate the virtual device.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.vdm_custom_home true
    ```

<!-- LINT.ThenChange() -->
<!-- LINT.IfChange(client_settings) -->

## Client Settings

### Input

The client has settings that enable more ways of interacting with the streamed
apps. Each of the following are able to inject input events into the focused
display. The focused display is indicated by the frame around it whenever at
least one of these settings is enabled. The display focus is based on
interaction.

-   **Show dpad**, **Show navigation touchpad**: Render a dpad / touchpad for
    navigating the activity on the focused display.

-   **Enable external keyboard**, **Enable external mouse**: Forward any events
    from a keyboard or mouse, which is externally connected to the client device
    to the activity on the focused display.

<!-- LINT.ThenChange() -->
<!-- LINT.IfChange(demos) -->

## Demos

-   **Sensors**: A simple activity balancing a beam on the screen based on the
    accelerometer events, which allows for selecting which device's sensor to
    use. By default, will use the sensors of the device it's shown on.

-   **Rotation**: A simple activity that is in landscape by default and can send
    orientation change requests on demand. Showcases the display rotation on the
    client, which will rotate the user-visible surface.

-   **Home**: A simple activity with utilities around launching HOME and
    SECONDARY_HOME Intents.

-   **Secure Window**: A simple activity that declares the Window as secure.
    This showcases the FLAG_SECURE streaming policies in VDM.

-   **Permissions**: A simple activity with buttons to request and revoke
    runtime permissions. This can help test the permission streaming and
    device-aware permission features.

-   **Latency**: Renders a simple counter view that renders a new frame with an
    incremented counter every second. Can be useful for debugging latency,
    encoder, decoder issues in the demo application.

-   **Vibration**: A simple activity making vibration requests via different
    APIs and allows for selecting which device's vibrator to use. By default,
    will use the vibrator of the device it's shown on. Note that currently there
    is no vibration support on virtual devices, so vibration requests from
    streamed activities are ignored.

<!-- LINT.ThenChange() -->
