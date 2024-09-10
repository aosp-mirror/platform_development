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

export const overlayPanelStyles = `
  .close-button {
    width: 24px;
    height: 24px;
    line-height: 24px;
  }

  .overlay-panel {
    font-family: 'Roboto', sans-serif;
    background: var(--overlay-panel-background-color);
    box-shadow: 0px 0px 9px 0px rgba(0, 0, 0, 0.36);
  }

  .overlay-panel-title {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    margin: 15px;
    font-size: 20px;
  }
  .overlay-panel-content {
    margin: 15px;
    display: flex;
    flex-direction: column;
  }

  .overlay-panel-section {
    margin: 10px 0px;
  }

  .overlay-panel-section-title {
    display: block;
    width: 100%;
  }
`;
