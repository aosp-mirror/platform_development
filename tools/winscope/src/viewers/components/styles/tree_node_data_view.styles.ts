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
export const hierarchyTreeNodeDataViewStyles = `
    .tree-view-internal-chip {
        display: inline-block;
    }

    .tree-view-chip {
        margin: 0 5px;
        padding: 0 10px;
        border-radius: 10px;
        background-color: #aaa;
        color: black;
    }

    .tree-view-chip.tree-view-chip-warn {
        background-color: #ffaa6b;
    }

    .tree-view-chip.tree-view-chip-error {
        background-color: #ff6b6b;
    }

    .tree-view-chip.tree-view-chip-gpu {
        background-color: #00c853;
    }

    .tree-view-chip.tree-view-chip-hwc {
        background-color: #448aff;
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
        color: #9b9b9b;
        display: flex;
    }
    .value {
        color: #8A2BE2;
    }
    .new {
        display: flex;
    }
    .value.null {
        color: #e1e1e1;
    }
    .value.number {
        color: #4c75fd;
    }
    .value.true {
        color: #2ECC40;
    }
    .value.false {
        color: #FF4136;
    }
`;
