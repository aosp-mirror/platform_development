/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {assertUnreachable} from 'common/assert_utils';
import {
  HttpRequest,
  HttpRequestHeaderType,
  HttpRequestStatus,
  HttpResponse,
} from 'common/http_request';
import {
  AdbResponse,
  OnRequestSuccessCallback,
} from 'trace_collection/adb/adb_host_connection';
import {ConnectionState} from 'trace_collection/connection_state';

export const WINSCOPE_PROXY_URL = 'http://localhost:5544';
export const VERSION = '6.0.0';

type StateChangeCallbackType = (
  newState: ConnectionState,
  errorText: string,
) => Promise<void>;

export async function getFromProxy(
  path: string,
  securityTokenHeader: HttpRequestHeaderType,
  onSuccess: OnRequestSuccessCallback,
  onStateChange: StateChangeCallbackType,
  type?: XMLHttpRequest['responseType'],
): Promise<string> {
  const response = await HttpRequest.get(
    makeRequestPath(path),
    securityTokenHeader,
    type,
  );
  return await processProxyResponse(response, onSuccess, onStateChange);
}

function makeRequestPath(path: string): string {
  return WINSCOPE_PROXY_URL + path;
}

export async function postToProxy(
  path: string,
  securityTokenHeader: HttpRequestHeaderType,
  onSuccess: OnRequestSuccessCallback,
  onStateChange: StateChangeCallbackType,
  jsonRequest?: object,
): Promise<string> {
  const response = await HttpRequest.post(
    makeRequestPath(path),
    securityTokenHeader,
    jsonRequest,
  );
  return await processProxyResponse(response, onSuccess, onStateChange);
}

async function processProxyResponse(
  response: HttpResponse,
  onSuccess: OnRequestSuccessCallback,
  onStateChange: StateChangeCallbackType,
): Promise<string> {
  if (
    response.status === HttpRequestStatus.SUCCESS &&
    !isVersionCompatible(response)
  ) {
    await onStateChange(ConnectionState.INVALID_VERSION, '');
    return 'invalid version';
  }
  const adbResponse = await processHttpResponse(response, onSuccess);
  if (adbResponse !== undefined) {
    await onStateChange(adbResponse.errorState, adbResponse.errorMsg ?? '');
  }
  try {
    return `${JSON.parse(response.body)}`;
  } catch (e) {
    return typeof response.body === 'string' ? response.body : '';
  }
}

function isVersionCompatible(req: HttpResponse): boolean {
  const proxyVersion = req.getHeader('Winscope-Proxy-Version');
  if (!proxyVersion) return false;
  const [proxyMajor, proxyMinor, proxyPatch] = proxyVersion
    .split('.')
    .map((s) => Number(s));
  const [clientMajor, clientMinor, clientPatch] = VERSION.split('.').map((s) =>
    Number(s),
  );

  if (proxyMajor !== clientMajor) {
    return false;
  }

  if (proxyMinor === clientMinor) {
    // Check patch number to ensure user has deployed latest bug fixes
    return proxyPatch >= clientPatch;
  }

  return proxyMinor > clientMinor;
}

async function processHttpResponse(
  resp: HttpResponse,
  onSuccess: OnRequestSuccessCallback,
): Promise<AdbResponse | undefined> {
  let errorState: ConnectionState | undefined;
  let errorMsg: string | undefined;

  switch (resp.status) {
    case HttpRequestStatus.UNSENT:
      errorState = ConnectionState.NOT_FOUND;
      break;

    case HttpRequestStatus.UNAUTH:
      errorState = ConnectionState.UNAUTH;
      break;

    case HttpRequestStatus.SUCCESS:
      try {
        await onSuccess(resp);
      } catch (err) {
        errorState = ConnectionState.ERROR;
        errorMsg =
          `Error handling request response: ${err}\n` +
          `Request text: ${resp.text}\n` +
          `Request body: ${resp.body}`;
      }
      break;

    case HttpRequestStatus.ERROR:
      if (resp.type === 'text' || !resp.type) {
        errorMsg = resp.text;
      } else if (resp.type === 'arraybuffer') {
        errorMsg = String.fromCharCode.apply(null, new Array(resp.body));
        if (errorMsg === '\x00') {
          errorMsg = 'No data received.';
        }
      }
      errorState = ConnectionState.ERROR;
      break;

    default:
      assertUnreachable(resp.status);
  }

  return errorState !== undefined ? {errorState, errorMsg} : undefined;
}
