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
import {View, Viewer, ViewType} from './viewer';

class ViewerStub implements Viewer {
  constructor(title: string, viewContent?: string) {
    this.title = title;

    if (viewContent !== undefined) {
      this.htmlElement = document.createElement('div');
      this.htmlElement.innerText = viewContent;
    } else {
      this.htmlElement = undefined as unknown as HTMLElement;
    }
  }

  notifyCurrentTraceEntries(entries: any) {
    // do nothing
  }

  getViews(): View[] {
    return [new View(ViewType.TAB, this.getDependencies(), this.htmlElement, this.title)];
  }

  getDependencies(): any {
    return;
  }

  private htmlElement: HTMLElement;
  private title: string;
}

export {ViewerStub};
