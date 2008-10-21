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
  This file consists of definition of the template class which implements the
  event interface ILoopbackDeviceEvents.
  This project has been created from DDK's SoftUSBLoopback sample project
  that is located at $(DDK_PATH)\src\Test\DSF\USB\SoftUSBLoopback
*/

template<class T>
class CProxy_ILoopbackDeviceEvents :
    public IConnectionPointImpl<T, &__uuidof(ILoopbackDeviceEvents)> {
 public:
  HRESULT _stdcall Fire_ContinueToPoll(VARIANT_BOOL *pfvarContinue) {
    HRESULT hr = S_OK;
    T* pThis = static_cast<T *>(this);
    int cConnections = m_vec.GetSize();

    for (int iConnection = 0; iConnection < cConnections; iConnection++) {
      pThis->Lock();
      CComPtr<IUnknown> punkConnection = m_vec.GetAt(iConnection);
      pThis->Unlock();

      IDispatch * pConnection = static_cast<IDispatch *>(punkConnection.p);

      if (pConnection) {
        CComVariant varResult;

        DISPPARAMS params = { NULL, NULL, 0, 0 };
        hr = pConnection->Invoke(1, IID_NULL, LOCALE_USER_DEFAULT,
                                 DISPATCH_METHOD, &params, &varResult,
                                 NULL, NULL);

        //Set the return parameter
        *pfvarContinue  = varResult.boolVal;
      }
    }
    return hr;
  }

  HRESULT _stdcall Fire_ContinueEventProcessing(VARIANT_BOOL *pfvarContinue) {
    HRESULT hr = S_OK;
    T * pThis = static_cast<T *>(this);
    int cConnections = m_vec.GetSize();

    for (int iConnection = 0; iConnection < cConnections; iConnection++) {
      pThis->Lock();
      CComPtr<IUnknown> punkConnection = m_vec.GetAt(iConnection);
      pThis->Unlock();

      IDispatch * pConnection = static_cast<IDispatch *>(punkConnection.p);

      if (pConnection) {
        CComVariant varResult;

        DISPPARAMS params = { NULL, NULL, 0, 0 };
        hr = pConnection->Invoke(2, IID_NULL, LOCALE_USER_DEFAULT,
                                 DISPATCH_METHOD, &params, &varResult,
                                 NULL, NULL);

        //Set the return parameter
        *pfvarContinue  = varResult.boolVal;
      }
    }
    return hr;
  }
};

