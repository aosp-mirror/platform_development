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
import {selectedElementStyle} from './selected_element.styles';

export const nodeStyles =
  `
    .node {
        position: relative;
        display: inline-flex;
        padding: 1px 0;
        width: 100%;
    }

    .node:not(.selected):not(.full-opacity):not(:hover) {
        opacity: 0.5;
    }

    .node.clickable {
        cursor: pointer;
    }

    .node:not(.selected).added,
    .node:not(.selected).addedMove {
        background-color: var(--added-element-color);
    }

    .node:not(.selected).deleted,
    .node:not(.selected).deletedMove {
        background-color: var(--deleted-element-color);
    }

    .node:not(.selected).modified {
        background-color: var(--modified-element-color);
    }

    .node:hover:not(.selected) {
        background-color: var(--hover-element-color);
    }

    .node.addedMove:after,
    .node.deletedMove:after {
        content: 'Moved';
        font: 14px 'Roboto', sans-serif;
        margin: 0 5px;
        background-color: ${Color.CHIP_BLUE};
        color: ${Color.TEXT_BLACK};
        border-radius: 10px;
        height: fit-content;
        padding: 3px;
    }
` + selectedElementStyle;

// FIXME: child-hover selector is not working.
export const treeNodeDataViewStyles = `
    .node + .children:not(.flattened):not(.with-gutter) {
        margin-left: 12px;
        padding-left: 11px;
    }

    .node + .children:not(.flattened).with-gutter {
        margin-left: 23px;
    }

    .node + .children:not(.flattened) {
        border-left: 1px solid var(--border-color);
    }

    .node.selected + .children {
        border-left: 1px solid rgb(150, 150, 150);
    }

    .node.child-selected + .children {
        border-left: 1px solid rgb(100, 100, 100);
    }

    .node:hover + .children {
        border-left: 1px solid rgba(150, 150, 150, 0.75);
    }

    .node.child-hover + .children {
        border-left: 1px solid #b4b4b4;
    }
`;

export const nodeInnerItemStyles = `
    .leaf-node-icon {
        content: '';
        display: inline-block;
        margin-left: 40%;
        margin-top: 40%;
        height: 5px;
        width: 5px;
        border-radius: 50%;
        background-color: ${Color.TEXT_GRAY};
    }

    .icon-wrapper, .description {
        position: relative;
        display: inline-block;
    }

    .icon-wrapper-show-state {
      position: absolute;
    }

    .icon-wrapper-copy {
        right: 0;
    }

    .toggle-tree-btn, .expand-tree-btn, .pin-node-btn, .toggle-rect-show-state-btn, .copy-btn {
        padding: 0;
    }

    .toggle-rect-show-state-btn, .copy-btn {
        transform: scale(0.75);
    }

    .pin-node-btn {
        transform: scale(0.7);
    }

    .description {
        align-items: center;
        flex: 1 1 auto;
        vertical-align: middle;
        word-break: break-all;
        flex-basis: 0;
    }

    .leaf-node-icon-wrapper {
        margin-left: 6px;
        min-height: 24px;
        width: 24px;
    }

    .icon-button {
        background: none;
        border: none;
        display: inline-block;
        vertical-align: middle;
        color: inherit;
        cursor: pointer;
        height: 24px;
        width: 24px;
        line-height: 24px;
    }

    .expand-tree-btn {
        float: right;
    }

    .expand-tree-btn.modified {
        background-color: var(--modified-element-color);
    }

    .expand-tree-btn.deleted,
    .expand-tree-btn.deletedMove {
        background-color: var(--deleted-element-color);
    }

    .expand-tree-btn.added,
    .expand-tree-btn.addedMove {
        background-color: var(--added-element-color);
    }

    .icon-wrapper-show-state {
        opacity: 1;
    }

    :host:not(:hover):not(.selected) .icon-wrapper-show-state,
    :host:not(:hover) .icon-wrapper-copy {
        visibility: hidden;
    }
`;
