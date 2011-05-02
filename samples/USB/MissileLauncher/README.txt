MissileLauncher is a simple program that controls Dream Cheeky USB missile launchers.
You control the left/right/up/down orientation of the launcher using the accelerometer.
Tilt the tablet to change the direction of the launcher.
Pressing the "Fire" button will fire one missile.

This program serves as an example of the following USB host features:

- filtering for multiple devices based on vendor and product IDs (see device_filter.xml)

- Sending control requests on endpoint zero that contain data

- Receiving packets on an interrupt endpoint using a thread that calls
  UsbRequest.queue and UsbDeviceConnection.requestWait()
