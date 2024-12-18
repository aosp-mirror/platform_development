/*
 * Copyright (C) 2022 The Android Open Source Project
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

export const hierarchyTreeNodeDataViewStyles = `
    .tree-view-internal-chip {
        display: inline-block;
    }

    .tree-view-chip {
        margin: 0 5px;
        padding: 0 10px;
        border-radius: 10px;
        background-color: ${Color.CHIP_BLUE};
        color: ${Color.TEXT_BLACK};
    }
`;

export const propertyTreeNodeDataViewStyles = `
    .node-property {
        display: flex;
        flex-direction: row;
    }
    .property-key {
        position: relative;
        display: inline-block;
        padding-right: 2px;
        word-break: keep-all;
    }
    .property-value {
        position: relative;
        display: inline-block;
        vertical-align: top;
    }
    .old-value {
        color: var(--gray-text-color);
        display: flex;
    }
    .new {
        display: flex;
    }
    .value.null {
        color: var(--gray-text-color);
    }
    .value.true {
        color: var(--green-text-color);
    }
    .value.false {
        color: var(--red-text-color);
    }
`;
