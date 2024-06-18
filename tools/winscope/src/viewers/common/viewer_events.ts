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

import {Timestamp} from 'common/time';

export class ViewerEvents {
  static HighlightedNodeChange = 'HighlightedNodeChange';
  static HighlightedIdChange = 'HighlightedIdChange';

  static HierarchyPinnedChange = 'HierarchyPinnedChange';
  static HierarchyUserOptionsChange = 'HierarchyUserOptionsChange';
  static HierarchyFilterChange = 'HierarchyFilterChange';
  static RectShowStateChange = 'RectShowStateChange';

  static PropertiesUserOptionsChange = 'PropertiesUserOptionsChange';
  static PropertiesFilterChange = 'PropertiesFilterChange';
  static HighlightedPropertyChange = 'HighlightedPropertyChange';

  static RectGroupIdChange = 'RectGroupIdChange';
  static RectsUserOptionsChange = 'RectsUserOptionsChange';

  static AdditionalPropertySelected = 'AdditionalPropertySelected';
  static RectsDblClick = 'RectsDblClick';
  static MiniRectsDblClick = 'MiniRectsDblClick';

  static TimestampClick = 'TimestampClick';

  static LogLevelsFilterChanged = 'LogLevelsFilterChanged';
  static TagsFilterChanged = 'TagsFilterChanged';
  static SourceFilesFilterChanged = 'SourceFilesFilterChanged';
  static SearchStringFilterChanged = 'SearchStringFilterChanged';

  static VSyncIdFilterChanged = 'VSyncIdFilterChanged';
  static PidFilterChanged = 'PidFilterChanged';
  static UidFilterChanged = 'UidFilterChanged';
  static TypeFilterChanged = 'TypeFilterChanged';
  static LayerIdFilterChanged = 'LayerIdFilterChanged';
  static WhatFilterChanged = 'WhatFilterChanged';

  static LogClicked = 'LogClicked';

  static LogChangedByKeyPress = 'LogChangedByKeyPress';
  static TransactionIdFilterChanged = 'TransactionIdFilterChanged';
  static TransitionSelected = 'TransitionSelected';
}

export class RectDblClickDetail {
  constructor(public clickedRectId: string) {}
}

export class TimestampClickDetail {
  constructor(public timestamp?: Timestamp, public index?: number) {}
}
