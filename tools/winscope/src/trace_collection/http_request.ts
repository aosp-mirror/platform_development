/*
 * Copyright 2024, The Android Open Source Project
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

import {OnRequestStateChangeCallback} from './on_request_state_change_callback';
import {RequestHeaderType} from './request_header_type';

export class HttpRequest {
  static async get(
    path: string,
    headers: RequestHeaderType,
    onReadyStateChange: OnRequestStateChangeCallback,
    type?: XMLHttpRequest['responseType'],
  ) {
    await HttpRequest.call('GET', path, headers, onReadyStateChange, type);
  }

  static async post(
    path: string,
    headers: RequestHeaderType,
    onReadyStateChange: OnRequestStateChangeCallback,
    jsonRequest?: object,
  ) {
    await HttpRequest.call(
      'POST',
      path,
      headers,
      onReadyStateChange,
      undefined,
      jsonRequest,
    );
  }

  private static async call(
    method: string,
    path: string,
    headers: RequestHeaderType,
    onReadyStateChange: OnRequestStateChangeCallback,
    type?: XMLHttpRequest['responseType'],
    jsonRequest?: object,
  ): Promise<void> {
    return new Promise((resolve) => {
      const request = new XMLHttpRequest();
      request.onreadystatechange = async () => {
        onReadyStateChange(request, resolve);
      };
      request.responseType = type || '';
      request.open(method, path);
      headers.forEach(([header, value]) => {
        request.setRequestHeader(header, value);
      });

      if (jsonRequest) {
        const json = JSON.stringify(jsonRequest);
        request.setRequestHeader(
          'Content-Type',
          'application/json;charset=UTF-8',
        );
        request.send(json);
      } else {
        request.send();
      }
    });
  }
}
