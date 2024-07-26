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

  .entries .filters {
    display: flex;
    flex-direction: row;
  }

  .entries .scroll {
    flex: 1;
  }

  .scroll .entry {
    display: flex;
    flex-direction: row;
    overflow-wrap: anywhere;
  }

  .filters div,
  .entries div {
    padding: 4px;
  }

  .time {
    flex: 0 1 250px;
  }

  .go-to-current-time {
    flex: none;
    margin-top: 4px;
    font-size: 12px;
    height: 65%;
    width: fit-content;
  }

  .placeholder-text {
    text-align: center;
  }

  .layer-or-display-id, .transaction-id {
    flex: none;
    width: 125px;
  }

  .vsyncid {
    flex: none;
    width: 110px;
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
    width: 200px;
  }

  .flags, .flags select-with-filter {
    flex: 2 0 250px;
  }

  .filters .flags {
    margin-right: 16px;
  }

  .log-level {
    flex: 1;
  }

  .filters .log-level, .filters .log-level select-with-filter {
    flex: 1;
  }

  .tag, .tag select-with-filter {
    flex: 2;
  }

  .source-file, .source-file select-with-filter {
    flex: 4;
  }

  .text {
    flex: 10;
  }

  .filters mat-form-field {
    width: 80%;
    font-size: 12px;
  }

  .title-section .filters {
    margin-top: 8px;
  }

  .transition-id {
    flex: 1;
  }

  .entries .headers {
    flex: 0 0 auto;
    display: flex;
    flex-direction: row;
    font-weight: bold;
    border-bottom: solid 1px rgba(0, 0, 0, 0.5);
  }

  .transition-type {
    flex: 2;
  }

  .jank_cuj-type {
    flex: 5;
  }

  .start-time, .end-time, .dispatch-time, .send-time {
    flex: 4;
  }

  .duration {
    flex: 3;
  }

  .status {
    flex: 2;
  }

  .entry .status {
    display: flex;
    align-items: center;
    gap: 5px;
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
`;
