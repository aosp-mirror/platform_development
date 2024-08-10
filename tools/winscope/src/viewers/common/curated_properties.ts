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

import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export interface SfLayerSummary {
  layerId: string;
  nodeId: string;
  name: string;
}

export interface SfSummaryProperty {
  key: string;
  desc?: string;
  simpleValue?: string;
  layerValues?: SfLayerSummary[];
}

export interface SfCuratedProperties {
  summary: SfSummaryProperty[];
  flags: string;
  calcTransform: PropertyTreeNode | undefined;
  calcCrop: string;
  finalBounds: string;
  reqTransform: PropertyTreeNode | undefined;
  reqCrop: string;
  bufferSize: string;
  frameNumber: string;
  bufferTransformType: string;
  destinationFrame: string;
  z: string;
  relativeParent: string;
  calcColor: string;
  calcShadowRadius: string;
  calcCornerRadius: string;
  calcCornerRadiusCrop: string;
  backgroundBlurRadius: string;
  reqColor: string;
  reqCornerRadius: string;
  inputTransform: PropertyTreeNode | undefined;
  inputRegion: string | undefined;
  focusable: string;
  cropTouchRegionWithItem: string;
  replaceTouchRegionWithCrop: string;
  inputConfig: string;
  ignoreDestinationFrame: boolean;
  hasInputChannel: boolean;
}

export interface VcCuratedProperties {
  className: string;
  hashcode: string;
  left: string;
  top: string;
  elevation: string;
  height: string;
  width: string;
  translationX: string;
  translationY: string;
  scrollX: string;
  scrollY: string;
  scaleX: string;
  scaleY: string;
  visibility: string;
  alpha: string;
  willNotDraw: string;
  clipChildren: string;
}

export type CuratedProperties = SfCuratedProperties | VcCuratedProperties;
