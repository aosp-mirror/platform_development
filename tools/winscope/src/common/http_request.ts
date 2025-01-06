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

import {assertDefined} from './assert_utils';

/**
 * Type for HTTP request headers.
 */
export type HttpRequestHeaderType = Array<[string, string]>;

/**
 * Status of an HTTP request.
 */
export enum HttpRequestStatus {
  UNSENT,
  UNAUTH,
  SUCCESS,
  ERROR,
}

/**
 * Response from an HTTP request.
 */
export interface HttpResponse {
  status: HttpRequestStatus;
  type: XMLHttpRequestResponseType; //eslint-disable-line no-undef
  text: string;
  body: any;
  getHeader: (name: string) => string | undefined;
}

/**
 * Class for making HTTP requests.
 */
export class HttpRequest {
  /**
   * Make a GET request.
   * @param path The path of the request.
   * @param headers The headers of the request.
   * @param type The response type of the request.
   * @return A promise that resolves to the response.
   */
  static async get(
    path: string,
    headers: HttpRequestHeaderType,
    type?: XMLHttpRequest['responseType'],
  ): Promise<HttpResponse> {
    return await HttpRequest.call('GET', path, headers, type);
  }

  /**
   * Make a POST request.
   * @param path The path of the request.
   * @param headers The headers of the request.
   * @param jsonRequest The JSON request body.
   * @return A promise that resolves to the response.
   */
  static async post(
    path: string,
    headers: HttpRequestHeaderType,
    jsonRequest?: object,
  ): Promise<HttpResponse> {
    return await HttpRequest.call(
      'POST',
      path,
      headers,
      undefined,
      jsonRequest,
    );
  }

  private static async call(
    method: string,
    path: string,
    headers: HttpRequestHeaderType,
    type?: XMLHttpRequest['responseType'],
    jsonRequest?: object,
  ): Promise<HttpResponse> {
    const req = new XMLHttpRequest();
    let status: HttpRequestStatus | undefined;

    await new Promise<void>((resolve) => {
      req.onreadystatechange = async () => {
        if (req.readyState !== XMLHttpRequest.DONE) {
          return;
        }
        if (req.status === XMLHttpRequest.UNSENT) {
          status = HttpRequestStatus.UNSENT;
        } else if (req.status === 200) {
          status = HttpRequestStatus.SUCCESS;
        } else if (req.status === 403) {
          status = HttpRequestStatus.UNAUTH;
        } else {
          status = HttpRequestStatus.ERROR;
        }
        resolve();
      };
      req.responseType = type || '';
      req.open(method, path);
      headers.forEach(([header, value]) => {
        req.setRequestHeader(header, value);
      });

      if (jsonRequest) {
        const json = JSON.stringify(jsonRequest);
        req.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
        req.send(json);
      } else {
        req.send();
      }
    });

    const hasResponseText =
      req.responseType === '' || req.responseType === 'text';

    return {
      status: assertDefined(status),
      type: req.responseType,
      text: hasResponseText ? req.responseText : '',
      body: req.response,
      getHeader: (name: string) => req.getResponseHeader(name) ?? undefined,
    };
  }
}
