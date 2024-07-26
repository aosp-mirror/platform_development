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
import {assertDefined} from 'common/assert_utils';
import {TransformMatrix} from 'common/geometry_types';
import * as THREE from 'three';
import {
  CSS2DObject,
  CSS2DRenderer,
} from 'three/examples/jsm/renderers/CSS2DRenderer';
import {ViewerEvents} from 'viewers/common/viewer_events';
import {
  Circle3D,
  ColorType,
  Label3D,
  Point3D,
  Rect3D,
  Scene3D,
} from './types3d';

export class Canvas {
  static readonly TARGET_SCENE_DIAGONAL = 4;
  private static readonly RECT_COLOR_HIGHLIGHTED_LIGHT_MODE = new THREE.Color(
    0xd2e3fc, // Keep in sync with :not(.dark-mode) --selected-element-color in material-theme.scss
  );
  private static readonly RECT_COLOR_HIGHLIGHTED_DARK_MODE = new THREE.Color(
    0x5f718a, // Keep in sync with .dark-mode --selected-element-color in material-theme.scss
  );
  private static readonly RECT_COLOR_HAS_CONTENT = new THREE.Color(0xad42f5);

  private static readonly RECT_EDGE_COLOR_LIGHT_MODE = 0x000000;
  private static readonly RECT_EDGE_COLOR_DARK_MODE = 0xffffff;
  private static readonly RECT_EDGE_COLOR_ROUNDED = 0x848884;

  private static readonly LABEL_LINE_COLOR = 0x808080;

  private static readonly OPACITY_REGULAR = 0.75;
  private static readonly OPACITY_OVERSIZED = 0.25;

  private camera?: THREE.OrthographicCamera;
  private scene?: THREE.Scene;
  private renderer?: THREE.WebGLRenderer;
  private labelRenderer?: CSS2DRenderer;
  private clickableObjects: THREE.Object3D[] = [];

  constructor(
    private canvasRects: HTMLCanvasElement,
    private canvasLabels?: HTMLElement,
    private isDarkMode = () => false,
  ) {}

  draw(scene: Scene3D) {
    // Must set 100% width and height so the HTML element expands to the parent's
    // boundaries and the correct clientWidth and clientHeight values can be read
    this.canvasRects.style.width = '100%';
    this.canvasRects.style.height = '100%';
    let widthAspectRatioAdjustFactor: number;
    let heightAspectRatioAdjustFactor: number;

    if (this.canvasRects.clientWidth > this.canvasRects.clientHeight) {
      heightAspectRatioAdjustFactor = 1;
      widthAspectRatioAdjustFactor =
        this.canvasRects.clientWidth / this.canvasRects.clientHeight;
    } else {
      heightAspectRatioAdjustFactor =
        this.canvasRects.clientHeight / this.canvasRects.clientWidth;
      widthAspectRatioAdjustFactor = 1;
    }

    const cameraWidth =
      Canvas.TARGET_SCENE_DIAGONAL * widthAspectRatioAdjustFactor;
    const cameraHeight =
      Canvas.TARGET_SCENE_DIAGONAL * heightAspectRatioAdjustFactor;

    const panFactorX =
      scene.camera.panScreenDistance.dx / this.canvasRects.clientWidth;
    const panFactorY =
      scene.camera.panScreenDistance.dy / this.canvasRects.clientHeight;

    this.scene = new THREE.Scene();
    const scaleFactor =
      (Canvas.TARGET_SCENE_DIAGONAL / scene.boundingBox.diagonal) *
      scene.camera.zoomFactor;
    this.scene.scale.set(scaleFactor, -scaleFactor, scaleFactor);
    this.scene.translateX(
      scaleFactor * -scene.boundingBox.center.x + cameraWidth * panFactorX,
    );
    this.scene.translateY(
      scaleFactor * scene.boundingBox.center.y - cameraHeight * panFactorY,
    );
    this.scene.translateZ(scaleFactor * -scene.boundingBox.center.z);

    this.camera = new THREE.OrthographicCamera(
      -cameraWidth / 2,
      cameraWidth / 2,
      cameraHeight / 2,
      -cameraHeight / 2,
      0,
      100,
    );

    const cameraPosition = new THREE.Vector3(
      0,
      0,
      Canvas.TARGET_SCENE_DIAGONAL,
    );
    cameraPosition.applyAxisAngle(
      new THREE.Vector3(1, 0, 0),
      -scene.camera.rotationAngleX,
    );
    cameraPosition.applyAxisAngle(
      new THREE.Vector3(0, 1, 0),
      scene.camera.rotationAngleY,
    );

    this.camera.position.set(
      cameraPosition.x,
      cameraPosition.y,
      cameraPosition.z,
    );
    this.camera.lookAt(0, 0, 0);

    this.renderer = new THREE.WebGLRenderer({
      antialias: true,
      canvas: this.canvasRects,
      alpha: true,
    });

    // set various factors for shading and shifting
    this.drawRects(scene.rects);
    const canvasRects = assertDefined(this.canvasRects);
    if (this.canvasLabels) {
      this.drawLabels(scene.labels, this.isDarkMode());

      this.labelRenderer = new CSS2DRenderer({element: this.canvasLabels});
      this.labelRenderer.setSize(
        canvasRects.clientWidth,
        canvasRects.clientHeight,
      );
      this.labelRenderer.render(this.scene, this.camera);
    }

    this.renderer.setSize(canvasRects.clientWidth, canvasRects.clientHeight);
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.compile(this.scene, this.camera);
    this.renderer.render(this.scene, this.camera);
  }

  getClickedRectId(x: number, y: number, z: number): undefined | string {
    const clickPosition = new THREE.Vector3(x, y, z);
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(clickPosition, assertDefined(this.camera));
    const intersected = raycaster.intersectObjects(this.clickableObjects);
    if (intersected.length > 0) {
      return intersected[0].object.name;
    }
    return undefined;
  }

  private drawRects(rects: Rect3D[]) {
    this.clickableObjects = [];
    rects.forEach((rect) => {
      const rectMesh = Canvas.makeRectMesh(rect, this.isDarkMode());
      const transform = Canvas.toMatrix4(rect.transform);
      rectMesh.applyMatrix4(transform);

      this.scene?.add(rectMesh);

      if (rect.isClickable) {
        this.clickableObjects.push(rectMesh);
      }
    });
  }

  private drawLabels(labels: Label3D[], isDarkMode: boolean) {
    this.clearLabels();
    labels.forEach((label) => {
      const circleMesh = this.makeLabelCircleMesh(label.circle, isDarkMode);
      this.scene?.add(circleMesh);

      const linePoints = label.linePoints.map((point: Point3D) => {
        return new THREE.Vector3(point.x, point.y, point.z);
      });
      const lineGeometry = new THREE.BufferGeometry().setFromPoints(linePoints);
      const lineMaterial = new THREE.LineBasicMaterial({
        color: label.isHighlighted
          ? isDarkMode
            ? Canvas.RECT_EDGE_COLOR_DARK_MODE
            : Canvas.RECT_EDGE_COLOR_LIGHT_MODE
          : Canvas.LABEL_LINE_COLOR,
      });
      const line = new THREE.Line(lineGeometry, lineMaterial);
      this.scene?.add(line);

      this.drawLabelTextHtml(label);
    });
  }

  private drawLabelTextHtml(label: Label3D) {
    // Add rectangle label
    const spanText: HTMLElement = document.createElement('span');
    spanText.innerText = label.text;
    spanText.className = 'mat-body-1';

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

    this.scene?.add(labelCss);
  }

  private static toMatrix4(transform: TransformMatrix): THREE.Matrix4 {
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

  private static makeRectMesh(rect: Rect3D, isDarkMode: boolean): THREE.Mesh {
    const rectShape = Canvas.createRectShape(rect);
    const rectGeometry = new THREE.ShapeGeometry(rectShape);
    const rectBorders = Canvas.createRectBorders(
      rect,
      rectGeometry,
      isDarkMode,
    );

    const color = Canvas.getColor(rect, isDarkMode);
    let mesh: THREE.Mesh | undefined;
    if (color === undefined) {
      mesh = new THREE.Mesh(
        rectGeometry,
        new THREE.MeshBasicMaterial({
          opacity: 0,
          transparent: true,
        }),
      );
    } else {
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
      mesh = new THREE.Mesh(
        rectGeometry,
        new THREE.MeshBasicMaterial({
          color,
          opacity,
          transparent: true,
        }),
      );
    }

    mesh.add(rectBorders);
    mesh.position.x = 0;
    mesh.position.y = 0;
    mesh.position.z = rect.topLeft.z;
    mesh.name = rect.id;

    return mesh;
  }

  private static createRectShape(rect: Rect3D): THREE.Shape {
    const bottomLeft: Point3D = {
      x: rect.topLeft.x,
      y: rect.bottomRight.y,
      z: rect.topLeft.z,
    };
    const topRight: Point3D = {
      x: rect.bottomRight.x,
      y: rect.topLeft.y,
      z: rect.bottomRight.z,
    };

    // Limit corner radius if larger than height/2 (or width/2)
    const height = rect.bottomRight.y - rect.topLeft.y;
    const width = rect.bottomRight.x - rect.topLeft.x;
    const minEdge = Math.min(height, width);
    let cornerRadius = Math.min(rect.cornerRadius, minEdge / 2);

    // Force radius > 0, because radius === 0 could result in weird triangular shapes
    // being drawn instead of rectangles. Seems like quadraticCurveTo() doesn't
    // always handle properly the case with radius === 0.
    cornerRadius = Math.max(cornerRadius, 0.01);

    // Create (rounded) rect shape
    return new THREE.Shape()
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
  }

  private static getVisibleRectColor(darkFactor: number) {
    const red = ((200 - 45) * darkFactor + 45) / 255;
    const green = ((232 - 182) * darkFactor + 182) / 255;
    const blue = ((183 - 44) * darkFactor + 44) / 255;
    return new THREE.Color(red, green, blue);
  }

  private static getColor(
    rect: Rect3D,
    isDarkMode: boolean,
  ): THREE.Color | undefined {
    switch (rect.colorType) {
      case ColorType.VISIBLE: {
        // green (darkness depends on z order)
        return Canvas.getVisibleRectColor(rect.darkFactor);
      }
      case ColorType.VISIBLE_WITH_OPACITY: {
        // same green for all rects - rect.darkFactor determines opacity
        return Canvas.getVisibleRectColor(0.7);
      }
      case ColorType.NOT_VISIBLE: {
        // gray (darkness depends on z order)
        const lower = 120;
        const upper = 220;
        const darkness = ((upper - lower) * rect.darkFactor + lower) / 255;
        return new THREE.Color(darkness, darkness, darkness);
      }
      case ColorType.HIGHLIGHTED: {
        return isDarkMode
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
        throw new Error(`Unexpected color type: ${rect.colorType}`);
      }
    }
  }

  private static createRectBorders(
    rect: Rect3D,
    rectGeometry: THREE.ShapeGeometry,
    isDarkMode: boolean,
  ): THREE.LineSegments {
    // create line edges for rect
    const edgeGeo = new THREE.EdgesGeometry(rectGeometry);
    let edgeMaterial: THREE.Material;
    if (rect.cornerRadius) {
      edgeMaterial = new THREE.LineBasicMaterial({
        color: Canvas.RECT_EDGE_COLOR_ROUNDED,
        linewidth: 1,
      });
    } else {
      edgeMaterial = new THREE.LineBasicMaterial({
        color: isDarkMode
          ? Canvas.RECT_EDGE_COLOR_DARK_MODE
          : Canvas.RECT_EDGE_COLOR_LIGHT_MODE,
        linewidth: 1,
      });
    }
    const lineSegments = new THREE.LineSegments(edgeGeo, edgeMaterial);
    lineSegments.computeLineDistances();
    return lineSegments;
  }

  private makeLabelCircleMesh(
    circle: Circle3D,
    isDarkMode: boolean,
  ): THREE.Mesh {
    const geometry = new THREE.CircleGeometry(circle.radius, 20);
    const material = new THREE.MeshBasicMaterial({
      color: isDarkMode
        ? Canvas.RECT_EDGE_COLOR_DARK_MODE
        : Canvas.RECT_EDGE_COLOR_LIGHT_MODE,
    });
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.set(circle.center.x, circle.center.y, circle.center.z);
    return mesh;
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

  private clearLabels() {
    if (this.canvasLabels) {
      this.canvasLabels.innerHTML = '';
    }
  }
}
