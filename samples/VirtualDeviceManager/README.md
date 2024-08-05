# VirtualDeviceManager Demo Apps

#### Page contents

[Overview](#overview) \
[Prerequisites](#prerequisites) \
[Build & Install](#build-and-install) \
[Run](#run) \
[Host Options](#host-options) \
[Client Options](#client-options) \
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
    `eng` build) to act as a host device. Even though VDM is available starting
    Android 13, the support there is minimal and the Host app is not compatible
    with Android 13.

*   Both devices need to support
    [Wi-Fi Aware](https://developer.android.com/develop/connectivity/wifi/wifi-aware)

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

    ```shell
    m -j VdmHost
    ```

1.  Install the application as a system app on the host device.

    ```shell
    adb root && adb disable-verity && adb reboot  # one time
    adb root && adb remount
    adb push $ANDROID_BUILD_TOP/development/samples/VirtualDeviceManager/host/com.example.android.vdmdemo.host.xml /system/etc/permissions/com.example.android.vdmdemo.host.xml
    adb shell mkdir /system/priv-app/VdmDemoHost
    adb push $OUT/system/priv-app/VdmHost/VdmHost.apk /system/priv-app/VdmDemoHost/
    adb reboot
    ```

    **Tip:** Subsequent installs without changes to permissions, etc. do not
    need all the commands above - you can just run \
    \
    `adb install -r -d -g $OUT/system/priv-app/VdmHost/VdmHost.apk`

1.  Build and install the Demo app on the host device.

    ```shell
    m -j VdmDemos && adb install -r -d -g $OUT/system/app/VdmDemos/VdmDemos.apk
    ```

1.  Build and install the Client app on the client device.

    ```shell
    m -j VdmClient && adb install -r -d -g $OUT/system/app/VdmClient/VdmClient.apk
    ```

## Run

1.  Start both the Client and the Host apps on each respective device.

1.  They should find each other and connect automatically. On the first launch
    the Host app will ask to create a CDM association: allow it.

    WARNING: If there are other devices in the vicinity with one of these apps
    running, they might interfere.

1.  Check out the different [Host Options](#host-options) and
    [Client Options](#client-options) that allow for changing the behavior of
    the streamed apps and the virtual device in general.

1.  Check out the [Demo apps](#demos) that are specifically meant to showcase
    the VDM features.

<!-- LINT.IfChange(host_options) -->

## Host Options

NOTE: Any flag changes require device reboot or "Force stop" of the host app
because the flag values are cached and evaluated only when the host app is
starting. Alternatively, run: \
\
`adb shell am force-stop com.example.android.vdmdemo.host`

### Launcher

Once the connection with the client device is established, the Host app will
show a launcher-like list of installed apps on the host device.

-   Clicking an app icon will create a new virtual display, launch the app there
    and start streaming the display contents to the client. The client will show
    the surface of that display and render its contents.

-   Long pressing on an app icon will open a dialog to select an existing
    display to launch the app on instead of creating a new one.

-   The Host app has a **CREATE HOME DISPLAY** button, clicking it will create a
    new virtual display, launch the secondary home activity there and start
    streaming the display contents to the client. The display on the Client app
    will have a home button, clicking it will navigate the streaming experience
    back to the home activity. Run the commands below to enable this
    functionality.

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.vdm_custom_home true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

-   The Host app has a **CREATE MIRROR DISPLAY** button, clicking it will create
    a new virtual display, mirror the default host display there and start
    streaming the display contents to the client. Run the commands below to
    enable this functionality.

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.consistent_display_flags true
    adb shell device_config put virtual_devices android.companion.virtual.flags.interactive_screen_mirror true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

### Settings

#### Input

The input menu button enables several different mechanisms for injecting input
from the host device into the focused display on the client device. The focused
display is indicated by the frame around its header whenever there are more than
one displays. The display focus is based on user interaction.

Each input screen has a "Back", "Home" and "Forward" buttons.

-   **Touchpad** shows an on-screen touchpad for injecting mouse events into the
    focused display.

-   **Remote** allows the host device to act as a pointer that controls the
    mouse movement on the focused display.

-   **Navigation** shows an on-screen D-Pad, rotary and touchpad for navigating
    the activity on the focused display.

-   **Keyboard** shows the host device's on-screen keyboard and sends any key
    events to the activity on the focused display.

-   **Stylus** allows for injecting simulated stylus events into the focused
    display. Use together with the stylus demo. Run the commands below to enable
    this functionality.

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.virtual_stylus true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

#### General

-   **Device profile**: Enables device streaming CDM role as opposed to app
    streaming role, with all differences in policies that this entails. \
    *Changing this will recreate the virtual device.*

-   **Hide streamed app from recents**: Whether streamed apps should show up in
    the host device's recent apps. Run the commands below to make this
    functionality dynamic. \
    *This can be changed dynamically starting with Android V.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.dynamic_policy true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

-   **Enable cross-device clipboard**: Whether to share the clipboard between
    the host and the virtual device. If disabled, both devices will have their
    own isolated clipboards. Run the commands below to enable this
    functionality. \
    *This can be changed dynamically.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.dynamic_policy true
    adb shell device_config put virtual_devices android.companion.virtual.flags.cross_device_clipboard true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

-   **Enable custom activity policy**: Whether to use custom user notification
    for activities that are unable to launch on the virtual display and send
    such activities to the default display, whenever possible. Use together with
    the activity policy demo. The behavior of the display fallback launch is
    different depending on whether an activity result is expected by the caller,
    and on whether the default display keyguard is currently locked. \
    *This can be changed dynamically.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.dynamic_policy true
    adb shell device_config put virtual_devices android.companion.virtualdevice.flags.activity_control_api true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

#### Client capabilities

-   **Enable client Sensors**: Enables sensor injection from the client device
    into the host device. Any context that is associated with the virtual device
    will access the virtual sensors by default. \
    *Changing this will recreate the virtual device.*

-   **Enable client Camera**: Enables front & back camera injection from the
    client device into the host device. (WIP: Any context that is associated
    with the virtual device will the virtual cameras by default). Run the
    commands below on host device to enable this functionality. \
    *Changing this will recreate the virtual device.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.virtual_camera true
    adb shell device_config put virtual_devices android.companion.virtualdevice.flags.virtual_camera_service_discovery true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

-   **Enable client Audio**: Enables audio output on the client device. Any
    context that is associated with the virtual device will play audio on the
    client by default. \
    *This can be changed dynamically.*

#### Displays

-   **Display rotation**: Whether orientation change requests from streamed apps
    should trigger orientation change of the relevant display. The client will
    automatically rotate the relevant display upon such request. Disabling this
    simulates a fixed orientation display that cannot physically rotate. Then
    any streamed apps on that display will be letterboxed/pillarboxed if they
    request orientation change. Run the commands below to enable this
    functionality. \
    *This can be changed dynamically but only applies to newly created
    displays.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.consistent_display_flags true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

-   **Display category**: Whether to specify a custom display category for the
    virtual displays. This means that only activities that have explicitly set
    a matching `android:requiredDisplayCategory` activity attribute can be
    launched on that display. \
    *Changing this will recreate the virtual device.*

-   **Always unlocked**: Whether the virtual displays should remain unlocked and
    interactive when the host device is locked. Disabling this will result in a
    simple lock screen shown on these displays when the host device is locked. \
    *Changing this will recreate the virtual device.*

-   **Show pointer icon**: Whether pointer icon should be shown for virtual
    input pointer devices. \
    *This can be changed dynamically.*

-   **Custom home**: Whether to use a custom activity as home on home displays,
    or use the device-default secondary home activity. Run the commands below to
    enable this functionality. \
    *Changing this will recreate the virtual device.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.vdm_custom_home true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

#### Input method

Note: The virtual keyboard acts like a physically connected keyboard to the host
device. If you want the software keyboard to be shown on the virtual displays,
you likely need to enable this in the host Settings. On a Pixel device: System
-> Language and input -> Physical keyboard.

-   **Display IME policy**: Choose the IME behavior on remote displays. Run the
    commands below to enable this functionality. \
    *This can be changed dynamically.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.vdm_custom_ime true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

-   **Use the native client IME**: Enables the native client IME instead of
    streaming the host's IME on the virtual displays. Requires the *Display IME
    Policy* to be set to *Show IME on the remote display*. Run the commands
    below to enable this functionality. \
    *Changing this will recreate the virtual device.*

    ```shell
    adb shell device_config put virtual_devices android.companion.virtual.flags.vdm_custom_ime true
    adb shell am force-stop com.example.android.vdmdemo.host
    ```

#### Debug

-   **Record encoder output**: Enables recording the output of the encoder on
    the host device to a local file on the device. This can be helpful with
    debugging Encoding related issues. To download and play the file locally:

    ```shell
    adb pull /sdcard/Download/vdmdemo_encoder_output_<displayId>.h264
    ffplay -f h264 vdmdemo_encoder_output_<displayId>.h264
    ```

<!-- LINT.ThenChange(README.md) -->
<!-- LINT.IfChange(client_options) -->

## Client Options

### Streamed displays

-   Each display on the Client app has a "Back" and "Close" buttons. When a
    display becomes empty, it's automatically removed.

-   Each display on the Client app has a "Rotate" button to switch between
    portrait and landscape orientation. This simulates the physical rotation of
    the display of the streamed activity. The "Resize" button can be used to
    change the display dimensions.

-   Each display on the Client app has a "Fullscreen" button which will move the
    contents of that display to an immersive fullscreen activity. The client's
    back button/gestures are sent back to the streamed app. Use Volume Down on
    the client device to exit fullscreen. Volume Up acts as a home key, if the
    streamed display is a home display.

### Input

The input menu button enables **on-screen D-Pad, rotary and touchpad** for
navigating the activity on the focused display. The focused display is indicated
by the frame around its header whenever there are more than one displays. The
display focus is based on user interaction.

In addition, any input events generated from an **externally connected
keyboard** are forwarded to the activity streamed on the focused display.

**Externally connected mouse** events are also forwarded to the relevant
display, if the mouse pointer is currently positioned on a streamed display.

<!-- LINT.ThenChange(README.md) -->
<!-- LINT.IfChange(demos) -->

## Demos

-   **Activity policy**: An activity showcasing blocking of activity launches on
    the virtual device - either because the activity has opted out via
    `android:canDisplayOnRemoteDevices` attribute, or because of the custom
    activity policy of that device.

-   **Sensors**: A simple activity balancing a beam on the screen based on the
    accelerometer events, which allows for selecting which device's sensor to
    use. By default, will use the sensors of the device it's shown on.

-   **Rotation**: A simple activity that is in landscape by default and can send
    orientation change requests on demand. Showcases the display rotation on the
    client, which will rotate the user-visible surface.

-   **Home**: A simple activity with utilities around launching HOME and
    SECONDARY_HOME Intents, as well as other implicit intents.

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

-   **Stylus**: A simple drawing activity that reacts on stylus input events.
    Use together with the simulated stylus input feature of the host app.

The demo activity depends on whether the **Display Category** Host preference is
enabled or not. If enabled, it becomes equivalent to the **Home** demo activity,
which showcases implicit intent handling.

<!-- LINT.ThenChange(README.md) -->

## SDK Version

### Beyond Android 15

-   Added support for per-display activity policies.

-   Added support for custom redirection of blocked activities.

-   Added support for hiding the blocked activity dialog.

-   Added support for virtual display cutout.

-   Added support for virtual display rotation.

-   Added support for virtual rotary input.

### Android 15 / Vanilla Ice Cream / SDK level 35

-   Added support for virtual stylus input.

-   Added support for cross-device clipboard.

-   Added support for custom home activities.

-   Added support for custom IME component.

-   Added support for per-display IME policy.

-   Added support for fixed orientation displays (disable display rotation).

-   Added support for mirroring the default display on the virtual device.

-   Added support for dynamic policy changes, so the device does not need to be
    recreated.

-   Improved support for displays that support home activities. Removed
    navigation bar and added support for normal home intents.

-   Improved handling of vibrating requests originating from virtual devices.

-   Improved multi-display mouse support.

-   Fixed bugs with hiding streamed apps from the host's recent apps.

### Android 14 / Upside Down Cake / SDK level 34

-   Added support for display categories and restricted activities.

-   Added support for virtual sensors.

-   Added device awareness to contexts.

-   Added support for clipboard on the virtual device.

-   Added support for hiding streamed apps from the host's recent apps.

-   Added `COMPANION_DEVICE_NEARBY_DEVICE_STREAMING` device profile.

-   Added support for virtual navigation input: D-Pad and navigation touchpad.

-   Added support for display categories and restricted activities.

-   Improved support for audio, allowing routing to be based on the origin
    context.

-   Improved support for creation of virtual displays and input devices.

-   Improved handling of virtual touch events.

### Android 13 / Tiramisu / SDK level 33

-   Added support for virtual audio device.

-   Added support for hiding the mouse pointer icon.

-   Added support for virtual mouse, keyboard, touchscreen.

-   Added support for always unlocked displays.

-   Added `COMPANION_DEVICE_APP_STREAMING` device profile.

-   Added support for virtual device creation.
