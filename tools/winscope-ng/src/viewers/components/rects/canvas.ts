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
import {Rectangle} from "viewers/common/rectangle";
import * as THREE from "three";
import {CSS2DObject, CSS2DRenderer} from "three/examples/jsm/renderers/CSS2DRenderer";
import {OrbitControls} from "three/examples/jsm/controls/OrbitControls";
import {ViewerEvents} from "viewers/common/viewer_events";
import {Circle3D, ColorType, Label3D, Point3D, Rect3D, Scene3D, Transform3D} from "./types3d";

export class Canvas {
  private static readonly TARGET_SCENE_DIAGONAL = 4;
  private static readonly RECT_COLOR_HIGHLIGHTED = new THREE.Color(0xD2E3FC);
  private static readonly RECT_EDGE_COLOR = 0x000000;
  private static readonly LABEL_CIRCLE_COLOR = 0x000000;
  private static readonly LABEL_LINE_COLOR = 0x000000;
  private static readonly LABEL_LINE_COLOR_HIGHLIGHTED = 0x808080;

  private canvasRects: HTMLCanvasElement;
  private canvasLabels: HTMLElement;
  private camera?: THREE.OrthographicCamera;
  private scene?: THREE.Scene;
  private renderer?: THREE.WebGLRenderer;
  private labelRenderer?: CSS2DRenderer;
  private rects: Rectangle[] = [];
  private clickableObjects: THREE.Object3D[] = [];

  constructor(canvasRects: HTMLCanvasElement, canvasLabels: HTMLElement) {
    this.canvasRects = canvasRects;
    this.canvasLabels = canvasLabels;
  }

  draw(scene: Scene3D) {
    // Must set 100% width and height so the HTML element expands to the parent's
    // boundaries and the correct clientWidth and clientHeight values can be read
    this.canvasRects.style.width = "100%";
    this.canvasRects.style.height = "100%";
    let widthAspectRatioAdjustFactor: number;
    let heightAspectRatioAdjustFactor: number;

    if (this.canvasRects.clientWidth > this.canvasRects.clientHeight) {
      heightAspectRatioAdjustFactor = 1;
      widthAspectRatioAdjustFactor = this.canvasRects.clientWidth / this.canvasRects.clientHeight;
    } else {
      heightAspectRatioAdjustFactor = this.canvasRects.clientHeight / this.canvasRects.clientWidth;
      widthAspectRatioAdjustFactor = 1;
    }

    this.scene = new THREE.Scene();
    const scaleFactor = Canvas.TARGET_SCENE_DIAGONAL / scene.boundingBox.diagonal * scene.camera.zoomFactor;
    this.scene.scale.set(scaleFactor, -scaleFactor, scaleFactor);
    this.scene.translateX(scaleFactor * -scene.boundingBox.center.x);
    this.scene.translateY(scaleFactor * scene.boundingBox.center.y);
    this.scene.translateZ(scaleFactor * -scene.boundingBox.center.z);

    this.camera = new THREE.OrthographicCamera(
      -Canvas.TARGET_SCENE_DIAGONAL / 2 * widthAspectRatioAdjustFactor, Canvas.TARGET_SCENE_DIAGONAL / 2 * widthAspectRatioAdjustFactor,
      Canvas.TARGET_SCENE_DIAGONAL / 2 * heightAspectRatioAdjustFactor, -Canvas.TARGET_SCENE_DIAGONAL / 2 * heightAspectRatioAdjustFactor,
      0, 100,
    );

    const rotationAngleX = scene.camera.rotationFactor * Math.PI * 45 / 360;
    const rotationAngleY = rotationAngleX * 1.5;
    const cameraPosition = new THREE.Vector3(0, 0, Canvas.TARGET_SCENE_DIAGONAL);
    cameraPosition.applyAxisAngle(new THREE.Vector3(1, 0, 0), -rotationAngleX);
    cameraPosition.applyAxisAngle(new THREE.Vector3(0, 1, 0), rotationAngleY);

    this.camera.position.set(cameraPosition.x, cameraPosition.y, cameraPosition.z);
    this.camera.lookAt(0, 0, 0);

    this.renderer = new THREE.WebGLRenderer({
      antialias: true,
      canvas: this.canvasRects,
      alpha: true
    });

    this.labelRenderer = new CSS2DRenderer({element: this.canvasLabels});

    // set various factors for shading and shifting
    const numberOfRects = this.rects.length;
    this.drawRects(scene.rects);
    this.drawLabels(scene.labels);

    this.renderer.setSize(this.canvasRects!.clientWidth, this.canvasRects!.clientHeight);
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.compile(this.scene, this.camera);
    this.renderer.render(this.scene, this.camera);

    this.labelRenderer.setSize(this.canvasRects!.clientWidth, this.canvasRects!.clientHeight);
    this.labelRenderer.render(this.scene, this.camera);
  }

  public getClickedRectId(x: number, y: number, z: number): undefined|string {
    const clickPosition = new THREE.Vector3(x, y, z);
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(clickPosition, this.camera!);
    const intersected = raycaster.intersectObjects(this.clickableObjects);
    if (intersected.length > 0) {
      return intersected[0].object.name;
    }
    return undefined;
  }

  private drawRects(rects: Rect3D[]) {
    this.clickableObjects = [];
    rects.forEach(rect => {
      const rectGeometry = new THREE.PlaneGeometry(rect.width, rect.height);

      const rectMesh = this.makeRectMesh(rect, rectGeometry);
      const rectEdges = this.makeRectLineSegments(rectMesh, rectGeometry);
      const transform = this.toMatrix4(rect.transform);
      rectMesh.applyMatrix4(transform);
      rectEdges.applyMatrix4(transform);

      this.scene?.add(rectMesh);
      this.scene?.add(rectEdges);

      if (rect.isClickable) {
        this.clickableObjects.push(rectMesh);
      }
    });
  }

  private drawLabels(labels: Label3D[]) {
    this.clearLabels();
    labels.forEach((label) => {
      const circleMesh = this.makeLabelCircleMesh(label.circle);
      this.scene?.add(circleMesh);

      const linePoints = label.linePoints.map((point: Point3D) => {
        return new THREE.Vector3(point.x, point.y, point.z);
      });
      const lineGeometry = new THREE.BufferGeometry().setFromPoints(linePoints);
      const lineMaterial = new THREE.LineBasicMaterial({
        color: label.isHighlighted ? Canvas.LABEL_LINE_COLOR_HIGHLIGHTED : Canvas.LABEL_LINE_COLOR
      });
      const line = new THREE.Line(lineGeometry, lineMaterial);
      this.scene?.add(line);

      this.drawLabelTextHtml(label);
    });
  }

  private drawLabelTextHtml(label: Label3D) {
    // Add rectangle label
    const spanText: HTMLElement = document.createElement("span");
    spanText.innerText = label.text;
    spanText.className = "mat-body-1";

    // Hack: transparent/placeholder text used to push the visible text towards left
    // (towards negative x) and properly align it with the label's vertical segment
    const spanPlaceholder: HTMLElement = document.createElement("span");
    spanPlaceholder.innerText = label.text;
    spanPlaceholder.className = "mat-body-1";
    spanPlaceholder.style.opacity = "0";

    const div: HTMLElement = document.createElement("div");
    div.className = "rect-label";
    div.style.display = "inline";
    div.appendChild(spanText);
    div.appendChild(spanPlaceholder);

    div.style.marginTop = "5px";
    if (label.isHighlighted) {
      div.style.color = "gray";
    }
    div.style.pointerEvents = "auto";
    div.style.cursor = "pointer";
    div.addEventListener(
      "click", (event) => this.propagateUpdateHighlightedItems(event, label.rectId)
    );

    const labelCss = new CSS2DObject(div);
    labelCss.position.set(label.textCenter.x, label.textCenter.y, label.textCenter.z);

    this.scene?.add(labelCss);
  }

  private toMatrix4(transform: Transform3D): THREE.Matrix4 {
    return new THREE.Matrix4().setFromMatrix3(
      new THREE.Matrix3().set(
        transform.dsdx, transform.dsdy, transform.tx,
        transform.dtdx, transform.dtdy, transform.ty,
        0, 0, 1
      )
    );
  }

  private makeRectMesh(
    rect: Rect3D,
    geometry: THREE.PlaneGeometry,
  ): THREE.Mesh {
    let color: THREE.Color;

    switch (rect.colorType) {
      case ColorType.VISIBLE: {
        // green (darkness depends on z order)
        const red = ((200 - 45) * rect.darkFactor + 45) / 255;
        const green = ((232 - 182) * rect.darkFactor + 182) / 255;
        const blue = ((183 - 44) * rect.darkFactor + 44) / 255;
        color = new THREE.Color(red, green, blue);
        break;
      }
      case ColorType.NOT_VISIBLE: {
        // gray (darkness depends on z order)
        const lower = 120;
        const upper = 220;
        const darkness = ((upper - lower) * rect.darkFactor + lower) / 255;
        color = new THREE.Color(darkness, darkness, darkness);
        break;
      }
      case ColorType.HIGHLIGHTED: {
        color = Canvas.RECT_COLOR_HIGHLIGHTED;
        break;
      }
    }

    const mesh = new THREE.Mesh(
      geometry,
      new THREE.MeshBasicMaterial({
        color: color,
        opacity: 0.75,
        transparent: true,
      }));
    mesh.position.x = rect.center.x;
    mesh.position.y = rect.center.y;
    mesh.position.z = rect.center.z;
    mesh.name = `${rect.id}`;

    return mesh;
  }

  private makeRectLineSegments(rectMesh: THREE.Mesh, geometry: THREE.PlaneGeometry): THREE.LineSegments {
    const edgeGeo = new THREE.EdgesGeometry(geometry);
    const edgeMaterial = new THREE.LineBasicMaterial({
      color: Canvas.RECT_EDGE_COLOR, linewidth: 1
    });
    const edgeSegments = new THREE.LineSegments(
      edgeGeo, edgeMaterial
    );
    edgeSegments.position.set(rectMesh.position.x, rectMesh.position.y, rectMesh.position.z);
    return edgeSegments;
  }

  private makeLabelCircleMesh(circle: Circle3D): THREE.Mesh {
    const geometry = new THREE.CircleGeometry(circle.radius, 20);
    const material = new THREE.MeshBasicMaterial({ color: Canvas.LABEL_CIRCLE_COLOR });
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.set(circle.center.x, circle.center.y, circle.center.z);
    return mesh;
  }

  private propagateUpdateHighlightedItems(event: MouseEvent, newId: number) {
    event.preventDefault();
    const highlightedChangeEvent: CustomEvent = new CustomEvent(
      ViewerEvents.HighlightedChange,
      {
        bubbles: true,
        detail: { id: `${newId}` }
      });
    event.target?.dispatchEvent(highlightedChangeEvent);
  }

  private clearLabels() {
    this.canvasLabels.innerHTML = "";
  }
}
