/*
 * Copyright 2021, The Android Open Source Project
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


import { BaseLayerTraceEntry } from "../common";
import LayerTraceEntry from "./LayerTraceEntry";

class LayerTraceEntryLazy extends BaseLayerTraceEntry {
  private _isInitialized: boolean = false;
  private _layersProto: any[];
  private _displayProtos: any[];
  timestamp: number;
  timestampMs: string;
  hwcBlob: string;
  where: string;
  private _lazyLayerTraceEntry: LayerTraceEntry;

  constructor (layersProto: any[], displayProtos: any[],
    timestamp: number, hwcBlob: string, where: string = '') {
      super();
      this._layersProto =  layersProto;
      this._displayProtos = displayProtos;
      this.timestamp = timestamp;
      this.timestampMs = timestamp.toString();
      this.hwcBlob = hwcBlob;
      this.where = where;

      this.declareLazyProperties();
    }

    private initialize() {
      if (this._isInitialized) return;

      this._isInitialized = true;
      this._lazyLayerTraceEntry = LayerTraceEntry.fromProto(
        this._layersProto, this._displayProtos, this.timestamp,
        this.hwcBlob, this.where);
      this._layersProto = [];
      this._displayProtos = [];
    }


    private declareLazyProperties() {
      Object.defineProperty(this, 'kind', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.kind;
      }});

      Object.defineProperty(this, 'timestampMs', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.timestampMs;
      }});

      Object.defineProperty(this, 'rects', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.rects;
      }});

      Object.defineProperty(this, 'proto', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.proto;
      }});

      Object.defineProperty(this, 'shortName', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.shortName;
      }});

      Object.defineProperty(this, 'isVisible', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.isVisible;
      }});

      Object.defineProperty(this, 'flattenedLayers', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.flattenedLayers;
      }});

      Object.defineProperty(this, 'stableId', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.stableId;
      }});

      Object.defineProperty(this, 'visibleLayers', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.visibleLayers;
      }});

      Object.defineProperty(this, 'children', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.children;
      }});

      Object.defineProperty(this, 'displays', {configurable: true, enumerable: true, get: function () {
        this.initialize();
        return this._lazyLayerTraceEntry.displays;
      }});
    }
}

export default LayerTraceEntryLazy;
