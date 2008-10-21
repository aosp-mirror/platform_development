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

/** \file
  This file consists of implementation of the class CLoopbackDevice:
    Implements the interface ILoopbackDevice and
    configures the loopback device to be a valid USB device.
    The device then processes input to its endpoint in one of 
    two ways.

    1. By running in polled mode where the data is simply 
       passed from the OUT Endpoint to the IN Endpoint

       or
    2. In Event mode where the loopback device receives a 
       callback to indicate that data needs to be processed,
       and then processes the data.
  This project has been created from DDK's SoftUSBLoopback sample project
  that is located at $(DDK_PATH)\src\Test\DSF\USB\SoftUSBLoopback
*/

#include "stdafx.h"
#include <stdio.h>
#include <conio.h>
#include <USBProtocolDefs.h>
#include <dsfif.h>
#include <softusbif.h>
#include "android_usb_common_defines.h"
#include "adb_api_extra.h"
#include "LoopbackDevice.h"
#include "DeviceEmulator_i.c"

// These are the indexes of the string descriptors. They are used both
// as the indexes of the strings with SoftUSBDevice.Strings and as the
// string descriptor index property values on the various objects (e.g.
// SoftUSBDevice.Manufacturer = STRING_IDX_MANUFACTURER).

#define STRING_IDX_MANUFACTURER     1
#define STRING_IDX_PRODUCT_DESC     2
#define STRING_IDX_SERIAL_NO        3
#define STRING_IDX_CONFIG           4
#define STRING_IDX_INTERFACE        5

CLoopbackDevice::CLoopbackDevice() {
  InitMemberVariables();
}

CLoopbackDevice::~CLoopbackDevice() {
  // Release the conneciton point
  ReleaseConnectionPoint();
    
  // Release any interface which the device is holding
  RELEASE(m_piConnectionPoint);
  RELEASE(m_piINEndpoint);
  RELEASE(m_piOUTEndpoint);

  if (NULL != m_piSoftUSBDevice) {
    (void)m_piSoftUSBDevice->Destroy();
    RELEASE(m_piSoftUSBDevice);
  }

  InitMemberVariables();
}

void CLoopbackDevice::InitMemberVariables() {
  m_piSoftUSBDevice = NULL;
  m_piINEndpoint = NULL;
  m_piOUTEndpoint = NULL;
  m_piConnectionPoint = NULL;
  m_iInterfaceString = 0;
  m_iConfigString = 0;
  m_dwConnectionCookie = 0;
}

HRESULT CLoopbackDevice::FinalConstruct() {
  // Perform tasks which may fail when the class CLoopbackDevice
  // is finally constructed. This involves creating the USB device 
  // object and initializing the device so that it is recognized
  // as a valid USB device by the controller
  HRESULT hr = S_OK;

  hr = CreateUSBDevice();
  IfFailHrGo(hr);

  hr = ConfigureDevice();
  IfFailHrGo(hr);

Exit:
    return hr;
}

void CLoopbackDevice::FinalRelease() {
}

HRESULT CLoopbackDevice::CreateUSBDevice() {
  // Creates the USB device and initializes the device member variables
  // and creates and initializes the device qualifier. The device qualifier
  // is required for USB2.0 devices.

  HRESULT hr = S_OK;
  ISoftUSBDeviceQualifier* piDeviceQual = NULL;
  USHORT prod_id = DEVICE_EMULATOR_PROD_ID;

  hr = ::CoCreateInstance(CLSID_SoftUSBDevice, 
                          NULL,
                          CLSCTX_INPROC_SERVER,
                          __uuidof(ISoftUSBDevice),     
                          reinterpret_cast<void**>(&m_piSoftUSBDevice));

  IfFailHrGo(hr);

  // Create the device qualifer
  hr = ::CoCreateInstance(CLSID_SoftUSBDeviceQualifier, 
                          NULL,
                          CLSCTX_INPROC_SERVER,
                          __uuidof(ISoftUSBDeviceQualifier),     
                          reinterpret_cast<void**>(&piDeviceQual));

  IfFailHrGo(hr);

  // Setup the device qualifier
  // binary coded decimal USB version 2.0
  IfFailHrGo(piDeviceQual->put_USB(0x0200));
  // FF=Vendor specfic device class
  IfFailHrGo(piDeviceQual->put_DeviceClass(0xff)); 
  // FF = Vendor specific device sub-class
  IfFailHrGo(piDeviceQual->put_DeviceSubClass(0xff));
  // FF = Vendor specific device protocol
  IfFailHrGo(piDeviceQual->put_DeviceProtocol(0xff)); 
  // Max packet size endpoint 0
  IfFailHrGo(piDeviceQual->put_MaxPacketSize0(64)); 
  // Number of configurations
  IfFailHrGo(piDeviceQual->put_NumConfigurations(1));

  // Setup the device 
  // binary coded decimal USB version 2.0
  IfFailHrGo(m_piSoftUSBDevice->put_USB(0x0200));
  // FF=Vendor specfic device class
  IfFailHrGo(m_piSoftUSBDevice->put_DeviceClass(0xff));
  // FF = Vendor specific device sub-class
  IfFailHrGo(m_piSoftUSBDevice->put_DeviceSubClass(0xff));
  // FF = Vendor specific device protocol
  IfFailHrGo(m_piSoftUSBDevice->put_DeviceProtocol(0xff)); 
  // Max packet size endpoint 0
  IfFailHrGo(m_piSoftUSBDevice->put_MaxPacketSize0(64)); 
  // Vendor ID - Google
  IfFailHrGo(m_piSoftUSBDevice->put_Vendor(DEVICE_VENDOR_ID));
  // product id - Device Emulator
  IfFailHrGo(m_piSoftUSBDevice->put_Product(static_cast<SHORT>(prod_id))); 
  // Binary decimal coded version 1.0
  IfFailHrGo(m_piSoftUSBDevice->put_Device(0x0100));
  // Device does not suppport remote wake up
  IfFailHrGo(m_piSoftUSBDevice->put_RemoteWakeup(VARIANT_FALSE));
  // Index of the manufacturer string
  IfFailHrGo(m_piSoftUSBDevice->put_Manufacturer(STRING_IDX_MANUFACTURER));
  // Index of the product descripton string
  IfFailHrGo(m_piSoftUSBDevice->put_ProductDesc(STRING_IDX_PRODUCT_DESC)); 
  // Index of the serial number string
  IfFailHrGo(m_piSoftUSBDevice->put_SerialNumber(STRING_IDX_SERIAL_NO));
  // Indicate that the device is self-powered
  IfFailHrGo(m_piSoftUSBDevice->put_SelfPowered(VARIANT_TRUE));
  // Indicate that the device has power
  IfFailHrGo(m_piSoftUSBDevice->put_Powered(VARIANT_TRUE));

  // Create the strings associated with the device
  IfFailHrGo(CreateStrings());

  // Add the device qualifier
  IfFailHrGo(m_piSoftUSBDevice->put_DeviceQualifier(piDeviceQual));

Exit:
  RELEASE(piDeviceQual);
  return hr;
}


HRESULT CLoopbackDevice::ConfigureConfig(ISoftUSBConfiguration* piConfig) {
  HRESULT hr = S_OK;
  // config number passed to SetConfig
  IfFailHrGo(piConfig->put_ConfigurationValue(1));
  // Index of string descriptor
  IfFailHrGo(piConfig->put_Configuration((BYTE)m_iConfigString));
  // Self powered
  IfFailHrGo(piConfig->put_Attributes(0x40));
  // Max power in 2mA units: 50 = 100mA
  IfFailHrGo(piConfig->put_MaxPower(50));

Exit:
  return hr;
}

HRESULT CLoopbackDevice::ConfigureINEndpoint() {
  HRESULT hr = S_OK;

  if (NULL == m_piINEndpoint) {
    IfFailHrGo(E_UNEXPECTED);
  }

  // Endpoint #1 IN 
  IfFailHrGo(m_piINEndpoint->put_EndpointAddress(0x81));
  // Bulk data endpoint
  IfFailHrGo(m_piINEndpoint->put_Attributes(0x02));
  IfFailHrGo(m_piINEndpoint->put_MaxPacketSize(1024));
  IfFailHrGo(m_piINEndpoint->put_Interval(0));
  IfFailHrGo(m_piINEndpoint->put_Halted(FALSE));
  // back pointer to the device
  IfFailHrGo(m_piINEndpoint->put_USBDevice(reinterpret_cast<SoftUSBDevice*>(m_piSoftUSBDevice)));
    
Exit:    
  return hr;
}

HRESULT CLoopbackDevice::ConfigureOUTEndpoint() {
  HRESULT hr = S_OK;

  if (NULL == m_piOUTEndpoint) {
    IfFailHrGo(E_UNEXPECTED);
  }

  // Endpoint #2 OUT
  IfFailHrGo(m_piOUTEndpoint->put_EndpointAddress(0x02));
  // Bulk data endpoint
  IfFailHrGo(m_piOUTEndpoint->put_Attributes(0x02));
  IfFailHrGo(m_piOUTEndpoint->put_MaxPacketSize(1024));
  IfFailHrGo(m_piOUTEndpoint->put_Interval(0));
  IfFailHrGo(m_piOUTEndpoint->put_Halted(FALSE));
  //back pointer to the device
  IfFailHrGo(m_piOUTEndpoint->put_USBDevice(reinterpret_cast<SoftUSBDevice*>(m_piSoftUSBDevice)));
    
Exit:    
  return hr;
}

HRESULT CLoopbackDevice::ConfigureInterface(ISoftUSBInterface* piInterface) {
  HRESULT hr = S_OK;

  IfFailHrGo(piInterface->put_InterfaceNumber(0));
  IfFailHrGo(piInterface->put_AlternateSetting(0));
  // Vendor specific class code
  IfFailHrGo(piInterface->put_InterfaceClass(0xFF));
  // Vendor specific sub class code
  IfFailHrGo(piInterface->put_InterfaceSubClass(0xFF));
  // Vendor specific protcol
  IfFailHrGo(piInterface->put_InterfaceProtocol(0xFF));
  //Index for string describing the interface
  IfFailHrGo(piInterface->put_Interface((BYTE)m_iInterfaceString));

Exit:
  return hr;
}

HRESULT CLoopbackDevice::ConfigureDevice() {
  HRESULT hr = S_OK;
  ISoftUSBConfiguration* piConfig = NULL;
  ISoftUSBInterface* piInterface = NULL;
  ISoftUSBConfigList* piConfigList = NULL;
  ISoftUSBInterfaceList* piInterfaceList = NULL;
  ISoftUSBEndpointList* piEndpointList= NULL; 
  VARIANT varIndex;
  VariantInit(&varIndex);

  // All members of the collection will be added at the default locations
  // so set up the index appropriately
  varIndex.vt = VT_ERROR;
  varIndex.scode = DISP_E_PARAMNOTFOUND;

  // Create the IN Endpoint
  hr = CoCreateInstance(CLSID_SoftUSBEndpoint, 
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBEndpoint),     
                        reinterpret_cast<void**>(&m_piINEndpoint));
  IfFailHrGo(hr);

  // Setup the IN Endpoint
  IfFailHrGo(ConfigureINEndpoint());

  // Create the OUT Endpoint
  hr = CoCreateInstance(CLSID_SoftUSBEndpoint, 
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBEndpoint),     
                        reinterpret_cast<void**>(&m_piOUTEndpoint));
  IfFailHrGo(hr);

  // Setup the OUT Endpoint
  IfFailHrGo(ConfigureOUTEndpoint());

  // Create the device interface
  hr = CoCreateInstance(CLSID_SoftUSBInterface, 
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBInterface),     
                        reinterpret_cast<void**>(&piInterface));
  IfFailHrGo(hr);

  // Setup the device interface
  IfFailHrGo(ConfigureInterface(piInterface));

  // Add the Endpoints to the endpoint list
  IfFailHrGo(piInterface->get_Endpoints(&piEndpointList));
  IfFailHrGo(piEndpointList->Add(reinterpret_cast<SoftUSBEndpoint*>(m_piINEndpoint), varIndex));
  IfFailHrGo(piEndpointList->Add(reinterpret_cast<SoftUSBEndpoint*>(m_piOUTEndpoint), varIndex));

  // Create the configuration
  hr = CoCreateInstance(CLSID_SoftUSBConfiguration, 
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBConfiguration),     
                        reinterpret_cast<void**>(&piConfig));
  IfFailHrGo(hr);

  // Set the configuration data up
  IfFailHrGo(ConfigureConfig(piConfig));

  // Add the interface to the interface collection
  IfFailHrGo(piConfig->get_Interfaces(&piInterfaceList));
  IfFailHrGo(piInterfaceList->Add(reinterpret_cast<SoftUSBInterface*>(piInterface), varIndex));

  // Add the configuration to the configuration collection
  IfFailHrGo(m_piSoftUSBDevice->get_Configurations(&piConfigList));
  IfFailHrGo(piConfigList->Add(reinterpret_cast<SoftUSBConfiguration*>(piConfig), varIndex));

Exit:
  RELEASE(piConfig);
  RELEASE(piInterface);
  RELEASE(piConfigList);
  RELEASE(piInterfaceList);
  RELEASE(piEndpointList);
  return hr;
}

HRESULT CLoopbackDevice::CreateStrings() {
  HRESULT hr = S_OK;
  ISoftUSBStringList* piStringList = NULL;
  ISoftUSBString* piStringManufacturer = NULL;
  ISoftUSBString* piStringProductDesc = NULL;
  ISoftUSBString* piStringSerialNo = NULL;
  ISoftUSBString* piStringConfig = NULL;
  ISoftUSBString* piStringEndpoint = NULL;
  BSTR bstrManufacturer = ::SysAllocString(L"Google, Inc");
  BSTR bstrProductDesc = ::SysAllocString(L"USB Emulating Device");
  BSTR bstrSerialNo = ::SysAllocString(L"123456789ABCDEF");
  BSTR bstrConfig = ::SysAllocString(L"Configuration with a single interface");
  BSTR bstrEndpoint = ::SysAllocString(L"Interface with bulk IN endpoint and bulk OUT endpoint");
  VARIANT varIndex;
  VariantInit(&varIndex);

  // Check that all BSTR allocations succeeded
  IfFalseHrGo(0 != ::SysStringLen(bstrManufacturer), E_OUTOFMEMORY);
  IfFalseHrGo(0 != ::SysStringLen(bstrProductDesc), E_OUTOFMEMORY);
  IfFalseHrGo(0 != ::SysStringLen(bstrSerialNo), E_OUTOFMEMORY);
  IfFalseHrGo(0 != ::SysStringLen(bstrConfig), E_OUTOFMEMORY);
  IfFalseHrGo(0 != ::SysStringLen(bstrEndpoint), E_OUTOFMEMORY);

  //Set up the varaint used as the index
  varIndex.vt = VT_I4;
  varIndex.lVal = STRING_IDX_MANUFACTURER;

  //Create and initialize the string descriptors. Also create a string 
  //descriptor index for each. This index is used both to set the string's
  //descriptors position in the m_piSoftUSBDevice.Strings and is the index value 
  //the GetDescriptors request from the host. Note that we don't use 
  //string descriptor index zero because that is a reserved value for a 
  //device's language ID descriptor.

  //Get the string list from the device
  hr = m_piSoftUSBDevice->get_Strings(&piStringList);
  IfFailHrGo(hr);

  hr = CoCreateInstance(CLSID_SoftUSBString,
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBString),     
                        reinterpret_cast<void**>(&piStringManufacturer));
  IfFailHrGo(hr);

  IfFailHrGo(piStringManufacturer->put_Value(bstrManufacturer));
  IfFailHrGo(piStringList->Add(reinterpret_cast<SoftUSBString*>(piStringManufacturer), varIndex));
    
  hr = CoCreateInstance(CLSID_SoftUSBString,
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBString),     
                        reinterpret_cast<void**>(&piStringProductDesc));

  IfFailHrGo(hr);
  IfFailHrGo(piStringProductDesc->put_Value(bstrProductDesc));
  varIndex.lVal = STRING_IDX_PRODUCT_DESC;
  IfFailHrGo(piStringList->Add(reinterpret_cast<SoftUSBString*>(piStringProductDesc), varIndex));

  hr = CoCreateInstance(CLSID_SoftUSBString,
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBString),     
                        reinterpret_cast<void**>(&piStringSerialNo));
  IfFailHrGo(hr);
  IfFailHrGo(piStringSerialNo->put_Value(bstrSerialNo));
  varIndex.lVal = STRING_IDX_SERIAL_NO;
  IfFailHrGo(piStringList->Add(reinterpret_cast<SoftUSBString*>(piStringSerialNo), varIndex));

  hr = CoCreateInstance(CLSID_SoftUSBString,
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBString),     
                        reinterpret_cast<void**>(&piStringConfig));
  IfFailHrGo(hr);
  IfFailHrGo(piStringConfig->put_Value(bstrConfig));
  varIndex.lVal = STRING_IDX_CONFIG;
  m_iConfigString = varIndex.lVal;
  IfFailHrGo(piStringList->Add(reinterpret_cast<SoftUSBString*>(piStringConfig), varIndex));

  hr = CoCreateInstance(CLSID_SoftUSBString,
                        NULL,
                        CLSCTX_INPROC_SERVER,
                        __uuidof(ISoftUSBString),     
                        reinterpret_cast<void**>(&piStringEndpoint));
  IfFailHrGo(hr);
  IfFailHrGo(piStringEndpoint->put_Value(bstrEndpoint));
  varIndex.lVal = STRING_IDX_INTERFACE;
  m_iInterfaceString = varIndex.lVal;
  IfFailHrGo(piStringList->Add(reinterpret_cast<SoftUSBString*>(piStringEndpoint), varIndex));

Exit:
  RELEASE(piStringList);
  RELEASE(piStringManufacturer);
  RELEASE(piStringProductDesc);
  RELEASE(piStringSerialNo);
  RELEASE(piStringConfig);
  RELEASE(piStringEndpoint);
  ::SysFreeString(bstrManufacturer);
  ::SysFreeString(bstrProductDesc);
  ::SysFreeString(bstrSerialNo);
  ::SysFreeString(bstrConfig);
  ::SysFreeString(bstrEndpoint);

  return hr;
}

HRESULT CLoopbackDevice::ReleaseConnectionPoint() {
  HRESULT hr = S_OK;
    
  if (NULL != m_piConnectionPoint) {
    m_piConnectionPoint->Unadvise(m_dwConnectionCookie);
    m_dwConnectionCookie = 0;
  }

  RELEASE(m_piConnectionPoint);

  return hr;
}


HRESULT CLoopbackDevice::SetupConnectionPoint(IUnknown* punkObject,
                                              REFIID iidConnectionPoint) {
  HRESULT hr = S_OK;
  IConnectionPointContainer* piConnectionPointContainer = NULL;
  IUnknown* punkSink = NULL;

  //If there is already connection point enabled, disable it
  if(NULL != m_piConnectionPoint) {
    IfFailHrGo(ReleaseConnectionPoint());
  }
        
  IfFailHrGo(punkObject->QueryInterface(IID_IConnectionPointContainer,
                                        reinterpret_cast<void **>(&piConnectionPointContainer)));

  IfFailHrGo(piConnectionPointContainer->FindConnectionPoint(iidConnectionPoint,
                                                             &m_piConnectionPoint));

  // Get the IUknown of this interface as this is the event sink
  punkSink = (this)->GetUnknown(); 

  if(NULL == punkSink) {
    IfFailHrGo(E_UNEXPECTED);
  }

  IfFailHrGo(m_piConnectionPoint->Advise(punkSink, &m_dwConnectionCookie));


Exit:
  return hr;
}

STDMETHODIMP CLoopbackDevice::get_DSFDevice(DSFDevice** ppDSFDevice) {
  HRESULT hr = S_OK;
  DSFDevice* pDSFDevice = NULL;

  //Validate the the UDB device exists else this is an
  //internal error
  if (NULL == m_piSoftUSBDevice) {
    IfFailHrGo(E_UNEXPECTED);
  }    

  if (NULL == ppDSFDevice) {
    IfFailHrGo(E_POINTER);
  }

  IfFailHrGo(m_piSoftUSBDevice->get_DSFDevice(&pDSFDevice));
  IfFailHrGo(reinterpret_cast<IDSFDevice *>(pDSFDevice)->QueryInterface(__uuidof(IDispatch), reinterpret_cast<void **>(ppDSFDevice)));

Exit:
  if (NULL != pDSFDevice)
    reinterpret_cast<IDSFDevice *>(pDSFDevice)->Release();

  return hr;
}


STDMETHODIMP CLoopbackDevice::DoPolledLoopback(long lTimeInterval) {
/*
   Demonstrates how to use the drain OUT queue and queue IN data
   methods to communicate with the host controller. 

   The code checks to see if there is any data in the OUT, if no 
   data is present an event is fired to indicate if the function 
   should exit. If the function should not exit then the function 
   sleeps for the time interval before re-checking the queue.

   If there is data then the function reads the data and passes the
   data to the IN queue. This simply provides a loopback mechanism
   to the host controller.
*/
  HRESULT hr = S_OK;
  BOOL fKeepLooping = TRUE;
  // Number of items currently in the queue
  ULONG ulNoOfQueuedItems = 0;
  // Only going to read one transfer at a time
  ULONG ulTransfers = 1;
  SOFTUSB_OUT_TRANSFER* pOUTTransfer = NULL;
  // Copied the message status
  BYTE bStatus = 0;
  // Copied the message data
  BYTE* pDataBuffer = NULL;
  // Holds the size of the data buffer
  ULONG cbDataBuffer      = 0;
  VARIANT_BOOL fvarContinue = VARIANT_TRUE;

  if (NULL == m_piINEndpoint || NULL == m_piOUTEndpoint) {
    IfFailHrGo(E_UNEXPECTED);
  }

  while (fKeepLooping) {
    // Reset the number of queued items
    ulNoOfQueuedItems = 0;
        
    // Check to see if there is any data in the out queue
    IfFailHrGo(m_piOUTEndpoint->DrainOUTQueue(0, &ulNoOfQueuedItems, NULL));

    if (0 == ulNoOfQueuedItems) {
      // There is no data in the list so we need to check
      // If we should continue to loop
      // Fire Event to check if more processing is required
      IfFailHrGo(Fire_ContinueToPoll(&fvarContinue));

      // Check to see if the return value is VARIANT_FALSE
      if (VARIANT_FALSE == fvarContinue)
        fKeepLooping = FALSE;
            
      if (fKeepLooping)
        ::Sleep(lTimeInterval);
    } else {
      // There is data to read, loop until we have moved all 
      // the data from the OUT queue to the IN queue moving
      // one data item at a time
      do {
        // Get the OUT data
        IfFailHrGo(m_piOUTEndpoint->DrainOUTQueue(ulTransfers, 
                                                  &ulNoOfQueuedItems, 
                                                  &pOUTTransfer));

        // Setup the IN data
        bStatus= pOUTTransfer->bStatus;
        cbDataBuffer = pOUTTransfer->cbData;
        pDataBuffer =&pOUTTransfer->Data[0];
                
        // Send the data to the out queue
        IfFailHrGo(m_piINEndpoint->QueueINData(pDataBuffer,
                                               cbDataBuffer,
                                               bStatus,
                                               SOFTUSB_FOREVER));

        // Free the memory used by pOUTTransfer   
        m_piOUTEndpoint->FreeOUTQueue(pOUTTransfer);
        pOUTTransfer = NULL;

        // Force a context switch 
        ::Sleep(1);
      } while (0 != ulNoOfQueuedItems);
    }
  }

Exit:
  // If one of the calls failed pOUTTransfer will be NON-NULL 
  // And needs to be freed
  if (NULL != pOUTTransfer) {
    // Free the memory used by pOUTTransfer   
    m_piOUTEndpoint->FreeOUTQueue(pOUTTransfer);
    pOUTTransfer = NULL;
  }

  return hr;
}

STDMETHODIMP CLoopbackDevice::StartEventProcessing() {
/*
   Demonstrates how to setup event sinks so that the 
   event mechanism can be used to control data flow to and
   from the USB controller. In this example an event sink
   is installed on the OUT USB endpoint, when the controller
   has data to send to the device the OnWriteTransfer event
   will fire, this will occur on an arbitrary thread. The 
   device then simply copies this data and passes it the
   IN queue of the IN Endpoint.
*/
  HRESULT               hr                = S_OK;
  BOOL                  fKeepLooping      = TRUE;
  VARIANT_BOOL          fvarContinue      = VARIANT_TRUE;

  // Set up event sink on the OUT endpoint
  IfFailHrGo(SetupConnectionPoint(m_piOUTEndpoint, __uuidof(ISoftUSBEndpointEvents)));

  // Loop waiting for Events to be fired
  while (TRUE == fKeepLooping) {
    // Context switch to allow other threads to process
    ::Sleep(1);

    // Fire Event to check if the caller want to continue processing
    IfFailHrGo(Fire_ContinueEventProcessing(&fvarContinue));

    // Check to see if the return value is VARIANT_FALSE
    if (VARIANT_FALSE == fvarContinue)
      fKeepLooping = FALSE;
  }

  // Remove the event sink from the OUT endpoint
  IfFailHrGo(ReleaseConnectionPoint());
    
Exit:
  return hr;
}

STDMETHODIMP CLoopbackDevice::StartAsyncEventProcessing() {
/*
   Demonstrates how to setup event sinks so that the event mechanism can
   be used to control data flow to and from the USB controller. In this
   example an event sink is installed on the OUT USB endpoint, when the
   controller has data to send to the device the OnWriteTransfer event
   will fire, this will occur on an arbitrary thread. The device then
   simply copies this data and passes it the IN queue of the IN
   Endpoint. Control returns to the caller and event processing
   continues in an arbitrary thread. To terminate event processing call
   StopAsyncEventProcessing.
*/
  HRESULT hr = S_OK;

  // Set up event sink on the OUT endpoint
  IfFailHrGo(SetupConnectionPoint(m_piOUTEndpoint, __uuidof(ISoftUSBEndpointEvents)));

Exit:
  return hr;
}

STDMETHODIMP CLoopbackDevice::StopAsyncEventProcessing() {
  HRESULT hr = S_OK;
  // Remove the event sink on the OUT endpoint
  IfFailHrGo(ReleaseConnectionPoint());

Exit:
  return hr;
}



STDMETHODIMP CLoopbackDevice::AreKeystrokesWaiting(
    VARIANT_BOOL* pfvarKeyWaiting) {
/*
   Implements IDeviceEmulator::AreKeystrokesWaiting. It calls the low level 
   IO function _kbhit to see if the keyboard has been struck. If the Keyboard
   has been hit the function return VARIANT_TRUE otherwise it returns VARIANT_FALSE
*/
  HRESULT hr = S_OK;
  int iKeyHit = 0;

  if (NULL == pfvarKeyWaiting) {
    IfFailHrGo(E_POINTER);
  }
    
  *pfvarKeyWaiting = VARIANT_FALSE;

  iKeyHit = _kbhit();

  if (0 != iKeyHit)
    *pfvarKeyWaiting = VARIANT_TRUE;

Exit:
  return hr;
}

//ISoftUSBEndpointEvents

STDMETHODIMP CLoopbackDevice::OnSetupTransfer(BYTE DataToggle,
                                              BYTE* pbDataBuffer,
                                              ULONG cbDataBuffer,
                                              BYTE *pbStatus) {
  HRESULT hr = E_NOTIMPL;
  UNREFERENCED_PARAMETER(DataToggle);
  UNREFERENCED_PARAMETER(pbDataBuffer);
  UNREFERENCED_PARAMETER(cbDataBuffer);
  UNREFERENCED_PARAMETER(pbStatus);
  return hr;
}

STDMETHODIMP CLoopbackDevice::OnWriteTransfer(BYTE DataToggle,
                                              BYTE* pbDataBuffer,
                                              ULONG cbDataBuffer,
                                              BYTE * pbStatus) {
    
  HRESULT hr = S_OK;
  BYTE bINStatus = USB_ACK;
  UNREFERENCED_PARAMETER(DataToggle);

  // Check that the IN endpoint is valid
  if (NULL == m_piINEndpoint) {
    IfFailHrGo(E_UNEXPECTED);
  }

  // Send the data to the IN Endpoint
  IfFailHrGo(m_piINEndpoint->QueueINData(pbDataBuffer,
                                         cbDataBuffer,
                                         bINStatus,
                                         SOFTUSB_FOREVER));

  // ACK the status as the data was successfully sent to the IN endpoint
  *pbStatus = USB_ACK;

Exit:
  if (FAILED(hr))
    *pbStatus = USB_STALL;

  return hr;
}

STDMETHODIMP CLoopbackDevice::OnReadTransfer(BYTE DataToggle,
                                             BYTE* pbDataBuffer,
                                             ULONG cbDataBuffer,
                                             ULONG* cbDataWritten,
                                             BYTE* pbStatus) {
  HRESULT hr = E_NOTIMPL;
  UNREFERENCED_PARAMETER(DataToggle);
  UNREFERENCED_PARAMETER(pbDataBuffer);
  UNREFERENCED_PARAMETER(cbDataBuffer);
  UNREFERENCED_PARAMETER(cbDataWritten);
  UNREFERENCED_PARAMETER(pbStatus);
  return hr;
}

STDMETHODIMP CLoopbackDevice::OnDeviceRequest(USBSETUPREQUEST *pSetupRequest,
                                              ULONG_PTR* RequestHandle,
                                              BYTE* pbHostData,
                                              ULONG cbHostData,
                                              BYTE** ppbResponseData,
                                              ULONG* pcbResponseData,
                                              BYTE* pbSetupStatus) {
  HRESULT hr = E_NOTIMPL;
  UNREFERENCED_PARAMETER(pSetupRequest);
  UNREFERENCED_PARAMETER(RequestHandle);
  UNREFERENCED_PARAMETER(pbHostData);
  UNREFERENCED_PARAMETER(cbHostData);
  UNREFERENCED_PARAMETER(ppbResponseData);
  UNREFERENCED_PARAMETER(pcbResponseData);
  UNREFERENCED_PARAMETER(pbSetupStatus);
  return hr;
}

STDMETHODIMP CLoopbackDevice::OnDeviceRequestComplete(
    ULONG_PTR RequestHandle,
    BYTE* pbFinalRequestStatus) {
  HRESULT hr = E_NOTIMPL;
  UNREFERENCED_PARAMETER(RequestHandle);
  UNREFERENCED_PARAMETER(pbFinalRequestStatus);
  return hr;
}
