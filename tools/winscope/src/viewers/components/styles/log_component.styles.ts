/*
 * Copyright (C) 2024 The Android Open Source Project
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

export const logComponentStyles = `
  .entries {
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: auto;
    margin: 4px;
    padding: 12px;
  }

  .entries .scroll {
    flex: 1;
  }

  .scroll .entry {
    display: flex;
    flex-direction: row;
    overflow-wrap: anywhere;
  }

  .headers div,
  .entries div {
    padding: 4px;
  }

  .filter {
    align-content: center;
  }

  .time {
    flex: 1;
    min-width: 135px;
  }

  .go-to-current-time {
    height: 100%;
    width: fit-content;
  }

  .placeholder-text {
    text-align: center;
  }

  .right-align {
    text-align: end;
    justify-content: end;
  }

  .layer-or-display-id {
    flex: 0.75;
    min-width: 85px;
  }

  .transaction-id {
    flex: 1;
    min-width: 85px;
  }

  .vsyncid {
    flex: none;
    width: 90px;
  }

  .pid {
    flex: none;
    width: 75px;
  }

  .uid {
    flex: none;
    width: 75px;
  }

  .transaction-type {
    flex: 1;
    min-width: 85px;
  }

  .flags {
    flex: 2;
    min-width: 100px;
  }

  .log-level {
    flex: 1;
    min-width: 85px;
  }

  .tag {
    flex: 2;
    min-width: 85px;
  }

  .source-file {
    flex: 4;
    min-width: 150px;
  }

  .text {
    flex: 10;
  }

  .title-section .filters {
    margin-top: 8px;
  }

  .transition-id {
    flex: none;
    width: 40px;
  }

  .entries .headers {
    flex: 0 0 auto;
    display: flex;
    flex-direction: row;
    font-weight: bold;
    border-bottom: solid 1px rgba(0, 0, 0, 0.5);
  }

  .header {
    display: flex;
    align-items: center;
  }

  .transition-type {
    flex: 1;
    min-width: 100px;
  }

  .handler {
    flex: 3;
    min-width: 70px;
  }

  .participants {
    flex: 3;
    white-space: pre-wrap;
    min-width: 100px;
  }

  .jank-cuj-type {
    flex: 5;
  }

  .start-time, .end-time, .dispatch-time, .send-time {
    flex: 2;
    min-width: 100px;
  }

  .duration {
    flex: none;
    width: 60px;
  }

  .status {
    flex: none;
    width: 110px;
  }

  .entry .status {
    display: flex;
    align-items: start;
    gap: 5px;
    justify-content: end;
  }

  .status .mat-icon {
    font-size: 18px;
    width: 18px;
    height: 18px;
  }

  .input-type {
    flex: 2;
  }
  .input-source {
    flex: 3;
  }
  .input-action {
    flex: 2;
  }
  .input-device-id {
    flex: 1;
  }
  .input-display-id {
    flex: 1;
    min-width: 50px;
  }
  .input-details {
    flex: 4;
  }
  .entry .input-windows {
    display: none;
    flex: 0;
  }
  .filters .input-windows {
    display: flex;
    flex: 10;
  }

  .search-result {
    flex: 1;
  }
`;
