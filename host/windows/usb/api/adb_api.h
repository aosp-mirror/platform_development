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

#ifndef ANDROID_USB_API_ADBWINAPI_H__
#define ANDROID_USB_API_ADBWINAPI_H__

/** \file
  This file consists of declarations of routines exported by the API as well
  as types, structures, and constants definitions used in the API.
*/

// Enables compillation for "straight" C
#ifdef __cplusplus
  #define EXTERN_C    extern "C"
#else
  #define EXTERN_C    extern
  typedef int bool;
  #define true  1
  #define false 0
#endif

/** \brief Enumerates ADB endpoint types.

  This enum is taken from WDF_USB_PIPE_TYPE enum found in WDK.
*/
typedef enum _AdbEndpointType {
  /// Unknown (invalid, or not initialized) endpoint type.
  AdbEndpointTypeInvalid = 0,

  /// Endpoint is device control pipe.
  AdbEndpointTypeControl,

  /// Endpoint is isochronous r/w pipe.
  AdbEndpointTypeIsochronous,

  /// Endpoint is a bulk r/w pipe.
  AdbEndpointTypeBulk,

  /// Endpoint is an interrupt r/w pipe.
  AdbEndpointTypeInterrupt,
} AdbEndpointType;

/** \brief Endpoint desriptor.

  This structure is based on WDF_USB_PIPE_INFORMATION structure found in WDK.
*/
typedef struct _AdbEndpointInformation {
  /// Maximum packet size this endpoint is capable of.
  unsigned long max_packet_size;

  /// Maximum size of one transfer which should be sent to the host controller.
  unsigned long max_transfer_size;

  /// ADB endpoint type.
  AdbEndpointType endpoint_type;

  /// Raw endpoint address on the device as described by its descriptor.
  unsigned char endpoint_address;

  /// Polling interval.
  unsigned char polling_interval;

  /// Which alternate setting this structure is relevant for.
  unsigned char setting_index;
} AdbEndpointInformation;

/// Shortcut to default write bulk endpoint in zero-based endpoint index API.
#define ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX  0xFC

/// Shortcut to default read bulk endpoint in zero-based endpoint index API.
#define ADB_QUERY_BULK_READ_ENDPOINT_INDEX  0xFE

// {F72FE0D4-CBCB-407d-8814-9ED673D0DD6B}
/// Our USB class id that driver uses to register our device.
#define ANDROID_USB_CLASS_ID \
{0xf72fe0d4, 0xcbcb, 0x407d, {0x88, 0x14, 0x9e, 0xd6, 0x73, 0xd0, 0xdd, 0x6b}};

/// Defines vendor ID for HCT devices.
#define DEVICE_VENDOR_ID                  0x0BB4

/// Defines product ID for the device with single interface.
#define DEVICE_SINGLE_PRODUCT_ID          0x0C01

/// Defines product ID for the Dream composite device.
#define DEVICE_COMPOSITE_PRODUCT_ID       0x0C02

/// Defines product ID for the Magic composite device.
#define DEVICE_MAGIC_COMPOSITE_PRODUCT_ID 0x0C03

/// Defines interface ID for the device.
#define DEVICE_INTERFACE_ID               0x01

/// Defines vendor ID for the device
#define DEVICE_EMULATOR_VENDOR_ID         0x18D1

/// Defines product ID for a SoftUSB device simulator that is used to test
/// the driver in isolation from hardware.
#define DEVICE_EMULATOR_PROD_ID           0xDDDD

// The following ifdef block is the standard way of creating macros which make
// exporting  from a DLL simpler. All files within this DLL are compiled with
// the ADBWIN_EXPORTS symbol defined on the command line. this symbol should
// not be defined on any project that uses this DLL. This way any other project
// whose source files include this file see ADBWIN_API functions as being
// imported from a DLL, whereas this DLL sees symbols defined with this macro
// as being exported.
#ifdef ADBWIN_EXPORTS
#define ADBWIN_API EXTERN_C __declspec(dllexport)
#define ADBWIN_API_CLASS     __declspec(dllexport)
#else
#define ADBWIN_API EXTERN_C __declspec(dllimport)
#define ADBWIN_API_CLASS     __declspec(dllimport)
#endif

/** \brief Handle to an API object.

  To access USB interface and its components clients must first obtain a
  handle to the required object. API Objects that are represented by a
  handle are:
  1. Interface enumerator that provides access to a list of interfaces that
     match certain criterias that were specified when interface enumerator
     has been created. This handle is created in AdbEnumInterfaces routine.
  2. Interface that is the major object this API deals with. In Windows
     model of the USB stack each USB device (that is physical device,
     attached to a USB port) exposes one or more interfaces that become the
     major entities through which that device gets accessed. Each of these
     interfaces are represented as Windows Device Objects on the USB stack.
     So, to this extent, at least as this API is concerned, terms "interface"
     and "device" are interchangeable, since each interface is represented by
     a device object on the Windows USB stack. This handle is created in
     either AdbCreateInterface or AdbCreateInterfaceByName routines.
  3. Endpoint object (also called a pipe) represents an endpoint on interface
     through which all I/O operations are performed. This handle is created in
     one of these routines: AdbOpenEndpoint, AdbOpenDefaultBulkReadEndpoint,
     or AdbOpenDefaultBulkWriteEndpoint.
  4. I/O completion object that tracks completion information of asynchronous
     I/O performed on an endpoint. When an endpoint object gets opened through
     this API it is opened for asynchronous (or overlapped) I/O. And each time
     an asynchronous I/O is performed by this API an I/O completion object is
     created to track the result of that I/O when it gets completed. Clients
     of the API can then use a handle to I/O completion object to query for
     the status and result of asynchronous I/O as well as wait for this I/O
     completion. This handle is created in one of these routines:
     AdbReadEndpointAsync, or AdbWriteEndpointAsync.
  After object is no longer needed by the client, its handle must be closed
  using AdbCloseHandle routine.
*/
typedef void* ADBAPIHANDLE;

/** \brief Defines access type with which an I/O object (endpoint)
  should be opened.
*/
typedef enum _AdbOpenAccessType {
  /// Opens for read and write access.
  AdbOpenAccessTypeReadWrite,

  /// Opens for read only access.
  AdbOpenAccessTypeRead,

  /// Opens for write only access.
  AdbOpenAccessTypeWrite,

  /// Opens for querying information.
  AdbOpenAccessTypeQueryInfo,
} AdbOpenAccessType;

/** \brief Defines sharing mode with which an I/O object (endpoint)
  should be opened.
*/
typedef enum _AdbOpenSharingMode {
  /// Shares read and write.
  AdbOpenSharingModeReadWrite,

  /// Shares only read.
  AdbOpenSharingModeRead,

  /// Shares only write.
  AdbOpenSharingModeWrite,

  /// Opens exclusive.
  AdbOpenSharingModeExclusive,
} AdbOpenSharingMode;

/** \brief Provides information about an interface.
*/
typedef struct _AdbInterfaceInfo {
  /// Inteface's class id (see SP_DEVICE_INTERFACE_DATA for details)
  GUID          class_id;

  /// Interface flags (see SP_DEVICE_INTERFACE_DATA for details)
  unsigned long flags;

  /// Device name for the interface (see SP_DEVICE_INTERFACE_DETAIL_DATA
  /// for details)
  wchar_t       device_name[1];
} AdbInterfaceInfo;

/** \brief Creates USB interface enumerator

  This routine enumerates all USB interfaces that match provided class ID.
  This routine uses SetupDiGetClassDevs SDK routine to enumerate devices that
  match class ID and then SetupDiEnumDeviceInterfaces SDK routine is called
  to enumerate interfaces on the devices.
  @param[in] class_id Device class ID, assigned by the driver.
  @param[in] exclude_not_present If true enumation will include only those
         devices that are currently present.
  @param[in] exclude_removed If true interfaces with SPINT_REMOVED flag set
         will be not included in the enumeration.
  @param[in] active_only If true only active interfaces (with flag
           SPINT_ACTIVE set) will be included in the enumeration.
  @return Handle to the enumerator object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbEnumInterfaces(GUID class_id,
                                          bool exclude_not_present,
                                          bool exclude_removed,
                                          bool active_only);

/** \brief Gets next interface information

  @param[in] adb_handle Handle to interface enumerator object obtained via
         AdbEnumInterfaces call.
  @param[out] info Upon successful completion will receive interface
         information. Can be NULL. If it is NULL, upon return from this
         routine size parameter will contain memory size required for the
         next entry.
  @param[in,out] size On the way in provides size of the memory buffer
         addressed by info parameter. On the way out (only if buffer was not
         big enough) will provide memory size required for the next entry.
  @return true on success, false on error. If false is returned
          GetLastError() provides extended error information.
          ERROR_INSUFFICIENT_BUFFER indicates that buffer provided in info
          parameter was not big enough and size parameter contains memory size
          required for the next entry. ERROR_NO_MORE_ITEMS indicates that
          enumeration is over and there are no more entries to return.
*/
ADBWIN_API bool __cdecl AdbNextInterface(ADBAPIHANDLE adb_handle,
                                 AdbInterfaceInfo* info,
                                 unsigned long* size);

/** \brief Resets enumerator so next call to AdbNextInterface will start
  from the beginning.

  @param[in] adb_handle Handle to interface enumerator object obtained via
         AdbEnumInterfaces call.
  @return true on success, false on error. If false is returned GetLastError()
          provides extended error information.
*/
ADBWIN_API bool __cdecl AdbResetInterfaceEnum(ADBAPIHANDLE adb_handle);

/** \brief Creates USB interface object

  This routine creates an object that represents a USB interface.
  @param[in] interface_name Name of the interface.
  @return Handle to the interface object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbCreateInterfaceByName(const wchar_t* interface_name);

/** \brief Creates USB interface object based on vendor, product and
  interface IDs.

  This routine creates and object that represents a USB interface on our
  device. It uses AdbCreateInterfaceByName to actually do the create.
  @param[in] class_id Device class ID, assigned by the driver.
  @param[in] vendor_id Device vendor ID
  @param[in] product_id Device product ID
  @param[in] interface_id Device interface ID. This parameter is optional.
         Value 0xFF indicates that interface should be addressed by vendor
         and product IDs only.
  @return Handle to the interface object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbCreateInterface(GUID class_id,
                                           unsigned short vendor_id,
                                           unsigned short product_id,
                                           unsigned char interface_id);

/** \brief Gets interface name.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[out] buffer Buffer for the name. Can be NULL in which case
         buffer_char_size will contain number of characters required for
         the name.
  @param[in,out] buffer_char_size On the way in supplies size (in characters)
         of the buffer. On the way out, if method failed and GetLastError
         reports ERROR_INSUFFICIENT_BUFFER, will contain number of characters
         required for the name.
  @param[in] ansi If true the name will be returned as single character
         string. Otherwise name will be returned as wide character string.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetInterfaceName(ADBAPIHANDLE adb_interface,
                                    void* buffer,
                                    unsigned long* buffer_char_size,
                                    bool ansi);

/** \brief Gets serial number for interface's device.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[out] buffer Buffer for the serail number string. Can be NULL in which
         case buffer_char_size will contain number of characters required for
         the string.
  @param[in,out] buffer_char_size On the way in supplies size (in characters)
         of the buffer. On the way out, if method failed and GetLastError
         reports ERROR_INSUFFICIENT_BUFFER, will contain number of characters
         required for the name.
  @param[in] ansi If true the name will be returned as single character
         string. Otherwise name will be returned as wide character string.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetSerialNumber(ADBAPIHANDLE adb_interface,
                                   void* buffer,
                                   unsigned long* buffer_char_size,
                                   bool ansi);

/** \brief Gets device descriptor for the USB device associated with
  the given interface.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[out] desc Upon successful completion will have usb device
         descriptor.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetUsbDeviceDescriptor(ADBAPIHANDLE adb_interface,
                                          USB_DEVICE_DESCRIPTOR* desc);

/** \brief Gets descriptor for the selected USB device configuration.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[out] desc Upon successful completion will have usb device
         configuration descriptor.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetUsbConfigurationDescriptor(
                    ADBAPIHANDLE adb_interface,
                    USB_CONFIGURATION_DESCRIPTOR* desc);

/** \brief Gets descriptor for the given interface.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[out] desc Upon successful completion will have usb device
         configuration descriptor.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetUsbInterfaceDescriptor(ADBAPIHANDLE adb_interface,
                                             USB_INTERFACE_DESCRIPTOR* desc);

/** \brief Gets information about an endpoint on the given interface.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[in] endpoint_index Zero-based endpoint index. There are two
         shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
         and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide information
         about bulk write and bulk read endpoints respectively.
  @param[out] info Upon successful completion will have endpoint information.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetEndpointInformation(ADBAPIHANDLE adb_interface,
                                          unsigned char endpoint_index,
                                          AdbEndpointInformation* info);

/** \brief Gets information about default bulk read endpoint on the given
  interface.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[out] info Upon successful completion will have endpoint information.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetDefaultBulkReadEndpointInformation(
                    ADBAPIHANDLE adb_interface,
                    AdbEndpointInformation* info);

/** \brief Gets information about default bulk write endpoint on the given
  interface.

  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[out] info Upon successful completion will have endpoint information.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbGetDefaultBulkWriteEndpointInformation(
                    ADBAPIHANDLE adb_interface,
                    AdbEndpointInformation* info);

/** \brief Opens an endpoint on the given interface.

  Endpoints are always opened for overlapped I/O.
  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[in] endpoint_index Zero-based endpoint index. There are two
         shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
         and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide information
         about bulk write and bulk read endpoints respectively.
  @param[in] access_type Desired access type. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always read / write access.
  @param[in] sharing_mode Desired share mode. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always shared for read / write.
  @return Handle to the opened endpoint object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbOpenEndpoint(ADBAPIHANDLE adb_interface,
                                        unsigned char endpoint_index,
                                        AdbOpenAccessType access_type,
                                        AdbOpenSharingMode sharing_mode);

/** \brief Opens default bulk read endpoint on the given interface.

  Endpoints are always opened for overlapped I/O.
  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[in] access_type Desired access type. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always read / write access.
  @param[in] sharing_mode Desired share mode. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always shared for read / write.
  @return Handle to the opened endpoint object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbOpenDefaultBulkReadEndpoint(
                            ADBAPIHANDLE adb_interface,
                            AdbOpenAccessType access_type,
                            AdbOpenSharingMode sharing_mode);

/** \brief Opens default bulk write endpoint on the given interface.

  Endpoints are always opened for overlapped I/O.
  @param[in] adb_interface A handle to interface object created with 
         AdbCreateInterface call.
  @param[in] access_type Desired access type. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always read / write access.
  @param[in] sharing_mode Desired share mode. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always shared for read / write.
  @return Handle to the opened endpoint object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbOpenDefaultBulkWriteEndpoint(
                            ADBAPIHANDLE adb_interface,
                            AdbOpenAccessType access_type,
                            AdbOpenSharingMode sharing_mode);

/** \brief Gets handle to interface object for the given endpoint

  @param[in] adb_endpoint A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @return Handle to the interface for this endpoint or NULL on failure. If NULL
          is returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbGetEndpointInterface(ADBAPIHANDLE adb_endpoint);

/** \brief Gets information about the given endpoint.

  @param[in] adb_endpoint A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param[out] info Upon successful completion will have endpoint information.
  @return true on success, false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbQueryInformationEndpoint(ADBAPIHANDLE adb_endpoint,
                                            AdbEndpointInformation* info);

/** \brief Asynchronously reads from the given endpoint.

  @param[in] adb_endpoint A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param[out] buffer Pointer to the buffer that receives the data.
  @param[in] bytes_to_read Number of bytes to be read.
  @param[out] bytes_read Number of bytes read. Can be NULL.
  @param[in] event_handle Event handle that should be signaled when async I/O
         completes. Can be NULL. If it's not NULL this handle will be used to
         initialize OVERLAPPED structure for this I/O.
  @param[in] time_out A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return A handle to IO completion object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbReadEndpointAsync(ADBAPIHANDLE adb_endpoint,
                                             void* buffer,
                                             unsigned long bytes_to_read,
                                             unsigned long* bytes_read,
                                             unsigned long time_out,
                                             HANDLE event_handle);

/** \brief Asynchronously writes to the given endpoint.

  @param[in] adb_endpoint A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param[in] buffer Pointer to the buffer containing the data to be written.
  @param[in] bytes_to_write Number of bytes to be written.
  @param[out] bytes_written Number of bytes written. Can be NULL.
  @param[in] event_handle Event handle that should be signaled when async I/O
         completes. Can be NULL. If it's not NULL this handle will be used to
         initialize OVERLAPPED structure for this I/O.
  @param[in] time_out A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return A handle to IO completion object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE __cdecl AdbWriteEndpointAsync(ADBAPIHANDLE adb_endpoint,
                                              void* buffer,
                                              unsigned long bytes_to_write,
                                              unsigned long* bytes_written,
                                              unsigned long time_out,
                                              HANDLE event_handle);

/** \brief Synchronously reads from the given endpoint.

  @param[in] adb_endpoint A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param[out] buffer Pointer to the buffer that receives the data.
  @param[in] bytes_to_read Number of bytes to be read.
  @param[out] bytes_read Number of bytes read. Can be NULL.
  @param[in] time_out A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return true on success and false on failure. If false is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbReadEndpointSync(ADBAPIHANDLE adb_endpoint,
                                    void* buffer,
                                    unsigned long bytes_to_read,
                                    unsigned long* bytes_read,
                                    unsigned long time_out);

/** \brief Synchronously writes to the given endpoint.

  @param[in] adb_endpoint A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param[in] buffer Pointer to the buffer containing the data to be written.
  @param[in] bytes_to_write Number of bytes to be written.
  @param[out] bytes_written Number of bytes written. Can be NULL.
  @param[in] time_out A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return true on success and false on failure. If false is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbWriteEndpointSync(ADBAPIHANDLE adb_endpoint,
                                     void* buffer,
                                     unsigned long bytes_to_write,
                                     unsigned long* bytes_written,
                                     unsigned long time_out);

/** \brief Gets overlapped I/O result for async I/O performed on the
  given endpoint.

  @param[in] adb_io_completion A handle to an I/O completion object returned
         from AdbRead/WriteAsync routines.
  @param[out] ovl_data Buffer for the copy of this object's OVERLAPPED
         structure. Can be NULL.
  @param[out] bytes_transferred Pointer to a variable that receives the
         number of bytes that were actually transferred by a read or write
         operation. See SDK doc on GetOvelappedResult for more information.
         Unlike regular GetOvelappedResult call this parameter can be NULL.
  @param[in] wait If this parameter is true, the method does not return
         until the operation has been completed. If this parameter is false
         and the operation is still pending, the method returns false and
         the GetLastError function returns ERROR_IO_INCOMPLETE.
  @return true if I/O has been completed or false on failure or if request
         is not yet completed. If false is returned GetLastError() provides
         extended error information. If GetLastError returns
         ERROR_IO_INCOMPLETE it means that I/O is not yet completed.
*/
ADBWIN_API bool __cdecl AdbGetOvelappedIoResult(ADBAPIHANDLE adb_io_completion,
                                        LPOVERLAPPED overlapped,
                                        unsigned long* bytes_transferred,
                                        bool wait);

/** \brief Checks if overlapped I/O has been completed.

  @param[in] adb_io_completion A handle to an I/O completion object returned
         from AdbRead/WriteAsync routines.
  @return true if I/O has been completed or false if it's still
          incomplete. Regardless of the returned value, caller should
          check GetLastError to validate that handle was OK.
*/
ADBWIN_API bool __cdecl AdbHasOvelappedIoComplated(ADBAPIHANDLE adb_io_completion);

/** \brief Closes handle previously opened with one of the API calls

  @param[in] adb_handle ADB handle previously opened with one of the API calls
  @return true on success or false on failure. If false is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool __cdecl AdbCloseHandle(ADBAPIHANDLE adb_handle);

#endif  // ANDROID_USB_API_ADBWINAPI_H__
