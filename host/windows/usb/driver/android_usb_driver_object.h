/*
 * Copyright (C) 2006 The Android Open Source Project
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

#ifndef ANDROID_USB_DRIVER_OBJECT_H__
#define ANDROID_USB_DRIVER_OBJECT_H__
/** \file
  This file consists of declaration of class AndroidUsbDriverObject that
  encapsulates our driver object.
*/

/// Globally accessible pointer to the driver object
extern class AndroidUsbDriverObject* global_driver_object;

/** AndroidUsbDriverObject class encapsulates driver object and provides
  overall initialization / cleanup as well as management of globally used
  resources. We use KMDF framework for this driver because it takes care of
  most of the USB related "things" (like PnP, power management and other
  stuff) so we can concentrate more on real functionality. This driver is
  based on KMDF's usbsamp driver sample available at DDK's src\kmdf\usbsamp
  directory. Instance of this class (always one) must be allocated from
  NonPagedPool.
*/
class AndroidUsbDriverObject {

 public:
  /** \brief Driver initialization entry point.

    This method is a "gate" to our driver class from main DriverEntry routine.
    Since this method is called from within DriverEntry only it is placed in
    "INIT" code segment.
    This method is called at IRQL PASSIVE_LEVEL.
    @param drv_object[in] Driver object passed to DriverEntry routine
    @param reg_path[in] Path to the driver's Registry passed to DriverEntry
          routine
    @returns STATUS_SUCCESS on success or an appropriate error code.
  */
  static NTSTATUS DriverEntry(PDRIVER_OBJECT drv_object,
                              PUNICODE_STRING reg_path);

 private:
  /** \brief Constructs driver object.

    Constructor for driver class must be as light as possible. All
    initialization that may fail must be deferred to OnDriverEntry method.
    Since this method is called from within DriverEntry only it is placed in
    "INIT" code segment.
    This method is called at IRQL PASSIVE_LEVEL.
    @param drv_object[in] Driver object passed to DriverEntry routine
    @param reg_path[in] Path to the driver's Registry passed to DriverEntry
          routine
  */
  AndroidUsbDriverObject(PDRIVER_OBJECT drv_object, PUNICODE_STRING reg_path);

  /** \brief Destructs driver object.

    Destructor for driver class must be as light as possible. All
    uninitialization must be done in OnDriverUnload method.
    This method must be called at PASSIVE IRQL.
  */
   ~AndroidUsbDriverObject();

  /** \brief Initializes instance of the driver object.

    This method is called immediatelly after driver object has been
    instantiated to perform actual initialization of the driver. Since this
    method is called from within DriverEntry only it is placed in
    "INIT" code segment.
    This method is called at IRQL PASSIVE_LEVEL.
    @param drv_object[in] Driver object passed to DriverEntry routine
    @param reg_path[in] Path to the driver's Registry passed to DriverEntry
          routine
    @returns STATUS_SUCCESS on success or an appropriate error code.
  */
  NTSTATUS OnDriverEntry(PDRIVER_OBJECT drv_object, PUNICODE_STRING reg_path);

  /** \brief Actual handler for KMDF's AddDevice event

    This method is called by the framework in response to AddDevice call from
    the PnP manager. We create and initialize a device object to represent a
    new instance of the device.
    This method is called at IRQL PASSIVE_LEVEL.
    @param device_init[in] A pointer to a framework-allocated WDFDEVICE_INIT
           structure.
    @return If the routine succeeds, it returns STATUS_SUCCESS. Otherwise,
            it returns one of the error status values defined in ntstatus.h.
  */
  NTSTATUS OnAddDevice(PWDFDEVICE_INIT device_init);

  /** \brief Actual driver unload event handler.

    This method is called when driver is being unloaded.
    This method is called at IRQL PASSIVE_LEVEL.
  */
  void OnDriverUnload();

  /** \brief KMDF's DeviceAdd event entry point

    This callback is called by the framework in response to AddDevice call from
    the PnP manager. We create and initialize a device object to represent a
    new instance of the device. All the software resources should be allocated
    in this callback.
    This method is called at IRQL PASSIVE_LEVEL.
    @param wdf_drv[in] WDF driver handle.
    @param device_init[in] A pointer to a framework-allocated WDFDEVICE_INIT
           structure.
    @return If the routine succeeds, it returns STATUS_SUCCESS. Otherwise,
            it returns one of the error status values defined in ntstatus.h.
  */
  static NTSTATUS EvtDeviceAddEntry(WDFDRIVER wdf_drv,
                                    PWDFDEVICE_INIT device_init);

  /** \brief Driver unload event entry point.

    Framework calls this callback when driver is being unloaded.
    This method is called at IRQL PASSIVE_LEVEL.
  */
  static VOID EvtDriverUnloadEntry(WDFDRIVER wdf_drv);

 public:

  /// Gets this driver's DRIVER_OBJECT
  __forceinline PDRIVER_OBJECT driver_object() const {
    return driver_object_;
  }

  /// Gets KMDF driver handle
  __forceinline WDFDRIVER wdf_driver() const {
    return wdf_driver_;
  }

 private:
  /// This driver's driver object
  PDRIVER_OBJECT    driver_object_;

  /// KMDF driver handle
  WDFDRIVER         wdf_driver_;
};

#endif  // ANDROID_USB_DRIVER_OBJECT_H__
