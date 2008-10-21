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
  This file consists of definition of the class CLoopbackDevice
  This project has been created from DDK's SoftUSBLoopback sample project
  that is located at $(DDK_PATH)\src\Test\DSF\USB\SoftUSBLoopback
*/

#pragma once
#include "resource.h"

#include "DeviceEmulator.h"
#include "LoopbackDeviceEvents.h"


//Release and Add ref macros
#define ADDREF(punk) { \
  if ((punk) != NULL) { \
    (punk)->AddRef(); \
  } \
}


#define RELEASE(punk) { \
  if ((punk) != NULL) { \
    IUnknown *_punkXxx = (punk); \
    (punk) = NULL; \
    _punkXxx->Release(); \
  } \
}

//HR check
#define IfFailHrGo(EXPR) { hr = (EXPR); if(FAILED(hr)) goto Exit; }
#define IfFalseHrGo(EXPR, HR) { if(!(EXPR)) { hr = (HR); goto Exit; } }

#pragma warning(disable: 4995) //Pragma deprecated

class ATL_NO_VTABLE CLoopbackDevice : 
    public CComObjectRootEx<CComSingleThreadModel>,
    public CComCoClass<CLoopbackDevice, &CLSID_LoopbackDevice>,
    public IConnectionPointContainerImpl<CLoopbackDevice>,
    public CProxy_ILoopbackDeviceEvents<CLoopbackDevice>, 
    public ISoftUSBEndpointEvents,
    public IDispatchImpl<ILoopbackDevice, &IID_ILoopbackDevice,
                         &LIBID_DeviceEmulatorLib,
                         /*wMajor =*/ 1, /*wMinor =*/ 0> {
 public:
    CLoopbackDevice();
    virtual ~CLoopbackDevice();
DECLARE_REGISTRY_RESOURCEID(IDR_LOOPBACKDEVICE)


BEGIN_COM_MAP(CLoopbackDevice)
    COM_INTERFACE_ENTRY(ILoopbackDevice)
    COM_INTERFACE_ENTRY(IDispatch)
    COM_INTERFACE_ENTRY(ISoftUSBEndpointEvents)
    COM_INTERFACE_ENTRY(IConnectionPointContainer)
END_COM_MAP()

BEGIN_CONNECTION_POINT_MAP(CLoopbackDevice)
    CONNECTION_POINT_ENTRY(__uuidof(ILoopbackDeviceEvents))
END_CONNECTION_POINT_MAP()


  DECLARE_PROTECT_FINAL_CONSTRUCT()

  HRESULT FinalConstruct();
    
  void FinalRelease();

 private:
  void InitMemberVariables();

   HRESULT CreateUSBDevice();
   HRESULT CreateStrings();
   HRESULT ConfigureDevice();
   HRESULT ConfigureOUTEndpoint();
   HRESULT ConfigureINEndpoint();
   HRESULT ConfigureInterface(ISoftUSBInterface* piInterface);
   HRESULT ConfigureConfig(ISoftUSBConfiguration* piConfig);

   HRESULT SetupConnectionPoint(IUnknown* punkObject,
                                REFIID iidConnectionPoint);
   HRESULT ReleaseConnectionPoint();

   // Underlying SoftUSBDevice object
   ISoftUSBDevice          *m_piSoftUSBDevice;
   // IN Endpoint
   ISoftUSBEndpoint        *m_piINEndpoint;
   // OUT Endpoint
   ISoftUSBEndpoint        *m_piOUTEndpoint;
   // Connection point interface
   IConnectionPoint        *m_piConnectionPoint;
   // Connection point cookie.
   DWORD                    m_dwConnectionCookie;
   // Index of interface identifier string
   int                      m_iInterfaceString;
   // Index of config identifier string
   int                      m_iConfigString;
     
 public:
  //ILoopbackDevice
  STDMETHOD(get_DSFDevice)(DSFDevice** ppDSFDevice);
  STDMETHOD(DoPolledLoopback)(long lTimeInterval);
  STDMETHOD(StartEventProcessing)();
  STDMETHOD(StartAsyncEventProcessing)();
  STDMETHOD(StopAsyncEventProcessing)();
  STDMETHOD(AreKeystrokesWaiting)(VARIANT_BOOL* pfvarKeyWaiting);

  //ISoftUSBEndpointEvents
  STDMETHOD(OnSetupTransfer)(BYTE DataToggle, BYTE* pbDataBuffer,
                             ULONG cbDataBuffer, BYTE* pbStatus);

  STDMETHOD(OnWriteTransfer)(BYTE DataToggle, BYTE* pbDataBuffer,
                             ULONG cbDataBuffer, BYTE* pbStatus);

  STDMETHOD(OnReadTransfer)(BYTE DataToggle, BYTE* pbDataBuffer,
                            ULONG cbDataBuffer,ULONG* cbDataWritten,
                            BYTE* pbStatus);       

  STDMETHOD(OnDeviceRequest)(USBSETUPREQUEST* pSetupRequest,
                             ULONG_PTR* RequestHandle, 
                             BYTE* pbHostData, ULONG cbHostData,
                             BYTE** ppbResponseData,
                             ULONG* pcbResponseData,BYTE* pbSetupStatus);

  STDMETHOD(OnDeviceRequestComplete)(ULONG_PTR RequestHandle,
                                     BYTE* pbFinalRequestStatus);
};

OBJECT_ENTRY_AUTO(__uuidof(LoopbackDevice), CLoopbackDevice)
