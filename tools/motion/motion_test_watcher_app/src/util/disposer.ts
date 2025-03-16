/*
 * Copyright 2024 Google LLC
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

/** Function type to be invoked when a disposer clear. */
export type DisposableFn = () => void;

/** Objects disposable with an `Disposer`. */
export interface Disposable {
  dispose(): void;
}

/**
 * Tracks cleanup code and runs it upon calling `dispose`.
 *
 * By default, the disposer must not be used after it has been disposed. This
 * helps avoid memory leaks by preventing accidental registration of cleanup
 * code. Create the Disposer with `multiShot` set to
 */
export class Disposer implements Disposable {
  // null after a one-shot disposer was disposed.
  private _disposables?: Array<DisposableFn> = [];

  /**
   * @param multiShot Whether the disposer can be used for multiple dispose
   * cycles.
   */
  constructor(private multiShot: boolean = false) {}

  /**
   * Registers the function to be called upon `dispose()`.
   *
   * Must not be called on a one-shot Disposer after it has been disposed.
   */
  addFunction(disposable: DisposableFn) {
    if (!this._disposables) {
      throw new Error(
        'Adding new disposabled to already disposed one-shot disposer'
      );
    }
    this._disposables!.push(disposable);
  }

  /**
   * Registers the listener at the `eventTarget` and unregisters it upon
   * `dispose()`.
   *
   * Must not be called on a one-shot Disposer after it has been disposed.
   */
  addListener(
    eventTarget: EventTarget,
    type: string,
    callback: EventListenerOrEventListenerObject,
    options?: AddEventListenerOptions | boolean
  ) {
    this.addFunction(() =>
      eventTarget.removeEventListener(type, callback, options)
    );
    eventTarget.addEventListener(type, callback, options);
  }

  /**
   * Registers the Disposable to be cleaned up upon `dispose()`.
   *
   * Must not be called on a one-shot Disposer after it has been disposed.
   */
  addDisposable(disposable: Disposable) {
    this.addFunction(disposable.dispose.bind(disposable));
  }

  /**
   * Disposes all registered cleanup code.
   *
   * Must be called exactly once for one-shot Disposers, or at least once for
   * multi-shot disposers.
   */
  dispose(): void {
    if (!this._disposables) {
      throw new Error('dispose() an already disposed one-shot disposer.');
    }

    const toDispose = this._disposables;
    this._disposables = this.multiShot ? [] : undefined;
    for (const disposable of toDispose) {
      disposable();
    }
  }
}
