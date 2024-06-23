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

import {
  animate,
  AnimationTriggerMetadata,
  state,
  style,
  transition,
  trigger,
} from '@angular/animations';
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ContentChild,
  ElementRef,
  forwardRef,
  Inject,
  Injectable,
  Input,
  NgZone,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import {Subject} from 'rxjs';
import {debounceTime, takeUntil} from 'rxjs/operators';

/**
 * Animations used by the Material drawers.
 * @docs-private
 */
export const matDrawerAnimations: {
  readonly transformDrawer: AnimationTriggerMetadata;
} = {
  /** Animation that slides a drawer in and out. */
  transformDrawer: trigger('transform', [
    // We remove the `transform` here completely, rather than setting it to zero, because:
    // 1. Having a transform can cause elements with ripples or an animated
    //    transform to shift around in Chrome with an RTL layout (see #10023).
    // 2. 3d transforms causes text to appear blurry on IE and Edge.
    state(
      'open, open-instant',
      style({
        transform: 'none',
        visibility: 'visible',
      }),
    ),
    state(
      'void',
      style({
        // Avoids the shadow showing up when closed in SSR.
        'box-shadow': 'none',
        visibility: 'hidden',
      }),
    ),
    transition('void => open-instant', animate('0ms')),
    transition(
      'void <=> open, open-instant => void',
      animate('400ms cubic-bezier(0.25, 0.8, 0.25, 1)'),
    ),
  ]),
};

/**
 * This component corresponds to a drawer that can be opened on the drawer container.
 */
@Injectable()
@Component({
  selector: 'mat-drawer',
  exportAs: 'matDrawer',
  template: `
    <div class="mat-drawer-inner-container" #content>
      <ng-content></ng-content>
    </div>
  `,
  styles: [
    `
      .mat-drawer.mat-drawer-bottom {
        left: 0;
        right: 0;
        bottom: 0;
        top: unset;
        position: fixed;
        z-index: 5;
        background-color: #f8f9fa;
        box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.3), 0px 1px 3px 1px rgba(0, 0, 0, 0.15);
      }
    `,
  ],
  animations: [matDrawerAnimations.transformDrawer],
  host: {
    class: 'mat-drawer mat-drawer-bottom',
    // must prevent the browser from aligning text based on value
    '[attr.align]': 'null',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class MatDrawer {
  @Input() mode: 'push' | 'overlay' = 'overlay';
  @Input() baseHeight = 0;

  getBaseHeight() {
    return this.baseHeight;
  }
}

@Component({
  selector: 'mat-drawer-content',
  template: '<ng-content></ng-content>',
  styles: [
    `
      .mat-drawer-content {
        display: flex;
        flex-direction: column;
        position: relative;
        z-index: 1;
        height: unset;
        overflow: unset;
        width: 100%;
        flex-grow: 1;
      }
    `,
  ],
  host: {
    class: 'mat-drawer-content',
    '[style.margin-top.px]': 'contentMargins.top',
    '[style.margin-bottom.px]': 'contentMargins.bottom',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class MatDrawerContent /*extends MatDrawerContentBase*/ {
  private contentMargins: {top: number | null; bottom: number | null} = {
    top: null,
    bottom: null,
  };

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
    @Inject(forwardRef(() => MatDrawerContainer))
    public container: MatDrawerContainer,
  ) {}

  ngAfterContentInit() {
    this.container.contentMarginChanges.subscribe(() => {
      this.changeDetectorRef.markForCheck();
    });
  }

  setMargins(margins: {top: number | null; bottom: number | null}) {
    this.contentMargins = margins;
  }
}

/**
 * Container for Material drawers
 * @docs-private
 */
@Component({
  selector: 'mat-drawer-container',
  exportAs: 'matDrawerContainer',
  template: `
    <ng-content select="mat-drawer-content"> </ng-content>

    <ng-content select="mat-drawer"></ng-content>
  `,
  styles: [
    `
      .mat-drawer-container {
        display: flex;
        flex-direction: column;
        flex-grow: 1;
        align-items: center;
        align-content: center;
        justify-content: center;
      }
    `,
  ],
  host: {
    class: 'mat-drawer-container',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
@Injectable()
export class MatDrawerContainer /*extends MatDrawerContainerBase*/ {
  /** Drawer that belong to this container. */
  @ContentChild(MatDrawer) drawer!: MatDrawer;
  @ContentChild(MatDrawer, {read: ElementRef}) drawerView!: ElementRef;

  @ContentChild(MatDrawerContent) content!: MatDrawerContent;
  @ViewChild(MatDrawerContent) userContent!: MatDrawerContent;

  /**
   * Margins to be applied to the content. These are used to push / shrink the drawer content when a
   * drawer is open. We use margin rather than transform even for push mode because transform breaks
   * fixed position elements inside of the transformed element.
   */
  contentMargins: {top: number | null; bottom: number | null} = {
    top: null,
    bottom: null,
  };

  readonly contentMarginChanges = new Subject<{
    top: number | null;
    bottom: number | null;
  }>();

  /** Emits on every ngDoCheck. Used for debouncing reflows. */
  private readonly doCheckSubject = new Subject<void>();

  /** Emits when the component is destroyed. */
  private readonly destroyed = new Subject<void>();

  constructor(@Inject(NgZone) private ngZone: NgZone) {}

  ngAfterContentInit() {
    this.updateContentMargins();

    // Avoid hitting the NgZone through the debounce timeout.
    this.ngZone.runOutsideAngular(() => {
      this.doCheckSubject
        .pipe(
          debounceTime(10), // Arbitrary debounce time, less than a frame at 60fps
          takeUntil(this.destroyed),
        )
        .subscribe(() => this.updateContentMargins());
    });
  }

  ngOnDestroy() {
    this.doCheckSubject.complete();
    this.destroyed.next();
    this.destroyed.complete();
  }

  ngDoCheck() {
    this.ngZone.runOutsideAngular(() => this.doCheckSubject.next());
  }

  /**
   * Recalculates and updates the inline styles for the content. Note that this should be used
   * sparingly, because it causes a reflow.
   */
  updateContentMargins() {
    // If shift is enabled want to shift the content without resizing it. We do
    // this by adding to the top or bottom margin and simultaneously subtracting
    // the same amount of margin from the other side.
    let top = 0;
    let bottom = 0;

    const baseHeight = this.drawer.getBaseHeight();
    const height = this.getDrawerHeight();
    const shiftAmount =
      this.drawer.mode === 'push' ? Math.max(0, height - baseHeight) : 0;

    top -= shiftAmount;
    bottom += baseHeight + shiftAmount;

    // If either `top` or `bottom` is zero, don't set a style to the element. This
    // allows users to specify a custom size via CSS class in SSR scenarios where the
    // measured widths will always be zero. Note that we reset to `null` here, rather
    // than below, in order to ensure that the types in the `if` below are consistent.
    top = top || null!;
    bottom = bottom || null!;

    if (
      top !== this.contentMargins.top ||
      bottom !== this.contentMargins.bottom
    ) {
      this.contentMargins = {top, bottom};

      this.content.setMargins(this.contentMargins);

      // Pull back into the NgZone since in some cases we could be outside. We need to be careful
      // to do it only when something changed, otherwise we can end up hitting the zone too often.
      this.ngZone.run(() =>
        this.contentMarginChanges.next(this.contentMargins),
      );
    }
  }

  getDrawerHeight(): number {
    return this.drawerView.nativeElement
      ? this.drawerView.nativeElement.offsetHeight || 0
      : 0;
  }
}
