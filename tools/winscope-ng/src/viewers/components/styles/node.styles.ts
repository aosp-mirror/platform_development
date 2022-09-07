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
export const nodeStyles = `
    .node {position: relative;display: inline-block;padding: 2px; height: 100%; width: 100%;}
    .node.clickable {cursor: pointer;}
    .node:not(.selected).added,
    .node:not(.selected).addedMove {
        background: #03ff35;
    }

    .node:not(.selected).deleted,
    .node:not(.selected).deletedMove {
        background: #ff6b6b;
    }

    .node:hover:not(.selected) {background: #f1f1f1;}

    .node:not(.selected).modified {
        background: cyan;
    }

    .node.addedMove:after,
    .node.deletedMove:after {
        content: 'moved';
        margin: 0 5px;
        background: #448aff;
        border-radius: 5px;
        padding: 3px;
        color: white;
    }

    .selected {background-color: #365179;color: white;}
`;

export const treeNodeDataViewStyles = `
    .node.shaded:not(:hover):not(.selected):not(.added):not(.addedMove):not(.deleted):not(.deletedMove):not(.modified) {background: #f8f9fa}
    .node.selected + .children {border-left: 1px solid rgb(200, 200, 200);}
    .node.child-hover + .children {border-left: 1px solid #b4b4b4;}
    .node.hover + .children { border-left: 1px solid rgb(200, 200, 200);}
`;

export const nodeInnerItemStyles = `
    .leaf-node-icon {content: ''; display: inline-block; margin-left: 40%; margin-top: 40%; height: 5px; width: 5px; border-radius: 50%;background-color: #9b9b9b;}
    .leaf-node-icon-wrapper, .description, .toggle-tree-btn, .expand-tree-btn, .pin-node-btn { position: relative; display: inline-block;}
    mat-icon {margin: 0}
    .pin-node-btn {padding: 0; transform: scale(0.7)}
    .description {align-items: center; flex: 1 1 auto; vertical-align: middle; word-break: break-all;}
    .leaf-node-icon-wrapper{padding-left: 6px; padding-right: 6px; min-height: 24px; width: 24px; position:relative; align-content: center; vertical-align: middle;}
    .icon-button { background: none;border: none;display: inline-block;vertical-align: middle;}

    .expand-tree-btn {
        float: right;
        padding-left: 0px;
        padding-right: 0px;
    }

    .expand-tree-btn.modified {
        background: cyan;
    }

    .expand-tree-btn.deleted,
    .expand-tree-btn.deletedMove {
        background: #ff6b6b;
    }

    .expand-tree-btn.added,
    .expand-tree-btn.addedMove {
        background: #03ff35;
    }
`;