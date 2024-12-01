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

import {FilterFlag} from 'common/filter_flag';
import {UserOptions} from 'viewers/common/user_options';
import {RectShowState} from './rect_show_state';
import {TextFilter} from './text_filter';

export class TextFilterValues {
  private constructor(
    public filterString: string,
    public flags: FilterFlag[],
  ) {}

  static fromTextFilter(textFilter: TextFilter) {
    return new TextFilterValues(textFilter.filterString, textFilter.flags);
  }
}

export interface PresetHierarchy {
  hierarchyUserOptions: UserOptions;
  hierarchyFilter: TextFilterValues;
  propertiesUserOptions: UserOptions;
  propertiesFilter: TextFilterValues;
  rectsUserOptions?: UserOptions;
  rectIdToShowState?: Map<string, RectShowState> | undefined;
}
