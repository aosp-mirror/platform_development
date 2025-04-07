/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {Type} from '@angular/core';
import {ComponentFixture, flush} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {assertDefined} from 'common/assert_utils';
import {KeyboardEventKey, KeyboardEventKeyCode} from 'common/dom_utils';

export class DOMTestHelper<T> {
  constructor(
    private fixture: ComponentFixture<T>,
    private root: HTMLElement,
  ) {}

  detectChanges() {
    this.fixture.detectChanges();
  }

  async whenStable() {
    await this.fixture.whenStable();
  }

  async whenRenderingDone() {
    await this.fixture.whenRenderingDone();
  }

  async detectChangesAndWaitStable() {
    this.detectChanges();
    await this.whenStable();
  }

  async detectChangesAndRenderingDone() {
    this.detectChanges();
    await this.fixture.whenRenderingDone();
  }

  find(selector: string): DOMTestHelper<T> | undefined {
    const element = this.root.querySelector<HTMLElement>(selector);
    return element ? new DOMTestHelper(this.fixture, element) : undefined;
  }

  findAll(selector: string): Array<DOMTestHelper<T>> {
    const helpers: Array<DOMTestHelper<T>> = [];
    this.root.querySelectorAll<HTMLElement>(selector).forEach((el) => {
      helpers.push(new DOMTestHelper(this.fixture, el));
    });
    return helpers;
  }

  findByDirective<T>(component: Type<T>): T | undefined {
    return (
      this.fixture.debugElement.query(By.directive(component))
        ?.componentInstance ?? undefined
    );
  }

  findInDocument(selector: string): DOMTestHelper<T> | undefined {
    const element = document.querySelector<HTMLElement>(selector);
    return element ? new DOMTestHelper(this.fixture, element) : undefined;
  }

  get(selector: string): DOMTestHelper<T> {
    return assertDefined(this.find(selector));
  }

  getInDocument(selector: string): DOMTestHelper<T> {
    return assertDefined(this.findInDocument(selector));
  }

  click() {
    this.root.click();
    this.fixture.detectChanges();
  }

  findAndClick(selector: string): DOMTestHelper<T> {
    const element = this.get(selector);
    element.click();
    return element;
  }

  findAndClickByIndex(selector: string, index: number): DOMTestHelper<T> {
    const element = this.findAll(selector)[index];
    element.click();
    return element;
  }

  findAndClickInDocument(selector: string): DOMTestHelper<T> {
    const element = this.getInDocument(selector);
    element.click();
    return element;
  }

  async clickAndWaitStable(selector: string) {
    const element = this.get(selector);
    element.click();
    await this.whenStable();
  }

  async clickByIndexAndWaitStable(selector: string, index: number) {
    const element = this.findAll(selector)[index];
    element.click();
    await this.whenStable();
  }

  async clickLastAndWaitStable(selector: string) {
    const elements = this.findAll(selector);
    const element = elements[elements.length - 1];
    element.click();
    await this.whenStable();
  }

  findAndDispatchInput(field: string, value: string): DOMTestHelper<T> {
    const input = this.get(field + ' input');
    input.dispatchInput(value);
    return input;
  }

  dispatchInput(value: string) {
    if (!(this.root instanceof HTMLInputElement)) {
      throw new Error('cannot dispatch input on node ' + this.root.nodeName);
    }
    this.root.value = value;
    this.root.dispatchEvent(new Event('input'));
    this.fixture.detectChanges();
  }

  isMatSelectOpen(): boolean {
    return this.findInDocument('.mat-select-panel') !== undefined;
  }

  async openMatSelect(index = 0) {
    const trigger = '.mat-select-trigger';
    await this.clickByIndexAndWaitStable(trigger, index);
  }

  clickMatOption() {
    const panel = this.getMatSelectPanel();
    panel.findAndClick('mat-option');
  }

  getMatSelectPanel(): DOMTestHelper<T> {
    return this.getInDocument('.mat-select-panel');
  }

  findMatTooltipPanel(): DOMTestHelper<T> | undefined {
    return this.findInDocument('.mat-tooltip-panel');
  }

  getSnackBar(): DOMTestHelper<T> {
    return this.getInDocument('snack-bar');
  }

  addEventListener(event: string, listener: (event: Event) => void) {
    this.root.addEventListener(event, listener);
  }

  keydownEnter() {
    const event = new KeyboardEvent('keydown', {key: KeyboardEventKey.ENTER});
    this.dispatchEvent(event);
  }

  keydownEsc() {
    const event = new KeyboardEvent('keydown', {key: KeyboardEventKey.ESCAPE});
    this.dispatchEvent(event);
  }

  keydownSpace() {
    const event = new KeyboardEvent('keydown', {
      keyCode: KeyboardEventKeyCode.SPACE,
    });
    this.dispatchEvent(event);
  }

  keydownArrowLeft(toDocument = false) {
    const event = new KeyboardEvent('keydown', {
      key: KeyboardEventKey.ARROW_LEFT,
    });
    toDocument
      ? this.dispatchEventInDocument(event)
      : this.dispatchEvent(event);
  }

  keydownArrowRight(toDocument = false) {
    const event = new KeyboardEvent('keydown', {
      key: KeyboardEventKey.ARROW_RIGHT,
    });
    toDocument
      ? this.dispatchEventInDocument(event)
      : this.dispatchEvent(event);
  }

  focusOut() {
    this.dispatchEvent(new FocusEvent('focusout'));
  }

  dragElement(x: number, y: number) {
    const {left, top} = this.root.getBoundingClientRect();
    this.dispatchMouseEvent(this.root, 'mousedown', left, top, 0, 0);
    this.dispatchMouseEvent(document, 'mousemove', left + 1, top + 0, 1, y);
    this.dispatchMouseEvent(document, 'mousemove', left + x, top + y, x, y);
    this.dispatchMouseEvent(document, 'mouseup', left + x, top + y, x, y);
  }

  dispatchEvent(event: Event) {
    this.root.dispatchEvent(event);
    this.detectChanges();
  }

  dispatchEventInDocument(event: Event) {
    document.dispatchEvent(event);
    this.detectChanges();
  }

  getHTMLElement<T extends HTMLElement>() {
    return this.root as T;
  }

  getText(): string | undefined {
    return this.root.textContent?.trim() ?? undefined;
  }

  checkText(value: string) {
    expect(this.root.textContent?.trim()).toContain(value);
  }

  checkTextExact(value: string) {
    expect(this.root.textContent?.trim()).toEqual(value);
  }

  checkInnerHTML(value: string, isPresent = true) {
    isPresent
      ? expect(this.root.innerHTML).toContain(value)
      : expect(this.root.innerHTML).not.toContain(value);
  }

  checkClassName(value: string, isPresent = true) {
    isPresent
      ? expect(this.root.className).toContain(value)
      : expect(this.root.className).not.toContain(value);
  }

  checkDisabled(value: boolean) {
    if ('disabled' in this.root) {
      return (this.root as any).disabled === value;
    }
    throw new Error('disabled not present on node ' + this.root.nodeName);
  }

  checkValue(value: string) {
    if ('value' in this.root) {
      return (this.root as any).value === value;
    }
    throw new Error('value not present on node ' + this.root.nodeName);
  }

  updateValue(value: string) {
    (this.root as any).value = value;
  }

  checkSectionCollapseAndExpand(selector: string, sectionTitle: string) {
    const section = this.get(selector);
    section
      .get('collapsible-section-title .mat-title')
      .checkTextExact(sectionTitle);
    section.findAndClick('collapsible-section-title button');
    section.checkClassName('collapsed');
    const collapsedSection = this.get('collapsed-sections .collapsed-section');
    collapsedSection.checkTextExact(sectionTitle + '  arrow_right');
    collapsedSection.click();
    this.checkNoCollapsedSectionButtons();
  }

  checkNoCollapsedSectionButtons() {
    const collapsedSections = this.get('collapsed-sections');
    expect(collapsedSections.find('.collapsed-section')).toBeUndefined();
  }

  async checkTooltip(text: string | undefined) {
    this.dispatchEvent(new Event('mouseenter'));
    const panel = this.findMatTooltipPanel();
    if (text !== undefined) {
      assertDefined(panel).checkText(text);
    } else {
      expect(panel).toBeUndefined();
    }
    this.dispatchEvent(new Event('mouseleave'));
    await this.whenStable();
  }

  private dispatchMouseEvent(
    source: Node,
    type: string,
    screenX: number,
    screenY: number,
    clientX: number,
    clientY: number,
  ) {
    const event = document.createEvent('MouseEvent');
    event.initMouseEvent(
      type,
      true /* canBubble */,
      false /* cancelable */,
      window /* view */,
      0 /* detail */,
      screenX /* screenX */,
      screenY /* screenY */,
      clientX /* clientX */,
      clientY /* clientY */,
      false /* ctrlKey */,
      false /* altKey */,
      false /* shiftKey */,
      false /* metaKey */,
      0 /* button */,
      null /* relatedTarget */,
    );
    Object.defineProperty(event, 'buttons', {get: () => 1});
    source.dispatchEvent(event);
    this.detectChanges();
    flush();
  }
}

export async function checkTooltips<T>(
  elements: Array<DOMTestHelper<T>>,
  expTooltips: Array<string | undefined>,
) {
  for (const [index, el] of elements.entries()) {
    await el.checkTooltip(expTooltips[index]);
  }
}
