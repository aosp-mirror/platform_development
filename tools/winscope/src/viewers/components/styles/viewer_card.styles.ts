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
  .rects-view:not(.collapsed),
  .hierarchy-view:not(.collapsed),
  .ime-additional-properties:not(.collapsed),
  .properties-view:not(.collapsed),
  .log-view:not(.collapsed),
  .property-groups:not(.collapsed),
  .global-search:not(.collapsed),
  .search-results:not(.collapsed),
  .how-to-search:not(.collapsed) {
    display: flex;
    flex-direction: column;
    overflow: auto;
    border-radius: 4px;
    margin: 4px;
    background-color: var(--background-color);
    box-shadow: 0px 1px 3px var(--border-color), 0px 1px 2px var(--border-color);
  }

  .hierarchy-view:not(.collapsed),
  .ime-additional-properties:not(.collapsed),
  .properties-view:not(.collapsed),
  .log-view:not(.collapsed),
  .property-groups:not(.collapsed),
  .global-search:not(.collapsed),
  .search-results:not(.collapsed),
  .how-to-search:not(.collapsed) {
    padding-bottom: 12px;
  }

  .rects-view:not(.collapsed),
  .hierarchy-view:not(.collapsed),
  .ime-additional-properties:not(.collapsed),
  .properties-view:not(.collapsed),
  .global-search:not(.collapsed),
  .how-to-search:not(.collapsed) {
    flex: 1;
  }

  .property-groups:not(.collapsed):not(.empty) {
    flex: 2;
  }

  .property-groups.empty:not(.collapsed) {
    flex: 0.2;
  }

  .log-view:not(.collapsed),
  .search-results:not(.collapsed) {
    flex: 3;
  }

  .collapsed {
    display: none;
  }
`;

export const viewerCardInnerStyle = `
  .title-section {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: space-between;
    background-color: var(--card-title-background-color);
    padding: 0px 12px 0px 12px;
  }

  .view-controls {
    display: flex;
    flex-direction: row;
    align-items: baseline;
    padding: 8px 12px;
    overflow-x: auto;
    overflow-y: hidden;
  }

  .placeholder-text {
    padding: 8px 12px;
  }

  .padded-title {
    padding-bottom: 8px;
  }

  .warning {
    display: flex;
    align-items: center;
    padding: 2px 12px;
    background-color: var(--warning-background-color);
  }
  .warning-message {
    padding-inline-start: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: flex;
    align-items: center;
    width: 100%;
  }
  .warning-icon, .inline-icon {
    font-size: 18px;
    width: 18px;
    height: 18px;
    padding-inline-end: 4px;
  }
  .inline-icon {
    padding-inline-start: 4px;
  }
`;
