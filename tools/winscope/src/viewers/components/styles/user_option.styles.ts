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

import {Color} from 'app/colors';

export const userOptionStyle = `
  .user-option {
    line-height: 24px;
    padding: 0 10px;
    margin-inline-end: 10px;
    min-width: fit-content;
  }
  .user-option.not-enabled {
    background-color: var(--disabled-color);
  }

  .user-option-label {
    display: flex;
    flex-direction: row;
  }
  .user-option-label.with-chip {
    align-items: baseline;
  }
  .user-option-label:not(.with-chip) {
    align-items: center;
  }
  .user-option .mat-icon {
    margin-inline-start: 5px;
  }

  .user-option-chip {
    margin-inline-start: 5px;
    margin-top: 2px;
    padding: 0 10px;
    border-radius: 10px;
    background-color: ${Color.CHIP_GRAY};
    font-weight: normal;
    color: ${Color.TEXT_BLACK};
    height: 18px;
    align-items: center;
    display: flex;
  }
`;
