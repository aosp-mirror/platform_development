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

import {ChangeDetectorRef, Component, Inject} from '@angular/core';
import {FunctionUtils} from 'common/function_utils';
import {
  Message,
  MessageBugReport,
  MessagePing,
  MessageTimestamp,
  MessageType,
} from 'cross_tool/messages';

@Component({
  selector: 'app-root',
  template: `
    <span class="app-title">Remote Tool Mock (simulates cross-tool protocol)</span>

    <hr />
    <p>Open Winscope tab</p>
    <input
      class="button-open-winscope"
      type="button"
      value="Open"
      (click)="onButtonOpenWinscopeClick()" />

    <hr />
    <p>Send bugreport</p>
    <input
      class="button-upload-bugreport"
      type="file"
      value=""
      (change)="onUploadBugreport($event)" />

    <hr />
    <p>Send timestamp [ns]</p>
    <input class="input-timestamp" type="number" id="name" name="name" />
    <input
      class="button-send-timestamp"
      type="button"
      value="Send"
      (click)="onButtonSendTimestampClick()" />

    <hr />
    <p>Received timestamp:</p>
    <p class="paragraph-received-timestamp"></p>
  `,
})
export class AppComponent {
  static readonly TARGET = 'http://localhost:8080';
  static readonly TIMESTAMP_IN_BUGREPORT_MESSAGE = 1670509911000000000n;

  private winscope: Window | null = null;
  private isWinscopeUp = false;
  private onMessagePongReceived = FunctionUtils.DO_NOTHING;

  constructor(
    @Inject(ChangeDetectorRef) private changeDetectorRef: ChangeDetectorRef,
  ) {
    window.addEventListener('message', (event) => {
      this.onMessageReceived(event);
    });
  }

  async onButtonOpenWinscopeClick() {
    this.openWinscope();
    await this.waitWinscopeUp();
  }

  async onUploadBugreport(event: Event) {
    const [file, buffer] = await this.readInputFile(event);
    this.sendBugreport(file, buffer);
  }

  onButtonSendTimestampClick() {
    const inputTimestampElement = document.querySelector(
      '.input-timestamp',
    )! as HTMLInputElement;
    this.sendTimestamp(BigInt(inputTimestampElement.value));
  }

  private openWinscope() {
    this.printStatus('OPENING WINSCOPE');

    this.winscope = window.open(AppComponent.TARGET);
    if (!this.winscope) {
      throw new Error('Failed to open winscope');
    }

    this.printStatus('OPENED WINSCOPE');
  }

  private async waitWinscopeUp() {
    this.printStatus('WAITING WINSCOPE UP');

    const promise = new Promise<void>((resolve) => {
      this.onMessagePongReceived = () => {
        this.isWinscopeUp = true;
        resolve();
      };
    });

    setTimeout(async () => {
      while (!this.isWinscopeUp) {
        this.winscope!.postMessage(new MessagePing(), AppComponent.TARGET);
        await this.sleep(10);
      }
    }, 0);

    await promise;

    this.printStatus('DONE WAITING (WINSCOPE IS UP)');
  }

  private sendBugreport(file: File, buffer: ArrayBuffer) {
    this.printStatus('SENDING BUGREPORT');

    this.winscope!.postMessage(
      new MessageBugReport(file, AppComponent.TIMESTAMP_IN_BUGREPORT_MESSAGE),
      AppComponent.TARGET,
    );

    this.printStatus('SENT BUGREPORT');
  }

  private sendTimestamp(value: bigint) {
    this.printStatus('SENDING TIMESTAMP');

    this.winscope!.postMessage(
      new MessageTimestamp(value),
      AppComponent.TARGET,
    );

    this.printStatus('SENT TIMESTAMP');
  }

  private onMessageReceived(event: MessageEvent) {
    const message = event.data as Message;
    if (!message.type) {
      console.log(
        'Cross-tool protocol received unrecognized message:',
        message,
      );
      return;
    }

    switch (message.type) {
      case MessageType.PING:
        console.log(
          'Cross-tool protocol received unexpected ping message:',
          message,
        );
        break;
      case MessageType.PONG:
        this.onMessagePongReceived();
        break;
      case MessageType.BUGREPORT:
        console.log(
          'Cross-tool protocol received unexpected bugreport message:',
          message,
        );
        break;
      case MessageType.TIMESTAMP:
        console.log('Cross-tool protocol received timestamp message:', message);
        this.onMessageTimestampReceived(message as MessageTimestamp);
        break;
      case MessageType.FILES:
        console.log(
          'Cross-tool protocol received unexpected files message:',
          message,
        );
        break;
      default:
        console.log(
          'Cross-tool protocol received unrecognized message:',
          message,
        );
        break;
    }
  }

  private onMessageTimestampReceived(message: MessageTimestamp) {
    const paragraph = document.querySelector(
      '.paragraph-received-timestamp',
    ) as HTMLParagraphElement;
    paragraph.textContent = message.timestampNs.toString();
    this.changeDetectorRef.detectChanges();
  }

  private printStatus(status: string) {
    console.log('STATUS: ' + status);
  }

  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  private async readInputFile(event: Event): Promise<[File, ArrayBuffer]> {
    const files: FileList | null = (event?.target as HTMLInputElement)?.files;

    if (!files || !files[0]) {
      throw new Error('Failed to read input files');
    }

    return [files[0], await files[0].arrayBuffer()];
  }
}
