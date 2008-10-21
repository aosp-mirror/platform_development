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
  Declarations in this file, combined with definitions found in adb_api_extra.h
  comprise ADB API for windows.
*/

#include "adb_api_extra.h"

// Enables compillation for "straight" C
#ifdef __cplusplus
  #define EXTERN_C    extern "C"
#else
  #define EXTERN_C    extern
  typedef int bool;
  #define true  1
  #define false 0
#endif

// The following ifdef block is the standard way of creating macros which make
// exporting  from a DLL simpler. All files within this DLL are compiled with
// the ADBWIN_EXPORTS symbol defined on the command line. this symbol should
// not be defined on any project that uses this DLL. This way any other project
// whose source files include this file see ADBWIN_API functions as being
// imported from a DLL, whereas this DLL sees symbols defined with this macro
// as being exported.
#ifdef ADBWIN_EXPORTS
#define ADBWIN_API EXTERN_C __declspec(dllexport)
#else
#define ADBWIN_API EXTERN_C __declspec(dllimport)
#endif

/** Handle to an API object

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

/** Enumeration AdbOpenAccessType defines access type with which
  an I/O object (endpoint) should be opened.
*/
typedef enum _AdbOpenAccessType {
  /// Opens for read and write access
  AdbOpenAccessTypeReadWrite,

  /// Opens for read only access
  AdbOpenAccessTypeRead,

  /// Opens for write only access
  AdbOpenAccessTypeWrite,

  /// Opens for querying information
  AdbOpenAccessTypeQueryInfo,
} AdbOpenAccessType;

/** Enumeration AdbOpenSharingMode defines sharing mode with which
  an I/O object (endpoint) should be opened.
*/
typedef enum _AdbOpenSharingMode {
  /// Shares read and write
  AdbOpenSharingModeReadWrite,

  /// Shares only read
  AdbOpenSharingModeRead,

  /// Shares only write
  AdbOpenSharingModeWrite,

  /// Opens exclusive
  AdbOpenSharingModeExclusive,
} AdbOpenSharingMode;

/** Structure AdbInterfaceInfo provides information about an interface
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
  @param class_id[in] Device class ID, assigned by the driver.
  @param exclude_not_present[in] If 'true' enumation will include only those
         devices that are currently present.
  @param exclude_removed[in] If 'true' interfaces with SPINT_REMOVED flag set
         will be not included in the enumeration.
  @param active_only[in] If 'true' only active interfaces (with flag
           SPINT_ACTIVE set) will be included in the enumeration.
  @return Handle to the enumerator object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbEnumInterfaces(GUID class_id,
                                          bool exclude_not_present,
                                          bool exclude_removed,
                                          bool active_only);

/** \brief Gets next interface information

  @param adb_handle[in] Handle to interface enumerator object obtained via
         AdbEnumInterfaces call.
  @param info[out] Upon successful completion will receive interface
         information. Can be NULL. If it is NULL, upon return from this
         routine size parameter will contain memory size required for the
         next entry.
  @param size[in,out]. On the way in provides size of the memory buffer
         addressed by info parameter. On the way out (only if buffer was not
         big enough) will provide memory size required for the next entry.
  @return true on success, false on error. If false is returned
          GetLastError() provides extended error information.
          ERROR_INSUFFICIENT_BUFFER indicates that buffer provided in info
          parameter was not big enough and size parameter contains memory size
          required for the next entry. ERROR_NO_MORE_ITEMS indicates that
          enumeration is over and there are no more entries to return.
*/
ADBWIN_API bool AdbNextInterface(ADBAPIHANDLE adb_handle,
                                 AdbInterfaceInfo* info,
                                 unsigned long* size);

/** \brief Resets enumerator so next call to AdbNextInterface will start
  from the beginning.

  @param adb_handle[in] Handle to interface enumerator object obtained via
         AdbEnumInterfaces call.
  @return true on success, false on error. If false is returned GetLastError()
          provides extended error information.
*/
ADBWIN_API bool AdbResetInterfaceEnum(ADBAPIHANDLE adb_handle);

/** \brief Creates USB interface object

  This routine creates an object that represents a USB interface.
  @param interface_name[in] Name of the interface.
  @return Handle to the interface object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbCreateInterfaceByName(const wchar_t* interface_name);

/** \brief
  Creates USB interface object based on vendor, product and interface IDs.

  This routine creates and object that represents a USB interface on our
  device. It uses AdbCreateInterfaceByName to actually do the create.
  @param class_id[in] Device class ID, assigned by the driver.
  @param vendor_id[in] Device vendor ID
  @param product_id[in] Device product ID
  @param interface_id[in] Device interface ID. This parameter is optional.
         Value 0xFF indicates that interface should be addressed by vendor
         and product IDs only.
  @return Handle to the interface object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbCreateInterface(GUID class_id,
                                           unsigned short vendor_id,
                                           unsigned short product_id,
                                           unsigned char interface_id);

/** \brief Gets interface name.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param buffer[out] Buffer for the name. Can be NULL in which case
         buffer_char_size will contain number of characters required for
         the name.
  @param buffer_char_size[in/out] On the way in supplies size (in characters)
         of the buffer. On the way out, if method failed and GetLastError
         reports ERROR_INSUFFICIENT_BUFFER, will contain number of characters
         required for the name.
  @param ansi[in] If 'true' the name will be returned as single character
         string. Otherwise name will be returned as wide character string.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetInterfaceName(ADBAPIHANDLE adb_interface,
                                    void* buffer,
                                    unsigned long* buffer_char_size,
                                    bool ansi);

/** \brief Gets serial number for interface's device.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param buffer[out] Buffer for the serail number string. Can be NULL in which
         case buffer_char_size will contain number of characters required for
         the string.
  @param buffer_char_size[in/out] On the way in supplies size (in characters)
         of the buffer. On the way out, if method failed and GetLastError
         reports ERROR_INSUFFICIENT_BUFFER, will contain number of characters
         required for the name.
  @param ansi[in] If 'true' the name will be returned as single character
         string. Otherwise name will be returned as wide character string.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetSerialNumber(ADBAPIHANDLE adb_interface,
                                   void* buffer,
                                   unsigned long* buffer_char_size,
                                   bool ansi);

/** \brief Gets device descriptor for the USB device associated with
  the given interface.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param desc[out] Upon successful completion will have usb device
         descriptor.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetUsbDeviceDescriptor(ADBAPIHANDLE adb_interface,
                                          USB_DEVICE_DESCRIPTOR* desc);

/** \brief Gets descriptor for the selected USB device configuration.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param desc[out] Upon successful completion will have usb device
         configuration descriptor.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetUsbConfigurationDescriptor(ADBAPIHANDLE adb_interface,
                                                 USB_CONFIGURATION_DESCRIPTOR* desc);

/** \brief Gets descriptor for the given interface.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param desc[out] Upon successful completion will have usb device
         configuration descriptor.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetUsbInterfaceDescriptor(ADBAPIHANDLE adb_interface,
                                             USB_INTERFACE_DESCRIPTOR* desc);

/** \brief Gets information about an endpoint on the given interface.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param endpoint_index[in] Zero-based endpoint index. There are two
         shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
         and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide information
         about bulk write and bulk read endpoints respectively.
  @param info[out] Upon successful completion will have endpoint information.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetEndpointInformation(ADBAPIHANDLE adb_interface,
                                          unsigned char endpoint_index,
                                          AdbEndpointInformation* info);

/** \brief
  Gets information about default bulk read endpoint on the given interface.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param info[out] Upon successful completion will have endpoint information.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetDefaultBulkReadEndpointInformation(ADBAPIHANDLE adb_interface,
                                                         AdbEndpointInformation* info);

/** \brief
  Gets information about default bulk write endpoint on the given interface.

  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param info[out] Upon successful completion will have endpoint information.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbGetDefaultBulkWriteEndpointInformation(ADBAPIHANDLE adb_interface,
                                                          AdbEndpointInformation* info);

/** \brief Opens an endpoint on the given interface.

  Endpoints are always opened for overlapped I/O.
  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param endpoint_index[in] Zero-based endpoint index. There are two
         shortcuts for this parameter: ADB_QUERY_BULK_WRITE_ENDPOINT_INDEX
         and ADB_QUERY_BULK_READ_ENDPOINT_INDEX that provide information
         about bulk write and bulk read endpoints respectively.
  @param access_type[in] Desired access type. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always read / write access.
  @param sharing_mode[in] Desired share mode. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always shared for read / write.
  @return Handle to the opened endpoint object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbOpenEndpoint(ADBAPIHANDLE adb_interface,
                                        unsigned char endpoint_index,
                                        AdbOpenAccessType access_type,
                                        AdbOpenSharingMode sharing_mode);

/** \brief Opens default bulk read endpoint on the given interface.

  Endpoints are always opened for overlapped I/O.
  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param access_type[in] Desired access type. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always read / write access.
  @param sharing_mode[in] Desired share mode. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always shared for read / write.
  @return Handle to the opened endpoint object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbOpenDefaultBulkReadEndpoint(ADBAPIHANDLE adb_interface,
                                                       AdbOpenAccessType access_type,
                                                       AdbOpenSharingMode sharing_mode);

/** \brief Opens default bulk write endpoint on the given interface.

  Endpoints are always opened for overlapped I/O.
  @param adb_interface[in] A handle to interface object created with 
         AdbCreateInterface call.
  @param access_type[in] Desired access type. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always read / write access.
  @param sharing_mode[in] Desired share mode. In the current implementation
         this parameter has no effect on the way endpoint is opened. It's
         always shared for read / write.
  @return Handle to the opened endpoint object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbOpenDefaultBulkWriteEndpoint(ADBAPIHANDLE adb_interface,
                                                        AdbOpenAccessType access_type,
                                                        AdbOpenSharingMode sharing_mode);

/** \brief Gets handle to interface object for the given endpoint

  @param adb_endpoint[in] A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @return Handle to the interface for this endpoint or NULL on failure. If NULL
          is returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbGetEndpointInterface(ADBAPIHANDLE adb_endpoint);

/** \brief Gets information about the given endpoint.

  @param adb_endpoint[in] A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param info[out] Upon successful completion will have endpoint information.
  @return 'true' on success, 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbQueryInformationEndpoint(ADBAPIHANDLE adb_endpoint,
                                            AdbEndpointInformation* info);

/** \brief Asynchronously reads from the given endpoint.

  @param adb_endpoint[in] A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param buffer[out] Pointer to the buffer that receives the data.
  @param bytes_to_read[in] Number of bytes to be read.
  @param bytes_read[out] Number of bytes read. Can be NULL.
  @param event_handle[in] Event handle that should be signaled when async I/O
         completes. Can be NULL. If it's not NULL this handle will be used to
         initialize OVERLAPPED structure for this I/O.
  @param time_out[in] A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return A handle to IO completion object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbReadEndpointAsync(ADBAPIHANDLE adb_endpoint,
                                             void* buffer,
                                             unsigned long bytes_to_read,
                                             unsigned long* bytes_read,
                                             unsigned long time_out,
                                             HANDLE event_handle);

/** \brief Asynchronously writes to the given endpoint.

  @param adb_endpoint[in] A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param buffer[in] Pointer to the buffer containing the data to be written.
  @param bytes_to_write[in] Number of bytes to be written.
  @param bytes_written[out] Number of bytes written. Can be NULL.
  @param event_handle[in] Event handle that should be signaled when async I/O
         completes. Can be NULL. If it's not NULL this handle will be used to
         initialize OVERLAPPED structure for this I/O.
  @param time_out[in] A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return A handle to IO completion object or NULL on failure. If NULL is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API ADBAPIHANDLE AdbWriteEndpointAsync(ADBAPIHANDLE adb_endpoint,
                                              void* buffer,
                                              unsigned long bytes_to_write,
                                              unsigned long* bytes_written,
                                              unsigned long time_out,
                                              HANDLE event_handle);

/** \brief Synchronously reads from the given endpoint.

  @param adb_endpoint[in] A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param buffer[out] Pointer to the buffer that receives the data.
  @param bytes_to_read[in] Number of bytes to be read.
  @param bytes_read[out] Number of bytes read. Can be NULL.
  @param time_out[in] A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return 'true' on success and 'false' on failure. If 'false' is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbReadEndpointSync(ADBAPIHANDLE adb_endpoint,
                                    void* buffer,
                                    unsigned long bytes_to_read,
                                    unsigned long* bytes_read,
                                    unsigned long time_out);

/** \brief Synchronously writes to the given endpoint.

  @param adb_endpoint[in] A handle to opened endpoint object, obtained via one
         of the AdbOpenXxxEndpoint calls.
  @param buffer[in] Pointer to the buffer containing the data to be written.
  @param bytes_to_write[in] Number of bytes to be written.
  @param bytes_written[out] Number of bytes written. Can be NULL.
  @param time_out[in] A timeout (in milliseconds) required for this I/O to
         complete. Zero value for this parameter means that there is no
         timeout for this I/O.
  @return 'true' on success and 'false' on failure. If 'false' is
          returned GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbWriteEndpointSync(ADBAPIHANDLE adb_endpoint,
                                     void* buffer,
                                     unsigned long bytes_to_write,
                                     unsigned long* bytes_written,
                                     unsigned long time_out);

/** \brief Gets overlapped I/O result for async I/O performed on the
  given endpoint

  @param adb_io_completion[in] A handle to an I/O completion object returned
         from AdbRead/WriteAsync routines.
  @param ovl_data[out] Buffer for the copy of this object's OVERLAPPED
         structure. Can be NULL.
  @param bytes_transferred[out] Pointer to a variable that receives the
         number of bytes that were actually transferred by a read or write
         operation. See SDK doc on GetOvelappedResult for more information.
         Unlike regular GetOvelappedResult call this parameter can be NULL.
  @param wait[in] If this parameter is 'true', the method does not return
         until the operation has been completed. If this parameter is 'false'
         and the operation is still pending, the method returns 'false' and
         the GetLastError function returns ERROR_IO_INCOMPLETE.
  @return 'true' if I/O has been completed or 'false' on failure or if request
         is not yet completed. If 'false' is returned GetLastError() provides
         extended error information. If GetLastError returns
         ERROR_IO_INCOMPLETE it means that I/O is not yet completed.
*/
ADBWIN_API bool AdbGetOvelappedIoResult(ADBAPIHANDLE adb_io_completion,
                                        LPOVERLAPPED overlapped,
                                        unsigned long* bytes_transferred,
                                        bool wait);

/** \brief Checks if overlapped I/O has been completed.

  @param adb_io_completion[in] A handle to an I/O completion object returned
         from AdbRead/WriteAsync routines.
  @return 'true' if I/O has been completed or 'false' if it's still
          incomplete. Regardless of the returned value, caller should
          check GetLastError to validate that handle was OK.
*/
ADBWIN_API bool AdbHasOvelappedIoComplated(ADBAPIHANDLE adb_io_completion);

/** \brief Closes handle previously opened with one of the API calls

  @param adb_handle[in] ADB handle previously opened with one of the API calls
  @return 'true' on success or 'false' on failure. If 'false' is returned
          GetLastError() provides extended error information.
*/
ADBWIN_API bool AdbCloseHandle(ADBAPIHANDLE adb_handle);


#endif  // ANDROID_USB_API_ADBWINAPI_H__
