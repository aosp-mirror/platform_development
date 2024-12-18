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
import {ArrayUtils} from 'common/array_utils';
import {assertDefined, assertUnreachable} from 'common/assert_utils';
import {Box3D} from 'common/geometry/box3d';
import {Point3D} from 'common/geometry/point3d';
import {Rect3D} from 'common/geometry/rect3d';
import {TransformMatrix} from 'common/geometry/transform_matrix';
import * as THREE from 'three';
import {
  CSS2DObject,
  CSS2DRenderer,
} from 'three/examples/jsm/renderers/CSS2DRenderer';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {Camera} from './camera';
import {ColorType} from './color_type';
import {RectLabel} from './rect_label';
import {UiRect3D} from './ui_rect3d';

export class Canvas {
  static readonly TARGET_SCENE_DIAGONAL = 4;
  static readonly RECT_COLOR_HIGHLIGHTED_LIGHT_MODE = new THREE.Color(
    0xd2e3fc, // Keep in sync with :not(.dark-mode) --selected-element-color in material-theme.scss
  );
  static readonly RECT_COLOR_HIGHLIGHTED_DARK_MODE = new THREE.Color(
    0x5f718a, // Keep in sync with .dark-mode --selected-element-color in material-theme.scss
  );
  static readonly RECT_COLOR_HAS_CONTENT = new THREE.Color(0xad42f5);
  static readonly RECT_EDGE_COLOR_LIGHT_MODE = 0x000000;
  static readonly RECT_EDGE_COLOR_DARK_MODE = 0xffffff;
  static readonly RECT_EDGE_COLOR_ROUNDED = 0x848884;
  static readonly RECT_EDGE_COLOR_PINNED = 0xffc24b; // Keep in sync with Color#PINNED_ITEM_BORDER
  static readonly RECT_EDGE_COLOR_PINNED_ALT = 0xb34a24;
  static readonly LABEL_LINE_COLOR = 0x808080;
  static readonly OPACITY_REGULAR = 0.75;
  static readonly OPACITY_OVERSIZED = 0.25;
  static readonly TRANSPARENT_MATERIAL = new THREE.MeshBasicMaterial({
    opacity: 0,
    transparent: true,
  });
  private static readonly RECT_EDGE_BOLD_WIDTH = 10;
  private static readonly FILL_REGION_NAME = 'fillRegion';

  renderer = new THREE.WebGLRenderer({
    antialias: true,
    canvas: this.canvasRects,
    alpha: true,
  });
  labelRenderer?: CSS2DRenderer;

  private camera = new THREE.OrthographicCamera(
    -Canvas.TARGET_SCENE_DIAGONAL / 2,
    Canvas.TARGET_SCENE_DIAGONAL / 2,
    Canvas.TARGET_SCENE_DIAGONAL / 2,
    -Canvas.TARGET_SCENE_DIAGONAL / 2,
    0,
    100,
  );
  private scene = new THREE.Scene();
  private pinnedIdToColorMap = new Map<string, number>();
  private lastAssignedDefaultPinnedColor = false;
  private firstDraw = true;
  private lastScene: SceneState = {
    isDarkMode: this.isDarkMode(),
    translatedPos: undefined,
    rectIdToRectGraphics: new Map<string, RectGraphics>(),
    rectIdToLabelGraphics: new Map<string, LabelGraphics>(),
  };

  constructor(
    private canvasRects: HTMLElement,
    private canvasLabels?: HTMLElement,
    private isDarkMode = () => false,
  ) {
    if (this.canvasLabels) {
      this.labelRenderer = new CSS2DRenderer({element: this.canvasLabels});
    }
  }

  updateViewPosition(camera: Camera, bounds: Box3D) {
    // Must set 100% width and height so the HTML element expands to the parent's
    // boundaries and the correct clientWidth and clientHeight values can be read
    this.canvasRects.style.width = '100%';
    this.canvasRects.style.height = '100%';
    const [maxWidth, maxHeight] = [
      this.canvasRects.clientWidth,
      this.canvasRects.clientHeight,
    ];
    if (maxWidth === 0 || maxHeight === 0) {
      return;
    }

    let widthAspectRatioAdjustFactor = 1;
    let heightAspectRatioAdjustFactor = 1;
    if (maxWidth > maxHeight) {
      widthAspectRatioAdjustFactor = maxWidth / maxHeight;
    } else {
      heightAspectRatioAdjustFactor = maxHeight / maxWidth;
    }
    const cameraWidth =
      Canvas.TARGET_SCENE_DIAGONAL * widthAspectRatioAdjustFactor;
    const cameraHeight =
      Canvas.TARGET_SCENE_DIAGONAL * heightAspectRatioAdjustFactor;

    const panFactorX = camera.panScreenDistance.dx / maxWidth;
    const panFactorY = camera.panScreenDistance.dy / maxHeight;

    const scaleFactor =
      (Canvas.TARGET_SCENE_DIAGONAL / bounds.diagonal) * camera.zoomFactor;
    this.scene.scale.set(scaleFactor, -scaleFactor, scaleFactor);

    const translatedPos = new Point3D(
      scaleFactor * -(bounds.depth * camera.rotationAngleX + bounds.center.x) +
        cameraWidth * panFactorX,
      scaleFactor *
        ((-bounds.depth * camera.rotationAngleY ** 2) / 2 + bounds.center.y) -
        cameraHeight * panFactorY,
      scaleFactor * -bounds.depth,
    );
    this.scene
      .translateX(translatedPos.x - (this.lastScene.translatedPos?.x ?? 0))
      .translateY(translatedPos.y - (this.lastScene.translatedPos?.y ?? 0))
      .translateZ(translatedPos.z - (this.lastScene.translatedPos?.z ?? 0));
    this.lastScene.translatedPos = translatedPos;

    this.camera.left = -cameraWidth / 2;
    this.camera.right = cameraWidth / 2;
    this.camera.top = cameraHeight / 2;
    this.camera.bottom = -cameraHeight / 2;
    const cPos = new THREE.Vector3(0, 0, Canvas.TARGET_SCENE_DIAGONAL)
      .applyAxisAngle(new THREE.Vector3(1, 0, 0), -camera.rotationAngleX)
      .applyAxisAngle(new THREE.Vector3(0, 1, 0), camera.rotationAngleY);
    this.camera.position.set(cPos.x, cPos.y, cPos.z);
    this.camera.lookAt(0, 0, 0);
    this.camera.updateProjectionMatrix();

    this.renderer.setSize(maxWidth, maxHeight);
    this.labelRenderer?.setSize(maxWidth, maxHeight);
  }

  updateRects(rects: UiRect3D[]) {
    for (const key of this.lastScene.rectIdToRectGraphics.keys()) {
      if (!rects.some((rect) => rect.id === key)) {
        this.lastScene.rectIdToRectGraphics.delete(key);
        this.scene.remove(assertDefined(this.scene.getObjectByName(key)));
      }
    }
    rects.forEach((rect) => {
      const existingGraphics = this.lastScene.rectIdToRectGraphics.get(rect.id);
      const mesh = !existingGraphics
        ? this.makeAndAddRectMesh(rect)
        : this.updateExistingRectMesh(
            rect,
            existingGraphics.rect,
            existingGraphics.mesh,
          );
      this.lastScene.rectIdToRectGraphics.set(rect.id, {rect, mesh});
    });
  }

  updateLabels(labels: RectLabel[]) {
    if (this.labelRenderer) {
      this.updateLabelGraphics(labels);
    }
  }

  renderView(): [THREE.Scene, THREE.OrthographicCamera] {
    this.labelRenderer?.render(this.scene, this.camera);
    this.renderer.setPixelRatio(window.devicePixelRatio);
    if (this.firstDraw) {
      this.renderer.compile(this.scene, this.camera);
      this.firstDraw = false;
    }
    this.renderer.render(this.scene, this.camera);
    this.lastScene.isDarkMode = this.isDarkMode();
    return [this.scene, this.camera];
  }

  getClickedRectId(x: number, y: number, z: number): undefined | string {
    const clickPosition = new THREE.Vector3(x, y, z);
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(clickPosition, assertDefined(this.camera));
    const intersected = raycaster.intersectObjects(
      Array.from(this.lastScene.rectIdToRectGraphics.values())
        .filter((graphics) => graphics.rect.isClickable)
        .map((graphics) => graphics.mesh),
    );
    return intersected.at(0)?.object.name;
  }

  private toMatrix4(transform: TransformMatrix): THREE.Matrix4 {
    return new THREE.Matrix4().set(
      transform.dsdx,
      transform.dtdx,
      0,
      transform.tx,
      transform.dtdy,
      transform.dsdy,
      0,
      transform.ty,
      0,
      0,
      1,
      0,
      0,
      0,
      0,
      1,
    );
  }

  private makeAndAddRectMesh(rect: UiRect3D): THREE.Mesh {
    const color = this.getColor(rect);
    const fillMaterial = this.getFillMaterial(rect, color);
    const mesh = new THREE.Mesh(
      this.makeRoundedRectGeometry(rect),
      rect.fillRegion ? Canvas.TRANSPARENT_MATERIAL : fillMaterial,
    );

    if (rect.fillRegion) {
      this.addFillRegionMesh(rect, fillMaterial, mesh);
    }
    this.addRectBorders(rect, mesh);

    mesh.position.x = 0;
    mesh.position.y = 0;
    mesh.position.z = rect.topLeft.z;
    mesh.name = rect.id;
    mesh.applyMatrix4(this.toMatrix4(rect.transform));
    this.scene.add(mesh);
    return mesh;
  }

  private makeRoundedRectGeometry(rect: UiRect3D): THREE.ShapeGeometry {
    const bottomLeft = new Point3D(
      rect.topLeft.x,
      rect.bottomRight.y,
      rect.topLeft.z,
    );
    const topRight = new Point3D(
      rect.bottomRight.x,
      rect.topLeft.y,
      rect.bottomRight.z,
    );
    const cornerRadius = this.getAdjustedCornerRadius(rect);

    // Create (rounded) rect shape
    const shape = new THREE.Shape()
      .moveTo(rect.topLeft.x, rect.topLeft.y + cornerRadius)
      .lineTo(bottomLeft.x, bottomLeft.y - cornerRadius)
      .quadraticCurveTo(
        bottomLeft.x,
        bottomLeft.y,
        bottomLeft.x + cornerRadius,
        bottomLeft.y,
      )
      .lineTo(rect.bottomRight.x - cornerRadius, rect.bottomRight.y)
      .quadraticCurveTo(
        rect.bottomRight.x,
        rect.bottomRight.y,
        rect.bottomRight.x,
        rect.bottomRight.y - cornerRadius,
      )
      .lineTo(topRight.x, topRight.y + cornerRadius)
      .quadraticCurveTo(
        topRight.x,
        topRight.y,
        topRight.x - cornerRadius,
        topRight.y,
      )
      .lineTo(rect.topLeft.x + cornerRadius, rect.topLeft.y)
      .quadraticCurveTo(
        rect.topLeft.x,
        rect.topLeft.y,
        rect.topLeft.x,
        rect.topLeft.y + cornerRadius,
      );
    return new THREE.ShapeGeometry(shape);
  }

  private makeRectShape(topLeft: Point3D, bottomRight: Point3D): THREE.Shape {
    const bottomLeft = new Point3D(topLeft.x, bottomRight.y, topLeft.z);
    const topRight = new Point3D(bottomRight.x, topLeft.y, bottomRight.z);

    // Create rect shape
    return new THREE.Shape()
      .moveTo(topLeft.x, topLeft.y)
      .lineTo(bottomLeft.x, bottomLeft.y)
      .lineTo(bottomRight.x, bottomRight.y)
      .lineTo(topRight.x, topRight.y)
      .lineTo(topLeft.x, topLeft.y);
  }

  private getVisibleRectColor(darkFactor: number) {
    const red = ((200 - 45) * darkFactor + 45) / 255;
    const green = ((232 - 182) * darkFactor + 182) / 255;
    const blue = ((183 - 44) * darkFactor + 44) / 255;
    return new THREE.Color(red, green, blue);
  }

  private getColor(rect: UiRect3D): THREE.Color | undefined {
    switch (rect.colorType) {
      case ColorType.VISIBLE: {
        // green (darkness depends on z order)
        return this.getVisibleRectColor(rect.darkFactor);
      }
      case ColorType.VISIBLE_WITH_OPACITY: {
        // same green for all rects - rect.darkFactor determines opacity
        return this.getVisibleRectColor(0.7);
      }
      case ColorType.NOT_VISIBLE: {
        // gray (darkness depends on z order)
        const lower = 120;
        const upper = 220;
        const darkness = ((upper - lower) * rect.darkFactor + lower) / 255;
        return new THREE.Color(darkness, darkness, darkness);
      }
      case ColorType.HIGHLIGHTED: {
        return this.isDarkMode()
          ? Canvas.RECT_COLOR_HIGHLIGHTED_DARK_MODE
          : Canvas.RECT_COLOR_HIGHLIGHTED_LIGHT_MODE;
      }
      case ColorType.HAS_CONTENT_AND_OPACITY: {
        return Canvas.RECT_COLOR_HAS_CONTENT;
      }
      case ColorType.HAS_CONTENT: {
        return Canvas.RECT_COLOR_HAS_CONTENT;
      }
      case ColorType.EMPTY: {
        return undefined;
      }
      default: {
        assertUnreachable(rect.colorType);
      }
    }
  }

  private makeRectBorders(
    rect: UiRect3D,
    rectGeometry: THREE.ShapeGeometry,
  ): THREE.LineSegments {
    // create line edges for rect
    const edgeGeo = new THREE.EdgesGeometry(rectGeometry);
    let color: number;
    if (rect.cornerRadius) {
      color = Canvas.RECT_EDGE_COLOR_ROUNDED;
    } else {
      color = this.isDarkMode()
        ? Canvas.RECT_EDGE_COLOR_DARK_MODE
        : Canvas.RECT_EDGE_COLOR_LIGHT_MODE;
    }
    const edgeMaterial = new THREE.LineBasicMaterial({color});
    const lineSegments = new THREE.LineSegments(edgeGeo, edgeMaterial);
    lineSegments.computeLineDistances();
    return lineSegments;
  }

  private getAdjustedCornerRadius(rect: UiRect3D): number {
    // Limit corner radius if larger than height/2 (or width/2)
    const height = rect.bottomRight.y - rect.topLeft.y;
    const width = rect.bottomRight.x - rect.topLeft.x;
    const minEdge = Math.min(height, width);
    const cornerRadius = Math.min(rect.cornerRadius, minEdge / 2);

    // Force radius > 0, because radius === 0 could result in weird triangular shapes
    // being drawn instead of rectangles. Seems like quadraticCurveTo() doesn't
    // always handle properly the case with radius === 0.
    return Math.max(cornerRadius, 0.01);
  }

  private makePinnedRectBorders(rect: UiRect3D): THREE.Mesh {
    const pinnedBorders = this.createPinnedBorderRects(rect);
    let color = this.pinnedIdToColorMap.get(rect.id);
    if (color === undefined) {
      color = this.lastAssignedDefaultPinnedColor
        ? Canvas.RECT_EDGE_COLOR_PINNED_ALT
        : Canvas.RECT_EDGE_COLOR_PINNED;
      this.pinnedIdToColorMap.set(rect.id, color);
      this.lastAssignedDefaultPinnedColor =
        !this.lastAssignedDefaultPinnedColor;
    }
    const pinnedBorderMesh = new THREE.Mesh(
      new THREE.ShapeGeometry(pinnedBorders),
      new THREE.MeshBasicMaterial({color}),
    );
    // Prevent z-fighting with the parent mesh
    pinnedBorderMesh.position.z = 2;
    return pinnedBorderMesh;
  }

  private createPinnedBorderRects(rect: UiRect3D): THREE.Shape[] {
    const cornerRadius = this.getAdjustedCornerRadius(rect);
    const xBoldWidth = Canvas.RECT_EDGE_BOLD_WIDTH / rect.transform.dsdx;
    const yBorderWidth = Canvas.RECT_EDGE_BOLD_WIDTH / rect.transform.dsdy;
    const borderRects = [
      // left and bottom borders
      new THREE.Shape()
        .moveTo(rect.topLeft.x, rect.topLeft.y + cornerRadius)
        .lineTo(rect.topLeft.x, rect.bottomRight.y - cornerRadius)
        .quadraticCurveTo(
          rect.topLeft.x,
          rect.bottomRight.y,
          rect.topLeft.x + cornerRadius,
          rect.bottomRight.y,
        )
        .lineTo(rect.bottomRight.x - cornerRadius, rect.bottomRight.y)
        .quadraticCurveTo(
          rect.bottomRight.x,
          rect.bottomRight.y,
          rect.bottomRight.x,
          rect.bottomRight.y - cornerRadius,
        )
        .lineTo(
          rect.bottomRight.x - xBoldWidth,
          rect.bottomRight.y - cornerRadius,
        )
        .quadraticCurveTo(
          rect.bottomRight.x - xBoldWidth,
          rect.bottomRight.y - yBorderWidth,
          rect.bottomRight.x - cornerRadius,
          rect.bottomRight.y - yBorderWidth,
        )
        .lineTo(
          rect.topLeft.x + cornerRadius,
          rect.bottomRight.y - yBorderWidth,
        )
        .quadraticCurveTo(
          rect.topLeft.x + xBoldWidth,
          rect.bottomRight.y - yBorderWidth,
          rect.topLeft.x + xBoldWidth,
          rect.bottomRight.y - cornerRadius,
        )
        .lineTo(rect.topLeft.x + xBoldWidth, rect.topLeft.y + cornerRadius)
        .lineTo(rect.topLeft.x, rect.topLeft.y + cornerRadius),

      // right and top borders
      new THREE.Shape()
        .moveTo(rect.bottomRight.x, rect.bottomRight.y - cornerRadius)
        .lineTo(rect.bottomRight.x, rect.topLeft.y + cornerRadius)
        .quadraticCurveTo(
          rect.bottomRight.x,
          rect.topLeft.y,
          rect.bottomRight.x - cornerRadius,
          rect.topLeft.y,
        )
        .lineTo(rect.topLeft.x + cornerRadius, rect.topLeft.y)
        .quadraticCurveTo(
          rect.topLeft.x,
          rect.topLeft.y,
          rect.topLeft.x,
          rect.topLeft.y + cornerRadius,
        )
        .lineTo(rect.topLeft.x + xBoldWidth, rect.topLeft.y + cornerRadius)
        .quadraticCurveTo(
          rect.topLeft.x + xBoldWidth,
          rect.topLeft.y + yBorderWidth,
          rect.topLeft.x + cornerRadius,
          rect.topLeft.y + yBorderWidth,
        )
        .lineTo(
          rect.bottomRight.x - cornerRadius,
          rect.topLeft.y + yBorderWidth,
        )
        .quadraticCurveTo(
          rect.bottomRight.x - xBoldWidth,
          rect.topLeft.y + yBorderWidth,
          rect.bottomRight.x - xBoldWidth,
          rect.topLeft.y + cornerRadius,
        )
        .lineTo(
          rect.bottomRight.x - xBoldWidth,
          rect.bottomRight.y - cornerRadius,
        )
        .lineTo(rect.bottomRight.x, rect.bottomRight.y - cornerRadius),
    ];
    return borderRects;
  }

  private getFillMaterial(
    rect: UiRect3D,
    color: THREE.Color | undefined,
  ): THREE.MeshBasicMaterial {
    if (color !== undefined) {
      let opacity: number | undefined;
      if (
        rect.colorType === ColorType.VISIBLE_WITH_OPACITY ||
        rect.colorType === ColorType.HAS_CONTENT_AND_OPACITY
      ) {
        opacity = rect.darkFactor;
      } else {
        opacity = rect.isOversized
          ? Canvas.OPACITY_OVERSIZED
          : Canvas.OPACITY_REGULAR;
      }
      return new THREE.MeshBasicMaterial({
        color,
        opacity,
        transparent: true,
      });
    }
    return Canvas.TRANSPARENT_MATERIAL;
  }

  private addFillRegionMesh(
    rect: UiRect3D,
    fillMaterial: THREE.MeshBasicMaterial,
    mesh: THREE.Mesh,
  ) {
    const fillShapes = assertDefined(rect.fillRegion).map((fillRect) =>
      this.makeRectShape(fillRect.topLeft, fillRect.bottomRight),
    );
    const fillMesh = new THREE.Mesh(
      new THREE.ShapeGeometry(fillShapes),
      fillMaterial,
    );
    // Prevent z-fighting with the parent mesh
    fillMesh.position.z = 1;
    fillMesh.name = rect.id + Canvas.FILL_REGION_NAME;
    mesh.add(fillMesh);
  }

  private updateExistingRectMesh(
    newRect: UiRect3D,
    existingRect: UiRect3D,
    existingMesh: THREE.Mesh,
  ): THREE.Mesh {
    this.updateRectMeshFillMaterial(newRect, existingRect, existingMesh);
    this.updateRectMeshGeometry(newRect, existingRect, existingMesh);
    return existingMesh;
  }

  private updateRectMeshFillMaterial(
    newRect: UiRect3D,
    existingRect: UiRect3D,
    existingMesh: THREE.Mesh,
  ) {
    const fillMaterial = this.getFillMaterial(newRect, this.getColor(newRect));
    const fillChanged =
      newRect.colorType !== existingRect.colorType ||
      this.lastScene.isDarkMode !== this.isDarkMode() ||
      newRect.darkFactor !== existingRect.darkFactor ||
      newRect.isOversized !== existingRect.isOversized;

    if (!newRect.fillRegion && existingRect.fillRegion) {
      existingMesh.material = fillMaterial;
      existingMesh.remove(
        assertDefined(
          existingMesh.getObjectByName(
            existingRect.id + Canvas.FILL_REGION_NAME,
          ),
        ),
      );
    } else if (newRect.fillRegion && !existingRect.fillRegion) {
      existingMesh.material = Canvas.TRANSPARENT_MATERIAL;
      this.addFillRegionMesh(newRect, fillMaterial, existingMesh);
    } else if (newRect.fillRegion && existingRect.fillRegion) {
      const fillRegionChanged = !ArrayUtils.equal(
        newRect.fillRegion,
        existingRect.fillRegion,
        (a, b) => {
          const [r, o] = [a as Rect3D, b as Rect3D];
          return (
            r.topLeft.isEqual(o.topLeft) && r.bottomRight.isEqual(o.bottomRight)
          );
        },
      );
      if (fillRegionChanged) {
        existingMesh.remove(
          assertDefined(
            existingMesh.getObjectByName(
              existingRect.id + Canvas.FILL_REGION_NAME,
            ),
          ),
        );
        this.addFillRegionMesh(newRect, fillMaterial, existingMesh);
      }
    }

    if (fillChanged) {
      if (newRect.fillRegion === undefined) {
        existingMesh.material = fillMaterial;
      } else {
        const fillMesh = assertDefined(
          existingMesh.getObjectByName(
            existingRect.id + Canvas.FILL_REGION_NAME,
          ),
        ) as THREE.Mesh;
        fillMesh.material = fillMaterial;
      }
    }
  }

  private updateRectMeshGeometry(
    newRect: UiRect3D,
    existingRect: UiRect3D,
    existingMesh: THREE.Mesh,
  ) {
    const isGeometryChanged =
      !newRect.bottomRight.isEqual(existingRect.bottomRight) ||
      !newRect.topLeft.isEqual(existingRect.topLeft) ||
      newRect.cornerRadius !== existingRect.cornerRadius;

    if (isGeometryChanged) {
      existingMesh.geometry = this.makeRoundedRectGeometry(newRect);
      existingMesh.position.z = newRect.topLeft.z;
    }

    const isColorChanged =
      this.isDarkMode() !== this.lastScene.isDarkMode ||
      newRect.isPinned !== existingRect.isPinned;
    if (isGeometryChanged || isColorChanged) {
      existingMesh.remove(
        assertDefined(existingMesh.getObjectByName(existingRect.id + 'border')),
      );
      this.addRectBorders(newRect, existingMesh);
    }

    if (!newRect.transform.isEqual(existingRect.transform)) {
      existingMesh.applyMatrix4(
        this.toMatrix4(existingRect.transform.inverse()),
      );
      existingMesh.applyMatrix4(this.toMatrix4(newRect.transform));
    }
  }

  private addRectBorders(newRect: UiRect3D, mesh: THREE.Mesh) {
    let borderMesh: THREE.Object3D;
    if (newRect.isPinned) {
      borderMesh = this.makePinnedRectBorders(newRect);
    } else {
      borderMesh = this.makeRectBorders(newRect, mesh.geometry);
    }
    borderMesh.name = newRect.id + 'border';
    mesh.add(borderMesh);
  }

  private updateLabelGraphics(labels: RectLabel[]) {
    this.clearLabels(labels);
    labels.forEach((label) => {
      let graphics: LabelGraphics;
      if (this.lastScene.rectIdToLabelGraphics.get(label.rectId)) {
        graphics = this.updateExistingLabelGraphics(label);
      } else {
        const circle = this.makeLabelCircleMesh(label);
        this.scene.add(circle);
        const line = this.makeLabelLine(label);
        this.scene.add(line);
        const text = this.makeLabelCssObject(label);
        this.scene.add(text);
        graphics = {label, circle, line, text};
      }
      this.lastScene.rectIdToLabelGraphics.set(label.rectId, graphics);
    });
  }

  private makeLabelCircleMesh(label: RectLabel): THREE.Mesh {
    const geometry = new THREE.CircleGeometry(label.circle.radius, 20);
    const material = this.makeLabelMaterial(label);
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.set(
      label.circle.center.x,
      label.circle.center.y,
      label.circle.center.z,
    );
    mesh.name = label.rectId + 'circle';
    return mesh;
  }

  private makeLabelLine(label: RectLabel): THREE.Line {
    const lineGeometry = this.makeLabelLineGeometry(label);
    const lineMaterial = this.makeLabelMaterial(label);
    const line = new THREE.Line(lineGeometry, lineMaterial);
    line.name = label.rectId + 'line';
    return line;
  }

  private makeLabelLineGeometry(label: RectLabel): THREE.BufferGeometry {
    const linePoints = label.linePoints.map((point: Point3D) => {
      return new THREE.Vector3(point.x, point.y, point.z);
    });
    return new THREE.BufferGeometry().setFromPoints(linePoints);
  }

  private makeLabelMaterial(label: RectLabel): THREE.LineBasicMaterial {
    return new THREE.LineBasicMaterial({
      color: label.isHighlighted
        ? this.isDarkMode()
          ? Canvas.RECT_EDGE_COLOR_DARK_MODE
          : Canvas.RECT_EDGE_COLOR_LIGHT_MODE
        : Canvas.LABEL_LINE_COLOR,
    });
  }

  private makeLabelCssObject(label: RectLabel): CSS2DObject {
    // Add rectangle label
    const spanText: HTMLElement = document.createElement('span');
    spanText.innerText = label.text;
    spanText.className = 'mat-body-1';
    spanText.style.backgroundColor = 'var(--background-color)';

    // Hack: transparent/placeholder text used to push the visible text towards left
    // (towards negative x) and properly align it with the label's vertical segment
    const spanPlaceholder: HTMLElement = document.createElement('span');
    spanPlaceholder.innerText = label.text;
    spanPlaceholder.className = 'mat-body-1';
    spanPlaceholder.style.opacity = '0';

    const div: HTMLElement = document.createElement('div');
    div.className = 'rect-label';
    div.style.display = 'inline';
    div.style.whiteSpace = 'nowrap';
    div.appendChild(spanText);
    div.appendChild(spanPlaceholder);

    div.style.marginTop = '5px';
    if (!label.isHighlighted) {
      div.style.color = 'gray';
    }
    div.style.pointerEvents = 'auto';
    div.style.cursor = 'pointer';
    div.addEventListener('click', (event) =>
      this.propagateUpdateHighlightedItem(event, label.rectId),
    );

    const labelCss = new CSS2DObject(div);
    labelCss.position.set(
      label.textCenter.x,
      label.textCenter.y,
      label.textCenter.z,
    );
    labelCss.name = label.rectId + 'text';
    return labelCss;
  }

  private updateExistingLabelGraphics(newLabel: RectLabel): LabelGraphics {
    const {
      label: existingLabel,
      circle,
      line,
      text,
    } = assertDefined(
      this.lastScene.rectIdToLabelGraphics.get(newLabel.rectId),
    );

    if (newLabel.circle.radius !== existingLabel.circle.radius) {
      circle.geometry = new THREE.CircleGeometry(newLabel.circle.radius, 20);
    }
    if (!newLabel.circle.center.isEqual(existingLabel.circle.center)) {
      circle.position.set(
        newLabel.circle.center.x,
        newLabel.circle.center.y,
        newLabel.circle.center.z,
      );
    }

    if (
      newLabel.isHighlighted !== existingLabel.isHighlighted ||
      this.isDarkMode() !== this.lastScene.isDarkMode
    ) {
      const lineMaterial = this.makeLabelMaterial(newLabel);
      circle.material = lineMaterial;
      line.material = lineMaterial;
      text.element.style.color = newLabel.isHighlighted ? '' : 'gray';
    }

    if (
      !ArrayUtils.equal(newLabel.linePoints, existingLabel.linePoints, (a, b) =>
        (a as Point3D).isEqual(b as Point3D),
      )
    ) {
      line.geometry = this.makeLabelLineGeometry(newLabel);
    }

    if (!newLabel.textCenter.isEqual(existingLabel.textCenter)) {
      text.position.set(
        newLabel.textCenter.x,
        newLabel.textCenter.y,
        newLabel.textCenter.z,
      );
    }

    return {label: newLabel, circle, line, text};
  }

  private propagateUpdateHighlightedItem(event: MouseEvent, newId: string) {
    event.preventDefault();
    const highlightedChangeEvent = new CustomEvent(
      ViewerEvents.HighlightedIdChange,
      {
        bubbles: true,
        detail: {id: newId},
      },
    );
    event.target?.dispatchEvent(highlightedChangeEvent);
  }

  private clearLabels(labels: RectLabel[]) {
    if (this.canvasLabels) {
      this.canvasLabels.innerHTML = '';
    }
    for (const [rectId, graphics] of this.lastScene.rectIdToLabelGraphics) {
      if (!labels.some((label) => label.rectId === rectId)) {
        this.scene.remove(graphics.circle);
        this.scene.remove(graphics.line);
        this.scene.remove(graphics.text);
        this.lastScene.rectIdToLabelGraphics.delete(rectId);
      }
    }
  }
}

interface SceneState {
  isDarkMode: boolean;
  translatedPos?: Point3D | undefined;
  rectIdToRectGraphics: Map<string, RectGraphics>;
  rectIdToLabelGraphics: Map<string, LabelGraphics>;
}

interface RectGraphics {
  rect: UiRect3D;
  mesh: THREE.Mesh;
}

interface LabelGraphics {
  label: RectLabel;
  circle: THREE.Mesh;
  line: THREE.Line;
  text: CSS2DObject;
}
