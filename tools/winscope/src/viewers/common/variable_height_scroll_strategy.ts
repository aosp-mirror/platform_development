/*
 * Copyright 2023, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  CdkVirtualScrollViewport,
  VirtualScrollStrategy,
} from '@angular/cdk/scrolling';
import {distinctUntilChanged, Observable, Subject} from 'rxjs';

export abstract class VariableHeightScrollStrategy
  implements VirtualScrollStrategy
{
  static readonly HIDDEN_ELEMENTS_TO_RENDER = 20;
  private scrollItems: object[] = [];
  private itemHeightCache = new Map<number, ItemHeight>(); // indexed by scrollIndex
  private wrapper: any = undefined;
  private viewport: CdkVirtualScrollViewport | undefined;
  scrolledIndexChangeSubject = new Subject<number>();
  scrolledIndexChange: Observable<number> =
    this.scrolledIndexChangeSubject.pipe(distinctUntilChanged());

  attach(viewport: CdkVirtualScrollViewport) {
    this.viewport = viewport;
    this.wrapper = viewport.getElementRef().nativeElement.childNodes[0];
    if (this.scrollItems.length > 0) {
      this.viewport.setTotalContentSize(this.getTotalItemsHeight());
      this.updateRenderedRange();
    }
  }

  detach() {
    this.viewport = undefined;
    this.wrapper = undefined;
  }

  onDataLengthChanged() {
    if (!this.viewport) {
      return;
    }
    this.viewport.setTotalContentSize(this.getTotalItemsHeight());
    this.updateRenderedRange();
  }

  onContentScrolled(): void {
    if (this.viewport) {
      this.updateRenderedRange();
    }
  }

  onContentRendered() {
    // do nothing
  }

  onRenderedOffsetChanged() {
    // do nothing
  }

  updateItems(items: object[]) {
    this.scrollItems = items;

    if (this.viewport) {
      this.viewport.checkViewportSize();
    }
  }

  scrollToIndex(index: number) {
    if (!this.viewport) {
      return;
    }
    // scroll previous index to top, so when previous index is partially rendered the target index is still fully rendered
    const previousIndex = Math.max(0, index - 1);
    const offset = this.getOffsetByItemIndex(previousIndex);
    this.viewport.scrollToOffset(offset);
  }

  private updateRenderedRange() {
    if (!this.viewport) {
      return;
    }

    const scrollIndex = this.calculateIndexFromOffset(
      this.viewport.measureScrollOffset(),
    );
    const range = {
      start: Math.max(
        0,
        scrollIndex - VariableHeightScrollStrategy.HIDDEN_ELEMENTS_TO_RENDER,
      ),
      end: Math.min(
        this.viewport.getDataLength(),
        scrollIndex +
          this.numberOfItemsInViewport(scrollIndex) +
          VariableHeightScrollStrategy.HIDDEN_ELEMENTS_TO_RENDER,
      ),
    };
    this.viewport.setRenderedRange(range);
    this.viewport.setRenderedContentOffset(
      this.getOffsetByItemIndex(range.start),
    );
    this.scrolledIndexChangeSubject.next(scrollIndex);

    this.updateItemHeightCache();
  }

  private updateItemHeightCache() {
    if (!this.wrapper || !this.viewport) {
      return;
    }

    let cacheUpdated = false;

    for (const node of this.wrapper.childNodes) {
      if (node && node.nodeName === 'DIV') {
        const id = Number(node.getAttribute('item-id'));
        const cachedHeight = this.itemHeightCache.get(id);

        if (
          cachedHeight?.source !== ItemHeightSource.PREDICTED ||
          cachedHeight.value !== node.clientHeight
        ) {
          this.itemHeightCache.set(id, {
            value: node.clientHeight,
            source: ItemHeightSource.RENDERED,
          });
          cacheUpdated = true;
        }
      }
    }

    if (cacheUpdated) {
      this.viewport.setTotalContentSize(this.getTotalItemsHeight());
    }
  }

  private getTotalItemsHeight(): number {
    return this.getItemsHeight(this.scrollItems);
  }

  private getOffsetByItemIndex(index: number): number {
    return this.getItemsHeight(this.scrollItems.slice(0, index));
  }

  private getItemsHeight(items: object[]): number {
    return items
      .map((item, index) => this.getItemHeight(item, index))
      .reduce((prev, curr) => prev + curr, 0);
  }

  private calculateIndexFromOffset(offset: number): number {
    return this.calculateIndexOfFinalRenderedItem(0, offset) ?? 0;
  }

  private numberOfItemsInViewport(start: number): number {
    if (!this.viewport) {
      return 0;
    }

    const viewportHeight = this.viewport.getViewportSize();
    const i = this.calculateIndexOfFinalRenderedItem(start, viewportHeight);
    return i ? i - start + 1 : 0;
  }

  private calculateIndexOfFinalRenderedItem(
    start: number,
    viewportHeight: number,
  ): number | undefined {
    let totalItemHeight = 0;
    for (let i = start; i < this.scrollItems.length; i++) {
      const item = this.scrollItems[i];
      totalItemHeight += this.getItemHeight(item, i);

      if (totalItemHeight >= viewportHeight) {
        return i;
      }
    }
    return undefined;
  }

  private getItemHeight(item: object, index: number): number {
    const currentHeight = this.itemHeightCache.get(index);
    if (!currentHeight) {
      const predictedHeight = this.predictScrollItemHeight(item);
      this.itemHeightCache.set(index, {
        value: predictedHeight,
        source: ItemHeightSource.PREDICTED,
      });
      return predictedHeight;
    } else {
      return currentHeight.value;
    }
  }

  protected subItemHeight(subItem: string, rowLength: number): number {
    return Math.ceil(subItem.length / rowLength) * this.defaultRowSize;
  }

  protected abstract readonly defaultRowSize: number;

  // best-effort estimate of item height using hardcoded values -
  // we render more items than are in the viewport, and once rendered,
  // the item's actual height is cached and used instead of the estimate
  protected abstract predictScrollItemHeight(entry: object): number;
}

enum ItemHeightSource {
  PREDICTED,
  RENDERED,
}

interface ItemHeight {
  value: number;
  source: ItemHeightSource;
}
