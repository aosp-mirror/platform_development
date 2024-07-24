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

export const viewerCardStyle = `
    .rects-view,
    .hierarchy-view,
    .ime-additional-properties,
    .properties-view {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: auto;
        border-radius: 4px;
    }

    .rects-view, .hierarchy-view, .properties-view {
        padding-top: 0px;
        padding-bottom: 16px;
        padding-left: 16px;
        padding-right: 16px;
    }

    .ime-additional-properties {
        padding: 16px;
    }
`;
